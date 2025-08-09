# Network 模块介绍

## 🎯 什么是 Network 模块？

Network 模块是一个基于 Ktor Client 的现代化 HTTP 客户端框架，为 Minecraft 插件提供异步任务支持、类型安全的网络操作解决方案。它融合了 violet/network 的设计思想，支持多种认证方式，并与 BasePlugin 完美集成。

**5分钟快速了解：** Network 模块解决了传统网络请求中同步阻塞、类型不安全、资源管理困难等问题，通过异步任务支持、类型安全 DSL、可取消请求和智能重试机制，让网络操作变得简单而强大。

## 🔍 解决的问题

### 传统网络请求的痛点

1. **同步阻塞** - 传统HTTP请求阻塞主线程，影响服务器性能
2. **类型不安全** - 手动解析响应，容易出现类型错误
3. **资源管理困难** - 手动管理HTTP连接，容易出现资源泄漏
4. **配置复杂** - HTTP客户端配置繁琐，缺乏最佳实践
5. **错误处理不完善** - 缺乏统一的错误处理和重试机制
6. **认证支持有限** - 各种认证方式需要手动实现

### Network 模块的解决方案

✅ **异步支持** - 基于任务调度器，支持异步非阻塞操作
✅ **类型安全** - 提供类型安全的DSL和自动序列化
✅ **可取消请求** - 支持任务取消，自动管理请求生命周期
✅ **多种认证** - 支持Basic、Bearer、API Key等认证方式
✅ **智能重试** - 内置请求重试机制和错误处理
✅ **批量操作** - 支持批量请求处理，提升效率

## 🆚 技术对比

### 与传统 HTTP 库对比

| 特性     | OkHttp      | Apache HttpClient | Network 模块   |
| -------- | ----------- | ----------------- | -------------- |
| 异步支持 | 回调方式    | 复杂配置          | 任务调度器原生 |
| 类型安全 | 手动解析    | 手动解析          | 自动序列化     |
| 资源管理 | 手动管理    | 手动管理          | 自动管理       |
| 配置方式 | Builder模式 | 复杂配置          | DSL配置        |
| 认证支持 | 基础支持    | 完整支持          | 简化配置       |
| 学习成本 | 中等        | 高                | 低             |

### 与其他异步HTTP库对比

| 库              | 优势                            | 劣势                 |
| --------------- | ------------------------------- | -------------------- |
| **Network模块** | Minecraft集成、可取消、简化配置 | 功能相对专一         |
| Ktor Client     | 功能强大、生态丰富              | 配置复杂、学习成本高 |
| Retrofit + 异步 | 成熟稳定、注解驱动              | 配置繁琐、依赖多     |
| Fuel            | 轻量级、Kotlin友好              | 功能有限、社区小     |

## 🚀 快速示例

### 传统 OkHttp 方式
```java
public class OldHttpManager {
    private OkHttpClient client = new OkHttpClient();

    public void fetchUserData(String userId) {
        // 构建请求
        Request request = new Request.Builder()
            .url("https://api.example.com/users/" + userId)
            .addHeader("Authorization", "Bearer " + token)
            .build();

        // 异步请求（回调地狱）
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 错误处理
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    // 手动解析JSON
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        User user = mapper.readValue(json, User.class);
                        // 回到主线程更新UI
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            updateUserDisplay(user);
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    // 处理HTTP错误
                    System.err.println("HTTP Error: " + response.code());
                }
            }
        });
    }
}
```

### Network 模块方式
```kotlin
class ModernHttpManager : BasePlugin() {
    override fun onPluginEnable() {
        // 使用插件异步调度器
        runAsync {
            try {
                // 类型安全的HTTP请求
                val user: User = httpGetTyped("https://api.example.com/users/$userId") {
                    bearerAuth(token)
                }

                // 自动回到主线程
                runSync {
                    updateUserDisplay(user)
                }
            } catch (e: Exception) {
                // 统一错误处理
                runSync {
                    logger.error("获取用户数据失败", e)
                }
            }
        }
    }
}
```

## 🏗️ 核心架构

### 1. 异步调度器支持
```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 使用插件异步调度器进行HTTP请求
        runAsync {
            try {
                // 并发请求
                val userTask = runAsync { httpGetTyped<User>("https://api.example.com/user") }
                val postsTask = runAsync { httpGetTyped<List<Post>>("https://api.example.com/posts") }

                // 等待所有请求完成
                val user = userTask.get()
                val posts = postsTask.get()

                // 在主线程处理结果
                runSync {
                    processUserData(user, posts)
                }
            } catch (e: Exception) {
                runSync {
                    logger.error("网络请求失败", e)
                }
            }
        }
    }
}
```

### 2. 类型安全的 DSL
```kotlin
// GET 请求
val response = httpGet("https://api.example.com/data") {
    header("Authorization", "Bearer $token")
    parameter("limit", "10")
    parameter("offset", "0")
}

// POST 请求
val result = httpPost("https://api.example.com/users") {
    contentType(ContentType.Application.Json)
    jsonBody(CreateUserRequest(name = "John", email = "john@example.com"))
}

// 类型安全的请求
val user: User = httpGetTyped("https://api.example.com/users/1")
val users: List<User> = httpGetTyped("https://api.example.com/users")
```

### 3. 认证支持
```kotlin
// API Key 认证
val apiClient = createApiKeyHttpClient("github", "ghp_xxxxxxxxxxxx")
val repos = apiClient.getTyped<List<Repository>>("https://api.github.com/user/repos")

// Bearer Token 认证
val oauthClient = createBearerHttpClient("oauth", "access_token_here")
val profile = oauthClient.getTyped<UserProfile>("https://api.example.com/profile")

// Basic 认证
val basicClient = createBasicAuthHttpClient("basic", "username", "password")
val data = basicClient.getTyped<ApiData>("https://api.example.com/protected")

// 自定义认证
val customClient = createHttpClient("custom") {
    auth {
        bearer { "custom_token_logic" }
    }
}
```

### 4. 可取消的请求任务
```kotlin
class ApiService : BasePlugin() {
    private val apiTasks = mutableListOf<ITaskHandler<*>>()

    fun fetchData() {
        // 可取消的HTTP请求任务
        val dataTask = runAsync {
            try {
                httpGetTyped<ApiData>("https://api.example.com/data")
            } catch (e: Exception) {
                throw e
            }
        }

        // 保存任务引用以便取消
        apiTasks.add(dataTask)

        // 处理结果
        dataTask.thenRunSync { data ->
            processData(data)
        }.exceptionally { error ->
            runSync {
                handleError(error)
            }
            null
        }
    }

    override fun onPluginDisable() {
        // 插件禁用时自动取消所有任务
        apiTasks.forEach { it.cancel(true) }
        apiTasks.clear()
    }

    fun close() {
        scope.cancel() // 取消所有进行中的请求
        terminableRegistry.close()
    }
}
```

## 📊 功能特性

### 批量请求处理
```kotlin
// 批量请求
val requests = listOf(
    { client: HttpClient -> client.getTyped<User>("https://api.example.com/users/1") },
    { client: HttpClient -> client.getTyped<User>("https://api.example.com/users/2") },
    { client: HttpClient -> client.getTyped<User>("https://api.example.com/users/3") }
)

batchHttpRequests(requests) { users ->
    logger.info("获取到 ${users.size} 个用户")
    users.forEach { user ->
        logger.info("用户: ${user.name}")
    }
}
```

### 文件下载
```kotlin
// 文件下载
downloadFile(
    url = "https://example.com/large-file.zip",
    maxSize = 50 * 1024 * 1024 // 50MB
) { result ->
    result.onSuccess { data ->
        File("downloads/file.zip").writeBytes(data)
        logger.info("文件下载完成: ${data.size} bytes")
    }.onFailure { error ->
        logger.warning("文件下载失败: ${error.message}")
    }
}
```

### 网络监控
```kotlin
// 网络状态监控
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
logger.info("""
    网络延迟统计:
    - 平均: ${latency.average}ms
    - 最小: ${latency.min}ms
    - 最大: ${latency.max}ms
    - 成功率: ${latency.successRate * 100}%
""".trimIndent())
```

### 重试机制
```kotlin
// 智能重试
httpRequestWithRetry(
    maxRetries = 3,
    delayMillis = 1000
) { client ->
    client.getTyped<String>("https://unreliable-api.com/data")
} { result ->
    result.onSuccess { data ->
        logger.info("获取数据成功: $data")
    }.onFailure { error ->
        logger.warning("获取数据失败: ${error.message}")
    }
}
```

## 🎯 适用场景

### ✅ 推荐使用
- 需要HTTP请求的Minecraft插件
- 与外部API集成的应用
- 需要文件下载/上传功能
- 高并发的网络请求需求
- 需要网络状态监控的系统

### ⚠️ 考虑因素
- 学习异步编程的成本
- 网络依赖的稳定性
- 团队对异步编程的熟悉度

## 🔄 迁移路径

### 从 OkHttp 迁移
1. **添加依赖** - 引入Network模块
2. **重构请求** - 使用插件异步调度器替换回调
3. **类型安全** - 使用类型安全的扩展函数
4. **资源管理** - 使用插件生命周期管理任务

### 从同步HTTP库迁移
1. **识别阻塞操作** - 找出所有同步HTTP请求
2. **异步重构** - 使用 `runAsync` 替换同步调用
3. **错误处理** - 统一异常处理机制
4. **性能测试** - 对比迁移前后的性能

## 🗄️ 数据库集成指导

### 数据库操作与网络请求结合
```kotlin
class DataSyncPlugin : BasePlugin() {

    override fun onPluginEnable() {
        // 同步远程数据到本地数据库
        runAsync {
            try {
                // 1. 从API获取数据
                val userData = httpGetTyped<List<User>>("https://api.example.com/users")

                // 2. 异步写入数据库
                val dbTask = runAsync {
                    // 使用数据库模块进行异步操作
                    database.transaction {
                        userData.forEach { user ->
                            userRepository.save(user)
                        }
                    }
                }

                // 3. 等待数据库操作完成
                dbTask.get()

                // 4. 在主线程通知完成
                runSync {
                    logger.info("数据同步完成，共${userData.size}条记录")
                    broadcastMessage("数据已更新")
                }

            } catch (e: Exception) {
                runSync {
                    logger.error("数据同步失败", e)
                }
            }
        }
    }

    // 定期数据同步
    private fun scheduleDataSync() {
        runSyncRepeating(0L, 72000L) { task -> // 每小时同步一次
            runAsync {
                syncPlayerStats()
            }
        }
    }

    private fun syncPlayerStats() {
        try {
            // 获取本地玩家统计
            val localStats = database.select<PlayerStats>()
                .where { PlayerStats::lastUpdated less (System.currentTimeMillis() - 3600000) }
                .list()

            // 批量上传到远程服务器
            localStats.chunked(50).forEach { batch ->
                httpPost("https://api.example.com/stats/batch") {
                    contentType(ContentType.Application.Json)
                    setBody(gson.toJson(batch))
                }
            }

            runSync {
                logger.info("玩家统计同步完成")
            }
        } catch (e: Exception) {
            runSync {
                logger.error("统计同步失败", e)
            }
        }
    }
}
```

### 最佳实践：网络 + 数据库
1. **异步优先** - 所有网络和数据库操作都使用 `runAsync`
2. **事务管理** - 数据库操作使用事务确保一致性
3. **错误恢复** - 网络失败时从本地数据库读取
4. **缓存策略** - 网络数据缓存到数据库减少请求
5. **批量操作** - 大量数据使用批量插入/更新

---

**准备开始？** → [🚀 快速开始](README.md)
