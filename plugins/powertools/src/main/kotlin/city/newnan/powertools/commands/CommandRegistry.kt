package city.newnan.powertools.commands

import city.newnan.powertools.PowerToolsPlugin
import city.newnan.powertools.commands.admin.ReloadCommand
import city.newnan.powertools.commands.user.SkullCommand
import city.newnan.powertools.i18n.LanguageKeys
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
import org.incendo.cloud.help.result.CommandEntry
import org.incendo.cloud.minecraft.extras.MinecraftHelp
import org.incendo.cloud.minecraft.extras.RichDescription
import org.incendo.cloud.paper.LegacyPaperCommandManager
import org.incendo.cloud.suggestion.Suggestion
import java.util.concurrent.CompletableFuture

/**
 * PowerTools命令注册器
 *
 * 基于Cloud框架的命令系统，提供：
 * - 注解驱动的命令定义
 * - 自动补全支持
 * - i18n集成
 * - 帮助系统
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class CommandRegistry(val plugin: PowerToolsPlugin) {

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
        // 注册所有命令类
        val commandClasses = listOf(
            ReloadCommand(plugin),
            SkullCommand(plugin)
        )

        // 解析注解并注册命令
        commandClasses.forEach { commandClass ->
            val commands = commandAnnotationParser.parse(commandClass)
            commands.forEach { commandManager.command(it) }
        }

        // 生成帮助指令
        help = MinecraftHelp.createNative("/powertools", commandManager)
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
    @Command("powertools help [help-query]")
    @CommandDescription(LanguageKeys.Commands.Help.DESCRIPTION)
    fun helpCommand(sender: CommandSender, @Greedy @Default("") @Argument(value = "help-query", description = LanguageKeys.Commands.Help.QUERY) query: String) {
        help.queryCommands(query, sender) // 所有插件的帮助命令都这样写
    }

    // 主命令
    @Command("powertools")
    @CommandDescription(LanguageKeys.Commands.Main.DESCRIPTION)
    fun mainCommand(sender: CommandSender) {
        helpCommand(sender, "") // 显示帮助信息
    }
}
