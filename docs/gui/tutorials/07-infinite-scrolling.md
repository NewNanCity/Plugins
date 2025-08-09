# PaginatedComponent 无限滚动指南

## 概述

`PaginatedComponent` 现在支持无限滚动功能，这是一个强大的特性，特别适用于：

- **大型数据集**：数据量巨大或未知的场景
- **API数据**：需要分页从远程服务器获取数据
- **实时数据**：持续更新的数据流
- **性能优化**：避免一次性加载大量数据

## 核心概念

### 模式自动识别

无限滚动模式通过 `DataProvider.getSize()` 的返回值自动识别：

```kotlin
// 有限分页：getSize() >= 0
val finiteProvider = DataProviders.fromList(myList) // getSize() 返回 myList.size

// 无限分页：getSize() < 0
val infiniteProvider = DataProviders.infinite { offset, limit ->
    // 数据获取逻辑
} // getSize() 返回 -1
```

### 关键特性

1. **懒加载**：只在需要时加载数据
2. **多页缓存**：自动缓存多个页面以提升性能
3. **加载状态**：显示加载中和错误状态
4. **智能预加载**：自动预加载相邻页面

## 创建无限滚动数据提供器

### 1. 基础无限分页

```kotlin
val infiniteProvider = DataProviders.infinite(
    itemProvider = { offset, limit ->
        // 根据 offset 和 limit 获取数据
        generateSequence(offset) { it + 1 }
            .take(limit)
            .map { "Item #$it" }
            .toList()
    },
    hasMoreProvider = { offset ->
        // 判断是否还有更多数据
        offset < 1000  // 例如：最多1000条数据
    }
)
```

### 2. API风格的无限分页

```kotlin
val apiProvider = DataProviders.infiniteApi { page, pageSize ->
    // 调用外部API，参数是页码（从0开始）和页面大小
    externalApi.getUsers(page + 1, pageSize) // API页码从1开始
}
```

### 3. 数据库无限分页

```kotlin
class DatabaseInfiniteProvider(
    private val database: MyDatabase
) : AsyncDataProvider<User> {

    override fun getSize(callback: DataProviderCallback<Int>) {
        // 无限模式返回 -1
        callback(Result.success(-1))
    }

    override fun fetchItems(offset: Int, limit: Int, callback: DataProviderCallback<List<User>>) {
        // 在异步线程中执行数据库查询
        runAsync {
            try {
                val users = database.getUsers(offset, limit)
                callback(Result.success(users))
            } catch (e: Exception) {
                callback(Result.failure(e))
            }
        }
    }

    override fun canFetchMore(offset: Int, callback: DataProviderCallback<Boolean>) {
        runAsync {
            try {
                val hasMore = database.hasMoreUsers(offset)
                callback(Result.success(hasMore))
            } catch (e: Exception) {
                callback(Result.success(false))
            }
        }
    }

    override fun getCacheStrategy(): CacheStrategy {
        return CacheStrategy.MULTI_PAGE
    }
}
```

## 在GUI中使用

### 基础无限滚动示例

```kotlin
paginatedComponent<String>(startX = 1, startY = 1, width = 7, height = 4) {
    // 设置无限分页数据提供器
    setDataProvider(infiniteProvider)

    render { context ->
        ItemUtil.create(Material.PAPER) {
            name("&e${context.item}")
            lore(
                "&7页面: ${context.pageIndex + 1}",
                "&7索引: ${context.index}",
                "&7全局索引: ${context.globalIndex}",
                "&7模式: ${if (context.isInfiniteMode) "&b无限滚动" else "&e有限分页"}",
                "&7加载状态: ${if (context.isLoading) "&e加载中..." else "&a已加载"}"
            )
        }
    }

    // 加载中状态渲染
    renderLoadingSlot { context ->
        ItemUtil.create(Material.CLOCK) {
            name("&e加载中...")
            lore("&7正在获取数据，请稍候")
        }
    }

    // 空槽位渲染
    renderEmptySlot { context ->
        ItemUtil.create(Material.GRAY_STAINED_GLASS_PANE) {
            name(" ")
        }
    }

    onLeftClick { context, index, item ->
        if (item != null) {
            context.player.sendMessage("&a点击了: $item")
        }
    }
}
```

### 排行榜无限滚动示例

```kotlin
class RankingGUI(private val plugin: MyPlugin) {

    fun createPage(): Page = gui {
        title = "服务器排行榜"

        paginatedComponent<RankEntry>(startX = 0, startY = 1, width = 9, height = 4) {
            setDataProvider(createRankingProvider())

            render { context ->
                val entry = context.item ?: return@render null
                val rank = context.globalIndex + 1

                // 根据排名设置颜色
                val rankColor = when (rank) {
                    1 -> "&6"      // 金色
                    2 -> "&7"      // 银色
                    3 -> "&c"      // 铜色
                    in 4..10 -> "&e"    // 黄色
                    else -> "&f"   // 白色
                }

                ItemUtil.skull(entry.playerUUID) {
                    name("$rankColor#$rank ${entry.playerName}")
                    lore(
                        "&7积分: &e${formatScore(entry.score)}",
                        "&7排名: $rankColor#$rank",
                        if (rank <= 10) "&6⭐ 前十名" else "",
                        "&7最后更新: ${formatTime(entry.lastUpdate)}",
                        "&7",
                        "&a点击查看详细信息",
                        "&7右键复制玩家名"
                    )

                    // 前三名添加附魔效果
                    if (rank <= 3) {
                        enchant(Enchantment.LURE, rank)
                        flag(ItemFlag.HIDE_ENCHANTS)
                    }
                }
            }

            renderLoadingSlot { context ->
                ItemUtil.create(Material.CLOCK) {
                    name("&e正在加载排行榜...")
                    lore(
                        "&7正在从服务器获取数据",
                        "&7请稍候..."
                    )
                }
            }

            onLeftClick { context, index, entry ->
                if (entry != null) {
                    showPlayerProfile(context.player, entry)
                }
            }

            onRightClick { context, index, entry ->
                if (entry != null) {
                    context.player.sendMessage("&a已复制玩家名: ${entry.playerName}")
                    // 这里可以调用复制到剪贴板的API（如果有的话）
                }
            }

            // 页面变更监听
            pageChangeHandler = { context ->
                context.player.sendMessage("&a正在加载第 ${context.currentPage + 1} 页排行榜...")

                // 可以在这里添加音效
                context.player.playSound(
                    context.player.location,
                    Sound.UI_BUTTON_CLICK,
                    1.0f, 1.0f
                )
            }
        }

        // 导航控制
        addNavigationControls()

        // 刷新按钮
        singleSlotComponent(x = 4, y = 0) {
            render { context ->
                ItemUtil.create(Material.NETHER_STAR) {
                    name("&b刷新排行榜")
                    lore(
                        "&7点击刷新排行榜数据",
                        "&7上次更新: ${getLastUpdateTime()}"
                    )
                }
            }

            onLeftClick { context ->
                paginatedComponent.clearCache()
                paginatedComponent.refresh()
                context.player.sendMessage("&a排行榜已刷新！")
            }
        }
    }

    private fun createRankingProvider(): DataProvider<RankEntry> {
        return DataProviders.infiniteApi { page, pageSize ->
            plugin.rankingAPI.getTopPlayers(page + 1, pageSize)
        }
    }
}
```

## 高级功能

### 1. 预加载控制

```kotlin
paginatedComponent<Item>(startX = 0, startY = 1, width = 9, height = 4) {
    setDataProvider(infiniteProvider)

    // 页面变更时的预加载逻辑
    pageChangeHandler = { context ->
        // 预加载下一页
        preloadPage(context.currentPage + 1)

        // 如果不是第一页，也预加载上一页
        if (context.currentPage > 0) {
            preloadPage(context.currentPage - 1)
        }
    }
}
```

### 2. 缓存管理

```kotlin
paginatedComponent<Item>(startX = 0, startY = 1, width = 9, height = 4) {
    setDataProvider(infiniteProvider)

    // 检查缓存状态
    render { context ->
        val isCached = isPageCached(context.pageIndex)
        val isLoading = isPageLoading(context.pageIndex)

        // 根据状态渲染不同的内容
        when {
            isLoading -> createLoadingItem()
            isCached -> createCachedItem(context.item!!)
            else -> createNormalItem(context.item!!)
        }
    }
}

// 定期清理缓存
Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, {
    paginatedComponent.clearCache()
}, 0L, 20L * 60 * 5) // 每5分钟清理一次
```

### 3. 错误处理

```kotlin
class RobustInfiniteProvider<T>(
    private val apiClient: ApiClient<T>
) : AsyncDataProvider<T> {

    private var consecutiveErrors = 0
    private val maxErrors = 3

    override fun getSize(callback: DataProviderCallback<Int>) {
        callback(Result.success(-1))
    }

    override fun fetchItems(offset: Int, limit: Int, callback: DataProviderCallback<List<T>>) {
        runAsync {
            try {
                // 异步执行API调用
                val result = apiClient.getData(offset, limit)
                consecutiveErrors = 0 // 重置错误计数
                callback(Result.success(result))
            } catch (e: Exception) {
                consecutiveErrors++

                when (e) {
                    is java.net.SocketTimeoutException -> {
                        plugin.logger.warn("API request timeout for offset $offset")
                    }
                    is java.net.ConnectException -> {
                        plugin.logger.warn("Failed to connect to API")
                    }
                    else -> {
                        plugin.logger.error("Unexpected error fetching data", e)
                    }
                }

                // 如果连续错误太多，返回错误指示
                if (consecutiveErrors >= maxErrors) {
                    callback(Result.success(listOf(createErrorItem(e))))
                } else {
                    callback(Result.success(emptyList()))
                }
            }
        }
    }

    override fun canFetchMore(offset: Int, callback: DataProviderCallback<Boolean>) {
        // 如果连续错误太多，停止尝试获取更多数据
        callback(Result.success(consecutiveErrors < maxErrors))
    }
}
```

## 实际应用场景

### 1. 服务器日志查看器

```kotlin
// 无限滚动查看服务器日志
val logProvider = DataProviders.infinite(
    itemProvider = { offset, limit ->
        logManager.getLogs(offset, limit)
    },
    hasMoreProvider = { offset ->
        logManager.hasMoreLogs(offset)
    }
)

paginatedComponent<LogEntry>(startX = 0, startY = 1, width = 9, height = 4) {
    setDataProvider(logProvider)

    render { context ->
        val log = context.item ?: return@render null
        val levelColor = when (log.level) {
            "ERROR" -> "&c"
            "WARN" -> "&e"
            "INFO" -> "&a"
            else -> "&7"
        }

        ItemUtil.create(Material.PAPER) {
            name("$levelColor[${log.level}] ${log.timestamp}")
            lore(
                "&7消息: &f${log.message}",
                "&7来源: &f${log.source}",
                "&7线程: &f${log.thread}"
            )
        }
    }
}
```

### 2. 聊天记录查看器

```kotlin
// 查看玩家聊天记录
class ChatHistoryGUI(private val targetPlayer: String) {

    fun createPage(): Page = gui {
        title = "$targetPlayer 的聊天记录"

        paginatedComponent<ChatMessage>(startX = 0, startY = 1, width = 9, height = 4) {
            setDataProvider(DataProviders.infiniteApi { page, pageSize ->
                chatManager.getChatHistory(targetPlayer, page, pageSize)
            })

            render { context ->
                val messageval item = context.item ?: return@render null
                ItemUtil.create(Material.WRITABLE_BOOK) {
                    name("&e${formatTime(message.timestamp)}")
                    lore(
                        "&7频道: &f${message.channel}",
                        "&7消息: &f${message.content}",
                        "&7服务器: &f${message.server}"
                    )
                }
            }

            renderLoadingSlot { context ->
                ItemUtil.create(Material.CLOCK) {
                    name("&e加载聊天记录...")
                }
            }
        }
    }
}
```

## 性能优化建议

### 1. 数据获取优化

```kotlin
// 使用连接池和缓存
class OptimizedApiProvider<T>(
    private val apiClient: ApiClient<T>
) : DataProvider<T> {

    private val requestCache = CacheBuilder.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build<String, List<T>>()

    override fun fetchItems(offset: Int, limit: Int, callback: DataProviderCallback<List<T>>) {
        val cacheKey = "$offset:$limit"

        try {
            val cachedResult = requestCache.getIfPresent(cacheKey)
            if (cachedResult != null) {
                callback(Result.success(cachedResult))
                return
            }

            // 异步获取数据
            runAsync {
                try {
                    val data = apiClient.getData(offset, limit)
                    requestCache.put(cacheKey, data)
                    callback(Result.success(data))
                } catch (e: Exception) {
                    callback(Result.failure(e))
                }
            }
        } catch (e: Exception) {
            callback(Result.failure(e))
        }
    }
}
```

### 2. 内存管理

```kotlin
// 限制缓存大小
paginatedComponent.apply {
    // 定期清理旧缓存
    Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, {
        // 只保留当前页面前后各2页的缓存
        val currentPage = getCurrentPage()
        val keepPages = (currentPage - 2)..(currentPage + 2)

        // 清理不需要的缓存页面
        clearCacheExcept(keepPages.toSet())
    }, 0L, 20L * 30) // 每30秒清理一次
}
```

### 3. 用户体验优化

```kotlin
// 添加加载进度指示
renderLoadingSlot { context ->
    val dots = ".".repeat((System.currentTimeMillis() / 500 % 4).toInt())

    ItemUtil.create(Material.CLOCK) {
        name("&e加载中$dots")
        lore(
            "&7正在获取数据",
            "&7页面: ${context.pageIndex + 1}",
            "&7请稍候..."
        )
    }
}
```

通过这些示例和最佳实践，您可以创建出流畅、高效的无限滚动界面，为玩家提供优秀的用户体验。
