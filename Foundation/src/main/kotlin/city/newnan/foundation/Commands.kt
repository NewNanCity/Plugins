package city.newnan.foundation

import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import org.bukkit.command.BlockCommandSender
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.math.RoundingMode
import java.util.*

@CommandAlias("foundation|fund")
object Commands : BaseCommand() {
    @HelpCommand
    @Subcommand("help")
    fun help(sender: CommandSender, help: CommandHelp) {
        help.showHelp()
    }

    @Subcommand("reload")
    @Description("重载插件")
    @CommandPermission("foundation.reload")
    fun reloadConfig(sender: CommandSender) {
        PluginMain.INSTANCE.reload()
        PluginMain.INSTANCE.messageManager.printf(sender, "插件重载完毕!")
    }

    @Subcommand("donate")
    @CommandAlias("donate")
    @Description("捐赠一定数量的牛腩币至基金会")
    @CommandPermission("foundation.donate")
    fun donate(sender: Player, amount: Double) {
        if (PluginMain.INSTANCE.targetAccount == null) {
            PluginMain.INSTANCE.messageManager.printf(sender, "§c基金会账户未设置, 请联系管理!")
        }
        if (amount < 0.0) {
            PluginMain.INSTANCE.messageManager.printf(sender, "§c请不要输入负数!")
            return
        }
        val amountT = amount.toBigDecimal().setScale(2, RoundingMode.HALF_UP)
        val amountTT = amountT.toDouble()
        if (PluginMain.INSTANCE.economy.getBalance(sender) < amountTT) {
            PluginMain.INSTANCE.messageManager.printf(sender, "§c余额不足, 无法捐赠!")
            return
        }
        PluginMain.INSTANCE.activeTransfer(sender, amountT)
        PluginMain.INSTANCE.economy.withdrawPlayer(sender, amountTT)
        PluginMain.INSTANCE.economy.depositPlayer(PluginMain.INSTANCE.targetAccount!!, amountTT)
        PluginMain.INSTANCE.messageManager.printf(sender, "您已向牛腩基金会慷慨捐款 §a${amountTT} ₦§r, 谢谢您让牛腩变得更好!")
    }

    @CommandAlias("sudo-donate")
    @Description("使用选择器选择玩家, 使其捐赠一定数额给基金会, 用于命令方块")
    @CommandPermission("foundation.donate.selector")
    fun sudoDonate(sender: BlockCommandSender, selector: String, amount: Double) {
        if (selector != "@p") {
            PluginMain.INSTANCE.messageManager.printf(sender, "只能用@p!")
            return
        }
        // find nearest player
        sender.block.world.getNearbyEntities(sender.block.location, 5.0, 5.0, 5.0).filterIsInstance<Player>()
            .minByOrNull { entity -> entity.location.distance(sender.block.location) }?.let { entity ->
                PluginMain.INSTANCE.messageManager.printf(sender, "已选择玩家 §a${entity.name} §r捐赠!")
                donate(entity, amount)
            } ?: run {
            PluginMain.INSTANCE.messageManager.printf(sender, "附近没有玩家!")
        }
    }

    @Subcommand("query")
    @Description("查询基金会余额")
    @CommandPermission("foundation.allocate")
    fun query(sender: CommandSender) {
        if (PluginMain.INSTANCE.targetAccount == null) {
            PluginMain.INSTANCE.messageManager.printf(sender, "§c基金会账户未设置, 请联系管理!")
        }
        val amount = PluginMain.INSTANCE.economy.getBalance(PluginMain.INSTANCE.targetAccount!!)
        PluginMain.INSTANCE.messageManager.printf(sender, "§a基金会余额为: §f${amount} ₦§r")
    }

    @Subcommand("allocate|pay")
    @CommandAlias("allocate")
    @Description("从基金会拨款一定数量的牛腩币至指定玩家")
    @CommandPermission("foundation.allocate")
    fun allocate(sender: CommandSender, amount: Double, target: String, reason: String) {
        if (PluginMain.INSTANCE.targetAccount == null) {
            PluginMain.INSTANCE.messageManager.printf(sender, "§c基金会账户未设置, 请联系管理!")
        }
        if (amount < 0.0) {
            PluginMain.INSTANCE.messageManager.printf(sender, "§c请不要输入负数!")
            return
        }
        if (reason.isEmpty()) {
            PluginMain.INSTANCE.messageManager.printf(sender, "§c请填写拨款原因!")
            return
        }
        val amountT = amount.toBigDecimal().setScale(2, RoundingMode.HALF_UP)
        val amountTT = amountT.toDouble()
        if (PluginMain.INSTANCE.economy.getBalance(PluginMain.INSTANCE.targetAccount!!) < amountTT) {
            PluginMain.INSTANCE.messageManager.printf(sender, "§c基金会余额不足, 无法拨款!")
            return
        }
        val targetAccount = PluginMain.INSTANCE.server.offlinePlayers.find { p -> p.name == target }
        if (targetAccount == null) {
            PluginMain.INSTANCE.messageManager.printf(sender, "§c玩家 §f${target} §r不存在!")
            return
        }
        PluginMain.INSTANCE.activeTransfer(targetAccount, -amountT)
        PluginMain.INSTANCE.economy.withdrawPlayer(PluginMain.INSTANCE.targetAccount!!, amountTT)
        PluginMain.INSTANCE.economy.depositPlayer(targetAccount, amountTT)
        PluginMain.INSTANCE.appendAllocationLog(Date(), if (sender is Player) sender else null, targetAccount, amountTT, reason)
        PluginMain.INSTANCE.messageManager.printf(sender, "您已向玩家 §a${target} §r拨款 §a${amountTT} ₦§r, 原因: §a${reason}§r (拨款日志记录在后台文件中)")
    }

    @Subcommand("top")
    @CommandAlias("fundtop|dnoatetop|donationtop|foundationtop|ftop|dtop")
    @Description("查看捐赠排行榜")
    @CommandPermission("foundation.top")
    fun top(sender: CommandSender, @Default("1") page: Int) {
        val list = PluginMain.INSTANCE.getTop()
        // each page 10 items
        val maxPage = (list.size + 9) / 10
        if (page < 1 || page > maxPage) {
            PluginMain.INSTANCE.messageManager.printf(sender, "§c页码超出范围!")
            return
        }
        val start = (page - 1) * 10
        val end = (start + 10).coerceAtMost(list.size)
        PluginMain.INSTANCE.messageManager.printf(sender, "§8 ========= §6牛腩基金会慈善榜 §r§7- §a第 §l$page §r§7/ §a§l$maxPage §r§a页§r§8 ========= ")
        for (i in start until end) {
            val record = list[i]
            PluginMain.INSTANCE.messageManager.printf(sender, "§8[§a${i + 1}§8] §7§l${record.name} §r§7-              §a${record.active}₦§ 9(+${record.passive}₦)§r")
        }
        PluginMain.INSTANCE.messageManager.printf(sender, "§8 ========= §6牛腩基金会慈善榜 §r§7- §a第 §l$page §r§7/ §a§l$maxPage §r§a页§r§8 ========= ")
        val indexOfSender = list.indexOfFirst { record -> record.name == sender.name }
        PluginMain.INSTANCE.messageManager.printf(sender, "您目前位于 §a第 §l${indexOfSender + 1} §r§a名§r   您可以使用 §a/fundtop <页码> §r查看更多!")
    }
}