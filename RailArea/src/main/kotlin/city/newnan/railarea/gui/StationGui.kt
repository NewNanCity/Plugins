package city.newnan.railarea.gui

import city.newnan.railarea.PluginMain
import city.newnan.railarea.config.Station
import city.newnan.railarea.input.handleStationInput
import city.newnan.railarea.input.handleYesInput
import dev.triumphteam.gui.builder.item.ItemBuilder
import me.lucko.helper.Schedulers
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

fun showStationGui (player: Player, unavailableStations: Set<Station>, back: () -> Unit, done: (station: Station) -> Unit) {
    val gui = pageGui(Component.text("所有可用站点"))
    fun update() {
        gui.clearPageItems()
        PluginMain.INSTANCE.stations.entries.sortedBy { it.value.id }.forEach { (_, station) ->
            val unavailable = unavailableStations.contains(station)
            val item = ItemStack(Material.ACTIVATOR_RAIL)
            item.itemMeta = item.itemMeta?.also {
                it.setDisplayName("${if (unavailable) "§5[已被添加]§r " else ""}${station.name}")
                it.lore = listOf(
                    "§6左键: 选择§r",
                    "§6shift+左键: 修改§r",
                    if (station.lines.size > 0) "无法删除, 请从先所有线路中移除" else "§6shift+右键: 删除§r",
                )
            }
            gui.addItem(ItemBuilder.from(item).asGuiItem {
                if (it.whoClicked != player && it.inventory != gui.inventory) return@asGuiItem
                if (it.isShiftClick) {
                    if (it.isRightClick) {
                        if (station.lines.size == 0) {
                            gui.close(player)
                            PluginMain.INSTANCE.messageManager.printf(player, "&c确认删除站点 ${station.name}? 回复Y确认, 回复其他取消")
                            handleYesInput(player) { yes ->
                                if (yes) {
                                    PluginMain.INSTANCE.removeStation(station)
                                    Schedulers.sync().runLater({ update(); gui.update(); gui.open(player) }, 1)
                                } else {
                                    Schedulers.sync().runLater({ gui.open(player) }, 1)
                                }
                            }
                        }
                    } else {
                        gui.close(player)
                        PluginMain.INSTANCE.messageManager.printf(player, "开始设置站点 &2${station.name}&r，接下来请设定站点的属性:")
                        handleStationInput(player, station) { newStation ->
                            if (newStation == null) {
                                Schedulers.sync().runLater({ gui.open(player) }, 1)
                            } else {
                                PluginMain.INSTANCE.stations.remove(station.name)
                                station.name = newStation.name
                                PluginMain.INSTANCE.stations[station.name] = station
                                PluginMain.INSTANCE.save()
                                Schedulers.sync().runLater({ update(); gui.update(); gui.open(player) }, 1)
                            }
                        }
                    }
                    return@asGuiItem
                }
                if (!unavailable) done(station)
            })
        }
    }
    gui.setItem(6, 1, ItemBuilder.from(Material.ACACIA_SIGN).name(Component.text("创建站点")).asGuiItem {
        if (it.whoClicked != player && it.inventory != gui.inventory) return@asGuiItem
        gui.close(player)
        PluginMain.INSTANCE.messageManager.printf(player, "&c请输入站点名称")
        handleStationInput(player, null) { newStation ->
            if (newStation == null) {
                Schedulers.sync().runLater({ gui.open(player) }, 1)
            } else {
                PluginMain.INSTANCE.addStation(newStation)
                Schedulers.sync().runLater({ update(); gui.update(); gui.open(player) }, 1)
            }
        }
    })
    gui.setItem(6, 9, ItemBuilder.from(Material.BARRIER).name(Component.text("返回")).asGuiItem {
        if (it.whoClicked != player && it.inventory != gui.inventory) return@asGuiItem
        back()
    })
    Schedulers.sync().runLater({ update(); gui.open(player) }, 1)
}