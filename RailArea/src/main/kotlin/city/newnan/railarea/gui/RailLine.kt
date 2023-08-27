package city.newnan.railarea.gui

import city.newnan.railarea.PluginMain
import city.newnan.railarea.config.RailLine
import city.newnan.railarea.config.Station
import city.newnan.railarea.config.toFMString
import city.newnan.railarea.input.handleYesInput
import city.newnan.violet.gui.PlayerGuiSession
import city.newnan.violet.gui.UpdateType
import city.newnan.violet.item.toSkull
import dev.triumphteam.gui.builder.item.ItemBuilder
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

fun openRailLineGui(session: PlayerGuiSession, editable: Boolean, lineO: RailLine, setLineStation: (station: Station) -> Unit) {
    var line = lineO
    val stationGui = pageGui(session, Component.text(if (editable) "§7[§3§l牛腩轨道交通§r§7]§r §r§8§r[§r${line.color.toFMString()}§l${line.name}§r§8§r]§r的所有站点" else "§8[§6§l牛腩轨道交通§r§8]§r 选择站点"))
    session.open(stationGui, { type, gui, _ ->
        // Init
        if (type == UpdateType.Init) {
            if (editable) {
                gui.setItem(6, 1, ItemBuilder.from("cf7cdeefc6d37fecab676c584bf620832aaac85375e9fcbff27372492d69f".toSkull())
                    .name(Component.text("添加站点")).asGuiItem {
                    openStationGui(session, line.stations.toSet()) { station ->
                        val newStations = line.stations.toMutableList()
                        newStations.add(station)
                        val newLine = RailLine(line.id, line.name, newStations, line.color, line.isCycle, line.colorMaterial,
                            leftReturn = false, rightReturn = false)
                        PluginMain.INSTANCE.updateLine(line, newLine)
                        line = newLine
                    }
                })
            }
        }
        // Update
        gui.clearPageItems()
        if (editable && !line.isCycle) {
            gui.addItem(ItemBuilder.from(if (line.leftReturn) Material.GREEN_CONCRETE else Material.RED_CONCRETE)
                .name(Component.text(if (line.leftReturn) "左侧折返: §a是" else "左侧折返: §c否"))
                .lore(Component.text("指到实际线路达终点站后"), Component.text("是否能够折返到另一边的起点"))
                .asGuiItem {
                    line.leftReturn = !line.leftReturn
                    PluginMain.INSTANCE.save()
                    session.refresh()
                }
            )
        }
        line.stations.forEach { stationKW ->
            val item = ItemStack(Material.ACTIVATOR_RAIL)
            val index = line.stations.indexOf(stationKW)
            val canMoveNext = index < line.stations.size - 1
            val canMoveLast = index > 0
            item.itemMeta = item.itemMeta?.also { it2 ->
                it2.setDisplayName(stationKW.name)
                if (editable) {
                    val lore = mutableListOf("§6右键: 删除§r")
                    if (canMoveNext) lore.add("§6shift+左键: 移动到下一站§r")
                    if (canMoveLast) lore.add("§6shift+右键: 移动到上一站§r")
                    it2.lore = lore.toList()
                }
            }
            gui.addItem(ItemBuilder.from(item).asGuiItem {
                if (editable) {
                    if (it.isShiftClick) {
                        if (it.isLeftClick) {
                            if (canMoveNext) {
                                val targetIndex = index + 1
                                line.stations[index] = line.stations[targetIndex]
                                line.stations[targetIndex] = stationKW
                                PluginMain.INSTANCE.save()
                                session.refresh()
                            }
                        } else if (it.isRightClick) {
                            if (canMoveLast) {
                                val targetIndex = index - 1
                                line.stations[index] = line.stations[targetIndex]
                                line.stations[targetIndex] = stationKW
                                PluginMain.INSTANCE.save()
                                session.refresh()
                            }
                        }
                        return@asGuiItem
                    } else if (it.isRightClick) {
                        handleYesInput(session, "&c确认从线路中移除站点 ${stationKW.name}? 站点自身不会被删除! 回复Y确认, 回复其他取消") {yes ->
                            if (yes) {
                                val newStations = line.stations.toMutableList()
                                newStations.remove(stationKW)
                                val newLine = RailLine(line.id, line.name, newStations, line.color, line.isCycle, line.colorMaterial, line.leftReturn, line.rightReturn)
                                PluginMain.INSTANCE.updateLine(line, newLine)
                                line = newLine
                            }
                            session.show()
                        }
                        return@asGuiItem
                    }
                }
                setLineStation(stationKW)
            })
        }
        if (editable && !line.isCycle) {
            gui.addItem(ItemBuilder.from(if (line.rightReturn) Material.GREEN_CONCRETE else Material.RED_CONCRETE)
                .name(Component.text(if (line.rightReturn) "右侧折返: §a是" else "右侧折返: §c否"))
                .lore(Component.text("指到实际线路达终点站后"), Component.text("是否能够折返到另一边的起点"))
                .asGuiItem {
                    line.rightReturn = !line.rightReturn
                    PluginMain.INSTANCE.save()
                    session.refresh()
                }
            )
        }
        true
    })
}