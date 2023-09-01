package city.newnan.dynamicaleconomy

import city.newnan.dynamicaleconomy.config.ConfigFile
import city.newnan.dynamicaleconomy.config.EconomyCache
import city.newnan.violet.config.ConfigManager2
import city.newnan.violet.message.MessageManager
import co.aikar.commands.PaperCommandManager
import me.lucko.helper.Events
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.plugin.ExtendedJavaPlugin
import net.ess3.api.events.UserBalanceUpdateEvent
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.ItemDespawnEvent
import org.bukkit.scoreboard.Scoreboard
import java.math.BigDecimal
import java.util.Locale
import kotlin.math.pow

class PluginMain : ExtendedJavaPlugin() {
    companion object {
        lateinit var INSTANCE: PluginMain
            private set
    }
    init { INSTANCE = this }
    private val configManager: ConfigManager2 by lazy {
        ConfigManager2(this).apply {
            setCache(ConfigManager2.CacheType.None)
        }
    }
    val messageManager: MessageManager by lazy { MessageManager(this) }
    private val commandManager: PaperCommandManager by lazy { PaperCommandManager(this) }
    val excludedWorlds = hashSetOf<String>()
    lateinit var economy: Economy
    var owner: OfflinePlayer? = null
    lateinit var cache: EconomyCache
    val systemCommodities = mutableMapOf<Material, MutableList<SystemCommodity>>()

    /**
     * 参考货币指数
     */
    var referenceCurrencyIndex = 1.0
    /**
     * 收购货币指数
     */
    var buyCurrencyIndex = 0.0
    /**
     * 出售货币指数
     */
    var sellCurrencyIndex = 0.0

    override fun enable() {
        if (server.pluginManager.getPlugin("Vault") == null) {
            throw Exception("Vault not found!")
        }
        economy = server.servicesManager.getRegistration(Economy::class.java)?.provider
            ?: throw Exception("Vault economy service not found!")

        reload()
        commandManager.enableUnstableAPI("help")
        commandManager.registerCommand(Commands)
        commandManager.locales.setDefaultLocale(Locale.SIMPLIFIED_CHINESE)
        messageManager setPlayerPrefix "§7[§6牛腩小镇§7] §f"

        // 破坏方块掉落价值资源时，更新系统总价值量
        Events.subscribe(BlockDropItemEvent::class.java, EventPriority.MONITOR)
            .filter(EventFilters.ignoreCancelled())
            .filter { e -> e.player.gameMode == org.bukkit.GameMode.SURVIVAL }
            .filter { e -> e.block.world.name !in excludedWorlds }
            .filter { e -> Data.valueResourceBlockItemMap.containsKey(e.block.type) }
            .filter { e -> !e.player.hasPermission("dynamical-economy.statics.bypass") }
            .handler { event ->
                val sourceDropItem = event.block.type
                val targetDropItem = Data.valueResourceBlockItemMap[sourceDropItem]!!
                var deltaWealth = 0.0
                event.items.forEach {
                    if (it.itemStack.type != sourceDropItem && it.itemStack.type != targetDropItem) return@forEach
                    cache.wealth.valuedResourceCount[sourceDropItem] =
                        (cache.wealth.valuedResourceCount[sourceDropItem] ?: 0) + it.itemStack.amount
                    deltaWealth += Data.valueResourceValueMap[targetDropItem]!! * it.itemStack.amount
                }
                cache.wealth.total += BigDecimal.valueOf(deltaWealth)
            }
            .bindWith(this)

        // 玩家放置价值资源时，更新系统总价值量
        Events.subscribe(BlockPlaceEvent::class.java, EventPriority.MONITOR)
            .filter(EventFilters.ignoreCancelled())
            .filter { it.canBuild() }
            .filter { e -> e.player.gameMode == org.bukkit.GameMode.SURVIVAL }
            .filter { e -> e.block.world.name !in excludedWorlds }
            .filter { e -> Data.valueResourceBlockItemMap.containsKey(e.blockPlaced.type) }
            .filter { e -> !e.player.hasPermission("dynamical-economy.statics.bypass") }
            .handler { event ->
                val type = event.blockPlaced.type
                cache.wealth.valuedResourceCount[type] = (cache.wealth.valuedResourceCount[type] ?: 1) - 1
                cache.wealth.total -= BigDecimal.valueOf(Data.valueResourceValueMap[type]!!)
            }
            .bindWith(this)

        // 物品被清理时，破坏方块掉落价值资源时，更新系统总价值量
        Events.subscribe(ItemDespawnEvent::class.java, EventPriority.MONITOR)
            .filter(EventFilters.ignoreCancelled())
            .filter { e -> e.entity.world.name !in excludedWorlds }
            .handler { event ->
                val type = when {
                    Data.valueResourceItemBlockMap.containsKey(event.entity.itemStack.type) -> {
                        Data.valueResourceItemBlockMap[event.entity.itemStack.type]!!
                    }
                    Data.valueResourceBlockItemMap.containsKey(event.entity.itemStack.type) -> {
                        event.entity.itemStack.type
                    }
                    else -> return@handler
                }
                val value = Data.valueResourceValueMap[type]!!
                val amount = event.entity.itemStack.amount.toLong()
                cache.wealth.total -= BigDecimal.valueOf(value * amount)
                cache.wealth.valuedResourceCount[type] =
                    (cache.wealth.valuedResourceCount[type] ?: amount) - amount
            }
            .bindWith(this)

        // 物品以其他方式消失就不检测了，spigot这种太难做

        // 玩家现金改变时，更新国库货币储量
        Events.subscribe(UserBalanceUpdateEvent::class.java, EventPriority.MONITOR)
            .filter { e -> owner != null && e.player.uniqueId != owner!!.uniqueId }
            .handler { event ->
                adjustNationalTreasury(event.newBalance.toDouble() - event.oldBalance.toDouble())
            }
            .bindWith(this)
    }

    override fun disable() {
        commandManager.unregisterCommands()
    }

    fun reload() {
        // config.yml
        configManager touch "config.yml"
        val config = configManager.parse<ConfigFile>("config.yml")
        owner = config.owner?.let { name ->
            if (name.isBlank()) return@let null
            Bukkit.getOfflinePlayers().find { p -> p.name == name }?.also { p ->
                messageManager.info("设置国库账户为: ${p.name}")
            }
        }
        if (owner == null) {
            messageManager.warn("国库账户未设置, 将不会有任何货币流通.")
        }
        excludedWorlds.clear()
        excludedWorlds.addAll(config.excludeWorlds)

        cache = configManager.parse<EconomyCache>("cache.yml")
        systemCommodities.clear()
        cache.commodities.forEach { (name, commodity) ->
            val c = SystemCommodity(commodity, name)
            if (c.itemStack == null) return@forEach
            systemCommodities.getOrPut(c.itemStack!!.type) { mutableListOf() }.add(c)
        }
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
        cache.nationalTreasury += BigDecimal.valueOf(deltaValue)
    }

    /**
     * 在国库内发行/销毁货币，即以改变国库货币储量的方式改变货币发行量
     * 这个才是真-手动调用的那个方法
     * @param deltaValue 发行量增量，正值发行货币，负值销毁货币
     */
    fun issueCurrency(deltaValue: Double) {
        val v = BigDecimal.valueOf(deltaValue)
        cache.nationalTreasury += v
        cache.currencyIssuance += v
    }

    /**
     * 手动调整系统累计价值量
     * @param deltaValue 调整增量
     */
    fun adjustSystemTotalWealth(deltaValue: Double) {
        cache.wealth.total += BigDecimal.valueOf(deltaValue)
    }

    /**
     * 更新货币指数
     */
    fun updateCurrencyIndex() {
        referenceCurrencyIndex =
            if (cache.wealth.total == BigDecimal.ZERO || cache.currencyIssuance == BigDecimal.ZERO) 1.0
            else (cache.currencyIssuance / cache.wealth.total).toDouble()
        buyCurrencyIndex = referenceCurrencyIndex.pow(0.691)
        sellCurrencyIndex = referenceCurrencyIndex.pow(1.309)
        systemCommodities.forEach { (_, commodities) -> commodities.forEach(SystemCommodity::updatePrice) }
    }

    /**
     * 重新遍历所有的玩家、城镇、集体，统计其持有的货币总量，以更新货币发行量
     */
    fun reloadCurrencyIssuance() {
        var currency = 0.0
        Bukkit.getOfflinePlayers().forEach { p ->
            currency += economy.getBalance(p)
        }
        cache.currencyIssuance = cache.nationalTreasury + BigDecimal.valueOf(currency)
    }
}
