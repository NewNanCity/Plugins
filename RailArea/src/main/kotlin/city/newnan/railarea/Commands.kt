package city.newnan.railarea

import city.newnan.railarea.config.toFMString
import city.newnan.railarea.gui.pageGui
import city.newnan.railarea.gui.showLineStationGui
import city.newnan.railarea.input.handleAreaInput
import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import dev.triumphteam.gui.builder.item.ItemBuilder
import me.lucko.helper.Schedulers
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack


@CommandAlias("rail")
object Commands : BaseCommand() {
    @HelpCommand
    @Subcommand("help")
    fun help(sender: CommandSender, help: CommandHelp) {
        help.showHelp()
    }

    @Default
    @Subcommand("gui")
    @Description("显示所有区域")
    @CommandPermission("rail-area.edit")
    fun showGui(sender: Player) {
        showLineStationGui(sender, true) { line, station, back ->
            val gui = pageGui(Component.text("铁路区域列表"))
            fun update() {
                gui.clearPageItems()
                PluginMain.INSTANCE.lineStationAreas[station to line]?.forEach { area ->
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
                            meta.lore = listOf(
                                if (next == null) "本站为终点站" else "§r§7下一站: §r${next.name}§r",
                                if (last == null) "本站为始发站" else "§r§7上一站: §r${last.name}§r",
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
                            if (it.isLeftClick) {
                                gui.close(sender)
                                val oldArea = area
                                PluginMain.INSTANCE.messageManager.printf(sender, "开始设置区域 &2$name&r，接下来请设定区域的属性:")
                                handleAreaInput(sender, oldArea) { area ->
                                    if (area != null) {
                                        PluginMain.INSTANCE.updateArea(oldArea, area)
                                        PluginMain.INSTANCE.messageManager.printf(sender, "区域 &2$name&r 已更新!")
                                        Schedulers.sync().runLater({ update(); gui.update(); gui.open(sender) }, 1)
                                    } else {
                                        Schedulers.sync().runLater({ gui.open(sender) }, 1)
                                    }
                                }
                            } else if (it.isRightClick) {
                                gui.close(sender)
                                PluginMain.INSTANCE.messageManager.printf(sender, "&c确认删除线路? 回复Y确认, 回复其他取消")
                                PluginMain.INSTANCE.messageManager.gets(sender) { input ->
                                    if (input == "Y") {
                                        PluginMain.INSTANCE.removeArea(area)
                                        PluginMain.INSTANCE.messageManager.printf(sender, "区域 &2$name&r 已删除!")
                                        Schedulers.sync().runLater({ update(); gui.update(); gui.open(sender) }, 1)
                                    } else {
                                        Schedulers.sync().runLater({ gui.open(sender) }, 1)
                                    }
                                    true
                                }
                            }
                        } else if (it.isRightClick) {
                            sender.teleport(Location(area.world, area.stopPoint.x.toDouble()+0.5, area.stopPoint.y.toDouble()+0.1, area.stopPoint.z.toDouble()+0.5))
                            PluginMain.INSTANCE.messageManager.printf(sender, "&a传送成功!")
                            gui.close(sender)
                        }
                    })
                }
            }
            gui.setItem(6, 9, ItemBuilder.from(Material.BARRIER).name(Component.text("返回")).asGuiItem {
                back()
            })
            gui.setItem(6, 1, ItemBuilder.from(Material.EMERALD_BLOCK).name(Component.text("添加区域")).asGuiItem {
                gui.close(sender)
                PluginMain.INSTANCE.messageManager.printf(sender, "开始设置区域，接下来请设定区域的属性:")
                handleAreaInput(sender, null, iStation = station, iLine = line) { area ->
                    if (area != null) {
                        PluginMain.INSTANCE.addArea(area)
                        PluginMain.INSTANCE.messageManager.printf(sender, "区域已创建!")
                        Schedulers.sync().runLater({ update(); gui.update(); gui.open(sender) }, 1)
                    } else {
                        Schedulers.sync().runLater({ gui.open(sender) }, 1)
                    }
                }
            })

            Schedulers.sync().runLater({ update(); gui.open(sender) }, 1)
        }
    }

    @Subcommand("reload")
    @Description("重载插件")
    @CommandPermission("rail-area.reload")
    fun reloadConfig(sender: CommandSender) {
        PluginMain.INSTANCE.reload()
        PluginMain.INSTANCE.messageManager.printf(sender, "插件重载完毕!")
    }
}