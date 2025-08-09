package city.newnan.createarea.utils

import city.newnan.createarea.config.Range2D
import city.newnan.createarea.config.Range2DWorld
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.bukkit.WorldEditPlugin
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * WorldEdit工具类
 *
 * 提供与WorldEdit插件的集成功能
 *
 * @author NewNanCity
 * @since 2.0.0
 */
object WorldEditUtils {

    /**
     * 检查WorldEdit是否可用
     */
    fun isWorldEditAvailable(): Boolean {
        return Bukkit.getPluginManager().getPlugin("WorldEdit") is WorldEditPlugin
    }

    /**
     * 获取玩家的WorldEdit选择
     */
    fun getPlayerSelection(player: Player): Range2DWorld? {
        if (!isWorldEditAvailable()) return null

        return try {
            val worldEdit = Bukkit.getPluginManager().getPlugin("WorldEdit") as WorldEditPlugin
            val session = worldEdit.getSession(player)
            val region = session.getSelection(BukkitAdapter.adapt(player.world)) ?: return null
            
            val min = region.minimumPoint
            val max = region.maximumPoint
            val world = Bukkit.getWorld(region.world!!.name)!!
            
            Range2DWorld(
                world.name,
                Range2D(min.x, min.z, max.x, max.z)
            )
        } catch (e: Exception) {
            null
        }
    }
}
