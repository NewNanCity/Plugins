package city.newnan.feefly.commands

import city.newnan.feefly.FeeFlyPlugin
import city.newnan.feefly.commands.admin.ListCommand
import city.newnan.feefly.commands.user.FlyCommand
import city.newnan.feefly.i18n.LanguageKeys
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
import org.incendo.cloud.minecraft.extras.MinecraftHelp
import org.incendo.cloud.minecraft.extras.RichDescription
import org.incendo.cloud.paper.LegacyPaperCommandManager
import org.incendo.cloud.suggestion.Suggestion
import java.util.concurrent.CompletableFuture

/**
 * FeeFly命令注册器
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
class CommandRegistry(val plugin: FeeFlyPlugin) {

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
            FlyCommand(plugin),
            // 管理员命令
            ListCommand(plugin)
        ).forEach { commandAnnotationParser.parse(it) }

        // 生成帮助指令
        help = MinecraftHelp.createNative("/feefly", commandManager)
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
    @Command("feefly help [help-query]")
    @CommandDescription(LanguageKeys.Commands.Help.DESCRIPTION)
    @Permission("feefly.self")
    fun helpCommand(
        sender: CommandSender,
        @Greedy @Default("") @Argument(value = "help-query", description = LanguageKeys.Commands.Help.QUERY)
        query: String
    ) {
        help.queryCommands(query, sender)
    }

    // 主命令
    @Command("feefly")
    @CommandDescription(LanguageKeys.Commands.Help.DESCRIPTION)
    @Permission("feefly.self")
    fun mainCommand(sender: CommandSender) {
        helpCommand(sender, "")
    }
}
