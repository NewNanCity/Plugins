package city.newnan.core.terminable

import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

/**
 * 可终止的事件监听器包装器
 * 
 * 将Bukkit事件监听器包装为Terminable，使其可以被CompositeTerminable管理。
 * 当关闭时，会自动注销监听器。
 * 
 * @param plugin 注册监听器的插件
 * @param listener 要包装的监听器
 */
class TerminableListener(
    private val plugin: Plugin,
    private val listener: Listener
) : Terminable {
    
    @Volatile
    private var registered = true
    
    init {
        // 注册监听器
        plugin.server.pluginManager.registerEvents(listener, plugin)
    }
    
    override fun close() {
        if (registered) {
            HandlerList.unregisterAll(listener)
            registered = false
        }
    }
    
    override fun isClosed(): Boolean = !registered
    
    /**
     * 获取包装的监听器
     */
    fun getListener(): Listener = listener
    
    /**
     * 获取注册插件
     */
    fun getPlugin(): Plugin = plugin
    
    companion object {
        /**
         * 创建并注册一个可终止的事件监听器
         * 
         * @param plugin 插件实例
         * @param listener 监听器实例
         * @return TerminableListener实例
         */
        fun register(plugin: Plugin, listener: Listener): TerminableListener {
            return TerminableListener(plugin, listener)
        }
        
        /**
         * 创建并注册一个可终止的事件监听器（使用lambda）
         * 
         * @param plugin 插件实例
         * @param listenerSetup 监听器设置函数
         * @return TerminableListener实例
         */
        inline fun register(plugin: Plugin, listenerSetup: () -> Listener): TerminableListener {
            val listener = listenerSetup()
            return register(plugin, listener)
        }
    }
}
