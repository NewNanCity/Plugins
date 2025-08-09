package city.newnan.mcron.modules

import city.newnan.core.base.BaseModule
import city.newnan.core.scheduler.runAsyncRepeating
import city.newnan.core.scheduler.runSyncLater
import city.newnan.mcron.MCronPlugin
import city.newnan.mcron.config.CronTask
import city.newnan.mcron.util.CronExpressionPreprocessor
import city.newnan.mcron.util.IntervalCalculator
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import java.time.ZoneOffset
import java.time.ZonedDateTime

class CronManager(private val plugin: MCronPlugin) : BaseModule("CronManager", plugin) {
    // 设置
    var timezoneOffset = ZoneOffset.UTC
        private set

    // Cron解析器
    val cronParser = CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ))

    // 任务池 - 无竞争，不需要确保线程安全
    private var newTasks = mutableListOf<CronTask>()
    internal val tasks = mutableListOf<CronTask>()
    internal val outdatedTasks = mutableListOf<CronTask>()

    // 1秒内即将执行的任务
    private var inTimeTasks: MutableList<CronTask> = mutableListOf()

    // 为防止卡顿导致错过一些任务(没有执行，但是错过了判断，被误认为是已过期任务)
    // 有一个缓冲池，在1s~60s后即将执行的命令也会在这里，这样如果这些任务过期了会立即执行
    // p.s. 不会有人写每秒都会运行的程序吧...
    private val cacheInTimeTasks = mutableListOf<CronTask>()
    private var needReload = false

    // 定时检查模块的计时器
    private var secondsCounter = 1L

    // 计时器的动态上限
    private var counterBorder = 1L

    init { init() }

    override fun onInit() {
        runAsyncRepeating(0L, 20L) {
            runCheck()
        }
    }

    override fun onReload() {
        val config = plugin.getPluginConfig()
        timezoneOffset = config.timezone.getZoneOffset()
        needReload = true
    }

    /**
     * 定时任务检查，每秒运行
     */
    private fun runCheck() {
        if (needReload) {
            try {
                // 清空
                tasks.clear()
                cacheInTimeTasks.clear()
                inTimeTasks.clear()
                outdatedTasks.clear()

                // 重载
                val config = plugin.getPluginConfig()
                config.tasks.scheduledTasks.forEach { (cronExpression, commands) ->
                    addTask(cronExpression, commands)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            needReload = false
        }

        // 线程安全
        if (newTasks.isNotEmpty()) {
            val newTasks = this.newTasks
            this.newTasks = mutableListOf()
            tasks.addAll(newTasks)
            // 不休眠，立即检查任务
            secondsCounter = 1L
            counterBorder = 1L
        }

        // 小于计数上限，继续休眠
        if (secondsCounter < counterBorder) {
            secondsCounter++
            return
        }

        // 下一次任务的间隔毫秒数(不含失效任务)
        var nextMillisecond = Long.MAX_VALUE

        // 当前时间戳
        val curZonedDateTime = ZonedDateTime.now(timezoneOffset)
        val curMillisecond = curZonedDateTime.toInstant().toEpochMilli()
        val tmpOutdated = mutableListOf<CronTask>()

        // 遍历所有任务
        for (task in tasks) {
            var tmp = task.getNextTime(curZonedDateTime)

            // 忽略失效的，并清理之
            if (tmp == 0L) {
                tmpOutdated.add(task)
                // 如果失效任务在缓冲列表中，肯定是漏掉了，赶快执行
                if (cacheInTimeTasks.contains(task)) {
                    cacheInTimeTasks.remove(task)
                    inTimeTasks.add(task)
                }
                continue
            }
            tmp -= curMillisecond

            // 统计下一次任务的间隔时长(不含失效任务)
            if (tmp < nextMillisecond) {
                nextMillisecond = tmp
            }

            // 一秒内即将执行的任务进入执行队列
            if (tmp <= 1000) {
                inTimeTasks.add(task)
                // 并从缓冲列表移除
                cacheInTimeTasks.remove(task)
                continue
            }

            // 1s~60s进入缓冲列表
            if (tmp <= 60000) {
                if (!cacheInTimeTasks.contains(task)) {
                    cacheInTimeTasks.add(task)
                }
                continue
            }

            // 到这里的肯定是60秒开外的任务
            // 如果这些任务中有些任务出现在缓冲区中
            // 说明漏掉了，尽快执行
            if (cacheInTimeTasks.contains(task)) {
                cacheInTimeTasks.remove(task)
                inTimeTasks.add(task)
            }
        }

        // 清理失效任务
        if (tmpOutdated.isNotEmpty()) {
            tmpOutdated.forEach { tasks.remove(it) }
            outdatedTasks.addAll(tmpOutdated)
            tmpOutdated.clear()
        }

        // 执行一秒内到来的任务
        if (inTimeTasks.isNotEmpty()) {
            runSyncLater(20L) { runInSecond() }
        }

        // 计算下次检查时间
        counterBorder = IntervalCalculator.getIntervalSeconds(nextMillisecond)
        secondsCounter = 1L
    }

    fun createTask(cronExpression: String, commands: List<String>): CronTask {
        try {
            // 预处理cron表达式，支持多语言星期和月份名称
            val processedExpression = CronExpressionPreprocessor.preprocess(cronExpression)

            // 解析cron表达式
            val executionTime = ExecutionTime.forCron(cronParser.parse(processedExpression))

            return CronTask(cronExpression, executionTime, commands)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid cron expression: $cronExpression", e)
        }
    }


    /**
     * 执行inTimeTask中的那些一秒内即将执行的任务
     */
    private fun runInSecond() {
        val tasks = inTimeTasks
        inTimeTasks = mutableListOf()
        tasks.forEach { plugin.executeCommands(it.commands.toList(), "SCHEDULED") }
    }

    /**
     * 在任务池中添加一个任务
     *
     * @param cronExpression cron表达式
     * @param commands 任务要执行的指令
     */
    fun addTask(cronExpression: String, commands: List<String>) {
        try {
            // 添加到任务池
            newTasks.add(createTask(cronExpression, commands))
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid cron expression: $cronExpression", e)
        }
    }

    fun addTask(nextTime: Long, commands: Array<String>) {
        newTasks.add(CronTask(nextTime, commands))
    }
}