package city.newnan.foundation.commands

import city.newnan.foundation.FoundationPlugin
import city.newnan.foundation.i18n.LanguageKeys
import city.newnan.foundation.commands.admin.ReloadCommand
import city.newnan.foundation.commands.admin.QueryCommand
import city.newnan.foundation.commands.admin.BalanceCommand
import city.newnan.foundation.commands.admin.AllocateCommand
import city.newnan.foundation.commands.user.DonateCommand
import city.newnan.foundation.commands.user.TopCommand
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotation.specifier.Greedy
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Default
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.bukkit.CloudBukkitCapabilities
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.minecraft.extras.MinecraftHelp
import org.incendo.cloud.minecraft.extras.RichDescription
import org.incendo.cloud.paper.LegacyPaperCommandManager
import org.incendo.cloud.suggestion.Suggestion
import org.incendo.cloud.annotations.AnnotationParser
import java.util.concurrent.CompletableFuture

/**
 * Foundation命令注册器
 *
 * 基于Cloud框架的注解驱动命令系统，提供：
 * - 注解驱动的命令定义
 * - 自动参数解析和验证
 * - 与i18n系统深度集成
 * - 自动补全和帮助系统
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class CommandRegistry(val plugin: FoundationPlugin) {

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

    val help: MinecraftHelp<CommandSender>

    init {
        // 解析注解并注册命令
        // 按照最佳实践：除了帮助命令+主命令，其他命令都在各自的类中实现
        listOf(
            // 帮助命令+主命令
            this,
            // 用户命令
            DonateCommand(plugin),
            TopCommand(plugin),
            // 管理员命令
            ReloadCommand(plugin),
            QueryCommand(plugin),
            BalanceCommand(plugin),
            AllocateCommand(plugin)
        ).forEach { commandAnnotationParser.parse(it) }

        // 生成帮助指令
        help = MinecraftHelp.createNative("/foundation", commandManager)
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

    // 帮助指令
    @Command("foundation|fund help [help-query]")
    @CommandDescription(LanguageKeys.Commands.Help.DESCRIPTION)
    fun helpCommand(sender: CommandSender, @Greedy @Default("") @Argument(value = "help-query", description = LanguageKeys.Commands.Help.QUERY) query: String) {
        help.queryCommands(query, sender) // 所有插件的帮助命令都这样写
    }

    // 主命令
    @Command("foundation|fund")
    @CommandDescription(LanguageKeys.Commands.Main.DESCRIPTION)
    fun mainCommand(sender: CommandSender) {
        helpCommand(sender, "") // 主命令默认显示帮助
    }
}
