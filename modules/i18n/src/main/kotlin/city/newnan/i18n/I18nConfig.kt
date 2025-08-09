package city.newnan.i18n

import java.util.*

/**
 * 国际化配置类
 *
 * 提供国际化模块的配置选项
 *
 * @author Gk0Wk
 * @since 1.0.0
 */
data class I18nConfig(
    /**
     * 是否启用国际化
     */
    val enabled: Boolean = true,

    /**
     * 默认语言
     */
    val defaultLanguage: Locale = Locale.US,

    /**
     * 主要语言（优先使用的语言）
     */
    val majorLanguage: Locale = Locale.getDefault(),

    /**
     * 语言文件目录
     */
    val languageDirectory: String = "lang",

    /**
     * 是否自动检测系统语言
     */
    val autoDetectSystemLanguage: Boolean = true,

    /**
     * 是否启用缓存
     */
    val cacheEnabled: Boolean = true,

    /**
     * 缓存大小
     */
    val cacheSize: Int = 64,

    /**
     * 模板变量匹配模式（正则表达式）
     * 必须包含一个捕获组来提取变量名
     * 默认格式：<%variable%> 或 <%prefix.variable.suffix%>
     */
    val templatePattern: String = "<%\\s*([a-zA-Z0-9_.]+)\\s*%>",

    /**
     * 是否在找不到翻译时记录警告
     */
    val warnOnMissingTranslation: Boolean = false,

    /**
     * 是否在语言文件重载失败时记录错误
     */
    val logReloadErrors: Boolean = true
) {

    companion object {
        /**
         * 默认配置实例
         */
        val DEFAULT = I18nConfig()

        /**
         * 中文环境配置
         */
        val CHINESE = I18nConfig(
            defaultLanguage = Locale.US,
            majorLanguage = Locale.SIMPLIFIED_CHINESE
        )

        /**
         * 禁用缓存的配置（适用于开发环境）
         */
        val NO_CACHE = I18nConfig(
            cacheEnabled = false,
            warnOnMissingTranslation = true
        )
    }
}
