package city.newnan.dynamiceconomy.modules

import city.newnan.config.extensions.configManager
import city.newnan.core.base.BaseModule
import city.newnan.core.event.subscribeEvent
import city.newnan.core.scheduler.runAsync
import city.newnan.core.scheduler.runAsyncRepeating
import city.newnan.dynamiceconomy.DynamicEconomyPlugin
import city.newnan.dynamiceconomy.config.EconomyCache
import city.newnan.dynamiceconomy.config.ValueResourceData
import city.newnan.dynamiceconomy.config.WealthCache
import city.newnan.dynamiceconomy.i18n.LanguageKeys
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.ItemDespawnEvent
import org.bukkit.inventory.ItemStack
import java.math.BigDecimal

/**
 * 财富管理器
 *
 * 负责管理服务器财富统计，包括：
 * - 价值资源统计
 * - 总财富计算
 * - 实时财富跟踪
 * - 财富变化监听
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class WealthManager(
    moduleName: String,
    val plugin: DynamicEconomyPlugin
) : BaseModule(moduleName, plugin) {

    // ===== 模块初始化 =====
    init { init() }

    // ===== 配置缓存 =====
    private var enableWealthTracking = true
    private var wealthUpdateInterval = 300L // 5分钟
    private var excludeWorlds = setOf<String>()

    // ===== 财富缓存 =====
    private var wealthCache = WealthCache()

    override fun onInit() {
        // 注册事件监听器
        registerEventListeners()

        // 启动定时任务
        startScheduledTasks()
    }

    override fun onReload() {
        // 重新加载配置
        loadConfigurations()

        // 重新加载财富缓存
        loadWealthCache()
    }

    /**
     * 注册事件监听器
     */
    private fun registerEventListeners() {
        // 破坏方块掉落价值资源时，更新系统总价值量
        subscribeEvent<BlockDropItemEvent> {
            priority(EventPriority.MONITOR)
            handler { event ->
                if (!enableWealthTracking || event.isCancelled) return@handler
                if (event.player.gameMode != GameMode.SURVIVAL) return@handler
                if (excludeWorlds.contains(event.block.world.name)) return@handler
                if (!ValueResourceData.valueResourceBlockItemMap.containsKey(event.block.type)) return@handler
                if (event.player.hasPermission("dynamiceconomy.statics.bypass")) return@handler

                val sourceDropItem = event.block.type
                val targetDropItem = ValueResourceData.valueResourceBlockItemMap[sourceDropItem]!!
                var deltaWealth = 0.0

                event.items.forEach { entityItem ->
                    val itemStack = entityItem.itemStack
                    if (itemStack.type != sourceDropItem && itemStack.type != targetDropItem) return@forEach

                    // 更新资源统计
                    updateResourceCount(sourceDropItem, itemStack.amount.toLong())

                    // 计算财富增量
                    deltaWealth += ValueResourceData.valueResourceValueMap[targetDropItem]!! * itemStack.amount
                }

                // 增加系统总财富
                updateTotalWealth(BigDecimal.valueOf(deltaWealth))
            }
        }

        // 玩家放置价值资源时，更新系统总价值量
        subscribeEvent<BlockPlaceEvent> {
            priority(EventPriority.MONITOR)
            handler { event ->
                if (!enableWealthTracking || event.isCancelled) return@handler
                if (!event.canBuild()) return@handler
                if (event.player.gameMode != GameMode.SURVIVAL) return@handler
                if (excludeWorlds.contains(event.block.world.name)) return@handler
                if (!ValueResourceData.valueResourceBlockItemMap.containsKey(event.blockPlaced.type)) return@handler
                if (event.player.hasPermission("dynamiceconomy.statics.bypass")) return@handler

                val type = event.blockPlaced.type
                // 减少资源统计
                updateResourceCount(type, -1)

                // 减少系统总财富
                val value = ValueResourceData.valueResourceValueMap[type]!!
                updateTotalWealth(BigDecimal.valueOf(-value))
            }
        }

        // 物品被清理时，更新系统总价值量
        subscribeEvent<ItemDespawnEvent> {
            priority(EventPriority.MONITOR)
            handler { event ->
                if (!enableWealthTracking || event.isCancelled) return@handler
                if (excludeWorlds.contains(event.entity.world.name)) return@handler

                val itemStack = event.entity.itemStack
                val type = when {
                    ValueResourceData.valueResourceItemBlockMap.containsKey(itemStack.type) -> {
                        ValueResourceData.valueResourceItemBlockMap[itemStack.type]!!
                    }
                    ValueResourceData.valueResourceBlockItemMap.containsKey(itemStack.type) -> {
                        itemStack.type
                    }
                    else -> return@handler
                }

                val value = ValueResourceData.valueResourceValueMap[type]!!
                val amount = itemStack.amount.toLong()

                // 减少系统总财富
                updateTotalWealth(BigDecimal.valueOf(-value * amount))

                // 减少资源统计
                updateResourceCount(type, -amount)
            }
        }
    }

    /**
     * 启动定时任务
     */
    private fun startScheduledTasks() {
        // 定时更新财富统计
        runAsyncRepeating(0L, wealthUpdateInterval * 20L) {
            if (enableWealthTracking) {
                calculateTotalWealth()
            }
        }
    }

    /**
     * 加载配置
     */
    private fun loadConfigurations() {
        val config = plugin.getPluginConfig()
        enableWealthTracking = config.economySettings.enableWealthTracking
        wealthUpdateInterval = config.economySettings.wealthUpdateInterval
        excludeWorlds = config.excludeWorlds
    }

    /**
     * 加载财富缓存
     */
    private fun loadWealthCache() {
        plugin.configManager.touchWithMerge("economy-cache.yml", "economy-cache-template.yml", createBackup = false)
        val economyCache = plugin.configManager.parse<EconomyCache>("economy-cache.yml")
        wealthCache = economyCache.wealth
    }

    /**
     * 保存财富缓存
     */
    private fun saveWealthCache() {
        plugin.configManager.touchWithMerge("economy-cache.yml", "economy-cache-template.yml", createBackup = false)
        val economyCache = plugin.configManager.parse<EconomyCache>("economy-cache.yml")
        plugin.configManager.save(economyCache.copy(wealth = wealthCache), "economy-cache.yml")
    }

    /**
     * 更新资源数量
     */
    private fun updateResourceCount(material: Material, delta: Long) {
        val currentCount = wealthCache.valuedResourceCount.getOrDefault(material, 0L)
        val newCount = (currentCount + delta).coerceAtLeast(0L)

        if (newCount == 0L) {
            wealthCache.valuedResourceCount.remove(material)
        } else {
            wealthCache.valuedResourceCount[material] = newCount
        }

        // 异步保存缓存
        runAsync {
            saveWealthCache()
        }
    }

    /**
     * 更新总财富
     */
    private fun updateTotalWealth(delta: BigDecimal) {
        wealthCache.total = wealthCache.total.add(delta)

        // 异步保存缓存
        runAsync {
            saveWealthCache()
        }
    }

    /**
     * 计算总财富
     */
    fun calculateTotalWealth(): BigDecimal {
        try {
            var totalWealth = BigDecimal.ZERO

            // 计算所有价值资源的总价值
            wealthCache.valuedResourceCount.forEach { (material, count) ->
                val value = ValueResourceData.valueResourceValueMap[material] ?: 0.0
                totalWealth = totalWealth.add(BigDecimal.valueOf(value * count))
            }

            // 遍历所有在线世界，统计未被挖掘的价值资源
            Bukkit.getWorlds().forEach { world ->
                if (!excludeWorlds.contains(world.name)) {
                    // 这里可以添加世界扫描逻辑，但考虑到性能，暂时跳过
                    // 实际实现中可能需要使用区块加载事件来逐步统计
                }
            }

            wealthCache.total = totalWealth
            wealthCache.lastUpdate = System.currentTimeMillis()

            // 异步保存缓存
            runAsync { _ ->
                saveWealthCache()
            }

            logger.info(LanguageKeys.Log.Info.WEALTH_CALCULATED, totalWealth.toString())
            return totalWealth
        } catch (e: Exception) {
            logger.error(LanguageKeys.Log.Error.WEALTH_CALCULATION_ERROR, e)
            return wealthCache.total
        }
    }

    /**
     * 获取总财富
     */
    fun getTotalWealth(): BigDecimal = wealthCache.total

    /**
     * 获取资源统计
     */
    fun getResourceCount(): Map<Material, Long> = wealthCache.valuedResourceCount.toMap()

    /**
     * 获取最后更新时间
     */
    fun getLastUpdateTime(): Long = wealthCache.lastUpdate

    /**
     * 手动触发财富计算
     */
    fun forceUpdateWealth(): BigDecimal {
        return calculateTotalWealth()
    }
}
