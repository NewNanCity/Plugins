package city.newnan.gui.optimization

import org.bukkit.inventory.ItemStack
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * 智能缓存系统
 *
 * 提供高级的缓存功能：
 * - TTL（生存时间）支持
 * - LRU（最近最少使用）淘汰策略
 * - 自动清理过期缓存
 * - 缓存统计和监控
 */
class SmartCache<K, V>(
    private val maxSize: Int = 1000,
    private val defaultTtlMs: Long = 300_000, // 5分钟默认TTL
    private val cleanupIntervalMs: Long = 60_000 // 1分钟清理间隔
) {

    private val cache = ConcurrentHashMap<K, CacheEntry<V>>()
    private val accessOrder = ConcurrentHashMap<K, Long>()
    private val accessCounter = AtomicLong(0)
    private val stats = CacheStats()

    // 定时清理器
    private val cleanupExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "SmartCache-Cleanup").apply { isDaemon = true }
    }

    init {
        // 启动定时清理任务
        cleanupExecutor.scheduleAtFixedRate(
            ::cleanup,
            cleanupIntervalMs,
            cleanupIntervalMs,
            TimeUnit.MILLISECONDS
        )
    }

    /**
     * 获取缓存值
     */
    fun get(key: K): V? {
        val entry = cache[key]

        if (entry == null) {
            stats.recordMiss()
            return null
        }

        // 检查是否过期
        if (entry.isExpired()) {
            cache.remove(key)
            accessOrder.remove(key)
            stats.recordMiss()
            stats.recordExpiration()
            return null
        }

        // 更新访问时间
        accessOrder[key] = accessCounter.incrementAndGet()
        stats.recordHit()

        return entry.value
    }

    /**
     * 存储缓存值
     */
    fun put(key: K, value: V, ttlMs: Long = defaultTtlMs) {
        // 检查是否需要淘汰旧条目
        if (cache.size >= maxSize) {
            evictLeastRecentlyUsed()
        }

        val entry = CacheEntry(value, System.currentTimeMillis() + ttlMs)
        cache[key] = entry
        accessOrder[key] = accessCounter.incrementAndGet()

        stats.recordPut()
    }

    /**
     * 移除缓存值
     */
    fun remove(key: K): V? {
        val entry = cache.remove(key)
        accessOrder.remove(key)

        if (entry != null) {
            stats.recordRemoval()
            return entry.value
        }

        return null
    }

    /**
     * 清空缓存
     */
    fun clear() {
        val size = cache.size
        cache.clear()
        accessOrder.clear()
        stats.recordClear(size)
    }

    /**
     * 检查是否包含键
     */
    fun containsKey(key: K): Boolean {
        val entry = cache[key]
        return entry != null && !entry.isExpired()
    }

    /**
     * 获取缓存大小
     */
    fun size(): Int = cache.size

    /**
     * 检查是否为空
     */
    fun isEmpty(): Boolean = cache.isEmpty()

    /**
     * 获取所有有效的键
     */
    fun keys(): Set<K> {
        return cache.entries
            .filter { !it.value.isExpired() }
            .map { it.key }
            .toSet()
    }

    /**
     * 淘汰最近最少使用的条目
     */
    private fun evictLeastRecentlyUsed() {
        val lruKey = accessOrder.entries
            .minByOrNull { it.value }
            ?.key

        if (lruKey != null) {
            cache.remove(lruKey)
            accessOrder.remove(lruKey)
            stats.recordEviction()
        }
    }

    /**
     * 清理过期条目
     */
    private fun cleanup() {
        val currentTime = System.currentTimeMillis()
        val expiredKeys = mutableListOf<K>()

        cache.entries.forEach { (key, entry) ->
            if (entry.expirationTime <= currentTime) {
                expiredKeys.add(key)
            }
        }

        expiredKeys.forEach { key ->
            cache.remove(key)
            accessOrder.remove(key)
            stats.recordExpiration()
        }

        if (expiredKeys.isNotEmpty()) {
            stats.recordCleanup(expiredKeys.size)
        }
    }

    /**
     * 获取缓存统计信息
     */
    fun getStats(): CacheStats = stats

    /**
     * 重置统计信息
     */
    fun resetStats() {
        stats.reset()
    }

    /**
     * 关闭缓存
     */
    fun close() {
        cleanupExecutor.shutdown()
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            cleanupExecutor.shutdownNow()
            Thread.currentThread().interrupt()
        }
        clear()
    }

    /**
     * 缓存条目
     */
    private data class CacheEntry<V>(
        val value: V,
        val expirationTime: Long
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() > expirationTime
    }
}

/**
 * 缓存统计信息
 */
class CacheStats {
    private val hits = AtomicLong(0)
    private val misses = AtomicLong(0)
    private val puts = AtomicLong(0)
    private val removals = AtomicLong(0)
    private val evictions = AtomicLong(0)
    private val expirations = AtomicLong(0)
    private val cleanups = AtomicLong(0)
    private val clearedEntries = AtomicLong(0)

    fun recordHit() = hits.incrementAndGet()
    fun recordMiss() = misses.incrementAndGet()
    fun recordPut() = puts.incrementAndGet()
    fun recordRemoval() = removals.incrementAndGet()
    fun recordEviction() = evictions.incrementAndGet()
    fun recordExpiration() = expirations.incrementAndGet()
    fun recordCleanup(count: Int) {
        cleanups.incrementAndGet()
    }
    fun recordClear(count: Int) = clearedEntries.addAndGet(count.toLong())

    fun reset() {
        hits.set(0)
        misses.set(0)
        puts.set(0)
        removals.set(0)
        evictions.set(0)
        expirations.set(0)
        cleanups.set(0)
        clearedEntries.set(0)
    }

    // 统计信息访问器
    fun getHits(): Long = hits.get()
    fun getMisses(): Long = misses.get()
    fun getPuts(): Long = puts.get()
    fun getRemovals(): Long = removals.get()
    fun getEvictions(): Long = evictions.get()
    fun getExpirations(): Long = expirations.get()
    fun getCleanups(): Long = cleanups.get()
    fun getClearedEntries(): Long = clearedEntries.get()

    fun getHitRate(): Double {
        val total = hits.get() + misses.get()
        return if (total > 0) hits.get().toDouble() / total else 0.0
    }

    fun getMissRate(): Double = 1.0 - getHitRate()

    override fun toString(): String {
        return buildString {
            appendLine("=== 缓存统计 ===")
            appendLine("命中次数: ${getHits()}")
            appendLine("未命中次数: ${getMisses()}")
            appendLine("命中率: ${"%.2f".format(getHitRate() * 100)}%")
            appendLine("存储次数: ${getPuts()}")
            appendLine("移除次数: ${getRemovals()}")
            appendLine("淘汰次数: ${getEvictions()}")
            appendLine("过期次数: ${getExpirations()}")
            appendLine("清理次数: ${getCleanups()}")
            appendLine("清空条目数: ${getClearedEntries()}")
        }
    }
}

/**
 * 物品缓存 - 专门用于ItemStack的缓存
 */
class ItemStackCache(
    maxSize: Int = 500,
    private val defaultTtlMs: Long = 180_000 // 3分钟默认TTL
) {
    private val cache = SmartCache<String, ItemStack>(maxSize, defaultTtlMs)

    /**
     * 根据材质和数据生成缓存键
     */
    fun generateKey(
        material: String,
        displayName: String? = null,
        lore: List<String>? = null,
        amount: Int = 1,
        customData: Map<String, Any>? = null
    ): String {
        return buildString {
            append(material)
            append(":$amount")

            if (displayName != null) {
                append(":name:${displayName.hashCode()}")
            }

            if (!lore.isNullOrEmpty()) {
                append(":lore:${lore.hashCode()}")
            }

            if (!customData.isNullOrEmpty()) {
                append(":data:${customData.hashCode()}")
            }
        }
    }

    /**
     * 存储物品到缓存
     */
    fun putItem(
        key: String,
        item: ItemStack,
        ttlMs: Long = defaultTtlMs
    ) {
        // 克隆物品以避免外部修改影响缓存
        cache.put(key, item.clone(), ttlMs)
    }

    /**
     * 获取物品从缓存
     */
    fun getItem(key: String): ItemStack? {
        val cached = cache.get(key)
        // 返回克隆以避免缓存被外部修改
        return cached?.clone()
    }

    /**
     * 获取缓存统计信息
     */
    fun getStats(): CacheStats = cache.getStats()

    /**
     * 清空缓存
     */
    fun clear() = cache.clear()

    /**
     * 获取缓存大小
     */
    fun size(): Int = cache.size()
}
