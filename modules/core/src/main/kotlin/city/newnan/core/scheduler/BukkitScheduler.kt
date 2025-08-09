package city.newnan.core.scheduler

import com.google.common.collect.ImmutableList
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.scheduler.BukkitTask

/**
 * 任务调度器实现
 * 
 * 提供统一的任务创建和管理功能，使用新的TaskHandler设计
 * 实现Terminable接口来管理任务生命周期
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class BukkitScheduler(override val plugin: Plugin) : IScheduler {
    
    /**
     * 跟踪所有创建的任务处理器
     */
    private val activeTasks = ConcurrentHashMap<Int, IInternalTaskHandler<*>>()
    
    /**
     * 任务ID生成器
     */
    private val taskIdGenerator = java.util.concurrent.atomic.AtomicInteger(0)

    /**
     * 依赖关系入度表
     */
    private val dependentTasks = ConcurrentHashMap<Int, MutableSet<IInternalTaskHandler<*>>>()

    override fun combinedTaskHandlers(mode: CombindMode, vararg handlers: ITaskHandler<*>): ITaskHandler<Unit> {
        val taskId = taskIdGenerator.incrementAndGet()
        val handler = CombinedTaskHandler(taskId, this, mode, handlers.toList())
        activeTasks[handler.taskId] = handler
        // 更新依赖反查表
        handlers.forEach { dependency ->
            if (dependency.isCompleted() || !activeTasks.contains(dependency.taskId)) return@forEach
            dependentTasks.computeIfAbsent(dependency.taskId) { ConcurrentHashMap.newKeySet() }.add(handler)
        }
        // 检查是否可以立即启动
        handler.start()
        return handler
    }

    // ==================== 基础任务创建 ====================
    
    /**
     * 运行同步任务
     */
    override fun <T> runSync(dependencies: Collection<ITaskHandler<*>>?, function: (ITaskHandler<T>) -> T): ITaskHandler<T> {
        return createTask(function, dependencies) { Bukkit.getScheduler().runTask(plugin, it) }
    }
    
    /**
     * 运行异步任务
     */
    override fun <T> runAsync(dependencies: Collection<ITaskHandler<*>>?, function: (ITaskHandler<T>) -> T): ITaskHandler<T> {
        return createTask(function, dependencies) { Bukkit.getScheduler().runTaskAsynchronously(plugin, it) }
    }
    
    // ==================== 延迟任务 ====================
    
    /**
     * 延迟运行同步任务（以tick为单位）
     */
    override fun <T> runSyncLater(delay: Long, dependencies: Collection<ITaskHandler<*>>?, function: (ITaskHandler<T>) -> T): ITaskHandler<T> {
        return createTask(function, dependencies) { Bukkit.getScheduler().runTaskLater(plugin, it, delay) }
    }
    
    /**
     * 延迟运行异步任务（以tick为单位）
     */
    override fun <T> runAsyncLater(delay: Long, dependencies: Collection<ITaskHandler<*>>?, function: (ITaskHandler<T>) -> T): ITaskHandler<T> {
        return createTask(function, dependencies) { Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, it, delay) }
    }
    
    /**
     * 延迟运行同步任务（以时间单位为单位）
     */
    override fun <T> runSyncLater(delay: Long, unit: TimeUnit, dependencies: Collection<ITaskHandler<*>>?, function: (ITaskHandler<T>) -> T): ITaskHandler<T> {
        val delayTicks = ticksFromDuration(delay, unit)
        return createTask(function, dependencies) { Bukkit.getScheduler().runTaskLater(plugin, it, delayTicks) }
    }
    
    /**
     * 延迟运行异步任务（以时间单位为单位）
     */
    override fun <T> runAsyncLater(delay: Long, unit: TimeUnit, dependencies: Collection<ITaskHandler<*>>?, function: (ITaskHandler<T>) -> T): ITaskHandler<T> {
        val delayTicks = ticksFromDuration(delay, unit)
        return createTask(function, dependencies) { Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, it, delayTicks) }
    }
    
    // ==================== 重复任务 ====================
    
    /**
     * 运行重复同步任务（以tick为单位）
     */
    override fun runSyncRepeating(delay: Long, period: Long, dependencies: Collection<ITaskHandler<*>>?, function: (ITaskHandler<Unit>) -> Unit): ITaskHandler<Unit> {
        return createTask(function, dependencies) { Bukkit.getScheduler().runTaskTimer(plugin, it, delay, period) }
    }
    
    /**
     * 运行重复异步任务（以tick为单位）
     */
    override fun runAsyncRepeating(delay: Long, period: Long, dependencies: Collection<ITaskHandler<*>>?, function: (ITaskHandler<Unit>) -> Unit): ITaskHandler<Unit> {
        return createTask(function, dependencies) { Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, it, delay, period) }
    }
    
    /**
     * 运行重复同步任务（以时间单位为单位）
     */
    override fun runSyncRepeating(delay: Long, delayUnit: TimeUnit, period: Long, periodUnit: TimeUnit, dependencies: Collection<ITaskHandler<*>>?, function: (ITaskHandler<Unit>) -> Unit): ITaskHandler<Unit> {
        val delayTicks = ticksFromDuration(delay, delayUnit)
        val periodTicks = ticksFromDuration(period, periodUnit)
        return createTask(function, dependencies) { Bukkit.getScheduler().runTaskTimer(plugin, it, delayTicks, periodTicks) }
    }
    
    /**
     * 运行重复异步任务（以时间单位为单位）
     */
    override fun runAsyncRepeating(delay: Long, delayUnit: TimeUnit, period: Long, periodUnit: TimeUnit, dependencies: Collection<ITaskHandler<*>>?, function: (ITaskHandler<Unit>) -> Unit): ITaskHandler<Unit> {
        val delayTicks = ticksFromDuration(delay, delayUnit)
        val periodTicks = ticksFromDuration(period, periodUnit)
        return createTask(function, dependencies) { Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, it, delayTicks, periodTicks) }
    }
    
    // ==================== 内部实现方法 ====================

    /**
     * 创建通用任务
     */
    private fun <T> createTask(
        runner: (ITaskHandler<T>) -> T,
        dependencies: Collection<ITaskHandler<*>>? = null,
        bukkitTaskBuilder: (Runnable) -> BukkitTask
    ): ITaskHandler<T> {
        if (!dependencies.isNullOrEmpty() && dependencies.any { it.getScheduler() != this }) {
            throw IllegalArgumentException("Cannot create dependent task with tasks from other schedulers")
        }
        val taskId = taskIdGenerator.incrementAndGet()
        val handler = BukkitTaskHandler<T>(
            taskId = taskId,
            scheduler = this,
            runner = runner,
            bukkitTaskBuilder = bukkitTaskBuilder,
            dependencies = if (dependencies.isNullOrEmpty()) null else ImmutableList.copyOf(dependencies)
        )
        activeTasks[taskId] = handler
        // 更新依赖反查表
        dependencies?.forEach { dependency ->
            if (dependency.isCompleted() || !activeTasks.contains(dependency.taskId)) return@forEach
            dependentTasks.computeIfAbsent(dependency.taskId) { ConcurrentHashMap.newKeySet() }.add(handler)
        }
        // 检查是否可以立即启动
        handler.start()
        return handler
    }

    /**
     * 任务完成时的内部处理
     */
    internal fun onTaskCompleted(taskId: Int, handler: IInternalTaskHandler<*>) {
        activeTasks.remove(taskId)
        
        // 使用倒排索引快速查找依赖于此任务的所有任务
        // 只有成功完成的任务才会触发依赖任务的启动
        if (handler.isSuccessful()) {
            dependentTasks[taskId]?.forEach { dependentTask ->
                dependentTask.start()
            }
        }
        
        // 清理依赖关系
        dependentTasks.remove(taskId)
    }
    
    /**
     * 将时间单位转换为tick
     */
    private fun ticksFromDuration(duration: Long, unit: TimeUnit): Long {
        return unit.toMillis(duration) / 50L // 1 tick = 50ms
    }
    
    // ==================== 资源管理 ====================
    
    /**
     * 获取活动任务数量
     */
    override fun getActiveTaskCount(): Int {
        return activeTasks.size
    }
    
    /**
     * 关闭调度器，停止所有任务
     */
    override fun close() {
        activeTasks.values.forEach { task ->
            try {
                task.close()
            } catch (e: Exception) {
                plugin.logger.warning("停止任务时出错: ${e.message}")
            }
        }
        activeTasks.clear()
        dependentTasks.clear()
    }
    
    override fun isClosed(): Boolean {
        return activeTasks.isEmpty()
    }
}