package city.newnan.railarea

import city.newnan.railarea.config.RailArea
import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.bukkit.WorldEditPlugin
import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import net.kyori.adventure.text.Component
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.*
import org.bukkit.command.BlockCommandSender
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

data class Range3DWorld(val world: World, val range: Range3D)

fun Player.getSelection(): Range3DWorld? {
    try {
        val worldEdit = server.pluginManager.getPlugin("WorldEdit") ?: return null
        if (worldEdit is WorldEditPlugin) {
            val region = worldEdit.getSession(player).getSelection(BukkitAdapter.adapt(world)) ?: return null
            val min = region.minimumPoint
            val max = region.maximumPoint
            return Range3DWorld(Bukkit.getWorld(region.world!!.name)!!, Range3D(min.x, min.y, min.z, max.x, max.y, max.z))
        }
    } catch (e: Exception) { return null }
    return null
}

fun handleAreaInput (sender: Player, name: String, oldArea: RailArea?, done: (area: RailArea) -> Unit): (String) -> Boolean {
    var title: String? = oldArea?.title
    var subtitle: String? = oldArea?.title
    var actionbar: String? = oldArea?.actionBar
    var area: Range3D? = oldArea?.range3D
    var world: World? = oldArea?.world
    val preview = fun (p: String?) {
        sender.sendTitle(title, subtitle, 1, 70, 2)
        if (actionbar != null)
            sender.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent(actionbar))
        if (p == null) {
            PluginMain.INSTANCE.messageManager.printf(sender, "效果预览已显示")
        } else {
            PluginMain.INSTANCE.messageManager.printf(sender, "&2$p&r 已设定, 效果预览已显示，请继续设置其他属性!")
        }
    }
    return fun (input: String): Boolean {
        when {
            input.startsWith("title:") -> {
                title = ChatColor.translateAlternateColorCodes('&', input.substring(6).trim())
                if (title!!.isBlank()) title = null
                preview("标题")
            }
            input.startsWith("subtitle:") -> {
                subtitle = ChatColor.translateAlternateColorCodes('&', input.substring(9).trim())
                if (subtitle!!.isBlank()) subtitle = null
                preview("子标题")
            }
            input.startsWith("actionbar:") -> {
                actionbar = ChatColor.translateAlternateColorCodes('&', input.substring(10).trim())
                if (actionbar!!.isBlank()) actionbar = null
                preview("活动栏")
            }
            input.startsWith("area:") -> {
                val areaStr = input.substring(5).trim()
                if (areaStr.isEmpty()) {
                    val areaw = sender.getSelection()
                    if (areaw == null) {
                        PluginMain.INSTANCE.messageManager.printf(sender, "&c请先用小木斧选择一个区域!")
                    } else {
                        world = areaw.world
                        area = areaw.range
                        area?.also { a ->
                            PluginMain.INSTANCE.messageManager.printf(sender,
                                "根据你小木斧选择的区域，已设定区域范围为: ${world!!.name} (${
                                    a.minX}, ${a.minY}, ${a.minZ}) - (${a.maxX}, ${a.maxY}, ${a.maxZ})")
                        }
                    }
                } else {
                    val range = areaStr.split(" ")
                    if (range.size != 7) {
                        PluginMain.INSTANCE.messageManager.printf(sender, "&c区域范围格式错误! 请使用 world x1 y1 z1 x2 y2 z2 的格式!")
                    } else {
                        try {
                            world = Bukkit.getWorld(range[0])
                            if (world == null) {
                                PluginMain.INSTANCE.messageManager.printf(sender, "&c世界 ${range[0]} 不存在!")
                            } else {
                                val minX = range[1].toInt()
                                val minY = range[2].toInt()
                                val minZ = range[3].toInt()
                                val maxX = range[4].toInt()
                                val maxY = range[5].toInt()
                                val maxZ = range[6].toInt()
                                area = Range3D(minX, minY, minZ, maxX, maxY, maxZ)
                                area?.also { a ->
                                PluginMain.INSTANCE.messageManager.printf(sender,
                                    "已设定区域范围为: ${world!!.name} (${
                                        a.minX}, ${a.minY}, ${a.minZ}) - (${a.maxX}, ${a.maxY}, ${a.maxZ})")
                                }
                            }
                        } catch (e: NumberFormatException) {
                            PluginMain.INSTANCE.messageManager.printf(sender, "&c区域范围格式错误! 请使用 world x1 y1 z1 x2 y2 z2 的格式!")
                        }
                    }
                }
            }
            input == "preview" -> {
                preview(null)
            }
            input == "ok" -> {
                if (title == null) {
                    PluginMain.INSTANCE.messageManager.printf(sender, "&c请先设定标题!")
                } else if (area == null || world == null) {
                    PluginMain.INSTANCE.messageManager.printf(sender, "&c请先设定区域范围!")
                } else {
                    val other = PluginMain.INSTANCE.getArea(world!!, area!!)
                    if (other != null && other != oldArea) {
                        PluginMain.INSTANCE.messageManager.printf(sender,
                            "&c新设定的区域范围与 &2${other.name}&r 相冲突!")
                        return false
                    }
                    val x = RailArea(name, world!!, area!!, title!!, subtitle, actionbar)
                    done(x)
                    return true
                }
            }
        }
        return false
    }
}

@CommandAlias("railarea")
object Commands : BaseCommand() {
    @Default
    @HelpCommand
    @Subcommand("help")
    fun help(sender: CommandSender, help: CommandHelp) {
        help.showHelp()
    }

    fun aaa(sender: BlockCommandSender) {
        sender.block.state.blockData
    }

    @Subcommand("gui")
    @Description("打开图形界面")
    fun showGui(sender: Player) {
        val gui = Gui.paginated()
            .title(Component.text("铁路区域列表"))
            .rows(6)
            .create()
        gui.setItem(6, 3, ItemBuilder.from(Material.PAPER).name(Component.text("上一页")).asGuiItem {
            gui.previous()
        })
        gui.setItem(6, 7, ItemBuilder.from(Material.PAPER).name(Component.text("下一页")).asGuiItem {
            gui.next()
        })
        listOf(1,2,4,5,6,8).forEach { gui.setItem(6, it, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE)
            .name(Component.text("§r")).asGuiItem()) }
        gui.setItem(6, 9, ItemBuilder.from(Material.BARRIER).name(Component.text("关闭")).asGuiItem {
            gui.close(it.whoClicked)
        })
        gui.setDefaultClickAction { it.isCancelled = true }

        PluginMain.INSTANCE.eachAreas { area ->
            val item = ItemStack(Material.RAIL).also {
                it.itemMeta = it.itemMeta?.also { meta ->
                    meta.setDisplayName(area.name)
                    meta.lore = listOf(
                        "§r§7世界: §r${area.world.name}§r",
                        "§r§7范围:",
                        "  §r${area.range3D.minX}, ${area.range3D.minY}, ${area.range3D.minZ}§r",
                        "  §r${area.range3D.maxX}, ${area.range3D.maxY}, ${area.range3D.maxZ}§r",
                        "§r§7标题: §r§f${area.title}§r",
                        "§r§7子标题: §r§f${area.subTitle ?: ""}§r",
                        "§r§7活动栏: §r§f${area.actionBar ?: ""}§r",
                        "",
                        "§6右键: 传送§r",
                        "§6shift + 左键: 修改§r",
                        "§6shift + 右键: 删除§r",
                    )
                }
            }
            gui.addItem(ItemBuilder.from(item).asGuiItem {
                val player = if(it.whoClicked is Player) it.whoClicked as Player else return@asGuiItem
                if (it.isShiftClick) {
                    if (PluginMain.INSTANCE.getArea(area.name) == null) {
                        PluginMain.INSTANCE.messageManager.printf(player, "&c该区域不存在!")
                        return@asGuiItem
                    }
                    if (it.isLeftClick) {
                        player.performCommand("railarea update ${area.name}")
                        gui.close(player)
                    } else if (it.isRightClick) {
                        player.performCommand("railarea remove ${area.name}")
                        gui.close(player)
                    }
                } else if (it.isRightClick) {
                    if (PluginMain.INSTANCE.getArea(area.name) == null) {
                        PluginMain.INSTANCE.messageManager.printf(player, "&c该区域不存在!")
                        return@asGuiItem
                    }
                    player.teleport(Location(area.world, area.range3D.minX.toDouble(), area.range3D.minY.toDouble(), area.range3D.minZ.toDouble()))
                    PluginMain.INSTANCE.messageManager.printf(player, "&a传送成功!")
                    gui.close(player)
                }
            })
        }
        gui.open(sender)
    }

    @Subcommand("reload")
    @CommandPermission("rail-area.reload")
    @Description("重载插件")
    fun reloadConfig(sender: CommandSender) {
        PluginMain.INSTANCE.reload()
        PluginMain.INSTANCE.messageManager.printf(sender, "插件重载完毕!")
    }

    @Subcommand("add|new")
    @CommandPermission("rail-area.edit")
    @Syntax("<name> &e- 添加一个铁路区域")
    fun addRailArea(sender: Player, name: String) {
        if (PluginMain.INSTANCE.getArea(name) != null) {
            PluginMain.INSTANCE.messageManager.printf(sender, "&c该区域已存在! 请使用 /railarea update $name 来更新该区域!")
            return
        }
        PluginMain.INSTANCE.messageManager.printf(sender, "开始设置区域 &2$name&r，接下来请设定区域的属性:")
        PluginMain.INSTANCE.messageManager.gets(sender, handleAreaInput(sender, name, null) { area ->
            PluginMain.INSTANCE.addArea(area)
            PluginMain.INSTANCE.messageManager.printf(sender, "区域 &2$name&r 已创建!")
        })
    }

    @Subcommand("remove|del|delete")
    @CommandPermission("rail-area.edit")
    @Syntax("<name> &e- 移除一个铁路区域")
    fun removeRailArea(sender: Player, name: String) {
        if (PluginMain.INSTANCE.getArea(name) == null) {
            PluginMain.INSTANCE.messageManager.printf(sender, "&c该区域不存在!")
            return
        }
        PluginMain.INSTANCE.removeArea(name)
        PluginMain.INSTANCE.messageManager.printf(sender, "区域 &2$name&r 已删除!")
    }

    @Subcommand("update|edit")
    @CommandPermission("rail-area.edit")
    @Syntax("<name> &e- 更新一个铁路区域")
    fun updateRailArea(sender: Player, name: String) {
        val oldArea = PluginMain.INSTANCE.getArea(name)
        if (oldArea == null) {
            PluginMain.INSTANCE.messageManager.printf(sender, "&c该区域不存在! 请使用 /railarea add $name 来添加该区域!")
            return
        }
        PluginMain.INSTANCE.messageManager.printf(sender, "开始设置区域 &2$name&r，接下来请设定区域的属性:")
        PluginMain.INSTANCE.messageManager.gets(sender, handleAreaInput(sender, name, oldArea) { area ->
            PluginMain.INSTANCE.updateArea(area)
            PluginMain.INSTANCE.messageManager.printf(sender, "区域 &2$name&r 已更新!")
        })
    }
}