# Config 模块介绍

## 🎯 什么是 Config 模块？

Config 模块是一个基于 Jackson 的多格式配置管理器，为 Minecraft 插件提供现代化、类型安全的配置管理解决方案。它支持多种配置格式，提供缓存机制，并与 BasePlugin 完美集成。

**5分钟快速了解：** Config 模块解决了传统配置管理中格式限制、类型不安全、序列化困难等问题，通过多格式支持、类型安全绑定、高性能缓存和 Bukkit 原生类型序列化，让配置管理变得简单而强大。

## 🔍 解决的问题

### 传统配置管理的痛点

1. **格式限制** - 只支持YAML格式，缺乏灵活性
2. **类型不安全** - 运行时类型转换容易出错
3. **序列化困难** - 复杂对象序列化支持不足
4. **性能问题** - 频繁读取配置文件影响性能
5. **缺乏验证** - 配置错误只能在运行时发现

### Config 模块的解决方案

✅ **多格式支持** - JSON、YAML、TOML、XML、CSV、Properties、HOCON
✅ **类型安全** - 完整的泛型类型推断和数据类绑定
✅ **高性能缓存** - LRU、LFU、无限容量缓存策略
✅ **Bukkit集成** - 原生支持Location、ItemStack等类型
✅ **插件化设计** - 支持自定义格式扩展

## 🆚 技术对比

### 与原生 Bukkit 配置对比

| 特性     | 原生 Bukkit | Config 模块             |
| -------- | ----------- | ----------------------- |
| 支持格式 | 仅YAML      | JSON、YAML、TOML、XML等 |
| 类型安全 | 运行时检查  | 编译时检查              |
| 序列化   | 基础支持    | 完整Jackson支持         |
| 缓存机制 | 无          | 多级缓存                |
| 扩展性   | 有限        | 插件化架构              |
| 性能     | 一般        | 高性能                  |

### 与其他配置库对比

| 配置库         | 优势                         | 劣势                 |
| -------------- | ---------------------------- | -------------------- |
| **Config模块** | 多格式、类型安全、Bukkit集成 | 学习成本             |
| Configurate    | 功能丰富                     | 复杂度高、体积大     |
| Gson           | 轻量级                       | 仅JSON、功能有限     |
| SnakeYAML      | YAML专用                     | 单一格式、类型不安全 |

## 🚀 快速示例

### 传统 Bukkit 配置
```java
// 传统方式
public class OldConfig {
    private FileConfiguration config;

    public void load() {
        config = YamlConfiguration.loadConfiguration(new File("config.yml"));

        // 类型不安全
        String name = config.getString("server.name", "默认服务器");
        int maxPlayers = config.getInt("server.maxPlayers", 20);

        // 复杂对象需要手动处理
        ConfigurationSection locationSection = config.getConfigurationSection("spawn");
        if (locationSection != null) {
            World world = Bukkit.getWorld(locationSection.getString("world"));
            double x = locationSection.getDouble("x");
            double y = locationSection.getDouble("y");
            double z = locationSection.getDouble("z");
            Location spawn = new Location(world, x, y, z);
        }
    }
}
```

### Config 模块配置
```kotlin
// 现代方式
@Serializable
data class ServerConfig(
    val name: String = "默认服务器",
    val maxPlayers: Int = 20,
    val spawn: Location,
    val features: List<String> = emptyList()
)

class ModernConfig : BasePlugin() {
    override fun onPluginEnable() {
        // 类型安全的配置读取（使用BasePlugin的configManager属性）
        val serverConfig = configManager.parse<ServerConfig>("server.yml")

        // 直接使用，无需类型转换
        logger.info("服务器名称: ${serverConfig.name}")
        logger.info("最大玩家数: ${serverConfig.maxPlayers}")
        logger.info("出生点: ${serverConfig.spawn}")

        // 修改和保存
        val updatedConfig = serverConfig.copy(
            maxPlayers = 100,
            features = listOf("pvp", "economy")
        )
        configManager.save(updatedConfig, "server.yml")
    }
}
```

## 🏗️ 核心架构

### 1. 多格式支持
- **核心格式** - JSON、YAML（必需，~500KB）
- **扩展格式** - TOML、XML、CSV、Properties、HOCON（可选）
- **自动识别** - 根据文件扩展名自动选择格式
- **插件化设计** - 支持自定义格式扩展

### 2. 类型安全系统
- **泛型支持** - 完整的泛型类型推断
- **数据类绑定** - 直接映射到Kotlin数据类
- **编译时检查** - 类型错误在编译时发现
- **空安全** - Kotlin空安全特性支持

### 3. 高性能缓存
- **多级缓存** - LRU、LFU、无限容量缓存
- **智能失效** - 文件修改时自动失效
- **内存优化** - 可配置的缓存容量
- **并发安全** - 线程安全的缓存实现

### 4. Bukkit 集成
- **原生类型** - Location、ItemStack、Material等
- **自动序列化** - 无需手动转换
- **完整支持** - 包含NBT数据和复杂属性
- **向后兼容** - 兼容原生Bukkit配置

## 📊 格式支持详情

### 核心格式（必需）
```kotlin
// JSON 格式
{
  "server": {
    "name": "我的服务器",
    "maxPlayers": 100
  }
}

// YAML 格式
server:
  name: "我的服务器"
  maxPlayers: 100
```

### 扩展格式（可选）
```toml
# TOML 格式
[server]
name = "我的服务器"
maxPlayers = 100
```

```xml
<!-- XML 格式 -->
<config>
  <server>
    <name>我的服务器</name>
    <maxPlayers>100</maxPlayers>
  </server>
</config>
```

```properties
# Properties 格式
server.name=我的服务器
server.maxPlayers=100
```

## 🎯 适用场景

### ✅ 推荐使用
- 需要多种配置格式的项目
- 要求类型安全的配置管理
- 需要序列化复杂Bukkit对象
- 高性能配置读取需求
- 大型插件项目

### ⚠️ 考虑因素
- 学习Jackson和序列化的成本
- 依赖包大小的影响
- 团队对新技术的接受度

## 🔄 迁移路径

### 从原生 Bukkit 配置迁移
1. **添加依赖** - 引入Config模块
2. **创建数据类** - 定义配置结构
3. **使用ConfigManager** - 替换FileConfiguration
4. **享受类型安全** - 无需手动类型转换

### 从其他配置库迁移
1. **分析现有配置** - 了解当前配置结构
2. **选择合适格式** - 根据需求选择格式
3. **逐步迁移** - 分模块逐步迁移
4. **测试验证** - 确保配置正确性

## 📈 性能优势

### 开发效率
- **减少代码量** - 自动序列化减少手动转换代码
- **类型安全** - 编译时发现配置错误
- **IDE支持** - 完整的代码补全和重构支持

### 运行时性能
- **缓存机制** - 减少文件I/O操作
- **批量操作** - 支持批量配置更新
- **内存优化** - 智能缓存管理

---

**准备开始？** → [🚀 快速开始](quick-start.md)
