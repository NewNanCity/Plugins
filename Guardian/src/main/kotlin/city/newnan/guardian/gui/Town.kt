package city.newnan.guardian.gui

import city.newnan.guardian.PluginMain
import city.newnan.guardian.model.*
import city.newnan.violet.gui.PlayerGuiSession
import city.newnan.violet.item.getSkull
import city.newnan.violet.item.toSkull
import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.ktorm.entity.toList

fun openTownGui(session: PlayerGuiSession, who: org.bukkit.entity.Player, player: Player, town: Town) {
    val leader = town.leader?.getPlayer()
    var needUpdate = true
    var members = town.getMembers().toList()

    val townLevel = if (town.level < 0) {
        "§c已注销"
    } else {
        val tLevel = minOf(town.level, 5)
        "§6".padEnd(tLevel+2, '★').padEnd(7, '☆')
    }
    val gui = Gui.paginated().rows(6).title(Component.text("§7[§3§l牛腩小镇§r§7]§r §l${town.name} §r$townLevel")).create()
    gui.setItem(6, 3, ItemBuilder.from("37aee9a75bf0df7897183015cca0b2a7d755c63388ff01752d5f4419fc645".toSkull())
        .name(Component.text("上一页")).asGuiItem { gui.previous() })
    gui.setItem(6, 7, ItemBuilder.from("682ad1b9cb4dd21259c0d75aa315ff389c3cef752be3949338164bac84a96e".toSkull())
        .name(Component.text("下一页")).asGuiItem { gui.next() })
    gui.setItem(6, 9, ItemBuilder.from(Material.BARRIER).name(Component.text("关闭")).asGuiItem {
        session.back()
    })
    listOf(1,2,4,5,6,8).forEach { gui.setItem(6, it, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE)
                    .name(Component.text("")).asGuiItem()) }
    listOf(2,3,4,5,6,7,8).forEach { gui.setItem(1, it, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE)
                    .name(Component.text("")).asGuiItem()) }
    gui.setDefaultClickAction { it.isCancelled = true }
    session.open(gui,
        { _, _, _ ->
            if (needUpdate) {
                val canEdit = who.hasPermission("guardian.town.write.other") || town.leader == player.id
                val leaderPlayer = if (leader == null) null else PluginMain.INSTANCE.playerNameMap[leader.name]
                // 镇长头像
                if (leaderPlayer == null) {
                    gui.setItem(1, 1, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).name(Component.text("")).asGuiItem())
                } else {
                    gui.setItem(1, 1, ItemBuilder.from(leaderPlayer.getSkull())
                        .name(Component.text("§6§l镇长§r§7: §f${leaderPlayer.name}"))
                        .asGuiItem())
                }
                if (town.qqGroup != null) {
                    gui.setItem(1, 1, ItemBuilder.from(Material.BELL).name(Component.text("QQ群: §6§l${town.qqGroup}")).asGuiItem())
                }
                // 添加成员
                if (canEdit) {
                    gui.setItem(1, 9, ItemBuilder.from("9a2d891c6ae9f6baa040d736ab84d48344bb6b70d7f1a280dd12cbac4d777".toSkull())
                        .name(Component.text("§a§l添加成员"))
                        .asGuiItem {
                            session.chatInput { input ->
                                if (input == "cancel") {
                                    PluginMain.INSTANCE.message.printf(session.player, "§c已取消操作")
                                    session.show()
                                    return@chatInput true
                                }
                                val target = PluginMain.INSTANCE.playerNameMap[input]
                                val record = input.findPlayer()
                                if (target == null || record == null) {
                                    PluginMain.INSTANCE.message.printf(session.player, "§c找不到玩家: $input, 请重新输入, 或者输入§c cancel §a取消操作")
                                    return@chatInput false
                                }
                                if (record.town != null) {
                                    PluginMain.INSTANCE.message.printf(session.player, "§c玩家: $input 已经加入了其他镇, 请重新输入, 或者输入§c cancel §a取消操作")
                                    return@chatInput false
                                }
                                record.town = town
                                record.flushChanges()
                                members = town.getMembers().toList()
                                needUpdate = true
                                PluginMain.INSTANCE.message.printf(session.player, "§a已成功添加玩家: $input")
                                session.show()
                                return@chatInput true
                            }.also {
                                if (it) {
                                    PluginMain.INSTANCE.message.printf(session.player, "§a请直接在聊天框输入要添加的玩家名字, 或者输入§c cancel §a取消操作")
                                    session.hide()
                                } else {
                                    PluginMain.INSTANCE.message.printf(session.player, "§c你正在进行其他输入, 请先取消之!")
                                }
                            }
                        }
                    )
                } else {
                    gui.setItem(1, 9, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).name(Component.text("")).asGuiItem())
                }
                gui.clearPageItems()
                // 成员列表
                members.forEach { member ->
                    if (member.id == town.leader) return@forEach
                    val memberPlayer = PluginMain.INSTANCE.playerNameMap[member.name] ?: return@forEach
                    gui.addItem(ItemBuilder.from(memberPlayer.getSkull())
                        .name(Component.text(member.name)).also {
                            if (canEdit) {
                                it.lore(
                                    Component.text("§7QQ: §f${member.qq ?: "未知"}"),
                                    Component.text("§7封禁: §f${if (member.isBan()) "§c是" else "§a否"}"),
                                    Component.text(""),
                                    Component.text("§6右键: 删除成员")
                                )
                            } else {
                                it.lore(
                                    Component.text("§7QQ: §f${member.qq ?: "未知"}"),
                                    Component.text("§7封禁: §f${if (member.isBan()) "§c是" else "§a否"}")
                                )
                            }
                        }.asGuiItem {
                            if (canEdit) {
                                if (it.isRightClick && !it.isShiftClick) {
                                    member.town = null
                                    member.flushChanges()
                                    members = town.getMembers().toList()
                                    needUpdate = true
                                    PluginMain.INSTANCE.message.printf(session.player, "§a已成功删除玩家: ${member.name}")
                                    session.refresh()
                                }
                            }
                        })
                }
                needUpdate = false
                return@open true
            }
            false
        }
    )
}