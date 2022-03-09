package city.newnan.mcpatch.addon

import city.newnan.mcpatch.MCPatch
import me.lucko.helper.Events
import org.bukkit.Material
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockPlaceEvent


/**
 * 防止发射器在z=0向下和z=255向上时发射崩服
 * (解决思路：解决出问题的人)
 */
object DispenserPatch {
    fun init() {
        Events.subscribe(BlockPlaceEvent::class.java, EventPriority.MONITOR)
            .filter { it.block.type == Material.DISPENSER }
            .filter { it.block.y % 255 == 0 }
            .handler { event: BlockPlaceEvent ->
                event.isCancelled = true
                MCPatch.messageManager?.printf(event.player, "此处不可放置发射器!")
                event.block.type = Material.AIR
            }
    }
}