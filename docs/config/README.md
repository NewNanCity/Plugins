# Config 模块用户教程

欢迎使用现代化的多格式配置管理框架！本教程将带您从零开始学习如何使用 Config 模块构建强大的配置系统。

## 📚 文档目录

### 基础教程
- [📖 介绍](intro.md) - 5分钟快速了解Config模块
- [🚀 快速开始](quick-start.md) - 第一个配置文件
- [🎯 基础概念](concepts.md) - 核心概念详解

### 功能指南
- [📄 多格式支持](formats.md) - JSON、YAML、TOML等格式
- [🔧 类型安全](type-safety.md) - 数据类绑定和泛型支持
- [💾 缓存机制](caching.md) - 高性能缓存策略
- [🎮 Bukkit集成](bukkit-integration.md) - 原生类型序列化
- [🔌 插件化扩展](plugins.md) - 自定义格式扩展
- [🛠️ DSL配置](dsl.md) - 流畅的配置API
- [⚙️ CorePluginConfig集成](core-integration.md) - 核心配置集成指南

### 高级主题
- [🔄 动态重载](hot-reload.md) - 运行时配置更新
- [✅ 配置验证](validation.md) - 数据验证和错误处理
- [🏗️ 架构设计](architecture.md) - 模块架构和设计模式
- [⚙️ 性能优化](performance.md) - 缓存和性能调优

### 参考资料
- [📋 API参考](api-reference.md) - 完整API文档
- [💡 最佳实践](best-practices.md) - 开发建议和模式
- [🔧 故障排除](troubleshooting.md) - 常见问题解决
- [📝 示例代码](examples.md) - 完整示例集合

## 🎯 快速导航

### 我想要...
- **创建第一个配置** → [快速开始](quick-start.md)
- **集成CorePluginConfig** → [CorePluginConfig集成](core-integration.md)
- **使用不同格式** → [多格式支持](formats.md)
- **类型安全配置** → [类型安全](type-safety.md)
- **提升性能** → [缓存机制](caching.md)
- **序列化Bukkit对象** → [Bukkit集成](bukkit-integration.md)
- **扩展新格式** → [插件化扩展](plugins.md)
- **使用DSL语法** → [DSL配置](dsl.md)
- **解决问题** → [故障排除](troubleshooting.md)

## 🆕 最新特性

- **多格式支持** - JSON、YAML（核心）+ TOML、XML、CSV、Properties、HOCON（可选）
- **类型安全** - 完整的Kotlin类型支持和泛型操作
- **Bukkit集成** - 原生支持Location、ItemStack、Material等类型
- **插件化架构** - 支持自定义格式扩展
- **高性能缓存** - 可配置的多级缓存策略
- **DSL语法** - 流畅的配置API设计
- **自动检测** - 根据文件扩展名自动识别格式
- **资源管理** - 实现Terminable接口，自动清理资源

## 🔧 依赖分析

### 核心格式（必需）
- **JSON**: Jackson Core (~350KB)
- **YAML**: Jackson YAML (~150KB)
- **总计**: ~500KB

### 可选格式（按需引入）
- **TOML**: Jackson TOML (~200KB)
- **XML**: Jackson XML (~800KB)
- **CSV**: Jackson CSV (~50KB)
- **Properties**: Jackson Properties (~30KB)
- **HOCON**: Jackson HOCON (~300KB)

### Bukkit序列化
- **Bukkit集成**: 自定义序列化器 (~50KB)

## 🎮 支持的Bukkit类型

### 基础类型
- `Location` - 位置信息（世界、坐标、朝向）
- `Vector` - 三维向量
- `Material` - 物品材质
- `Sound` - 音效类型
- `Particle` - 粒子效果

### 复杂类型
- `ItemStack` - 物品堆叠（包含NBT数据）
- `Inventory` - 背包内容
- `PotionEffect` - 药水效果
- `Enchantment` - 附魔信息
- `Color` - 颜色值

### 区域类型
- `Region` - 自定义区域类型
- `Position` - 简化的位置类型

## 🚀 快速预览

### 简单配置
```kotlin
// 创建配置
val config = configManager.get("server.yml")
config.set("server.name", "我的服务器")
config.set("server.maxPlayers", 100)
config.save()

// 读取配置
val serverName = config.getString("server.name")
val maxPlayers = config.getInt("server.maxPlayers")
```

### 类型安全配置
```kotlin
@Serializable
data class ServerConfig(
    val name: String,
    val maxPlayers: Int,
    val features: List<String>,
    val spawn: Location
)

// 保存配置
val config = ServerConfig(
    name = "我的服务器",
    maxPlayers = 100,
    features = listOf("pvp", "economy"),
    spawn = world.spawnLocation
)
configManager.save(config, "server.yml")

// 读取配置
val serverConfig = configManager.parse<ServerConfig>("server.yml")
```

### DSL配置
```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 使用DSL配置
        config("settings.yml") {
            cache {
                type = CacheType.LRU
                capacity = 100
            }

            autoSave = true
            format = ConfigFormat.YAML
        }
    }
}
```

## 🤝 贡献

如果您发现文档中的错误或有改进建议，欢迎提交Issue或Pull Request。

---

**开始您的Config开发之旅** → [📖 介绍](intro.md)
