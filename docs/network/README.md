# Network 模块用户教程

欢迎使用现代化的 HTTP 客户端框架！本教程将带您从零开始学习如何使用 Network 模块构建强大的网络应用。

## 📚 文档目录

### 基础教程
- [📖 介绍](intro.md) - 5分钟快速了解Network模块
- [🚀 快速开始](quick-start.md) - 第一个HTTP请求
- [🎯 基础概念](concepts.md) - 核心概念详解

### 功能指南
- [🌐 HTTP客户端](http-client.md) - 基础HTTP请求和响应
- [🔧 类型安全](type-safety.md) - 类型安全的请求和序列化
- [🔐 认证支持](authentication.md) - Basic、Bearer、API Key认证
- [⚡ 批量请求](batch-requests.md) - 高效的批量请求处理
- [📁 文件操作](file-operations.md) - 文件上传和下载
- [🔄 重试机制](retry-mechanism.md) - 智能请求重试

### 高级主题
- [♻️ 可终止请求](terminable-requests.md) - 生命周期管理和资源清理
- [📊 网络监控](network-monitoring.md) - 连接状态监控和延迟测量
- [🏗️ 架构设计](architecture.md) - 模块架构和设计模式
- [⚙️ 性能优化](performance.md) - 性能调优和最佳实践

### 参考资料
- [📋 API参考](api-reference.md) - 完整API文档
- [💡 最佳实践](best-practices.md) - 开发建议和模式
- [🔧 故障排除](troubleshooting.md) - 常见问题解决
- [📝 示例代码](examples.md) - 完整示例集合

## 🎯 快速导航

### 我想要...
- **发送第一个HTTP请求** → [快速开始](quick-start.md)
- **创建HTTP客户端** → [HTTP客户端](http-client.md)
- **类型安全的请求** → [类型安全](type-safety.md)
- **添加认证** → [认证支持](authentication.md)
- **批量处理请求** → [批量请求](batch-requests.md)
- **下载文件** → [文件操作](file-operations.md)
- **处理失败重试** → [重试机制](retry-mechanism.md)
- **管理请求生命周期** → [可终止请求](terminable-requests.md)
- **监控网络状态** → [网络监控](network-monitoring.md)
- **解决问题** → [故障排除](troubleshooting.md)

## 🆕 最新特性

- **异步支持** - 基于任务调度器，支持异步非阻塞操作
- **类型安全** - 提供类型安全的DSL和扩展函数
- **插件化** - 支持日志、序列化、认证等插件
- **多引擎** - 支持OkHttp、Java、Apache等多种HTTP引擎
- **序列化** - 支持kotlinx.serialization和Jackson
- **认证** - 支持Basic、Bearer、API Key等认证方式
- **生命周期** - 自动集成到BasePlugin生命周期管理
- **可终止** - 支持TerminableConsumer，自动管理请求生命周期
- **监控** - 提供网络状态监控和延迟测量
- **重试** - 内置请求重试机制
- **批量** - 支持批量请求处理

## 🔧 技术栈

### 核心依赖
- **Ktor Client Core** - 核心HTTP客户端
- **Ktor Client OkHttp** - OkHttp引擎（默认）
- **Ktor Client Content Negotiation** - 内容协商
- **Ktor Client Logging** - 请求日志
- **Ktor Client Auth** - 认证支持

### 序列化支持
- **kotlinx.serialization** - Kotlin原生序列化
- **Jackson** - 兼容Config模块的Jackson序列化

### 版本要求
- **Kotlin** 2.2.0+
- **Java** 17+
- **Ktor** 2.3.12+

## 🚀 快速预览

### 异步调度方式（推荐）
```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 使用插件异步调度器进行网络请求
        runAsync {
            try {
                // GET请求
                val response = httpGet("https://api.example.com/data") {
                    header("Authorization", "Bearer token")
                    parameter("limit", "10")
                }

                // 在主线程处理响应
                runSync {
                    logger.info("响应状态: ${response.status}")
                }

                // POST请求
                val postResponse = httpPost("https://api.example.com/users") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"name": "John", "age": 30}""")
                }

                // 类型安全的请求
                val user: User = httpGetTyped("https://api.example.com/users/1")

                runSync {
                    logger.info("用户名: ${user.name}")
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

### 任务链方式
```kotlin
class TaskChainPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 使用任务链进行网络操作
        val networkTask = runAsync {
            // 使用默认HTTP客户端
            val client = getHttpClient()
            client.get("https://api.example.com/data")
        }

        networkTask.thenRunSync { response ->
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

### 认证支持
```kotlin
override fun onPluginEnable() {
    // 使用异步调度器进行认证请求
    runAsync {
        try {
            // API Key认证
            val apiClient = createApiKeyHttpClient("github", "ghp_xxxxxxxxxxxx")
            val repos = apiClient.getTyped<List<Repository>>("https://api.github.com/user/repos")

            // Bearer Token认证
            val oauthClient = createBearerHttpClient("oauth", "access_token_here")
            val profile = oauthClient.getTyped<UserProfile>("https://api.example.com/profile")

            // Basic认证
            val basicClient = createBasicAuthHttpClient("basic", "username", "password")
            val data = basicClient.getTyped<ApiData>("https://api.example.com/protected")

            // 处理认证后的数据
            runSync {
                processAuthenticatedData(repos, profile, data)
            }
        } catch (e: Exception) {
            runSync {
                logger.error("认证请求失败", e)
            }
        }
    }
        }
    }
}
```

### 批量请求
```kotlin
override fun onPluginEnable() {
    // 使用异步调度器进行批量请求
    runAsync {
        try {
            // 批量请求
            val client = getHttpClient()
            val responses = mutableListOf<Any>()

            // 并行执行多个请求
            val usersTask = runAsync { client.get("https://api.example.com/users") }
            val postsTask = runAsync { client.get("https://api.example.com/posts") }
            val analyticsTask = runAsync {
                client.post("https://api.example.com/analytics") {
                    setBody("""{"event": "page_view"}""")
                }
            }

            // 等待所有请求完成
            responses.add(usersTask.get())
            responses.add(postsTask.get())
            responses.add(analyticsTask.get())

            // 批量用户请求处理
            val userTasks = listOf(
                runAsync { client.getTyped<User>("https://api.example.com/users/1") },
                runAsync { client.getTyped<User>("https://api.example.com/users/2") },
                runAsync { client.getTyped<User>("https://api.example.com/users/3") }
            )

            val users = userTasks.map { it.get() }

            // 在主线程处理结果
            runSync {
                processBatchResults(responses, users)
            }
        } catch (e: Exception) {
            runSync {
                logger.error("批量请求失败", e)
            }
        }
    }

            batchHttpRequests(requests) { users ->
                logger.info("获取到 ${users.size} 个用户")
            }
        }
    }
}
```

### 网络监控
```kotlin
override fun onPluginEnable() {
    // 使用重复任务进行网络监控
    runSyncRepeating(0L, 600L) { task -> // 每30秒检查一次
        runAsync {
            try {
                // 监控多个服务
                val healthTask = runAsync {
                    httpGet("https://api.example.com/health")
                }
                val pingTask = runAsync {
                    httpGet("https://cdn.example.com/ping")
                }

                val healthResponse = healthTask.get()
                val pingResponse = pingTask.get()

                runSync {
                    // 处理监控结果
                    if (healthResponse.status.value in 200..299) {
                        logger.info("API服务可用")
                    } else {
                        logger.warning("API服务不可用: ${healthResponse.status}")
                    }

                    if (pingResponse.status.value in 200..299) {
                        logger.info("CDN服务可用")
                    } else {
                        logger.warning("CDN服务不可用: ${pingResponse.status}")
                    }
                }
            } catch (e: Exception) {
                runSync {
                    logger.error("网络监控检查失败", e)
                }
            }
        }
    }

    // 延迟测量任务
    runAsync {
        try {
            val latency = measureNetworkLatency("https://api.example.com/ping", 5)
            runSync {
                logger.info("""
                    网络延迟统计:
                    - 平均: ${latency.average}ms
                    - 最小: ${latency.min}ms
                    - 最大: ${latency.max}ms
                    - 成功率: ${latency.successRate * 100}%
                """.trimIndent())
            }
        } catch (e: Exception) {
            runSync {
                logger.error("延迟测量失败", e)
            }
        }
    }
}
```

### 可取消的任务请求
```kotlin
class ApiService : BasePlugin() {
    private val activeTasks = mutableListOf<ITaskHandler<*>>()

    fun fetchUserData(userId: Int) {
        // 创建可取消的HTTP请求任务
        val userTask = runAsync {
            httpGetTyped<User>("https://api.example.com/users/$userId")
        }

        // 保存任务引用
        activeTasks.add(userTask)

        // 处理结果
        userTask.thenRunSync { user ->
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

### 文件下载
```kotlin
override fun onPluginEnable() {
    // 使用异步调度器进行文件下载
    runAsync {
        try {
            // 文件下载
            val data = downloadFile(
                url = "https://example.com/large-file.zip",
                maxSize = 50 * 1024 * 1024 // 50MB
            )

            // 保存文件
            File("downloads/file.zip").writeBytes(data)

            runSync {
                logger.info("文件下载完成: ${data.size} bytes")
            }
        } catch (error: Exception) {
            runSync {
                logger.warning("文件下载失败: ${error.message}")
            }
        }
    }

    // 可取消的文件下载任务
    val downloadTask = runAsync {
        try {
            val data = downloadFile(
                url = "https://example.com/file.zip",
                maxSize = 50 * 1024 * 1024
            )

            runSync {
                logger.info("下载完成: ${data.size} bytes")
            }
        } catch (error: Exception) {
            runSync {
                logger.info("下载失败: ${error.message}")
            }
        }
    }

    // 保存任务引用以便后续取消
    bind(downloadTask)
        }
    }
}
```

## 🤝 贡献

如果您发现文档中的错误或有改进建议，欢迎提交Issue或Pull Request。

---

**开始您的Network开发之旅** → [📖 介绍](intro.md)
