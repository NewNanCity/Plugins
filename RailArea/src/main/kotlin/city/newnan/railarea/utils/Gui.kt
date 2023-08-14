package city.newnan.railarea.utils

import city.newnan.railarea.PluginMain
import city.newnan.railarea.config.RailLine
import city.newnan.railarea.config.toColor
import city.newnan.railarea.config.toFMString
import city.newnan.railarea.config.toMaterial
import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.PaginatedGui
import net.kyori.adventure.text.Component
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.entity.Player

fun pageGui(title: Component): PaginatedGui {
    val gui = Gui.paginated()
        .title(title)
        .rows(6)
        .create()
    gui.setItem(6, 3, ItemBuilder.from(Material.PAPER).name(Component.text("上一页")).asGuiItem {
        gui.previous()
    })
    gui.setItem(6, 7, ItemBuilder.from(Material.PAPER).name(Component.text("下一页")).asGuiItem {
        gui.next()
    })
    listOf(1,2,4,5,6,8).forEach { gui.setItem(6, it, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE)
        .name(Component.text("")).asGuiItem()) }
    gui.setItem(6, 9, ItemBuilder.from(Material.BARRIER).name(Component.text("关闭")).asGuiItem {
        gui.close(it.whoClicked)
    })
    gui.setDefaultClickAction { it.isCancelled = true }
    return gui
}

fun handleRailLineInput(player: Player, oldLine: RailLine?, done: (line: RailLine) -> Unit) {
    var name: String? = oldLine?.name
    var color: Color? = oldLine?.color
    PluginMain.INSTANCE.messageManager.gets(player) { input ->
        when {
            input.startsWith("name:") -> {
                val nameT = input.substring(5).trim()
                if (PluginMain.INSTANCE.lines.containsKey(nameT)) {
                    PluginMain.INSTANCE.messageManager.printf(player, "&c线路 $nameT 已存在!")
                } else {
                    name = nameT
                    PluginMain.INSTANCE.messageManager.printf(player, "已设置名称: $nameT")
                }
            }
            input.startsWith("color:") -> {
                val colorT = input.substring(6).trim()
                if (!colorT.startsWith("#")) {
                    PluginMain.INSTANCE.messageManager.printf(player, "&c颜色格式错误! 请使用 #RRGGBB 格式!")
                }
                try {
                    color = colorT.toColor()
                    PluginMain.INSTANCE.messageManager.printf(player, "已设置颜色: ${color!!.toFMString()}$colorT")
                } catch (e: Exception) {
                    PluginMain.INSTANCE.messageManager.printf(player, "&c颜色格式错误! 请使用 #RRGGBB 格式!")
                }
            }
            input == "cancel" -> {
                PluginMain.INSTANCE.messageManager.printf(player, "已取消")
                return@gets true
            }
            input == "ok" -> {
                if (color == null || name == null) {
                    PluginMain.INSTANCE.messageManager.printf(player, "&c请先设置名称和颜色!")
                } else {
                    val line = RailLine(PluginMain.INSTANCE.nextLineId++, name!!,
                        oldLine?.stations ?: mutableListOf(), color!!, false, color!!.toMaterial())
                    PluginMain.INSTANCE.lines[name!!] = line
                    PluginMain.INSTANCE.messageManager.printf(player, "&a线路 $name 已添加!")
                    done(line)
                    return@gets true
                }
            }
        }
        false
    }
}