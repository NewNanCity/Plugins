package city.newnan.dynamiceconomy.commands

import city.newnan.dynamiceconomy.DynamicEconomyPlugin
import city.newnan.dynamiceconomy.i18n.LanguageKeys
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Default
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.bukkit.CloudBukkitCapabilities
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.minecraft.extras.MinecraftHelp
import org.incendo.cloud.minecraft.extras.RichDescription
import org.incendo.cloud.paper.LegacyPaperCommandManager
import java.math.BigDecimal
import org.bukkit.inventory.ItemStack
import city.newnan.core.utils.PlayerUtils

/**
 * DynamicEconomy插件命令注册器
 *
 * 使用Cloud框架的注解驱动命令系统
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class CommandRegistry(val plugin: DynamicEconomyPlugin) {

    // 创建命令管理器
    val commandManager = LegacyPaperCommandManager.createNative(
        plugin,
        ExecutionCoordinator.asyncCoordinator()
    ).also {
        if (it.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            it.registerBrigadier()
        } else if (it.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            it.registerAsynchronousCompletions()
        }
    }

    // 创建注解解析器
    val commandAnnotationParser = AnnotationParser(commandManager, CommandSender::class.java).also {
        // i18n 映射，支持 Adventure 组件
        it.descriptionMapper { key -> RichDescription.of(plugin.messager.sprintf(key)) }
    }

    // 帮助系统
    private val help: MinecraftHelp<CommandSender>

    init {
        // 注册所有命令
        val commands = commandAnnotationParser.parse(this)
        commands.forEach { commandManager.command(it) }

        // 初始化帮助系统（保持旧版主命令前缀）
        help = MinecraftHelp.createNative("/dynamicaleconomy help", commandManager)

        plugin.logger.info("Commands registered successfully")
    }

    // ===== 主命令 =====

    @Command("dynamicaleconomy|de")
    @CommandDescription(LanguageKeys.Commands.Main.DESCRIPTION)
    fun mainCommand(sender: CommandSender) {
        if (sender is Player) {
            // 玩家执行时显示统计信息
            statsCommand(sender)
        } else {
            // 控制台执行时显示帮助
            helpCommand(sender, "")
        }
    }

    // ===== 帮助命令 =====

    @Command("dynamicaleconomy|de help [help-query] | economy help [help-query]")
    @CommandDescription(LanguageKeys.Commands.Help.DESCRIPTION)
    fun helpCommand(
        sender: CommandSender,
        @Default("") @Argument(value = "help-query", description = LanguageKeys.Commands.Help.QUERY) query: String
    ) {
        help.queryCommands(query, sender)
    }

    // ===== 重载命令 =====

    @Command("dynamicaleconomy|de reload")
    @CommandDescription(LanguageKeys.Commands.Reload.DESCRIPTION)
    @Permission("dynamicaleconomy.reload")
    fun reloadCommand(sender: CommandSender) {
        try {
            plugin.reloadPlugin()
            plugin.messager.printf(sender, LanguageKeys.Commands.Reload.SUCCESS)
            plugin.logger.info(LanguageKeys.Commands.Reload.LOG_SUCCESS, sender.name)
        } catch (e: Exception) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Reload.FAILED, e.message ?: "未知错误")
            plugin.logger.error(LanguageKeys.Commands.Reload.LOG_FAILED, e, sender.name)
        }
    }

    // ===== 统计命令 =====

    @Command("dynamicaleconomy|de stats")
    @CommandDescription(LanguageKeys.Commands.Stats.DESCRIPTION)
    @Permission("dynamicaleconomy.stats")
    fun statsCommand(sender: CommandSender) {
        val wealthManager = plugin.getWealthManager()
        val economyManager = plugin.getEconomyManager()

        // 显示财富统计
        plugin.messager.printf(sender, LanguageKeys.Commands.Stats.WEALTH_HEADER)
        plugin.messager.printf(sender, LanguageKeys.Wealth.TOTAL_WEALTH, wealthManager.getTotalWealth().toString())

        val resourceCount = wealthManager.getResourceCount()
        if (resourceCount.isNotEmpty()) {
            resourceCount.forEach { (material, count) ->
                plugin.messager.printf(sender, LanguageKeys.Wealth.RESOURCE_COUNT, material.name, count)
            }
        }

        // 显示经济统计
        plugin.messager.printf(sender, LanguageKeys.Commands.Stats.ECONOMY_HEADER)
        plugin.messager.printf(sender, LanguageKeys.Economy.CURRENCY_ISSUANCE, economyManager.getCurrencyIssuance().toString())
        plugin.messager.printf(sender, LanguageKeys.Economy.NATIONAL_TREASURY, economyManager.getNationalTreasury().toString())
        plugin.messager.printf(sender, "§7参考货币指数: §e${economyManager.referenceCurrencyIndex}")
        plugin.messager.printf(sender, "§7收购货币指数: §e${economyManager.buyCurrencyIndex}")
        plugin.messager.printf(sender, "§7出售货币指数: §e${economyManager.sellCurrencyIndex}")

        // 显示商品统计
        val commodityManager = plugin.getCommodityManager()
        val commodities = commodityManager.getAllCommodities()

        plugin.messager.printf(sender, LanguageKeys.Commands.Stats.COMMODITY_HEADER)
        if (commodities.isEmpty()) {
            plugin.messager.printf(sender, LanguageKeys.Commodity.NOT_FOUND, "无商品")
        } else {
            commodities.forEach { (name, commodity) ->
                plugin.messager.printf(sender, "$name: 库存=${commodity.getStock()}, 买入=${commodity.getBuyPrice()}, 卖出=${commodity.getSellPrice()}")
            }
        }
    }

    // ===== 商品命令 =====

    @Command("dynamicaleconomy|de commodity list")
    @CommandDescription(LanguageKeys.Commands.Commodity.LIST_DESCRIPTION)
    @Permission("dynamicaleconomy.use")
    fun commodityListCommand(sender: CommandSender) {
        val commodityManager = plugin.getCommodityManager()
        val commodities = commodityManager.getAllCommodities()

        if (commodities.isEmpty()) {
            plugin.messager.printf(sender, LanguageKeys.Commodity.NOT_FOUND, "无商品")
            return
        }

        plugin.messager.printf(sender, "=== 商品列表 ===")
        commodities.forEach { (name, commodity) ->
            plugin.messager.printf(sender, "§e$name§f:")
            plugin.messager.printf(sender, "  §7库存: §f${commodity.getStock()}")
            plugin.messager.printf(sender, "  §7买入价: §a${commodity.getBuyPrice()}")
            plugin.messager.printf(sender, "  §7卖出价: §c${commodity.getSellPrice()}")
        }
    }

    @Command("dynamicaleconomy|de commodity info <name>")
    @CommandDescription(LanguageKeys.Commands.Commodity.INFO_DESCRIPTION)
    @Permission("dynamicaleconomy.use")
    fun commodityInfoCommand(
        sender: CommandSender,
        @Argument(value = "name", description = LanguageKeys.Commands.Commodity.NAME_ARG) name: String
    ) {
        val commodityManager = plugin.getCommodityManager()
        val commodity = commodityManager.getCommodity(name)

        if (commodity == null) {
            plugin.messager.printf(sender, LanguageKeys.Commodity.NOT_FOUND, name)
            return
        }

        plugin.messager.printf(sender, "=== 商品信息: $name ===")
        plugin.messager.printf(sender, LanguageKeys.Commodity.STOCK, commodity.getStock())
        plugin.messager.printf(sender, LanguageKeys.Commodity.BUY_PRICE, commodity.getBuyPrice())
        plugin.messager.printf(sender, LanguageKeys.Commodity.SELL_PRICE, commodity.getSellPrice())
    }

    @Command("dynamicaleconomy|de commodity buy <name> <amount>")
    @CommandDescription(LanguageKeys.Commands.Commodity.BUY_DESCRIPTION)
    @Permission("dynamicaleconomy.use")
    fun commodityBuyCommand(
        sender: Player,
        @Argument(value = "name", description = LanguageKeys.Commands.Commodity.NAME_ARG) name: String,
        @Argument(value = "amount", description = LanguageKeys.Commands.Commodity.AMOUNT_ARG) amount: Long
    ) {
        val commodityManager = plugin.getCommodityManager()
        val commodity = commodityManager.getCommodity(name)

        if (commodity == null) {
            plugin.messager.printf(sender, LanguageKeys.Commodity.NOT_FOUND, name)
            return
        }

        if (commodity.getStock() < amount) {
            plugin.messager.printf(sender, LanguageKeys.Commodity.INSUFFICIENT_STOCK)
            return
        }

        val totalPrice = commodity.getSellPrice().multiply(BigDecimal.valueOf(amount))
        val economyManager = plugin.getEconomyManager()

        if (!economyManager.hasEnoughBalance(sender, totalPrice)) {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.INSUFFICIENT_FUNDS)
            return
        }

        // 执行交易（系统 -> 玩家）：先扣钱与扣库存，再发物品
        if (economyManager.executeTransaction(sender, totalPrice, false) &&
            commodityManager.sellCommodity(name, amount)) {
            // 发放物品（若绑定了物品原型）
            val template: ItemStack? = commodity.itemStack
            if (template != null) {
                var remaining = amount.toInt()
                while (remaining > 0) {
                    val give = minOf(remaining, template.maxStackSize)
                    val stack = template.clone()
                    stack.amount = give
                    PlayerUtils.giveItem(sender, stack)
                    remaining -= give
                }
            }
            plugin.messager.printf(sender, LanguageKeys.Commodity.BUY_SUCCESS, amount, name, totalPrice)
        } else {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.OPERATION_FAILED, "交易失败")
        }
    }

    @Command("dynamicaleconomy|de commodity sell <name> <amount>")
    @CommandDescription(LanguageKeys.Commands.Commodity.SELL_DESCRIPTION)
    @Permission("dynamicaleconomy.use")
    fun commoditySellCommand(
        sender: Player,
        @Argument(value = "name", description = LanguageKeys.Commands.Commodity.NAME_ARG) name: String,
        @Argument(value = "amount", description = LanguageKeys.Commands.Commodity.AMOUNT_ARG) amount: Long
    ) {
        val commodityManager = plugin.getCommodityManager()
        val commodity = commodityManager.getCommodity(name)

        if (commodity == null) {
            plugin.messager.printf(sender, LanguageKeys.Commodity.NOT_FOUND, name)
            return
        }

        val template: ItemStack? = commodity.itemStack
        if (template == null) {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.OPERATION_FAILED, "该商品未绑定具体物品，无法出售")
            return
        }

        // 检查玩家背包是否有足够数量的相似物品
        val owned = countSimilarItems(sender, template)
        if (owned < amount) {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.OPERATION_FAILED, "物品数量不足")
            return
        }

        val totalPrice = commodity.getBuyPrice().multiply(BigDecimal.valueOf(amount))
        val economyManager = plugin.getEconomyManager()

        // 执行交易（玩家 -> 系统）：先收钱，再入库，最后移除玩家物品
        if (economyManager.executeTransaction(sender, totalPrice, true) &&
            commodityManager.buyCommodity(name, amount)) {
            removeSimilarItems(sender, template, amount.toInt())
            plugin.messager.printf(sender, LanguageKeys.Commodity.SELL_SUCCESS, amount, name, totalPrice)
        } else {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.OPERATION_FAILED, "交易失败")
        }
    }

    // ===== 发行货币命令 =====

    @Command("dynamicaleconomy|de issue <amount>")
    @CommandDescription(LanguageKeys.Commands.Issue.DESCRIPTION)
    @Permission("dynamicaleconomy.issue")
    fun issueCommand(
        sender: CommandSender,
        @Argument(value = "amount", description = LanguageKeys.Commands.Issue.AMOUNT_ARG) amount: Double
    ) {
        try {
            val economyManager = plugin.getEconomyManager()
            economyManager.issueCurrency(amount)
            economyManager.updateCurrencyIndex()

            plugin.messager.printf(sender, LanguageKeys.Commands.Issue.SUCCESS, amount)
            plugin.logger.info(LanguageKeys.Commands.Issue.LOG_SUCCESS, sender.name, amount)
        } catch (e: Exception) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Issue.FAILED, e.message ?: "未知错误")
            plugin.logger.error(LanguageKeys.Commands.Issue.LOG_FAILED, e, sender.name)
        }
    }

    // ===== 更新货币指数命令 =====

    @Command("dynamicaleconomy|de update-index")
    @CommandDescription(LanguageKeys.Commands.UpdateIndex.DESCRIPTION)
    @Permission("dynamicaleconomy.admin")
    fun updateIndexCommand(sender: CommandSender) {
        try {
            val economyManager = plugin.getEconomyManager()
            economyManager.updateCurrencyIndex()

            plugin.messager.printf(sender, LanguageKeys.Commands.UpdateIndex.SUCCESS)
            plugin.logger.info(LanguageKeys.Commands.UpdateIndex.LOG_SUCCESS, sender.name)
        } catch (e: Exception) {
            plugin.messager.printf(sender, LanguageKeys.Commands.UpdateIndex.FAILED, e.message ?: "未知错误")
            plugin.logger.error(LanguageKeys.Commands.UpdateIndex.LOG_FAILED, e, sender.name)
        }
    }

    // ===== 内部工具：统计与移除相似物品 =====
    private fun countSimilarItems(player: Player, template: ItemStack): Long {
        var count = 0L
        for (item in player.inventory.contents) {
            if (item != null && item.isSimilar(template)) {
                count += item.amount
            }
        }
        return count
    }

    private fun removeSimilarItems(player: Player, template: ItemStack, amount: Int) {
        var remaining = amount
        val inv = player.inventory
        for (slot in 0 until inv.size) {
            val item = inv.getItem(slot) ?: continue
            if (!item.isSimilar(template)) continue
            if (remaining <= 0) break
            val remove = minOf(remaining, item.amount)
            item.amount = item.amount - remove
            if (item.amount <= 0) inv.setItem(slot, null)
            remaining -= remove
        }
    }

    // ===== 重新统计货币发行量命令 =====

    @Command("dynamicaleconomy|de reload-issuance")
    @CommandDescription(LanguageKeys.Commands.ReloadIssuance.DESCRIPTION)
    @Permission("dynamicaleconomy.admin")
    fun reloadIssuanceCommand(sender: CommandSender) {
        try {
            val economyManager = plugin.getEconomyManager()
            economyManager.reloadCurrencyIssuance()
            economyManager.updateCurrencyIndex()

            plugin.messager.printf(sender, LanguageKeys.Commands.ReloadIssuance.SUCCESS)
            plugin.logger.info(LanguageKeys.Commands.ReloadIssuance.LOG_SUCCESS, sender.name)
        } catch (e: Exception) {
            plugin.messager.printf(sender, LanguageKeys.Commands.ReloadIssuance.FAILED, e.message ?: "未知错误")
            plugin.logger.error(LanguageKeys.Commands.ReloadIssuance.LOG_FAILED, e, sender.name)
        }
    }
}
