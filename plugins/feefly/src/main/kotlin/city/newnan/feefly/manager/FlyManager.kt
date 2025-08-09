package city.newnan.feefly.manager

import city.newnan.core.base.BaseModule
import city.newnan.config.extensions.configManager
import city.newnan.core.scheduler.runAsyncRepeating
import city.newnan.core.scheduler.runSync
import city.newnan.core.scheduler.runSyncRepeating
import city.newnan.core.scheduler.ITaskHandler
import city.newnan.core.event.subscribeEvent
import city.newnan.feefly.FeeFlyPlugin
import city.newnan.feefly.config.FlyingPlayer
import city.newnan.feefly.config.PlayerCache
import city.newnan.feefly.event.FlyStartEvent
import city.newnan.feefly.i18n.LanguageKeys
import city.newnan.feefly.event.FlyEndEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title

import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.*
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

/**
 * 飞行管理器
 *
 * 负责管理玩家的付费飞行功能，包括：
 * - 飞行状态管理
 * - 费用计算和扣除
 * - 事件监听和处理
 * - 状态持久化
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class FlyManager(
    moduleName: String,
    val plugin: FeeFlyPlugin
) : BaseModule(moduleName, plugin) {

    /**
     * 当前飞行的玩家
     * 使用ConcurrentHashMap确保线程安全
     */
    private val flyingPlayers = ConcurrentHashMap<Player, FlyingPlayer>()

    /**
     * 玩家命令使用时间记录
     * 用于实现命令速率限制
     */
    private val playerCommandTimes = ConcurrentHashMap<String, Long>()

    /**
     * 飞行配置缓存
     */
    private var flySpeed = 0.05f
    private var costPerCount = 0.3
    private var tickPerCount = 20L
    private var costPerSecond = 0.0
    private var lowBalanceWarningSeconds = 60
    private var commandCooldownSeconds = 3
    private var stateValidationIntervalSeconds = 30
    private var targetAccount: OfflinePlayer? = null

    init { init() }

    override fun onInit() {
        // 注册事件监听器
        registerEventListeners()

        // 启动费用扣除任务
        startChargingTask()

        // 启动状态验证任务
        startStateValidationTask()

        // 恢复之前的飞行状态
        restoreFlyingPlayers()
    }

    override fun onReload() {
        val config = plugin.getPluginConfig()
        val flyingConfig = config.flying

        flySpeed = flyingConfig.flySpeed
        costPerCount = flyingConfig.costPerCount
        tickPerCount = flyingConfig.tickPerCount
        lowBalanceWarningSeconds = flyingConfig.lowBalanceWarningSeconds
        commandCooldownSeconds = flyingConfig.commandCooldownSeconds
        stateValidationIntervalSeconds = flyingConfig.stateValidationIntervalSeconds
        costPerSecond = (20.0 / tickPerCount) * costPerCount

        // 设置目标账户
        targetAccount = config.targetAccount?.let { name ->
            if (name.isBlank()) return@let null
            try {
                val uuid = UUID.fromString(name)
                val account = Bukkit.getOfflinePlayer(uuid)
                logger.info(LanguageKeys.Business.Economy.TARGET_ACCOUNT_SET, "[UUID: $name]")
                account
            } catch (e: IllegalArgumentException) {
                val account = Bukkit.getOfflinePlayer(name)
                if (!account.hasPlayedBefore()) throw Exception("Player $name not found")
                logger.info(LanguageKeys.Business.Economy.TARGET_ACCOUNT_SET, name)
                account
            }
        }

        logger.info(LanguageKeys.Commands.Reload.SUCCESS)
    }

    /**
     * 注册事件监听器
     * 使用Core模块的events DSL最佳实践
     */
    private fun registerEventListeners() {
        // 玩家死亡事件
        subscribeEvent<PlayerDeathEvent> {
            priority(EventPriority.MONITOR)
            handler { event ->
                cancelFly(event.entity, false, FlyEndEvent.EndReason.PLAYER_DEATH)
            }
        }

        // 玩家游戏模式改变事件
        subscribeEvent<PlayerGameModeChangeEvent> {
            priority(EventPriority.MONITOR)
            filter { !it.isCancelled }
            filter { event -> event.newGameMode == GameMode.CREATIVE || event.newGameMode == GameMode.SPECTATOR }
            handler { event ->
                cancelFly(event.player, false, FlyEndEvent.EndReason.GAMEMODE_CHANGE)
            }
        }

        // 玩家世界改变事件
        subscribeEvent<PlayerChangedWorldEvent> {
            priority(EventPriority.MONITOR)
            handler { event ->
                cancelFly(event.player, false, FlyEndEvent.EndReason.WORLD_CHANGE)
            }
        }

        // 玩家退出事件
        subscribeEvent<PlayerQuitEvent> {
            priority(EventPriority.MONITOR)
            handler { event ->
                cancelFly(event.player, false, FlyEndEvent.EndReason.PLAYER_QUIT)
                // 清理命令冷却记录
                playerCommandTimes.remove(event.player.name)
            }
        }

        // 玩家加入事件
        subscribeEvent<PlayerJoinEvent> {
            priority(EventPriority.MONITOR)
            // 只清理FeeFly自己管理的飞行状态，不影响其他插件给予的飞行权限
            filter { event -> flyingPlayers.containsKey(event.player) }
            // 如果玩家在飞行列表中但实际没有飞行权限，说明状态异常，需要清理
            filter { event -> !event.player.allowFlight && event.player.gameMode != GameMode.CREATIVE && event.player.gameMode != GameMode.SPECTATOR }
            handler { event ->
                // 玩家在FeeFly飞行列表中但没有飞行权限，清理异常状态
                logger.debug("Cleaning up invalid flying state for player ${event.player.name}")
                cancelFly(event.player, false)
            }
        }
    }

    /**
     * 切换玩家飞行状态
     */
    fun toggleFly(player: Player) {
        // 检查命令冷却
        if (!checkCommandCooldown(player)) {
            return
        }

        plugin.runSync {
            if (!cancelFly(player, true)) {
                // 玩家不在飞行，尝试开启飞行
                startFly(player)
            }
        }
    }

    /**
     * 检查命令冷却时间
     * @param player 玩家
     * @return 如果通过冷却检查返回true，否则返回false
     */
    private fun checkCommandCooldown(player: Player): Boolean {
        // 管理员绕过冷却限制
        if (player.hasPermission("feefly.bypass.cooldown")) {
            return true
        }

        val playerName = player.name
        val currentTime = System.currentTimeMillis()
        val lastCommandTime = playerCommandTimes[playerName] ?: 0L
        val cooldownMs = commandCooldownSeconds * 1000L

        if (currentTime - lastCommandTime < cooldownMs) {
            val remainingSeconds = ((cooldownMs - (currentTime - lastCommandTime)) / 1000.0).toInt() + 1
            messager.printf(player, LanguageKeys.Business.Error.COMMAND_COOLDOWN, remainingSeconds)
            return false
        }

        // 更新最后使用时间
        playerCommandTimes[playerName] = currentTime
        return true
    }

    /**
     * 开始飞行
     */
    private fun startFly(player: Player) {
        // 检查游戏模式
        if (player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR) {
            messager.printf(player, LanguageKeys.Business.Error.CREATIVE_MODE)
            return
        }

        // 检查是否已经在付费飞行中
        if (flyingPlayers.containsKey(player)) {
            messager.printf(player, LanguageKeys.Business.Error.ALREADY_IN_FEE_FLYING)
            return
        }

        // 检查是否已经能飞行
        if (player.allowFlight) {
            // 如果已经能飞行，直接切换飞行状态，不进入付费系统
            player.isFlying = !player.isFlying
            messager.printf(player, if (player.isFlying) LanguageKeys.Business.Fly.NORMAL_STARTED else LanguageKeys.Business.Fly.NORMAL_ENDED)
            return
        }

        // 检查是否正在飞行
        if (player.isFlying) {
            messager.printf(player, LanguageKeys.Business.Error.ALREADY_FLYING)
            return
        }

        // 检查余额或免费权限
        if (plugin.economy.getBalance(player) <= 0.0 && !player.hasPermission("feefly.free")) {
            messager.printf(player, LanguageKeys.Business.Error.INSUFFICIENT_BALANCE)
            return
        }

        // 记录玩家原始状态
        val flyingPlayer = FlyingPlayer(
            flyStartTimestamp = System.currentTimeMillis(),
            previousFlyingSpeed = player.flySpeed,
            previousAllowFlight = player.allowFlight,
            previousFlying = player.isFlying
        )

        flyingPlayers[player] = flyingPlayer

        // 取消疾跑状态
        player.isSprinting = false

        // 设置飞行
        player.flySpeed = flySpeed
        player.allowFlight = true

        // 计算预估飞行时长
        val isFree = player.hasPermission("feefly.free")
        val estimatedDuration = if (isFree) {
            -1 // 免费飞行无时间限制
        } else {
            val balance = plugin.economy.getBalance(player)
            (balance / costPerSecond).roundToInt()
        }

        // 触发飞行开始事件
        Bukkit.getPluginManager().callEvent(FlyStartEvent(player, isFree, estimatedDuration))

        // 发送消息和音效
        messager.printf(player, LanguageKeys.Business.Fly.STARTED)
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1.0f, 0.0f)

        // 保存状态
        saveFlyingPlayers()
    }

    /**
     * 取消飞行
     * @param player 玩家
     * @param playSound 是否播放音效
     * @param reason 结束原因
     * @return 如果玩家之前在飞行返回true，否则返回false
     */
    fun cancelFly(player: Player, playSound: Boolean, reason: FlyEndEvent.EndReason = FlyEndEvent.EndReason.PLAYER_TOGGLE): Boolean {
        val flyingPlayer = flyingPlayers.remove(player) ?: return false

        // 计算飞行时长和总费用
        val duration = flyingPlayer.getFlyDuration()
        val totalCost = if (player.hasPermission("feefly.free")) {
            0.0
        } else {
            val durationSeconds = duration / 1000.0
            durationSeconds * costPerSecond
        }

        // 恢复玩家状态
        player.allowFlight = flyingPlayer.previousAllowFlight
        player.isFlying = flyingPlayer.previousFlying
        player.flySpeed = flyingPlayer.previousFlyingSpeed

        // 触发飞行结束事件
        Bukkit.getPluginManager().callEvent(FlyEndEvent(player, reason, duration, totalCost))

        if (playSound) {
            player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1.0f, 0.0f)
        }

        // 发送消息
        messager.printf(player, LanguageKeys.Business.Fly.ENDED)
        player.sendActionBar(messager.sprintf(LanguageKeys.Business.Fly.ENDED))

        // 保存状态
        saveFlyingPlayers()

        return true
    }

    /**
     * 取消飞行（兼容旧方法）
     */
    fun cancelFly(player: Player, playSound: Boolean): Boolean {
        return cancelFly(player, playSound, FlyEndEvent.EndReason.PLAYER_TOGGLE)
    }

    /**
     * 玩家更新操作数据类
     */
    private data class PlayerUpdateAction(
        val player: Player,
        val actionType: ActionType,
        val actionBarMessage: Component? = null,
        val titleMessage: Component? = null,
        val subtitleMessage: Component? = null,
        val shouldRemove: Boolean = false
    ) {
        enum class ActionType {
            FREE_FLIGHT,
            NORMAL_FLIGHT,
            LOW_BALANCE_WARNING,
            INSUFFICIENT_BALANCE
        }
    }

    /**
     * 启动费用扣除任务
     */
    private fun startChargingTask() {
        runAsyncRepeating(0L, tickPerCount, function = { _: ITaskHandler<Unit> ->
            if (flyingPlayers.isNotEmpty()) {
                val playerUpdateActions = mutableListOf<PlayerUpdateAction>()

                flyingPlayers.forEach { (player, _) ->
                    if (player.hasPermission("feefly.free")) {
                        // 免费飞行
                        val actionBarMessage = messager.sprintf(LanguageKeys.Business.Fly.FREE_FLIGHT, player.name)
                        playerUpdateActions.add(
                            PlayerUpdateAction(
                                player = player,
                                actionType = PlayerUpdateAction.ActionType.FREE_FLIGHT,
                                actionBarMessage = actionBarMessage
                            )
                        )
                        return@forEach
                    }

                    val balance = plugin.economy.getBalance(player)
                    if (balance > 0.0) {
                        val remainSeconds = (balance / costPerSecond).roundToInt()
                        val cost = balance.coerceAtMost(costPerCount)

                        // 扣除费用
                        plugin.economy.withdrawPlayer(player, cost)
                        targetAccount?.let { plugin.economy.depositPlayer(it, cost) }

                        // 准备状态显示消息
                        val actionBarMessage = messager.sprintf(
                            LanguageKeys.Business.Fly.STATUS,
                            formatTime(remainSeconds),
                            String.format("%.2f", balance - cost)
                        )

                        if (remainSeconds <= lowBalanceWarningSeconds) {
                            // 低余额警告
                            val titleMessage = messager.sprintf(LanguageKeys.Business.Fly.LOW_BALANCE_TITLE)
                            val subtitleMessage = messager.sprintf(LanguageKeys.Business.Fly.LOW_BALANCE_SUBTITLE)

                            playerUpdateActions.add(
                                PlayerUpdateAction(
                                    player = player,
                                    actionType = PlayerUpdateAction.ActionType.LOW_BALANCE_WARNING,
                                    actionBarMessage = actionBarMessage,
                                    titleMessage = titleMessage,
                                    subtitleMessage = subtitleMessage
                                )
                            )
                        } else {
                            // 正常飞行状态
                            playerUpdateActions.add(
                                PlayerUpdateAction(
                                    player = player,
                                    actionType = PlayerUpdateAction.ActionType.NORMAL_FLIGHT,
                                    actionBarMessage = actionBarMessage
                                )
                            )
                        }
                    } else {
                        // 余额不足，标记移除
                        playerUpdateActions.add(
                            PlayerUpdateAction(
                                player = player,
                                actionType = PlayerUpdateAction.ActionType.INSUFFICIENT_BALANCE,
                                shouldRemove = true
                            )
                        )
                    }
                }

                // 在主线程中统一处理所有玩家更新操作
                if (playerUpdateActions.isNotEmpty()) {
                    runSync { _: ITaskHandler<Unit> ->
                        processBatchPlayerUpdates(playerUpdateActions)
                    }
                }
            }
        })
    }

    /**
     * 批量处理玩家更新操作
     * @param actions 待处理的玩家更新操作列表
     */
    private fun processBatchPlayerUpdates(actions: List<PlayerUpdateAction>) {
        val playersToRemove = mutableListOf<Player>()

        actions.forEach { action ->
            try {
                // 确保飞行状态（对所有非移除操作的玩家）
                if (!action.shouldRemove) {
                    action.player.flySpeed = flySpeed
                    action.player.allowFlight = true
                }

                when (action.actionType) {
                    PlayerUpdateAction.ActionType.FREE_FLIGHT -> {
                        action.actionBarMessage?.let { message ->
                            action.player.sendActionBar(message)
                        }
                    }

                    PlayerUpdateAction.ActionType.NORMAL_FLIGHT -> {
                        action.actionBarMessage?.let { message ->
                            action.player.sendActionBar(message)
                        }
                    }

                    PlayerUpdateAction.ActionType.LOW_BALANCE_WARNING -> {
                        action.actionBarMessage?.let { message ->
                            action.player.sendActionBar(message)
                        }

                        if (action.titleMessage != null && action.subtitleMessage != null) {
                            action.player.showTitle(Title.title(
                                action.titleMessage,
                                action.subtitleMessage,
                                Title.Times.times(
                                    Duration.ofMillis(1000),
                                    Duration.ofMillis(7000),
                                    Duration.ofMillis(2000)
                                )
                            ))
                        }
                    }

                    PlayerUpdateAction.ActionType.INSUFFICIENT_BALANCE -> {
                        playersToRemove.add(action.player)
                    }
                }
            } catch (e: Exception) {
                logger.error("处理玩家 ${action.player.name} 的更新操作失败", e)
            }
        }

        // 移除余额不足的玩家
        playersToRemove.forEach { player ->
            cancelFly(player, true, FlyEndEvent.EndReason.INSUFFICIENT_BALANCE)
        }
    }

    /**
     * 启动状态验证任务
     * 定期验证飞行玩家状态的完整性
     */
    private fun startStateValidationTask() {
        runSyncRepeating(
            stateValidationIntervalSeconds * 20L,
            stateValidationIntervalSeconds * 20L,
            function = { _: ITaskHandler<Unit> ->
                validateFlyingPlayersState()
            }
        )
    }

    /**
     * 验证飞行玩家状态完整性
     */
    private fun validateFlyingPlayersState() {
        val invalidPlayers = mutableListOf<Player>()

        flyingPlayers.forEach { (player, flyingPlayer) ->
            try {
                // 检查玩家是否还在线
                if (!player.isOnline) {
                    logger.debug("Player ${player.name} is offline but still in flying list")
                    invalidPlayers.add(player)
                    return@forEach
                }

                // 检查游戏模式
                if (player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR) {
                    logger.debug("Player ${player.name} is in creative/spectator mode but still in flying list")
                    invalidPlayers.add(player)
                    return@forEach
                }

                // 检查是否还有基本权限
                if (!player.hasPermission("feefly.self") && !player.hasPermission("feefly.free")) {
                    logger.debug("Player ${player.name} lost flying permissions")
                    invalidPlayers.add(player)
                    return@forEach
                }

                // 检查飞行状态一致性
                if (!player.allowFlight) {
                    logger.debug("Player ${player.name} lost allowFlight but still in flying list")
                    invalidPlayers.add(player)
                    return@forEach
                }

                // 检查飞行时间是否异常（超过24小时）
                val flyDurationHours = flyingPlayer.getFlyDuration() / (1000 * 60 * 60)
                if (flyDurationHours > 24) {
                    logger.warn("Player ${player.name} has been flying for $flyDurationHours hours, this might be abnormal")
                }

                // 验证玩家数据完整性
                if (flyingPlayer.flyStartTimestamp <= 0) {
                    logger.debug("Player ${player.name} has invalid fly start timestamp")
                    invalidPlayers.add(player)
                    return@forEach
                }

            } catch (e: Exception) {
                logger.error("Error validating player ${player.name} state", e)
                invalidPlayers.add(player)
            }
        }

        // 清理无效的玩家
        invalidPlayers.forEach { player ->
            logger.info("Removing invalid flying player: ${player.name}")
            cancelFly(player, false, FlyEndEvent.EndReason.STATE_VALIDATION_FAILED)
        }

        // 清理过期的命令冷却记录（超过1小时的记录）
        val currentTime = System.currentTimeMillis()
        val oneHourMs = 60 * 60 * 1000L
        playerCommandTimes.entries.removeIf { (_, time) ->
            currentTime - time > oneHourMs
        }

        if (invalidPlayers.isNotEmpty()) {
            logger.info("State validation completed, cleaned up ${invalidPlayers.size} invalid flying players")
        }
    }

    /**
     * 格式化时间显示
     */
    private fun formatTime(seconds: Int): String {
        return when {
            seconds < 60 -> messager.sprintfPlain(LanguageKeys.Business.Time.SECONDS, seconds)
            seconds < 3600 -> {
                val m = seconds / 60
                val s = seconds % 60
                messager.sprintfPlain(LanguageKeys.Business.Time.MINUTES_SECONDS, m, s)
            }
            seconds < 86400 -> {
                val h = seconds / 3600
                val m = seconds % 3600 / 60
                val s = seconds % 3600 % 60
                messager.sprintfPlain(LanguageKeys.Business.Time.HOURS_MINUTES_SECONDS, h, m, s)
            }
            else -> {
                val d = seconds / 86400
                val h = seconds % 86400 / 3600
                val m = seconds % 3600 / 60
                val s = seconds % 3600 % 60
                messager.sprintfPlain(LanguageKeys.Business.Time.DAYS_HOURS_MINUTES_SECONDS, d, h, m, s)
            }
        }
    }

    /**
     * 保存飞行玩家状态
     */
    private fun saveFlyingPlayers() {
        val playerCache = PlayerCache(
            players = flyingPlayers.mapKeys { it.key.uniqueId }
        )
        plugin.configManager.save(playerCache, "player-cache.yml")
    }

    /**
     * 恢复飞行玩家状态
     */
    private fun restoreFlyingPlayers() {
        try {
            val playerCache = plugin.configManager.parse<PlayerCache>("player-cache.yml")
            playerCache.players.forEach { (uuid: java.util.UUID, flyingPlayer: FlyingPlayer) ->
                Bukkit.getPlayer(uuid)?.let { player ->
                    flyingPlayers[player] = flyingPlayer
                }
            }
            logger.info(LanguageKeys.Business.Fly.STATE_RESTORED, flyingPlayers.size)
        } catch (e: Exception) {
            logger.warn(LanguageKeys.Business.Fly.STATE_RESTORE_FAILED, e)
        }
    }

    /**
     * 获取当前飞行玩家列表
     */
    fun getFlyingPlayers(): Map<Player, FlyingPlayer> = flyingPlayers.toMap()

    override fun onClose() {
        // 保存飞行状态
        saveFlyingPlayers()
        // 取消所有玩家的飞行
        flyingPlayers.keys.forEach { player ->
            cancelFly(player, false, FlyEndEvent.EndReason.PLUGIN_DISABLE)
        }
        logger.info("FlyManager closed")
    }
}
