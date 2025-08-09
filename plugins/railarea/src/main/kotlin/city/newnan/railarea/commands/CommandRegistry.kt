package city.newnan.railarea.commands

import city.newnan.railarea.RailAreaPlugin
import city.newnan.railarea.gui.input.handleAreaInput
import city.newnan.railarea.gui.openRailLinesGui
import city.newnan.railarea.i18n.LanguageKeys
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
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
 * RailArea 插件命令注册器
 *
 * 使用 Cloud 命令框架实现现代化的注解驱动命令系统。
 * 支持以下命令：
 * - /railarea (主命令，打开管理界面)
 * - /railarea reload (重载插件配置)
 * - /railarea help (显示帮助信息)
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class CommandRegistry(val plugin: RailAreaPlugin) {
    
    // ====== 从这里开始是固定模板 ======
    
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
        // 解析注解 - 只注册当前类，因为所有命令都在这里实现
        val commands = commandAnnotationParser.parse(this)
        commands.forEach { commandManager.command(it) }

        // 生成帮助指令
        help = MinecraftHelp.createNative("/railarea", commandManager)
    }

    // ====== 固定模板结束 ======

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
    @Command("railarea|rail help [help-query]")
    @CommandDescription(LanguageKeys.Commands.Help.DESCRIPTION)
    fun helpCommand(
        sender: CommandSender, 
        @Greedy @Default("") @Argument(value = "help-query", description = LanguageKeys.Commands.Help.QUERY) query: String
    ) {
        help.queryCommands(query, sender)
    }

    // 主命令 - 打开管理界面
    @Command("railarea|rail")
    @Command("railarea|rail gui")
    @CommandDescription(LanguageKeys.Commands.Main.DESCRIPTION)
    @Permission("railarea.edit")
    fun mainCommand(sender: CommandSender) {
        // 玩家检查
        if (sender !is Player) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Common.PLAYER_ONLY)
            return
        }

        try {
            openRailLinesGui(plugin, sender, true)
            plugin.messager.printf(sender, LanguageKeys.Commands.Main.GUI_OPENED)
            plugin.logger.info(LanguageKeys.Commands.Main.LOG_SUCCESS, sender.name)
        } catch (e: Exception) {
            plugin.logger.error(LanguageKeys.Commands.Main.LOG_FAILED, e, sender.name)
            plugin.messager.printf(sender, LanguageKeys.Commands.Main.GUI_FAILED)
        }
    }

    @Command("railarea|rail new-area")
    @CommandDescription(LanguageKeys.Commands.NewArea.DESCRIPTION)
    @Permission("railarea.edit")
    fun newAreaCommand(sender: CommandSender) {
        // 玩家检查
        if (sender !is Player) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Common.PLAYER_ONLY)
            return
        }
        handleAreaInput(plugin, sender, null) { area ->
            if (area != null) {
                try {
                    plugin.stationStorage.addArea(
                        world = area.world,
                        range3D = area.range3D,
                        direction = area.direction,
                        stopPoint = area.stopPoint,
                        stationId = area.station.id,
                        lineId = area.line.id,
                        reverse = area.reverse
                    )
                    plugin.messager.printf(sender, LanguageKeys.Commands.NewArea.AREA_ADDED)
                } catch (e: Exception) {
                    plugin.logger.error("Failed to add area", e)
                    plugin.messager.printf(sender, LanguageKeys.Commands.NewArea.AREA_ADD_FAILED, e.message ?: "Unknown error")
                }
            }
        }
    }

    // 重载命令
    @Command("railarea|rail reload")
    @CommandDescription(LanguageKeys.Commands.Reload.DESCRIPTION)
    @Permission("railarea.reload")
    fun reloadCommand(sender: CommandSender) {
        try {
            plugin.reloadPlugin()
            plugin.messager.printf(sender, LanguageKeys.Commands.Reload.SUCCESS)
            plugin.logger.info(LanguageKeys.Commands.Reload.LOG_SUCCESS, sender.name)
        } catch (e: Exception) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Reload.FAILED, e.message ?: "Unknown error")
            plugin.logger.error(LanguageKeys.Commands.Reload.LOG_FAILED, e, sender.name)
        }
    }
}