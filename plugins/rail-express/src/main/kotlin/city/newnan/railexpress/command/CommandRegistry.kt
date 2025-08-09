package city.newnan.railexpress.command

import city.newnan.railexpress.RailExpressPlugin
import city.newnan.railexpress.command.admin.ReloadCommand
import city.newnan.railexpress.i18n.LanguageKeys
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotation.specifier.Greedy
import org.incendo.cloud.annotations.AnnotationParser
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
import java.util.concurrent.CompletableFuture

/**
 * RailExpress命令注册器
 *
 * 负责注册所有RailExpress命令，基于Cloud框架实现。
 *
 * 使用模板化的命令管理器和注解解析器，支持：
 * - 主命令和别名
 * - 帮助系统
 * - i18n国际化
 * - 权限管理
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class CommandRegistry(val plugin: RailExpressPlugin) {
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
        // i18n 映射，并支持 Adventure 组件
        it.descriptionMapper { it -> RichDescription.of(plugin.messager.sprintf(it)) }
    }

    val help: MinecraftHelp<CommandSender>

    init {
        // 解析注解
        // 最佳实践应当是将指令方法按功能划分到不同的类/单例中
        // 除了帮助指令之外+主指令，其他指令都应当在各自的类中实现
        // 这样可以避免一个类过于庞大，难以维护
        listOf(
            // 帮助命令+主指令
            this,
            // 管理员命令
            ReloadCommand(plugin)
        ).forEach { commandAnnotationParser.parse(it) }

        // 生成帮助指令 /railexpress 是插件的指令前缀
        help = MinecraftHelp.createNative("/railexpress", commandManager)
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
    @Command("railexpress help [help-query]")
    @CommandDescription(LanguageKeys.Commands.Help.DESCRIPTION)
    fun helpCommand(sender: CommandSender, @Greedy @Default("") @Argument(value = "help-query", description = LanguageKeys.Commands.Help.QUERY) query: String) {
        help.queryCommands(query, sender) // 所有插件的帮助命令都这样写
    }

    // 主命令
    @Command("railexpress")
    @CommandDescription(LanguageKeys.Commands.Main.DESCRIPTION)
    fun mainCommand(sender: CommandSender) {
        helpCommand(sender, "") // 显示帮助信息
    }
}