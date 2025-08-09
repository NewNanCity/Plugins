package city.newnan.dynamiceconomy.modules

import city.newnan.config.extensions.configManager
import city.newnan.core.base.BaseModule
import city.newnan.core.event.subscribeEvent
import city.newnan.dynamiceconomy.DynamicEconomyPlugin
import city.newnan.dynamiceconomy.config.EconomyCache
import city.newnan.dynamiceconomy.i18n.LanguageKeys
import me.yic.xconomy.api.event.PlayerAccountEvent
import net.ess3.api.events.UserBalanceUpdateEvent
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.event.EventPriority
import java.math.BigDecimal
import java.util.UUID
import kotlin.math.pow

/**
 * 经济管理器
 *
 * 负责管理动态经济系统，包括：
 * - 货币发行管理
 * - 国库管理
 * - 经济平衡调节
 * - Vault经济系统集成
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class EconomyManager(
    moduleName: String,
    val plugin: DynamicEconomyPlugin
) : BaseModule(moduleName, plugin) {

    // ===== 模块初始化 =====
    init { init() }

    // ===== 配置缓存 =====
    private var enableCurrencyIssuance = true
    private var nationalTreasuryThreshold = BigDecimal("500000")
    private var owner: OfflinePlayer? = null

    // ===== 经济缓存 =====
    private var economyCache = EconomyCache()

    // ===== 货币指数 =====
    /**
     * 参考货币指数
     */
    var referenceCurrencyIndex = 1.0
        private set

    /**
     * 收购货币指数
     */
    var buyCurrencyIndex = 0.0
        private set

    /**
     * 出售货币指数
     */
    var sellCurrencyIndex = 0.0
        private set

    override fun onInit() {
        // 玩家现金改变时，更新国库货币储量
        subscribeEvent<UserBalanceUpdateEvent> {
            priority(EventPriority.MONITOR)
            handler { event ->
                if (owner == null) return@handler
                if (owner!!.uniqueId != event.player.uniqueId) {
                    adjustNationalTreasury(event.newBalance.toDouble() - event.oldBalance.toDouble())
                }
            }
        }
        subscribeEvent<PlayerAccountEvent> {
            priority(EventPriority.MONITOR)
            handler { event ->
                if (owner == null) return@handler
                if (owner!!.uniqueId != event.uniqueId) {
                    adjustNationalTreasury(event.getamount().toDouble() * if (event.getisadd()) 1.0 else -1.0)
                }
            }
        }
    }

    override fun onReload() {
        // 重新加载配置
        loadConfigurations()

        // 重新加载经济缓存
        loadEconomyCache()

        // 更新货币指数
        updateCurrencyIndex()
    }

    /**
     * 加载配置
     */
    private fun loadConfigurations() {
        val config = plugin.getPluginConfig()
        enableCurrencyIssuance = config.economySettings.enableCurrencyIssuance
        nationalTreasuryThreshold = config.economySettings.nationalTreasuryThreshold
        owner = config.owner.let { name ->
            if (name.isBlank()) return@let null
            try {
                val uuid = UUID.fromString(name)
                val account = Bukkit.getOfflinePlayer(uuid)
                logger.info(LanguageKeys.Economy.OWNER_ACCOUNT_SET, "[UUID: $name]")
                account
            } catch (e: IllegalArgumentException) {
                val account = Bukkit.getOfflinePlayer(name)
                if (!account.hasPlayedBefore()) throw Exception("Player $name not found")
                logger.info(LanguageKeys.Economy.OWNER_ACCOUNT_SET, name)
                account
            }
        }
    }

    /**
     * 加载经济缓存
     */
    private fun loadEconomyCache() {
        plugin.configManager.touchWithMerge("economy-cache.yml", "economy-cache-template.yml", createBackup = false)
        economyCache = plugin.configManager.parse<EconomyCache>("economy-cache.yml")
    }

    /**
     * 保存经济缓存
     */
    private fun saveEconomyCache() {
        plugin.configManager.save(economyCache, "economy-cache.yml")
    }

    /**
     * 获取货币发行量
     */
    fun getCurrencyIssuance(): BigDecimal = economyCache.currencyIssuance

    /**
     * 获取国库余额
     */
    fun getNationalTreasury(): BigDecimal = economyCache.nationalTreasury

    /**
     * 更新货币发行量
     */
    fun updateCurrencyIssuance(amount: BigDecimal) {
        economyCache.currencyIssuance = economyCache.currencyIssuance.add(amount)
        saveEconomyCache()
        logger.info("Currency issuance updated: ${economyCache.currencyIssuance}")
    }

    /**
     * 更新国库余额
     */
    fun updateNationalTreasury(amount: BigDecimal) {
        economyCache.nationalTreasury = economyCache.nationalTreasury.add(amount)
        saveEconomyCache()
        logger.info("National treasury updated: ${economyCache.nationalTreasury}")
    }

    /**
     * 确保国库余额充足
     */
    fun ensureNationalTreasury(): Boolean {
        if (plugin.economy == null) {
            logger.warning("Vault economy not available")
            return false
        }

        try {
            val ownerPlayer = owner ?: return false
            val currentBalance = plugin.economy!!.getBalance(ownerPlayer)

            if (BigDecimal.valueOf(currentBalance) < nationalTreasuryThreshold) {
                val deficit = nationalTreasuryThreshold.subtract(BigDecimal.valueOf(currentBalance))

                // 增发货币补充国库
                if (enableCurrencyIssuance) {
                    plugin.economy!!.depositPlayer(ownerPlayer, deficit.toDouble())
                    updateCurrencyIssuance(deficit)
                    updateNationalTreasury(deficit)

                    logger.info("National treasury replenished: ${deficit}")
                    return true
                } else {
                    logger.warning(LanguageKeys.Economy.INSUFFICIENT_TREASURY)
                    return false
                }
            }

            return true
        } catch (e: Exception) {
            logger.error(LanguageKeys.Log.Error.ECONOMY_ERROR, e)
            return false
        }
    }

    /**
     * 执行经济交易
     */
    fun executeTransaction(player: OfflinePlayer, amount: BigDecimal, isDeposit: Boolean): Boolean {
        if (plugin.economy == null) {
            logger.warning("Vault economy not available")
            return false
        }

        try {
            val success = if (isDeposit) {
                plugin.economy!!.depositPlayer(player, amount.toDouble()).transactionSuccess()
            } else {
                plugin.economy!!.withdrawPlayer(player, amount.toDouble()).transactionSuccess()
            }

            if (success) {
                // 更新国库统计
                val treasuryChange = if (isDeposit) amount.negate() else amount
                updateNationalTreasury(treasuryChange)

                logger.info("Transaction executed: ${player.name}, amount: ${amount}, deposit: ${isDeposit}")
            }

            return success
        } catch (e: Exception) {
            logger.error(LanguageKeys.Log.Error.ECONOMY_ERROR, e)
            return false
        }
    }

    /**
     * 获取玩家余额
     */
    fun getPlayerBalance(player: OfflinePlayer): BigDecimal? {
        return if (plugin.economy != null) {
            try {
                BigDecimal.valueOf(plugin.economy!!.getBalance(player))
            } catch (e: Exception) {
                logger.error(LanguageKeys.Log.Error.ECONOMY_ERROR, e)
                null
            }
        } else {
            null
        }
    }

    /**
     * 检查玩家是否有足够余额
     */
    fun hasEnoughBalance(player: OfflinePlayer, amount: BigDecimal): Boolean {
        val balance = getPlayerBalance(player) ?: return false
        return balance >= amount
    }

    /**
     * 调整国库货币储量数值
     * 这一步通常由上面的事件监听自动调用，但是有两个特例：
     *   - 城镇/集体成员给城镇打钱、或者从中提款，这一步没有经过国库但是却改变了国库，需要加反作用量
     *   - 城镇/集体被系统扣款，这一步没有被上面的监听自动操作，需要自己加正作用量
     * 注意：如果不是上面两个特例请不要调用这个方法，因为这个不会更新货币总发行量！
     * @param deltaValue 调整增量
     */
    fun adjustNationalTreasury(deltaValue: Double) {
        economyCache.nationalTreasury = economyCache.nationalTreasury.add(BigDecimal.valueOf(deltaValue))
        saveEconomyCache()
    }

    /**
     * 在国库内发行/销毁货币，即以改变国库货币储量的方式改变货币发行量
     * 这个才是真-手动调用的那个方法
     * @param deltaValue 发行量增量，正值发行货币，负值销毁货币
     */
    fun issueCurrency(deltaValue: Double) {
        val v = BigDecimal.valueOf(deltaValue)
        economyCache.nationalTreasury = economyCache.nationalTreasury.add(v)
        economyCache.currencyIssuance = economyCache.currencyIssuance.add(v)
        saveEconomyCache()
        logger.info("Currency issued: $deltaValue, total issuance: ${economyCache.currencyIssuance}")
    }

    /**
     * 手动调整系统累计价值量
     * @param deltaValue 调整增量
     */
    fun adjustSystemTotalWealth(deltaValue: Double) {
        plugin.getWealthManager().forceUpdateWealth()
    }

    /**
     * 更新货币指数
     */
    fun updateCurrencyIndex() {
        val totalWealth = plugin.getWealthManager().getTotalWealth()
        referenceCurrencyIndex = if (totalWealth == BigDecimal.ZERO || economyCache.currencyIssuance == BigDecimal.ZERO) {
            1.0
        } else {
            (economyCache.currencyIssuance.divide(totalWealth, 10, BigDecimal.ROUND_HALF_UP)).toDouble()
        }

        buyCurrencyIndex = referenceCurrencyIndex.pow(0.691)
        sellCurrencyIndex = referenceCurrencyIndex.pow(1.309)

        // 更新所有商品价格
        plugin.getCommodityManager().updateAllPrices()

        logger.debug("Currency index updated - Reference: $referenceCurrencyIndex, Buy: $buyCurrencyIndex, Sell: $sellCurrencyIndex")
    }

    /**
     * 重新遍历所有的玩家、城镇、集体，统计其持有的货币总量，以更新货币发行量
     */
    fun reloadCurrencyIssuance() {
        if (plugin.economy == null) {
            logger.warning("Vault economy not available")
            return
        }

        try {
            var currency = 0.0
            Bukkit.getOfflinePlayers().forEach { player ->
                currency += plugin.economy!!.getBalance(player)
            }
            economyCache.currencyIssuance = economyCache.nationalTreasury.add(BigDecimal.valueOf(currency))
            saveEconomyCache()
            logger.info("Currency issuance reloaded: ${economyCache.currencyIssuance}")
        } catch (e: Exception) {
            logger.error("Failed to reload currency issuance", e)
        }
    }

    /**
     * 获取经济统计信息
     */
    fun getEconomyStats(): Map<String, Any> {
        return mapOf(
            "currencyIssuance" to economyCache.currencyIssuance,
            "nationalTreasury" to economyCache.nationalTreasury,
            "totalWealth" to plugin.getWealthManager().getTotalWealth(),
            "referenceCurrencyIndex" to referenceCurrencyIndex,
            "buyCurrencyIndex" to buyCurrencyIndex,
            "sellCurrencyIndex" to sellCurrencyIndex,
            "enableCurrencyIssuance" to enableCurrencyIssuance,
            "nationalTreasuryThreshold" to nationalTreasuryThreshold
        )
    }
}
