package city.newnan.dynamiceconomy.config

import city.newnan.core.config.CorePluginConfig
import city.newnan.core.logging.LogLevel
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.bukkit.Material
import java.math.BigDecimal

/**
 * DynamicEconomy插件配置类
 *
 * @author NewNanCity
 * @since 2.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class DynamicEconomyConfig(
    @JsonProperty("debug")
    val debug: Boolean = false,

    @JsonProperty("player-prefix")
    val playerPrefix: String = "§7[§6动态经济§7] §f",

    @JsonProperty("console-prefix")
    val consolePrefix: String = "[DynamicEconomy] ",

    @JsonProperty("owner")
    val owner: String = "Server",

    @JsonProperty("exclude-worlds")
    val excludeWorlds: Set<String> = setOf("world_nether", "world_the_end"),

    @JsonProperty("economy-settings")
    val economySettings: EconomySettings = EconomySettings(),

    @JsonProperty("commodity-settings")
    val commoditySettings: CommoditySettings = CommoditySettings()
) {
    /**
     * 获取核心配置
     */
    fun getCoreConfig(): CorePluginConfig = CorePluginConfig.build {
        message.playerPrefix = playerPrefix
        message.consolePrefix = consolePrefix
    }
}

/**
 * 经济系统设置
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class EconomySettings(
    @JsonProperty("enable-wealth-tracking")
    val enableWealthTracking: Boolean = true,

    @JsonProperty("enable-currency-issuance")
    val enableCurrencyIssuance: Boolean = true,

    @JsonProperty("national-treasury-threshold")
    val nationalTreasuryThreshold: BigDecimal = BigDecimal("500000"),

    @JsonProperty("wealth-update-interval")
    val wealthUpdateInterval: Long = 300L // 5分钟
)

/**
 * 商品系统设置
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CommoditySettings(
    @JsonProperty("enable-dynamic-pricing")
    val enableDynamicPricing: Boolean = true,

    @JsonProperty("price-update-interval")
    val priceUpdateInterval: Long = 60L, // 1分钟

    @JsonProperty("max-price-ratio")
    val maxPriceRatio: Double = 10.0,

    @JsonProperty("min-price-ratio")
    val minPriceRatio: Double = 1.0
)

/**
 * 经济缓存数据
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class EconomyCache(
    @JsonProperty("wealth")
    val wealth: WealthCache = WealthCache(),

    @JsonProperty("currency-issuance")
    var currencyIssuance: BigDecimal = BigDecimal.ZERO,

    @JsonProperty("national-treasury")
    var nationalTreasury: BigDecimal = BigDecimal.ZERO,

    @JsonProperty("commodities")
    val commodities: MutableMap<String, Commodity> = mutableMapOf()
)

/**
 * 财富缓存数据
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class WealthCache(
    @JsonProperty("total")
    var total: BigDecimal = BigDecimal.ZERO,

    @JsonProperty("valued-resource-count")
    val valuedResourceCount: MutableMap<Material, Long> = mutableMapOf(),

    @JsonProperty("last-update")
    var lastUpdate: Long = 0L
)

/**
 * 商品数据
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Commodity(
    @JsonProperty("data")
    var data: String,

    @JsonProperty("amount")
    var amount: Long,

    @JsonProperty("value")
    var value: Double,

    @JsonProperty("sell-response-volume")
    var sellResponseVolume: Double = 0.0,

    @JsonProperty("buy-response-volume")
    var buyResponseVolume: Double = 0.0,

    @JsonProperty("last-sell-time")
    var lastSellTime: Long = 0L,

    @JsonProperty("last-buy-time")
    var lastBuyTime: Long = 0L
)

/**
 * 价值资源数据
 */
object ValueResourceData {
    /**
     * 价值资源矿与价值资源的对应
     */
    val valueResourceBlockItemMap = mapOf(
        Material.COAL_ORE to Material.COAL,             // 煤矿 -> 煤炭
        Material.IRON_ORE to Material.IRON_INGOT,       // 铁矿 -> 铁锭
        Material.GOLD_ORE to Material.GOLD_INGOT,       // 金矿 -> 金锭
        Material.REDSTONE_ORE to Material.REDSTONE,     // 红石矿 -> 红石
        Material.LAPIS_ORE to Material.LAPIS_LAZULI,    // 青金石矿 -> 青金石
        Material.DIAMOND_ORE to Material.DIAMOND,       // 钻石矿 -> 钻石
        Material.EMERALD_ORE to Material.EMERALD,       // 绿宝石矿 -> 绿宝石
        Material.NETHER_QUARTZ_ORE to Material.QUARTZ,  // 下界石英矿 -> 下界石英
        Material.ANCIENT_DEBRIS to Material.NETHERITE_SCRAP, // 远古残骸 -> 下界合金碎片
        // 深层矿石
        Material.DEEPSLATE_COAL_ORE to Material.COAL,
        Material.DEEPSLATE_IRON_ORE to Material.IRON_INGOT,
        Material.DEEPSLATE_GOLD_ORE to Material.GOLD_INGOT,
        Material.DEEPSLATE_REDSTONE_ORE to Material.REDSTONE,
        Material.DEEPSLATE_LAPIS_ORE to Material.LAPIS_LAZULI,
        Material.DEEPSLATE_DIAMOND_ORE to Material.DIAMOND,
        Material.DEEPSLATE_EMERALD_ORE to Material.EMERALD

    )

    val valueResourceItemBlockMap = valueResourceBlockItemMap.entries.associate { (k, v) -> v to k }

    /**
     * 价值资源的价值量
     */
    val valueResourceValueMap = mapOf(
        Material.COAL_ORE to 1.7,          // 煤矿石
        Material.IRON_ORE to 3.2,          // 铁矿石
        Material.GOLD_ORE to 12.5,         // 金矿石
        Material.REDSTONE_ORE to 35.0,     // 红石矿石
        Material.LAPIS_ORE to 90.0,        // 青金石矿石
        Material.DIAMOND_ORE to 100.0,     // 钻石矿石
        Material.EMERALD_ORE to 620.0,     // 绿宝石矿石
        Material.NETHER_QUARTZ_ORE to 5.2, // 下界石英矿石
        Material.ANCIENT_DEBRIS to 1000.0, // 远古残骸
        // 深层矿石价值相同
        Material.DEEPSLATE_COAL_ORE to 1.7,
        Material.DEEPSLATE_IRON_ORE to 3.2,
        Material.DEEPSLATE_GOLD_ORE to 12.5,
        Material.DEEPSLATE_REDSTONE_ORE to 35.0,
        Material.DEEPSLATE_LAPIS_ORE to 90.0,
        Material.DEEPSLATE_DIAMOND_ORE to 100.0,
        Material.DEEPSLATE_EMERALD_ORE to 620.0
    )
}
