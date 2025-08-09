package city.newnan.guardian.gui

import city.newnan.core.scheduler.runAsync
import city.newnan.core.scheduler.runSync
import city.newnan.guardian.GuardianPlugin
import city.newnan.guardian.i18n.LanguageKeys
import city.newnan.guardian.model.*
import city.newnan.gui.component.paginated.onRightClick
import city.newnan.gui.component.singleslot.onLeftClick
import city.newnan.gui.dataprovider.AsyncDataProvider
import city.newnan.gui.dsl.borderFillComponent
import city.newnan.gui.dsl.item
import city.newnan.gui.dsl.openPage
import city.newnan.gui.dsl.paginatedComponent
import city.newnan.gui.dsl.skull
import city.newnan.gui.dsl.slotComponent
import city.newnan.gui.dsl.urlSkull
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryType

class TownMembersProvider(
    private val town: Town,
    private val plugin: GuardianPlugin
) : AsyncDataProvider<Player>(
    sizeProvider = { callback ->
        plugin.runAsync {
            val count = plugin.playerDB.getTownMembersCount(town.id)
            callback(Result.success(count))
        }
    },
    itemProvider = { offset, limit, callback ->
        plugin.runAsync {
            val items = plugin.playerDB.getTownMembers(town.id, offset, limit)
            callback(Result.success(items))
        }
    }
)

fun openTownGui(plugin: GuardianPlugin, who: org.bukkit.entity.Player, targetTownName: String?) {
    plugin.runAsync {
        try {
            // 查询玩家数据
            val playerData = plugin.playerDB.getPlayerByName(who.name)
            if (playerData == null) {
                plugin.runSync { _ ->
                    plugin.messager.printf(who, LanguageKeys.Core.Error.PLAYER_NOT_FOUND, who.name)
                }
                return@runAsync Unit
            }

            // 确定要查看的小镇
            val town = if (targetTownName != null) {
                // 查看指定小镇
                plugin.playerDB.findTownByName(targetTownName)
            } else {
                // 查看玩家自己的小镇
                if (playerData.town != null) {
                    plugin.playerDB.getTownById(playerData.town!!)
                } else {
                    null
                }
            }

            if (town == null) {
                plugin.runSync { _ ->
                    if (targetTownName != null) {
                        plugin.messager.printf(who, LanguageKeys.Commands.Town.NOT_FOUND, targetTownName)
                    } else {
                        plugin.messager.printf(who, LanguageKeys.Commands.Town.NO_TOWN)
                    }
                }
                return@runAsync Unit
            }

            val provider = TownMembersProvider(town, plugin)
            val canEdit = who.hasPermission("guardian.town.write.other") || town.leader == playerData.id
            val leader = town.leader?.let { plugin.playerDB.getPlayerById(it) }

            val townLevel = if (town.level < 0) {
                plugin.messager.sprintfPlain(LanguageKeys.Gui.Town.LEVEL_CANCELLED) // "<red>已注销</red>"
            } else {
                val tLevel = minOf(town.level, 5)
                "<gold>".padEnd(tLevel+2, '★').padEnd(7, '☆').endsWith("</gold>")
            }
            val title = plugin.messager.sprintfPlain(LanguageKeys.Gui.Town.TITLE, town.name, townLevel) // "<gray>[<aqua><bold>牛腩小镇</bold></aqua>]</gray> <bold>${town.name}</bold> $townLevel"

            plugin.runSync { _ ->
                plugin.openPage(InventoryType.CHEST, 54, who, title) {
            borderFillComponent(0, 0, 9, 6) {
                fill(Material.BLACK_STAINED_GLASS_PANE)
            }

            val paginatedComponent = paginatedComponent(startX=1, startY=1, width=7, height=4, provider) {
                render { cxt ->
                    val memberPlayer = Bukkit.getOfflinePlayer(cxt.item!!.name)
                    skull(memberPlayer) {
                        name(Component.text(cxt.item!!.name))
                        if (canEdit) {
                            lore(formatPlain(LanguageKeys.Gui.Town.MEMBER_LORE_EDITABLE,
                                cxt.item!!.qq ?: formatPlain(LanguageKeys.Gui.Town.UNKNOWN),
                                if (cxt.item!!.banned) formatPlain(LanguageKeys.Gui.Town.BAN_YES) else formatPlain(LanguageKeys.Gui.Town.BAN_NO)
                            ))
                        } else {
                            lore(formatPlain(LanguageKeys.Gui.Town.MEMBER_LORE_READONLY,
                                cxt.item!!.qq ?: formatPlain(LanguageKeys.Gui.Town.UNKNOWN),
                                if (cxt.item!!.banned) formatPlain(LanguageKeys.Gui.Town.BAN_YES) else formatPlain(LanguageKeys.Gui.Town.BAN_NO)
                            ))
                        }
                    }
                }
                onRightClick { cxt, index, member ->
                    if (!canEdit) return@onRightClick
                    if (cxt.isShiftClick) return@onRightClick
                    // 移除玩家的小镇
                    plugin.playerDB.updatePlayerTown(member!!.id, null)
                    plugin.messager.printf(who, LanguageKeys.Gui.Town.PLAYER_REMOVED_SUCCESS, member.name) // "<green>已成功删除玩家: ${member.name}</green>"
                    this@paginatedComponent.clearCache()
                    this@paginatedComponent.update()
                }
            }

            // 上一页按钮
            slotComponent(2, 5) {
                render {
                    urlSkull("37aee9a75bf0df7897183015cca0b2a7d755c63388ff01752d5f4419fc645") {
                        name(LanguageKeys.Gui.Town.PREVIOUS_PAGE) // "上一页"
                    }
                }
                onLeftClick { _, _, _ ->
                    paginatedComponent.previousPage()
                }
            }

            // 下一页按钮
            slotComponent(6, 5) {
                render {
                    urlSkull("682ad1b9cb4dd21259c0d75aa315ff389c3cef752be3949338164bac84a96e") {
                        name(LanguageKeys.Gui.Town.NEXT_PAGE) // "下一页"
                    }
                }
                onLeftClick { _, _, _ ->
                    paginatedComponent.nextPage()
                }
            }

            // 关闭按钮
            slotComponent(8, 5) {
                render {
                    item(Material.BARRIER) {
                        name(LanguageKeys.Gui.Town.CLOSE) // "关闭"
                    }
                }
                onLeftClick { _, _, _ ->
                    this@openPage.close()
                }
            }

            slotComponent(0, 0) {
                render {
                    if (leader == null) {
                        item(Material.BLACK_STAINED_GLASS_PANE) {
                            name("")
                        }
                    } else {
                        skull(leader.name) {
                            name(format(LanguageKeys.Gui.Town.LEADER_LABEL, leader.name)) // "<gold><bold>镇长</bold></gold><gray>: </gray><white>${leader.name}</white>"
                        }
                    }
                }
            }

            if (town.qqgroup != null) {
                slotComponent(1, 0) {
                    render {
                        item(Material.BELL) {
                            name(format(LanguageKeys.Gui.Town.QQ_GROUP_LABEL, town.qqgroup)) // "<gold><bold>QQ群</bold></gold><gray>: </gray><white>${town.qqgroup}</white>"
                        }
                    }
                }
            }

            if (canEdit) {
                slotComponent(1, 9) {
                    render {
                        urlSkull("9a2d891c6ae9f6baa040d736ab84d48344bb6b70d7f1a280dd12cbac4d777") {
                            name(LanguageKeys.Gui.Town.ADD_MEMBER_BUTTON) // "<green><bold>添加成员</bold></green>"
                        }
                    }
                    onLeftClick { _, _, _ ->
                        chatInput(true) { input ->
                            if (input == "cancel") {
                                plugin.messager.printf(session.player, LanguageKeys.Gui.Town.OPERATION_CANCELLED) // "<red>已取消操作</red>"
                                session.show()
                                return@chatInput true
                            }
                            val record = plugin.playerDB.getPlayerByName(input)
                            if (record == null) {
                                plugin.messager.printf(session.player, LanguageKeys.Gui.Town.PLAYER_NOT_FOUND, input) // "<red>找不到玩家: $input, 请重新输入, 或者输入<red> cancel </red><green>取消操作</green>"
                                return@chatInput false
                            }
                            if (record.town != null) {
                                plugin.messager.printf(session.player, LanguageKeys.Gui.Town.PLAYER_ALREADY_IN_TOWN, input) // "<red>玩家: $input 已经加入了其他镇, 请重新输入, 或者输入<red> cancel </red><green>取消操作</green>"
                                return@chatInput false
                            }
                            // 更新玩家的小镇
                            plugin.playerDB.updatePlayerTown(record.id, town.id)
                            paginatedComponent.clearCache()
                            plugin.messager.printf(session.player, LanguageKeys.Gui.Town.PLAYER_ADDED_SUCCESS, input) // "<green>已成功添加玩家: $input</green>"
                            session.show()
                            return@chatInput true
                        }.also {
                            if (it) {
                                plugin.messager.printf(session.player, LanguageKeys.Gui.Town.INPUT_PLAYER_NAME) // "<green>请直接在聊天框输入要添加的玩家名字, 或者输入<red> cancel </red><green>取消操作</green>"
                                session.hide()
                            } else {plugin
                                plugin.messager.printf(session.player, LanguageKeys.Gui.Town.INPUT_IN_PROGRESS) // "<red>你正在进行其他输入, 请先取消之!</red>"
                            }
                        }
                    }
                }
            }
        }
            }
        } catch (e: Exception) {
            plugin.runSync {
                plugin.messager.printf(who, LanguageKeys.Core.Error.OPERATION_FAILED, e.message ?: "Unknown error")
            }
        }
    }
}