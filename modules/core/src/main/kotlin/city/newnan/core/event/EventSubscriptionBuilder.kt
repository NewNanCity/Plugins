package city.newnan.core.event

import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.plugin.Plugin
import java.util.function.Consumer
import java.util.function.Predicate

/**
 * 事件订阅构建器
 *
 * 提供链式API来配置事件监听器的各种属性。
 */
class EventSubscriptionBuilder<T : Event>(
    private val eventClass: Class<T>,
    private val priority: EventPriority
) {
    private val filters = mutableListOf<Predicate<T>>()
    private var expiryCondition: Predicate<EventSubscription<T>>? = null
    private var exceptionHandler: ((T, Throwable) -> Unit)? = null

    /**
     * 添加事件过滤器
     *
     * @param predicate 过滤条件，返回true表示处理该事件
     * @return 构建器实例
     */
    fun filter(predicate: Predicate<T>): EventSubscriptionBuilder<T> {
        filters.add(predicate)
        return this
    }

    /**
     * 添加事件过滤器（Kotlin lambda语法）
     *
     * @param predicate 过滤条件
     * @return 构建器实例
     */
    fun filter(predicate: (T) -> Boolean): EventSubscriptionBuilder<T> {
        return filter(Predicate { predicate(it) })
    }

    /**
     * 设置监听器在指定次数后自动过期
     *
     * @param maxCalls 最大调用次数
     * @return 构建器实例
     */
    fun expireAfter(maxCalls: Int): EventSubscriptionBuilder<T> {
        require(maxCalls > 0) { "maxCalls must be positive" }
        expiryCondition = Predicate { it.callCount >= maxCalls }
        return this
    }

    /**
     * 设置监听器在指定时间后自动过期
     *
     * @param duration 持续时间（毫秒）
     * @return 构建器实例
     */
    fun expireAfter(duration: Long): EventSubscriptionBuilder<T> {
        require(duration > 0) { "duration must be positive" }
        val expireTime = System.currentTimeMillis() + duration
        expiryCondition = Predicate { System.currentTimeMillis() > expireTime }
        return this
    }

    /**
     * 设置自定义过期条件
     *
     * @param condition 过期条件
     * @return 构建器实例
     */
    fun expireIf(condition: Predicate<EventSubscription<T>>): EventSubscriptionBuilder<T> {
        expiryCondition = condition
        return this
    }

    /**
     * 设置异常处理器
     *
     * @param handler 异常处理器
     * @return 构建器实例
     */
    fun exceptionHandler(handler: (T, Throwable) -> Unit): EventSubscriptionBuilder<T> {
        exceptionHandler = handler
        return this
    }

    /**
     * 构建并注册事件监听器
     *
     * @param plugin 注册监听器的插件
     * @param handler 事件处理器
     * @return 事件订阅对象
     */
    fun handler(plugin: Plugin, handler: Consumer<T>): EventSubscription<T> {
        return handler(plugin) { handler.accept(it) }
    }

    /**
     * 构建并注册事件监听器（Kotlin lambda语法）
     *
     * @param plugin 注册监听器的插件
     * @param handler 事件处理器
     * @return 事件订阅对象
     */
    fun handler(plugin: Plugin, handler: (T) -> Unit): EventSubscription<T> {
        val subscription = EventSubscription(
            plugin = plugin,
            eventClass = eventClass,
            priority = priority,
            filters = filters.toList(),
            expiryCondition = expiryCondition,
            exceptionHandler = exceptionHandler,
            handler = handler
        )

        subscription.register()
        return subscription
    }
}
