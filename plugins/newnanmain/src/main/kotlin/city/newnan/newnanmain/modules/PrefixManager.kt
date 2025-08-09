package city.newnan.newnanmain.modules

import city.newnan.config.extensions.configManager
import city.newnan.core.base.BaseModule
import city.newnan.core.event.subscribeEvent
import city.newnan.newnanmain.NewNanMainPlugin
import city.newnan.newnanmain.config.GlobalPrefixConfig
import city.newnan.newnanmain.config.PlayerPrefixConfig
import city.newnan.newnanmain.i18n.LanguageKeys
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent

/**
 * 前缀管理器
 *
 * 负责管理玩家前缀系统，包括：
 * - 全局前缀配置管理
 * - 玩家前缀配置管理
 * - 自动前缀检查和应用
 * - Vault聊天系统集成
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class PrefixManager(
    moduleName: String,
    val plugin: NewNanMainPlugin
) : BaseModule(moduleName, plugin) {

    // ===== 模块初始化 =====
    init { init() }

    // ===== 配置缓存 =====
    private var globalPrefix = mutableMapOf<String, MutableMap<String, String>>()
    private var enableAutoCheck = true
    private var checkOnJoin = true
    private var checkOnWorldChange = true

    override fun onInit() {
        // 玩家加入事件
        subscribeEvent<PlayerJoinEvent> {
            priority(EventPriority.MONITOR)
            handler { event ->
                if (checkOnJoin) {
                    checkPlayer(event.player)
                }
            }
        }

        // 玩家切换世界事件
        subscribeEvent<PlayerChangedWorldEvent> {
            priority(EventPriority.MONITOR)
            handler { event ->
                if (checkOnWorldChange) {
                    checkPlayer(event.player)
                }
            }
        }
    }

    override fun onReload() {
        // 加载插件配置
        val config = plugin.getPluginConfig()
        enableAutoCheck = config.prefixSettings.enableAutoCheck
        checkOnJoin = config.prefixSettings.checkOnJoin
        checkOnWorldChange = config.prefixSettings.checkOnWorldChange

        // 加载全局前缀配置
        plugin.configManager.touchWithMerge("prefix-config.yml", createBackup = false)
        globalPrefix = plugin.configManager.parse<GlobalPrefixConfig>("prefix-config.yml")
    }

    /**
     * 检查玩家前缀
     */
    fun checkPlayer(player: Player) {
        if (!enableAutoCheck) return

        val chat = plugin.chat ?: return

        try {
            // 创建和读取文件
            val path = "player-prefix/${player.uniqueId}.yml"
            plugin.configManager.touchWithMerge(path, "player-prefix-template.yml", createBackup = false)
            val config = plugin.configManager.parse<PlayerPrefixConfig>(path)

            // 可用性检查
            if (config.current != null) {
                if (!config.available.containsKey(config.current)) {
                    config.current = null
                    plugin.configManager.save(config, path)
                } else if (globalPrefix[config.current]?.containsKey(config.available[config.current]) != true) {
                    config.current = null
                    plugin.configManager.save(config, path)
                }
            }

            // 一致性检查
            val currentPrefix = chat.getPlayerPrefix(player.world.name, player)
            if (config.current == null) {
                if (currentPrefix.isNotBlank()) {
                    chat.setPlayerPrefix(player.world.name, player, "")
                }
            } else {
                val p = "${globalPrefix[config.current]!![config.available[config.current]]!!}§r"
                if (p != currentPrefix) {
                    plugin.chat!!.setPlayerPrefix(player.world.name, player, p)
                }
            }

            logger.debug("Checked prefix for player ${player.name}")
        } catch (e: Exception) {
            logger.error(LanguageKeys.Log.Error.PREFIX_ERROR, e)
        }
    }

    // ===== 全局前缀管理 =====

    /**
     * 获取全局前缀
     */
    fun getGlobalPrefix(namespace: String, key: String): String? =
        globalPrefix[namespace]?.get(key)

    /**
     * 设置全局前缀
     */
    fun setGlobalPrefix(namespace: String, key: String, value: String, save: Boolean = true) {
        globalPrefix.computeIfAbsent(namespace) { mutableMapOf() }[key] = value
        if (save) {
            plugin.configManager.save(globalPrefix, "prefix-config.yml")
        }
        logger.info(LanguageKeys.Log.Info.PREFIX_UPDATED, "$namespace:$key")
    }

    /**
     * 移除全局前缀
     */
    fun removeGlobalPrefix(namespace: String, key: String, save: Boolean = true) {
        globalPrefix[namespace]?.remove(key)
        if (globalPrefix[namespace]?.isEmpty() == true) {
            globalPrefix.remove(namespace)
        }
        if (save) {
            plugin.configManager.save(globalPrefix, "prefix-config.yml")
        }
        logger.info("Removed global prefix: $namespace:$key")
    }

    /*
     * 移除全局前缀命名空间
     */
    fun removeGlobalPrefixNamespace(namespace: String, save: Boolean = true) {
        globalPrefix.remove(namespace)
        if (save) {
            plugin.configManager.save(globalPrefix, "prefix-config.yml")
        }
        logger.info("Removed global prefix namespace: $namespace")
    }

    // ===== 玩家前缀管理 =====

    /**
     * 设置玩家前缀
     */
    fun setPlayerPrefix(player: OfflinePlayer, namespace: String, key: String, activate: Boolean = false) {
        val path = "player-prefix/${player.uniqueId}.yml"
        plugin.configManager.touchWithMerge(path, "player-prefix-template.yml", createBackup = false)
        val config = plugin.configManager.parse<PlayerPrefixConfig>(path)
        config.available[namespace] = key
        if (activate) config.current = namespace
        plugin.configManager.save(config, path)

        // 如果玩家在线且需要激活，立即检查
        if (activate && player.isOnline) {
            checkPlayer(player.player!!)
        }
    }

    /**
     * 移除玩家前缀
     */
    fun removePlayerPrefix(player: OfflinePlayer, namespace: String) {
        val path = "player-prefix/${player.uniqueId}.yml"
        plugin.configManager.touchWithMerge(path, "player-prefix-template.yml", createBackup = false)
        val config = plugin.configManager.parse<PlayerPrefixConfig>(path)
        config.available.remove(namespace)
        if (config.current == namespace) config.current = null
        plugin.configManager.save(config, path)

        // 如果玩家在线，立即检查
        if (player.isOnline) {
            checkPlayer(player.player!!)
        }
    }

    /**
     * 激活玩家前缀
     */
    fun activatePlayerPrefix(player: OfflinePlayer, namespace: String) {
        val path = "player-prefix/${player.uniqueId}.yml"
        plugin.configManager.touchWithMerge(path, "player-prefix-template.yml", createBackup = false)
        val config = plugin.configManager.parse<PlayerPrefixConfig>(path)
        if (config.available.containsKey(namespace) && config.current != namespace) {
            config.current = namespace
            plugin.configManager.save(config, path)

            // 如果玩家在线，立即检查
            if (player.isOnline) {
                checkPlayer(player.player!!)
            }
        }
    }

    /**
     * 禁用玩家前缀
     */
    fun deactivatePlayerPrefix(player: OfflinePlayer) {
        val path = "player-prefix/${player.uniqueId}.yml"
        plugin.configManager.touchWithMerge(path, "player-prefix-template.yml", createBackup = false)
        val config = plugin.configManager.parse<PlayerPrefixConfig>(path)
        config.current = null
        plugin.configManager.save(config, path)

        // 如果玩家在线，立即检查
        if (player.isOnline) {
            checkPlayer(player.player!!)
        }
    }

    // ===== 数据访问方法 =====

    /**
     * 获取所有全局前缀
     */
    fun getAllGlobalPrefixes(): Map<String, Map<String, String>> {
        return globalPrefix.toMap()
    }

    /**
     * 获取玩家前缀配置
     */
    fun getPlayerPrefixConfig(player: org.bukkit.OfflinePlayer): city.newnan.newnanmain.config.PlayerPrefixConfig {
        val path = "player-prefix/${player.uniqueId}.yml"
        plugin.configManager.touchWithMerge(path, "player-prefix-template.yml", createBackup = false)
        return plugin.configManager.parse<city.newnan.newnanmain.config.PlayerPrefixConfig>(path)
    }
}
