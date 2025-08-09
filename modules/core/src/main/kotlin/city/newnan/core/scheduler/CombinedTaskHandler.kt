package city.newnan.core.scheduler

import java.util.concurrent.TimeUnit

enum class CombindMode {
    ALL, ANY
}

internal class CombinedTaskHandler(
    override val taskId: Int,
    private val scheduler: BukkitScheduler,
    private val mode: CombindMode,
    private val handlers: List<ITaskHandler<*>>
) : IInternalTaskHandler<Unit> {

    private var _internalCancelled = false
    private var _internalFinished = false

    override fun getScheduler(): IScheduler = scheduler

    override fun getState(): TaskState {
        if (_internalCancelled) return TaskState.FAILED
        val state = when (mode) {
            CombindMode.ALL -> {
                if (handlers.any { !it.isCompleted() }) TaskState.PENDING
                else if (handlers.any { it.isFailed() }) TaskState.FAILED
                else TaskState.SUCCESS
            }
            CombindMode.ANY -> {
                if (handlers.any { it.isSuccessful() }) TaskState.SUCCESS
                else if (handlers.all { it.isCompleted() }) TaskState.FAILED
                else TaskState.PENDING
            }
        }
        if (state == TaskState.SUCCESS && !_internalFinished) {
            return TaskState.RUNNING
        }
        return state
    }

    override fun isCancelled(): Boolean {
        if (_internalCancelled) return true
        return when (mode) {
            CombindMode.ALL -> handlers.all { it.isCancelled() }
            CombindMode.ANY -> handlers.any { it.isCancelled() }
        }
    }

    override fun get() = null

    override fun get(timeout: Long, unit: TimeUnit) = null

    override fun getNow(defaultValue: Unit?) = null

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        _internalCancelled = true
        return true
    }

    override fun getDependencies(): List<ITaskHandler<*>> = handlers

    override fun close() {
        cancel(true)
    }

    override fun start(forced: Boolean) {
        if (!forced && getState() != TaskState.RUNNING) return
        scheduler.onTaskCompleted(taskId, this)
        _internalFinished = true
    }
}