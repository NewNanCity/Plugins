package city.newnan.foundation.gui

import city.newnan.foundation.FoundationPlugin
import city.newnan.foundation.i18n.LanguageKeys
import city.newnan.foundation.repository.TransferRankEntry
import city.newnan.foundation.manager.TransferManager
import city.newnan.gui.component.paginated.onLeftClick
import city.newnan.gui.component.singleslot.onLeftClick
import city.newnan.gui.dataprovider.AsyncDataProvider
import city.newnan.gui.dsl.borderFillComponent
import city.newnan.gui.dsl.item
import city.newnan.gui.dsl.openPage
import city.newnan.gui.dsl.paginatedComponent
import city.newnan.gui.dsl.runAsync
import city.newnan.gui.dsl.runSync
import city.newnan.gui.dsl.skull
import city.newnan.gui.dsl.slotComponent
import city.newnan.gui.dsl.urlSkull
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import java.math.BigDecimal

/**
 * Foundation插件排行榜GUI
 *
 * 使用GUI模块实现的现代化排行榜界面：
 * - 分页显示捐赠排行榜
 * - 当前玩家高亮显示
 * - 实时数据更新
 * - 优雅的用户界面
 *
 * @author NewNanCity
 * @since 2.0.0
 */
/**
 * 打开排行榜GUI
 */
fun openTopGui(plugin: FoundationPlugin, player: Player) {
    plugin.openPage(
        InventoryType.CHEST,
        size = 54,
        player = player,
        title = LanguageKeys.Gui.Top.TITLE
    ) {
        // 创建边框
        borderFillComponent(0, 0, 9, 6) {
            fill(Material.BLACK_STAINED_GLASS_PANE)
        }

        // 创建异步数据提供器
        val dataProvider = TopRankDataProvider(plugin.transferManager)

        // 使用分页组件显示排行榜
        val topListComponent = paginatedComponent(
            startX = 1, startY = 1,
            width = 7, height = 4,
            dataProvider = dataProvider,
        ) {
            render { context ->
                val entry = context.item ?: return@render null
                val rank = context.globalIndex + 1
                val isCurrentPlayer = entry.uuid == player.uniqueId

                skull(entry.uuid) {
                    name(format(
                        if (isCurrentPlayer) LanguageKeys.Gui.Top.RANK_SELF else LanguageKeys.Gui.Top.RANK,
                        rank,
                        plugin.server.getOfflinePlayer(entry.uuid).name ?: "?"
                    ))
                    lore(formatPlain(
                        LanguageKeys.Gui.Top.RANK_LORE,
                        rank,
                        formatAmount(entry.active),
                        formatAmount(entry.passive),
                        formatAmount(entry.total)
                    ))
                    if (rank <= 3 || isCurrentPlayer) {
                        enchant(org.bukkit.enchantments.Enchantment.LURE, if (rank == 1) 3 else 1)
                        flag(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
                    }
                }
            }

            onLeftClick { context, index, entry ->
                // 异步显示详细信息，避免阻塞主线程
                runAsync {
                    val entryData = entry!!
                    val playerName = plugin.server.getOfflinePlayer(entryData.uuid).name ?: "Unknown"
                    val active = formatAmount(entryData.active)
                    val passive = formatAmount(entryData.passive)
                    val total = formatAmount(entryData.total)

                    // 回到主线程发送消息
                    runSync {
                        plugin.messager.printf(player, LanguageKeys.Gui.Top.PLAYER_DETAILS_HEADER, playerName)
                        plugin.messager.printf(player, LanguageKeys.Gui.Top.PLAYER_ACTIVE, active)
                        plugin.messager.printf(player, LanguageKeys.Gui.Top.PLAYER_PASSIVE, passive)
                        plugin.messager.printf(player, LanguageKeys.Gui.Top.PLAYER_TOTAL, total)
                    }
                }
            }
        }

        // 上一页按钮
        slotComponent(0, 5) {
            render {
                urlSkull("37aee9a75bf0df7897183015cca0b2a7d755c63388ff01752d5f4419fc645") {
                    name(LanguageKeys.Gui.Common.PREVIOUS_PAGE)
                    lore(formatPlain(LanguageKeys.Gui.Top.PAGE_LORE, topListComponent.getCurrentPage(), topListComponent.totalPages))
                }
            }
            onLeftClick { _, _, _ ->
                topListComponent.previousPage()
            }
        }

        // 下一页按钮
        slotComponent(8, 5) {
            render {
                urlSkull("682ad1b9cb4dd21259c0d75aa315ff389c3cef752be3949338164bac84a96e") {
                    name(LanguageKeys.Gui.Common.NEXT_PAGE)
                    lore(formatPlain(LanguageKeys.Gui.Top.PAGE_LORE, topListComponent.getCurrentPage() + 1, topListComponent.totalPages))
                }
            }
            onLeftClick { _, _, _ ->
                topListComponent.nextPage()
            }
        }

        // 统计信息按钮
        slotComponent(1, 5) {
            render {
                item(Material.PAPER) {
                    name(LanguageKeys.Gui.Top.STATS)

                    val totalPlayers = plugin.transferManager.getRecordCount()
                    val firstPageResult = plugin.transferManager.getTopDonors(0, 1000) // 获取前1000条记录
                    val totalActive = firstPageResult.items.sumOf { it.active }
                    val totalPassive = firstPageResult.items.sumOf { it.passive }
                    val totalDonations = totalActive.add(totalPassive)
                    lore(formatPlain(
                        LanguageKeys.Gui.Top.STATS_LORE,
                        totalPlayers,
                        formatAmount(totalActive),
                        formatAmount(totalPassive),
                        formatAmount(totalDonations)
                    ))
                }
            }
            onLeftClick { _, _, _ ->
                // 异步获取详细统计信息，避免阻塞主线程
                runAsync {
                    try {
                        val totalPlayers = plugin.transferManager.getRecordCount()
                        val (totalActive, totalPassive) = plugin.transferManager.getTotalDonations()
                        val totalDonations = totalActive.add(totalPassive)
                        val avgDonation = if (totalPlayers > 0)
                            totalDonations.divide(BigDecimal(totalPlayers), 2, java.math.RoundingMode.HALF_UP)
                            else BigDecimal.ZERO

                        // 回到主线程发送消息
                        runSync {
                            plugin.messager.printf(player, LanguageKeys.Gui.Top.DETAILED_STATS_HEADER)
                            plugin.messager.printf(player, LanguageKeys.Gui.Top.STATS_TOTAL_PLAYERS, totalPlayers)
                            plugin.messager.printf(player, LanguageKeys.Gui.Top.STATS_TOTAL_ACTIVE, formatAmount(totalActive))
                            plugin.messager.printf(player, LanguageKeys.Gui.Top.STATS_TOTAL_PASSIVE, formatAmount(totalPassive))
                            plugin.messager.printf(player, LanguageKeys.Gui.Top.STATS_GRAND_TOTAL, formatAmount(totalDonations))
                            plugin.messager.printf(player, LanguageKeys.Gui.Top.STATS_AVERAGE, formatAmount(avgDonation))
                        }
                    } catch (e: Exception) {
                        plugin.logger.error("Failed to get detailed stats", e)
                        runSync {
                            plugin.messager.printf(player, LanguageKeys.Gui.Top.OPERATION_FAILED)
                        }
                    }
                }
            }
        }

        // 返回按钮 - 位置 (0,0)
        slotComponent(0, 0) {
            render {
                item(Material.BARRIER) {
                    name(LanguageKeys.Gui.Common.BACK)
                    lore(LanguageKeys.Gui.Common.BACK_LORE)
                }
            }
            onLeftClick { _, _, _ ->
                this@openPage.back()
            }
        }
    }
}

private fun formatAmount(amount: BigDecimal): String {
    return String.format("%.2f", amount.toDouble())
}

/**
 * 排行榜异步数据提供器
 *
 * 专为Foundation插件排行榜设计的异步数据提供器
 * 通过调用transferManager来获得分页的排行榜数据
 *
 * @param transferManager 传输管理器，用于获取排行榜数据
 */
class TopRankDataProvider(
    private val transferManager: TransferManager
) : AsyncDataProvider<TransferRankEntry>(
    sizeProvider = { callback ->
        try {
            val count = transferManager.getRecordCount()
            callback(Result.success(count))
        } catch (e: Exception) {
            callback(Result.failure(e))
        }
    },
    itemProvider = { offset, limit, callback ->
        try {
            val pageResult = transferManager.getTopDonors(offset, limit)
            callback(Result.success(pageResult.items))
        } catch (e: Exception) {
            callback(Result.success(emptyList()))
        }
    }
)