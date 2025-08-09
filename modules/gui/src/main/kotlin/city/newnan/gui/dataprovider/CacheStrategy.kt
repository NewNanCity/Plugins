package city.newnan.gui.dataprovider

/**
 * 缓存策略枚举
 */
enum class CacheStrategy {
    NO_CACHE,           // 不缓存
    CURRENT_PAGE_ONLY,  // 只缓存当前页（适合有限分页）
    MULTI_PAGE,         // 多页缓存（适合无限分页）
    AGGRESSIVE          // 激进缓存（适合小数据集）
}