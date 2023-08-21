package city.newnan.mcron

import city.newnan.mcron.config.ConfigFile
import city.newnan.mcron.timeiterator.CronExpression
import city.newnan.mcron.timeiterator.TimeIterator
import me.lucko.helper.Schedulers
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class CronManager : me.lucko.helper.terminable.Terminable {
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    private var newTasks = mutableListOf<CronCommand>() // 线程安全
    internal val tasks = mutableListOf<CronCommand>()
    internal val outdatedTasks = mutableListOf<CronCommand>()

    // 1秒内即将执行的任务
    private var inTimeTasks: MutableList<CronCommand> = mutableListOf()

    // 为防止卡顿导致错过一些任务(没有执行，但是错过了判断，被误认为是已过期任务)
    // 有一个缓冲池，在1s~60s后即将执行的命令也会在这里，这样如果这些任务过期了会立即执行
    // p.s. 不会有人写每秒都会运行的程序吧...
    private val cacheInTimeTasks = mutableListOf<CronCommand>()
    private var cronTask: me.lucko.helper.scheduler.Task? = null
    private var needReload = false

    init {
        reload()
        bindWith(PluginMain.INSTANCE)
    }

    fun run() {
        cronTask = Schedulers.async().runRepeating(Runnable { this.runCheck() }, 0, 20)
        println("run")
    }

    fun reload() {
        needReload = true
        println("needReload = true")
    }

    /**
     * 定时检查模块的计时器
     */
    private var secondsCounter = 1L

    /**
     * 计时器的动态上限
     */
    private var counterBorder = 1L

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

                PluginMain.INSTANCE.configManager.parse<ConfigFile>("config.yml").also {
                    // 设置时区
                    CronExpression.setTimeZoneOffset(it.timezoneOffset)
                    dateFormatter.timeZone = TimeZone.getTimeZone(CronExpression.localTimezoneOffset)
                    // 重载
                    it.scheduleTasks.forEach { (key, value) ->
                        addTask(key, value)
                    }
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
        val curMillisecond = System.currentTimeMillis()
        val tmpOutdated: MutableList<CronCommand> = ArrayList()

        // 遍历所有任务
        for (task in tasks) {
            var tmp = task.expression.getNextTime(curMillisecond)

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
            Schedulers.sync().runLater({ runInSecond() }, 20)
        }

        // 计算下次检查时间
        counterBorder = getIntervalSeconds(nextMillisecond)
        secondsCounter = 1
    }

    /**
     * 根据毫秒差获得合适的休眠间隔(秒为单位)
     * @param delta 毫秒差
     * @return 休眠间隔(秒为单位)
     */
    private fun getIntervalSeconds(delta: Long): Long {
        // 小于30秒   - 每秒
        if (delta < MILLISECOND_OF_30SEC) return SECONDS_OF_SECOND

        // 小于1分钟  - 每5秒
        if (delta < MILLISECOND_OF_MINUTE) return SECONDS_OF_5SEC

        // 小于5分钟  - 每15秒
        if (delta < MILLISECOND_OF_5MIN) return SECONDS_OF_15SEC

        // 小于15分钟 - 每1分钟
        if (delta < MILLISECOND_OF_15MIN) return SECONDS_OF_MINUTE

        // 小于30分钟 - 每4分钟
        if (delta < MILLISECOND_OF_30MIN) return SECONDS_OF_4MIN

        // 小于1小时 - 每8分钟
        if (delta < MILLISECOND_OF_HOUR) return SECONDS_OF_8MIN

        // 小于2小时 - 每15分钟
        if (delta < MILLISECOND_OF_2HOUR) return SECONDS_OF_15MIN

        // 小于4小时 - 每半小时
        if (delta < MILLISECOND_OF_4HOUR) return SECONDS_OF_30MIN

        // 小于12小时 - 每1小时
        if (delta < MILLISECOND_OF_12HOUR) return SECONDS_OF_1HOUR

        // 小于1天 - 每3小时
        if (delta < MILLISECOND_OF_1DAY) return SECONDS_OF_3HOUR

        // 小于2天 - 每6小时
        return if (delta < MILLISECOND_OF_2DAY) SECONDS_OF_6HOUR else SECONDS_OF_12HOUR

        // 其他 - 每12小时
    }

    /**
     * 执行inTimeTask中的那些一秒内即将执行的任务
     */
    private fun runInSecond() {
        val tasks = inTimeTasks
        inTimeTasks = mutableListOf()
        tasks.forEach { task ->
            task.expression.onExecute()
            PluginMain.INSTANCE.executeCommands(task.commands)
        }
    }

    override fun close() {
        cronTask?.stop()
    }

    /**
     * 在任务池中添加一个任务
     * @param cronExpression cron表达式
     * @param commands 任务要执行的指令
     */
    private fun addTask(cronExpression: String, commands: Array<String>) {
        try {
            val task = CronCommand(cronExpression, commands)
            newTasks.add(task)
        } catch (e: Exception) {
            PluginMain.INSTANCE.messageManager.apply {
                warn(sprintf("\$msg.invalid_expression$", cronExpression))
            }
        }
    }

    fun addTask(nextTime: Long, commands: Array<String>) {
        val task = CronCommand(nextTime, commands)
        newTasks.add(task)
    }

    internal class CronCommand {
        val expression: TimeIterator
        val commands: Array<String>

        constructor(expression: String, commands: Array<String>) {
            this.expression = CronExpression(expression)
            this.commands = commands
        }

        constructor(nextTime: Long, commands: Array<String>) {
            var hasExecuted = false
            this.expression = object : TimeIterator {
                override fun getNextTime(now: Long): Long {
                    return if (hasExecuted) 0L else nextTime
                }

                override fun onExecute() {
                    hasExecuted = true
                }
            }
            this.commands = commands
        }
    }

    companion object {
        /**
         * 一段时间所对应的秒数
         */
        private val SECONDS_OF_12HOUR = TimeUnit.HOURS.toSeconds(12)
        private val SECONDS_OF_6HOUR = TimeUnit.HOURS.toSeconds(6)
        private val SECONDS_OF_3HOUR = TimeUnit.HOURS.toSeconds(3)
        private val SECONDS_OF_1HOUR = TimeUnit.HOURS.toSeconds(1)
        private val SECONDS_OF_30MIN = TimeUnit.MINUTES.toSeconds(30)
        private val SECONDS_OF_15MIN = TimeUnit.MINUTES.toSeconds(15)
        private val SECONDS_OF_8MIN = TimeUnit.MINUTES.toSeconds(8)
        private val SECONDS_OF_4MIN = TimeUnit.MINUTES.toSeconds(4)
        private val SECONDS_OF_MINUTE = TimeUnit.MINUTES.toSeconds(1)
        private val SECONDS_OF_15SEC = TimeUnit.SECONDS.toSeconds(15)
        private val SECONDS_OF_5SEC = TimeUnit.SECONDS.toSeconds(5)
        private val SECONDS_OF_SECOND = TimeUnit.SECONDS.toSeconds(1)

        /**
         * 一段时间所对应的毫秒数
         */
        private val MILLISECOND_OF_2DAY: Long = TimeUnit.DAYS.toMillis(2)
        private val MILLISECOND_OF_1DAY: Long = TimeUnit.DAYS.toMillis(1)
        private val MILLISECOND_OF_12HOUR: Long = TimeUnit.HOURS.toMillis(12)
        private val MILLISECOND_OF_4HOUR: Long = TimeUnit.HOURS.toMillis(4)
        private val MILLISECOND_OF_2HOUR: Long = TimeUnit.HOURS.toMillis(2)
        private val MILLISECOND_OF_HOUR: Long = TimeUnit.HOURS.toMillis(1)
        private val MILLISECOND_OF_30MIN: Long = TimeUnit.MINUTES.toMillis(30)
        private val MILLISECOND_OF_15MIN: Long = TimeUnit.MINUTES.toMillis(15)
        private val MILLISECOND_OF_5MIN: Long = TimeUnit.MINUTES.toMillis(5)
        private val MILLISECOND_OF_MINUTE: Long = TimeUnit.MINUTES.toMillis(1)
        private val MILLISECOND_OF_30SEC: Long = TimeUnit.SECONDS.toMillis(30)
    }
}