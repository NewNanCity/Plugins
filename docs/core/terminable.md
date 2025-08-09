# Terminable 资源管理系统

Core 模块的 Terminable 体系提供了完整的自动资源管理解决方案，确保所有资源在插件禁用时自动清理，防止内存泄漏。

## 🎯 核心概念

### 什么是 Terminable？

Terminable 是一个可终止的资源接口，所有需要清理的资源都应该实现这个接口：

```kotlin
interface Terminable : AutoCloseable {
    override fun close()
}
```

### 什么是 TerminableConsumer？

TerminableConsumer 是资源消费者，负责管理多个 Terminable 资源：

```kotlin
interface TerminableConsumer {
    fun <T : AutoCloseable> bind(terminable: T): T
}
```

## 🔄 工作原理

### 自动资源绑定

```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 1. 创建资源
        val database = DatabaseManager()
        val cache = CacheManager()
        val network = NetworkClient()
        
        // 2. 绑定到插件生命周期
        bind(database)  // 插件禁用时自动调用 database.close()
        bind(cache)     // 插件禁用时自动调用 cache.close()
        bind(network)   // 插件禁用时自动调用 network.close()
        
        // 3. 事件和任务也会自动绑定
        subscribeEvent<PlayerJoinEvent> { /* 自动注销 */ }
        runSyncRepeating(0L, 20L) { /* 自动取消 */ }
    }
    
    // 插件禁用时，所有绑定的资源按 LIFO 顺序自动清理
}
```

### 资源清理顺序

资源按照 **LIFO（后进先出）** 顺序清理，确保依赖关系正确：

```kotlin
bind(database)    // 第一个绑定
bind(cache)       // 第二个绑定（依赖 database）
bind(service)     // 第三个绑定（依赖 cache）

// 清理顺序：service -> cache -> database
```

## 🛠️ 实现自定义 Terminable

### 基础实现

```kotlin
class DatabaseManager : Terminable {
    private var connection: Connection? = null
    private var isShutdown = false
    
    init {
        connection = DriverManager.getConnection(url, user, password)
    }
    
    fun executeQuery(sql: String): ResultSet {
        if (isShutdown) throw IllegalStateException("DatabaseManager is shutdown")
        return connection!!.createStatement().executeQuery(sql)
    }
    
    override fun close() {
        if (isShutdown) return
        isShutdown = true
        
        try {
            connection?.close()
            connection = null
        } catch (e: SQLException) {
            logger.error("关闭数据库连接失败", e)
        }
    }
}
```

### 复杂资源管理

```kotlin
class NetworkManager : Terminable {
    private val clients = ConcurrentHashMap<String, HttpClient>()
    private val executorService = Executors.newFixedThreadPool(4)
    private var isShutdown = false
    
    fun createClient(name: String): HttpClient {
        if (isShutdown) throw IllegalStateException("NetworkManager is shutdown")
        
        val client = HttpClient.newBuilder()
            .executor(executorService)
            .build()
        
        clients[name] = client
        return client
    }
    
    override fun close() {
        if (isShutdown) return
        isShutdown = true
        
        // 1. 关闭所有客户端
        clients.values.forEach { client ->
            try {
                // HttpClient 没有 close 方法，但我们可以清理引用
            } catch (e: Exception) {
                logger.error("关闭 HTTP 客户端失败", e)
            }
        }
        clients.clear()
        
        // 2. 关闭线程池
        executorService.shutdown()
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow()
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warning("线程池未能正常关闭")
                }
            }
        } catch (e: InterruptedException) {
            executorService.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }
}
```

## 📦 BaseModule 中的资源管理

### 模块级资源管理

```kotlin
class PlayerModule(moduleName: String, val plugin: MyPlugin) : BaseModule(moduleName, plugin) {
    
    override fun onInit() {
        // 创建模块专用资源
        val playerCache = PlayerCacheManager()
        val playerDatabase = PlayerDatabaseManager()
        
        // 绑定到模块（模块关闭时自动清理）
        bind(playerCache)
        bind(playerDatabase)
        
        // 事件绑定到模块
        subscribeEvent<PlayerJoinEvent> { event ->
            playerCache.loadPlayer(event.player)
        }
        
        // 任务绑定到模块
        runAsyncRepeating(0L, 20L * 60) {
            playerCache.cleanup()
        }
    }
    
    override fun onClose() {
        // 可选：自定义清理逻辑
        logger.info("PlayerModule 正在关闭...")
        super.onClose() // 调用父类清理
    }
}
```

### 嵌套模块管理

```kotlin
class ParentModule(moduleName: String, plugin: MyPlugin) : BaseModule(moduleName, plugin) {
    
    override fun onInit() {
        // 创建子模块（自动绑定到父模块）
        val childModule1 = ChildModule("Child1", this)
        val childModule2 = ChildModule("Child2", this)
        
        // 父模块关闭时，子模块自动关闭
    }
}

class ChildModule(moduleName: String, parentModule: BaseModule) : BaseModule(moduleName, parentModule) {
    
    override fun onInit() {
        // 子模块的资源管理
        val childResource = ChildResource()
        bind(childResource)
    }
}
```

## 🔧 高级用法

### 条件资源绑定

```kotlin
class ConditionalResourceManager(plugin: MyPlugin) : BaseModule("ConditionalResourceManager", plugin) {
    
    override fun onInit() {
        val config = plugin.getPluginConfig()
        
        // 根据配置条件性地创建资源
        if (config.enableDatabase) {
            val database = DatabaseManager()
            bind(database)
        }
        
        if (config.enableCache) {
            val cache = CacheManager()
            bind(cache)
        }
        
        if (config.enableNetworking) {
            val network = NetworkManager()
            bind(network)
        }
    }
}
```

### 资源依赖管理

```kotlin
class DependentResourceManager(plugin: MyPlugin) : BaseModule("DependentResourceManager", plugin) {
    
    override fun onInit() {
        // 1. 先创建基础资源
        val database = DatabaseManager()
        bind(database)
        
        // 2. 创建依赖基础资源的资源
        val userService = UserService(database)
        bind(userService)
        
        // 3. 创建依赖用户服务的资源
        val authService = AuthService(userService)
        bind(authService)
        
        // 清理顺序：authService -> userService -> database
    }
}
```

### 资源状态监控

```kotlin
class ResourceMonitor(plugin: MyPlugin) : BaseModule("ResourceMonitor", plugin) {
    
    override fun onInit() {
        // 监控资源使用情况
        runAsyncRepeating(0L, 20L * 60) { // 每分钟
            val stats = getResourceStats()
            
            logger.info("""
                资源监控报告:
                - 绑定资源数: ${stats.totalBound}
                - 活跃资源数: ${stats.activeCount}
                - 内存使用: ${stats.memoryUsage}MB
            """.trimIndent())
            
            // 检查资源泄漏
            if (stats.inactiveCount > 50) {
                logger.warning("检测到可能的资源泄漏: ${stats.inactiveCount} 个非活跃资源")
                cleanupInactiveResources()
            }
        }
    }
    
    private fun getResourceStats(): ResourceStats {
        // 实现资源统计逻辑
        return ResourceStats(
            totalBound = terminableRegistry.size(),
            activeCount = terminableRegistry.activeCount(),
            inactiveCount = terminableRegistry.inactiveCount(),
            memoryUsage = Runtime.getRuntime().let { 
                (it.totalMemory() - it.freeMemory()) / 1024 / 1024 
            }
        )
    }
}

data class ResourceStats(
    val totalBound: Int,
    val activeCount: Int,
    val inactiveCount: Int,
    val memoryUsage: Long
)
```

## 🛡️ 最佳实践

### 1. 防止重复关闭

```kotlin
class SafeResource : Terminable {
    private var isClosed = false
    
    override fun close() {
        if (isClosed) return
        isClosed = true
        
        // 执行清理逻辑
        performCleanup()
    }
    
    private fun performCleanup() {
        // 实际的清理代码
    }
}
```

### 2. 异常安全的清理

```kotlin
class ExceptionSafeResource : Terminable {
    override fun close() {
        val exceptions = mutableListOf<Exception>()
        
        // 尝试清理资源1
        try {
            resource1.close()
        } catch (e: Exception) {
            exceptions.add(e)
        }
        
        // 尝试清理资源2
        try {
            resource2.close()
        } catch (e: Exception) {
            exceptions.add(e)
        }
        
        // 如果有异常，记录但不抛出
        if (exceptions.isNotEmpty()) {
            logger.error("资源清理时发生异常: ${exceptions.size} 个错误")
            exceptions.forEach { e ->
                logger.error("清理异常", e)
            }
        }
    }
}
```

### 3. 资源使用检查

```kotlin
class CheckedResource : Terminable {
    private var isClosed = false
    
    fun doSomething() {
        checkNotClosed()
        // 执行操作
    }
    
    private fun checkNotClosed() {
        if (isClosed) {
            throw IllegalStateException("Resource has been closed")
        }
    }
    
    override fun close() {
        isClosed = true
        // 清理逻辑
    }
}
```

## ⚠️ 常见陷阱

### 1. 避免循环依赖

```kotlin
// ❌ 错误：循环依赖
class ServiceA(private val serviceB: ServiceB) : Terminable
class ServiceB(private val serviceA: ServiceA) : Terminable

// ✅ 正确：使用事件或回调解耦
class ServiceA : Terminable {
    fun onServiceBEvent(event: ServiceBEvent) {
        // 处理事件
    }
}

class ServiceB : Terminable {
    private val eventBus = EventBus()
    
    fun doSomething() {
        eventBus.post(ServiceBEvent())
    }
}
```

### 2. 避免在构造函数中绑定

```kotlin
// ❌ 错误：在构造函数中绑定
class BadModule(plugin: MyPlugin) : BaseModule("BadModule", plugin) {
    init {
        bind(SomeResource()) // 可能在模块完全初始化前绑定
    }
}

// ✅ 正确：在 onInit 中绑定
class GoodModule(plugin: MyPlugin) : BaseModule("GoodModule", plugin) {
    override fun onInit() {
        bind(SomeResource()) // 在适当的时机绑定
    }
}
```

---

**相关文档：** [📦 BaseModule](base-module.md) | [🔄 生命周期管理](lifecycle.md) | [💡 最佳实践](best-practices.md)
