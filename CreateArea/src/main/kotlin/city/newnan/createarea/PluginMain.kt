package city.newnan.createarea

import city.newnan.createarea.config.ConfigFile
import city.newnan.createarea.config.CreateArea
import city.newnan.createarea.config.CreateAreas
import city.newnan.violet.config.ConfigManager2
import city.newnan.violet.gui.GuiManager
import city.newnan.violet.message.MessageManager
import co.aikar.commands.PaperCommandManager
import me.lucko.helper.Events
import me.lucko.helper.plugin.ExtendedJavaPlugin
import net.milkbowl.vault.permission.Permission
import org.bukkit.OfflinePlayer
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.dynmap.DynmapCommonAPI
import org.dynmap.DynmapCommonAPIListener
import java.util.*

data class Range2D(val minX: Int, val minZ: Int, val maxX: Int, val maxZ: Int) {
    companion object {
        fun valueOf(x1: Int, x2: Int, z1: Int, z2: Int): Range2D {
            return Range2D(minOf(x1, x2), minOf(z1, z2), maxOf(x1, x2), maxOf(z1, z2))
        }
    }
}

const val MARKER_SET_ID = "NewNanCity.CreateArea"

class PluginMain : ExtendedJavaPlugin() {
    companion object {
        lateinit var INSTANCE: PluginMain
            private set
    }
    init { INSTANCE = this }
    private val configManager: ConfigManager2 by lazy {
        ConfigManager2(this).apply {
            setCache(ConfigManager2.CacheType.None)
        }
    }
    val guiManager: GuiManager by lazy { GuiManager(this) }
    val messageManager: MessageManager by lazy { MessageManager(this) }
    private var dynmap: DynmapCommonAPI? = null
    private lateinit var permission: Permission
    private val commandManager: PaperCommandManager by lazy { PaperCommandManager(this) }

    var createWorld: String = ""
    private var defaultGroup: String = ""
    private var builderGroup: String = ""
    val builders = mutableMapOf<OfflinePlayer, Range2D>()

    override fun enable() {
        reload()
        commandManager.enableUnstableAPI("help")
        commandManager.registerCommand(Commands)
        commandManager.locales.setDefaultLocale(Locale.SIMPLIFIED_CHINESE)

        messageManager setPlayerPrefix "§7[§6牛腩创造§7] §f"
        DynmapCommonAPIListener.register(object : DynmapCommonAPIListener() {
            override fun apiEnabled(api: DynmapCommonAPI) {
                dynmap = api
                syncAreas()
            }
        })
        if (server.pluginManager.getPlugin("Vault") == null) throw Exception("Vault not found!")
        permission = server.servicesManager.getRegistration(Permission::class.java)?.provider
            ?: throw Exception("Vault permission service not found!")
        Events.subscribe(PlayerJoinEvent::class.java, EventPriority.MONITOR)
            .handler { checkPlayer(it.player) }
            .bindWith(this)
        Events.subscribe(PlayerChangedWorldEvent::class.java, EventPriority.MONITOR)
            .filter { it.from.name == createWorld }
            .handler { it.player.gameMode = org.bukkit.GameMode.SURVIVAL }
            .bindWith(this)
    }

    override fun disable() {
        commandManager.unregisterCommands()
    }

    fun reload() {
        // config.yml
        configManager touch "config.yml"
        val config = configManager.parse<ConfigFile>("config.yml")
        createWorld = config.world
        defaultGroup = config.defaultGroup
        builderGroup = config.builderGroup

        configManager touch "areas.yml"
        builders.clear()
        configManager.parse<CreateAreas>("areas.yml").forEach { (uuid, area) ->
            val p = server.getOfflinePlayer(uuid)
            if (!p.hasPlayedBefore() || p.name == null) return@forEach
            builders[server.getOfflinePlayer(uuid)] = Range2D.valueOf(area.x1, area.x2, area.z1, area.z2)
        }
    }

    private fun save() {
        val buildersConfigMap = mutableMapOf<UUID, CreateArea>()
        builders.forEach { (player, range2D) ->
            if (player.name == null) return@forEach
            buildersConfigMap[player.uniqueId] = CreateArea(
                range2D.minX,
                range2D.minZ,
                range2D.maxX,
                range2D.maxZ,
            )
        }
        configManager.save(buildersConfigMap, "areas.yml")
    }

    /**
     * 同步创造区
     */
    fun syncAreas() {
        if (dynmap == null) return
        val makerSet = dynmap!!.markerAPI.getMarkerSet(MARKER_SET_ID) ?:
            dynmap!!.markerAPI.createMarkerSet(MARKER_SET_ID, "创造区", null, false)
        val existIds = makerSet.areaMarkers.map { it.markerID }.toMutableSet()
        builders.forEach {
            val id = it.key.uniqueId.toString()
            val exist = makerSet.findAreaMarker(id)
            val minX = it.value.minX.toDouble()
            val maxX = it.value.maxX.toDouble()
            val minZ = it.value.minZ.toDouble()
            val maxZ = it.value.maxZ.toDouble()
            if (exist == null) {
                makerSet.createAreaMarker(id, "${it.key.name!!}的创造区", false, createWorld,
                    doubleArrayOf(minX, minX, maxX, maxX), doubleArrayOf(minZ, maxZ, maxZ, minZ), false)
            } else {
                exist.label = "${it.key.name!!}的创造区"
                exist.setCornerLocations(doubleArrayOf(minX, minX, maxX, maxX), doubleArrayOf(minZ, maxZ, maxZ, minZ))
            }
            existIds.remove(id)
        }
        existIds.forEach { makerSet.findAreaMarker(it).deleteMarker() }
    }

    private fun checkPlayer(p: OfflinePlayer) {
        val player = p.player ?: return
        val pp = permission.getPrimaryGroup(createWorld, player)
        if (pp == defaultGroup && builders.containsKey(p)) {
            permission.playerRemoveGroup(createWorld, player, defaultGroup)
            permission.playerAddGroup(createWorld, player, builderGroup)
            messageManager.printf(player, "您的创造区已创建, 使用 §6/ctp§r 传送到自己的创造区, 使用 §9//wand§r 使用小木斧")
        } else if (pp == builderGroup && !builders.containsKey(p) && !player.hasPermission("createarea.bypass")) {
            permission.playerRemoveGroup(createWorld, player, builderGroup)
            permission.playerAddGroup(createWorld, player, defaultGroup)
            messageManager.printf(player, "您你的创造区已被移除，无法在奎特世界建造!")
        }
    }

    fun updateArea(player: OfflinePlayer, x1: Int, x2: Int, z1: Int, z2: Int) {
        builders[player] = Range2D.valueOf(x1, x2, z1, z2)
        syncAreas()
        save()
        checkPlayer(player)
    }

    fun deleteArea(player: OfflinePlayer) {
        builders.remove(player)?.also {
            syncAreas()
            save()
            checkPlayer(player)
        }
    }
}
