# Config Module

基于 Jackson 的配置管理模块，支持多种配置文件格式和 Bukkit 原生类型序列化。

## 特性

### 🎯 核心特性
- **多格式支持**: JSON、YAML（核心格式），TOML、XML、CSV、Properties、HOCON（可选格式）
- **Bukkit 集成**: 原生支持 Location、ItemStack、Material 等 Bukkit 类型
- **类型安全**: 完整的 Kotlin 类型支持和泛型操作
- **缓存机制**: 可配置的缓存策略，提升性能
- **插件化架构**: 支持格式插件扩展

### 🔧 技术特性
- **协变泛型**: 类型安全的配置操作
- **链式调用**: 流畅的 API 设计
- **路径访问**: 支持点分隔路径操作
- **自动检测**: 根据文件扩展名自动识别格式
- **资源管理**: 实现 Terminable 接口，自动清理资源
- **版本兼容性**: 自动补全缺失的配置项，支持版本升级

## 依赖分析

### 核心依赖（必需，约 2MB）
```kotlin
implementation("com.fasterxml.jackson.core:jackson-core:2.15.2")           // ~350KB
implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")       // ~1.5MB
implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2") // ~150KB
implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2") // ~350KB
```

### 可选依赖（按需引入）
```kotlin
// TOML 支持 (~100KB)
implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-toml:2.15.2")

// XML 支持 (~700KB)
implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.15.2")

// CSV 支持 (~50KB)
implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv:2.15.2")

// Properties 支持 (~30KB)
implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-properties:2.15.2")

// HOCON 支持 (~200KB)
implementation("com.jasonclawson.jackson.dataformat:jackson-dataformat-hocon:1.1.0")
```

## 快速开始

提供两种API风格：**Kotlin DSL**（推荐）和**Java兼容工厂函数**：

### Kotlin DSL风格（推荐）

```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 配置文件操作（Kotlin DSL）- 支持版本兼容性
        touchConfigWithMerge("config.yml", createBackup = true)

        config("config.yml") {
            set("server.name", "My Server")
            set("server.max-players", 100)
            set("server.last-startup", System.currentTimeMillis())
            save()
        }

        // 读取配置值
        val serverConfig = config("config.yml")
        val serverName = serverConfig.get("server.name", "Default Server")
        val maxPlayers = serverConfig.get("server.max-players", 20)

        // 类型安全的配置解析
        val settings = configParse<ServerSettings>("settings.yml")

        // 批量配置操作
        config("config.yml").batch {
            set("server.version", pluginMeta.version)
            set("server.startup-time", System.currentTimeMillis())
            set("server.features", listOf("feature1", "feature2"))
        }
    }
}

data class ServerSettings(
    val name: String = "Default Server",
    val maxPlayers: Int = 20,
    val features: List<String> = emptyList()
)
```

### Java兼容工厂函数

```kotlin
class JavaCompatiblePlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 获取配置文件（使用BasePlugin的configManager属性）
        val config = configManager.get("config.yml")

        // 读取配置
        val serverName = config.get("server.name", "Default Server")
        val maxPlayers = config.get("server.max-players", 20)
        val features = config.get("server.features", emptyList<String>())

        // 修改配置
        config.set("server.last-startup", System.currentTimeMillis())
            .set("server.version", pluginMeta.version)
            .save()
    }
}
```

### 类型安全配置

```kotlin
// 定义配置数据类
data class ServerConfig(
    val name: String = "Default Server",
    val maxPlayers: Int = 20,
    val features: List<String> = emptyList(),
    val database: DatabaseConfig = DatabaseConfig()
)

data class DatabaseConfig(
    val host: String = "localhost",
    val port: Int = 3306,
    val username: String = "root",
    val password: String = "",
    val ssl: Boolean = false
)

// 使用类型安全解析
val serverConfig = configManager.parse<ServerConfig>("server.yml")
println("服务器名称: ${serverConfig.name}")
println("数据库主机: ${serverConfig.database.host}")

// 保存配置
val newConfig = serverConfig.copy(name = "My Awesome Server")
configManager.save(newConfig, "server.yml")
```

### 多格式支持

```kotlin
// JSON 格式
val jsonConfig = configManager.get("settings.json")

// YAML 格式
val yamlConfig = configManager.get("config.yml")

// TOML 格式（需要可选依赖）
val tomlConfig = configManager.get("config.toml")

// 格式转换
val yamlAsJson = yamlConfig.clone("config.json", "JSON")
```

### Bukkit 类型支持

```kotlin
// 保存 Location
val spawn = Location(world, 0.0, 64.0, 0.0)
config.set("spawn.location", spawn).save()

// 读取 Location
val savedSpawn = config.getOrNull<Location>("spawn.location")

// 保存 ItemStack
val item = ItemStack(Material.DIAMOND_SWORD)
config.set("rewards.sword", item).save()

// 读取 ItemStack
val savedItem = config.getOrNull<ItemStack>("rewards.sword")
```

### 扩展序列化支持

从 helper 库融合的高级序列化功能：

#### 位置和坐标系统

```kotlin
// 精确位置（支持小数坐标）
val position = Position.of(10.5, 64.0, -20.3, "world")
config.set("teleport.spawn", position).save()

// 方块位置（整数坐标）
val blockPos = BlockPosition.of(10, 64, -20, "world")
config.set("protection.corner", blockPos).save()

// 区块位置
val chunkPos = ChunkPosition.of(5, -3, "world")
config.set("loaded.chunks", listOf(chunkPos)).save()

// 方向（偏航角和俯仰角）
val direction = Direction.of(90.0f, -45.0f)
config.set("camera.direction", direction).save()

// 位置+方向组合
val point = Point.of(position, direction)
config.set("player.respawn", point).save()
```

#### 区域系统

```kotlin
// 三维矩形区域
val region = Region.of(
    Position.of(0.0, 0.0, 0.0, "world"),
    Position.of(100.0, 256.0, 100.0, "world")
)
config.set("protection.area", region).save()

// 方块级矩形区域
val blockRegion = BlockRegion.of(
    BlockPosition.of(0, 0, 0, "world"),
    BlockPosition.of(100, 256, 100, "world")
)

// 圆形区域（忽略Y轴）
val circularRegion = CircularRegion.of(
    Position.of(0.0, 64.0, 0.0, "world"),
    50.0 // 半径
)
config.set("spawn.protection", circularRegion).save()

// 区块级区域
val chunkRegion = ChunkRegion.of(
    ChunkPosition.of(0, 0, "world"),
    ChunkPosition.of(5, 5, "world")
)
```

#### 向量系统

```kotlin
// 二维向量
val vector2d = Vector2d(1.5, 2.5)
val vector2f = Vector2f(1.5f, 2.5f)

// 三维向量
val vector3d = Vector3d(1.5, 2.5, 3.5)
val vector3f = Vector3f(1.5f, 2.5f, 3.5f)

// 向量运算
val sum = vector3d + Vector3d(1.0, 1.0, 1.0)
val scaled = vector3d * 2.0
val dotProduct = vector3d.dot(Vector3d(1.0, 0.0, 0.0))

// 向量点（位置+方向的向量表示）
val vectorPoint = VectorPoint(vector3d, vector2f)
config.set("physics.velocity", vectorPoint).save()
```

#### 物品栏系统

```kotlin
// 序列化单个物品堆栈
val itemStack = ItemStack(Material.DIAMOND_SWORD)
val encoded = InventorySerializationUtils.encodeItemStackToString(itemStack)
config.set("reward.item", encoded).save()

// 序列化物品堆栈数组
val items = arrayOf(
    ItemStack(Material.DIAMOND),
    ItemStack(Material.GOLD_INGOT),
    null // 空槽位
)
val encodedArray = InventorySerializationUtils.encodeItemStacksToString(items)

// 序列化整个物品栏
val inventory = player.inventory
val serializableInventory = SerializableInventory.of(inventory, "Player Inventory")
config.set("backup.inventory", serializableInventory).save()

// 反序列化
val restoredInventory = config.getOrNull<SerializableInventory>("backup.inventory")
    ?.toInventory()
```

### 版本兼容性

配置模块支持自动补全缺失的配置项，解决版本升级时的配置兼容性问题：

```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 自动补全缺失的配置项
        touchConfigWithMerge("config.yml", createBackup = true)

        // 使用默认数据对象补全
        touchConfigWithMerge("settings.yml", { ServerSettings() }, createBackup = true)

        // 正常加载配置
        val config = configManager.parse<ServerConfig>("config.yml")
    }

    override fun reloadPlugin() {
        // 重新加载时也补全配置
        touchConfigWithMerge("config.yml", createBackup = false)

        // 重新加载配置
        val config = configManager.parse<ServerConfig>("config.yml")
    }
}
```

**工作原理**：
- 保留现有配置值不变
- 只添加缺失的配置项，使用默认值
- 支持深度嵌套的配置结构
- 可选择性创建备份文件

**使用场景**：
- 插件版本升级时自动补全新配置项
- 用户配置文件缺失部分配置时自动修复
- 开发过程中配置结构变更时保持兼容性

详细文档请参考：[配置文件版本兼容性](../docs/config/version-compatibility.md)

### 高级操作

```kotlin
// 链式操作
config.root {
    put("version", "1.0.0")
    put("debug", false)
}.objectPath("database") {
    put("host", "localhost")
    put("port", 3306)
}.arrayPath("features") {
    add("feature1")
    add("feature2")
}.save()

// 路径操作
config.path("server.settings") { node ->
    if (node.isObject) {
        val settings = node as ObjectNode
        settings.put("maintenance", false)
    }
}

// 条件操作
if (config.has("old.setting")) {
    val oldValue = config.get("old.setting", "")
    config.set("new.setting", oldValue)
        .remove("old.setting")
        .save()
}
```

## 格式插件系统

### 检查格式可用性

```kotlin
// 获取所有支持的格式
val supportedFormats = configManager.getSupportedFormats()
supportedFormats.forEach { (format, available) ->
    println("$format: ${if (available) "可用" else "缺失依赖"}")
}

// 获取可用格式
val availableFormats = configManager.getAvailableFormats()
println("可用格式: ${availableFormats.joinToString(", ")}")
```

### 自定义格式插件

```kotlin
class CustomFormatPlugin : ConfigFormatPlugin {
    override val formatName = "CUSTOM"
    override val supportedExtensions = setOf("custom")
    override val isCoreFormat = false

    override fun createMapper(): ObjectMapper {
        // 实现自定义格式的 ObjectMapper
        return ObjectMapper(CustomFactory())
            .registerKotlinModule()
            .registerBukkitSerializers()
    }

    override fun isAvailable(): Boolean {
        return try {
            Class.forName("com.example.CustomFactory")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
}

// 注册自定义格式
ConfigFormatRegistry.register(CustomFormatPlugin())
```

## 迁移指南

### 简化的 API 设计

```kotlin
// 统一的配置管理器
val configManager = ConfigManager(plugin)

// 简洁的配置操作
val config = configManager.get("config.yml")
val value = config.get("some.path", "default")
config.set("some.path", "new value").save()

// 类型安全的配置解析
data class MyConfig(val name: String, val port: Int)
val myConfig = configManager.parse<MyConfig>("config.yml")
```

### 格式支持检查

```kotlin
// 直接使用新的配置管理器
val configManager = ConfigManager(plugin)

// 检查格式兼容性
val supportedFormats = configManager.getSupportedFormats()
supportedFormats.forEach { (format, available) ->
    val status = if (available) "✓ 可用" else "✗ 缺失依赖"
    logger.info("$format: $status")
}
```

## 性能优化

### 缓存配置

```kotlin
// 自定义缓存
val customCache = LRUCache<String, Any>(32)
configManager.setCache(customCache)

// 禁用缓存
configManager.setCache(null)

// 使用缓存控制
val config1 = configManager.get("config.yml", useCache = true)  // 使用缓存
val config2 = configManager.get("config.yml", useCache = false) // 跳过缓存
```

### 批量操作

```kotlin
// 批量设置
config.root {
    put("setting1", "value1")
    put("setting2", "value2")
    put("setting3", "value3")
}.save() // 一次性保存

// 批量读取
data class Settings(
    val setting1: String,
    val setting2: String,
    val setting3: String
)
val settings = configManager.parse<Settings>("config.yml")
```

## CorePluginConfig集成

Config模块提供了`JacksonCorePluginConfig`类，这是`CorePluginConfig`的Jackson注解版本，支持直接序列化到配置文件。

> 📖 **迁移指南**: 如果你正在从旧的配置方式迁移，请参考 [MIGRATION_GUIDE.md](MIGRATION_GUIDE.md)

### 三种配置继承方式

#### 1. 继承方式（推荐用于简单插件）

插件配置类直接继承`JacksonCorePluginConfig`：

```kotlin
@JsonInclude(JsonInclude.Include.NON_NULL)
data class MyPluginConfig(
    @JsonProperty("my-setting")
    val mySetting: String = "default",

    @JsonProperty("my-number")
    val myNumber: Int = 42
) : JacksonCorePluginConfig()

class MyPlugin : BasePlugin() {
    fun getPluginConfig(): MyPluginConfig {
        configManager.touchWithMerge("config.yml", createBackup = true)
        return configManager.parse<MyPluginConfig>("config.yml")
    }

    override fun getCoreConfig(): CorePluginConfig {
        return getPluginConfig() // 直接返回，因为继承了CorePluginConfig
    }
}
```

#### 2. 组合方式（推荐用于复杂插件）

插件配置类包含core字段：

```kotlin
@JsonInclude(JsonInclude.Include.NON_NULL)
data class MyPluginConfig(
    @JsonProperty("core")
    val core: JacksonCorePluginConfig = JacksonCorePluginConfig(),

    @JsonProperty("my-settings")
    val mySettings: MySettings = MySettings(),

    @JsonProperty("advanced")
    val advanced: AdvancedConfig = AdvancedConfig()
)

class MyPlugin : BasePlugin() {
    fun getPluginConfig(): MyPluginConfig {
        configManager.touchWithMerge("config.yml", createBackup = true)
        return configManager.parse<MyPluginConfig>("config.yml")
    }

    override fun getCoreConfig(): CorePluginConfig {
        return getPluginConfig().core
    }
}
```

#### 3. 自定义方式（推荐用于独特需求）

插件完全自定义配置，临时构造CoreConfig：

```kotlin
@JsonInclude(JsonInclude.Include.NON_NULL)
data class MyPluginConfig(
    @JsonProperty("debug-mode")
    val debugMode: Boolean = false,

    @JsonProperty("log-level")
    val logLevel: String = "INFO",

    @JsonProperty("my-settings")
    val mySettings: MySettings = MySettings()
)

class MyPlugin : BasePlugin() {
    fun getPluginConfig(): MyPluginConfig {
        configManager.touchWithMerge("config.yml", createBackup = true)
        return configManager.parse<MyPluginConfig>("config.yml")
    }

    override fun getCoreConfig(): CorePluginConfig {
        val config = getPluginConfig()
        return object : CorePluginConfig() {
            override fun getLoggingConfig(): LoggingConfig = object : LoggingConfig() {
                override val debugEnabled: Boolean = config.debugMode
                override val fileLoggingEnabled: Boolean = config.logLevel == "DEBUG"
            }
        }
    }
}
```

### 配置方式选择指南

- **继承方式**: 适用于插件配置简单、不会与core配置产生属性冲突的情况
- **组合方式**: 适用于插件配置复杂、希望明确分离core配置和插件配置的情况
- **自定义方式**: 适用于插件有独特配置风格、希望完全控制配置结构的情况

### 重要的最佳实践

**标准配置管理模式**：所有插件都应该遵循以下固定模式：

```kotlin
class MyPlugin : BasePlugin() {
    // ✅ 标准写法（基本固定）
    fun getPluginConfig(): MyPluginConfig {
        configManager.touchWithMerge("config.yml", createBackup = true)
        return configManager.parse<MyPluginConfig>("config.yml")
    }

    // ✅ 根据选择的配置继承方式实现
    override fun getCoreConfig(): CorePluginConfig {
        // 具体实现取决于选择的方式
    }
}
```

**关键要点**：
- `getPluginConfig()`的写法基本固定，只需要修改配置类型和文件名
- `touchWithMerge()`确保配置文件存在并处理版本升级
- `getCoreConfig()`的实现取决于选择的配置继承方式

## 最佳实践

1. **格式选择**: 优先使用 JSON 和 YAML，它们是核心格式，无需额外依赖
2. **类型安全**: 使用数据类和类型安全解析，避免运行时类型错误
3. **缓存策略**: 根据配置文件大小和访问频率调整缓存设置
4. **资源管理**: 确保在插件关闭时调用 `configManager.close()`
5. **错误处理**: 使用 `getOrNull()` 方法处理可能不存在的配置项
6. **配置继承**: 根据插件复杂度选择合适的CorePluginConfig集成方式

## 故障排除

### 常见问题

1. **格式不支持**: 检查是否添加了对应的可选依赖
2. **序列化失败**: 确保类实现了正确的序列化接口
3. **缓存问题**: 使用 `useCache = false` 强制重新加载
4. **路径错误**: 检查路径分隔符和大小写

### 调试技巧

```kotlin
// 检查格式可用性
if (!configManager.getAvailableFormats().contains("TOML")) {
    plugin.logger.warning("TOML 格式不可用，请添加依赖")
}

// 输出配置内容
val configContent = config.toString("JSON")
plugin.logger.info("配置内容: $configContent")

// 验证配置结构
if (!config.has("required.setting")) {
    plugin.logger.warning("缺少必需的配置项: required.setting")
}
```
