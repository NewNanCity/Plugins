package city.newnan.railarea.gui

import city.newnan.railarea.PluginMain
import city.newnan.railarea.input.handleAreaInput
import city.newnan.railarea.input.handleYesInput
import city.newnan.railarea.utils.visualize
import city.newnan.violet.gui.PlayerGuiSession
import city.newnan.violet.gui.UpdateType
import dev.triumphteam.gui.builder.item.ItemBuilder
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.inventory.ItemStack

fun openFreeAreaGui (session: PlayerGuiSession) {
    val player = session.player
    val key = PluginMain.INSTANCE.unknownStation to PluginMain.INSTANCE.unknownLine
    session.open(pageGui(session, Component.text("§7[§3§l牛腩轨道交通§r§7]§r 游离区域")), { type, gui, _ ->
        gui.clearPageItems()
        PluginMain.INSTANCE.lineStationAreas[key]?.forEach { area ->
            val item = ItemStack(Material.RAIL).also {
                it.itemMeta = it.itemMeta?.also { meta ->
                    meta.setDisplayName("游离区域")
                    meta.lore = listOf(
                        "§r§7世界: §r${area.world.name}§r",
                        "§r§7范围:",
                        "  §r${area.range3D.minX}, ${area.range3D.minY}, ${area.range3D.minZ}§r",
                        "  §r${area.range3D.maxX}, ${area.range3D.maxY}, ${area.range3D.maxZ}§r",
                        "§r§7停靠: §r§f${area.stopPoint.x},${area.stopPoint.y},${area.stopPoint.z} ${area.direction}§r",
                        "",
                        "§6右键: 传送§r",
                        "§6shift+左键: 修改§r",
                        "§6shift+右键: 删除§r",
                    )
                }
            }
            gui.addItem(ItemBuilder.from(item).asGuiItem {
                if (it.isShiftClick) {
                    if (it.isRightClick) {
                        handleYesInput(session, "&c确认删除区域? 回复Y确认, 回复其他取消") { yes ->
                            if (yes) PluginMain.INSTANCE.removeArea(area)
                            session.show()
                        }
                    } else {
                        handleAreaInput(session, area) { newArea ->
                            if (newArea != null) {
                                PluginMain.INSTANCE.updateArea(area, newArea)
                                PluginMain.INSTANCE.checkPlayer(player)
                            }
                            session.show()
                        }
                    }
                } else if (it.isRightClick) {
                    area.range3D.visualize(area.world, Particle.FLAME, 10)
                    area.stopPoint.visualize(area.world, Particle.BARRIER, 10)
                    area.teleport(player)
                    PluginMain.INSTANCE.messageManager.printf(player, "&a传送成功!")
                }
            })
        }
        true
    }, null)
}