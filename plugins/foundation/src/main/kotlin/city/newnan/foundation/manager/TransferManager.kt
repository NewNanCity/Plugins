package city.newnan.foundation.manager

import city.newnan.core.base.BaseModule
import city.newnan.core.event.subscribeEvent
import city.newnan.core.scheduler.runAsync
import city.newnan.core.scheduler.runAsyncRepeating
import city.newnan.foundation.FoundationPlugin
import city.newnan.foundation.config.TransferRecord
import city.newnan.foundation.config.AllocationLogRecord
import city.newnan.foundation.config.DataStorageMode
import city.newnan.foundation.i18n.LanguageKeys
import city.newnan.foundation.repository.TransferRecordRepository
import city.newnan.foundation.repository.AllocationLogRepository
import city.newnan.foundation.repository.PageResult
import city.newnan.foundation.repository.TransferRankEntry
import city.newnan.foundation.repository.impl.CsvTransferRecordRepository
import city.newnan.foundation.repository.impl.CsvAllocationLogRepository
import city.newnan.foundation.repository.impl.DatabaseRepositories
import net.ess3.api.events.UserBalanceUpdateEvent
import me.yic.xconomy.api.event.PlayerAccountEvent
import org.bukkit.OfflinePlayer
import org.bukkit.block.CommandBlock
import org.bukkit.command.CommandSender
import org.bukkit.event.EventPriority
import org.bukkit.entity.Player
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 转账管理器
 *
 * 负责监控和记录玩家向基金会账户的转账行为，支持：
 * - 自动检测转账事件（异步处理）
 * - 转账记录统计
 * - CSV数据存储（异步保存）
 * - 主动和被动转账分类
 *
 * 转账分类说明：
 * - 主动转账：通过捐款指令进行的转账，使用activeTransfer方法
 * - 被动转账：通过其他插件/机制进行的转账，如死亡扣费等
 * - 拨款：基金会向玩家的转账，不在此处记录，由AllocationLogRepository处理
 *
 * 性能优化：
 * - 事件处理在异步线程中执行，避免阻塞主线程
 * - 数据保存采用异步操作，提高服务器响应性
 * - 智能匹配系统优化内存使用和处理速度
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class TransferManager(
    moduleName: String,
    val plugin: FoundationPlugin,
) : BaseModule(moduleName, plugin) {
    // 存储库
    private var csvTransferImpl: CsvTransferRecordRepository? = null
    private var csvAllocationImpl: CsvAllocationLogRepository? = null
    private var databaseImpl: DatabaseRepositories? = null
    private lateinit var transferRecordRepository: TransferRecordRepository
    private lateinit var allocationLogRepository: AllocationLogRepository

    // 转账检测相关
    private val otherTransfers = ArrayDeque<TransferOther>()
    private val selfTransfers = ArrayDeque<TransferSelf>()
    private val bypassTransfer1 = mutableSetOf<Pair<UUID, BigDecimal>>()
    private val bypassTransfer2 = mutableSetOf<BigDecimal>()

    // 配置缓存变量
    private var enableTransferDetection: Boolean = true
    private var expireMilliseconds: Long = TimeUnit.SECONDS.toMillis(5)

    // 日期格式化器 - 使用ISO 8601标准格式
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    init { init() }

    override fun onInit() {
        // 检查经济插件可用性
        checkEconomyPluginsAvailability()

        // 注册事件监听器
        registerEventListeners()
        logger.info("Event listeners registered")

        // 启动定期清理任务 每30秒清理一次过期数据
        runAsyncRepeating(600L, 600L) { handler ->
            val now = System.currentTimeMillis()
            val expired = now - expireMilliseconds

            otherTransfers.removeIf { it.time < expired }
            selfTransfers.removeIf { it.time < expired }
        }

        logger.info("TransferManager initialized successfully")
    }

    override fun onReload() {
        val config = plugin.getPluginConfig()
        val transferConfig = config.transferDetection

        enableTransferDetection = transferConfig.enabled
        expireMilliseconds = transferConfig.expireMilliseconds

        // 切换存储模式
        when (config.dataStorage.mode) {
            DataStorageMode.FILE -> {
                databaseImpl?.close()
                databaseImpl = null
                if (csvTransferImpl == null) {
                    csvTransferImpl = CsvTransferRecordRepository(this, plugin)
                }
                if (csvAllocationImpl == null) {
                    csvAllocationImpl = CsvAllocationLogRepository(this, plugin)
                }
                transferRecordRepository = csvTransferImpl!!
                allocationLogRepository = csvAllocationImpl!!
            }
            DataStorageMode.DATABASE -> {
                csvTransferImpl?.close()
                csvTransferImpl = null
                csvAllocationImpl?.close()
                csvAllocationImpl = null
                if (databaseImpl == null) {
                    databaseImpl = DatabaseRepositories(this, plugin)
                }
                transferRecordRepository = databaseImpl!!
                allocationLogRepository = databaseImpl!!
            }
        }

        // 重新加载存储库数据
        transferRecordRepository.reload()
    }

    /**
     * 注册事件监听器
     */
    private fun registerEventListeners() {
        // 检查并注册EssentialsX事件监听器
        if (plugin.isPluginPresent("Essentials") || plugin.isPluginPresent("EssentialsX")) {
            try {
                registerEssentialsEventListener()
            } catch (e: Exception) {
                logger.warn("Failed to register EssentialsX event listener", e)
            }
        }

        // 检查并注册XConomy事件监听器
        if (plugin.isPluginPresent("XConomy")) {
            try {
                registerXConomyEventListener()
            } catch (e: Exception) {
                logger.warn("Failed to register XConomy event listener", e)
            }
        }
    }

    /**
     * 注册Essentials事件监听器
     */
    private fun registerEssentialsEventListener() {
        subscribeEvent<UserBalanceUpdateEvent> {
            priority(EventPriority.MONITOR)
            filter { enableTransferDetection } // 只有启用转账检测时才处理
            filter {
                // 检查目标账户是否已设置
                plugin.targetAccount != null
            }
            filter { event ->
                // 确保玩家对象有效
                event.player != null
            }
            handler { event ->
                handleEssentialsUserBalanceUpdate(event)
            }
            onException { event, e ->
                logger.error("处理余额更新事件失败: ${event.player?.name ?: "Unknown"}", e)
                logger.error("事件详情 - 旧余额: ${event.oldBalance}, 新余额: ${event.newBalance}, 原因: ${event.cause.name}")
            }
        }
        logger.info("EssentialsX event listener registered successfully")
    }

    /**
     * 注册XConomy事件监听器
     */
    private fun registerXConomyEventListener() {
        subscribeEvent<PlayerAccountEvent> {
            priority(EventPriority.MONITOR)
            filter { enableTransferDetection } // 只有启用转账检测时才处理
            filter {
                // 检查目标账户是否已设置
                plugin.targetAccount != null
            }
            filter { event ->
                // 确保玩家UUID有效
                event.uniqueId != null
            }
            handler { event ->
                handleXConomyPlayerAccountEvent(event)
            }
            onException { event, e ->
                logger.error("处理XConomy玩家账户事件失败: ${event.uniqueId}", e)
                logger.error("事件详情 - 金额: ${event.getamount()}")
            }
        }
        logger.info("XConomy event listener registered successfully")
    }

    /**
     * 检查经济插件可用性
     */
    private fun checkEconomyPluginsAvailability() {
        val essentialsPlugin = plugin.server.pluginManager.getPlugin("Essentials")
        val xconomyPlugin = plugin.server.pluginManager.getPlugin("XConomy")

        var hasEconomyPlugin = false

        if (essentialsPlugin != null) {
            logger.info("EssentialsX plugin detected: ${essentialsPlugin.pluginMeta.version}")
            hasEconomyPlugin = true
        }

        if (xconomyPlugin != null) {
            logger.info("XConomy plugin detected: ${xconomyPlugin.pluginMeta.version}")
            hasEconomyPlugin = true
        }

        if (!hasEconomyPlugin) {
            logger.warn("No supported economy plugins found - transfer detection will be limited")
            logger.warn("Please install EssentialsX or XConomy for full transfer detection functionality")
        } else {
            logger.info("Transfer detection functionality enabled")
        }
    }

    /**
     * 处理余额更新事件（异步处理）
     *
     * 监听EssentialsX的UserBalanceUpdateEvent事件，自动检测玩家向基金会的转账行为。
     * 为了避免阻塞主线程，所有转账检测逻辑都在异步线程中执行。
     *
     * 处理逻辑：
     * 1. 目标账户收到钱 -> 记录为被动转账（来自其他插件的转账）
     * 2. 目标账户失去钱 -> 忽略（拨款，不记录）
     * 3. 玩家失去钱 -> 记录为主动转账（通过其他插件扣费转给基金会）
     * 4. 玩家收到钱 -> 忽略（收到拨款，不记录）
     * 5. 通过activeTransfer进行的转账会被bypass机制过滤，避免重复记录
     */
    private fun handleEssentialsUserBalanceUpdate(event: UserBalanceUpdateEvent) {
        // 异步处理转账逻辑，避免阻塞主线程
        runAsync { handler ->
            try {
                val now = System.currentTimeMillis()
                val expired = now - expireMilliseconds

                val player = event.player

                // 检查是否为目标账户的余额变化
                if (player.uniqueId == plugin.targetAccount!!.uniqueId) {
                    // 目标账户余额变化
                    val amount = event.newBalance - event.oldBalance

                    // 只处理目标账户收到钱的情况（amount > 0），忽略拨款（amount < 0）
                    if (amount <= BigDecimal.ZERO) {
                        return@runAsync
                    }

                    // 处理目标账户收到钱的情况
                    handleTargetAccountReceiveMoney(amount, now, expired, "EssentialsX")
                } else {
                    // 其他玩家余额变化
                    val amount = event.oldBalance - event.newBalance

                    // 只处理玩家失去钱的情况（amount > 0），忽略玩家收到钱的情况（拨款）
                    if (amount <= BigDecimal.ZERO) {
                        return@runAsync
                    }

                    // 处理玩家失去钱的情况
                    handlePlayerLoseMoney(player, player.uniqueId, amount, now, expired, "EssentialsX")
                }

            } catch (e: Exception) {
                logger.error("Error processing balance update event for player ${event.player?.name ?: "Unknown"}", e)
                logger.error("Event details - Old: ${event.oldBalance}, New: ${event.newBalance}, Cause: ${event.cause.name}")
            }
        }
    }

    /**
     * 处理XConomy玩家账户事件（异步处理）
     *
     * 监听XConomy的PlayerAccountEvent事件，自动检测玩家向基金会的转账行为。
     * 为了避免阻塞主线程，所有转账检测逻辑都在异步线程中执行。
     *
     * 处理逻辑：
     * 1. 目标账户增加金额(isadd=true) -> 记录为被动转账（来自其他插件的转账）
     * 2. 目标账户减少金额(isadd=false) -> 忽略（拨款，不记录）
     * 3. 玩家减少金额(isadd=false) -> 记录为主动转账（通过其他插件扣费转给基金会）
     * 4. 玩家增加金额(isadd=true) -> 忽略（收到拨款，不记录）
     * 5. 通过activeTransfer进行的转账会被bypass机制过滤，避免重复记录
     */
    private fun handleXConomyPlayerAccountEvent(event: PlayerAccountEvent) {
        // 异步处理转账逻辑，避免阻塞主线程
        runAsync { handler ->
            try {
                val now = System.currentTimeMillis()
                val expired = now - expireMilliseconds

                // 获取玩家对象
                val player = plugin.server.getOfflinePlayer(event.uniqueId)

                // 检查是否为目标账户的余额变化
                if (event.uniqueId == plugin.targetAccount!!.uniqueId) {
                    // 只处理目标账户收到钱的情况（isadd=true），忽略拨款（isadd=false）
                    if (!event.getisadd()) {
                        return@runAsync
                    }

                    // 目标账户收到钱 - 这是玩家的捐款
                    val amount = event.getamount()

                    // 处理目标账户收到钱的情况
                    handleTargetAccountReceiveMoney(amount, now, expired, "XConomy")
                } else {
                    // 只处理玩家失去钱的情况（isadd=false），忽略玩家收到钱的情况（拨款）
                    if (event.getisadd()) {
                        return@runAsync
                    }

                    // 玩家失去钱 - 这是玩家的捐款
                    val amount = event.getamount()

                    // 处理玩家失去钱的情况
                    handlePlayerLoseMoney(player, event.uniqueId, amount, now, expired, "XConomy")
                }

            } catch (e: Exception) {
                logger.error("Error processing XConomy player account event for player ${event.uniqueId}", e)
                logger.error("Event details - Amount: ${event.getamount()}, IsAdd: ${event.getisadd()}")
            }
        }
    }

    /**
     * 记录主动转账（玩家向目标账户转账）
     */
    fun recordActiveTransfer(player: OfflinePlayer, amount: BigDecimal) {
        // 更新记录
        transferRecordRepository.updateActiveRecord(player.uniqueId, amount)
    }

    /**
     * 记录被动转账（通过其他方式向基金会转账，如死亡扣费等）
     */
    fun recordPassiveTransfer(player: OfflinePlayer, amount: BigDecimal) {
        // 更新记录
        transferRecordRepository.updatePassiveRecord(player.uniqueId, amount)
    }

    /**
     * 获取转账记录
     */
    fun getTransferRecord(uuid: UUID): TransferRecord? {
        return transferRecordRepository.getRecord(uuid)
    }

    /**
     * 获取排行榜（分页）
     */
    fun getTopDonors(offset: Int = 0, limit: Int = 50): PageResult<TransferRankEntry> {
        return transferRecordRepository.getTopDonors(offset, limit)
    }

    /**
     * 获取转账记录数量
     */
    fun getRecordCount(): Int {
        return transferRecordRepository.getRecordCount()
    }

    /**
     * 检查账户是否有记录
     */
    fun hasRecord(uuid: UUID): Boolean {
        return transferRecordRepository.hasRecord(uuid)
    }

    /**
     * 主动转账（完整的转账流程，包含验证、转账和错误处理）
     *
     * @param player 转账的玩家
     * @param amount 转账金额
     * @return TransferResult 转账结果
     */
    fun activeTransfer(player: Player, amount: Double): TransferResult {
        try {
            // 验证基金账户
            val targetAccount = plugin.targetAccount ?: run {
                logger.warn("Target account not configured for transfer")
                return TransferResult.failure(LanguageKeys.Commands.Donate.NO_TARGET_ACCOUNT)
            }

            // 验证金额
            if (!isValidAmount(amount)) {
                logger.warn("Invalid transfer amount: $amount")
                return TransferResult.failure(LanguageKeys.Commands.Donate.INVALID_AMOUNT)
            }

            // 格式化金额
            val formattedAmount = BigDecimal(amount).setScale(2, RoundingMode.HALF_UP)
            val finalAmount = formattedAmount.toDouble()

            // 检查玩家余额
            val playerBalance = plugin.economy.getBalance(player)
            if (playerBalance < finalAmount) {
                logger.warn("Insufficient balance for transfer: $playerBalance < $finalAmount")
                TransferResult.failure(LanguageKeys.Commands.Donate.INSUFFICIENT_BALANCE,
                    formatAmount(playerBalance), formatAmount(finalAmount))
            }

            // 设置bypass标记避免重复检测
            bypassTransfer2.add(formattedAmount)
            bypassTransfer1.add(player.uniqueId to formattedAmount)

            // 记录转账（在实际转账前记录，确保数据一致性）
            recordActiveTransfer(player, formattedAmount)

            // 执行经济系统转账
            val withdrawResult = plugin.economy.withdrawPlayer(player, finalAmount)
            if (!withdrawResult.transactionSuccess()) {
                logger.error("Transfer withdraw failed: ${withdrawResult.errorMessage}")
                return TransferResult.failure(LanguageKeys.Commands.Donate.WITHDRAW_FAILED, withdrawResult.errorMessage ?: "Unknown error")
            }

            val depositResult = plugin.economy.depositPlayer(targetAccount, finalAmount)
            if (!depositResult.transactionSuccess()) {
                logger.error("Transfer deposit failed: ${depositResult.errorMessage}")
                // 如果存款失败，尝试退还金额
                val refundResult = plugin.economy.depositPlayer(player, finalAmount)
                if (!refundResult.transactionSuccess()) {
                    logger.error("CRITICAL: Failed to refund player after transfer deposit failure! Player: ${player.name}, Amount: $finalAmount")
                }
                return TransferResult.failure(LanguageKeys.Commands.Donate.DEPOSIT_FAILED, depositResult.errorMessage ?: "Unknown error")
            }

            // 根据是否为强制转账返回不同的成功消息
            return TransferResult.success(LanguageKeys.Commands.Donate.SUCCESS, formatAmount(finalAmount))

        } catch (e: Exception) {
            logger.error("Unexpected error during active transfer", e)
            return TransferResult.failure(LanguageKeys.Commands.Error.OPERATION_FAILED, "transfer", e.message ?: "Unknown error")
        }
    }

    /**
     * 验证金额是否有效
     */
    private fun isValidAmount(amount: Double): Boolean {
        return amount > 0.0 && amount.isFinite() && !amount.isNaN()
    }

    /**
     * 格式化金额显示
     */
    private fun formatAmount(amount: Double): String {
        return String.format("%.2f", amount)
    }

    /**
     * 记录拨款日志
     */
    fun allocation(
        operator: CommandSender,
        target: OfflinePlayer,
        amount: Double,
        reason: String
    ) {
        // 检查基金账户
        val targetAccount = plugin.targetAccount ?: run {
            plugin.messager.printf(operator, LanguageKeys.Commands.Allocate.NO_TARGET_ACCOUNT)
            return
        }

        // 检查基金余额
        val foundationBalance = plugin.economy.getBalance(targetAccount)
        if (foundationBalance < amount) {
            plugin.messager.printf(operator, LanguageKeys.Commands.Allocate.INSUFFICIENT_FOUNDATION_BALANCE)
            return
        }

        // 执行转账
        val withdrawResult = plugin.economy.withdrawPlayer(targetAccount, amount)
        if (!withdrawResult.transactionSuccess()) {
            plugin.messager.printf(operator, LanguageKeys.Commands.Allocate.FAILED, withdrawResult.errorMessage ?: "Unknown error")
            return
        }

        val depositResult = plugin.economy.depositPlayer(target, amount)
        if (!depositResult.transactionSuccess()) {
            // 回滚
            plugin.economy.depositPlayer(targetAccount, amount)
            plugin.messager.printf(operator, LanguageKeys.Commands.Allocate.FAILED, depositResult.errorMessage ?: "Unknown error")
            return
        }

        // 拨款日志记录
        val operatorName = when (operator) {
            is Player -> "#Player#${operator.uniqueId}"
            is CommandBlock -> "#CommandBlock#${operator.location.world.name}:${operator.location.blockX}:${operator.location.blockY}:${operator.location.blockZ}"
            else -> "#Console#"
        }
        allocationLogRepository.appendAllocation(AllocationLogRecord(
            date = LocalDateTime.now().format(dateFormatter),
            who = operatorName,
            target = target.uniqueId,
            amount = amount,
            reason = reason
        ))

        plugin.messager.printf(operator, LanguageKeys.Commands.Allocate.SUCCESS, String.format("%.2f", amount), target.name ?: "Unknown", reason)
        plugin.logger.info(LanguageKeys.Commands.Allocate.LOG_SUCCESS, amount, target.name ?: "Unknown", operatorName, reason)
    }

    fun getTotalDonations(): Pair<BigDecimal, BigDecimal> =
        transferRecordRepository.getTotalDonations()

    fun getTotalAllocation() = allocationLogRepository.getTotalAllocation()

    /**
     * 处理目标账户收到钱的情况
     *
     * @param amount 收到的金额
     * @param now 当前时间戳
     * @param expired 过期时间戳
     * @param eventType 事件类型（用于日志）
     */
    private fun handleTargetAccountReceiveMoney(
        amount: BigDecimal,
        now: Long,
        expired: Long,
        eventType: String
    ) {
        if (bypassTransfer2.remove(amount)) {
            return
        }

        // 查看转入交易中有没有金额一致的玩家失去钱
        val transfer = otherTransfers.find { t ->
            val isMatch = t.amount == amount && t.time >= expired
            isMatch
        }

        if (transfer == null) {
            val newSelfTransfer = TransferSelf(amount, now)
            selfTransfers.add(newSelfTransfer)
        } else {
            transfer.time = 0 // 标记为已处理
            recordPassiveTransfer(transfer.account, amount)
        }
    }

    /**
     * 处理玩家失去钱的情况
     *
     * @param player 失去钱的玩家
     * @param playerUuid 玩家UUID
     * @param amount 失去的金额
     * @param now 当前时间戳
     * @param expired 过期时间戳
     * @param eventType 事件类型（用于日志）
     */
    private fun handlePlayerLoseMoney(
        player: OfflinePlayer,
        playerUuid: UUID,
        amount: BigDecimal,
        now: Long,
        expired: Long,
        eventType: String
    ) {
        val bypassKey = playerUuid to amount
        if (bypassTransfer1.remove(bypassKey)) {
            return
        }

        // 查看转出交易中有没有金额一致的目标账户收到钱
        val transfer = selfTransfers.find { t ->
            val isMatch = t.amount == amount && t.time >= expired
            isMatch
        }

        if (transfer == null) {
            val newOtherTransfer = TransferOther(player, amount, now)
            otherTransfers.add(newOtherTransfer)
        } else {
            transfer.time = 0 // 标记为已处理
            recordPassiveTransfer(player, amount)
        }
    }
}

/**
 * 排行榜条目数据类
 */
data class TopEntry(
    val uuid: UUID,
    val player: OfflinePlayer,
    val active: BigDecimal,
    val passive: BigDecimal
)

/**
 * 其他玩家转账记录
 */
private data class TransferOther(
    val account: OfflinePlayer,
    val amount: BigDecimal,
    var time: Long,
)

/**
 * 自己转账记录
 */
private data class TransferSelf(
    val amount: BigDecimal,
    var time: Long,
)

/**
 * 转账结果
 */
data class TransferResult(
    val success: Boolean,
    val messageKey: String,
    val messageArgs: Array<Any> = emptyArray()
) {
    companion object {
        fun success(messageKey: String, vararg args: Any) = TransferResult(true, messageKey, arrayOf(*args))
        fun failure(messageKey: String, vararg args: Any) = TransferResult(false, messageKey, arrayOf(*args))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TransferResult

        if (success != other.success) return false
        if (messageKey != other.messageKey) return false
        if (!messageArgs.contentEquals(other.messageArgs)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = success.hashCode()
        result = 31 * result + messageKey.hashCode()
        result = 31 * result + messageArgs.contentHashCode()
        return result
    }
}
