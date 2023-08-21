package city.newnan.railarea.gui

import city.newnan.railarea.PluginMain
import city.newnan.railarea.config.RailLine
import city.newnan.railarea.config.Station
import city.newnan.railarea.config.toFMString
import city.newnan.railarea.config.toHexString
import city.newnan.railarea.input.handleRailLineInput
import city.newnan.railarea.input.handleYesInput
import dev.triumphteam.gui.builder.item.ItemBuilder
import me.lucko.helper.Schedulers
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack


fun showLineStationGui (player: Player, editable: Boolean, done: (line: RailLine, station: Station, back: () -> Unit) -> Unit) {
    val railGui = pageGui(Component.text(if (editable) "所有线路" else "选择线路"))

    fun openLineGui(lineO: RailLine, back: () -> Unit) {
        var line = lineO
        val stationGui = pageGui(Component.text(if (editable) "§r§8§r[§r${line.color.toFMString()}§l${line.name}§r§8§r]§r的所有站点" else "选择站点"))
        stationGui.setItem(6, 9, ItemBuilder.from(Material.BARRIER).name(Component.text("返回")).asGuiItem {
            if (it.whoClicked != player && it.inventory != stationGui.inventory) return@asGuiItem
            back()
        })
        fun updateLineGui() {
            stationGui.clearPageItems()
            if (editable && !line.isCycle) {
                stationGui.addItem(ItemBuilder.from(if (line.leftReturn) Material.GREEN_CONCRETE else Material.RED_CONCRETE)
                    .name(Component.text(if (line.leftReturn) "左侧折返: §a是" else "左侧折返: §c否"))
                    .lore(Component.text("指到实际线路达终点站后"), Component.text("是否能够折返到另一边的起点"))
                    .asGuiItem {
                        if (it.whoClicked != player && it.inventory != stationGui.inventory) return@asGuiItem
                        line.leftReturn = !line.leftReturn
                        PluginMain.INSTANCE.save()
                        Schedulers.sync().runLater({ updateLineGui(); stationGui.update(); stationGui.open(player) }, 1)
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
                stationGui.addItem(ItemBuilder.from(item).asGuiItem {
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
                            handleYesInput(player) {yes ->
                                if (yes) {
                                    val newStations = line.stations.toMutableList()
                                    newStations.remove(stationKW)
                                    val newLine = RailLine(line.id, line.name, newStations, line.color, line.isCycle, line.colorMaterial, line.leftReturn, line.rightReturn)
                                    PluginMain.INSTANCE.updateLine(line, newLine)
                                    line = newLine
                                    Schedulers.sync().runLater({ updateLineGui(); stationGui.update(); stationGui.open(player) }, 1)
                                } else {
                                    Schedulers.sync().runLater({ stationGui.open(player) }, 1)
                                }
                            }
                            return@asGuiItem
                        }
                    }
                    done(line, stationKW) {
                        Schedulers.sync().runLater({ stationGui.open(player) }, 1)
                    }
                })
            }
            if (editable && !line.isCycle) {
                stationGui.addItem(ItemBuilder.from(if (line.rightReturn) Material.GREEN_CONCRETE else Material.RED_CONCRETE)
                    .name(Component.text(if (line.rightReturn) "右侧折返: §a是" else "右侧折返: §c否"))
                    .lore(Component.text("指到实际线路达终点站后"), Component.text("是否能够折返到另一边的起点"))
                    .asGuiItem {
                        if (it.whoClicked != player && it.inventory != stationGui.inventory) return@asGuiItem
                        line.rightReturn = !line.rightReturn
                        PluginMain.INSTANCE.save()
                        Schedulers.sync().runLater({ updateLineGui(); stationGui.update(); stationGui.open(player) }, 1)
                    }
                )
            }
        }
        if (editable) {
            stationGui.setItem(6, 1, ItemBuilder.from(Material.ACACIA_SIGN).name(Component.text("添加站点")).asGuiItem {
                if (it.whoClicked != player && it.inventory != stationGui.inventory) return@asGuiItem
                showStationGui(player, line.stations.toSet(), { Schedulers.sync().runLater({ updateLineGui(); stationGui.update(); stationGui.open(player) }, 1) }) { station ->
                    val newStations = line.stations.toMutableList()
                    newStations.add(station)
                    val newLine = RailLine(line.id, line.name, newStations, line.color, line.isCycle, line.colorMaterial,
                        leftReturn = false, rightReturn = false)
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
        PluginMain.INSTANCE.lines.entries.sortedBy { it.value.id }.forEach { lineKW ->
            val item = ItemStack(lineKW.value.colorMaterial)
            item.itemMeta = item.itemMeta?.also {
                it.setDisplayName("§r${lineKW.value.color.toFMString()}${lineKW.value.name}§r")
                if (editable) {
                    it.lore = listOf(
                        "颜色: §r${lineKW.value.color.toFMString()}${lineKW.value.color.toHexString()}",
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
                                railGui.close(player)
                                PluginMain.INSTANCE.messageManager.printf(player, "&c请设定线路名称和颜色")
                                handleRailLineInput(player, lineKW.value) { newLine ->
                                    if (newLine == null) {
                                        Schedulers.sync().runLater({ railGui.open(player) }, 1)
                                    } else {
                                        PluginMain.INSTANCE.lines.remove(lineKW.value.name)
                                        lineKW.value.name = newLine.name
                                        lineKW.value.color = newLine.color
                                        lineKW.value.colorMaterial = newLine.colorMaterial
                                        PluginMain.INSTANCE.lines[lineKW.value.name] = lineKW.value
                                        PluginMain.INSTANCE.save()
                                        Schedulers.sync().runLater({ updateRailGui(); railGui.update(); railGui.open(player) }, 1)
                                    }
                                }
                            } else {
                                railGui.close(player)
                                PluginMain.INSTANCE.messageManager.printf(player, "§c确认删除线路 ${
                                    lineKW.value.color.toFMString()}${lineKW.value.name}§r§c? 将删除其包含的所有站点(站点自身不会被删除)! 回复Y确认, 回复其他取消")
                                handleYesInput(player) {yes ->
                                    if (yes) {
                                        PluginMain.INSTANCE.removeLine(lineKW.value)
                                        Schedulers.sync().runLater({ updateRailGui(); railGui.update(); railGui.open(player) }, 1)
                                    } else {
                                        Schedulers.sync().runLater({ railGui.open(player) }, 1)
                                    }
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
                railGui.close(player)
                PluginMain.INSTANCE.messageManager.printf(player, "§c请设定线路名称和颜色")
                handleRailLineInput(player, null) { line ->
                    if (line == null) {
                        Schedulers.sync().runLater({ railGui.open(player) }, 1)
                    } else {
                        PluginMain.INSTANCE.addRailLine(line)
                        PluginMain.INSTANCE.messageManager.printf(player, "§a线路 ${line.color.toFMString()}${line.name}§r 已添加!")
                        Schedulers.sync().runLater({ updateRailGui(); railGui.update(); railGui.open(player) }, 1)
                    }
                }
            })
            if (PluginMain.INSTANCE.lineStationAreas.containsKey(PluginMain.INSTANCE.unknownStation to PluginMain.INSTANCE.unknownLine)) {
                val item = ItemStack(Material.CHEST)
                item.itemMeta = item.itemMeta?.also {
                    it.setDisplayName("未知区域")
                    it.lore = listOf("这些区域由于其所在的线路或站点被删除,", "处于游离状态, 等待分配")
                }
                railGui.setItem(6, 5, ItemBuilder.from(item).asGuiItem {
                    if (it.whoClicked != player) return@asGuiItem
                    showFreeAreaGui(player) {
                        Schedulers.sync().runLater({ updateRailGui(); railGui.update(); railGui.open(player) }, 1)
                    }
                })
            }
        }
    }
    Schedulers.sync().runLater({ updateRailGui(); railGui.open(player) }, 1)
}