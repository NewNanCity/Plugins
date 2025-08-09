package city.newnan.core.event

import city.newnan.core.terminable.Terminable
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Predicate

/**
 * 事件订阅对象
 *
 * 表示一个已注册的事件监听器，实现了Terminable接口以支持生命周期管理。
 * 提供了调用计数、自动过期、异常处理等高级功能。
 */
class EventSubscription<T : Event>(
    private val plugin: Plugin,
    private val eventClass: Class<T>,
    private val priority: EventPriority,
    private val filters: List<Predicate<T>>,
    private val expiryCondition: Predicate<EventSubscription<T>>?,
    private val exceptionHandler: ((T, Throwable) -> Unit)?,
    private val handler: (T) -> Unit
) : Terminable {

    private val callCounter = AtomicInteger(0)
    private val closed = AtomicBoolean(false)
    private var listener: Listener? = null

    /**
     * 获取调用次数
     */
    val callCount: Int
        get() = callCounter.get()

    /**
     * 注册事件监听器
     */
    internal fun register() {
        if (closed.get()) {
            throw IllegalStateException("Subscription is already closed")
        }

        listener = object : Listener {}

        // 手动注册事件监听器，指定优先级
        plugin.server.pluginManager.registerEvent(
            eventClass,
            listener!!,
            priority,
            { _, event ->
                if (!eventClass.isInstance(event)) return@registerEvent
                @Suppress("UNCHECKED_CAST")
                handleEvent(event as T)
            },
            plugin
        )
    }

    /**
     * 处理事件
     */
    private fun handleEvent(event: T) {
        if (closed.get()) return

        // 检查过期条件
        expiryCondition?.let { condition ->
            if (condition.test(this)) {
                close()
                return
            }
        }

        // 应用过滤器
        for (filter in filters) {
            if (!filter.test(event)) {
                return
            }
        }

        // 执行处理器
        try {
            handler(event)
            callCounter.incrementAndGet()
        } catch (e: Throwable) {
            if (exceptionHandler != null) {
                try {
                    exceptionHandler.invoke(event, e)
                } catch (handlerException: Throwable) {
                    plugin.logger.severe("Exception in event exception handler: ${handlerException.message}")
                    handlerException.printStackTrace()
                }
            } else {
                plugin.logger.severe("Unhandled exception in event handler: ${e.message}")
                e.printStackTrace()
            }
        }

        // 再次检查过期条件（在处理后）
        expiryCondition?.let { condition ->
            if (condition.test(this)) {
                close()
            }
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            listener?.let { HandlerList.unregisterAll(it) }
            listener = null
        }
    }

    override fun isClosed(): Boolean = closed.get()

    /**
     * 获取事件类型
     */
    fun getEventClass(): Class<T> = eventClass

    /**
     * 获取事件优先级
     */
    fun getPriority(): EventPriority = priority

    /**
     * 获取注册的插件
     */
    fun getPlugin(): Plugin = plugin

    /**
     * 检查是否仍然活跃（未关闭且未过期）
     */
    fun isActive(): Boolean {
        if (closed.get()) return false

        expiryCondition?.let { condition ->
            if (condition.test(this)) {
                close()
                return false
            }
        }

        return true
    }
}
