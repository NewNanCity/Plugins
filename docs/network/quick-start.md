# Network 模块快速开始

> 📋 **状态**: 文档规划中，内容正在完善

## 概述

Network 模块提供了强大的网络请求功能，支持 HTTP/HTTPS 请求、文件上传下载、异步处理等。本页面将指导您快速上手使用 Network 模块。

## 快速开始步骤

### 1. 添加依赖

```kotlin
// 在您的插件中添加 network 模块依赖
dependencies {
    implementation(project(":modules:network"))
}
```

### 2. 基本 HTTP 请求

```kotlin
class MyPlugin : BasePlugin() {
    private lateinit var networkManager: NetworkManager
    
    override fun onPluginEnable() {
        networkManager = NetworkManager(this)
        super.onPluginEnable()
        
        // 发送简单的 GET 请求
        sendGetRequest()
    }
    
    private fun sendGetRequest() {
        networkManager.get("https://api.example.com/data")
            .thenAccept { response ->
                logger.info("响应状态: ${response.statusCode}")
                logger.info("响应内容: ${response.body}")
            }
            .exceptionally { throwable ->
                logger.error("请求失败", throwable)
                null
            }
    }
}
```

### 3. POST 请求示例

```kotlin
// 发送 JSON 数据
fun sendPostRequest() {
    val jsonData = """
        {
            "player": "Steve",
            "action": "login",
            "timestamp": ${System.currentTimeMillis()}
        }
    """.trimIndent()
    
    networkManager.post("https://api.example.com/events")
        .header("Content-Type", "application/json")
        .body(jsonData)
        .thenAccept { response ->
            if (response.isSuccessful) {
                logger.info("事件发送成功")
            } else {
                logger.warn("事件发送失败: ${response.statusCode}")
            }
        }
}
```

### 4. 异步处理

```kotlin
// 异步处理网络请求
fun fetchPlayerData(playerUuid: String) {
    networkManager.get("https://api.mojang.com/user/profiles/$playerUuid/names")
        .async() // 异步执行
        .thenApply { response ->
            // 解析 JSON 响应
            parsePlayerNames(response.body)
        }
        .thenAccept { playerNames ->
            // 在主线程中更新玩家信息
            scheduler.runSync {
                updatePlayerInfo(playerUuid, playerNames)
            }
        }
        .exceptionally { error ->
            logger.error("获取玩家数据失败: $playerUuid", error)
            null
        }
}
```

## 支持的功能

### HTTP 方法
- **GET** - 获取数据
- **POST** - 提交数据
- **PUT** - 更新数据
- **DELETE** - 删除数据
- **PATCH** - 部分更新

### 请求配置
- **请求头** - 自定义 HTTP 头
- **请求体** - 支持文本、JSON、表单数据
- **超时设置** - 连接和读取超时
- **重试机制** - 自动重试失败的请求

### 响应处理
- **状态码检查** - HTTP 状态码验证
- **内容解析** - JSON、XML、文本解析
- **错误处理** - 异常和错误响应处理

## 配置示例

```kotlin
// 网络模块配置
data class NetworkConfig(
    val connectTimeout: Long = 30000,  // 连接超时 30 秒
    val readTimeout: Long = 60000,     // 读取超时 60 秒
    val maxRetries: Int = 3,           // 最大重试次数
    val retryDelay: Long = 1000,       // 重试延迟 1 秒
    val userAgent: String = "MinecraftPlugin/1.0"
)
```

## 文件操作

```kotlin
// 下载文件
fun downloadFile(url: String, savePath: String) {
    networkManager.download(url)
        .to(File(savePath))
        .progress { downloaded, total ->
            val percentage = (downloaded * 100 / total).toInt()
            logger.info("下载进度: $percentage%")
        }
        .thenAccept { file ->
            logger.info("文件下载完成: ${file.absolutePath}")
        }
}

// 上传文件
fun uploadFile(file: File, uploadUrl: String) {
    networkManager.upload(uploadUrl)
        .file("file", file)
        .field("description", "插件数据文件")
        .thenAccept { response ->
            logger.info("文件上传成功: ${response.body}")
        }
}
```

## 相关文档

- [📖 模块介绍](intro.md) - 了解 Network 模块的核心概念
- [💡 基础概念](concepts.md) - 深入了解网络请求系统
- [🔧 配置指南](configuration.md) - 详细配置说明

## 下一步

- [类型安全](type-safety.md) - 了解类型安全的请求处理
- [认证机制](authentication.md) - 学习 API 认证方法
- [API 参考](api-reference.md) - 完整的 API 文档

---

**📝 注意**: 此文档正在完善中，如有疑问请参考 [README](README.md) 或查看 [示例代码](examples.md)。
