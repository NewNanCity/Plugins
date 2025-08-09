@file:Suppress("unused")

package city.newnan.config.formats

import com.fasterxml.jackson.databind.ObjectMapper

/**
 * 配置格式插件接口
 * 
 * 定义了配置格式插件的标准接口，支持动态加载和注册不同的配置文件格式
 * 
 * @author Gk0Wk
 * @since 1.0.0
 */
interface ConfigFormatPlugin {
    
    /**
     * 格式名称
     */
    val formatName: String
    
    /**
     * 支持的文件扩展名
     */
    val supportedExtensions: Set<String>
    
    /**
     * 是否为核心格式（必需格式）
     */
    val isCoreFormat: Boolean
    
    /**
     * 创建对应格式的 ObjectMapper
     * 
     * @return 配置好的 ObjectMapper 实例
     * @throws UnsupportedOperationException 如果格式不受支持或依赖缺失
     */
    fun createMapper(): ObjectMapper
    
    /**
     * 检查格式是否可用（依赖是否存在）
     * 
     * @return true 如果格式可用，false 否则
     */
    fun isAvailable(): Boolean
}

/**
 * 配置格式注册表
 * 
 * 管理所有已注册的配置格式插件
 */
object ConfigFormatRegistry {
    
    private val plugins = mutableMapOf<String, ConfigFormatPlugin>()
    private val extensionMap = mutableMapOf<String, ConfigFormatPlugin>()
    
    /**
     * 注册格式插件
     * 
     * @param plugin 要注册的插件
     */
    fun register(plugin: ConfigFormatPlugin) {
        plugins[plugin.formatName.lowercase()] = plugin
        plugin.supportedExtensions.forEach { ext ->
            extensionMap[ext.lowercase()] = plugin
        }
    }
    
    /**
     * 根据格式名称获取插件
     * 
     * @param formatName 格式名称
     * @return 对应的插件，如果不存在则返回 null
     */
    fun getByFormat(formatName: String): ConfigFormatPlugin? {
        return plugins[formatName.lowercase()]
    }
    
    /**
     * 根据文件扩展名获取插件
     * 
     * @param extension 文件扩展名
     * @return 对应的插件，如果不存在则返回 null
     */
    fun getByExtension(extension: String): ConfigFormatPlugin? {
        return extensionMap[extension.lowercase()]
    }
    
    /**
     * 获取所有已注册的格式名称
     * 
     * @return 格式名称集合
     */
    fun getRegisteredFormats(): Set<String> {
        return plugins.keys.toSet()
    }
    
    /**
     * 获取所有可用的格式插件
     * 
     * @return 可用的插件列表
     */
    fun getAvailablePlugins(): List<ConfigFormatPlugin> {
        return plugins.values.filter { it.isAvailable() }
    }
    
    /**
     * 获取所有核心格式插件
     * 
     * @return 核心插件列表
     */
    fun getCorePlugins(): List<ConfigFormatPlugin> {
        return plugins.values.filter { it.isCoreFormat }
    }
    
    /**
     * 清空注册表
     */
    fun clear() {
        plugins.clear()
        extensionMap.clear()
    }
    
    init {
        // 注册内置格式插件
        register(JsonFormatPlugin())
        register(YamlFormatPlugin())
        register(TomlFormatPlugin())
        register(XmlFormatPlugin())
        register(CsvFormatPlugin())
        register(PropertiesFormatPlugin())
        register(HoconFormatPlugin())
    }
}

/**
 * 配置格式异常
 */
sealed class ConfigFormatException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    
    /**
     * 不支持的格式异常
     */
    class UnsupportedFormatException(format: String) : 
        ConfigFormatException("不支持的配置格式: $format")
    
    /**
     * 格式依赖缺失异常
     */
    class MissingDependencyException(format: String, dependency: String) : 
        ConfigFormatException("格式 $format 的依赖 $dependency 缺失")
    
    /**
     * 格式初始化异常
     */
    class FormatInitializationException(format: String, cause: Throwable) : 
        ConfigFormatException("格式 $format 初始化失败", cause)
}
