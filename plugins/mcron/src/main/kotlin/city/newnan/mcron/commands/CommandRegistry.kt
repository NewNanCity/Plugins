package city.newnan.mcron.commands

import city.newnan.mcron.MCronPlugin
import city.newnan.mcron.commands.admin.ReloadCommand
import city.newnan.mcron.i18n.LanguageKeys
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
 * MCron命令注册器
 *
 * 使用Cloud框架的注解驱动命令系统，负责注册所有MCron命令。
 * 严格按照文档模板实现，不允许修改固定部分。
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class CommandRegistry(val plugin: MCronPlugin) {
    // 创建命令管理器 - 固定模板，不允许修改
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

    // 创建注解解析器 - 固定模板，不允许修改
    val commandAnnotationParser = AnnotationParser(commandManager, CommandSender::class.java).also {
        // i18n 映射，并支持 Adventure 组件
        it.descriptionMapper { it -> RichDescription.of(plugin.messager.sprintf(it)) }
    }

    val help: MinecraftHelp<CommandSender>

    init {
        // 解析注解 - 按功能划分到不同的类中
        listOf(
            // 帮助命令+主指令
            this,
            // 管理员命令
            ReloadCommand(plugin),
        ).forEach { commandAnnotationParser.parse(it) }

        // 生成帮助指令 - /mcron 是插件的指令前缀
        help = MinecraftHelp.createNative("/mcron", commandManager)
    }

    // 为help的查询提供补全 - 固定模板，不允许修改
    @Suggestions("help-query")
    fun helpQuerySuggestions(ctx: CommandContext<CommandSender>, input: String) = CompletableFuture.supplyAsync {
        commandManager.createHelpHandler()
            .queryRootIndex(ctx.sender())
            .entries()
            .map { Suggestion.suggestion(it.syntax()) }
            .toList()
    }

    // 帮助指令 - 固定模板，不允许修改
    @Command("mcron help [help-query]")
    @CommandDescription(LanguageKeys.Commands.Help.DESCRIPTION)
    fun helpCommand(sender: CommandSender, @Greedy @Default("") @Argument(value = "help-query", description = LanguageKeys.Commands.Help.QUERY) query: String) {
        help.queryCommands(query, sender) // 所有插件的帮助命令都这样写
    }

    // 主命令 - 根据插件需求自定义行为
    @Command("mcron")
    @CommandDescription(LanguageKeys.Commands.Main.DESCRIPTION)
    fun mainCommand(sender: CommandSender) {
        helpCommand(sender, "") // MCron主命令显示帮助信息
    }
}
