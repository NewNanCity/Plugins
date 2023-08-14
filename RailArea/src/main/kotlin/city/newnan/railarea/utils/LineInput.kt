package city.newnan.railarea.utils

import city.newnan.railarea.PluginMain
import city.newnan.railarea.config.RailLine
import city.newnan.railarea.config.Station
import city.newnan.railarea.config.toFMString
import dev.triumphteam.gui.builder.item.ItemBuilder
import me.lucko.helper.Schedulers
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

fun getStation (player: Player, unavailableStations: Set<Station>, back: () -> Unit, done: (station: Station) -> Unit) {
    val gui = pageGui(Component.text("所有可用站点"))
    fun update() {
        gui.clearPageItems()
        PluginMain.INSTANCE.stations.forEach { (_, station) ->
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
                        if (station.lines.size > 0) {
                            gui.close(player)
                            PluginMain.INSTANCE.messageManager.printf(player, "&c确认删除站点 ${station.name}? 回复Y确认, 回复其他取消")
                            PluginMain.INSTANCE.messageManager.gets(player) { input ->
                                if (input == "Y") {
                                    PluginMain.INSTANCE.removeStation(station)
                                    Schedulers.sync().runLater({ update(); gui.update(); gui.open(player) }, 1)
                                } else {
                                    Schedulers.sync().runLater({ gui.open(player) }, 1)
                                }
                                true
                            }
                            return@asGuiItem
                        }
                    } else {
                        gui.close(player)
                        PluginMain.INSTANCE.messageManager.printf(player, "开始设置站点 &2${station.name}&r，接下来请设定站点的属性:")
                        PluginMain.INSTANCE.messageManager.gets(player) { input ->
                            if (PluginMain.INSTANCE.stations.containsKey(input)) {
                                PluginMain.INSTANCE.messageManager.printf(player, "&c站点 $input 已存在!")
                                return@gets false
                            } else {
                                station.name = input
                                PluginMain.INSTANCE.save()
                                Schedulers.sync().runLater({ update(); gui.update(); gui.open(player) }, 1)
                                return@gets true
                            }
                        }
                        return@asGuiItem
                    }
                }
                if (!unavailable) done(station)
            })
        }
    }
    gui.setItem(6, 1, ItemBuilder.from(Material.ACACIA_SIGN).name(Component.text("创建站点")).asGuiItem {
        if (it.whoClicked != player && it.inventory != gui.inventory) return@asGuiItem
        gui.close(player)
        PluginMain.INSTANCE.messageManager.printf(player, "&c请输入站点名称")
        PluginMain.INSTANCE.messageManager.gets(player) { input ->
            if (PluginMain.INSTANCE.stations.containsKey(input)) {
                PluginMain.INSTANCE.messageManager.printf(player, "&c站点 $input 已存在!")
                return@gets false
            } else {
                PluginMain.INSTANCE.addStation(Station(PluginMain.INSTANCE.nextStationId++, input))
                Schedulers.sync().runLater({ update(); gui.update(); gui.open(player) }, 1)
                return@gets true
            }
        }
    })
    gui.setItem(6, 9, ItemBuilder.from(Material.BARRIER).name(Component.text("返回")).asGuiItem {
        if (it.whoClicked != player && it.inventory != gui.inventory) return@asGuiItem
        back()
    })
    Schedulers.sync().runLater({ update(); gui.open(player) }, 1)
}

fun showFreeArea (player: Player, done: () -> Unit) {
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
                        PluginMain.INSTANCE.messageManager.gets(player) { input ->
                            if (input == "Y") {
                                PluginMain.INSTANCE.removeArea(area)
                                update()
                                gui.update()
                                gui.open(player)
                            } else {
                                gui.open(player)
                            }
                            true
                        }
                    } else {
                        gui.close(player)
                        PluginMain.INSTANCE.messageManager.printf(player, "开始设置区域，接下来请设定区域的属性:")
                        handleAreaInput(player, area) { newArea ->
                            if (newArea == null) {
                                gui.open(player)
                            } else {
                                PluginMain.INSTANCE.updateArea(area, newArea)
                                update()
                                gui.update()
                                gui.open(player)
                            }
                        }
                    }
                } else if (it.isRightClick) {
                    player.teleport(Location(area.world,
                        area.stopPoint.x.toDouble()+0.5, area.stopPoint.y.toDouble()+0.1, area.stopPoint.z.toDouble()+0.5))
                    PluginMain.INSTANCE.messageManager.printf(player, "&a传送成功!")
                }
            })
        }
    }
    gui.setItem(6, 9, ItemBuilder.from(Material.BARRIER).name(Component.text("返回")).asGuiItem {
        gui.close(player)
        done()
    })
    update()
    Schedulers.sync().run { gui.open(player) }
}

fun getLineStation (player: Player, editable: Boolean, done: (line: RailLine, station: Station, back: () -> Unit) -> Unit) {
    val railGui = pageGui(Component.text(if (editable) "所有线路" else "选择线路"))

    fun openLineGui(lineO: RailLine, back: () -> Unit) {
        var line = lineO
        val stationGui = pageGui(Component.text(if (editable) "所有站点" else "选择站点"))
        stationGui.setItem(6, 9, ItemBuilder.from(Material.BARRIER).name(Component.text("返回")).asGuiItem {
            if (it.whoClicked != player && it.inventory != stationGui.inventory) return@asGuiItem
            back()
        })
        fun updateLineGui() {
            stationGui.clearPageItems()
            line.stations.forEach { stationKW ->
                val item = ItemStack(Material.ACTIVATOR_RAIL)
                val index = line.stations.indexOf(stationKW)
                val canMoveNext = index < line.stations.size - 1
                val canMoveLast = index > 0
                item.itemMeta = item.itemMeta?.also { it2 ->
                    it2.setDisplayName(stationKW.name)
                    println("editable = $editable")
                    it2.lore = listOf("aaaaaaaaaaa")
//                    if (editable) {
//                        val lore = mutableListOf("§6右键: 切换环线模式§r")
//                        if (canMoveNext) lore.add("§6shift+左键: 移动到下一站§r")
//                        if (canMoveLast) lore.add("§6shift+右键: 移动到上一站§r")
//                        it2.lore = lore.toList()
//                    }
                }
                stationGui.addItem(ItemBuilder.from(Material.ACTIVATOR_RAIL).name(Component.text(stationKW.name)).asGuiItem {
                    if (it.whoClicked != player && it.inventory != stationGui.inventory) return@asGuiItem
                    if (editable) {
                        if (it.isShiftClick) {
                            if (it.isLeftClick) {
                                if (canMoveNext) {
                                    val targetIndex = index + 1
                                    line.stations[index] = line.stations[targetIndex]
                                    line.stations[targetIndex] = stationKW
                                    PluginMain.INSTANCE.save()
                                    Schedulers.sync().runLater({ updateLineGui(); stationGui.update(); stationGui.open(player) }, 1)
                                }
                            } else if (it.isRightClick) {
                                if (canMoveLast) {
                                    val targetIndex = index - 1
                                    line.stations[index] = line.stations[targetIndex]
                                    line.stations[targetIndex] = stationKW
                                    PluginMain.INSTANCE.save()
                                    Schedulers.sync().runLater({ updateLineGui(); stationGui.update(); stationGui.open(player) }, 1)
                                }
                            }
                            return@asGuiItem
                        } else if (it.isRightClick) {
                            stationGui.close(player)
                            PluginMain.INSTANCE.messageManager.printf(player, "&c确认从线路中移除站点 ${stationKW.name}? 站点自身不会被删除! 回复Y确认, 回复其他取消")
                            PluginMain.INSTANCE.messageManager.gets(player) { input ->
                                if (input == "Y") {
                                    val newStations = line.stations.toMutableList()
                                    newStations.remove(stationKW)
                                    val newLine = RailLine(line.id, line.name, newStations, line.color, line.isCycle, line.colorMaterial)
                                    PluginMain.INSTANCE.updateLine(line, newLine)
                                    line = newLine
                                    Schedulers.sync().runLater({ updateLineGui(); stationGui.update(); stationGui.open(player) }, 1)
                                } else {
                                    Schedulers.sync().runLater({ stationGui.open(player) }, 1)
                                }
                                true
                            }
                            return@asGuiItem
                        }
                    }
                    done(line, stationKW) {
                        Schedulers.sync().runLater({ stationGui.open(player) }, 1)
                    }
                })
            }
        }
        if (editable) {
            stationGui.setItem(6, 1, ItemBuilder.from(Material.ACACIA_SIGN).name(Component.text("添加站点")).asGuiItem {
                if (it.whoClicked != player && it.inventory != stationGui.inventory) return@asGuiItem
                getStation(player, line.stations.toSet(), { Schedulers.sync().runLater({ stationGui.open(player) }, 1) }) { station ->
                    val newStations = line.stations.toMutableList()
                    newStations.add(station)
                    val newLine = RailLine(line.id, line.name, newStations, line.color, line.isCycle, line.colorMaterial)
                    PluginMain.INSTANCE.updateLine(line, newLine)
                    line = newLine
                    Schedulers.sync().runLater({ updateLineGui(); stationGui.update(); stationGui.open(player) }, 1)
                }
            })
        }
        Schedulers.sync().runLater({ updateLineGui(); stationGui.open(player) }, 1)
    }

    fun updateRailGui() {
        railGui.clearPageItems()
        PluginMain.INSTANCE.lines.forEach { lineKW ->
            val item = ItemStack(lineKW.value.colorMaterial)
            item.itemMeta = item.itemMeta?.also {
                it.setDisplayName("§r${lineKW.value.color.toFMString()}${lineKW.value.name}§r")
                if (editable) {
                    it.lore = listOf(
                        "环线: ${if (lineKW.value.isCycle) "§a是" else "§c否"}",
                        "",
                        "§6右键: 切换环线模式§r",
                        "§6shift+左键: 修改§r",
                        "§6shift+右键: 删除§r",
                    )
                } else {
                    it.lore = listOf("环线: ${if (lineKW.value.isCycle) "§a是" else "§c否"}")
                }
            }
            railGui.addItem(ItemBuilder.from(item)
                .asGuiItem {
                    if (it.whoClicked != player && it.inventory != railGui.inventory) return@asGuiItem
                    if (editable) {
                        if (it.isShiftClick) {
                            if (it.isLeftClick) {
                                handleRailLineInput(player, lineKW.value) { newLine ->
                                    lineKW.value.name = newLine.name
                                    lineKW.value.color = newLine.color
                                    lineKW.value.colorMaterial = newLine.colorMaterial
                                    PluginMain.INSTANCE.save()
                                    Schedulers.sync().runLater({ updateRailGui(); railGui.update(); railGui.open(player) }, 1)
                                }
                            } else {
                                railGui.close(player)
                                PluginMain.INSTANCE.messageManager.printf(player, "&c确认删除线路 ${lineKW.value.name}? 将删除其包含的所有站点(站点自身不会被删除)! 回复Y确认, 回复其他取消")
                                PluginMain.INSTANCE.messageManager.gets(player) { input ->
                                    if (input == "Y") {
                                        PluginMain.INSTANCE.removeLine(lineKW.value)
                                        Schedulers.sync().runLater({ updateRailGui(); railGui.update(); railGui.open(player) }, 1)
                                    } else {
                                        Schedulers.sync().runLater({ railGui.open(player) }, 1)
                                    }
                                    true
                                }
                            }
                            return@asGuiItem
                        } else if (it.isRightClick) {
                            lineKW.value.isCycle = !lineKW.value.isCycle
                            PluginMain.INSTANCE.save()
                            Schedulers.sync().runLater({ updateRailGui(); railGui.update(); railGui.open(player) }, 1)
                            return@asGuiItem
                        }
                    }
                    openLineGui(lineKW.value) {
                        Schedulers.sync().runLater({ updateRailGui(); railGui.update(); railGui.open(player) }, 1)
                    }
                }
            )
        }
        if (editable) {
            railGui.setItem(6, 1, ItemBuilder.from(Material.ACACIA_SIGN).name(Component.text("添加线路")).asGuiItem {
                if (it.whoClicked != player && it.inventory != railGui.inventory) return@asGuiItem
                PluginMain.INSTANCE.messageManager.printf(player, "&c请设定线路名称和颜色")
                handleRailLineInput(player, null) { line ->
                    PluginMain.INSTANCE.addRailLine(line)
                    Schedulers.sync().runLater({ updateRailGui(); railGui.update(); railGui.open(player) }, 1)
                }
            })
            if (PluginMain.INSTANCE.lineStationAreas.containsKey(PluginMain.INSTANCE.unknownStation to PluginMain.INSTANCE.unknownLine)) {
                val item = ItemStack(Material.CHEST)
                item.itemMeta = item.itemMeta?.also {
                    it.setDisplayName("未知区域")
                    it.lore = listOf("这些区域由于其所在的线路和站点被删除, 处于游离状态，等待分配")
                }
                railGui.setItem(6, 5, ItemBuilder.from(item).asGuiItem {
                    if (it.whoClicked != player) return@asGuiItem
                    showFreeArea(player) {
                        Schedulers.sync().runLater({ updateRailGui(); railGui.update(); railGui.open(player) }, 1)
                    }
                })
            }
        }
    }
    Schedulers.sync().runLater({ updateRailGui(); railGui.open(player) }, 1)
}