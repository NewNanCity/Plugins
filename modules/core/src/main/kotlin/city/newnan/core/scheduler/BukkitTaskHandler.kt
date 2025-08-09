package city.newnan.core.scheduler

import java.util.concurrent.TimeUnit
import java.util.concurrent.CompletableFuture

/**
 * Bukkit任务处理器适配器
 * 
 * 将新的ITaskHandler接口适配到现有的TaskHandler实现
 */
class BukkitTaskHandler<T>(
    override val taskId: Int,
    private val scheduler: BukkitScheduler,
    private val runner: (ITaskHandler<T>) -> T,
    private val bukkitTaskBuilder: (Runnable) -> org.bukkit.scheduler.BukkitTask,
    private val dependencies: List<ITaskHandler<*>>? = null
) : IInternalTaskHandler<T> {

    private val completableFuture = CompletableFuture<T>()
    private var bukkitTask: org.bukkit.scheduler.BukkitTask? = null
    private var state = TaskState.PENDING
    private var error : Throwable? = null

    // ============= 基本状态查询 =============

    override fun getState(): TaskState = state

    override fun isCancelled(): Boolean = completableFuture.isCancelled

    override fun getScheduler(): IScheduler = scheduler
    
    // ============= 结果获取 =============
    
    override fun get(): T? {
        return try {
            completableFuture.get()
        } catch (e: Exception) {
            throw RuntimeException("任务执行失败", e)
        }
    }
    
    override fun get(timeout: Long, unit: TimeUnit): T? {
        return try {
            completableFuture.get(timeout, unit)
        } catch (e: Exception) {
            throw RuntimeException("任务执行失败或超时", e)
        }
    }
    
    override fun getNow(defaultValue: T?): T? {
        return if (completableFuture.isDone && !completableFuture.isCompletedExceptionally) {
            try {
                completableFuture.get()
            } catch (_: Exception) {
                defaultValue
            }
        } else {
            defaultValue
        }
    }
    
    // ============= 取消操作 =============
    
    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        val cancelled = completableFuture.cancel(mayInterruptIfRunning)
        if (cancelled) {
            bukkitTask?.cancel()
            state = TaskState.FAILED
            scheduler.onTaskCompleted(taskId, this)
        }
        return cancelled
    }
    
    // ============= 链式调用 =============

    override fun getDependencies(): List<ITaskHandler<*>> =
        dependencies ?: emptyList()

    // ============= 内部执行控制 =============
    
    override fun start(forced: Boolean) {
        if (!forced) {
            if (state != TaskState.PENDING) return
            if (!dependencies.isNullOrEmpty() && dependencies.any { !it.isSuccessful() }) return
        }
        bukkitTask = bukkitTaskBuilder(Runnable {
            try {
                error = null
                state = TaskState.RUNNING
                val result = runner(this)
                if (completableFuture.complete(result)) {
                    state = TaskState.SUCCESS
                    error = null
                    scheduler.onTaskCompleted(taskId, this)
                }
            } catch (e: Throwable) {
                if (completableFuture.completeExceptionally(e)) {
                    error = e
                    state = TaskState.FAILED
                    scheduler.onTaskCompleted(taskId, this)
                }
            }
        })
    }
    
    // ============= Terminable =============
    
    override fun close() {
        cancel(true)
    }
    
    override fun isClosed(): Boolean {
        return isCompleted()
    }
}