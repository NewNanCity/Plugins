package city.newnan.newnanmain

import com.fasterxml.jackson.annotation.JsonInclude
import me.lucko.helper.Events
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.terminable.Terminable
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PlayerPrefixConfig(
    val available: MutableMap<String, String> = mutableMapOf(),
    var current: String? = null
)

typealias GlobalPrefixConfig = MutableMap<String, MutableMap<String, String>>

class PrefixManager(val plugin: PluginMain) : Terminable {
    var globalPrefix = mutableMapOf<String, MutableMap<String, String>>()

    init { bindWith(plugin) }

    fun enable() {
        Events.subscribe(PlayerJoinEvent::class.java, EventPriority.MONITOR)
            .handler { checkPlayer(it.player) }
            .bindWith(plugin)
        Events.subscribe(PlayerChangedWorldEvent::class.java, EventPriority.MONITOR)
            .handler { checkPlayer(it.player) }
            .bindWith(plugin)
    }

    fun reload() {
        plugin.configManager touch "prefix-config.yml"
        globalPrefix = plugin.configManager.parse<GlobalPrefixConfig>("prefix-config.yml", saveToCache = false, useCacheIfPossible = false)
    }

    override fun close() {}

    fun checkPlayer(player: Player) {
        val currentPrefix = plugin.chat.getPlayerPrefix(player.world.name, player)
        val path = "player-prefix/${player.uniqueId}.yml"
        plugin.configManager.touch(path, { PlayerPrefixConfig() })
        val config = plugin.configManager.parse<PlayerPrefixConfig>(path)

        // 可用性检查
        if (config.current != null) {
            if (!config.available.containsKey(config.current)) {
                config.current = null
                plugin.configManager.save(config, path)
            }
            else if (globalPrefix[config.current]?.containsKey(config.available[config.current]) != true) {
                config.current = null
                plugin.configManager.save(config, path)
            }
        }

        // 一致性检查
        if (config.current == null) {
            if (currentPrefix.isNotBlank()) {
                plugin.chat.setPlayerPrefix(player.world.name, player, "")
            }
        } else {
            val p = "${globalPrefix[config.current]!![config.available[config.current]]!!}§r"
            if (p != currentPrefix) {
                plugin.chat.setPlayerPrefix(player.world.name, player, p)
            }
        }
    }

    fun getGlobalPrefix(namespace: String, key: String): String? =
        globalPrefix[namespace]?.get(key)

    fun setGlobalPrefix(namespace: String, key: String, value: String, save: Boolean = true) {
        globalPrefix.computeIfAbsent(namespace) { mutableMapOf() }[key] = value
        if (save) plugin.configManager.save(globalPrefix, "prefix-config.yml", saveToCache = false)
    }

    fun removeGlobalPrefix(namespace: String, key: String, save: Boolean = true) {
        globalPrefix[namespace]?.remove(key)
        if (globalPrefix[namespace]?.isEmpty() == true) globalPrefix.remove(namespace)
        if (save) plugin.configManager.save(globalPrefix, "prefix-config.yml", saveToCache = false)
    }

    fun setPlayerPrefix(player: OfflinePlayer, namespace: String, key: String, activate: Boolean = false) {
        val path = "player-prefix/${player.uniqueId}.yml"
        plugin.configManager.touch(path, { PlayerPrefixConfig() })
        val config = plugin.configManager.parse<PlayerPrefixConfig>(path)
        config.available[namespace] = key
        if (activate) config.current = namespace
        plugin.configManager.save(config, path)
    }

    fun removePlayerPrefix(player: OfflinePlayer, namespace: String) {
        val path = "player-prefix/${player.uniqueId}.yml"
        plugin.configManager.touch(path, { PlayerPrefixConfig() })
        val config = plugin.configManager.parse<PlayerPrefixConfig>(path)
        config.available.remove(namespace)
        if (config.current == namespace) config.current = null
        plugin.configManager.save(config, path)
    }

    fun activatePlayerPrefix(player: OfflinePlayer, namespace: String) {
        val path = "player-prefix/${player.uniqueId}.yml"
        plugin.configManager.touch(path, { PlayerPrefixConfig() })
        val config = plugin.configManager.parse<PlayerPrefixConfig>(path)
        if (config.available.containsKey(namespace) && config.current != namespace) {
            config.current = namespace
            plugin.configManager.save(config, path)
        }
    }
}