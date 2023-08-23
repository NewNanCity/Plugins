package city.newnan.railarea.gui

import city.newnan.railarea.PluginMain
import city.newnan.railarea.config.RailLine
import city.newnan.railarea.config.toFMString
import city.newnan.railarea.config.toHexString
import city.newnan.railarea.input.handleRailLineInput
import city.newnan.railarea.input.handleYesInput
import city.newnan.violet.gui.PlayerGuiSession
import city.newnan.violet.item.toSkull
import dev.triumphteam.gui.builder.item.ItemBuilder
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

fun openRailLinesGui(session: PlayerGuiSession, editable: Boolean, setLine: (RailLine) -> Unit) {
    val player = session.player
    session.open(pageGui(session, Component.text(if (editable) "§7[§3§l牛腩轨道交通§r§7]§r 所有线路" else "§8[§6§l牛腩轨道交通§r§8]§r 选择线路")), { _, gui, _ ->
        if (editable) {
            gui.setItem(6,
                1,
                ItemBuilder.from("9a2d891c6ae9f6baa040d736ab84d48344bb6b70d7f1a280dd12cbac4d777".toSkull())
                    .name(Component.text("添加线路")).asGuiItem {
                        handleRailLineInput(session, null) { line ->
                            if (line != null) {
                                PluginMain.INSTANCE.addRailLine(line)
                                PluginMain.INSTANCE.messageManager.printf(
                                    player,
                                    "§a线路 ${line.color.toFMString()}${line.name}§r 已添加!"
                                )
                            }
                            session.show()
                        }
                    })
            if (PluginMain.INSTANCE.lineStationAreas.containsKey(PluginMain.INSTANCE.unknownStation to PluginMain.INSTANCE.unknownLine)) {
                val item = "b0253144515d14243cd176cf7a9c4ff1af49010e5716ca88a7adfd13d9bee165".toSkull()
                item.itemMeta = item.itemMeta?.also {
                    it.setDisplayName("未知区域")
                    it.lore = listOf("这些区域由于其所在的线路或站点被删除,", "处于游离状态, 等待分配")
                }
                gui.setItem(6, 5, ItemBuilder.from(item).asGuiItem { openFreeAreaGui(session) })
            } else {
                gui.setItem(6, 5, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).name(Component.text("")).asGuiItem())
            }
        }

        gui.clearPageItems()
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
            gui.addItem(ItemBuilder.from(item)
                .asGuiItem {
                    if (editable) {
                        if (it.isShiftClick) {
                            if (it.isLeftClick) {
                                handleRailLineInput(session, lineKW.value) { newLine ->
                                    if (newLine != null) {
                                        PluginMain.INSTANCE.lines.remove(lineKW.value.name)
                                        lineKW.value.name = newLine.name
                                        lineKW.value.color = newLine.color
                                        lineKW.value.colorMaterial = newLine.colorMaterial
                                        PluginMain.INSTANCE.lines[lineKW.value.name] = lineKW.value
                                        PluginMain.INSTANCE.save()
                                    }
                                    session.show()
                                }
                            } else {
                                handleYesInput(session, "§c确认删除线路 ${lineKW.value.color.toFMString()}${lineKW.value.name
                                }§r§c? 将删除其包含的所有站点(站点自身不会被删除)! 回复Y确认, 回复其他取消") { yes ->
                                    if (yes) PluginMain.INSTANCE.removeLine(lineKW.value)
                                    session.show()
                                }
                            }
                            return@asGuiItem
                        } else if (it.isRightClick) {
                            lineKW.value.isCycle = !lineKW.value.isCycle
                            PluginMain.INSTANCE.save()
                            session.refresh()
                            return@asGuiItem
                        }
                    }
                    setLine(lineKW.value)
                }
            )
        }
        true
    })
}