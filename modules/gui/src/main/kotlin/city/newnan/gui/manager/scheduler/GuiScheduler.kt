package city.newnan.gui.manager.scheduler

import city.newnan.core.scheduler.BukkitScheduler
import city.newnan.core.scheduler.ITaskHandler
import city.newnan.core.terminable.CompositeTerminable
import city.newnan.core.terminable.Terminable
import city.newnan.core.terminable.TerminableConsumer
import city.newnan.gui.manager.GuiManager
import city.newnan.gui.manager.logging.GuiLogger
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

/**
 * GUI调度器
 *
 * 为GUI组件和页面提供完整的调度器API，与core模块保持一致。
 * 委托给BasePlugin的调度器实现，同时提供GUI特定的资源管理。
 * 支持调度器方法和协程方法。
 */
class GuiScheduler(private val manager: GuiManager) : Terminable, TerminableConsumer {

    // =================== 核心属性 ===================

    // 资源管理器
    private val terminableRegistry = CompositeTerminable.create()

    private val activeTasks = mutableSetOf<ITaskHandler<*>>()
    private val activeJobs = mutableSetOf<Job>()
    private var closed = false

    // 调度器实例
    val scheduler: BukkitScheduler by lazy { BukkitScheduler(manager.plugin).also { bind(it) } }

    // ==================== 基础调度器方法 ====================

    /**
     * 运行同步任务
     */
    fun <T> runSync(task: () -> T, managed: Boolean = true): ITaskHandler<T> {
        checkNotClosed()
        val handler = scheduler.runSync { handler -> task() }
        if (managed) activeTasks.add(handler)
        return handler
    }

    /**
     * 运行异步任务
     */
    fun <T> runAsync(task: () -> T, managed: Boolean = true): ITaskHandler<T> {
        checkNotClosed()
        val handler = scheduler.runAsync { handler -> task() }
        if (managed) activeTasks.add(handler)
        return handler
    }

    // ==================== 延迟调度器方法 ====================

    /**
     * 延迟运行同步任务
     */
    fun <T> runSyncLater(delay: Long, task: () -> T, managed: Boolean = true): ITaskHandler<T> {
        checkNotClosed()
        val handler = scheduler.runSyncLater(delay) { handler -> task() }
        if (managed) activeTasks.add(handler)
        return handler
    }

    /**
     * 延迟运行异步任务
     */
    fun <T> runAsyncLater(delay: Long, task: () -> T, managed: Boolean = true): ITaskHandler<T> {
        checkNotClosed()
        val handler = scheduler.runAsyncLater(delay) { handler -> task() }
        if (managed) activeTasks.add(handler)
        return handler
    }

    // ==================== 时间单位延迟方法 ====================

    /**
     * 延迟运行同步任务（时间单位）
     */
    fun <T> runSyncLater(delay: Long, unit: TimeUnit, task: () -> T, managed: Boolean = true): ITaskHandler<T> {
        checkNotClosed()
        val handler = scheduler.runSyncLater(delay, unit) { handler -> task() }
        if (managed) activeTasks.add(handler)
        return handler
    }

    /**
     * 延迟运行异步任务（时间单位）
     */
    fun <T> runAsyncLater(delay: Long, unit: TimeUnit, task: () -> T, managed: Boolean = true): ITaskHandler<T> {
        checkNotClosed()
        val handler = scheduler.runAsyncLater(delay, unit) { handler -> task() }
        if (managed) activeTasks.add(handler)
        return handler
    }

    // ==================== 重复调度器方法 ====================

    /**
     * 重复运行同步任务
     */
    fun runSyncRepeating(delay: Long, period: Long, task: () -> Unit, managed: Boolean = true): ITaskHandler<Unit> {
        checkNotClosed()
        val handler = scheduler.runSyncRepeating(delay, period) { handler -> task() }
        if (managed) activeTasks.add(handler)
        return handler
    }

    /**
     * 重复运行异步任务
     */
    fun runAsyncRepeating(delay: Long, period: Long, task: () -> Unit, managed: Boolean = true): ITaskHandler<Unit> {
        checkNotClosed()
        val handler = scheduler.runAsyncRepeating(delay, period) { handler -> task() }
        if (managed) activeTasks.add(handler)
        return handler
    }

    // ==================== 时间单位重复方法 ====================

    /**
     * 重复运行同步任务（时间单位）
     */
    fun runSyncRepeating(delay: Long, delayUnit: TimeUnit, period: Long, periodUnit: TimeUnit, task: () -> Unit, managed: Boolean = true): ITaskHandler<Unit> {
        checkNotClosed()
        val handler = scheduler.runSyncRepeating(delay, delayUnit, period, periodUnit) { handler -> task() }
        if (managed) activeTasks.add(handler)
        return handler
    }

    /**
     * 重复运行异步任务（时间单位）
     */
    fun runAsyncRepeating(delay: Long, delayUnit: TimeUnit, period: Long, periodUnit: TimeUnit, task: () -> Unit, managed: Boolean = true): ITaskHandler<Unit> {
        checkNotClosed()
        val handler = scheduler.runAsyncRepeating(delay, delayUnit, period, periodUnit) { handler -> task() }
        if (managed) activeTasks.add(handler)
        return handler
    }

    // ==================== 资源管理方法 ====================

    /**
     * 取消任务
     */
    fun cancelTask(task: ITaskHandler<*>) {
        task.close()
        activeTasks.remove(task)
    }

    /**
     * 取消协程
     */
    fun cancelJob(job: Job) {
        job.cancel()
        activeJobs.remove(job)
    }

    /**
     * 取消所有任务和协程
     */
    fun cancelAll() {
        // 取消所有任务
        activeTasks.forEach { task ->
            try {
                task.close()
            } catch (e: Exception) {
                manager.logger.logError(GuiLogger.ErrorType.SCHEDULER, "取消任务时发生异常", e)
            }
        }
        activeTasks.clear()

        // 取消所有协程
        activeJobs.forEach { job ->
            try {
                job.cancel()
            } catch (e: Exception) {
                manager.logger.logError(GuiLogger.ErrorType.SCHEDULER, "取消协程时发生异常", e)
            }
        }
        activeJobs.clear()
    }

    /**
     * 获取活跃任务数量
     */
    fun getActiveTaskCount(): Int = activeTasks.size

    /**
     * 获取活跃协程数量
     */
    fun getActiveJobCount(): Int = activeJobs.size

    // ==================== 内部方法 ====================

    /**
     * 检查调度器是否已关闭
     */
    private fun checkNotClosed() {
        if (closed) {
            throw IllegalStateException("GuiScheduler已关闭，无法执行新任务")
        }
    }

    // TerminableConsumer 实现
    override fun <T : AutoCloseable> bind(terminable: T): T {
        return terminableRegistry.bind(terminable)
    }

    /**
     * 关闭调度器，取消所有任务和协程
     */
    override fun close() {
        if (closed) return

        cancelAll()

        // 清理所有资源
        terminableRegistry.closeAndReportException()

        closed = true

        manager.logger.logDebug("GuiScheduler已关闭，取消了${activeTasks.size}个任务和${activeJobs.size}个协程")
    }

    /**
     * 检查调度器是否已关闭
     */
    override fun isClosed(): Boolean = closed
}
