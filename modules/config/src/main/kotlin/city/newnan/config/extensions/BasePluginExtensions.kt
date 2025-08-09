package city.newnan.config.extensions

import city.newnan.core.base.BasePlugin
import city.newnan.core.terminable.terminable
import city.newnan.config.ConfigManager

/**
 * 配置管理器属性扩展
 *
 * 为 BasePlugin 添加配置管理器支持
 * 使用 ConcurrentHashMap 确保线程安全
 */
private val configManagerMap = java.util.concurrent.ConcurrentHashMap<BasePlugin, ConfigManager>()

/**
 * 获取或创建配置管理器
 * 使用 computeIfAbsent 确保原子性操作，避免竞态条件
 */
val BasePlugin.configManager: ConfigManager
    get() = configManagerMap.computeIfAbsent(this) { plugin ->
        val manager = ConfigManager(plugin)

        // 在插件禁用时清理（使用bind方法注册清理回调）
        bind(terminable {
            manager.close()
            configManagerMap.remove(plugin)
        })

        manager
    }

/**
 * 检查插件是否有配置管理器
 */
fun BasePlugin.hasConfigManager(): Boolean = configManagerMap.containsKey(this)

/**
 * 检查配置文件是否存在，不存在则创建，存在则补全缺失的配置项
 *
 * @param configPath 配置文件路径（相对于插件数据目录）
 * @param templatePath 模板文件路径（资源路径）
 * @param createBackup 是否在修改前创建备份
 * @return true 如果文件已存在且无需修改，false 如果新创建或已补全
 */
fun BasePlugin.touchConfigWithMerge(
    configPath: String,
    templatePath: String = configPath,
    createBackup: Boolean = false
): Boolean = configManager.touchWithMerge(configPath, templatePath, createBackup)

/**
 * 检查配置文件是否存在，不存在则创建，存在则补全缺失的配置项
 *
 * @param configPath 配置文件路径（相对于插件数据目录）
 * @param defaultData 默认数据提供者
 * @param format 配置格式，null 则根据文件扩展名推断
 * @param createBackup 是否在修改前创建备份
 * @return true 如果文件已存在且无需修改，false 如果新创建或已补全
 */
inline fun <reified T : Any> BasePlugin.touchConfigWithMerge(
    configPath: String,
    noinline defaultData: () -> T,
    format: String? = null,
    createBackup: Boolean = false
): Boolean = configManager.touchWithMerge(configPath, defaultData, format, createBackup)

/**
 * 移除配置管理器
 * 通常在插件禁用时调用
 */
internal fun BasePlugin.removeConfigManager() {
    configManagerMap.remove(this)?.close()
}
