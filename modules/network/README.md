# Network 模块

现代化的 HTTP 客户端模块，基于 Ktor Client 构建，提供协程原生支持和类型安全的 DSL。

## 特性

- 🚀 **协程原生**: 基于 Kotlin 协程，支持异步非阻塞操作
- 🔧 **类型安全**: 提供类型安全的 DSL 和扩展函数
- 🔌 **插件化**: 支持日志、序列化、认证等插件
- 🔄 **多引擎**: 支持 OkHttp、Java、Apache 等多种 HTTP 引擎
- 📦 **序列化**: 支持 kotlinx.serialization 和 Jackson
- 🔐 **认证**: 支持 Basic、Bearer、API Key 等认证方式
- 🔧 **生命周期**: 自动集成到 BasePlugin 生命周期管理
- 🛑 **可终止**: 支持 TerminableConsumer，自动管理请求生命周期
- 📊 **监控**: 提供网络状态监控和延迟测量
- 🔄 **重试**: 内置请求重试机制
- 📁 **批量**: 支持批量请求处理

## 快速开始

提供两种API风格：**Kotlin DSL**（推荐）和**Java兼容工厂函数**：

### Kotlin DSL风格（推荐）

```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // HTTP请求（Kotlin DSL）
        tasks {
            coroutine {
                // GET请求
                val response = httpGet("https://api.example.com/data") {
                    header("Authorization", "Bearer token")
                    parameter("limit", "10")
                }

                // POST请求
                val postResponse = httpPost("https://api.example.com/users") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"name": "John", "age": 30}""")
                }

                // 批量请求
                network {
                    batch {
                        get("https://api.example.com/users")
                        get("https://api.example.com/posts")
                        post("https://api.example.com/analytics") {
                            setBody("""{"event": "page_view"}""")
                        }
                    }
                }

                // 网络监控
                networkMonitor {
                    url("https://api.example.com/health")
                    url("https://cdn.example.com/ping")
                    interval(30) // 30秒检查一次
                    onStatusChange { url, isAvailable ->
                        if (isAvailable) {
                            logger.info("服务 $url 可用")
                        } else {
                            logger.warning("服务 $url 不可用")
                        }
                    }
                }

                // 延迟测量
                val latency = networkLatency("https://api.example.com/ping", 5)
                logger.info("网络延迟: ${latency.average}ms")
            }
        }
    }
}
```

### Java兼容工厂函数

```kotlin
import city.newnan.network.*
import kotlinx.coroutines.runBlocking

class JavaCompatiblePlugin : BasePlugin() {
    override fun onPluginEnable() {
        runBlocking {
            // 使用默认 HTTP 客户端
            val client = getHttpClient()
            val response = client.get("https://api.example.com/data")
            logger.info("响应状态: ${response.status}")

            // 自定义客户端配置
            val customClient = createHttpClient("custom") {
                timeout(60000)
                retries(5)
                userAgent("MyPlugin/1.0")
            }
        }
    }
}
```

### 类型安全的请求

```kotlin
import kotlinx.serialization.Serializable

@Serializable
data class User(val id: Int, val name: String, val email: String)

suspend fun example() {
    val client = getHttpClient()

    // GET 请求并自动反序列化
    val user: User = client.getTyped("https://api.example.com/users/1")
    println("用户: ${user.name}")

    // POST 请求
    val newUser = User(0, "新用户", "new@example.com")
    val created: User = client.postTyped("https://api.example.com/users") {
        jsonBody(newUser)
    }
}
```

### 配置自定义客户端

```kotlin
suspend fun example() {
    // 创建配置了认证的客户端
    val apiClient = createApiKeyHttpClient("api", "your-api-key")

    val data = apiClient.getTyped<List<String>>("https://api.example.com/protected")
    println("获取到 ${data.size} 条数据")
}
```

### 可终止的网络请求

```kotlin
class MyService : TerminableConsumer {
    private val terminableRegistry = CompositeTerminable.create()
    private val scope = CoroutineScope(Dispatchers.Default)

    fun fetchUserData(userId: Int) {
        // 创建可终止的 HTTP 请求
        terminableGetTyped<User>(
            "https://api.example.com/users/$userId",
            scope
        ) { result ->
            result.onSuccess { user ->
                println("获取用户成功: ${user.name}")
            }.onFailure { error ->
                println("获取用户失败: ${error.message}")
            }
        }
    }

    override fun <T : AutoCloseable> bind(terminable: T): T {
        return terminableRegistry.bind(terminable)
    }

    fun close() {
        scope.cancel()
        terminableRegistry.close()
    }
}
```

## 认证支持

### API Key 认证

```kotlin
val client = createApiKeyHttpClient("github", "ghp_xxxxxxxxxxxx", "Authorization")
```

### Bearer Token 认证

```kotlin
val client = createBearerHttpClient("oauth", "access_token_here")
```

### Basic 认证

```kotlin
val client = createBasicAuthHttpClient("basic", "username", "password")
```

## 可终止网络请求

Network 模块完全支持 TerminableConsumer 模式，确保所有网络请求都能被正确管理和清理。

### 基本用法

```kotlin
class ApiService : TerminableConsumer {
    private val terminableRegistry = CompositeTerminable.create()
    private val scope = CoroutineScope(Dispatchers.Default)

    fun fetchData() {
        // 可终止的 GET 请求
        terminableGet("https://api.example.com/data", scope) { result ->
            result.onSuccess { response ->
                println("请求成功: ${response.status}")
            }.onFailure { error ->
                println("请求失败: ${error.message}")
            }
        }
    }

    override fun <T : AutoCloseable> bind(terminable: T): T {
        return terminableRegistry.bind(terminable)
    }

    fun close() {
        scope.cancel()
        terminableRegistry.close()
    }
}
```

### 认证客户端

```kotlin
class AuthenticatedService : TerminableConsumer {
    private val terminableRegistry = CompositeTerminable.create()

    fun init() {
        // 创建可终止的认证客户端
        val apiClient = createTerminableApiKeyHttpClient("api", "your-api-key")

        // 客户端会自动绑定到生命周期
    }

    override fun <T : AutoCloseable> bind(terminable: T): T {
        return terminableRegistry.bind(terminable)
    }
}
```

### 网络监控

```kotlin
fun startNetworkMonitoring(scope: CoroutineScope) {
    // 可终止的网络连接检查
    terminableNetworkCheck(scope = scope) { isConnected ->
        println("网络状态: ${if (isConnected) "正常" else "异常"}")
    }
}
```

### 文件下载

```kotlin
fun downloadFile(scope: CoroutineScope) {
    terminableDownload(
        url = "https://example.com/file.zip",
        maxSize = 50 * 1024 * 1024, // 50MB
        scope = scope
    ) { result ->
        result.onSuccess { data ->
            println("下载完成: ${data.size} bytes")
        }.onFailure { error ->
            println("下载失败: ${error.message}")
        }
    }
}
```

## 高级功能

### 请求重试

```kotlin
httpRequestWithRetry(
    maxRetries = 3,
    delayMillis = 1000
) { client ->
    client.getTyped<String>("https://unreliable-api.com/data")
} { result ->
    result.onSuccess { data ->
        pluginLogger.info("获取数据成功: $data")
    }.onFailure { error ->
        pluginLogger.warning("获取数据失败: ${error.message}")
    }
}
```

### 批量请求

```kotlin
val requests = listOf(
    { client: HttpClient -> client.getTyped<User>("https://api.example.com/users/1") },
    { client: HttpClient -> client.getTyped<User>("https://api.example.com/users/2") },
    { client: HttpClient -> client.getTyped<User>("https://api.example.com/users/3") }
)

batchHttpRequests(requests) { users ->
    pluginLogger.info("获取到 ${users.size} 个用户")
}
```

### 文件下载

```kotlin
downloadFile(
    url = "https://example.com/large-file.zip",
    maxSize = 50 * 1024 * 1024 // 50MB
) { result ->
    result.onSuccess { data ->
        // 保存文件
        File("downloads/file.zip").writeBytes(data)
    }.onFailure { error ->
        pluginLogger.warning("下载失败: ${error.message}")
    }
}
```

### 网络监控

```kotlin
val monitor = createNetworkMonitor()

monitor.startMonitoring(
    urls = listOf(
        "https://api.example.com/health",
        "https://cdn.example.com/ping"
    ),
    intervalSeconds = 30
) { url, isAvailable ->
    if (isAvailable) {
        pluginLogger.info("服务 $url 可用")
    } else {
        pluginLogger.warning("服务 $url 不可用")
    }
}
```

### 延迟测量

```kotlin
measureNetworkLatency(
    url = "https://api.example.com/ping",
    attempts = 5
) { latency ->
    pluginLogger.info("""
        网络延迟统计:
        - 平均: ${latency.average}ms
        - 最小: ${latency.min}ms
        - 最大: ${latency.max}ms
        - 成功率: ${latency.successRate * 100}%
        - 健康状态: ${if (latency.isHealthy) "良好" else "异常"}
    """.trimIndent())
}
```

## DSL 扩展

### 请求构建

```kotlin
httpClient.post("https://api.example.com/data") {
    // 设置 JSON 请求体
    jsonBody(mapOf("key" to "value"))

    // 设置查询参数
    queryParams("page" to "1", "size" to "10")

    // 设置认证头
    bearerAuth("token")

    // 设置自定义头
    header("X-Custom", "value")
}
```

### 表单数据

```kotlin
httpClient.post("https://api.example.com/form") {
    formData(
        "username" to "user",
        "password" to "pass"
    )
}
```

### 流式下载

```kotlin
httpClient.getStream("https://example.com/large-file").collect { chunk ->
    // 处理数据块
    processChunk(chunk)
}
```

## 配置选项

### HttpClientConfig

```kotlin
HttpClientConfig(
    expectSuccess = true,                    // 期望成功响应
    requestTimeout = Duration.ofSeconds(30), // 请求超时
    connectTimeout = Duration.ofSeconds(10), // 连接超时
    socketTimeout = Duration.ofSeconds(30),  // Socket 超时
    serialization = SerializationType.KOTLINX_JSON, // 序列化类型
    prettyPrint = false,                     // JSON 格式化
    lenient = true,                          // 宽松解析
    ignoreUnknownKeys = true,                // 忽略未知字段
    encodeDefaults = false,                  // 编码默认值
    enableLogging = true,                    // 启用日志
    logLevel = LogLevel.INFO,                // 日志级别
    enableResources = true,                  // 启用资源支持
    userAgent = "NewNanPlugins-HttpClient/1.0", // User-Agent
    authConfig = null,                       // 认证配置
    defaultHeaders = emptyMap()              // 默认请求头
)
```

## 最佳实践

1. **使用类型安全的扩展函数**: 优先使用 `getTyped`、`postTyped` 等扩展函数
2. **合理配置超时**: 根据 API 特性设置合适的超时时间
3. **启用日志**: 开发阶段启用详细日志，生产环境使用 INFO 级别
4. **使用命名客户端**: 为不同的 API 创建专用的命名客户端
5. **处理异常**: 始终处理网络请求可能的异常
6. **资源清理**: 依赖 BasePlugin 的自动清理机制，无需手动关闭客户端
7. **批量操作**: 对于多个相关请求，使用批量请求提高效率
8. **监控重要服务**: 对关键 API 启用网络监控

## 依赖

- Ktor Client Core
- Ktor Client OkHttp Engine
- Ktor Client Content Negotiation
- Ktor Client Logging
- Ktor Client Auth
- kotlinx.serialization
- Jackson (兼容 config 模块)

## 版本要求

- Kotlin 2.2.0+
- Java 17+
- Ktor 2.3.12+
