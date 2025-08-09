package city.newnan.core.base

import city.newnan.core.terminable.*
import city.newnan.core.logging.Logger
import city.newnan.core.logging.PerformanceMonitor
import city.newnan.core.message.MessageManager
import city.newnan.core.config.CorePluginConfig
import city.newnan.core.config.DefaultCorePluginConfig
import city.newnan.core.scheduler.BukkitScheduler
import city.newnan.core.utils.text.LanguageProvider
import city.newnan.core.utils.text.StringFormatter
import city.newnan.core.logging.createPerformanceMonitor
import city.newnan.core.logging.provider.BukkitConsoleLoggerProvider
import city.newnan.core.logging.provider.JsonlFileLoggerProvider
import city.newnan.core.logging.provider.LogFileLoggerProvider
import org.bukkit.Bukkit
import org.bukkit.event.Listener
import org.bukkit.plugin.ServicePriority
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 增强的插件基类
 * 基于helper库的设计思想，提供完整的生命周期管理和资源管理功能
 *
 * 主要特性：
 * - 自动资源管理（Terminable模式）
 * - 服务注册和发现
 * - 事件监听器自动注册
 * - 命令注册简化
 * - 异常处理和日志记录
 */
abstract class BasePlugin : JavaPlugin(), TerminableConsumer {

    // 资源管理器
    val terminableRegistry: CompositeTerminable = CompositeTerminable.create()

    // 字符串格式化器
    val stringFormatter: StringFormatter by lazy {
        StringFormatter()
    }

    // 日志记录器
    val logger: Logger by lazy {
        getCoreConfig().getLoggingConfig().let { config ->
            Logger(name, stringFormatter).apply {
                addProvider(BukkitConsoleLoggerProvider(
                    plugin = this@BasePlugin,
                    minimumLevel = config.logLevel
                ))
                if (config.fileLoggingEnabled) {
                    val logsDir = File(dataFolder, "logs")
                    addProvider(
                        when (config.logFileType) {
                            CorePluginConfig.LoggingConfig.LogFileType.TEXT -> LogFileLoggerProvider(
                                logDirectory = logsDir,
                                filePrefix = config.logFilePrefix,
                                minimumLevel = config.logLevel,
                                maxRetentionDays = config.logRetentionDays
                            )
                            CorePluginConfig.LoggingConfig.LogFileType.JSONL -> JsonlFileLoggerProvider(
                                logDirectory = logsDir,
                                filePrefix = config.logFilePrefix,
                                minimumLevel = config.logLevel,
                                maxRetentionDays = config.logRetentionDays
                            )
                        }
                    )
                }
            }.also { bind(it) }
        }
    }

    // 性能监控器
    val performanceMonitor: PerformanceMonitor by lazy {
        logger.createPerformanceMonitor()
    }

    // 消息管理器
    val messager: MessageManager by lazy {
        getCoreConfig().getMessageConfig().let { config ->
            MessageManager(
                bukkitLogger = super.logger,
                stringFormatter = stringFormatter,
                playerPrefix = config.playerPrefix,
                consolePrefix = config.consolePrefix,
                defaultComponentParseMode = config.defaultParser
            ).also { bind(it) }
        }
    }

    // 调度器实例
    val scheduler: BukkitScheduler by lazy { BukkitScheduler(this).also { bind(it) } }

    // 子模块
    private val childModules = ConcurrentHashMap<Class<out BaseModule>, CopyOnWriteArrayList<BaseModule>>()

    override fun onLoad() {
        try {
            onPluginLoad()
        } catch (e: Exception) {
            logger.error("Failed to load plugin ${getPluginName()} ${getPluginVersion()}", e)
            throw e
        }
    }

    override fun onEnable() {
        // 创建数据文件夹
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        try {
            // 使用性能监控来监控插件启用过程
            performanceMonitor.monitor("Plugin Enable Monitoring") {
                onPluginEnable()
            }
        } catch (e: Exception) {
            logger.error("Failed to enable plugin ${getPluginName()} ${getPluginVersion()}", e)
            server.pluginManager.disablePlugin(this)
            throw e
        }
    }

    override fun onDisable() {
        try {
            onPluginDisable()
        } catch (e: Exception) {
            logger.error("Error during plugin ${getPluginName()} ${getPluginVersion()} disable", e)
        } finally {
            // 清理所有资源（包括协程管理器和子模块）
            terminableRegistry.closeAndReportException()
            // 注销所有服务
            server.servicesManager.unregisterAll(this)
            // 取消所有任务
            Bukkit.getScheduler().cancelTasks(this)
        }
    }

    /**
     * 插件加载时调用（onLoad阶段）
     * 子类可重写此方法实现加载逻辑
     */
    protected open fun onPluginLoad() {}

    /**
     * 插件启用时调用
     * 子类应重写此方法实现具体的启用逻辑
     *
     * 注意：大部分可重载的功能应该在 reloadPlugin() 中实现，
     * onPluginEnable() 中应该调用 reloadPlugin() 来避免代码重复
     */
    protected abstract fun onPluginEnable()

    /**
     * 插件禁用时调用
     * 子类应重写此方法实现具体的禁用逻辑
     */
    protected abstract fun onPluginDisable()

    /**
     * 重载插件配置和状态
     *
     * 所有插件都必须实现此方法，包含以下标准功能：
     * - 缓存清空和内存释放
     * - 配置文件重新读取
     * - 语言文件重新注册
     * - 管理器状态重置
     * - 其他可重载的初始化逻辑
     * 注意：如果要覆盖父类的 reloadPlugin()，必须调用 super.reloadPlugin()，以便重载所有子模块!
     *
     * 建议在 onPluginEnable() 中调用此方法来避免代码重复
     */
    open fun reloadPlugin() {
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

    // TerminableConsumer 实现
    override fun <T : AutoCloseable> bind(terminable: T): T {
        return terminableRegistry.bind(terminable)
    }

    /**
     * 注册事件监听器
     * 监听器会在插件禁用时自动注销
     */
    fun <T : Listener> registerListener(listener: T): T {
        bind(TerminableListener(this, listener))
        return listener
    }

    /**
     * 提供服务到Bukkit服务管理器
     */
    fun <T : Any> provideService(
        clazz: Class<T>,
        instance: T,
        priority: ServicePriority = ServicePriority.Normal
    ): T {
        server.servicesManager.register(clazz, instance, this, priority)
        return instance
    }

    /**
     * 获取服务
     */
    fun <T : Any> getService(clazz: Class<T>): T? {
        return server.servicesManager.getRegistration(clazz)?.provider
    }

    /**
     * 检查插件是否存在
     */
    fun isPluginPresent(name: String): Boolean {
        return server.pluginManager.getPlugin(name) != null
    }

    /**
     * 获取核心配置
     * 子类可以重写此方法提供自定义配置
     */
    open fun getCoreConfig(): CorePluginConfig = DefaultCorePluginConfig()

    /**
     * 设置语言提供者
     */
    fun setLanguageProvider(languageProvider: LanguageProvider) {
        stringFormatter.setLanguageProvider(languageProvider)
    }

    protected fun getPluginName() = pluginMeta.name

    protected fun getPluginVersion() = pluginMeta.version

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

    // ========== 属性访问器 ==========

    /**
     * 获取消息管理器实例（Java兼容）
     * 注意：由于Kotlin属性访问器会自动生成getMessager()方法，
     * 这里使用不同的方法名避免冲突
     */
    @JvmName("getMessagerInstance")
    fun getMessager(): MessageManager = messager

    /**
     * 获取性能监控器实例（Java兼容）
     */
    @JvmName("getPerformanceMonitorInstance")
    fun getPerformanceMonitor(): PerformanceMonitor = performanceMonitor

    /**
     * 获取字符串格式化器实例（Java兼容）
     */
    @JvmName("getStringFormatterInstance")
    fun getStringFormatter(): StringFormatter = stringFormatter
}
