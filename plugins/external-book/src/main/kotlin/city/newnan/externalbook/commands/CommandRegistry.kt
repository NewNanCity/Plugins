package city.newnan.externalbook.commands

import city.newnan.externalbook.ExternalBookPlugin
import city.newnan.externalbook.commands.admin.AdminCommand
import city.newnan.externalbook.commands.admin.GiveCommand
import city.newnan.externalbook.commands.admin.OpenCommand
import city.newnan.externalbook.commands.admin.ReloadCommand
import city.newnan.externalbook.commands.user.ExportCommand
import city.newnan.externalbook.commands.user.GuiCommand
import city.newnan.externalbook.commands.user.ImportCommand
import city.newnan.externalbook.commands.user.PublishCommand
import city.newnan.externalbook.commands.user.StripCommand
import city.newnan.externalbook.i18n.LanguageKeys
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

// ====== 从这里开始是固定模板 ======
class CommandRegistry(val plugin: ExternalBookPlugin) {
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
        // 相关键：
        // LanguageKeys
        // ├── Commands
        // │   ├── Common 一些通用的信息
        // │   │   ├── NoPermission 无权限执行
        // │   │   └── UUIDInvalid UUID无效
        // │   ├── Help
        // │   │   ├── Description 指令的描述
        // │   │   └── Query 查询参数的描述
        // │   └── Foo
        // │       ├── Description 指令的描述
        // │       ├── String1 参数的描述
        // │       └── String2 参数的描述
        // └── ... 其他模块的语言键
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
            // 用户命令
            ImportCommand(plugin),
            ExportCommand(plugin),
            PublishCommand(plugin),
            StripCommand(plugin),
            GuiCommand(plugin),
            // 管理员命令
            AdminCommand(plugin),
            ReloadCommand(plugin),
            OpenCommand(plugin),
            GiveCommand(plugin)
        ).forEach { commandAnnotationParser.parse(it) }

        // 生成帮助指令 /externalbook 是插件的指令前缀
        help = MinecraftHelp.createNative("/externalbook", commandManager)
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
    @Command("externalbook|book help [help-query]")
    @CommandDescription(LanguageKeys.Commands.Help.DESCRIPTION)
    fun helpCommand(sender: CommandSender, @Greedy @Default("") @Argument(value = "help-query", description = LanguageKeys.Commands.Help.QUERY) query: String) {
        help.queryCommands(query, sender) // 所有插件的帮助命令都这样写
    }

    // 主命令
    @Command("externalbook|book")
    @CommandDescription(LanguageKeys.Commands.Main.DESCRIPTION)
    fun mainCommand(sender: CommandSender) {
        helpCommand(sender, "") // 不同插件的主命令行为可能不同，比如展示帮助、打印信息或者打开某个gui都有可能
    }
}