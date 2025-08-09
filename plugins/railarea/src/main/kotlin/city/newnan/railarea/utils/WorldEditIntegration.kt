package city.newnan.railarea.utils

import city.newnan.config.serializers.BlockPosition
import city.newnan.config.serializers.BlockRegion
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.bukkit.WorldEditPlugin
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player

/**
 * WorldEdit集成工具类
 * 
 * 提供与WorldEdit插件的集成功能，支持区域选择和停靠点选择
 */

/**
 * 区域选择结果
 */
data class RegionSelection(val world: World, val region: BlockRegion)

/**
 * 点选择结果
 */
data class PointSelection(val world: World, val point: BlockPosition)

/**
 * 获取玩家的WorldEdit区域选择
 * 
 * @return 区域选择结果，如果没有选择或出错则返回null
 */
fun Player.getWorldEditSelection(): RegionSelection? {
    try {
        val worldEdit = server.pluginManager.getPlugin("WorldEdit") ?: return null
        if (worldEdit !is WorldEditPlugin) return null
        
        val session = worldEdit.getSession(this)
        val region = session.getSelection(BukkitAdapter.adapt(world)) ?: return null
        
        val min = region.minimumPoint
        val max = region.maximumPoint
        val bukkitWorld = Bukkit.getWorld(region.world!!.name) ?: return null
        
        val blockRegion = BlockRegion.of(
            BlockPosition.of(min.x, min.y, min.z, bukkitWorld.name),
            BlockPosition.of(max.x, max.y, max.z, bukkitWorld.name)
        )
        
        return RegionSelection(bukkitWorld, blockRegion)
    } catch (e: Exception) {
        return null
    }
}

/**
 * 获取玩家的WorldEdit点选择
 * 
 * @return 点选择结果，如果没有选择单点或出错则返回null
 */
fun Player.getWorldEditPoint(): PointSelection? {
    try {
        val worldEdit = server.pluginManager.getPlugin("WorldEdit") ?: return null
        if (worldEdit !is WorldEditPlugin) return null
        
        val session = worldEdit.getSession(this)
        val region = session.getSelection(BukkitAdapter.adapt(world)) ?: return null
        
        val min = region.minimumPoint
        val max = region.maximumPoint
        
        // 检查是否为单点选择
        if (min.x != max.x || min.y != max.y || min.z != max.z) return null
        
        val bukkitWorld = Bukkit.getWorld(region.world!!.name) ?: return null
        val point = BlockPosition.of(min.x, min.y, min.z, bukkitWorld.name)
        
        return PointSelection(bukkitWorld, point)
    } catch (e: Exception) {
        return null
    }
}

/**
 * 检查WorldEdit插件是否可用
 */
fun isWorldEditAvailable(): Boolean {
    val worldEdit = Bukkit.getPluginManager().getPlugin("WorldEdit")
    return worldEdit != null && worldEdit.isEnabled && worldEdit is WorldEditPlugin
}
