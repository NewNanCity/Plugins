package city.newnan.core.terminable

import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask

/**
 * 可终止的Bukkit任务包装器
 * 
 * 将BukkitTask包装为Terminable，使其可以被CompositeTerminable管理。
 * 当关闭时，会自动取消任务。
 * 
 * @param task 要包装的BukkitTask
 */
class TerminableTask(private val task: BukkitTask) : Terminable {
    
    override fun close() {
        if (!task.isCancelled) {
            task.cancel()
        }
    }
    
    override fun isClosed(): Boolean = task.isCancelled
    
    /**
     * 获取包装的BukkitTask
     */
    fun getTask(): BukkitTask = task
    
    /**
     * 获取任务ID
     */
    fun getTaskId(): Int = task.taskId
    
    /**
     * 检查任务是否同步
     */
    fun isSync(): Boolean = task.isSync
    
    companion object {
        /**
         * 创建一个可终止的同步任务
         * 
         * @param plugin 插件实例
         * @param runnable 要执行的任务
         * @return TerminableTask实例
         */
        fun runTask(plugin: Plugin, runnable: Runnable): TerminableTask {
            val task = plugin.server.scheduler.runTask(plugin, runnable)
            return TerminableTask(task)
        }
        
        /**
         * 创建一个可终止的异步任务
         * 
         * @param plugin 插件实例
         * @param runnable 要执行的任务
         * @return TerminableTask实例
         */
        fun runTaskAsynchronously(plugin: Plugin, runnable: Runnable): TerminableTask {
            val task = plugin.server.scheduler.runTaskAsynchronously(plugin, runnable)
            return TerminableTask(task)
        }
        
        /**
         * 创建一个可终止的延迟同步任务
         * 
         * @param plugin 插件实例
         * @param runnable 要执行的任务
         * @param delay 延迟时间（tick）
         * @return TerminableTask实例
         */
        fun runTaskLater(plugin: Plugin, runnable: Runnable, delay: Long): TerminableTask {
            val task = plugin.server.scheduler.runTaskLater(plugin, runnable, delay)
            return TerminableTask(task)
        }
        
        /**
         * 创建一个可终止的延迟异步任务
         * 
         * @param plugin 插件实例
         * @param runnable 要执行的任务
         * @param delay 延迟时间（tick）
         * @return TerminableTask实例
         */
        fun runTaskLaterAsynchronously(plugin: Plugin, runnable: Runnable, delay: Long): TerminableTask {
            val task = plugin.server.scheduler.runTaskLaterAsynchronously(plugin, runnable, delay)
            return TerminableTask(task)
        }
        
        /**
         * 创建一个可终止的定时同步任务
         * 
         * @param plugin 插件实例
         * @param runnable 要执行的任务
         * @param delay 初始延迟时间（tick）
         * @param period 执行间隔（tick）
         * @return TerminableTask实例
         */
        fun runTaskTimer(plugin: Plugin, runnable: Runnable, delay: Long, period: Long): TerminableTask {
            val task = plugin.server.scheduler.runTaskTimer(plugin, runnable, delay, period)
            return TerminableTask(task)
        }
        
        /**
         * 创建一个可终止的定时异步任务
         * 
         * @param plugin 插件实例
         * @param runnable 要执行的任务
         * @param delay 初始延迟时间（tick）
         * @param period 执行间隔（tick）
         * @return TerminableTask实例
         */
        fun runTaskTimerAsynchronously(plugin: Plugin, runnable: Runnable, delay: Long, period: Long): TerminableTask {
            val task = plugin.server.scheduler.runTaskTimerAsynchronously(plugin, runnable, delay, period)
            return TerminableTask(task)
        }
    }
}
