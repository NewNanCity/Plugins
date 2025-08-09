package city.newnan.config

import city.newnan.core.base.BasePlugin
import city.newnan.config.extensions.configManager

/**
 * 配置DSL构建器
 *
 * 提供Kotlin DSL风格的配置管理API，基于工厂函数实现。
 * 这是推荐的配置管理方式，提供更简洁和类型安全的API。
 *
 * @author NewNanCity
 * @since 1.0.0
 */
class ConfigDSLBuilder(internal val plugin: BasePlugin) {
    internal lateinit var configManager: ConfigManager

    /**
     * 初始化配置管理器
     */
    fun manager(block: ConfigManagerBuilder.() -> Unit = {}): ConfigManager {
        val builder = ConfigManagerBuilder()
        builder.block()

        // ConfigManager构造函数只接受plugin参数
        configManager = ConfigManager(plugin)

        // 绑定到插件生命周期
        plugin.bind(configManager)

        return configManager
    }

    /**
     * 获取配置文件
     */
    fun get(path: String): Config {
        if (!::configManager.isInitialized) {
            configManager = plugin.configManager
        }
        return configManager.get(path)
    }

    /**
     * 解析配置为数据类
     */
    inline fun <reified T : Any> parse(path: String): T {
        return getManager().parse<T>(path)
    }

    /**
     * 创建配置文件（使用touch方法）
     */
    fun create(path: String, format: String? = null): Config {
        if (!::configManager.isInitialized) {
            configManager = plugin.configManager
        }
        configManager.touch(path)
        return configManager.get(path, format)
    }

    /**
     * 检查配置文件是否存在
     */
    fun exists(path: String): Boolean {
        if (!::configManager.isInitialized) {
            configManager = plugin.configManager
        }
        val file = java.io.File(plugin.dataFolder, path)
        return file.exists()
    }

    /**
     * 删除配置文件
     */
    fun delete(path: String): Boolean {
        if (!::configManager.isInitialized) {
            configManager = plugin.configManager
        }
        configManager.remove(path)
        return true
    }

    /**
     * 获取配置管理器实例
     */
    fun getManager(): ConfigManager {
        if (!::configManager.isInitialized) {
            configManager = plugin.configManager
        }
        return configManager
    }
}

/**
 * 配置管理器构建器
 */
class ConfigManagerBuilder {
    private var useCache: Boolean = true
    private var cacheSize: Int = 100
    private var autoSave: Boolean = false
    private var autoSaveInterval: Long = 300000L // 5分钟
    private var createMissingFiles: Boolean = true
    private var createMissingDirectories: Boolean = true

    fun useCache(enabled: Boolean) { this.useCache = enabled }
    fun cacheSize(size: Int) { this.cacheSize = size }
    fun autoSave(enabled: Boolean) { this.autoSave = enabled }
    fun autoSaveInterval(interval: Long) { this.autoSaveInterval = interval }
    fun createMissingFiles(enabled: Boolean) { this.createMissingFiles = enabled }
    fun createMissingDirectories(enabled: Boolean) { this.createMissingDirectories = enabled }

    internal fun build(): ConfigManagerOptions {
        return ConfigManagerOptions(
            useCache = useCache,
            cacheSize = cacheSize,
            autoSave = autoSave,
            autoSaveInterval = autoSaveInterval,
            createMissingFiles = createMissingFiles,
            createMissingDirectories = createMissingDirectories
        )
    }
}

/**
 * 配置管理器选项
 */
data class ConfigManagerOptions(
    val useCache: Boolean = true,
    val cacheSize: Int = 100,
    val autoSave: Boolean = false,
    val autoSaveInterval: Long = 300000L,
    val createMissingFiles: Boolean = true,
    val createMissingDirectories: Boolean = true
)

/**
 * 配置文件DSL构建器
 */
class ConfigFileDSLBuilder(val config: Config) {

    /**
     * 设置配置值
     */
    fun set(path: String, value: Any?) {
        config.set(path, value)
    }

    /**
     * 获取配置值
     */
    inline fun <reified T : Any> get(path: String, defaultValue: T): T {
        return config.get(path, defaultValue)
    }

    /**
     * 获取配置值（可能为null）
     */
    inline fun <reified T> getOrNull(path: String): T? {
        return config.getOrNull<T>(path)
    }

    /**
     * 检查路径是否存在
     */
    fun has(path: String): Boolean {
        return config.has(path)
    }

    /**
     * 删除配置项
     */
    fun remove(path: String) {
        config.remove(path)
    }

    /**
     * 保存配置
     */
    fun save() {
        config.save()
    }

    /**
     * 重新加载配置
     */
    fun reload() {
        config.reload()
    }
}

/**
 * BasePlugin的配置DSL扩展函数
 */
fun BasePlugin.configs(block: ConfigDSLBuilder.() -> Unit): ConfigDSLBuilder {
    val builder = ConfigDSLBuilder(this)
    builder.block()
    return builder
}

/**
 * 简化的配置管理DSL
 */
fun BasePlugin.config(path: String, block: ConfigFileDSLBuilder.() -> Unit = {}): Config {
    val config = configManager.get(path)
    val builder = ConfigFileDSLBuilder(config)
    builder.block()

    return config
}

/**
 * 简化的配置解析DSL
 */
inline fun <reified T : Any> BasePlugin.configParse(path: String): T {
    return configManager.parse<T>(path)
}

/**
 * 配置文件操作DSL
 */
fun Config.configure(block: ConfigFileDSLBuilder.() -> Unit): Config {
    val builder = ConfigFileDSLBuilder(this)
    builder.block()
    return this
}

/**
 * 批量配置设置DSL
 */
fun Config.batch(block: ConfigFileDSLBuilder.() -> Unit): Config {
    val builder = ConfigFileDSLBuilder(this)
    builder.block()
    this.save()
    return this
}
