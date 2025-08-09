@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package city.newnan.config

import city.newnan.config.formats.ConfigFormatException
import city.newnan.config.formats.ConfigFormatPlugin
import city.newnan.config.formats.ConfigFormatRegistry
import city.newnan.core.cache.Cache
import city.newnan.core.cache.LRUCache
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import city.newnan.core.terminable.Terminable
import city.newnan.core.terminable.TerminableConsumer
import org.bukkit.plugin.Plugin
import java.io.*
import java.nio.file.Path
import kotlin.io.path.extension

/**
 * 配置管理器
 *
 * 基于 Jackson 的多格式配置管理器，支持：
 * - JSON、YAML（核心格式）
 * - TOML、XML、CSV、Properties、HOCON（可选格式）
 * - Bukkit 原生类型序列化
 * - 缓存机制
 * - 插件式格式扩展
 * - 类型安全的配置操作
 *
 * @param plugin 绑定的插件实例
 * @author Gk0Wk
 * @since 1.0.0
 */
class ConfigManager(
    /**
     * 绑定的插件实例
     */
    val plugin: Plugin
) : Terminable {

    /**
     * 配置缓存
     */
    @Volatile
    private var cache: Cache<String, Any>? = LRUCache(DEFAULT_CACHE_CAPACITY)

    /**
     * 格式映射器缓存
     * 使用 ConcurrentHashMap 确保线程安全
     */
    private val mapperCache = java.util.concurrent.ConcurrentHashMap<String, ObjectMapper>()

    init {
        if (plugin is TerminableConsumer) {
            bindWith(plugin)
        }
    }

    companion object {
        /**
         * 默认缓存容量
         */
        const val DEFAULT_CACHE_CAPACITY = 16

        /**
         * 配置文件类型引用
         */
        private val CONFIG_TYPE_REF = object : TypeReference<Config>() {}
    }

    /**
     * 设置缓存实例
     *
     * @param cache 缓存实例，null 表示禁用缓存
     */
    fun setCache(cache: Cache<String, Any>?) {
        this.cache = cache
    }

    /**
     * 获取缓存值
     */
    internal fun <T> getCachedValue(key: String): T? {
        val currentCache = cache
        val result = if (currentCache != null) {
            @Suppress("UNCHECKED_CAST")
            currentCache[key] as? T
        } else {
            null
        }
        return result
    }

    /**
     * 设置缓存值
     */
    internal fun setCachedValue(key: String, value: Any) {
        cache?.set(key, value)
    }

    /**
     * 清除指定前缀的缓存项
     * 使用线程安全的方式清理缓存
     */
    private fun clearCacheByPrefix(prefix: String) {
        cache?.let { currentCache ->
            val keysToRemove = currentCache.keys.filter { it.startsWith(prefix) }
            keysToRemove.forEach { key -> currentCache.remove(key) }
        }
    }

    /**
     * 获取指定格式的 ObjectMapper
     * 使用 computeIfAbsent 确保原子性操作，避免竞态条件
     *
     * @param format 格式名称
     * @return ObjectMapper 实例
     * @throws ConfigFormatException.UnsupportedFormatException 如果格式不支持
     */
    fun getMapper(format: String): ObjectMapper {
        return mapperCache.computeIfAbsent(format.lowercase()) { formatKey ->
            val plugin = ConfigFormatRegistry.getByFormat(formatKey)
                ?: throw ConfigFormatException.UnsupportedFormatException(formatKey)

            if (!plugin.isAvailable()) {
                throw ConfigFormatException.MissingDependencyException(
                    formatKey,
                    "格式 $formatKey 的依赖库未找到"
                )
            }

            plugin.createMapper()
        }
    }

    /**
     * 根据文件路径推断格式
     *
     * @param path 文件路径
     * @return 格式插件
     * @throws ConfigFormatException.UnsupportedFormatException 如果格式不支持
     */
    fun guessFormat(path: Path): ConfigFormatPlugin {
        val extension = path.extension.lowercase()
        return ConfigFormatRegistry.getByExtension(extension)
            ?: throw ConfigFormatException.UnsupportedFormatException("扩展名: $extension")
    }

    /**
     * 根据文件路径推断格式
     *
     * @param file 文件
     * @return 格式插件
     */
    fun guessFormat(file: File): ConfigFormatPlugin = guessFormat(file.toPath())

    /**
     * 检查配置文件是否存在，不存在则创建
     *
     * @param configPath 配置文件路径（相对于插件数据目录）
     * @param templatePath 模板文件路径（资源路径）
     * @return true 如果文件已存在，false 如果新创建
     */
    fun touch(configPath: String, templatePath: String = configPath): Boolean {
        val file = File(plugin.dataFolder, configPath)

        if (file.exists()) {
            return true
        }

        // 确保父目录存在
        file.parentFile?.mkdirs()

        // 从资源复制模板文件
        plugin.getResource(templatePath)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: run {
            // 如果模板不存在，创建空文件
            file.createNewFile()
        }

        return false
    }

    /**
     * 检查配置文件是否存在，不存在则使用默认数据创建
     *
     * @param configPath 配置文件路径（相对于插件数据目录）
     * @param defaultData 默认数据提供者
     * @param format 配置格式，null 则根据文件扩展名推断
     * @return true 如果文件已存在，false 如果新创建
     */
    inline fun <reified T : Any> touch(
        configPath: String,
        noinline defaultData: () -> T,
        format: String? = null
    ): Boolean {
        val file = File(plugin.dataFolder, configPath)

        if (file.exists()) {
            return true
        }

        // 确保父目录存在
        file.parentFile?.mkdirs()

        // 保存默认数据
        val typeRef = object : TypeReference<T>() {}
        saveInternal(defaultData(), typeRef, configPath, format)

        return false
    }

    /**
     * 检查配置文件是否存在，不存在则创建，存在则补全缺失的配置项
     *
     * @param configPath 配置文件路径（相对于插件数据目录）
     * @param templatePath 模板文件路径（资源路径）
     * @param createBackup 是否在修改前创建备份
     * @return true 如果文件已存在且无需修改，false 如果新创建或已补全
     */
    fun touchWithMerge(
        configPath: String,
        templatePath: String = configPath,
        createBackup: Boolean = false
    ): Boolean {
        val file = File(plugin.dataFolder, configPath)

        if (!file.exists()) {
            // 文件不存在，直接创建
            return touch(configPath, templatePath)
        }

        // 文件存在，检查是否需要补全
        return mergeWithTemplate(configPath, templatePath, createBackup)
    }

    /**
     * 检查配置文件是否存在，不存在则创建，存在则补全缺失的配置项
     *
     * @param configPath 配置文件路径（相对于插件数据目录）
     * @param defaultData 默认数据提供者
     * @param format 配置格式，null 则根据文件扩展名推断
     * @param createBackup 是否在修改前创建备份
     * @return true 如果文件已存在且无需修改，false 如果新创建或已补全
     */
    inline fun <reified T : Any> touchWithMerge(
        configPath: String,
        noinline defaultData: () -> T,
        format: String? = null,
        createBackup: Boolean = false
    ): Boolean {
        val file = File(plugin.dataFolder, configPath)

        if (!file.exists()) {
            // 文件不存在，直接创建
            return touch(configPath, defaultData, format)
        }

        // 文件存在，检查是否需要补全
        return mergeWithDefaultData(configPath, defaultData, format, createBackup)
    }

    /**
     * 获取配置文件
     *
     * @param configPath 配置文件路径（相对于插件数据目录）
     * @param format 配置格式，null 则根据文件扩展名推断
     * @param useCache 是否使用缓存
     * @return 配置实例
     */
    fun get(
        configPath: String,
        format: String? = null,
        useCache: Boolean = true
    ): Config {
        val file = File(plugin.dataFolder, configPath)
        val cacheKey = "${file.canonicalPath}:${(Config::class as Any).javaClass.canonicalName}"

        // 检查缓存
        if (useCache) {
            cache?.get(cacheKey)?.let { cached ->
                if (cached is Config) {
                    return cached
                }
            }
        }

        // 确保文件存在
        touch(configPath)

        // 确定格式
        val formatPlugin = if (format != null) {
            ConfigFormatRegistry.getByFormat(format)
                ?: throw ConfigFormatException.UnsupportedFormatException(format)
        } else {
            guessFormat(file)
        }

        // 加载配置
        val mapper = getMapper(formatPlugin.formatName)
        val node = if (file.length() == 0L) {
            mapper.createObjectNode()
        } else {
            mapper.readTree(file) as? ObjectNode
                ?: mapper.createObjectNode()
        }

        val config = Config(
            file = file,
            configPath = configPath,
            format = formatPlugin.formatName,
            node = node,
            manager = this
        )

        // 缓存配置
        if (useCache) {
            cache?.set(cacheKey, config)
        }

        return config
    }

    /**
     * 解析配置文件为指定类型
     *
     * @param T 目标类型
     * @param configPath 配置文件路径（相对于插件数据目录）
     * @param format 配置格式，null 则根据文件扩展名推断
     * @param useCache 是否使用缓存
     * @return 解析后的对象
     */
    inline fun <reified T : Any> parse(
        configPath: String,
        format: String? = null,
        useCache: Boolean = true
    ): T {
        val typeRef = object : TypeReference<T>() {}
        return parseInternal(configPath, typeRef, format, useCache)
    }

    /**
     * 内部解析方法（非 inline）
     */
    fun <T : Any> parseInternal(
        configPath: String,
        typeRef: TypeReference<T>,
        format: String? = null,
        useCache: Boolean = true
    ): T {
        val file = File(plugin.dataFolder, configPath)
        val cacheKey = "${file.canonicalPath}:${typeRef.type.typeName}"

        // 检查缓存
        if (useCache) {
            getCachedValue<T>(cacheKey)?.let { cached ->
                return cached
            }
        }

        // 确保文件存在
        touch(configPath)

        // 确定格式
        val formatPlugin = if (format != null) {
            ConfigFormatRegistry.getByFormat(format)
                ?: throw ConfigFormatException.UnsupportedFormatException(format)
        } else {
            guessFormat(file)
        }

        // 解析配置
        val mapper = getMapper(formatPlugin.formatName)
        val result = mapper.readValue(file, typeRef)

        // 缓存结果
        if (useCache) {
            setCachedValue(cacheKey, result)
        }

        return result
    }

    /**
     * 保存对象到配置文件
     *
     * @param T 对象类型
     * @param obj 要保存的对象
     * @param configPath 配置文件路径（相对于插件数据目录）
     * @param format 配置格式，null 则根据文件扩展名推断
     * @param updateCache 是否更新缓存
     */
    inline fun <reified T : Any> save(
        obj: T,
        configPath: String,
        format: String? = null,
        updateCache: Boolean = true
    ) {
        val typeRef = object : TypeReference<T>() {}
        return saveInternal(obj, typeRef, configPath, format, updateCache)
    }

    fun <T : Any> saveInternal(
        obj: T,
        typeRef: TypeReference<T>,
        configPath: String,
        format: String? = null,
        updateCache: Boolean = true
    ) {
        val file = File(plugin.dataFolder, configPath)

        // 确保父目录存在
        file.parentFile?.mkdirs()

        // 确定格式
        val formatPlugin = if (format != null) {
            ConfigFormatRegistry.getByFormat(format)
                ?: throw ConfigFormatException.UnsupportedFormatException(format)
        } else {
            guessFormat(file)
        }

        // 保存配置
        val mapper = getMapper(formatPlugin.formatName)

        when (obj) {
            is Config -> {
                mapper.writeValue(file, obj.node)
                if (updateCache) {
                    setCachedValue("${file.canonicalPath}:", obj)
                }
            }
            else -> {
                mapper.writeValue(file, obj)
                if (updateCache) {
                    val canonicalPath = "${file.canonicalPath}:"
                    clearCacheByPrefix(canonicalPath)
                    val cacheKey = "${file.canonicalPath}:${typeRef.type.typeName}"
                    setCachedValue(cacheKey, obj)
                }
            }
        }
    }

    /**
     * 删除配置文件
     *
     * @param configPath 配置文件路径（相对于插件数据目录）
     * @param removeFromCache 是否从缓存中移除
     */
    fun remove(configPath: String, removeFromCache: Boolean = true) {
        val file = File(plugin.dataFolder, configPath)

        if (removeFromCache) {
            val canonicalPath = "${file.canonicalPath}:"
            clearCacheByPrefix(canonicalPath)
        }

        file.delete()
    }

    /**
     * 重置配置文件为默认值
     *
     * @param configPath 配置文件路径（相对于插件数据目录）
     * @param templatePath 模板文件路径（资源路径）
     */
    fun reset(configPath: String, templatePath: String = configPath) {
        remove(configPath)
        touch(configPath, templatePath)
    }

    /**
     * 重置配置文件为默认数据
     *
     * @param T 数据类型
     * @param configPath 配置文件路径（相对于插件数据目录）
     * @param defaultData 默认数据提供者
     * @param format 配置格式，null 则根据文件扩展名推断
     */
    inline fun <reified T : Any> reset(
        configPath: String,
        noinline defaultData: () -> T,
        format: String? = null
    ) {
        remove(configPath)
        touch(configPath, defaultData, format)
    }

    /**
     * 获取支持的格式列表
     *
     * @return 格式名称到可用性的映射
     */
    fun getSupportedFormats(): Map<String, Boolean> {
        return ConfigFormatRegistry.getRegisteredFormats().associateWith { format ->
            ConfigFormatRegistry.getByFormat(format)?.isAvailable() ?: false
        }
    }

    /**
     * 获取可用的格式列表
     *
     * @return 可用的格式名称列表
     */
    fun getAvailableFormats(): List<String> {
        return ConfigFormatRegistry.getAvailablePlugins().map { it.formatName }
    }

    /**
     * 将现有配置文件与模板文件合并，补全缺失的配置项
     *
     * @param configPath 配置文件路径（相对于插件数据目录）
     * @param templatePath 模板文件路径（资源路径）
     * @param createBackup 是否在修改前创建备份
     * @return true 如果无需修改，false 如果已补全
     */
    private fun mergeWithTemplate(
        configPath: String,
        templatePath: String,
        createBackup: Boolean
    ): Boolean {
        val file = File(plugin.dataFolder, configPath)

        // 获取模板内容
        val templateContent = plugin.getResource(templatePath)?.use { input ->
            input.readBytes()
        } ?: return true // 模板不存在，无法合并

        // 确定格式
        val formatPlugin = guessFormat(file)
        val mapper = getMapper(formatPlugin.formatName)

        try {
            // 读取现有配置
            val existingNode = if (file.length() == 0L) {
                mapper.createObjectNode()
            } else {
                mapper.readTree(file) as? ObjectNode ?: mapper.createObjectNode()
            }

            // 读取模板配置
            val templateNode = mapper.readTree(templateContent) as? ObjectNode
                ?: return true // 模板格式错误，无法合并

            // 检查是否需要合并
            val mergedNode = deepMerge(existingNode, templateNode)

            // 如果没有变化，直接返回
            if (existingNode == mergedNode) {
                return true
            }

            // 创建备份
            if (createBackup) {
                createBackupFile(file)
            }

            // 保存合并后的配置
            mapper.writeValue(file, mergedNode)

            // 清除相关缓存
            val canonicalPath = "${file.canonicalPath}:"
            clearCacheByPrefix(canonicalPath)

            plugin.logger.info("Configuration file merged: $configPath")
            return false

        } catch (e: Exception) {
            plugin.logger.warning("Failed to merge configuration file $configPath: ${e.message}")
            return true
        }
    }

    /**
     * 将现有配置文件与默认数据合并，补全缺失的配置项
     *
     * @param configPath 配置文件路径（相对于插件数据目录）
     * @param defaultData 默认数据提供者
     * @param format 配置格式，null 则根据文件扩展名推断
     * @param createBackup 是否在修改前创建备份
     * @return true 如果无需修改，false 如果已补全
     */
    fun <T : Any> mergeWithDefaultData(
        configPath: String,
        defaultData: () -> T,
        format: String?,
        createBackup: Boolean
    ): Boolean {
        val file = File(plugin.dataFolder, configPath)

        // 确定格式
        val formatPlugin = if (format != null) {
            ConfigFormatRegistry.getByFormat(format)
                ?: throw ConfigFormatException.UnsupportedFormatException(format)
        } else {
            guessFormat(file)
        }
        val mapper = getMapper(formatPlugin.formatName)

        try {
            // 读取现有配置
            val existingNode = if (file.length() == 0L) {
                mapper.createObjectNode()
            } else {
                mapper.readTree(file) as? ObjectNode ?: mapper.createObjectNode()
            }

            // 将默认数据转换为JsonNode
            val defaultObj = defaultData()
            val templateNode = mapper.valueToTree<ObjectNode>(defaultObj)

            // 检查是否需要合并
            val mergedNode = deepMerge(existingNode, templateNode)

            // 如果没有变化，直接返回
            if (existingNode == mergedNode) {
                return true
            }

            // 创建备份
            if (createBackup) {
                createBackupFile(file)
            }

            // 保存合并后的配置
            mapper.writeValue(file, mergedNode)

            // 清除相关缓存
            val canonicalPath = "${file.canonicalPath}:"
            clearCacheByPrefix(canonicalPath)

            plugin.logger.info("Configuration file merged: $configPath")
            return false

        } catch (e: Exception) {
            plugin.logger.warning("Failed to merge configuration file $configPath: ${e.message}")
            return true
        }
    }

    /**
     * 深度合并两个ObjectNode，保留existing中的值，只添加template中缺失的键
     *
     * @param existing 现有配置节点
     * @param template 模板配置节点
     * @return 合并后的节点
     */
    private fun deepMerge(existing: ObjectNode, template: ObjectNode): ObjectNode {
        val result = existing.deepCopy()

        template.fields().forEach { (key, templateValue) ->
            val existingValue = result.get(key)

            when {
                existingValue == null -> {
                    // 现有配置中没有这个键，直接添加模板值
                    result.set(key, templateValue.deepCopy())
                }
                existingValue.isObject && templateValue.isObject -> {
                    // 两者都是对象，递归合并
                    val mergedChild = deepMerge(
                        existingValue as ObjectNode,
                        templateValue as ObjectNode
                    )
                    result.set<JsonNode>(key, mergedChild)
                }
                // 其他情况保留现有值，不做修改
            }
        }

        return result
    }

    /**
     * 创建配置文件的备份
     *
     * @param file 要备份的文件
     */
    private fun createBackupFile(file: File) {
        try {
            val timestamp = System.currentTimeMillis()
            val backupFile = File(file.parentFile, "${file.nameWithoutExtension}.backup.$timestamp.${file.extension}")
            file.copyTo(backupFile)
            plugin.logger.info("Configuration backup created: ${backupFile.name}")
        } catch (e: Exception) {
            plugin.logger.warning("Failed to create backup for ${file.name}: ${e.message}")
        }
    }

    /**
     * 清除所有缓存
     */
    fun clearCache() {
        cache?.clear()
    }

    /**
     * 清除特定配置文件的缓存
     *
     * @param configPath 配置文件路径（相对于插件数据目录）
     */
    fun clearCache(configPath: String) {
        val canonicalPath = "${File(plugin.dataFolder, configPath).canonicalPath}:"
        clearCacheByPrefix(canonicalPath)
    }

    /**
     * 清理资源
     */
    override fun close() {
        cache?.clear()
        mapperCache.clear()
    }

    override fun isClosed(): Boolean {
        return (cache?.size ?: 0) == 0 && mapperCache.isEmpty()
    }
}
