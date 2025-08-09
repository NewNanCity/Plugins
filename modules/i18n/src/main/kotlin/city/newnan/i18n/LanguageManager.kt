package city.newnan.i18n

import city.newnan.i18n.exceptions.LanguageFileNotFoundException
import city.newnan.i18n.exceptions.LanguageRegistrationException
import city.newnan.i18n.exceptions.LanguageReloadException
import city.newnan.config.ConfigManager
import city.newnan.core.cache.Cache
import city.newnan.core.cache.LRUCache
import city.newnan.core.utils.text.LanguageProvider
import city.newnan.core.terminable.Terminable
import city.newnan.core.terminable.TerminableConsumer
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * 语言管理器
 *
 * 基于配置模块的多语言管理系统，提供完整的国际化支持
 *
 * @param plugin 绑定的插件实例
 * @param configManager 配置管理器
 * @param config 国际化配置
 * @author Gk0Wk
 * @since 1.0.0
 */
class LanguageManager(
    private val plugin: Plugin,
    private val configManager: ConfigManager,
    private val config: I18nConfig = I18nConfig.DEFAULT
) : LanguageProvider, Terminable {

    /**
     * 主要语言（优先使用）
     */
    var majorLanguage: Language? = null
        private set

    /**
     * 默认语言（回退使用）
     */
    var defaultLanguage: Language? = null
        private set

    /**
     * 语言映射表
     */
    private val languageMap: MutableMap<Locale, Language> = ConcurrentHashMap()

    /**
     * 模板变量匹配模式
     */
    private val templatePattern: Pattern = Pattern.compile(config.templatePattern)

    /**
     * 翻译结果缓存
     */
    private val translationCache: Cache<String, String>? = if (config.cacheEnabled) {
        LRUCache(config.cacheSize)
    } else null

    init {
        // 如果插件实现了TerminableConsumer，自动绑定生命周期
        if (plugin is TerminableConsumer) {
            bindWith(plugin)
        }

        // 自动检测系统语言
        if (config.autoDetectSystemLanguage) {
            guessMajorLanguage()
        }
    }

    /**
     * 注册语言文件
     *
     * @param locale 语言区域
     * @param filePath 文件路径（相对于插件数据目录）
     * @return 当前实例（支持链式调用）
     * @throws LanguageFileNotFoundException 文件不存在时抛出
     * @throws LanguageRegistrationException 注册失败时抛出
     */
    fun register(locale: Locale, filePath: String): LanguageManager {
        return register(locale, filePath, false)
    }

    /**
     * 注册语言文件，支持补全缺失的翻译键
     *
     * @param locale 语言区域
     * @param filePath 文件路径（相对于插件数据目录）
     * @param mergeWithTemplate 是否补全缺失的翻译键
     * @param createBackup 是否在修改前创建备份
     * @return 当前实例（支持链式调用）
     * @throws LanguageFileNotFoundException 文件不存在时抛出
     * @throws LanguageRegistrationException 注册失败时抛出
     */
    fun register(
        locale: Locale,
        filePath: String,
        mergeWithTemplate: Boolean,
        createBackup: Boolean = false
    ): LanguageManager {
        try {
            val file = File(plugin.dataFolder, filePath)

            // 如果文件不存在，尝试从JAR资源复制
            if (!file.exists()) {
                // 尝试从JAR资源复制文件
                val resourceStream = plugin.getResource(filePath)
                if (resourceStream != null) {
                    // 确保父目录存在
                    file.parentFile?.mkdirs()

                    // 复制资源文件到目标位置
                    resourceStream.use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } else {
                    throw LanguageFileNotFoundException("Language file not found in JAR resources: $filePath")
                }
            } else if (mergeWithTemplate) {
                // 文件存在且需要合并，补全缺失的翻译键
                mergeLanguageFileWithTemplate(filePath, createBackup)
            }

            // 再次检查文件是否存在且为文件
            if (!file.exists() || !file.isFile) {
                throw LanguageFileNotFoundException(filePath)
            }

            val language = Language(locale, file, configManager)
            languageMap[locale] = language

            // 清空缓存
            translationCache?.clear()

            return this
        } catch (e: Exception) {
            throw LanguageRegistrationException(locale.toString(), e)
        }
    }

    /**
     * 注销语言
     *
     * @param locale 语言区域
     * @return 当前实例（支持链式调用）
     */
    fun unregister(locale: Locale): LanguageManager {
        languageMap.remove(locale)

        // 如果注销的是当前使用的语言，清空引用
        if (majorLanguage?.locale == locale) {
            majorLanguage = null
        }
        if (defaultLanguage?.locale == locale) {
            defaultLanguage = null
        }

        // 清空缓存
        translationCache?.clear()

        return this
    }

    /**
     * 获取指定语言
     *
     * @param locale 语言区域
     * @return 语言实例，如果不存在则返回null
     */
    fun getLanguage(locale: Locale): Language? {
        return languageMap[locale]
    }

    /**
     * 设置主要语言
     *
     * @param locale 语言区域
     * @return 当前实例（支持链式调用）
     */
    fun setMajorLanguage(locale: Locale): LanguageManager {
        languageMap[locale]?.let { language ->
            majorLanguage = language
            translationCache?.clear()
        }
        return this
    }

    /**
     * 设置默认语言
     *
     * @param locale 语言区域
     * @return 当前实例（支持链式调用）
     */
    fun setDefaultLanguage(locale: Locale): LanguageManager {
        languageMap[locale]?.let { language ->
            defaultLanguage = language
            translationCache?.clear()
        }
        return this
    }

    /**
     * 自动猜测主要语言
     * 基于系统默认语言设置
     */
    fun guessMajorLanguage() {
        val systemLocale = Locale.getDefault()
        if (languageMap.containsKey(systemLocale)) {
            setMajorLanguage(systemLocale)
        }
    }

    /**
     * 重新加载所有语言文件
     *
     * @return 当前实例（支持链式调用）
     */
    fun reloadAll(): LanguageManager {
        val errors = mutableListOf<Exception>()

        languageMap.forEach { (locale, language) ->
            try {
                language.reload()
            } catch (e: LanguageReloadException) {
                errors.add(e)
                if (config.logReloadErrors) {
                    (plugin as? city.newnan.core.base.BasePlugin)?.logger?.warn("Failed to reload language $locale: ${e.message ?: "Unknown error"}")
                        ?: plugin.logger.warning("Failed to reload language $locale: ${e.message}")
                }
            }
        }

        // 清空缓存
        translationCache?.clear()

        // 如果有错误且配置要求记录，抛出第一个错误
        if (errors.isNotEmpty() && config.logReloadErrors) {
            (plugin as? city.newnan.core.base.BasePlugin)?.logger?.warn("%d language(s) failed to reload", errors.size)
                ?: plugin.logger.warning("${errors.size} language(s) failed to reload")
        }

        return this
    }

    /**
     * 实现LanguageProvider接口
     * 提供本地化文本转换功能
     *
     * @param rawText 原始文本（包含模板变量）
     * @return 本地化后的文本，如果没有找到翻译则返回null
     */
    override fun provideLanguage(rawText: String): String? {
        if (!config.enabled) {
            return null
        }

        // 检查缓存
        translationCache?.get(rawText)?.let { cached ->
            return cached
        }
        val result = processTemplate(rawText)

        // 缓存结果
        if (result != null) {
            translationCache?.put(rawText, result)
        }

        return result
    }

    /**
     * 处理模板文本
     *
     * @param rawText 原始文本
     * @return 处理后的文本
     */
    private fun processTemplate(rawText: String): String? {
        // 测试正则表达式是否能匹配
        val testMatcher = templatePattern.matcher(rawText)
        val canMatch = testMatcher.find()

        if (canMatch) {
            testMatcher.reset()
            var matchCount = 0
            while (testMatcher.find()) {
                matchCount++
            }
        } else {
            return null
        }

        testMatcher.reset() // 重置matcher状态

        var processedText = rawText
        val matcher = templatePattern.matcher(processedText)
        var hasReplacement = false
        var replacementCount = 0

        while (matcher.find()) {
            val templateVar = matcher.group(0)  // 完整的模板变量，如 <%player%>
            val key = matcher.group(1)          // 捕获组中的键值，如 player

            val replacement = findTranslation(key)
            if (replacement != null) {
                val beforeReplace = processedText
                processedText = processedText.replace(templateVar, replacement)
                hasReplacement = true
                replacementCount++
            } else {
                if (config.warnOnMissingTranslation) {
                    Bukkit.getLogger().warning("Missing translation for key: $key")
                }
            }
        }
        val result = if (hasReplacement || processedText != rawText) processedText else null
        return result
    }

    /**
     * 查找翻译文本
     * 按照 主语言 → 默认语言 的顺序查找
     *
     * @param key 翻译键
     * @return 翻译文本，如果找不到则返回null
     */
    private fun findTranslation(key: String): String? {
        // 优先使用主语言
        majorLanguage?.let { lang ->
            lang.getNodeString(key)?.let { translation ->
                return translation
            }
        }

        // 回退到默认语言
        defaultLanguage?.let { lang ->
            lang.getNodeString(key)?.let { translation ->
                return translation
            }
        }
        return null
    }

    /**
     * 获取当前语言代码
     *
     * @return 当前主要语言的代码，如果没有设置则返回默认语言代码
     */
    override fun getCurrentLanguage(): String {
        return majorLanguage?.locale?.toString()
            ?: defaultLanguage?.locale?.toString()
            ?: config.majorLanguage.toString()
    }

    /**
     * 检查是否支持指定的语言
     *
     * @param language 语言代码
     * @return 是否支持该语言
     */
    override fun supportsLanguage(language: String): Boolean {
        return try {
            val locale = Locale.forLanguageTag(language.replace("_", "-"))
            languageMap.containsKey(locale)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取所有已注册的语言
     *
     * @return 语言区域集合
     */
    fun getRegisteredLanguages(): Set<Locale> {
        return languageMap.keys.toSet()
    }

    /**
     * 获取语言统计信息
     *
     * @return 包含各语言节点数量的映射
     */
    fun getLanguageStats(): Map<Locale, Int> {
        return languageMap.mapValues { (_, language) -> language.getNodeCount() }
    }

    /**
     * 清空翻译缓存
     */
    fun clearCache() {
        translationCache?.clear()
    }

    /**
     * 获取缓存统计信息
     *
     * @return 缓存统计信息字符串
     */
    fun getCacheStats(): String {
        return translationCache?.let { cache ->
            "Cache size: ${cache.size}, enabled: true"
        } ?: "Cache disabled"
    }

    /**
     * 将现有语言文件与模板文件合并，补全缺失的翻译键
     *
     * @param filePath 语言文件路径（相对于插件数据目录）
     * @param createBackup 是否在修改前创建备份
     */
    private fun mergeLanguageFileWithTemplate(filePath: String, createBackup: Boolean) {
        try {
            // 使用ConfigManager的touchWithMerge功能
            val wasModified = !configManager.touchWithMerge(filePath, filePath, createBackup)

            if (wasModified) {
                plugin.logger.info("Language file merged with template: $filePath")
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to merge language file $filePath: ${e.message}")
        }
    }

    /**
     * 批量注册语言文件并补全缺失的翻译键
     *
     * @param languageFiles 语言文件映射（语言区域 -> 文件路径）
     * @param mergeWithTemplate 是否补全缺失的翻译键
     * @param createBackup 是否在修改前创建备份
     * @return 当前实例（支持链式调用）
     */
    fun registerAll(
        languageFiles: Map<Locale, String>,
        mergeWithTemplate: Boolean = false,
        createBackup: Boolean = false
    ): LanguageManager {
        languageFiles.forEach { (locale, filePath) ->
            register(locale, filePath, mergeWithTemplate, createBackup)
        }
        return this
    }

    /**
     * 重新加载所有语言文件并补全缺失的翻译键
     *
     * @param mergeWithTemplate 是否补全缺失的翻译键
     * @param createBackup 是否在修改前创建备份
     * @return 当前实例（支持链式调用）
     */
    fun reloadAllWithMerge(
        mergeWithTemplate: Boolean = true,
        createBackup: Boolean = false
    ): LanguageManager {
        val errors = mutableListOf<Exception>()

        // 先进行合并操作
        if (mergeWithTemplate) {
            languageMap.forEach { (locale, language) ->
                try {
                    // 使用Language对象中保存的配置文件路径
                    val filePath = language.configPath
                    if (filePath != null) {
                        mergeLanguageFileWithTemplate(filePath, createBackup)
                    } else {
                        plugin.logger.warning("Cannot merge language file for locale $locale: file path not available")
                    }
                } catch (e: Exception) {
                    errors.add(e)
                    if (config.logReloadErrors) {
                        (plugin as? city.newnan.core.base.BasePlugin)?.logger?.warn("Failed to merge language $locale: ${e.message ?: "Unknown error"}")
                            ?: plugin.logger.warning("Failed to merge language $locale: ${e.message}")
                    }
                }
            }
        }

        // 然后重新加载
        languageMap.forEach { (locale, language) ->
            try {
                language.reload()
            } catch (e: LanguageReloadException) {
                errors.add(e)
                if (config.logReloadErrors) {
                    (plugin as? city.newnan.core.base.BasePlugin)?.logger?.warn("Failed to reload language $locale: ${e.message ?: "Unknown error"}")
                        ?: plugin.logger.warning("Failed to reload language $locale: ${e.message}")
                }
            }
        }

        // 清空缓存
        translationCache?.clear()

        // 如果有错误且配置要求记录，抛出第一个错误
        if (errors.isNotEmpty() && config.logReloadErrors) {
            (plugin as? city.newnan.core.base.BasePlugin)?.logger?.warn("${errors.size} language(s) failed to reload/merge")
                ?: plugin.logger.warning("${errors.size} language(s) failed to reload/merge")
        }

        return this
    }

    /**
     * 实现Terminable接口
     * 清理所有资源
     */
    override fun close() {
        languageMap.clear()
        translationCache?.clear()
        majorLanguage = null
        defaultLanguage = null
    }

    override fun toString(): String {
        return "LanguageManager(languages=${languageMap.size}, major=${majorLanguage?.locale}, default=${defaultLanguage?.locale})"
    }
}
