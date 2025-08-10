# Config 模块快速开始

> 📋 **状态**: 文档规划中，内容正在完善

## 概述

Config 模块提供了强大的配置管理功能，支持多种格式和动态加载。本页面将指导您快速上手使用 Config 模块。

## 快速开始步骤

### 1. 添加依赖

```kotlin
// 在您的插件中添加 config 模块依赖
dependencies {
    implementation(project(":modules:config"))
}
```

### 2. 基本配置类

```kotlin
// 创建配置类
data class MyPluginConfig(
    val serverName: String = "默认服务器",
    val maxPlayers: Int = 100,
    val enableFeature: Boolean = true
) : BasePluginConfig()
```

### 3. 在插件中使用

```kotlin
class MyPlugin : BasePlugin() {
    private lateinit var config: MyPluginConfig
    
    override fun onPluginEnable() {
        // 加载配置
        config = configManager.getPluginConfig()
        super.onPluginEnable()
    }
}
```

## 相关文档

- [📖 模块介绍](intro.md) - 了解 Config 模块的核心概念
- [🔧 核心集成](core-integration.md) - 与 Core 模块的集成方式
- [⚠️ 故障排除](troubleshooting.md) - 常见问题解决方案

## 下一步

- [基础概念](concepts.md) - 深入了解配置系统的设计理念
- [支持的格式](formats.md) - 了解支持的配置文件格式
- [API 参考](api-reference.md) - 完整的 API 文档

---

**📝 注意**: 此文档正在完善中，如有疑问请参考 [README](README.md) 或查看 [示例代码](examples.md)。
