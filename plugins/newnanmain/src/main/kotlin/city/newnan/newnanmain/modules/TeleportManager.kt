package city.newnan.newnanmain.modules

import city.newnan.config.extensions.configManager
import city.newnan.core.base.BaseModule
import city.newnan.core.scheduler.runSyncLater
import city.newnan.newnanmain.NewNanMainPlugin
import city.newnan.newnanmain.config.TeleportPoint
import city.newnan.newnanmain.i18n.LanguageKeys
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player

/**
 * 传送管理器
 *
 * 负责管理传送点系统，包括：
 * - 传送点配置管理
 * - 权限检查
 * - 传送执行
 * - 冷却时间管理
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class TeleportManager(
    moduleName: String,
    val plugin: NewNanMainPlugin
) : BaseModule(moduleName, plugin) {

    // ===== 模块初始化 =====
    init { init() }

    // ===== 配置缓存 =====
    private var points = mutableMapOf<String, TeleportPoint>()
    private var enableTeleport = true
    private var teleportDelay = 0
    private var teleportCooldown = 0

    // ===== 冷却时间管理 =====
    private val cooldowns = mutableMapOf<String, Long>()

    override fun onReload() {
        // 重新加载配置
        loadConfigurations()
    }

    /**
     * 加载配置
     */
    private fun loadConfigurations() {
        // 加载插件配置
        val config = plugin.getPluginConfig()
        enableTeleport = config.teleportSettings.enableTeleport
        teleportDelay = config.teleportSettings.teleportDelay
        teleportCooldown = config.teleportSettings.teleportCooldown

        // 加载传送点配置
        plugin.configManager.touchWithMerge("teleport.yml", "teleport-template.yml", createBackup = false)
        val pointsImport = plugin.configManager.parse<List<TeleportPoint>>("teleport.yml")
        points.clear()
        pointsImport.forEach { points[it.name] = it }

        logger.info("Loaded ${points.size} teleport points")
    }

    /**
     * 保存传送点配置
     */
    fun save() {
        plugin.configManager.save(points.values.toList(), "teleport.yml")
    }

    /**
     * 获取玩家可用的传送点
     */
    fun getTeleportPoints(player: Player): List<TeleportPoint> {
        if (!enableTeleport) return emptyList()

        return points.filterValues { point ->
            point.permission == null || player.hasPermission(point.permission)
        }.values.toList()
    }

    /**
     * 获取所有传送点
     */
    fun getAllTeleportPoints(): List<TeleportPoint> = points.values.toList()

    /**
     * 添加传送点
     */
    fun addTeleportPoint(point: TeleportPoint): Boolean {
        points[point.name] = point
        save()
        logger.info("Added teleport point: ${point.name}")
        return true
    }

    /**
     * 移除传送点
     */
    fun removeTeleportPoint(name: String): Boolean {
        if (points.remove(name) != null) {
            save()
            logger.info("Removed teleport point: $name")
            return true
        }
        return false
    }

    /**
     * 根据名称获取传送点
     */
    fun getTeleportPoint(name: String): TeleportPoint? = points[name]

    /**
     * 执行传送
     */
    fun teleportPlayer(player: Player, pointName: String): Boolean {
        if (!enableTeleport) {
            plugin.messager.printf(player, LanguageKeys.Teleport.FAILED, "传送功能已禁用")
            return false
        }

        // 检查冷却时间
        if (teleportCooldown > 0) {
            val lastTeleport = cooldowns[player.name] ?: 0
            val remaining = (lastTeleport + teleportCooldown * 1000) - System.currentTimeMillis()
            if (remaining > 0) {
                plugin.messager.printf(player, LanguageKeys.Teleport.COOLDOWN, remaining / 1000)
                return false
            }
        }

        // 查找传送点
        val point = getTeleportPoint(pointName)
        if (point == null) {
            plugin.messager.printf(player, LanguageKeys.Teleport.POINT_NOT_FOUND, pointName)
            return false
        }

        // 检查权限
        if (point.permission != null && !player.hasPermission(point.permission)) {
            plugin.messager.printf(player, LanguageKeys.Teleport.NO_PERMISSION)
            return false
        }

        // 检查世界是否存在
        val world = Bukkit.getWorld(point.world)
        if (world == null) {
            plugin.messager.printf(player, LanguageKeys.Core.Error.WORLD_NOT_FOUND, point.world)
            return false
        }

        try {
            // 创建传送位置
            val location = Location(world, point.x.toDouble() + 0.5, point.y.toDouble(), point.z.toDouble() + 0.5)

            // 执行传送
            if (teleportDelay > 0) {
                // 延迟传送
                plugin.messager.printf(player, "传送将在 $teleportDelay 秒后执行...")
                runSyncLater(teleportDelay * 20L) { _ ->
                    if (player.isOnline) {
                        player.teleport(location)
                        plugin.messager.printf(player, LanguageKeys.Teleport.SUCCESS, point.name)

                        // 设置冷却时间
                        if (teleportCooldown > 0) {
                            cooldowns[player.name] = System.currentTimeMillis()
                        }

                        logger.info(LanguageKeys.Log.Info.TELEPORT_EXECUTED, player.name, point.name)
                    }
                    Unit // 返回Unit类型
                }
            } else {
                // 立即传送
                player.teleport(location)
                plugin.messager.printf(player, LanguageKeys.Teleport.SUCCESS, point.name)

                // 设置冷却时间
                if (teleportCooldown > 0) {
                    cooldowns[player.name] = System.currentTimeMillis()
                }

                logger.info(LanguageKeys.Log.Info.TELEPORT_EXECUTED, player.name, point.name)
            }

            return true
        } catch (e: Exception) {
            logger.error(LanguageKeys.Log.Error.TELEPORT_ERROR, e)
            plugin.messager.printf(player, LanguageKeys.Teleport.FAILED, e.message ?: "未知错误")
            return false
        }
    }

    /**
     * 清理过期的冷却时间
     */
    fun cleanupCooldowns() {
        val now = System.currentTimeMillis()
        val expiredKeys = cooldowns.filter { (_, time) ->
            now - time > teleportCooldown * 1000
        }.keys

        expiredKeys.forEach { cooldowns.remove(it) }
    }

    /**
     * 获取玩家剩余冷却时间（秒）
     */
    fun getRemainingCooldown(player: Player): Long {
        if (teleportCooldown <= 0) return 0

        val lastTeleport = cooldowns[player.name] ?: return 0
        val remaining = (lastTeleport + teleportCooldown * 1000) - System.currentTimeMillis()
        return if (remaining > 0) remaining / 1000 else 0
    }
}
