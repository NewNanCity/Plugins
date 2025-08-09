package city.newnan.i18n.extensions

import city.newnan.core.base.BasePlugin
import city.newnan.config.extensions.configManager
import city.newnan.i18n.LanguageManager
import java.util.*

/**
 * i18n模块的BasePlugin扩展
 *
 * 为BasePlugin添加便捷的语言管理功能
 */

/**
 * 创建语言管理器并注册语言文件，支持补全缺失的翻译键
 *
 * @param languageFiles 语言文件映射（语言区域 -> 文件路径）
 * @param mergeWithTemplate 是否补全缺失的翻译键
 * @param createBackup 是否在修改前创建备份
 * @param majorLanguage 主要语言
 * @param defaultLanguage 默认语言（回退语言）
 * @return 语言管理器实例
 */
fun BasePlugin.setupLanguageManager(
    languageFiles: Map<Locale, String>,
    mergeWithTemplate: Boolean = true,
    createBackup: Boolean = false,
    majorLanguage: Locale? = null,
    defaultLanguage: Locale? = null
): LanguageManager {
    val languageManager = LanguageManager(this, configManager)
        .registerAll(languageFiles, mergeWithTemplate, createBackup)

    // 设置主要语言
    majorLanguage?.let { languageManager.setMajorLanguage(it) }

    // 设置默认语言
    defaultLanguage?.let { languageManager.setDefaultLanguage(it) }

    // 自动设置为插件的语言提供者
    setLanguageProvider(languageManager)

    // 重新加载所有语言文件
    languageManager.reloadAll()

    return languageManager
}