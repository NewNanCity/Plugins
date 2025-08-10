# Network 模块基础概念

> 📋 **状态**: 文档规划中，内容正在完善

## 核心概念

### 网络管理器 (NetworkManager)

NetworkManager 是 Network 模块的核心组件，负责：
- HTTP/HTTPS 请求的发送和处理
- 连接池管理和优化
- 请求重试和错误处理
- 异步操作和回调管理
- 资源生命周期管理

### 请求构建器 (Request Builder)

使用流式 API 构建网络请求：

```kotlin
val request = networkManager.get("https://api.example.com/data")
    .header("Authorization", "Bearer $token")
    .header("Content-Type", "application/json")
    .timeout(30000)
    .retry(3)
    .build()
```

### 响应处理 (Response Handling)

网络响应包含完整的 HTTP 信息：

```kotlin
data class NetworkResponse(
    val statusCode: Int,
    val headers: Map<String, String>,
    val body: String,
    val contentType: String?,
    val contentLength: Long,
    val isSuccessful: Boolean
) {
    fun <T> parseJson(clazz: Class<T>): T {
        return JsonParser.parse(body, clazz)
    }
}
```

## 异步处理模式

### CompletableFuture 集成

Network 模块与 Java 的 CompletableFuture 深度集成：

```kotlin
// 链式异步处理
networkManager.get("https://api.example.com/user/123")
    .thenApply { response -> parseUser(response.body) }
    .thenCompose { user -> fetchUserPosts(user.id) }
    .thenAccept { posts -> updateUserInterface(posts) }
    .exceptionally { error ->
        logger.error("请求链失败", error)
        null
    }
```

### 线程管理

- **网络线程池** - 专用于网络 I/O 操作
- **主线程集成** - 自动切换到 Bukkit 主线程
- **资源绑定** - 绑定到插件生命周期

```kotlin
// 自动线程切换
networkManager.get("https://api.example.com/data")
    .async() // 在网络线程池中执行
    .thenAcceptSync { response -> // 自动切换到主线程
        // 在主线程中安全地操作 Bukkit API
        updatePlayerData(response)
    }
```

## 连接管理

### 连接池配置

```kotlin
data class ConnectionPoolConfig(
    val maxConnections: Int = 50,
    val maxConnectionsPerRoute: Int = 10,
    val connectionTimeout: Long = 30000,
    val socketTimeout: Long = 60000,
    val connectionRequestTimeout: Long = 10000,
    val keepAliveTime: Long = 300000
)
```

### 连接复用

- 自动复用 HTTP 连接
- 支持 HTTP/1.1 持久连接
- 智能连接池管理
- 连接泄漏检测和清理

## 错误处理和重试

### 重试策略

```kotlin
enum class RetryStrategy {
    NONE,           // 不重试
    FIXED_DELAY,    // 固定延迟重试
    EXPONENTIAL,    // 指数退避重试
    LINEAR          // 线性增长重试
}

data class RetryConfig(
    val strategy: RetryStrategy = RetryStrategy.EXPONENTIAL,
    val maxRetries: Int = 3,
    val initialDelay: Long = 1000,
    val maxDelay: Long = 30000,
    val multiplier: Double = 2.0
)
```

### 错误分类

```kotlin
sealed class NetworkError : Exception() {
    // 连接错误
    class ConnectionError(cause: Throwable) : NetworkError()
    
    // 超时错误
    class TimeoutError(val timeoutType: TimeoutType) : NetworkError()
    
    // HTTP 错误
    class HttpError(val statusCode: Int, val message: String) : NetworkError()
    
    // 解析错误
    class ParseError(val content: String, cause: Throwable) : NetworkError()
}
```

## 请求类型和配置

### GET 请求

```kotlin
// 简单 GET 请求
networkManager.get("https://api.example.com/users")

// 带参数的 GET 请求
networkManager.get("https://api.example.com/users")
    .param("page", "1")
    .param("size", "20")
    .param("sort", "name")
```

### POST 请求

```kotlin
// JSON 数据
networkManager.post("https://api.example.com/users")
    .json(userData)

// 表单数据
networkManager.post("https://api.example.com/login")
    .form("username", "player")
    .form("password", "secret")

// 原始数据
networkManager.post("https://api.example.com/data")
    .body(rawData, "application/octet-stream")
```

### 文件操作

```kotlin
// 文件上传
networkManager.upload("https://api.example.com/files")
    .file("file", File("data.txt"))
    .field("description", "数据文件")
    .progress { uploaded, total ->
        val percent = (uploaded * 100 / total).toInt()
        logger.info("上传进度: $percent%")
    }

// 文件下载
networkManager.download("https://example.com/file.zip")
    .to(File("downloads/file.zip"))
    .progress { downloaded, total ->
        updateProgressBar(downloaded, total)
    }
```

## 安全性考虑

### HTTPS 支持

- 默认启用 SSL/TLS 验证
- 支持自定义证书验证
- 支持客户端证书认证

```kotlin
// SSL 配置
data class SSLConfig(
    val verifyHostname: Boolean = true,
    val trustAllCertificates: Boolean = false,
    val keyStore: String? = null,
    val keyStorePassword: String? = null,
    val trustStore: String? = null,
    val trustStorePassword: String? = null
)
```

### 认证机制

```kotlin
// Bearer Token 认证
networkManager.get("https://api.example.com/protected")
    .bearerAuth(token)

// Basic 认证
networkManager.get("https://api.example.com/protected")
    .basicAuth(username, password)

// 自定义认证头
networkManager.get("https://api.example.com/protected")
    .header("X-API-Key", apiKey)
```

## 性能优化

### 缓存策略

```kotlin
// 响应缓存
networkManager.get("https://api.example.com/static-data")
    .cache(Duration.ofMinutes(30))

// 条件请求
networkManager.get("https://api.example.com/data")
    .ifModifiedSince(lastModified)
    .ifNoneMatch(etag)
```

### 压缩支持

- 自动 GZIP 压缩
- 支持 Deflate 压缩
- 请求体压缩选项

### 连接优化

- HTTP/2 支持
- 连接预热
- DNS 缓存
- 连接池调优

## 监控和调试

### 请求日志

```kotlin
// 启用详细日志
networkManager.get("https://api.example.com/data")
    .debug(true) // 记录请求和响应详情
    .trace(true) // 记录网络传输详情
```

### 性能指标

```kotlin
data class RequestMetrics(
    val requestTime: Long,      // 请求总时间
    val connectTime: Long,      // 连接建立时间
    val dnsLookupTime: Long,    // DNS 查询时间
    val sslHandshakeTime: Long, // SSL 握手时间
    val transferTime: Long,     // 数据传输时间
    val bytesUploaded: Long,    // 上传字节数
    val bytesDownloaded: Long   // 下载字节数
)
```

## 相关文档

- [🚀 快速开始](quick-start.md) - 快速上手指南
- [🔐 认证机制](authentication.md) - API 认证方法
- [📊 性能优化](performance.md) - 性能调优指南

---

**📝 注意**: 此文档正在完善中，更多概念说明请参考 [API 参考](api-reference.md)。
