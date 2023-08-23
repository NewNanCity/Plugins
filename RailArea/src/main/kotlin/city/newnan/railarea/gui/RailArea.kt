package city.newnan.railarea.gui

import city.newnan.railarea.PluginMain
import city.newnan.railarea.config.RailArea
import city.newnan.railarea.config.RailLine
import city.newnan.railarea.config.Station
import city.newnan.railarea.config.toFMString
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

fun openRailAreaGui(session: PlayerGuiSession, editable: Boolean, line: RailLine, station: Station, setRailArea: (RailArea) -> Unit) {
    val player = session.player
    val key = station to line
    session.open(pageGui(session, Component.text("§7[§3§l牛腩轨道交通§r§7]§r 铁路区域列表")), { type, gui, _ ->
        if (type == UpdateType.Init) {
            gui.setItem(6, 9, ItemBuilder.from(Material.BARRIER).name(Component.text("返回")).asGuiItem {
                session.back()
            })
            if (editable) {
                gui.setItem(6, 1, ItemBuilder.from(Material.EMERALD_BLOCK).name(Component.text("添加区域")).asGuiItem {
                    openReverseGui(session, line) {
                        handleAreaInput(session, null, iStation = station, iLine = line, iReverse = it) { area ->
                            if (area != null) {
                                PluginMain.INSTANCE.addArea(area)
                                PluginMain.INSTANCE.messageManager.printf(player, "区域已创建!")
                                PluginMain.INSTANCE.checkPlayer(player)
                            }
                            session.show()
                        }
                    }
                })
            }
        }
        gui.clearPageItems()
        PluginMain.INSTANCE.lineStationAreas[key]?.forEach { area ->
            val item = ItemStack(Material.RAIL).also {
                it.itemMeta = it.itemMeta?.also { meta ->
                    meta.setDisplayName("§r§f${area.station.name} §r${area.line.color.toFMString()}${area.line.name} §r§6${
                        if (area.reverse) area.line.stations.first().name else area.line.stations.last().name}§r方向")
                    val i = area.line.stations.indexOf(area.station)
                    var next = if ((i + 1) >= area.line.stations.size) (if (area.line.isCycle) area.line.stations[0] else null) else area.line.stations[i + 1]
                    var last = if (i == 0) (if (area.line.isCycle) area.line.stations[area.line.stations.size - 1] else null) else area.line.stations[i - 1]
                    if (area.reverse) {
                        val t = next
                        next = last
                        last = t
                    }
                    val lore = mutableListOf(
                        if (next == null) "本站为终点站" else "§r§7下一站: §r${next.name}§r",
                        if (last == null) "本站为始发站" else "§r§7上一站: §r${last.name}§r",
                        "§r§7世界: §r${area.world.name}§r",
                        "§r§7范围:",
                        "  §r${area.range3D.minX}, ${area.range3D.minY}, ${area.range3D.minZ}§r",
                        "  §r${area.range3D.maxX}, ${area.range3D.maxY}, ${area.range3D.maxZ}§r",
                        "§r§7停靠: §r§f${area.stopPoint.x},${area.stopPoint.y},${area.stopPoint.z} ${area.direction}§r",
                        "",
                        "§6左键: 选择§r",
                        "§6右键: 传送§r",
                    )
                    if (editable) {
                        lore.add("§6shift+左键: 修改§r")
                        lore.add("§6shift+右键: 删除§r")
                    }
                    meta.lore = lore.toList()
                }
            }
            gui.addItem(ItemBuilder.from(item).asGuiItem {
                if (it.isShiftClick && editable) {
                    if (it.isLeftClick) {
                        val oldArea = area
                        handleAreaInput(session, oldArea) { area ->
                            if (area != null) {
                                PluginMain.INSTANCE.updateArea(oldArea, area)
                                PluginMain.INSTANCE.messageManager.printf(player, "区域 &2${area.station.name}(${area.line.name})&r 已更新!")
                                PluginMain.INSTANCE.checkPlayer(player)
                            }
                            session.show()
                        }
                    } else if (it.isRightClick) {
                        handleYesInput(session, "&c确认删除区域? 回复Y确认, 回复其他取消") { yes ->
                            if (yes) {
                                PluginMain.INSTANCE.removeArea(area)
                                PluginMain.INSTANCE.messageManager.printf(player, "区域 &2${area.station.name}(${area.line.name})&r 已删除!")
                                PluginMain.INSTANCE.checkPlayer(player)
                            }
                            session.show()
                        }
                    }
                    return@asGuiItem
                }
                if (it.isRightClick) {
                    area.range3D.visualize(area.world, Particle.FLAME, 10)
                    area.stopPoint.visualize(area.world, Particle.BARRIER, 10)
                    area.teleport(player)
                    PluginMain.INSTANCE.messageManager.printf(player, "&a传送成功!")
                    session.clear()
                } else {
                    setRailArea(area)
                }
            })
        }
        true
    })
}