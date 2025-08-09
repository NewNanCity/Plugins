package city.newnan.core.scheduler

import city.newnan.core.terminable.Terminable
import java.util.concurrent.TimeUnit
import java.util.function.BiFunction

/**
 * 面向用户的任务处理器接口
 * 
 * 提供任务状态查询、结果获取、链式调用等用户需要的功能
 * 类似于CompletableFuture的API设计
 */
interface ITaskHandler<T> : Terminable {

    val taskId: Int

    /**
     * 获取任务调度器
     */
    fun getScheduler(): IScheduler

    // ============= 基本状态查询 =============
    
    /**
     * 获取任务状态
     */
    fun getState(): TaskState

    /**
     * 任务是否在队列中等待执行
     */
    fun isPending(): Boolean = getState() == TaskState.PENDING

    /**
     * 任务是否正在运行
     */
    fun isRunning(): Boolean = getState() == TaskState.RUNNING
    
    /**
     * 任务是否已完成（成功或失败）
     */
    fun isCompleted(): Boolean = getState() == TaskState.SUCCESS || getState() == TaskState.FAILED
    
    /**
     * 任务是否被取消
     */
    fun isCancelled(): Boolean
    
    /**
     * 任务是否成功完成
     */
    fun isSuccessful(): Boolean = getState() == TaskState.SUCCESS

    /**
     * 任务是否失败
     */
    fun isFailed(): Boolean = getState() == TaskState.FAILED
    
    // ============= 结果获取 =============
    
    /**
     * 获取任务结果（阻塞等待）
     * 
     * ⚠️ **警告**: 此方法会阻塞当前线程，在主线程中使用可能导致服务器卡顿！
     * 建议在异步线程中使用，或者使用 `getNow()` 进行非阻塞获取
     * 
     * @return 任务结果，如果任务失败则抛出异常
     */
    fun get(): T?
    
    /**
     * 获取任务结果（带超时的阻塞等待）
     * 
     * ⚠️ **警告**: 此方法会阻塞当前线程，在主线程中使用可能导致服务器卡顿！
     * 建议在异步线程中使用，或者使用 `getNow()` 进行非阻塞获取
     * 
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 任务结果，超时则抛出异常
     */
    fun get(timeout: Long, unit: TimeUnit): T?
    
    /**
     * 立即获取任务结果，如果未完成则返回默认值
     * @param defaultValue 默认值
     * @return 任务结果或默认值
     */
    fun getNow(defaultValue: T?): T?
    
    // ============= 取消操作 =============
    
    /**
     * 取消任务
     * @param mayInterruptIfRunning 是否可以中断正在运行的任务
     * @return 是否成功取消
     */
    fun cancel(mayInterruptIfRunning: Boolean = false): Boolean
    
    // ============= 链式调用 =============
    
    /**
     * 任务完成后同步执行指定动作
     */
    fun <U> thenRunSync(function: (T, ITaskHandler<U>) -> U): ITaskHandler<U> =
        getScheduler().runSync<U>(listOf(this)) { handler ->
            function(getNow(null)!!, handler)
        }

    /**
     * 任务完成后应用函数转换结果
     */
    fun <U> thenRunAsync(function: (T, ITaskHandler<U>) -> U): ITaskHandler<U> =
        getScheduler().runAsync<U>(listOf(this)) { handler ->
            function(getNow(null)!!, handler)
        }

    /**
     * 任务完成后延迟同步执行指定动作（tick）
     */
    fun <U> thenRunSyncLater(delay: Long, function: (T, ITaskHandler<U>) -> U): ITaskHandler<U> =
        getScheduler().runSyncLater(delay, listOf(this)) { handler ->
            function(getNow(null)!!, handler)
        }

    /**
     * 任务完成后延迟异步执行指定动作（tick）
     */
    fun <U> thenRunAsyncLater(delay: Long, function: (T, ITaskHandler<U>) -> U): ITaskHandler<U> =
        getScheduler().runAsyncLater(delay, listOf(this)) { handler ->
            function(getNow(null)!!, handler)
        }

    /**
     * 任务完成后延迟同步执行指定动作（时间单位）
     */
    fun <U> thenRunSyncLater(delay: Long, unit: TimeUnit, function: (T, ITaskHandler<U>) -> U): ITaskHandler<U> =
        getScheduler().runSyncLater(delay, unit, listOf(this)) { handler ->
            function(getNow(null)!!, handler)
        }

    /**
     * 任务完成后延迟异步执行指定动作（时间单位）
     */
    fun <U> thenRunAsyncLater(delay: Long, unit: TimeUnit, function: (T, ITaskHandler<U>) -> U): ITaskHandler<U> =
        getScheduler().runAsyncLater(delay, unit, listOf(this)) { handler ->
            function(getNow(null)!!, handler)
        }

    /**
     * 任务完成后重复同步执行指定动作（tick）
     */
    fun thenRunSyncRepeating(delay: Long, period: Long, function: (T, ITaskHandler<Unit>) -> Unit): ITaskHandler<Unit> =
        getScheduler().runSyncRepeating(delay, period, listOf(this)) { handler ->
            function(getNow(null)!!, handler)
        }

    /**
     * 任务完成后重复异步执行指定动作（tick）
     */
    fun thenRunAsyncRepeating(delay: Long, period: Long, function: (T, ITaskHandler<Unit>) -> Unit): ITaskHandler<Unit> =
        getScheduler().runAsyncRepeating(delay, period, listOf(this)) { handler ->
            function(getNow(null)!!, handler)
        }

    /**
     * 任务完成后重复同步执行指定动作（时间单位）
     */
    fun thenRunSyncRepeating(delay: Long, delayUnit: TimeUnit, period: Long, periodUnit: TimeUnit, function: (T, ITaskHandler<Unit>) -> Unit): ITaskHandler<Unit> =
        getScheduler().runSyncRepeating(delay, delayUnit, period, periodUnit, listOf(this)) { handler ->
            function(getNow(null)!!, handler)
        }

    /**
     * 任务完成后重复异步执行指定动作（时间单位）
     */
    fun thenRunAsyncRepeating(delay: Long, delayUnit: TimeUnit, period: Long, periodUnit: TimeUnit, function: (T, ITaskHandler<Unit>) -> Unit): ITaskHandler<Unit> =
        getScheduler().runAsyncRepeating(delay, delayUnit, period, periodUnit, listOf(this)) { handler ->
            function(getNow(null)!!, handler)
        }

    /**
     * 获取所有前置依赖
     */
    fun getDependencies(): List<ITaskHandler<*>>

    // ============= 别名调用 =============

    fun thenRunSync(function: (T) -> Unit): ITaskHandler<Unit> =
        thenRunSync<Unit> { v, _ -> function(v) }

    fun thenRunAsync(function: (T) -> Unit): ITaskHandler<Unit> =
        thenRunAsync<Unit> { v, _ -> function(v) }

    fun thenRunSyncLater(delay: Long, function: (T) -> Unit): ITaskHandler<Unit> =
        thenRunSyncLater<Unit>(delay) { v, _ -> function(v) }

    fun thenRunAsyncLater(delay: Long, function: (T) -> Unit): ITaskHandler<Unit> =
        thenRunAsyncLater<Unit>(delay) { v, _ -> function(v) }

    fun thenRunSyncLater(delay: Long, unit: TimeUnit, function: (T) -> Unit): ITaskHandler<Unit> =
        thenRunSyncLater<Unit>(delay, unit) { v, _ -> function(v) }

    fun thenRunAsyncLater(delay: Long, unit: TimeUnit, function: (T) -> Unit): ITaskHandler<Unit> =
        thenRunAsyncLater<Unit>(delay, unit) { v, _ -> function(v) }

    fun thenRunSyncRepeating(delay: Long, period: Long, function: (T) -> Unit): ITaskHandler<Unit> =
        thenRunSyncRepeating(delay, period) { v, _ -> function(v) }

    fun thenRunAsyncRepeating(delay: Long, period: Long, function: (T) -> Unit): ITaskHandler<Unit> =
        thenRunAsyncRepeating(delay, period) { v, _ -> function(v) }

    fun thenRunSyncRepeating(delay: Long, delayUnit: TimeUnit, period: Long, periodUnit: TimeUnit, function: (T) -> Unit): ITaskHandler<Unit> =
        thenRunSyncRepeating(delay, delayUnit, period, periodUnit) { v, _ -> function(v) }

    fun thenRunAsyncRepeating(delay: Long, delayUnit: TimeUnit, period: Long, periodUnit: TimeUnit, function: (T) -> Unit): ITaskHandler<Unit> =
        thenRunAsyncRepeating(delay, delayUnit, period, periodUnit) { v, _ -> function(v) }

    // ============= JAVA兼容 =============

    fun <U> thenRunSync(function: BiFunction<T, ITaskHandler<U>, U>) =
        thenRunSync<U> { v, h -> function.apply(v, h) }

    fun <U> thenRunAsync(function: BiFunction<T, ITaskHandler<U>, U>) =
        thenRunAsync<U> { v, h -> function.apply(v, h) }

    fun <U> thenRunSyncLater(delay: Long, function: BiFunction<T, ITaskHandler<U>, U>) =
        thenRunSyncLater<U>(delay) { v, h -> function.apply(v, h) }

    fun <U> thenRunAsyncLater(delay: Long, function: BiFunction<T, ITaskHandler<U>, U>) =
        thenRunAsyncLater<U>(delay) { v, h -> function.apply(v, h) }

    fun <U> thenRunSyncLater(delay: Long, unit: TimeUnit, function: BiFunction<T, ITaskHandler<U>, U>) =
        thenRunSyncLater<U>(delay, unit) { v, h -> function.apply(v, h) }

    fun <U> thenRunAsyncLater(delay: Long, unit: TimeUnit, function: BiFunction<T, ITaskHandler<U>, U>) =
        thenRunAsyncLater<U>(delay, unit) { v, h -> function.apply(v, h) }
}