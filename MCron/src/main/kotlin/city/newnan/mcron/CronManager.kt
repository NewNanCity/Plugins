package city.newnan.mcron

import city.newnan.violet.message.MessageManager
import city.newnan.violet.config.ConfigManager
import city.newnan.violet.config.setListIfNull
import me.lucko.helper.Schedulers
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.function.Consumer


class CronManager : me.lucko.helper.terminable.Terminable {
    private val tasks = ArrayList<CronCommand>()
    private val outdatedTasks: MutableList<CronCommand> = ArrayList()

    // 1秒内即将执行的任务
    private val inTimeTasks: MutableList<CronCommand> = ArrayList()

    // 为防止卡顿导致错过一些任务(没有执行，但是错过了判断，被误认为是已过期任务)
    // 有一个缓冲池，在1s~60s后即将执行的命令也会在这里，这样如果这些任务过期了会立即执行
    // p.s. 不会有人写每秒都会运行的程序吧...
    private val cacheInTimeTasks: MutableList<CronCommand> = ArrayList()
    private var cronTask: me.lucko.helper.scheduler.Task? = null

    init {
        reload()
        bindWith(MCron.INSTANCE)
    }

    fun run() {
        cronTask = Schedulers.async().runRepeating(Runnable { this.runCheck() }, 0, 20)
    }

    fun reload() {
        // 清空
        tasks.clear()
        cacheInTimeTasks.clear()
        inTimeTasks.clear()
        outdatedTasks.clear()

        MCron.INSTANCE.configManager["config.yml"]?.also {
            // 设置时区
            CronExpression.setTimeZoneOffset(it.getNode("timezone-offset").getString("Z"))
            // 重载
            it.getNode("schedule-tasks").childrenMap
                .forEach { (key, value) ->
                    if (key is String) {
                        addTask(key, value.setListIfNull().getList { obj: Any -> obj.toString() }.toTypedArray())
                    }
                }
        }
    }

    private var dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    internal fun listCron(sender: CommandSender?) {
        MCron.INSTANCE.messageManager.run {
            printf(sender, "\$msg.list-head$")
            tasks.forEach(Consumer { task: CronCommand ->
                printf(
                    sender, "\$msg.list-cron$",
                    task.expression.expressionString, dateFormatter.format(Date(task.expression.getNextTime()))
                )
                for (command in task.commands) {
                    printf(sender, "\$msg.list-command$", command)
                }
                printf(sender, "")
            })
            outdatedTasks.forEach(Consumer { task: CronCommand ->
                printf(
                    sender, "\$msg.list-cron-outdated$",
                    task.expression.expressionString, dateFormatter.format(Date(task.expression.getNextTime()))
                )
                for (command in task.commands) {
                    printf(sender, "\$msg.list-command$", command)
                }
                printf(sender, "")
            })
        }
    }

    /**
     * 定时检查模块的计时器
     */
    private var secondsCounter = 1

    /**
     * 计时器的动态上限
     */
    private var counterBorder = 1

    /**
     * 定时任务检查，每秒运行
     */
    private fun runCheck() {
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
            var tmp = task.expression.getNextTime()

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
            tmpOutdated.forEach(Consumer { o: CronCommand ->
                tasks.remove(
                    o
                )
            })
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
    private fun getIntervalSeconds(delta: Long): Int {
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
        val sender: CommandSender = Bukkit.getConsoleSender()
        val messageManager: MessageManager = MCron.INSTANCE.messageManager
        inTimeTasks.forEach(Consumer { task: CronCommand ->
            for (command in task.commands) {
                messageManager info "§a§lRun Command: §r$command"
                Bukkit.dispatchCommand(sender, command)
            }
        })
        inTimeTasks.clear()
    }

    override fun close() {
        cronTask?.stop()
    }

    /**
     * 在任务池中添加一个任务
     * @param cronExpression cron表达式
     * @param commands 任务要执行的指令
     */
    internal fun addTask(cronExpression: String?, commands: Array<String>) {
        try {
            val task = CronCommand(cronExpression, commands)
            tasks.add(task)
        } catch (e: Exception) {
            MCron.INSTANCE.messageManager.apply {
                warn(sprintf("\$msg.invalid_expression$", cronExpression))
            }
        }
    }

    internal class CronCommand(expression: String?, commands: Array<String>) {
        val expression: CronExpression
        val commands: Array<String>

        init {
            this.expression = CronExpression(expression!!)
            this.commands = commands
        }
    }

    companion object {
        /**
         * 一段时间所对应的秒数
         */
        private const val SECONDS_OF_12HOUR = 43200
        private const val SECONDS_OF_6HOUR = 21600
        private const val SECONDS_OF_3HOUR = 10800
        private const val SECONDS_OF_1HOUR = 3600
        private const val SECONDS_OF_30MIN = 1800
        private const val SECONDS_OF_15MIN = 900
        private const val SECONDS_OF_8MIN = 480
        private const val SECONDS_OF_4MIN = 240
        private const val SECONDS_OF_MINUTE = 60
        private const val SECONDS_OF_15SEC = 15
        private const val SECONDS_OF_5SEC = 5
        private const val SECONDS_OF_SECOND = 1

        /**
         * 一段时间所对应的毫秒数
         */
        private const val MILLISECOND_OF_2DAY: Long = 172800000
        private const val MILLISECOND_OF_1DAY: Long = 86400000
        private const val MILLISECOND_OF_12HOUR: Long = 43200000
        private const val MILLISECOND_OF_4HOUR: Long = 14400000
        private const val MILLISECOND_OF_2HOUR: Long = 7200000
        private const val MILLISECOND_OF_HOUR: Long = 3600000
        private const val MILLISECOND_OF_30MIN: Long = 1800000
        private const val MILLISECOND_OF_15MIN: Long = 900000
        private const val MILLISECOND_OF_5MIN: Long = 300000
        private const val MILLISECOND_OF_MINUTE: Long = 60000
        private const val MILLISECOND_OF_30SEC: Long = 30000
    }
}