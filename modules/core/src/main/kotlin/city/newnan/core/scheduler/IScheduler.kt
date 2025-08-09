package city.newnan.core.scheduler

import city.newnan.core.terminable.Terminable
import org.bukkit.plugin.Plugin
import java.util.concurrent.TimeUnit
import java.util.function.Function

/**
 * 调度器接口
 *
 * 提供任务调度功能，使用新的TaskHandler设计
 *
 * @author NewNanCity
 * @since 2.0.0
 */
interface IScheduler : Terminable {

    /**
     * 插件实例
     */
    val plugin: Plugin

    /**
     * 组合多个任务句柄为一个新的任务句柄
     *
     * @param mode 组合模式，ALL表示所有任务都成功才算成功，ANY表示任意一个任务成功就算成功
     * @param handlers 要组合的任务句柄列表
     * @return 组合后的任务句柄
     */
    fun combinedTaskHandlers(mode: CombindMode, vararg handlers: ITaskHandler<*>): ITaskHandler<Unit>

    // ==================== 基础任务创建 ====================

    /**
     * 运行同步任务
     * @param dependencies 依赖任务列表
     * @param function 任务执行器，接收当前任务处理器
     * @return 任务处理器
     */
    fun <T> runSync(dependencies: Collection<ITaskHandler<*>>? = null, function: (ITaskHandler<T>) -> T): ITaskHandler<T>

    /**
     * 运行异步任务
     * @param dependencies 依赖任务列表
     * @param function 任务执行器，接收当前任务处理器
     * @return 任务处理器
     */
    fun <T> runAsync(dependencies: Collection<ITaskHandler<*>>? = null, function: (ITaskHandler<T>) -> T): ITaskHandler<T>

    // ==================== 基础任务重载（无handler接收） ====================

    /**
     * 运行同步任务（无handler接收）
     * @param dependencies 依赖任务列表
     * @param function 任务执行器
     * @return 任务处理器
     */
    fun <T> runSync(dependencies: Collection<ITaskHandler<*>>? = null, function: () -> T) =
        runSync(dependencies) { _ -> function() }

    /**
     * 运行异步任务（无handler接收）
     * @param dependencies 依赖任务列表
     * @param function 任务执行器
     * @return 任务处理器
     */
    fun <T> runAsync(dependencies: Collection<ITaskHandler<*>>? = null, function: () -> T): ITaskHandler<T> =
        runAsync(dependencies) { _ -> function() }

    // ==================== 延迟任务 ====================

    /**
     * 延迟运行同步任务（以tick为单位）
     * @param delay 延迟时间（tick）
     * @param dependencies 依赖任务列表
     * @param function 任务执行器，接收当前任务处理器
     * @return 任务处理器
     */
    fun <T> runSyncLater(delay: Long, dependencies: Collection<ITaskHandler<*>>? = null, function: (ITaskHandler<T>) -> T): ITaskHandler<T>

    /**
     * 延迟运行异步任务（以tick为单位）
     * @param delay 延迟时间（tick）
     * @param dependencies 依赖任务列表
     * @param function 任务执行器，接收当前任务处理器
     * @return 任务处理器
     */
    fun <T> runAsyncLater(delay: Long, dependencies: Collection<ITaskHandler<*>>? = null, function: (ITaskHandler<T>) -> T): ITaskHandler<T>

    /**
     * 延迟运行同步任务（以时间单位为单位）
     * @param delay 延迟时间
     * @param unit 时间单位
     * @param dependencies 依赖任务列表
     * @param function 任务执行器，接收当前任务处理器
     * @return 任务处理器
     */
    fun <T> runSyncLater(delay: Long, unit: TimeUnit, dependencies: Collection<ITaskHandler<*>>? = null, function: (ITaskHandler<T>) -> T): ITaskHandler<T>

    /**
     * 延迟运行异步任务（以时间单位为单位）
     * @param delay 延迟时间
     * @param unit 时间单位
     * @param dependencies 依赖任务列表
     * @param function 任务执行器，接收当前任务处理器
     * @return 任务处理器
     */
    fun <T> runAsyncLater(delay: Long, unit: TimeUnit, dependencies: Collection<ITaskHandler<*>>? = null, function: (ITaskHandler<T>) -> T): ITaskHandler<T>

    // ==================== 延迟任务重载（无handler接收） ====================

    /**
     * 延迟运行同步任务（无handler接收）
     * @param delay 延迟时间（tick）
     * @param dependencies 依赖任务列表
     * @param function 任务执行器
     * @return 任务处理器
     */
    fun <T> runSyncLater(delay: Long, dependencies: Collection<ITaskHandler<*>>? = null, function: () -> T) =
        runSyncLater(delay, dependencies) { _ -> function() }

    /**
     * 延迟运行异步任务（无handler接收）
     * @param delay 延迟时间（tick）
     * @param dependencies 依赖任务列表
     * @param function 任务执行器
     * @return 任务处理器
     */
    fun <T> runAsyncLater(delay: Long, dependencies: Collection<ITaskHandler<*>>? = null, function: () -> T) =
        runAsyncLater(delay, dependencies) { _ -> function() }

    /**
     * 延迟运行同步任务（无handler接收，时间单位）
     * @param delay 延迟时间
     * @param unit 时间单位
     * @param dependencies 依赖任务列表
     * @param function 任务执行器
     * @return 任务处理器
     */
    fun <T> runSyncLater(delay: Long, unit: TimeUnit, dependencies: Collection<ITaskHandler<*>>? = null, function: () -> T) =
        runSyncLater(delay, unit, dependencies) { _ -> function() }

    /**
     * 延迟运行异步任务（无handler接收，时间单位）
     * @param delay 延迟时间
     * @param unit 时间单位
     * @param dependencies 依赖任务列表
     * @param function 任务执行器
     * @return 任务处理器
     */
    fun <T> runAsyncLater(delay: Long, unit: TimeUnit, dependencies: Collection<ITaskHandler<*>>? = null, function: () -> T) =
        runAsyncLater(delay, unit, dependencies) { _ -> function() }

    // ==================== 重复任务 ====================

    /**
     * 运行重复同步任务（以tick为单位）
     * @param delay 初始延迟（tick）
     * @param period 重复间隔（tick）
     * @param dependencies 依赖任务列表
     * @param function 任务执行器，接收当前任务处理器
     * @return 任务处理器
     */
    fun runSyncRepeating(delay: Long, period: Long, dependencies: Collection<ITaskHandler<*>>? = null, function: (ITaskHandler<Unit>) -> Unit): ITaskHandler<Unit>

    /**
     * 运行重复异步任务（以tick为单位）
     * @param delay 初始延迟（tick）
     * @param period 重复间隔（tick）
     * @param dependencies 依赖任务列表
     * @param function 任务执行器，接收当前任务处理器
     * @return 任务处理器
     */
    fun runAsyncRepeating(delay: Long, period: Long, dependencies: Collection<ITaskHandler<*>>? = null, function: (ITaskHandler<Unit>) -> Unit): ITaskHandler<Unit>

    /**
     * 运行重复同步任务（以时间单位为单位）
     * @param delay 初始延迟
     * @param delayUnit 延迟时间单位
     * @param period 重复间隔
     * @param periodUnit 间隔时间单位
     * @param dependencies 依赖任务列表
     * @param function 任务执行器，接收当前任务处理器
     * @return 任务处理器
     */
    fun runSyncRepeating(delay: Long, delayUnit: TimeUnit, period: Long, periodUnit: TimeUnit, dependencies: Collection<ITaskHandler<*>>? = null, function: (ITaskHandler<Unit>) -> Unit): ITaskHandler<Unit>

    /**
     * 运行重复异步任务（以时间单位为单位）
     * @param delay 初始延迟
     * @param delayUnit 延迟时间单位
     * @param period 重复间隔
     * @param periodUnit 间隔时间单位
     * @param dependencies 依赖任务列表
     * @param function 任务执行器，接收当前任务处理器
     * @return 任务处理器
     */
    fun runAsyncRepeating(delay: Long, delayUnit: TimeUnit, period: Long, periodUnit: TimeUnit, dependencies: Collection<ITaskHandler<*>>? = null, function: (ITaskHandler<Unit>) -> Unit): ITaskHandler<Unit>

    // ==================== 重复任务别名（无handler接收） ====================

    /**
     * 运行重复同步任务（无handler接收）
     * @param delay 初始延迟（tick）
     * @param period 重复间隔（tick）
     * @param dependencies 依赖任务列表
     * @param function 任务执行器
     * @return 任务处理器
     */
    fun runSyncRepeating(delay: Long, period: Long, dependencies: Collection<ITaskHandler<*>>? = null, function: () -> Unit): ITaskHandler<Unit> =
        runSyncRepeating(delay, period, dependencies) { _ -> function() }

    /**
     * 运行重复异步任务（无handler接收）
     * @param delay 初始延迟（tick）
     * @param period 重复间隔（tick）
     * @param dependencies 依赖任务列表
     * @param function 任务执行器
     * @return 任务处理器
     */
    fun runAsyncRepeating(delay: Long, period: Long, dependencies: Collection<ITaskHandler<*>>? = null, function: () -> Unit): ITaskHandler<Unit> =
        runAsyncRepeating(delay, period, dependencies) { _ -> function() }

    /**
     * 运行重复同步任务（无handler接收，时间单位）
     * @param delay 初始延迟
     * @param delayUnit 延迟时间单位
     * @param period 重复间隔
     * @param periodUnit 间隔时间单位
     * @param dependencies 依赖任务列表
     * @param function 任务执行器
     * @return 任务处理器
     */
    fun runSyncRepeating(delay: Long, delayUnit: TimeUnit, period: Long, periodUnit: TimeUnit, dependencies: Collection<ITaskHandler<*>>? = null, function: () -> Unit): ITaskHandler<Unit> =
        runSyncRepeating(delay, delayUnit, period, periodUnit, dependencies) { _ -> function() }

    /**
     * 运行重复异步任务（无handler接收，时间单位）
     * @param delay 初始延迟
     * @param delayUnit 延迟时间单位
     * @param period 重复间隔
     * @param periodUnit 间隔时间单位
     * @param dependencies 依赖任务列表
     * @param function 任务执行器
     * @return 任务处理器
     */
    fun runAsyncRepeating(delay: Long, delayUnit: TimeUnit, period: Long, periodUnit: TimeUnit, dependencies: Collection<ITaskHandler<*>>? = null, function: () -> Unit): ITaskHandler<Unit> =
        runAsyncRepeating(delay, delayUnit, period, periodUnit, dependencies) { _ -> function() }

    // ==================== Java兼容方法 ====================

    /**
     * 运行同步任务（Java兼容）
     */
    fun <T> runSync(dependencies: Collection<ITaskHandler<*>>? = null, function: Function<ITaskHandler<T>, T>): ITaskHandler<T> =
        runSync(dependencies) { handler -> function.apply(handler) }

    /**
     * 运行异步任务（Java兼容）
     */
    fun <T> runAsync(dependencies: Collection<ITaskHandler<*>>? = null, function: Function<ITaskHandler<T>, T>): ITaskHandler<T> =
        runAsync(dependencies) { handler -> function.apply(handler) }

    /**
     * 延迟运行同步任务（Java兼容）
     */
    fun <T> runSyncLater(delay: Long, dependencies: Collection<ITaskHandler<*>>? = null, function: Function<ITaskHandler<T>, T>): ITaskHandler<T> =
        runSyncLater(delay, dependencies) { handler -> function.apply(handler) }

    /**
     * 延迟运行异步任务（Java兼容）
     */
    fun <T> runAsyncLater(delay: Long, dependencies: Collection<ITaskHandler<*>>? = null, function: Function<ITaskHandler<T>, T>): ITaskHandler<T> =
        runAsyncLater(delay, dependencies) { handler -> function.apply(handler) }

    /**
     * 延迟运行同步任务（Java兼容，时间单位）
     */
    fun <T> runSyncLater(delay: Long, unit: TimeUnit, dependencies: Collection<ITaskHandler<*>>? = null, function: Function<ITaskHandler<T>, T>): ITaskHandler<T> =
        runSyncLater(delay, unit, dependencies) { handler -> function.apply(handler) }

    /**
     * 延迟运行异步任务（Java兼容，时间单位）
     */
    fun <T> runAsyncLater(delay: Long, unit: TimeUnit, dependencies: Collection<ITaskHandler<*>>? = null, function: Function<ITaskHandler<T>, T>): ITaskHandler<T> =
        runAsyncLater(delay, unit, dependencies) { handler -> function.apply(handler) }

    // ==================== 统计 ====================

    /**
     * 获取活动任务数量
     */
    fun getActiveTaskCount(): Int
}