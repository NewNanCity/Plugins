package city.newnan.gui.dsl

import city.newnan.core.scheduler.ITaskHandler
import city.newnan.gui.page.BasePage
import java.util.concurrent.TimeUnit

/**
 * GUI调度器DSL扩展
 *
 * 为Page和Component提供完整的调度器DSL方法，与core模块保持一致。
 * 包含基础调度器方法、时间单位支持、重复任务和协程支持。
 */

// ==================== BasePage调度器DSL扩展 ====================

/**
 * 运行同步任务
 */
fun <T> BasePage.runSync(managed: Boolean = true, task: () -> T): ITaskHandler<T> {
    return scheduler.runSync(task, managed)
}

/**
 * 运行异步任务
 */
fun <T> BasePage.runAsync(managed: Boolean = true, task: () -> T): ITaskHandler<T> {
    return scheduler.runAsync(task, managed)
}

/**
 * 延迟运行同步任务
 */
fun <T> BasePage.runSyncLater(delay: Long, managed: Boolean = true, task: () -> T): ITaskHandler<T> {
    return scheduler.runSyncLater(delay, task, managed)
}

/**
 * 延迟运行异步任务
 */
fun <T> BasePage.runAsyncLater(delay: Long, managed: Boolean = true, task: () -> T): ITaskHandler<T> {
    return scheduler.runAsyncLater(delay, task, managed)
}

/**
 * 延迟运行同步任务（时间单位）
 */
fun <T> BasePage.runSyncLater(delay: Long, unit: TimeUnit, managed: Boolean = true, task: () -> T): ITaskHandler<T> {
    return scheduler.runSyncLater(delay, unit, task, managed)
}

/**
 * 延迟运行异步任务（时间单位）
 */
fun <T> BasePage.runAsyncLater(delay: Long, unit: TimeUnit, managed: Boolean = true, task: () -> T): ITaskHandler<T> {
    return scheduler.runAsyncLater(delay, unit, task, managed)
}

/**
 * 重复运行同步任务
 */
fun BasePage.runSyncRepeating(delay: Long, period: Long, managed: Boolean = true, task: () -> Unit): ITaskHandler<Unit> {
    return scheduler.runSyncRepeating(delay, period, task, managed)
}

/**
 * 重复运行异步任务
 */
fun BasePage.runAsyncRepeating(delay: Long, period: Long, managed: Boolean = true, task: () -> Unit): ITaskHandler<Unit> {
    return scheduler.runAsyncRepeating(delay, period, task, managed)
}

/**
 * 重复运行同步任务（时间单位）
 */
fun BasePage.runSyncRepeating(delay: Long, delayUnit: TimeUnit, period: Long, periodUnit: TimeUnit, managed: Boolean = true, task: () -> Unit): ITaskHandler<Unit> {
    return scheduler.runSyncRepeating(delay, delayUnit, period, periodUnit, task, managed)
}

/**
 * 重复运行异步任务（时间单位）
 */
fun BasePage.runAsyncRepeating(delay: Long, delayUnit: TimeUnit, period: Long, periodUnit: TimeUnit, managed: Boolean = true, task: () -> Unit): ITaskHandler<Unit> {
    return scheduler.runAsyncRepeating(delay, delayUnit, period, periodUnit, task, managed)
}
