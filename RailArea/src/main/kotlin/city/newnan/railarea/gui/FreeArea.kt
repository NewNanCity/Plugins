package city.newnan.railarea.gui

import city.newnan.railarea.PluginMain
import city.newnan.railarea.input.handleAreaInput
import city.newnan.railarea.input.handleYesInput
import dev.triumphteam.gui.builder.item.ItemBuilder
import me.lucko.helper.Schedulers
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

fun showFreeAreaGui (player: Player, done: () -> Unit) {
    val gui = pageGui(Component.text("游离区域"))
    fun update() {
        gui.clearPageItems()
        PluginMain.INSTANCE.lineStationAreas[PluginMain.INSTANCE.unknownStation to PluginMain.INSTANCE.unknownLine]?.forEach { area ->
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
                if (it.whoClicked != player) return@asGuiItem
                if (it.isShiftClick) {
                    if (it.isRightClick) {
                        gui.close(player)
                        PluginMain.INSTANCE.messageManager.printf(player, "&c确认删除区域? 回复Y确认, 回复其他取消")
                        handleYesInput(player) { yes ->
                            if (yes) {
                                PluginMain.INSTANCE.removeArea(area)
                                Schedulers.sync().runLater({ update(); gui.update(); gui.open(player) }, 1)
                            } else {
                                Schedulers.sync().runLater({ gui.open(player) }, 1)
                            }
                        }
                    } else {
                        gui.close(player)
                        PluginMain.INSTANCE.messageManager.printf(player, "开始设置区域，接下来请设定区域的属性:")
                        handleAreaInput(player, area) { newArea ->
                            if (newArea == null) {
                                Schedulers.sync().runLater({ gui.open(player) }, 1)
                            } else {
                                PluginMain.INSTANCE.updateArea(area, newArea)
                                Schedulers.sync().runLater({ update(); gui.update(); gui.open(player) }, 1)
                                PluginMain.INSTANCE.checkPlayer(player)
                            }
                        }
                    }
                } else if (it.isRightClick) {
                    player.teleport(
                        Location(area.world,
                        area.stopPoint.x.toDouble()+0.5, area.stopPoint.y.toDouble()+0.1, area.stopPoint.z.toDouble()+0.5)
                    )
                    PluginMain.INSTANCE.messageManager.printf(player, "&a传送成功!")
                }
            })
        }
    }
    gui.setItem(6, 9, ItemBuilder.from(Material.BARRIER).name(Component.text("返回")).asGuiItem {
        done()
    })
    Schedulers.sync().runLater({ update(); gui.open(player) }, 1)
}