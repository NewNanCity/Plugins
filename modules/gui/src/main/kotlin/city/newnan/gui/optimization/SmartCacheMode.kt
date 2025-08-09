package city.newnan.gui.optimization

/**
 * 渲染优化级别
 * 
 * 定义不同的渲染优化策略，平衡性能和实时性
 */
enum class SmartCacheMode(
    val displayName: String,
    val description: String,
    val cacheTtlMs: Long,
    val maxCacheSize: Int,
    val enableAsyncRendering: Boolean
) {
    
    /**
     * 保守模式
     * - 最小缓存，确保实时性
     * - 适合需要频繁更新的GUI
     */
    CONSERVATIVE(
        displayName = "保守模式",
        description = "最小缓存，确保实时性",
        cacheTtlMs = 60_000L,      // 1分钟
        maxCacheSize = 50,
        enableAsyncRendering = false
    ),
    
    /**
     * 平衡模式
     * - 平衡性能和实时性
     * - 适合大多数GUI场景
     */
    BALANCED(
        displayName = "平衡模式", 
        description = "平衡性能和实时性",
        cacheTtlMs = 300_000L,     // 5分钟
        maxCacheSize = 100,
        enableAsyncRendering = true
    ),
    
    /**
     * 激进模式
     * - 最大化性能，可能影响实时性
     * - 适合静态或更新频率低的GUI
     */
    AGGRESSIVE(
        displayName = "激进模式",
        description = "最大化性能，可能影响实时性", 
        cacheTtlMs = 600_000L,     // 10分钟
        maxCacheSize = 200,
        enableAsyncRendering = true
    );
    
    /**
     * 获取推荐的缓存配置
     */
    fun getCacheConfig(): CacheConfig {
        return CacheConfig(
            maxSize = maxCacheSize,
            defaultTtlMs = cacheTtlMs,
            cleanupIntervalMs = cacheTtlMs / 10 // 缓存TTL的1/10作为清理间隔
        )
    }
    
    /**
     * 是否应该使用异步渲染
     */
    fun shouldUseAsyncRendering(): Boolean = enableAsyncRendering
    
    /**
     * 获取渲染延迟阈值（毫秒）
     * 超过此阈值的渲染操作应该考虑异步执行
     */
    fun getRenderDelayThreshold(): Long {
        return when (this) {
            CONSERVATIVE -> 1L      // 1ms
            BALANCED -> 5L          // 5ms
            AGGRESSIVE -> 10L       // 10ms
        }
    }
    
    /**
     * 获取批量渲染的最大批次大小
     */
    fun getBatchRenderSize(): Int {
        return when (this) {
            CONSERVATIVE -> 5
            BALANCED -> 10
            AGGRESSIVE -> 20
        }
    }
    
    companion object {
        /**
         * 根据服务器性能自动选择优化级别
         */
        fun autoDetect(): SmartCacheMode {
            val runtime = Runtime.getRuntime()
            val maxMemory = runtime.maxMemory()
            val availableProcessors = runtime.availableProcessors()
            
            // 基于内存和CPU核心数判断
            return when {
                maxMemory > 4L * 1024 * 1024 * 1024 && availableProcessors >= 4 -> AGGRESSIVE
                maxMemory > 2L * 1024 * 1024 * 1024 && availableProcessors >= 2 -> BALANCED
                else -> CONSERVATIVE
            }
        }
        
        /**
         * 根据在线玩家数量推荐优化级别
         */
        fun recommendForPlayerCount(playerCount: Int): SmartCacheMode {
            return when {
                playerCount > 50 -> AGGRESSIVE
                playerCount > 20 -> BALANCED
                else -> CONSERVATIVE
            }
        }
    }
}

/**
 * 缓存配置
 */
data class CacheConfig(
    val maxSize: Int,
    val defaultTtlMs: Long,
    val cleanupIntervalMs: Long
)
