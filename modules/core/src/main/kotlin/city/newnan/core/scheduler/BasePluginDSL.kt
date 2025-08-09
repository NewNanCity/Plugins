package city.newnan.core.scheduler

import city.newnan.core.base.BasePlugin
import java.util.concurrent.TimeUnit
import java.util.function.Function

/**
 * 任务DSL扩展函数
 *
 * 为BasePlugin提供简洁的任务调度API，基于新的ITaskHandler设计。
 * 保持原有的 BasePlugin.runSync { ... } 调用风格。
 *
 * @author NewNanCity
 * @since 2.0.0
 */

// ==================== 基础任务方法 ====================

/**
 * 组合多个任务句柄为一个新的任务句柄
 *
 * @param mode 组合模式，ALL表示所有任务都成功才算成功，ANY表示任意一个任务成功就算成功
 * @param handlers 要组合的任务句柄列表
 * @return 组合后的任务句柄
 */
fun BasePlugin.combinedTaskHandlers(mode: CombindMode, vararg handlers: ITaskHandler<*>): ITaskHandler<Unit> =
    scheduler.combinedTaskHandlers(mode, *handlers)

/**
 * 运行同步任务
 *
 * 在主线程中执行任务，适用于需要调用Bukkit API的操作。
 *
 * @param dependencies 依赖任务列表，当前任务会等待这些任务完成后再执行
 * @param function 任务执行器，接收当前任务句柄，返回任务结果
 * @return 任务句柄，可用于查询状态、获取结果或链式调用
 */
fun <T> BasePlugin.runSync(dependencies: Collection<ITaskHandler<*>>? = null, function: (ITaskHandler<T>) -> T): ITaskHandler<T> =
    scheduler.runSync(dependencies, function)

/**
 * 运行异步任务
 *
 * 在后台线程中执行任务，适用于耗时操作如文件IO、网络请求、数据库操作等。
 *
 * @param dependencies 依赖任务列表，当前任务会等待这些任务完成后再执行
 * @param function 任务执行器，接收当前任务句柄，返回任务结果
 * @return 任务句柄，可用于查询状态、获取结果或链式调用
 */
fun <T> BasePlugin.runAsync(dependencies: Collection<ITaskHandler<*>>? = null, function: (ITaskHandler<T>) -> T): ITaskHandler<T> =
    scheduler.runAsync(dependencies, function)

// ==================== 延迟任务方法（tick单位） ====================

/**
 * 延迟运行同步任务
 *
 * 在指定tick数后在主线程中执行任务。
 *
 * @param delay 延迟时间（tick，1 tick = 50ms）
 * @param dependencies 依赖任务列表
 * @param function 任务执行器，接收当前任务句柄，返回任务结果
 * @return 任务句柄
 */
fun <T> BasePlugin.runSyncLater(delay: Long, dependencies: Collection<ITaskHandler<*>>? = null, function: (ITaskHandler<T>) -> T): ITaskHandler<T> =
    scheduler.runSyncLater(delay, dependencies, function)

/**
 * 延迟运行异步任务
 *
 * 在指定tick数后在后台线程中执行任务。
 *
 * @param delay 延迟时间（tick，1 tick = 50ms）
 * @param dependencies 依赖任务列表
 * @param function 任务执行器，接收当前任务句柄，返回任务结果
 * @return 任务句柄
 */
fun <T> BasePlugin.runAsyncLater(delay: Long, dependencies: Collection<ITaskHandler<*>>? = null, function: (ITaskHandler<T>) -> T): ITaskHandler<T> =
    scheduler.runAsyncLater(delay, dependencies, function)

// ==================== 延迟任务方法（时间单位） ====================

/**
 * 延迟运行同步任务（时间单位）
 *
 * 在指定时间后在主线程中执行任务。
 *
 * @param delay 延迟时间
 * @param unit 时间单位
 * @param dependencies 依赖任务列表
 * @param function 任务执行器，接收当前任务句柄，返回任务结果
 * @return 任务句柄
 */
fun <T> BasePlugin.runSyncLater(delay: Long, unit: TimeUnit, dependencies: Collection<ITaskHandler<*>>? = null, function: (ITaskHandler<T>) -> T): ITaskHandler<T> =
    scheduler.runSyncLater(delay, unit, dependencies, function)

/**
 * 延迟运行异步任务（时间单位）
 *
 * 在指定时间后在后台线程中执行任务。
 *
 * @param delay 延迟时间
 * @param unit 时间单位
 * @param dependencies 依赖任务列表
 * @param function 任务执行器，接收当前任务句柄，返回任务结果
 * @return 任务句柄
 */
fun <T> BasePlugin.runAsyncLater(delay: Long, unit: TimeUnit, dependencies: Collection<ITaskHandler<*>>? = null, function: (ITaskHandler<T>) -> T): ITaskHandler<T> =
    scheduler.runAsyncLater(delay, unit, dependencies, function)

// ==================== 重复任务方法（tick单位） ====================

/**
 * 运行重复同步任务
 *
 * 在主线程中重复执行任务，适用于定时更新游戏状态、GUI等。
 *
 * @param delay 初始延迟时间（tick，1 tick = 50ms）
 * @param period 重复间隔时间（tick）
 * @param dependencies 依赖任务列表
 * @param function 任务执行器，接收当前任务句柄
 * @return 任务句柄，可用于取消重复任务
 */
fun BasePlugin.runSyncRepeating(delay: Long, period: Long, dependencies: Collection<ITaskHandler<*>>? = null, function: (ITaskHandler<Unit>) -> Unit): ITaskHandler<Unit> =
    scheduler.runSyncRepeating(delay, period, dependencies, function)

/**
 * 运行重复异步任务
 *
 * 在后台线程中重复执行任务，适用于定时保存数据、清理缓存等。
 *
 * @param delay 初始延迟时间（tick，1 tick = 50ms）
 * @param period 重复间隔时间（tick）
 * @param dependencies 依赖任务列表
 * @param function 任务执行器，接收当前任务句柄
 * @return 任务句柄，可用于取消重复任务
 */
fun BasePlugin.runAsyncRepeating(delay: Long, period: Long, dependencies: Collection<ITaskHandler<*>>? = null, function: (ITaskHandler<Unit>) -> Unit): ITaskHandler<Unit> =
    scheduler.runAsyncRepeating(delay, period, dependencies, function)

// ==================== 重复任务方法（时间单位） ====================

/**
 * 运行重复同步任务（时间单位）
 *
 * 在主线程中重复执行任务，支持自定义时间单位。
 *
 * @param delay 初始延迟时间
 * @param delayUnit 延迟时间单位
 * @param period 重复间隔时间
 * @param periodUnit 间隔时间单位
 * @param dependencies 依赖任务列表
 * @param function 任务执行器，接收当前任务句柄
 * @return 任务句柄
 */
fun BasePlugin.runSyncRepeating(delay: Long, delayUnit: TimeUnit, period: Long, periodUnit: TimeUnit, dependencies: Collection<ITaskHandler<*>>? = null, function: (ITaskHandler<Unit>) -> Unit): ITaskHandler<Unit> =
    scheduler.runSyncRepeating(delay, delayUnit, period, periodUnit, dependencies, function)

/**
 * 运行重复异步任务（时间单位）
 *
 * 在后台线程中重复执行任务，支持自定义时间单位。
 *
 * @param delay 初始延迟时间
 * @param delayUnit 延迟时间单位
 * @param period 重复间隔时间
 * @param periodUnit 间隔时间单位
 * @param dependencies 依赖任务列表
 * @param function 任务执行器，接收当前任务句柄
 * @return 任务句柄
 */
fun BasePlugin.runAsyncRepeating(delay: Long, delayUnit: TimeUnit, period: Long, periodUnit: TimeUnit, dependencies: Collection<ITaskHandler<*>>? = null, function: (ITaskHandler<Unit>) -> Unit): ITaskHandler<Unit> =
    scheduler.runAsyncRepeating(delay, delayUnit, period, periodUnit, dependencies, function)

// ==================== Java兼容函数 ====================

/**
 * 运行同步任务（Java兼容）
 *
 * 为Java代码提供兼容的调用方式，使用Runnable接口。
 *
 * @param dependencies 依赖任务列表
 * @param function 任务执行器
 * @return 任务句柄
 */
@JvmOverloads
fun <T> BasePlugin.runSyncJava(dependencies: Collection<ITaskHandler<*>>? = null, function: Function<ITaskHandler<T>, T>): ITaskHandler<T> =
    scheduler.runSync(dependencies) { handler -> function.apply(handler) }

/**
 * 运行异步任务（Java兼容）
 *
 * 为Java代码提供兼容的调用方式，使用Runnable接口。
 *
 * @param dependencies 依赖任务列表
 * @param function 任务执行器
 * @return 任务句柄
 */
@JvmOverloads
fun <T> BasePlugin.runAsyncJava(dependencies: Collection<ITaskHandler<*>>? = null, function: Function<ITaskHandler<T>, T>): ITaskHandler<T> =
    scheduler.runAsync(dependencies) { handler -> function.apply(handler) }

/**
 * 延迟运行同步任务（Java兼容）
 *
 * @param delay 延迟时间（tick）
 * @param dependencies 依赖任务列表
 * @param function 任务执行器
 * @return 任务句柄
 */
@JvmOverloads
fun <T> BasePlugin.runSyncLaterJava(delay: Long, dependencies: Collection<ITaskHandler<*>>? = null, function: Function<ITaskHandler<T>, T>): ITaskHandler<T> =
    scheduler.runSyncLater(delay, dependencies) { handler -> function.apply(handler) }

/**
 * 延迟运行异步任务（Java兼容）
 *
 * @param delay 延迟时间（tick）
 * @param dependencies 依赖任务列表
 * @param function 任务执行器
 * @return 任务句柄
 */
@JvmOverloads
fun <T> BasePlugin.runAsyncLaterJava(delay: Long, dependencies: Collection<ITaskHandler<*>>? = null, function: Function<ITaskHandler<T>, T>): ITaskHandler<T> =
    scheduler.runAsyncLater(delay, dependencies) { handler -> function.apply(handler) }

/**
 * 延迟运行同步任务（Java兼容，时间单位）
 *
 * @param delay 延迟时间
 * @param unit 时间单位
 * @param dependencies 依赖任务列表
 * @param function 任务执行器
 * @return 任务句柄
 */
@JvmOverloads
fun <T> BasePlugin.runSyncLaterJava(delay: Long, unit: TimeUnit, dependencies: Collection<ITaskHandler<*>>? = null, function: Function<ITaskHandler<T>, T>): ITaskHandler<T> =
    scheduler.runSyncLater(delay, unit, dependencies) { handler -> function.apply(handler) }

/**
 * 延迟运行异步任务（Java兼容，时间单位）
 *
 * @param delay 延迟时间
 * @param unit 时间单位
 * @param dependencies 依赖任务列表
 * @param function 任务执行器
 * @return 任务句柄
 */
@JvmOverloads
fun <T> BasePlugin.runAsyncLaterJava(delay: Long, unit: TimeUnit, dependencies: Collection<ITaskHandler<*>>? = null, function: Function<ITaskHandler<T>, T>): ITaskHandler<T> =
    scheduler.runAsyncLater(delay, unit, dependencies) { handler -> function.apply(handler) }

/**
 * 运行重复同步任务（Java兼容）
 *
 * @param delay 初始延迟时间（tick）
 * @param period 重复间隔时间（tick）
 * @param dependencies 依赖任务列表
 * @param function 任务执行器
 * @return 任务句柄
 */
@JvmOverloads
fun BasePlugin.runSyncRepeatingJava(delay: Long, period: Long, dependencies: Collection<ITaskHandler<*>>? = null, function: Function<ITaskHandler<Unit>, Unit>): ITaskHandler<Unit> =
    scheduler.runSyncRepeating(delay, period, dependencies) { handler -> function.apply(handler) }

/**
 * 运行重复异步任务（Java兼容）
 *
 * @param delay 初始延迟时间（tick）
 * @param period 重复间隔时间（tick）
 * @param dependencies 依赖任务列表
 * @param function 任务执行器
 * @return 任务句柄
 */
@JvmOverloads
fun BasePlugin.runAsyncRepeatingJava(delay: Long, period: Long, dependencies: Collection<ITaskHandler<*>>? = null, function: Function<ITaskHandler<Unit>, Unit>): ITaskHandler<Unit> =
    scheduler.runAsyncRepeating(delay, period, dependencies) { handler -> function.apply(handler) }

/**
 * 运行重复同步任务（Java兼容，时间单位）
 *
 * @param delay 初始延迟时间
 * @param delayUnit 延迟时间单位
 * @param period 重复间隔时间
 * @param periodUnit 间隔时间单位
 * @param dependencies 依赖任务列表
 * @param function 任务执行器
 * @return 任务句柄
 */
@JvmOverloads
fun BasePlugin.runSyncRepeatingJava(delay: Long, delayUnit: TimeUnit, period: Long, periodUnit: TimeUnit, dependencies: Collection<ITaskHandler<*>>? = null, function: Function<ITaskHandler<Unit>, Unit>): ITaskHandler<Unit> =
    scheduler.runSyncRepeating(delay, delayUnit, period, periodUnit, dependencies) { handler -> function.apply(handler) }

/**
 * 运行重复异步任务（Java兼容，时间单位）
 *
 * @param delay 初始延迟时间
 * @param delayUnit 延迟时间单位
 * @param period 重复间隔时间
 * @param periodUnit 间隔时间单位
 * @param dependencies 依赖任务列表
 * @param function 任务执行器
 * @return 任务句柄
 */
@JvmOverloads
fun BasePlugin.runAsyncRepeatingJava(delay: Long, delayUnit: TimeUnit, period: Long, periodUnit: TimeUnit, dependencies: Collection<ITaskHandler<*>>? = null, function: Function<ITaskHandler<Unit>, Unit>): ITaskHandler<Unit> =
    scheduler.runAsyncRepeating(delay, delayUnit, period, periodUnit, dependencies) { handler -> function.apply(handler) }