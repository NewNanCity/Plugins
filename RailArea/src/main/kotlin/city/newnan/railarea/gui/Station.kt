package city.newnan.railarea.gui

import city.newnan.railarea.PluginMain
import city.newnan.railarea.config.Station
import city.newnan.railarea.input.handleStationInput
import city.newnan.railarea.input.handleYesInput
import city.newnan.violet.gui.PlayerGuiSession
import city.newnan.violet.gui.UpdateType
import city.newnan.violet.item.toSkull
import dev.triumphteam.gui.builder.item.ItemBuilder
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

fun openStationGui (session: PlayerGuiSession, unavailableStations: Set<Station>, setStation: (Station) -> Unit) {
    var shouldUpdate = true
    session.open(pageGui(session, Component.text("§7[§3§l牛腩轨道交通§r§7]§r 所有可用站点")), { type, gui, _ ->
        // Init
        if (type == UpdateType.Init) {
            gui.setItem(6, 9, ItemBuilder.from(Material.BARRIER).name(Component.text("返回")).asGuiItem {
                session.back()
            })
            gui.setItem(6, 1, ItemBuilder.from("9a2d891c6ae9f6baa040d736ab84d48344bb6b70d7f1a280dd12cbac4d777".toSkull())
                .name(Component.text("创建站点")).asGuiItem {
                handleStationInput(session, null) { newStation ->
                    if (newStation != null) {
                        PluginMain.INSTANCE.addStation(newStation)
                        shouldUpdate = true
                    }
                    session.show()
                }
            })
        }
        // Update
        if (shouldUpdate) {
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
                    if (it.isShiftClick) {
                        if (it.isRightClick) {
                            if (station.lines.size == 0) {
                                handleYesInput(session, "&c确认删除站点 ${station.name}? 回复Y确认, 回复其他取消") { yes ->
                                    if (yes) {
                                        PluginMain.INSTANCE.removeStation(station)
                                        shouldUpdate = true
                                    }
                                    session.show()
                                }
                            }
                        } else {
                            handleStationInput(session, station) { newStation ->
                                if (newStation != null) {
                                    PluginMain.INSTANCE.stations.remove(station.name)
                                    station.name = newStation.name
                                    PluginMain.INSTANCE.stations[station.name] = station
                                    PluginMain.INSTANCE.save()
                                    shouldUpdate = true
                                }
                                session.show()
                            }
                        }
                        return@asGuiItem
                    }
                    if (!unavailable) {
                        setStation(station)
                    }
                })
            }
        }
        if (shouldUpdate) {
            shouldUpdate = false
            return@open true
        } else return@open false
    })
}