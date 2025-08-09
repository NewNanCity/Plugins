package city.newnan.core.utils.text

/**
 * 语言提供者接口
 *
 * 用于实现多语言支持，将原始文本转换为本地化文本
 */
interface LanguageProvider {

    /**
     * 提供本地化文本
     * @param rawText 原始文本（通常是键值）
     * @return 本地化后的文本，如果没有找到对应的翻译则返回null
     */
    fun provideLanguage(rawText: String): String?

    /**
     * 获取当前语言代码
     * @return 语言代码（如 "zh_CN", "en_US"）
     */
    fun getCurrentLanguage(): String = "zh_CN"

    /**
     * 检查是否支持指定的语言
     * @param language 语言代码
     * @return 是否支持
     */
    fun supportsLanguage(language: String): Boolean = true
}