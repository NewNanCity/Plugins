package city.newnan.createarea

import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.bukkit.WorldEditPlugin
import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

data class Range2DWorld(val world: World, val range: Range2D)

fun Player.getSelection(): Range2DWorld?  {
    try {
        val worldEdit = server.pluginManager.getPlugin("WorldEdit") ?: return null
        if (worldEdit is WorldEditPlugin) {
            val region = worldEdit.getSession(player).getSelection(BukkitAdapter.adapt(world)) ?: return null
            val min = region.minimumPoint
            val max = region.maximumPoint
            return Range2DWorld(Bukkit.getWorld(region.world!!.name)!!, Range2D(min.x, min.z, max.x, max.z))
        }
    } catch (e: Exception) { return null }
    return null
}


@CommandAlias("carea|createarea")
object Commands : BaseCommand() {
    @Subcommand("reload")
    @CommandPermission("createarea.reload")
    @Description("重载插件")
    fun onReload(sender: CommandSender) {
        PluginMain.INSTANCE.reload()
        PluginMain.INSTANCE.messageManager.printf(sender, "插件已重载!")
    }

    @Default
    @HelpCommand
    @Subcommand("help")
    fun onHelp(sender: CommandSender, help: CommandHelp) {
        help.showHelp()
    }

    @Subcommand("tp")
    @CommandAlias("ctp")
    @CommandCompletion("@players")
    fun onTp(sender: Player, @Optional target: String?) {
        if (target == null || target == sender.name) {
            if (!sender.hasPermission("createarea.tp.self")) {
                PluginMain.INSTANCE.messageManager.printf(sender, "§c你没有权限传送到自己的创造区域!")
                return
            }
        } else if (!sender.hasPermission("createarea.tp.other")) {
            PluginMain.INSTANCE.messageManager.printf(sender, "§c你没有权限传送到他人的创造区域!")
            return
        }
        fun tpTo(area: Range2D) {
            Bukkit.getWorld(PluginMain.INSTANCE.createWorld)?.run {
                sender.teleport(Location(this, area.minX.toDouble(),
                    getHighestBlockYAt(area.minX, area.minZ).toDouble(), area.minZ.toDouble()))
            } ?: PluginMain.INSTANCE.messageManager.printf(sender, "§c世界 ${PluginMain.INSTANCE.createWorld} 不存在!")
        }
        if (target != null) {
            PluginMain.INSTANCE.builders.keys.find { it.name == target }?.run {
                val area = PluginMain.INSTANCE.builders[this]!!
                tpTo(area)
                PluginMain.INSTANCE.messageManager.printf(sender, "§a已将你传送到玩家 §c$target§r 的创造区域!")
            } ?: PluginMain.INSTANCE.messageManager.printf(sender, "§c玩家 §a$target§r 的创造区域不存在!")
        } else {
            PluginMain.INSTANCE.builders[sender]?.run {
                tpTo(this)
                PluginMain.INSTANCE.messageManager.printf(sender, "§a已将你传送到你的创造区域!")
            } ?: PluginMain.INSTANCE.messageManager.printf(sender, "§c你没有创建创造区域!")
        }
    }

    @Subcommand("set")
    @CommandAlias("cset")
    @CommandCompletion("@players")
    fun onSet(sender: Player, @Optional target: String?) {
        if (target == null || target == sender.name) {
            if (!sender.hasPermission("createarea.set.self")) {
                PluginMain.INSTANCE.messageManager.printf(sender, "§c你没有权限设置自己的创造区域!")
                return
            }
        } else if (!sender.hasPermission("createarea.set.other")) {
            PluginMain.INSTANCE.messageManager.printf(sender, "§c你没有权限设置他人的创造区域!")
            return
        }
        val selection = sender.getSelection()
        if (selection == null) {
            PluginMain.INSTANCE.messageManager.printf(sender, "§c请先用小木斧选择一个区域, 高度无所谓, 只关注水平区域!")
            return
        }
        if (target == null) {
            PluginMain.INSTANCE.updateArea(sender, selection.range.minX, selection.range.maxX, selection.range.minZ, selection.range.maxZ)
            PluginMain.INSTANCE.messageManager.printf(sender, "§a已为自己创建创造区域!")
        } else {
            PluginMain.INSTANCE.server.offlinePlayers.find { it.name == target && it.hasPlayedBefore() }?.run {
                PluginMain.INSTANCE.updateArea(this, selection.range.minX, selection.range.maxX, selection.range.minZ, selection.range.maxZ)
                PluginMain.INSTANCE.messageManager.printf(sender, "§a已为玩家 §c$target§r 创建创造区域!")
            } ?: PluginMain.INSTANCE.messageManager.printf(sender, "§c玩家 §a$target§r 不存在!")
        }
    }

    @Subcommand("delete|remove|del")
    @CommandAlias("cdel")
    @CommandCompletion("@players")
    fun onDelete(sender: Player, @Optional target: String?) {
        if (target == null || target == sender.name) {
            if (!sender.hasPermission("createarea.delete.self")) {
                PluginMain.INSTANCE.messageManager.printf(sender, "§c你没有权限删除自己的创造区域!")
                return
            }
        } else if (!sender.hasPermission("createarea.delete.other")) {
            PluginMain.INSTANCE.messageManager.printf(sender, "§c你没有权限删除他人的创造区域!")
            return
        }
        if (target == null) {
            PluginMain.INSTANCE.deleteArea(sender)
            PluginMain.INSTANCE.messageManager.printf(sender, "§a已删除你的创造区域!")
        } else {
            PluginMain.INSTANCE.builders.keys.find { it.name == target }?.run {
                PluginMain.INSTANCE.deleteArea(this)
                PluginMain.INSTANCE.messageManager.printf(sender, "§a已删除玩家 §c$target§r 的创造区域!")
            } ?: PluginMain.INSTANCE.messageManager.printf(sender, "§c玩家 §a$target§r 的创造区域不存在!")
        }
    }

    @Subcommand("gui|list")
    @CommandPermission("createarea.gui")
    fun onGui(sender: Player) {
        val gui = Gui.paginated()
            .title(Component.text("所有创造区"))
            .pageSize(6)
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
            gui.close(sender)
        })
        PluginMain.INSTANCE.builders.forEach { (player, range) ->
            gui.addItem(ItemBuilder.from(Material.PAPER)
                .name(Component.text("${player.name!!} 的创造区"))
                .lore(
                    Component.text("§r§7范围:"),
                    Component.text("  §r${range.minX}, ${range.minZ}§r"),
                    Component.text("  §r${range.maxX}, ${range.maxZ}§r"),
                    Component.text(""),
                    Component.text("§6左键: 传送§r")
                )
                .asGuiItem {
                    gui.close(sender)
                    onTp(sender, player.name)
                }
            )
        }
    }
}