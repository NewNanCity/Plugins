package city.newnan.core.utils.text

import city.newnan.core.cache.LRUCache
import net.kyori.adventure.text.Component
import java.text.MessageFormat

/**
 * 字符串格式化类
 *
 * 提供共享的字符串格式化功能，支持以下特性：
 * - 多语言处理
 * - MessageFormat 格式化
 * - 可选的颜色代码处理
 * - LRU缓存优化
 */
class StringFormatter(
    /**
     * 语言提供者，用于国际化文本
     */
    var languageProvider: LanguageProvider? = null
) {

    /**
     * Component缓存，用于缓存plain文本到Component的转换结果
     */
    private val componentCache = LRUCache<String, Component>(128)

    /**
     * Legacy字符串缓存，用于缓存plain文本到Legacy字符串的转换结果
     */
    private val legacyCache = LRUCache<String, String>(128)

    /**
     * 格式化字符串（sprintf风格，用于消息国际化）
     * @param useProvider 是否使用语言提供者
     * @param parseMode 组件解析模式
     * @param formatText 格式化文本
     * @param params 参数
     * @return 格式化后的组件
     */
    fun sprintf(
        useProvider: Boolean,
        parseMode: ComponentParseMode,
        formatText: String,
        vararg params: Any?
    ): Component {
        val plain = sprintfPlain(useProvider, formatText, *params)
        val cacheKey = "${parseMode.name}:$plain"

        return componentCache.getOrPut(cacheKey) {
            plain.toComponent(parseMode)
        }
    }

    /**
     * 格式化字符串（sprintf风格，用于消息国际化） - 兼容格式，用于不支持Component的情况
     * @param useProvider 是否使用语言提供者
     * @param parseMode 组件解析模式
     * @param formatText 格式化文本
     * @param params 参数
     * @return 格式化后的字符串
     */
    fun sprintfLegacy(
        useProvider: Boolean,
        parseMode: ComponentParseMode,
        formatText: String,
        vararg params: Any?
    ): String {
        val plain = sprintfPlain(useProvider, formatText, *params)
        val cacheKey = "${parseMode.name}:$plain"

        return legacyCache.getOrPut(cacheKey) {
            plain.toLegacy(parseMode)
        }
    }

    /**
     * 格式化字符串（sprintf风格，用于消息国际化） - 只处理语言映射和数据格式化
     * @param useProvider 是否使用语言提供者
     * @param formatText 格式化文本
     * @param params 参数
     * @return 格式化后的字符串
     */
    fun sprintfPlain(
        useProvider: Boolean,
        formatText: String,
        vararg params: Any?
    ): String {
        // 语言映射处理
        var text = if (useProvider) languageProvider?.provideLanguage(formatText) ?: formatText else formatText

        // 数据格式化
        text = if (params.isEmpty()) text else
            MessageFormat.format(text, *params.map { it.toString() }.toTypedArray())

        return text
    }

    /**
     * 设置语言提供者
     * @param provider 语言提供者
     * @return 当前StringFormatter实例，用于链式调用
     */
    fun setLanguageProvider(provider: LanguageProvider?): StringFormatter = this.also {
        this.languageProvider = provider
        clearCache()
    }

    fun clearCache() {
        componentCache.clear()
        legacyCache.clear()
    }
}