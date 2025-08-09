package city.newnan.dynamiceconomy.modules

import city.newnan.config.extensions.configManager
import city.newnan.core.base.BaseModule
import city.newnan.core.scheduler.runAsyncRepeating
import city.newnan.config.serializers.InventorySerializationUtils
import city.newnan.core.scheduler.runSync
import city.newnan.dynamiceconomy.DynamicEconomyPlugin
import city.newnan.dynamiceconomy.config.Commodity
import city.newnan.dynamiceconomy.config.EconomyCache
import city.newnan.dynamiceconomy.i18n.LanguageKeys
import org.bukkit.inventory.ItemStack
import java.math.BigDecimal
import kotlin.math.log10
import kotlin.math.pow

/**
 * 商品管理器
 *
 * 负责管理动态商品系统，包括：
 * - 商品价格动态调整
 * - 商品交易处理
 * - 供需关系管理
 * - 商品数据持久化
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class CommodityManager(
    moduleName: String,
    val plugin: DynamicEconomyPlugin
) : BaseModule(moduleName, plugin) {

    // ===== 模块初始化 =====
    init { init() }

    // ===== 配置缓存 =====
    private var enableDynamicPricing = true
    private var priceUpdateInterval = 60L // 1分钟
    private var maxPriceRatio = 10.0
    private var minPriceRatio = 1.0

    // ===== 商品缓存 =====
    private val commodities = mutableMapOf<String, SystemCommodity>()

    // ===== 常量 =====
    companion object {
        private const val EPSILON = 0.001
    }

    override fun onInit() {
        // 定时更新商品价格
        runAsyncRepeating(0L, priceUpdateInterval * 20L) {
            if (enableDynamicPricing) {
                updateAllPrices()
            }
        }
    }

    override fun onReload() {
        // 重新加载配置
        loadConfigurations()

        // 重新加载商品数据
        loadCommodities()
    }

    /**
     * 加载配置
     */
    private fun loadConfigurations() {
        val config = plugin.getPluginConfig()
        enableDynamicPricing = config.commoditySettings.enableDynamicPricing
        priceUpdateInterval = config.commoditySettings.priceUpdateInterval
        maxPriceRatio = config.commoditySettings.maxPriceRatio
        minPriceRatio = config.commoditySettings.minPriceRatio
    }

    /**
     * 加载商品数据
     */
    private fun loadCommodities() {
        plugin.configManager.touchWithMerge("economy-cache.yml", "economy-cache-template.yml", createBackup = false)
        val economyCache = plugin.configManager.parse<EconomyCache>("economy-cache.yml")

        commodities.clear()
        economyCache.commodities.forEach { (name: String, commodity: Commodity) ->
            commodities[name] = SystemCommodity(commodity, name)
        }

        logger.info("Loaded ${commodities.size} commodities")
    }

    /**
     * 保存商品数据
     */
    private fun saveCommodities() {
        plugin.configManager.touchWithMerge("economy-cache.yml", "economy-cache-template.yml", createBackup = false)
        val economyCache = plugin.configManager.parse<EconomyCache>("economy-cache.yml")

        commodities.forEach { (name, systemCommodity) ->
            economyCache.commodities[name] = systemCommodity.config
        }

        plugin.configManager.save(economyCache, "economy-cache.yml")
    }

    /**
     * 更新所有商品价格
     */
    fun updateAllPrices() {
        commodities.values.forEach { commodity ->
            commodity.updatePrice()
        }
        logger.debug("Updated prices for ${commodities.size} commodities")
    }

    /**
     * 获取商品
     */
    fun getCommodity(name: String): SystemCommodity? = commodities[name]

    /**
     * 获取所有商品
     */
    fun getAllCommodities(): Map<String, SystemCommodity> = commodities.toMap()

    /**
     * 添加商品
     */
    fun addCommodity(name: String, commodity: Commodity): Boolean {
        if (commodities.containsKey(name)) {
            return false // 商品已存在
        }

        commodities[name] = SystemCommodity(commodity, name)
        saveCommodities()
        logger.info("Added commodity: $name")
        return true
    }

    /**
     * 移除商品
     */
    fun removeCommodity(name: String): Boolean {
        val removed = commodities.remove(name) != null
        if (removed) {
            saveCommodities()
            logger.info("Removed commodity: $name")
        }
        return removed
    }

    /**
     * 购买商品（玩家 -> 系统）
     */
    fun buyCommodity(name: String, amount: Long): Boolean {
        val commodity = commodities[name] ?: return false

        try {
            commodity.buy(amount)
            saveCommodities()
            logger.info(LanguageKeys.Log.Info.COMMODITY_TRADED, "buy", name, amount)
            return true
        } catch (e: Exception) {
            logger.error(LanguageKeys.Log.Error.COMMODITY_ERROR, e)
            return false
        }
    }

    /**
     * 出售商品（系统 -> 玩家）
     */
    fun sellCommodity(name: String, amount: Long): Boolean {
        val commodity = commodities[name] ?: return false

        if (commodity.config.amount < amount) {
            return false // 库存不足
        }

        try {
            commodity.sell(amount)
            saveCommodities()
            logger.info(LanguageKeys.Log.Info.COMMODITY_TRADED, "sell", name, amount)
            return true
        } catch (e: Exception) {
            logger.error(LanguageKeys.Log.Error.COMMODITY_ERROR, e)
            return false
        }
    }

    /**
     * 系统商品类
     */
    inner class SystemCommodity(val config: Commodity, val name: String) {
        var itemStack: ItemStack? = null
        var buyValue = 0.0
        var sellValue = 0.0

        init {
            // 从配置数据解析ItemStack
            // 支持两种格式：
            // 1) Base64 编码（兼容旧实现）
            // 2) JSON 字符串（以 '{' 开头，使用config模块的ObjectMapper解析）
            itemStack = try {
                if (config.data.isNotEmpty() && config.data.first() == '{') {
                    // JSON 格式：使用ObjectMapper读取（需在主线程操作ItemStack构造安全）
                    var parsed: ItemStack? = null
                    runSync { _ ->
                        val mapper = plugin.configManager.getMapper("json")
                        parsed = mapper.readValue(config.data, ItemStack::class.java)
                        parsed
                    }.getNow(null)
                    parsed
                } else {
                    // Base64 格式：解码为ItemStack
                    runSync { _ ->
                        InventorySerializationUtils.decodeItemStack(config.data)
                    }.getNow(null)
                }
            } catch (e: Exception) {
                logger.warning("Failed to parse ItemStack for commodity $name: ${e.message}")
                null
            }
            updatePrice()
        }

        /**
         * 收购：系统 <- 玩家
         */
        fun buy(amount: Long) {
            // 确保国库余额充足
            plugin.getEconomyManager().ensureNationalTreasury()

            config.amount += amount
            val curTime = System.currentTimeMillis()

            // 计算γ（时间衰减因子）
            val gamma = if (config.lastBuyTime == 0L) {
                0.0
            } else {
                10.0 / (10.0 + log10((1 + curTime - config.lastBuyTime).toDouble()))
            }

            // 更新时间和响应量
            config.lastBuyTime = curTime
            config.buyResponseVolume = amount + gamma * config.buyResponseVolume

            // 更新商品价值
            updatePrice()
        }

        /**
         * 售卖：系统 -> 玩家
         */
        fun sell(amount: Long) {
            config.amount = (config.amount - amount).coerceAtLeast(0L)
            val curTime = System.currentTimeMillis()

            // 计算γ（时间衰减因子）
            val gamma = if (config.lastSellTime == 0L) {
                0.0
            } else {
                10.0 / (10.0 + log10((1 + curTime - config.lastSellTime).toDouble()))
            }

            // 更新时间和响应量
            config.lastSellTime = curTime
            config.sellResponseVolume = amount + gamma * config.sellResponseVolume

            // 更新商品价值
            updatePrice()
        }

        /**
         * 更新价格
         */
        fun updatePrice() {
            // 计算响应比
            var ratio = (config.amount + config.sellResponseVolume + EPSILON) /
                       (config.amount + config.buyResponseVolume + EPSILON)

            // 限制价格比例范围
            ratio = ratio.coerceIn(minPriceRatio, maxPriceRatio)

            // 计算买入和卖出价格
            buyValue = config.value * ratio.pow(0.8)
            sellValue = config.value * ratio.pow(1.2)
        }

        /**
         * 获取买入价格
         */
        fun getBuyPrice(): BigDecimal = BigDecimal.valueOf(buyValue)

        /**
         * 获取卖出价格
         */
        fun getSellPrice(): BigDecimal = BigDecimal.valueOf(sellValue)

        /**
         * 获取库存数量
         */
        fun getStock(): Long = config.amount
    }
}
