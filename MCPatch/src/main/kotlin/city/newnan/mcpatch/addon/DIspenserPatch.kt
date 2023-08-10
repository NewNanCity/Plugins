package city.newnan.mcpatch.addon

import city.newnan.mcpatch.PluginMain
import me.lucko.helper.Events
import org.bukkit.Material
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockPlaceEvent


/**
 * 防止发射器在z=0向下和z=255向上时发射崩服
 * (解决思路：解决出问题的人)
 */
object DispenserPatch: IMCPatchAddon {
    override val addonName = "DispenserPatch"

    override fun enable() {
        Events.subscribe(BlockPlaceEvent::class.java, EventPriority.MONITOR)
            .filter { it.block.type == Material.DISPENSER }
            .filter { it.block.y % 255 == 0 }
            .handler { event: BlockPlaceEvent ->
                event.isCancelled = true
                PluginMain.INSTANCE.messageManager.printf(event.player, "此处不可放置发射器!")
                event.block.type = Material.AIR
            }
            .bindWith(PluginMain.INSTANCE)
    }

    override fun close() {}
}