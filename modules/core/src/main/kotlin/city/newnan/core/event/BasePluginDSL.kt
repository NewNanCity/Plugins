package city.newnan.core.event

import city.newnan.core.base.BasePlugin
import org.bukkit.event.Event

/**
 * BasePlugin的事件DSL扩展函数
 */
inline fun <reified T : Event> BasePlugin.subscribeEvent(
    noinline block: EventDSLBuilder<T>.() -> Unit
): EventSubscription<T> {
    return subscribeEvent(T::class.java, block)
}

/**
 * BasePlugin的事件DSL扩展函数（Java兼容）
 */
fun <T : Event> BasePlugin.subscribeEvent(
    eventClass: Class<T>,
    block: EventDSLBuilder<T>.() -> Unit
): EventSubscription<T> {
    val builder = EventDSLBuilder(this, eventClass)
    builder.block()
    val subscription = builder.build()

    // 自动绑定到插件生命周期
    bind(subscription)

    return subscription
}