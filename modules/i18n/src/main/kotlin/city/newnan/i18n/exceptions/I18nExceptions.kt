package city.newnan.i18n.exceptions

/**
 * 国际化模块异常基类
 *
 * @param message 异常消息
 * @param cause 异常原因
 * @author Gk0Wk
 * @since 1.0.0
 */
sealed class I18nException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * 语言文件未找到异常
 *
 * @param path 文件路径
 * @author Gk0Wk
 * @since 1.0.0
 */
class LanguageFileNotFoundException(path: String) : I18nException("Language file not found: $path")

/**
 * 语言文件格式异常
 *
 * @param path 文件路径
 * @param cause 异常原因
 * @author Gk0Wk
 * @since 1.0.0
 */
class LanguageFileFormatException(path: String, cause: Throwable) : 
    I18nException("Invalid language file format: $path", cause)

/**
 * 语言注册异常
 *
 * @param locale 语言区域
 * @param cause 异常原因
 * @author Gk0Wk
 * @since 1.0.0
 */
class LanguageRegistrationException(locale: String, cause: Throwable) : 
    I18nException("Failed to register language: $locale", cause)

/**
 * 语言重载异常
 *
 * @param locale 语言区域
 * @param cause 异常原因
 * @author Gk0Wk
 * @since 1.0.0
 */
class LanguageReloadException(locale: String, cause: Throwable) : 
    I18nException("Failed to reload language: $locale", cause)
