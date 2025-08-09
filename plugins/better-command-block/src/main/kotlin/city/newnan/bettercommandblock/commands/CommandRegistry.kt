package city.newnan.bettercommandblock.commands

import city.newnan.bettercommandblock.BetterCommandBlockPlugin
import city.newnan.bettercommandblock.commands.impl.ExtendedCommands
import city.newnan.bettercommandblock.i18n.LanguageKeys
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotation.specifier.Greedy
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Default
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.bukkit.CloudBukkitCapabilities
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.help.result.CommandEntry
import org.incendo.cloud.minecraft.extras.MinecraftHelp
import org.incendo.cloud.paper.LegacyPaperCommandManager
import org.incendo.cloud.suggestion.Suggestion
import java.util.concurrent.CompletableFuture

/**
 * 命令注册器
 *
 * 基于Cloud框架的命令系统，提供完整的命令注册、权限管理和帮助系统。
 * 使用注解驱动的方式定义命令，支持自动补全和参数验证。
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class CommandRegistry(val plugin: BetterCommandBlockPlugin) {

    // Cloud命令管理器
    val commandManager = LegacyPaperCommandManager.createNative(
        plugin,
        ExecutionCoordinator.asyncCoordinator()
    ).also {
        // 启用Brigadier支持（如果可用）
        if (it.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            it.registerBrigadier()
        } else if (it.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            it.registerAsynchronousCompletions()
        }
    }

    // 注解解析器
    val commandAnnotationParser = AnnotationParser(commandManager, CommandSender::class.java)

    // 帮助系统
    val help: MinecraftHelp<CommandSender>

    init {
        try {
            // 注册所有命令类
            val commandClasses = listOf(
                this, // 基础命令
                ExtendedCommands(plugin) // 扩展命令
            )

            // 解析并注册所有命令
            commandClasses.forEach { commandClass ->
                val commands = commandAnnotationParser.parse(commandClass)
                commands.forEach { commandManager.command(it) }
            }

            // 初始化帮助系统
            help = MinecraftHelp.createNative("/cb", commandManager)

            plugin.logger.info(LanguageKeys.Log.Info.COMMAND_REGISTERED, "CommandRegistry")

        } catch (e: Exception) {
            plugin.logger.error(LanguageKeys.Log.Error.INITIALIZATION_FAILED, e)
            throw e
        }
    }

    // ==================== 基础命令 ====================

    /**
     * 重载命令
     */
    @Command("cb reload")
    @CommandDescription(LanguageKeys.Commands.Reload.DESCRIPTION)
    @Permission("better-command-block.reload")
    fun reloadCommand(sender: CommandSender) {
        try {
            plugin.reloadPlugin()
            plugin.messager.printf(sender, LanguageKeys.Commands.Reload.SUCCESS)
        } catch (e: Exception) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Reload.FAILED, e.message)
            plugin.logger.error(LanguageKeys.Log.Error.COMMAND_EXECUTION_FAILED, e)
        }
    }

    /**
     * 防火墙状态命令
     */
    @Command("cb firewall status")
    @CommandDescription(LanguageKeys.Firewall.Commands.Status.DESCRIPTION)
    @Permission("better-command-block.firewall.status")
    fun firewallStatusCommand(sender: CommandSender) {
        try {
            val config = plugin.firewallModule.getFirewallConfig()
            val trie = plugin.firewallModule.getCommandTrie()
            val validators = plugin.firewallModule.getValidators()

            val status = if (config.enabled) "启用" else "禁用"
            val trieSize = trie.size()
            val whitelistCount = config.whitelistCommands.size
            val validatorCount = validators.size
            val configVersion = "2.0.0"

            plugin.messager.printf(
                sender,
                LanguageKeys.Firewall.Commands.Status.SUCCESS,
                status, trieSize, whitelistCount, validatorCount, configVersion
            )
        } catch (e: Exception) {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.OPERATION_FAILED, e.message)
            plugin.logger.error(LanguageKeys.Log.Error.COMMAND_EXECUTION_FAILED, e)
        }
    }

    /**
     * 防火墙统计命令
     */
    @Command("cb firewall stats [reset]")
    @CommandDescription(LanguageKeys.Firewall.Commands.Stats.DESCRIPTION)
    @Permission("better-command-block.firewall.stats")
    fun firewallStatsCommand(
        sender: CommandSender,
        @Argument(value = "reset", description = "重置统计信息") @Default("") reset: String
    ) {
        try {
            if (reset.equals("reset", ignoreCase = true)) {
                plugin.firewallModule.resetStatistics()
                plugin.messager.printf(sender, LanguageKeys.Firewall.Commands.Stats.RESET_SUCCESS)
                return
            }

            val stats = plugin.firewallModule.getStatistics()
            val totalCommands = stats["totalCommands"] as Long
            val blockedCommands = stats["blockedCommands"] as Long
            val allowedCommands = stats["allowedCommands"] as Long
            val validationErrors = stats["validationErrors"] as Long
            val blockRate = stats["blockRate"] as Double
            val allowRate = 100.0 - blockRate
            val avgValidationTime = stats["averageValidationTime"] as Double
            val trieSize = stats["trieSize"] as Long

            @Suppress("UNCHECKED_CAST")
            val topCommands = stats["topCommands"] as Map<String, Long>
            val topCommandsStr = topCommands.entries
                .take(5)
                .joinToString("\n") { "  <yellow>${it.key}:</yellow> ${it.value}" }

            plugin.messager.printf(
                sender,
                LanguageKeys.Firewall.Commands.Stats.SUCCESS,
                totalCommands, blockedCommands, String.format("%.2f", blockRate),
                allowedCommands, String.format("%.2f", allowRate), validationErrors,
                String.format("%.0f", avgValidationTime), trieSize, topCommandsStr
            )
        } catch (e: Exception) {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.OPERATION_FAILED, e.message)
            plugin.logger.error(LanguageKeys.Log.Error.COMMAND_EXECUTION_FAILED, e)
        }
    }

    /**
     * 防火墙测试命令
     */
    @Command("cb firewall test <command>")
    @CommandDescription(LanguageKeys.Firewall.Commands.Test.DESCRIPTION)
    @Permission("better-command-block.firewall.test")
    fun firewallTestCommand(
        sender: CommandSender,
        @Greedy @Argument(value = "command", description = "要测试的命令") command: String
    ) {
        try {
            val isCommandSafe = plugin.firewallModule.isCommandSafe(command)

            if (isCommandSafe) {
                plugin.messager.printf(sender, LanguageKeys.Firewall.Commands.Test.COMMAND_SAFE, command)
            } else {
                plugin.messager.printf(sender, LanguageKeys.Firewall.Commands.Test.COMMAND_BLOCKED, command)
            }
        } catch (e: Exception) {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.OPERATION_FAILED, e.message)
            plugin.logger.error(LanguageKeys.Log.Error.COMMAND_EXECUTION_FAILED, e)
        }
    }

    /**
     * 防火墙重载命令
     */
    @Command("cb firewall reload")
    @CommandDescription(LanguageKeys.Firewall.Commands.Reload.DESCRIPTION)
    @Permission("better-command-block.firewall.reload")
    fun firewallReloadCommand(sender: CommandSender) {
        try {
            plugin.firewallModule.onReload()
            plugin.messager.printf(sender, LanguageKeys.Firewall.Commands.Reload.SUCCESS)
        } catch (e: Exception) {
            plugin.messager.printf(sender, LanguageKeys.Firewall.Commands.Reload.FAILED, e.message)
            plugin.logger.error(LanguageKeys.Log.Error.COMMAND_EXECUTION_FAILED, e)
        }
    }

    // 为help的查询提供补全
    @Suggestions("help-query")
    fun helpQuerySuggestions(ctx: CommandContext<CommandSender>, input: String) = CompletableFuture.supplyAsync {
        commandManager.createHelpHandler()
            .queryRootIndex(ctx.sender())
            .entries()
            .map { Suggestion.suggestion(it.syntax()) }
            .toList()
    }

    /**
     * 帮助命令
     */
    @Command("cb help [help-query]")
    @CommandDescription(LanguageKeys.Commands.Help.DESCRIPTION)
    fun helpCommand(
        sender: CommandSender,
        @Greedy @Default("") @Argument(value = "help-query", description = LanguageKeys.Commands.Help.QUERY) query: String
    ) {
        help.queryCommands(query, sender) // 所有插件的帮助命令都这样写
    }

    /**
     * 默认命令（显示帮助）
     */
    @Command("cb")
    @CommandDescription(LanguageKeys.Commands.Help.DESCRIPTION)
    fun defaultCommand(sender: CommandSender) {
        helpCommand(sender, "")
    }
}
