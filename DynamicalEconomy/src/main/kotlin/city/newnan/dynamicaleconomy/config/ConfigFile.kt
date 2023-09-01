package city.newnan.dynamicaleconomy.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.bukkit.Material
import java.math.BigDecimal

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ConfigFile(
    val owner: String? = null,
    @JsonProperty("exclude-worlds")
    val excludeWorlds: Set<String> = emptySet(),
)

data class EconomyCache(
    val wealth: WealthCache,
    var currencyIssuance: BigDecimal = BigDecimal.ZERO,
    var nationalTreasury: BigDecimal = BigDecimal.ZERO,
    val commodities: MutableMap<String, Commodity> = mutableMapOf(),
)

data class WealthCache(
    var total: BigDecimal = BigDecimal.ZERO,
    @JsonProperty("valued-resource-count")
    val valuedResourceCount: MutableMap<Material, Long> = mutableMapOf(),
)

data class Commodity(
    var data: String,
    var amount: Long,
    var value: Double,
    var sellResponseVolume: Double,
    var buyResponseVolume: Double,
    var lastSellTime: Long,
    var lastBuyTime: Long,
)