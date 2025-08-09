package city.newnan.core.base

import city.newnan.core.logging.ILogger
import city.newnan.core.message.IMessager
import city.newnan.core.terminable.CompositeTerminable
import city.newnan.core.terminable.Terminable
import city.newnan.core.terminable.TerminableConsumer
import city.newnan.core.scheduler.BukkitScheduler
import org.bukkit.plugin.Plugin
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 模块初始化异常
 */
class ModuleInitializationException(message: String, cause: Throwable) : RuntimeException(message, cause)

abstract class BaseModule(
    val moduleName: String,
    val bukkitPlugin: Plugin,
    val logger: ILogger,
    val messager: IMessager
) : Terminable, TerminableConsumer {

    // ================== 别名构造器 ==================

    constructor(moduleName: String, parentModule: BaseModule): this(
        moduleName,
        parentModule.bukkitPlugin,
        parentModule.logger,
        parentModule.messager
    ) { parentModule.addChild(this) }

    constructor(moduleName: String, plugin: BasePlugin): this(
        moduleName,
        plugin,
        plugin.logger,
        plugin.messager
    ) { plugin.addChild(this) }

    // ================== 核心属性 ==================

    @Volatile
    private var initialized = false

    // 资源管理器
    val terminableRegistry: CompositeTerminable = CompositeTerminable.create()

    // 调度器实例
    val scheduler: BukkitScheduler by lazy { BukkitScheduler(bukkitPlugin).also { bind(it) } }

    // 子模块
    private val childModules = ConcurrentHashMap<Class<out BaseModule>, CopyOnWriteArrayList<BaseModule>>()

    // ================== 核心方法 ==================

    fun init() {
        if (initialized) return
        try {
            onReload()
            onInit()
        } catch (e: Exception) {
            throw ModuleInitializationException("Failed to initialize module: $moduleName", e)
        }
        initialized = true
    }

    override fun close() {
        if (!initialized) return
        initialized = false
        try {
            onClose()
        } catch (e: Exception) {
            logger.error("Error closing module: $moduleName", e)
        }
        terminableRegistry.close() // 包括了子模块的关闭
        childModules.clear()
    }

    override fun isClosed(): Boolean = !initialized

    override fun <T : AutoCloseable> bind(terminable: T): T {
        return terminableRegistry.bind(terminable)
    }

    fun reload() {
        if (!initialized) return
        try {
            onReload()
        } catch (e: Exception) {
            logger.error("Error reloading module: $moduleName", e)
        }
        childModules.forEach { instances ->
            instances.value.forEach { instance ->
                try {
                    instance.reload()
                } catch (e: Exception) {
                    logger.error("Error reloading child module: ${instance.moduleName}", e)
                }
            }
        }
    }

    // ================== 子模块管理 ==================

    fun hasChildren(clazz: Class<out BaseModule>): Boolean {
        return childModules.containsKey(clazz)
    }

    fun hasChild(child: BaseModule): Boolean {
        val instances = childModules[child::class.java] ?: return false
        return instances.contains(child)
    }

    fun <T : BaseModule> addChild(child: T): T {
        if (hasChild(child)) return child
        childModules.computeIfAbsent(child::class.java) { CopyOnWriteArrayList() }.add(child)
        bind(child)
        return child
    }

    fun <T : BaseModule> getChildren(clazz: Class<T>): List<T> {
        @Suppress("UNCHECKED_CAST")
        return (childModules[clazz]?.toList() ?: emptyList()) as List<T>
    }

    fun <T : BaseModule> getFirstChildOrNull(clazz: Class<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return childModules[clazz]?.firstOrNull() as T?
    }

    // ================== 生命周期 ==================

    open fun onInit() {}

    open fun onReload() {}

    open fun onClose() {}
}