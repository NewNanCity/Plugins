package city.newnan.newnanmain.gui

import city.newnan.feefly.api.FeeFlyService
import city.newnan.gui.component.singleslot.onLeftClick
import city.newnan.gui.dsl.*
import city.newnan.newnanmain.NewNanMainPlugin
import city.newnan.newnanmain.i18n.LanguageKeys
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType

/**
 * 主菜单GUI
 *
 * 提供牛腩小镇的主要功能入口，包括：
 * - 传送中心
 * - 前缀管理
 * - 其他功能
 *
 * @author NewNanCity
 * @since 2.0.0
 */
fun openMainMenuGui(plugin: NewNanMainPlugin, player: Player) {
    plugin.openPage(InventoryType.CHEST, 54, player, LanguageKeys.Gui.Main.TITLE) {
        // 边框填充
        borderFillComponent(0, 0, 9, 6) {
            fill(Material.BLACK_STAINED_GLASS_PANE)
        }

        // 第一行功能按钮
    // 传送按钮
        slotComponent(x = 1, y = 1) {
            render {
                item(Material.COMPASS) {
            name(LanguageKeys.Gui.Main.TELEPORT)
                }
            }
            onLeftClick { _, _, _ ->
                openTeleportGui(plugin, player)
            }
        }

    // 称号按钮
        slotComponent(x = 3, y = 1) {
            render {
                item(Material.NAME_TAG) {
            name(LanguageKeys.Gui.Main.PREFIX)
                }
            }
            onLeftClick { _, _, _ ->
                openPlayerPrefixGui(plugin, player, player)
            }
        }

        // 飞行按钮
        slotComponent(x = 5, y = 1) {
            render {
                item(Material.FIREWORK_ROCKET) {
                    name(LanguageKeys.Gui.Main.FLY)
                }
            }
            onLeftClick { _, _, _ ->
                if (plugin.isPluginPresent("FeeFly")) {
                    val feeFlyService = plugin.getService(FeeFlyService::class.java)
                    feeFlyService?.toggleFly(player) ?: run {
                        player.performCommand("fly")
                    }
                }
                this@openPage.close()
            }
        }

        // TPA按钮
        slotComponent(x = 7, y = 1) {
            render {
                item(Material.ENDER_PEARL) {
                    name(LanguageKeys.Gui.Main.TPA)
                }
            }
            onLeftClick { _, _, _ ->
                player.performCommand("tpa")
                this@openPage.close()
            }
        }

        // 第二行功能按钮
        // 牛腩书局按钮
        slotComponent(x = 2, y = 2) {
            render {
                item(Material.WRITABLE_BOOK) {
                    name(LanguageKeys.Gui.Main.BOOK)
                }
            }
            onLeftClick { _, _, _ ->
                player.performCommand("book")
                this@openPage.close()
            }
        }

        // 创造区按钮
        slotComponent(x = 4, y = 2) {
            render {
                item(Material.WOODEN_AXE) {
                    name(LanguageKeys.Gui.Main.CREATE_AREA)
                }
            }
            onLeftClick { _, _, _ ->
                player.performCommand("ctp")
                this@openPage.close()
            }
        }

        // 慈善榜按钮
        slotComponent(x = 6, y = 2) {
            render {
                item(Material.EMERALD) {
                    name(LanguageKeys.Gui.Main.CHARITY)
                }
            }
            onLeftClick { _, _, _ ->
                player.performCommand("ftop")
                this@openPage.close()
            }
        }

        // 第三行功能按钮
        // 成就按钮
        slotComponent(x = 1, y = 3) {
            render {
                item(Material.OAK_SIGN) {
                    name(LanguageKeys.Gui.Main.ACHIEVEMENT)
                }
            }
            onLeftClick { _, _, _ ->
                player.performCommand("aach list")
                this@openPage.close()
            }
        }

        // 小镇按钮
        slotComponent(x = 3, y = 3) {
            render {
                urlSkull("cf7cdeefc6d37fecab676c584bf620832aaac85375e9fcbff27372492d69f") {
                    name(LanguageKeys.Gui.Main.TOWN)
                }
            }
            onLeftClick { _, _, _ ->
                player.performCommand("town")
                this@openPage.close()
            }
        }

        // 新人指南按钮
        slotComponent(x = 5, y = 3) {
            render {
                item(Material.WRITTEN_BOOK) {
                    name(LanguageKeys.Gui.Main.GUIDE)
                }
            }
            onLeftClick { _, _, _ ->
                plugin.server.dispatchCommand(plugin.server.consoleSender, "book open ${player.name} b7cc4a26-ab98-400c-bd89-809ea964d2a4")
                this@openPage.close()
            }
        }

        // 熊服查询按钮
        slotComponent(x = 7, y = 3) {
            render {
                item(Material.TNT) {
                    name(LanguageKeys.Gui.Main.INSPECT)
                }
            }
            onLeftClick { _, _, _ ->
                player.performCommand("co i")
                this@openPage.close()
            }
        }

        // 管理按钮（仅管理员可见）
        if (player.hasPermission("newnan.admin")) {
            slotComponent(x = 7, y = 5) {
                render {
                    item(Material.COMMAND_BLOCK) {
                        name(LanguageKeys.Gui.Main.ADMIN)
                    }
                }
                onLeftClick { _, _, _ ->
                    openAdminGui(plugin, player)
                }
            }
        }

        // 返回按钮
        slotComponent(x = 8, y = 5) {
            render {
                item(Material.ARROW) {
                    name(LanguageKeys.Gui.Common.BACK)
                }
            }
            onLeftClick { _, _, _ ->
                back()
            }
        }
    }
}
