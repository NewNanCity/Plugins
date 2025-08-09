package city.newnan.tpa.commands

import city.newnan.tpa.TPAPlugin
import city.newnan.tpa.commands.admin.ReloadCommand
import city.newnan.tpa.commands.user.*
import city.newnan.tpa.gui.openOnlinePlayersGui
import city.newnan.tpa.i18n.LanguageKeys
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.bukkit.CloudBukkitCapabilities
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.minecraft.extras.RichDescription
import org.incendo.cloud.paper.LegacyPaperCommandManager

/**
 * TPA命令注册器
 *
 * 基于Cloud框架的现代命令系统，提供完整的命令生命周期管理：
 * - 使用注解驱动的命令定义
 * - 自动命令注册和补全
 * - 本地化描述和帮助系统
 * - 模块化命令组织结构
 *
 * @author AI Assistant  
 * @since 2.0.0
 */
class CommandRegistry(val plugin: TPAPlugin) {

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

    val commandAnnotationParser = AnnotationParser(commandManager, CommandSender::class.java).also {
        it.descriptionMapper { key -> RichDescription.of(plugin.messager.sprintf(key)) }
    }

    init {
        // 创建命令实例列表
//        listOf(
//            // 管理员命令
//            ReloadCommand(plugin),
//
//            // 用户命令
//            TPACommand(plugin),
//            TPAHereCommand(plugin),
//            AcceptCommand(plugin),
//            RejectCommand(plugin),
//            BlockCommand(plugin),
//            UnblockCommand(plugin)
//        ).forEach { commandAnnotationParser.parse(it) }
    }
}