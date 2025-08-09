package city.newnan.newnanmain.commands

import city.newnan.newnanmain.NewNanMainPlugin
import city.newnan.newnanmain.gui.openMainMenuGui
import city.newnan.newnanmain.i18n.LanguageKeys
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
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
 * NewNanMain插件命令注册器
 *
 * 使用Cloud框架的注解驱动命令系统
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class CommandRegistry(val plugin: NewNanMainPlugin) {

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

        // 初始化帮助系统
        help = MinecraftHelp.createNative("/newnan help", commandManager)

        plugin.logger.info("Commands registered successfully")
    }

    // ===== 主命令 =====

    @Command("newnan|nn")
    @Command("newnan|nn gui")
    @CommandDescription(LanguageKeys.Commands.Main.DESCRIPTION)
    fun mainCommand(sender: CommandSender) {
        if (sender is Player) {
            // 玩家执行时打开GUI
            openMainMenuGui(plugin, sender)
            plugin.messager.printf(sender, LanguageKeys.Commands.Gui.OPENED)
        } else {
            // 控制台执行时显示帮助
            helpCommand(sender, "")
        }
    }

    // ===== 帮助命令 =====

    // 为help的查询提供补全
    @Suggestions("help-query")
    fun helpQuerySuggestions(ctx: CommandContext<CommandSender>, input: String) = CompletableFuture.supplyAsync {
        commandManager.createHelpHandler()
            .queryRootIndex(ctx.sender())
            .entries()
            .map { Suggestion.suggestion(it.syntax()) }
            .toList()
    }

    @Command("newnan help [help-query]")
    @CommandDescription(LanguageKeys.Commands.Help.DESCRIPTION)
    fun helpCommand(
        sender: CommandSender,
        @Default("") @Argument(value = "help-query", description = LanguageKeys.Commands.Help.QUERY) query: String
    ) {
        help.queryCommands(query, sender)
    }

    // ===== 重载命令 =====

    @Command("newnan reload")
    @CommandDescription(LanguageKeys.Commands.Reload.DESCRIPTION)
    @Permission("newnanmain.reload")
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

    // ===== GUI命令 =====

    @Command("newnan gui")
    @CommandDescription(LanguageKeys.Commands.Gui.DESCRIPTION)
    @Permission("newnanmain.gui")
    fun guiCommand(sender: Player) {
        openMainMenuGui(plugin, sender)
        plugin.messager.printf(sender, LanguageKeys.Commands.Gui.OPENED)
    }

    // ===== 前缀管理命令 =====

    @Command("newnan prefix player set <player> <namespace> <key>")
    @CommandDescription(LanguageKeys.Commands.Prefix.SET_DESCRIPTION)
    @Permission("newnanmain.prefix.admin")
    fun prefixSetCommand(
        sender: CommandSender,
        @Argument(value = "player", description = LanguageKeys.Commands.Prefix.PLAYER_ARG) player: OfflinePlayer,
        @Argument(value = "namespace", description = LanguageKeys.Commands.Prefix.NAMESPACE_ARG) namespace: String,
        @Argument(value = "key", description = LanguageKeys.Commands.Prefix.KEY_ARG) key: String
    ) {
        // 检查全局前缀是否存在
        if (plugin.prefixManager.getGlobalPrefix(namespace, key) == null) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Prefix.NOT_FOUND, "$namespace:$key")
            return
        }

        // 设置玩家前缀
        plugin.prefixManager.setPlayerPrefix(player, namespace, key)
        plugin.messager.printf(sender, LanguageKeys.Commands.Prefix.SET_SUCCESS, player.name, "$namespace:$key")
    }

    @Command("newnan prefix player remove <player> <namespace>")
    @CommandDescription(LanguageKeys.Commands.Prefix.REMOVE_DESCRIPTION)
    @Permission("newnanmain.prefix.admin")
    fun prefixRemoveCommand(
        sender: CommandSender,
        @Argument(value = "player", description = LanguageKeys.Commands.Prefix.PLAYER_ARG) player: OfflinePlayer,
        @Argument(value = "namespace", description = LanguageKeys.Commands.Prefix.NAMESPACE_ARG) namespace: String
    ) {
        // 移除玩家前缀
        plugin.prefixManager.removePlayerPrefix(player, namespace)
        plugin.messager.printf(sender, LanguageKeys.Commands.Prefix.REMOVE_SUCCESS, player.name, namespace)
    }

    @Command("newnan prefix player activate <player> <namespace>")
    @CommandDescription(LanguageKeys.Commands.Prefix.ACTIVATE_DESCRIPTION)
    @Permission("newnanmain.prefix.admin")
    fun prefixActivateCommand(
        sender: CommandSender,
        @Argument(value = "player", description = LanguageKeys.Commands.Prefix.PLAYER_ARG) player: OfflinePlayer,
        @Argument(value = "namespace", description = LanguageKeys.Commands.Prefix.NAMESPACE_ARG) namespace: String
    ) {
        // 激活玩家前缀
        plugin.prefixManager.activatePlayerPrefix(player, namespace)
        plugin.messager.printf(sender, LanguageKeys.Commands.Prefix.ACTIVATE_SUCCESS, player.name, namespace)
    }

    @Command("newnan prefix player deactivate <player>")
    @CommandDescription(LanguageKeys.Commands.Prefix.DEACTIVATE_DESCRIPTION)
    @Permission("newnanmain.prefix.admin")
    fun prefixDeactivateCommand(
        sender: CommandSender,
        @Argument(value = "player", description = LanguageKeys.Commands.Prefix.PLAYER_ARG) player: OfflinePlayer
    ) {
        // 激活玩家前缀
        plugin.prefixManager.deactivatePlayerPrefix(player)
    plugin.messager.printf(sender, LanguageKeys.Commands.Prefix.DEACTIVATE_SUCCESS, player.name)
    }
}
