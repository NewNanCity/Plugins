package city.newnan.createarea.modules

import city.newnan.config.extensions.configManager
import city.newnan.core.base.BaseModule
import city.newnan.core.event.subscribeEvent
import city.newnan.createarea.CreateAreaPlugin
import city.newnan.createarea.config.CreateArea
import city.newnan.createarea.config.CreateAreas
import city.newnan.createarea.config.Range2D
import city.newnan.createarea.i18n.LanguageKeys
import net.milkbowl.vault.permission.Permission
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.dynmap.DynmapCommonAPI
import org.dynmap.DynmapCommonAPIListener
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 创造区域管理器
 *
 * 负责管理所有创造区域相关功能：
 * - 区域的创建、删除、查询
 * - 玩家权限组管理
 * - Dynmap集成
 * - 世界切换处理
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class CreateAreaManager(
    moduleName: String,
    val plugin: CreateAreaPlugin
) : BaseModule(moduleName, plugin) {

    // 重要：手动调用init()来触发初始化
    init { init() }

    // 创造区域数据
    private val builders = ConcurrentHashMap<OfflinePlayer, Range2D>()

    // 外部服务
    private var dynmap: DynmapCommonAPI? = null
    private var permission: Permission? = null

    var dynmapPluginEnableLister: DynmapCommonAPIListener? = null

    override fun onInit() {
        // 初始化Vault权限系统
        try {
            if (plugin.isPluginPresent("Vault")) {
                permission = plugin.getService(Permission::class.java)
                if (permission != null) {
                    logger.info("Vault permission system connected")
                } else {
                    logger.warning(LanguageKeys.Log.Warning.VAULT_NOT_FOUND)
                }
            } else {
                logger.warning(LanguageKeys.Log.Warning.VAULT_NOT_FOUND)
            }
        } catch (e: Exception) {
            logger.error(LanguageKeys.Log.Error.VAULT_ERROR, e)
        }

        // 初始化Dynmap集成
        try {
            dynmapPluginEnableLister = object : DynmapCommonAPIListener() {
                override fun apiEnabled(api: DynmapCommonAPI) {
                    dynmap = api
                    logger.info(LanguageKeys.Log.Info.DYNMAP_CONNECTED)
                    syncAreas()
                }
            }
            DynmapCommonAPIListener.register(dynmapPluginEnableLister)
        } catch (e: Exception) {
            logger.error(LanguageKeys.Log.Error.DYNMAP_ERROR, e)
        }

        // 注册事件监听器
        subscribeEvent<PlayerJoinEvent> {
            priority(EventPriority.MONITOR)
            handler { event ->
                checkPlayer(event.player)
            }
        }

        // 问题：每个世界的状态可能不一样
//        subscribeEvent<PlayerChangedWorldEvent> {
//            priority(EventPriority.MONITOR)
//            handler { event ->
//                val config = plugin.getPluginConfig()
//                if (event.player.world.name != config.world && event.from.name == config.world) {
//                    // 离开创造世界时重置玩家状态
//                    event.player.gameMode = GameMode.SURVIVAL
//                    event.player.allowFlight = false
//                    event.player.isFlying = false
//                    event.player.flySpeed = 0.2f
//                    event.player.walkSpeed = 0.2f
//                    event.player.isHealthScaled = false
//                }
//            }
//        }
    }

    override fun onReload() {
        // 重新加载区域数据
        loadAreas()

        // 同步到Dynmap
        syncAreas()
    }

    override fun onClose() {
        if (dynmapPluginEnableLister != null) {
            try {
                DynmapCommonAPIListener.unregister(dynmapPluginEnableLister)
            } catch (e: Exception) {
                logger.error(LanguageKeys.Log.Error.DYNMAP_ERROR, e)
            }
            dynmapPluginEnableLister = null
        }
    }

    /**
     * 加载区域数据
     */
    private fun loadAreas() {
        try {
            plugin.configManager.touch("areas.yml")
            val areas = plugin.configManager.parse<CreateAreas>("areas.yml")

            builders.clear()
            areas.forEach { (uuid, area) ->
                val player = Bukkit.getOfflinePlayer(uuid)
                builders[player] = Range2D.valueOf(area.x1, area.x2, area.z1, area.z2)
            }

            logger.info("Loaded ${builders.size} create areas")
        } catch (e: Exception) {
            logger.error("Failed to load areas", e)
        }
    }

    /**
     * 保存区域数据
     */
    private fun saveAreas() {
        try {
            val buildersConfigMap = mutableMapOf<UUID, CreateArea>()
            builders.forEach { (player, range2D) ->
                buildersConfigMap[player.uniqueId] = CreateArea(
                    range2D.minX,
                    range2D.minZ,
                    range2D.maxX,
                    range2D.maxZ
                )
            }
            plugin.configManager.save(buildersConfigMap, "areas.yml")
        } catch (e: Exception) {
            logger.error("Failed to save areas", e)
        }
    }

    /**
     * 同步区域到Dynmap
     */
    fun syncAreas() {
        val config = plugin.getPluginConfig()
        if (!config.dynmapSettings.enabled || dynmap == null) return

        try {
            val markerSet = dynmap!!.markerAPI.getMarkerSet(config.dynmapSettings.markerSetId) ?:
                dynmap!!.markerAPI.createMarkerSet(
                    config.dynmapSettings.markerSetId,
                    config.dynmapSettings.markerSetLabel,
                    null,
                    false
                )

            val existIds = markerSet.areaMarkers.map { it.markerID }.toMutableSet()

            builders.forEach { (player, range) ->
                val id = player.uniqueId.toString()
                val exist = markerSet.findAreaMarker(id)
                val minX = range.minX.toDouble()
                val maxX = range.maxX.toDouble()
                val minZ = range.minZ.toDouble()
                val maxZ = range.maxZ.toDouble()

                if (exist == null) {
                    markerSet.createAreaMarker(
                        id,
                        "${player.name ?: "未知玩家"}的创造区",
                        false,
                        config.world,
                        doubleArrayOf(minX, minX, maxX, maxX),
                        doubleArrayOf(minZ, maxZ, maxZ, minZ),
                        false
                    )
                } else {
                    exist.label = "${player.name ?: "未知玩家"}的创造区"
                    exist.setCornerLocations(
                        doubleArrayOf(minX, minX, maxX, maxX),
                        doubleArrayOf(minZ, maxZ, maxZ, minZ)
                    )
                }
                existIds.remove(id)
            }

            // 删除不存在的区域标记
            existIds.forEach { markerSet.findAreaMarker(it)?.deleteMarker() }

            logger.info("Dynmap markers synced successfully")
        } catch (e: Exception) {
            logger.error(LanguageKeys.Log.Error.DYNMAP_ERROR, e)
        }
    }

    /**
     * 检查玩家权限组
     */
    private fun checkPlayer(player: OfflinePlayer) {
        val bukkitPlayer = player.player ?: return
        val config = plugin.getPluginConfig()

        if (permission == null) return

        try {
            val primaryGroup = permission!!.getPrimaryGroup(config.world, bukkitPlayer)

            if (primaryGroup == config.defaultGroup && builders.containsKey(player)) {
                // 玩家有区域但在默认组，升级到建造者组
                permission!!.playerRemoveGroup(config.world, bukkitPlayer, config.defaultGroup)
                permission!!.playerAddGroup(config.world, bukkitPlayer, config.builderGroup)
                plugin.messager.printf(bukkitPlayer, "您的创造区已创建, 使用 <gold>/ctp</gold> 传送到自己的创造区, 使用 <blue>//wand</blue> 使用小木斧")
            } else if (primaryGroup == config.builderGroup && !builders.containsKey(player) && !bukkitPlayer.hasPermission("createarea.bypass")) {
                // 玩家在建造者组但没有区域，降级到默认组
                permission!!.playerRemoveGroup(config.world, bukkitPlayer, config.builderGroup)
                permission!!.playerAddGroup(config.world, bukkitPlayer, config.defaultGroup)
                plugin.messager.printf(bukkitPlayer, "<red>您的创造区已被移除，无法在创造世界建造!</red>")
            }
        } catch (e: Exception) {
            logger.error(LanguageKeys.Log.Error.VAULT_ERROR, e)
        }
    }

    // ===== 公共API =====

    /**
     * 获取所有区域
     */
    fun getAllAreas(): Map<OfflinePlayer, Range2D> = builders.toMap()

    /**
     * 获取玩家的区域
     */
    fun getPlayerArea(player: OfflinePlayer): Range2D? = builders[player]

    /**
     * 更新玩家区域
     */
    fun updateArea(player: OfflinePlayer, x1: Int, x2: Int, z1: Int, z2: Int) {
        builders[player] = Range2D.valueOf(x1, x2, z1, z2)
        syncAreas()
        saveAreas()
        checkPlayer(player)
        logger.info(LanguageKeys.Log.Info.AREA_CREATED, player.name ?: "Unknown")
    }

    /**
     * 删除玩家区域
     */
    fun deleteArea(player: OfflinePlayer): Boolean {
        return builders.remove(player)?.let {
            syncAreas()
            saveAreas()
            checkPlayer(player)
            logger.info(LanguageKeys.Log.Info.AREA_DELETED, player.name ?: "Unknown")
            true
        } ?: false
    }

    /**
     * 传送到区域
     */
    fun teleportToArea(player: Player, area: Range2D): Boolean {
        val config = plugin.getPluginConfig()
        val world = Bukkit.getWorld(config.world)

        if (world == null) {
            plugin.messager.printf(player, LanguageKeys.Core.Error.WORLD_NOT_FOUND, config.world)
            return false
        }

        try {
            val location = Location(
                world,
                area.minX.toDouble() + 0.5,
                world.getHighestBlockYAt(area.minX, area.minZ).toDouble() + 0.3,
                area.minZ.toDouble() + 0.5
            )
            player.teleport(location)
            return true
        } catch (e: Exception) {
            logger.error("Failed to teleport player ${player.name}", e)
            return false
        }
    }
}
