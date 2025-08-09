package city.newnan.core.command

import city.newnan.core.base.BaseModule
import city.newnan.core.base.BasePlugin
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPICommand
import java.util.concurrent.ConcurrentHashMap

/**
 * 基础命令注册器
 *
 * 基于BaseModule架构的命令管理系统，提供完整的命令生命周期管理：
 * - 自动注册：在onInit()时调用registerCommands()
 * - 自动注销：在onClose()时注销所有跟踪的命令
 * - 命令跟踪：线程安全的命令跟踪和管理
 * - 错误处理：完善的异常处理和日志记录
 *
 * ## 使用示例
 * ```kotlin
 * class MyCommandRegistry(
 *     private val plugin: MyPlugin
 * ) : BaseCommandRegistry("MyPluginCommandRegistry", plugin) {
 *
 *     override fun registerCommands() {
 *         registerMainCommand()
 *         registerAliasCommands()
 *     }
 *
 *     private fun registerMainCommand() {
 *         val mainCommand = CommandAPICommand("myplugin")
 *             .withPermission("myplugin.use")
 *             .withSubcommands(/* subcommands */)
 *             .executes(CommandExecutor { sender, _ ->
 *                 // Command logic
 *             })
 *
 *         // 自动跟踪注册
 *         registerAndTrack(mainCommand, "myplugin")
 *     }
 * }
 * ```
 *
 * @param moduleName 模块名称，建议使用 "{PluginName}CommandRegistry" 格式
 * @param plugin 插件实例
 *
 * @author NewNanCity
 * @since 2.0.0
 */
abstract class BaseCommandRegistry(
    moduleName: String,
    plugin: BasePlugin
) : BaseModule(moduleName, plugin) {

    // ================== 命令跟踪 ==================

    /**
     * 已注册的命令跟踪映射
     * Key: 命令名称, Value: CommandAPICommand实例
     */
    private val registeredCommands = ConcurrentHashMap<String, CommandAPICommand>()

    /**
     * Bukkit命令跟踪集合
     * 用于标记需要特殊注销方式的命令
     */
    private val bukkitCommands = ConcurrentHashMap.newKeySet<String>()

    // ================== 生命周期管理 ==================

    override fun onInit() {
        logger.info("正在注册命令...")
        try {
            registerCommands()
            logger.info("命令注册完成，共注册 ${registeredCommands.size} 个命令")
        } catch (e: Exception) {
            logger.error("命令注册失败", e)
            throw e
        }
    }

    override fun onReload() {
        logger.info("正在重载命令配置...")
        try {
            onCommandReload()
            logger.info("命令配置重载完成")
        } catch (e: Exception) {
            logger.error("命令配置重载失败", e)
        }
    }

    override fun onClose() {
        logger.info("正在注销命令...")
        try {
            unregisterAllCommands()
            logger.info("命令注销完成")
        } catch (e: Exception) {
            logger.error("命令注销失败", e)
        }
    }

    // ================== 抽象方法 ==================

    /**
     * 注册所有命令
     *
     * 子类必须实现此方法来注册插件的所有命令。
     * 在此方法中应该调用 [registerAndTrack] 来注册并跟踪命令。
     */
    abstract fun registerCommands()

    /**
     * 命令重载时的回调方法
     *
     * 子类可以重写此方法来处理命令重载时的特殊逻辑，
     * 例如重新加载命令权限配置、更新命令帮助信息等。
     *
     * 默认实现为空，子类可选择性重写。
     */
    protected open fun onCommandReload() {
        // 默认空实现，子类可重写
    }

    // ================== 命令注册方法 ==================

    /**
     * 注册并跟踪命令
     *
     * 此方法会注册命令并将其添加到跟踪列表中，
     * 当模块关闭时会自动注销所有跟踪的命令。
     *
     * @param command 要注册的CommandAPICommand
     * @param commandName 命令名称，用于跟踪和日志
     * @param isBukkitCommand 是否为Bukkit命令，影响注销方式
     */
    protected fun registerAndTrack(
        command: CommandAPICommand,
        commandName: String,
        isBukkitCommand: Boolean = false
    ) {
        try {
            // 注册命令
            command.register()

            // 添加到跟踪列表
            registeredCommands[commandName] = command

            // 标记Bukkit命令
            if (isBukkitCommand) {
                bukkitCommands.add(commandName)
            }

            logger.debug("命令 '$commandName' 注册成功${if (isBukkitCommand) " (Bukkit命令)" else ""}")

        } catch (e: Exception) {
            logger.error("注册命令 '$commandName' 失败", e)
            throw e
        }
    }

    /**
     * 注册别名命令并跟踪
     *
     * 用于注册独立的别名命令（不是主命令的别名）
     *
     * @param command 要注册的CommandAPICommand
     * @param aliasName 别名命令名称
     * @param isBukkitCommand 是否为Bukkit命令
     */
    protected fun registerAliasAndTrack(
        command: CommandAPICommand,
        aliasName: String,
        isBukkitCommand: Boolean = false
    ) {
        registerAndTrack(command, aliasName, isBukkitCommand)
    }

    // ================== 命令注销方法 ==================

    /**
     * 注销所有跟踪的命令
     */
    private fun unregisterAllCommands() {
        if (registeredCommands.isEmpty()) {
            logger.debug("没有需要注销的命令")
            return
        }

        var successCount = 0
        var failureCount = 0

        registeredCommands.forEach { (commandName, _) ->
            try {
                unregisterCommand(commandName)
                successCount++
            } catch (e: Exception) {
                logger.error("注销命令 '$commandName' 失败", e)
                failureCount++
            }
        }

        // 清理跟踪列表
        registeredCommands.clear()
        bukkitCommands.clear()

        logger.info("命令注销完成：成功 $successCount 个，失败 $failureCount 个")
    }

    /**
     * 注销单个命令
     *
     * @param commandName 要注销的命令名称
     */
    private fun unregisterCommand(commandName: String) {
        try {
            if (bukkitCommands.contains(commandName)) {
                // Bukkit命令的特殊注销方式（如果需要）
                logger.debug("注销Bukkit命令: $commandName")
                CommandAPI.unregister(commandName)
            } else {
                // 标准CommandAPI命令注销
                logger.debug("注销CommandAPI命令: $commandName")
                CommandAPI.unregister(commandName)
            }
        } catch (e: Exception) {
            logger.warn("注销命令 '$commandName' 时发生异常", e)
            // 不重新抛出异常，避免影响其他命令的注销
        }
    }

    // ================== 查询方法 ==================

    /**
     * 获取已注册的命令数量
     */
    fun getRegisteredCommandCount(): Int = registeredCommands.size

    /**
     * 获取所有已注册的命令名称
     */
    fun getRegisteredCommandNames(): Set<String> = registeredCommands.keys.toSet()

    /**
     * 检查命令是否已注册
     */
    fun isCommandRegistered(commandName: String): Boolean = registeredCommands.containsKey(commandName)
}
