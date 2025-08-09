package city.newnan.network.extensions

import city.newnan.core.base.BasePlugin
import city.newnan.core.terminable.terminable
import city.newnan.network.HttpClientManager

/**
 * 网络管理器属性扩展
 *
 * 为 BasePlugin 添加网络管理器支持
 */
private val networkManagerMap = mutableMapOf<BasePlugin, HttpClientManager>()

/**
 * 获取或创建网络管理器
 */
val BasePlugin.networkManager: HttpClientManager
    get() = networkManagerMap.getOrPut(this) {
        val manager = HttpClientManager()

        // 在插件禁用时清理（使用bind方法注册清理回调）
        bind(terminable {
            manager.close()
            networkManagerMap.remove(this@networkManager)
        })

        manager
    }

/**
 * 检查插件是否有网络管理器
 */
fun BasePlugin.hasNetworkManager(): Boolean = networkManagerMap.containsKey(this)

/**
 * 移除网络管理器
 * 通常在插件禁用时调用
 */
internal fun BasePlugin.removeNetworkManager() {
    networkManagerMap.remove(this)?.close()
}
