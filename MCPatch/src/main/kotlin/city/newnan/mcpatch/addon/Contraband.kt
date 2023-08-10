package city.newnan.mcpatch.addon

import city.newnan.mcpatch.PluginMain
import me.lucko.helper.Events
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.inventory.InventoryType


object Contraband: IMCPatchAddon {
    override val addonName = "Contraband"

    override fun enable() {
        Events.subscribe(InventoryOpenEvent::class.java, EventPriority.LOWEST)
            .filter { it.player.gameMode != GameMode.CREATIVE }
            .filter { !it.player.hasPermission("mcpatch.contraband.bypass") }
            .filter { checkInventories.contains(it.inventory.type) }
            .handler { event: InventoryOpenEvent ->
                val inventory = event.inventory
                val size = inventory.size
                for (i in 0 until size) {
                    val itemStack = inventory.getItem(i)
                    if (!materials.contains(itemStack!!.type)) continue
                    PluginMain.INSTANCE.messageManager.let {
                        it.printf(event.player, "违禁品 {0} 会被删除", itemStack.type.name)
                        it.printf("玩家 {0} 的违禁品 {1} 会被删除", event.player, itemStack.type.name)
                    }
                }
            }
            .bindWith(PluginMain.INSTANCE)
    }

    override fun close() {}

    private val checkInventories: HashSet<InventoryType> = hashSetOf(
        InventoryType.CHEST,
        InventoryType.ENDER_CHEST,
        InventoryType.PLAYER,
        InventoryType.SHULKER_BOX,
    )
    private val materials: HashSet<Material> = hashSetOf(
        Material.BEDROCK,
        Material.BARRIER,
        Material.END_CRYSTAL,
        Material.END_PORTAL,
        Material.END_PORTAL_FRAME,
        Material.END_GATEWAY,
        Material.AIR,
        Material.WATER,
        Material.LAVA,
    )
}