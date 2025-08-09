package city.newnan.core.terminable

import org.bukkit.scheduler.BukkitTask

/**
 * Terminable相关的扩展函数
 *
 * 提供便利的扩展函数来简化Terminable的使用
 */

// TerminableConsumer扩展函数

/**
 * 将当前Terminable绑定到指定的TerminableConsumer
 */
fun <T : Terminable> T.bindWith(consumer: TerminableConsumer): T {
    consumer.bind(this)
    return this
}

/**
 * 绑定一个BukkitTask为可终止任务
 */
fun TerminableConsumer.bindTask(task: BukkitTask): BukkitTask {
    bind(TerminableTask(task))
    return task
}

// 便利函数

/**
 * 创建一个简单的Terminable，执行指定的关闭操作
 */
fun terminable(closeAction: () -> Unit): Terminable = object : Terminable {
    override fun close() = closeAction()
}

/**
 * 创建一个带状态检查的Terminable
 */
fun terminable(closeAction: () -> Unit, isClosedCheck: () -> Boolean): Terminable = object : Terminable {
    override fun close() = closeAction()
    override fun isClosed(): Boolean = isClosedCheck()
}
