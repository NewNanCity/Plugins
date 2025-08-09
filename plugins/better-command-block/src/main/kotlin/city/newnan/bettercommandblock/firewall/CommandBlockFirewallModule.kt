package city.newnan.bettercommandblock.firewall

import city.newnan.bettercommandblock.BetterCommandBlockPlugin
import city.newnan.bettercommandblock.firewall.config.FirewallConfig
import city.newnan.bettercommandblock.firewall.scanner.CommandScanner
import city.newnan.bettercommandblock.firewall.trie.CommandTrie
import city.newnan.bettercommandblock.firewall.validators.*
import city.newnan.bettercommandblock.i18n.LanguageKeys
import city.newnan.core.base.BaseModule
import city.newnan.core.event.subscribeEvent
import city.newnan.core.scheduler.runAsyncRepeating
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.command.BlockCommandSender
import org.bukkit.event.EventPriority
import org.bukkit.event.server.ServerCommandEvent
import java.io.File
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 命令方块防火墙模块
 *
 * 基于高性能前缀树算法的命令方块安全模块，提供比传统方法更强大的安全防护。
 *
 * 核心特性：
 * - 流式处理流水线：预处理 → 分词 → Trie匹配 → 特殊规则验证
 * - 惰性分词（Lazy Tokenization）
 * - 动态预处理（大小写、空白符处理）
 * - 特殊规则节点（Validator Nodes）用于复杂命令如execute的深度验证
 * - 递归验证子命令
 * - 完整的统计和监控
 *
 * @param plugin 插件实例
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class CommandBlockFirewallModule(
    val plugin: BetterCommandBlockPlugin
) : BaseModule("CommandBlockFirewall", plugin) {

    companion object {
        /**
         * 统计信息更新间隔（毫秒）
         */
        private const val STATS_UPDATE_INTERVAL = 30000L // 30秒

        /**
         * 最大日志文件大小（字节）
         */
        private const val MAX_LOG_FILE_SIZE = 10 * 1024 * 1024 // 10MB
    }

    // 核心组件
    private lateinit var commandTrie: CommandTrie
    private lateinit var executeValidator: ExecuteValidator
    private lateinit var itemValidator: ItemValidator
    private lateinit var coordinateValidator: CoordinateValidator
    private lateinit var selectorValidator: SelectorValidator

    // 配置缓存
    private lateinit var firewallConfig: FirewallConfig

    // 日志文件
    private lateinit var logFile: File

    // 统计信息
    private val totalCommands = AtomicLong(0)
    private val blockedCommands = AtomicLong(0)
    private val allowedCommands = AtomicLong(0)
    private val validationErrors = AtomicLong(0)
    private val lastStatsUpdate = AtomicLong(System.currentTimeMillis())

    // 性能监控
    private val validationTimes = ConcurrentHashMap<String, Long>()
    private val commandFrequency = ConcurrentHashMap<String, AtomicLong>()

    init { init() }

    override fun onInit() {
        try {
            // 初始化日志文件
            initializeLogFile()

            // 注册事件监听器
            subscribeEvent<ServerCommandEvent> {
                priority(EventPriority.HIGHEST)
                filter { !it.isCancelled }
                filter { it.sender is BlockCommandSender }
                handler { event ->
                    try {
                        handleCommandBlockCommand(event)
                    } catch (e: Exception) {
                        logger.error("Error handling command block command", e)
                        validationErrors.incrementAndGet()
                    }
                }
            }

            // 启动统计任务
            startStatisticsTask()

            logger.info("CommandBlockFirewall initialized successfully")
            logger.info("Loaded ${commandTrie.getAllCommands().size} safe commands")
            logger.info("Firewall mode: ${if (firewallConfig.enabled) "ENABLED" else "DISABLED"}")

        } catch (e: Exception) {
            logger.error("Failed to initialize CommandBlockFirewall", e)
            throw e
        }
    }

    override fun onReload() {
        try {
            logger.info("Reloading CommandBlockFirewall...")

            // 重新加载配置
            firewallConfig = plugin.getPluginConfig().firewall

            // 重新初始化验证器
            initializeValidators()

            // 重新构建命令树
            buildCommandTrie()

            logger.info("CommandBlockFirewall reloaded successfully")
            logger.info("Loaded ${commandTrie.getAllCommands().size} safe commands")

        } catch (e: Exception) {
            logger.error("Failed to reload CommandBlockFirewall", e)
        }
    }

    /**
     * 初始化日志文件
     */
    private fun initializeLogFile() {
        logFile = File(plugin.dataFolder, "firewall-blocked-commands.log")
        if (!logFile.exists()) {
            logFile.parentFile?.mkdirs()
            logFile.createNewFile()
        }

        // 检查日志文件大小，如果过大则轮转
        if (logFile.length() > MAX_LOG_FILE_SIZE) {
            val backupFile = File(plugin.dataFolder, "firewall-blocked-commands.log.old")
            if (backupFile.exists()) {
                backupFile.delete()
            }
            logFile.renameTo(backupFile)
            logFile.createNewFile()
        }
    }

    /**
     * 初始化验证器
     */
    private fun initializeValidators() {
        // 创建验证器实例
        itemValidator = ItemValidator(
            safeItems = firewallConfig.safeItems,
            maxQuantity = firewallConfig.maxItemQuantity,
            allowCustomNamespaces = firewallConfig.allowCustomNamespaces
        )

        coordinateValidator = CoordinateValidator(
            maxRange = firewallConfig.maxCoordinateRange,
            allowRelative = firewallConfig.allowRelativeCoordinates,
            allowLocal = firewallConfig.allowLocalCoordinates
        )

        selectorValidator = SelectorValidator(
            allowedSelectors = firewallConfig.allowedSelectors,
            maxRange = firewallConfig.maxSelectorRange,
            allowPlayerNames = firewallConfig.allowPlayerNames,
            maxTargetCount = firewallConfig.maxTargetCount
        )

        commandTrie = CommandTrie()

        executeValidator = ExecuteValidator(
            rootTrie = commandTrie,
            maxDepth = firewallConfig.maxExecuteDepth,
            selectorValidator = selectorValidator,
            coordinateValidator = coordinateValidator
        )
    }

    /**
     * 构建命令树
     */
    private fun buildCommandTrie() {
        // 添加白名单命令
        for (command in firewallConfig.whitelistCommands) {
            when (command) {
                "give", "minecraft:give" -> {
                    commandTrie.addCommand(command, itemValidator)
                }
                "summon", "minecraft:summon" -> {
                    commandTrie.addCommand(command, itemValidator)
                }
                "tp", "teleport", "minecraft:tp", "minecraft:teleport" -> {
                    commandTrie.addCommand(command, coordinateValidator)
                }
                "setblock", "minecraft:setblock" -> {
                    commandTrie.addCommand(command, coordinateValidator)
                }
                "fill", "minecraft:fill" -> {
                    commandTrie.addCommand(command, coordinateValidator)
                }
                "execute", "minecraft:execute" -> {
                    commandTrie.addCommand(command, executeValidator)
                }
                else -> {
                    commandTrie.addCommand(command)
                }
            }
        }

        logger.debug("Command trie built with ${commandTrie.size()} nodes")
    }

    /**
     * 处理命令方块执行的命令
     */
    private fun handleCommandBlockCommand(event: ServerCommandEvent) {
        if (!firewallConfig.enabled) {
            return // 防火墙已禁用
        }

        val startTime = System.nanoTime()
        val sender = event.sender as BlockCommandSender
        val block = sender.block
        val command = event.command

        totalCommands.incrementAndGet()

        // 更新命令频率统计
        val baseCommand = CommandScanner(command).nextToken() ?: return
        commandFrequency.computeIfAbsent(baseCommand) { AtomicLong(0) }.incrementAndGet()

        try {
            // 使用命令树验证命令
            val isCommandSafe = commandTrie.isCommandSafe(command)

            if (isCommandSafe) {
                allowedCommands.incrementAndGet()
                logger.debug("Command allowed: $command")
            } else {
                blockedCommands.incrementAndGet()
                blockCommand(block, command, "Firewall blocked")
                event.isCancelled = true

                logger.info("Blocked command: $command at ${block.location}")
            }

        } finally {
            // 记录验证时间
            val validationTime = System.nanoTime() - startTime
            validationTimes[baseCommand] = validationTime
        }
    }

    /**
     * 阻止命令执行并记录日志
     */
    private fun blockCommand(block: Block, command: String, reason: String) {
        val location = block.location
        val world = location.world?.name ?: "unknown"
        val timestamp = LocalDateTime.now()

        // 记录到日志文件
        val logEntry = "[$timestamp] BLOCKED: $reason | World: $world | " +
                      "Location: (${location.blockX}, ${location.blockY}, ${location.blockZ}) | " +
                      "Command: $command\n"

        try {
            logFile.appendText(logEntry)
        } catch (e: Exception) {
            logger.error("Failed to write to firewall log file", e)
        }

        // 如果配置允许，销毁命令方块
        if (firewallConfig.destroyBlockedCommandBlocks) {
            block.type = Material.AIR
            logger.info("Destroyed command block at ($world, ${location.blockX}, ${location.blockY}, ${location.blockZ})")
        }
    }

    /**
     * 启动统计任务
     */
    private fun startStatisticsTask() {
        runAsyncRepeating(0L, STATS_UPDATE_INTERVAL / 50L) {
            updateStatistics()
        }
    }

    /**
     * 更新统计信息
     */
    private fun updateStatistics() {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastUpdate = currentTime - lastStatsUpdate.get()

        if (timeSinceLastUpdate >= STATS_UPDATE_INTERVAL) {
            val stats = getStatistics()
            logger.debug("Firewall Statistics: $stats")
            lastStatsUpdate.set(currentTime)
        }
    }

    /**
     * 获取统计信息
     */
    fun getStatistics(): Map<String, Any> {
        val trieStats = commandTrie.getStatistics()

        return mapOf(
            "totalCommands" to totalCommands.get(),
            "blockedCommands" to blockedCommands.get(),
            "allowedCommands" to allowedCommands.get(),
            "validationErrors" to validationErrors.get(),
            "blockRate" to if (totalCommands.get() > 0) {
                blockedCommands.get().toDouble() / totalCommands.get()
            } else 0.0,
            "trieSize" to (trieStats["treeSize"] ?: 0),
            "trieValidations" to (trieStats["totalValidations"] ?: 0),
            "trieMatches" to (trieStats["totalMatches"] ?: 0),
            "trieRejections" to (trieStats["totalRejections"] ?: 0),
            "topCommands" to commandFrequency.entries
                .sortedByDescending { it.value.get() }
                .take(10)
                .associate { it.key to it.value.get() },
            "averageValidationTime" to if (validationTimes.isNotEmpty()) validationTimes.values.average() else 0.0
        )
    }

    /**
     * 重置统计信息
     */
    fun resetStatistics() {
        totalCommands.set(0)
        blockedCommands.set(0)
        allowedCommands.set(0)
        validationErrors.set(0)
        commandFrequency.clear()
        validationTimes.clear()
        commandTrie.resetStatistics()
    }

    /**
     * 获取防火墙配置
     */
    fun getFirewallConfig(): FirewallConfig = firewallConfig

    /**
     * 获取命令树
     */
    fun getCommandTrie(): CommandTrie = commandTrie

    /**
     * 检查命令是否安全
     */
    fun isCommandSafe(command: String): Boolean {
        return if (firewallConfig.enabled) {
            commandTrie.isCommandSafe(command)
        } else {
            true // 防火墙禁用时所有命令都安全
        }
    }

    /**
     * 获取验证器实例
     */
    fun getValidators(): Map<String, Validator> = mapOf(
        "execute" to executeValidator,
        "item" to itemValidator,
        "coordinate" to coordinateValidator,
        "selector" to selectorValidator
    )
}
