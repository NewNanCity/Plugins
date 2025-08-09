# Config 模块故障排除

本文档收集了使用 Config 模块时常见的问题和解决方案。

## 🚨 常见错误

### 1. 配置文件格式错误

**问题描述：**
```
[ERROR] Failed to parse config file: server.yml
[ERROR] JsonParseException: Unexpected character at line 5
```

**原因分析：**
- YAML/JSON 语法错误
- 文件编码问题
- 特殊字符未正确转义

**解决方案：**
```kotlin
// ✅ 正确的 YAML 格式
"""
server:
  name: "我的服务器"
  maxPlayers: 100
  features:
    - "pvp"
    - "economy"
  spawn:
    world: "world"
    x: 0.0
    y: 64.0
    z: 0.0
"""

// ❌ 错误的 YAML 格式
"""
server:
name: 我的服务器  # 缺少缩进
maxPlayers: "100  # 缺少引号
features:
- pvp
- economy"        # 引号位置错误
"""

// 验证配置文件
try {
    val config = configManager.parse<ServerConfig>("server.yml")
    logger.info("配置文件解析成功")
} catch (e: ConfigParseException) {
    logger.error("配置文件格式错误: ${e.message}")
    // 使用默认配置
    val defaultConfig = ServerConfig()
    configManager.save(defaultConfig, "server.yml")
}
```

### 2. 类型转换错误

**问题描述：**
```
[ERROR] ClassCastException: String cannot be cast to Integer
[ERROR] Type mismatch: expected Location, got Map
```

**原因分析：**
- 配置文件中的类型与数据类不匹配
- 缺少必要的序列化注解
- Bukkit 类型序列化失败

**解决方案：**
```kotlin
@Serializable
data class ServerConfig(
    // ✅ 正确：使用默认值处理可选字段
    val name: String = "默认服务器",
    val maxPlayers: Int = 20,

    // ✅ 正确：可空类型处理
    val description: String? = null,

    // ✅ 正确：使用 @SerialName 处理字段名映射
    @SerialName("max_players")
    val maxPlayersAlt: Int = 20,

    // ✅ 正确：Bukkit 类型需要自定义序列化器
    @Serializable(with = LocationSerializer::class)
    val spawn: Location? = null
)

// 类型安全的配置读取
fun loadConfig(): ServerConfig {
    return try {
        configManager.parse<ServerConfig>("server.yml")
    } catch (e: Exception) {
        logger.warning("配置加载失败，使用默认配置: ${e.message}")
        ServerConfig() // 返回默认配置
    }
}
```

### 3. 文件权限问题

**问题描述：**
```
[ERROR] IOException: Permission denied
[ERROR] Failed to save config file: config.yml
```

**原因分析：**
- 文件或目录权限不足
- 文件被其他进程占用
- 磁盘空间不足

**解决方案：**
```kotlin
fun saveConfigSafely(config: Any, path: String): Boolean {
    return try {
        // 检查目录是否存在
        val file = File(plugin.dataFolder, path)
        file.parentFile?.mkdirs()

        // 检查文件权限
        if (file.exists() && !file.canWrite()) {
            logger.error("配置文件无写入权限: $path")
            return false
        }

        // 检查磁盘空间
        val freeSpace = file.parentFile.freeSpace
        if (freeSpace < 1024 * 1024) { // 小于1MB
            logger.error("磁盘空间不足")
            return false
        }

        // 保存配置
        configManager.save(config, path)
        logger.info("配置保存成功: $path")
        true
    } catch (e: IOException) {
        logger.error("配置保存失败: ${e.message}")
        false
    }
}
```

### 4. 缓存问题

**问题描述：**
```
[WARNING] Config cache hit rate too low: 15%
[ERROR] OutOfMemoryError: Cache size exceeded
```

**原因分析：**
- 缓存配置不当
- 配置文件过于频繁变更
- 缓存容量设置过大

**解决方案：**
```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        val configManager = ConfigManager(this).apply {
            // ✅ 合理的缓存配置
            setCacheConfig(
                type = CacheType.LRU,
                capacity = 50, // 根据实际需求调整
                expireAfterAccess = Duration.ofMinutes(30)
            )
        }

        // 监控缓存性能
        runSyncRepeating(0L, 20L * 60) { // 每分钟检查
            val stats = configManager.getCacheStats()
            if (stats.hitRate < 0.5) { // 命中率低于50%
                logger.warning("配置缓存命中率较低: ${stats.hitRate * 100}%")
            }

            if (stats.size > 100) { // 缓存项过多
                logger.warning("配置缓存项过多: ${stats.size}")
                configManager.clearCache()
            }
        }
    }
}
```

## 🔧 性能问题

### 1. 配置加载缓慢

**症状：**
- 插件启动时间过长
- 配置文件读取耗时
- 服务器启动卡顿

**诊断方法：**
```kotlin
fun loadConfigWithTiming(path: String): ServerConfig {
    val startTime = System.currentTimeMillis()

    return try {
        val config = configManager.parse<ServerConfig>(path)
        val loadTime = System.currentTimeMillis() - startTime

        if (loadTime > 1000) { // 超过1秒
            logger.warning("配置加载耗时过长: ${path} - ${loadTime}ms")
        }

        config
    } catch (e: Exception) {
        logger.error("配置加载失败: ${path}", e)
        throw e
    }
}
```

**优化方案：**
```kotlin
class OptimizedConfigManager(plugin: Plugin) {
    private val configManager = ConfigManager(plugin)

    init {
        // 预加载常用配置
        preloadPlugins()

        // 启用缓存
        configManager.setCacheConfig(
            type = CacheType.LRU,
            capacity = 100
        )
    }

    private fun preloadPlugins() {
        runAsync {
            // 异步预加载配置
            val commonConfigs = listOf("server.yml", "messages.yml", "features.yml")
            commonConfigs.forEach { path ->
                try {
                    configManager.get(path)
                    logger.info("预加载配置: $path")
                } catch (e: Exception) {
                    logger.warning("预加载配置失败: $path")
                }
            }
        }
    }
}
```

### 2. 内存占用过高

**症状：**
- 配置相关的内存使用过多
- 频繁的 GC
- OutOfMemoryError

**解决方案：**
```kotlin
// ✅ 优化：使用轻量级配置结构
@Serializable
data class LightweightConfig(
    val essentialSettings: Map<String, String>,
    val flags: Set<String>
) {
    // 避免存储大量重复数据
    companion object {
        private val commonDefaults = mapOf(
            "language" to "zh_CN",
            "timezone" to "Asia/Shanghai"
        )
    }

    fun getSetting(key: String): String? {
        return essentialSettings[key] ?: commonDefaults[key]
    }
}

// ✅ 优化：定期清理缓存
runSyncRepeating(0L, 20L * 300) { // 每5分钟
    configManager.clearCache()
    System.gc() // 建议垃圾回收
}
```

## 🐛 调试技巧

### 1. 启用配置调试

```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        val configManager = ConfigManager(this)

        if (config.getBoolean("debug.config", false)) {
            // 启用配置调试
            configManager.setDebugMode(true)

            // 监控配置操作
            configManager.onConfigLoad { path, duration ->
                logger.info("配置加载: $path (${duration}ms)")
            }

            configManager.onConfigSave { path, duration ->
                logger.info("配置保存: $path (${duration}ms)")
            }
        }
    }
}
```

### 2. 配置验证

```kotlin
@Serializable
data class ValidatedConfig(
    val serverName: String,
    val maxPlayers: Int,
    val features: List<String>
) {
    init {
        // 配置验证
        require(serverName.isNotBlank()) { "服务器名称不能为空" }
        require(maxPlayers in 1..1000) { "最大玩家数必须在1-1000之间" }
        require(features.all { it.isNotBlank() }) { "功能名称不能为空" }
    }

    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (serverName.length > 50) {
            errors.add("服务器名称过长")
        }

        if (maxPlayers < 1) {
            errors.add("最大玩家数不能小于1")
        }

        return errors
    }
}

// 使用验证
fun loadValidatedConfig(): ValidatedConfig? {
    return try {
        val config = configManager.parse<ValidatedConfig>("server.yml")
        val errors = config.validate()

        if (errors.isNotEmpty()) {
            logger.warning("配置验证失败: ${errors.joinToString(", ")}")
            return null
        }

        config
    } catch (e: Exception) {
        logger.error("配置加载失败", e)
        null
    }
}
```

### 3. 格式兼容性检查

```kotlin
fun checkFormatSupport() {
    val supportedFormats = mapOf(
        "json" to ::testJsonSupport,
        "yaml" to ::testYamlSupport,
        "toml" to ::testTomlSupport,
        "xml" to ::testXmlSupport
    )

    supportedFormats.forEach { (format, test) ->
        try {
            test()
            logger.info("格式支持: $format ✓")
        } catch (e: Exception) {
            logger.warning("格式不支持: $format ✗ (${e.message})")
        }
    }
}

private fun testJsonSupport() {
    val testData = mapOf("test" to "value")
    configManager.save(testData, "test.json")
    configManager.parse<Map<String, String>>("test.json")
}
```

## 📋 检查清单

### 配置文件检查
- [ ] 文件格式正确（YAML/JSON语法）
- [ ] 文件编码为UTF-8
- [ ] 特殊字符正确转义
- [ ] 文件权限可读写
- [ ] 目录结构存在

### 数据类检查
- [ ] 所有字段有默认值或可空
- [ ] 使用正确的序列化注解
- [ ] Bukkit类型有自定义序列化器
- [ ] 字段名与配置文件匹配

### 性能检查
- [ ] 缓存配置合理
- [ ] 配置加载时间正常
- [ ] 内存使用稳定
- [ ] 无频繁的配置重载

## 🆘 获取帮助

如果以上解决方案都无法解决您的问题，请：

1. **检查配置文件语法** - 使用在线YAML/JSON验证器
2. **查看完整错误日志** - 包括堆栈跟踪信息
3. **提供配置文件示例** - 脱敏后的配置文件内容
4. **环境信息** - Jackson版本、文件系统类型等
5. **提交Issue** - 在项目仓库提交详细的问题报告

---

**返回文档首页** → [📚 Config模块文档](README.md)
