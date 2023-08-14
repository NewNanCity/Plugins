package city.newnan.railarea.utils

import city.newnan.railarea.octree.Point3D
import city.newnan.railarea.octree.Range3D
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.bukkit.WorldEditPlugin
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player

class Range3DWorld(val world: World, val range: Range3D)
class Point3DWorld(val world: World, val point: Point3D)

fun Player.getSelection(): Range3DWorld?  {
    try {
        val worldEdit = server.pluginManager.getPlugin("WorldEdit") ?: return null
        if (worldEdit is WorldEditPlugin) {
            val region = worldEdit.getSession(player).getSelection(BukkitAdapter.adapt(world)) ?: return null
            val min = region.minimumPoint
            val max = region.maximumPoint
            return Range3DWorld(Bukkit.getWorld(region.world!!.name)!!, Range3D(min.x, min.y, min.z, max.x, max.y, max.z))
        }
    } catch (e: Exception) { return null }
    return null
}

fun Player.getPoint(): Point3DWorld? {
    try {
        val worldEdit = server.pluginManager.getPlugin("WorldEdit") ?: return null
        if (worldEdit is WorldEditPlugin) {
            val region = worldEdit.getSession(player).getSelection(BukkitAdapter.adapt(world)) ?: return null
            val min = region.minimumPoint
            val max = region.maximumPoint
            if (min.x != max.x || min.y != max.y || min.z != max.z) return null
            return Point3DWorld(Bukkit.getWorld(region.world!!.name)!!, Point3D(min.x, min.y, min.z))
        }
    } catch (e: Exception) { return null }
    return null
}