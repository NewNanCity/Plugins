package city.newnan.core.event

import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.plugin.Plugin
import java.util.function.Predicate

/**
 * 事件DSL构建器
 *
 * 提供Kotlin DSL风格的事件订阅API，基于工厂函数实现。
 * 这是推荐的事件处理方式，提供更简洁和类型安全的API。
 *
 * @author NewNanCity
 * @since 1.0.0
 */
class EventDSLBuilder<T : Event>(
    private val plugin: Plugin,
    private val eventClass: Class<T>
) {
    private var priority: EventPriority = EventPriority.NORMAL
    private val filters = mutableListOf<Predicate<T>>()
    private var expiryCondition: Predicate<EventSubscription<T>>? = null
    private var exceptionHandler: ((T, Throwable) -> Unit)? = null
    private var eventHandler: ((T) -> Unit)? = null

    /**
     * 设置事件优先级
     */
    fun priority(priority: EventPriority) {
        this.priority = priority
    }

    /**
     * 添加事件过滤器
     */
    fun filter(filter: Predicate<T>) {
        filters.add(filter)
    }

    /**
     * 添加事件过滤器（Kotlin lambda）
     */
    fun filter(filter: (T) -> Boolean) {
        filters.add(Predicate { filter(it) })
    }

    /**
     * 设置过期条件（处理指定次数后自动注销）
     */
    fun expireAfter(count: Int) {
        var counter = 0
        expiryCondition = Predicate { ++counter >= count }
    }

    /**
     * 设置过期条件（自定义条件）
     */
    fun expireWhen(condition: Predicate<EventSubscription<T>>) {
        expiryCondition = condition
    }

    /**
     * 设置过期条件（Kotlin lambda）
     */
    fun expireWhen(condition: (EventSubscription<T>) -> Boolean) {
        expiryCondition = Predicate { condition(it) }
    }

    /**
     * 设置异常处理器
     */
    fun onException(handler: (T, Throwable) -> Unit) {
        exceptionHandler = handler
    }

    /**
     * 设置事件处理器
     */
    fun handler(handler: (T) -> Unit) {
        eventHandler = handler
    }

    /**
     * 构建并注册事件订阅
     */
    fun build(): EventSubscription<T> {
        val handler = eventHandler ?: throw IllegalStateException("事件处理器不能为空")

        // 使用工厂函数创建事件订阅
        val builder = EventSubscriptionBuilder(eventClass, priority)

        // 应用过滤器
        filters.forEach { filter ->
            builder.filter(filter)
        }

        // 应用过期条件
        expiryCondition?.let { condition ->
            builder.expireIf(condition)
        }

        // 应用异常处理器
        exceptionHandler?.let { handler ->
            builder.exceptionHandler(handler)
        }

        // 注册处理器
        return builder.handler(plugin, handler)
    }
}