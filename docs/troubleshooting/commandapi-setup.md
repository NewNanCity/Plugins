# 命令系统迁移说明

## ⚠️ 重要通知：已迁移到 Cloud 框架

**本项目已从 CommandAPI 迁移到 Cloud 命令框架**。如果您遇到与命令系统相关的问题，请参考新的文档。

## 🚀 新的命令系统

### Cloud 框架优势

- **跨平台支持**：支持 Paper、Velocity、Fabric 等多个平台
- **注解驱动**：使用注解方式编写命令，更加简洁
- **类型安全**：提供完整的类型安全保障
- **自动补全**：内置强大的自动补全系统
- **i18n 集成**：与项目的国际化系统深度集成

### 快速迁移指南

如果您有使用 CommandAPI 的旧代码，请按以下步骤迁移：

1. **移除 CommandAPI 依赖**
2. **添加 Cloud 框架依赖**
3. **重写命令类使用注解**
4. **更新 plugin.yml**（不再需要 CommandAPI 依赖）

详细迁移指南请参考：[命令系统详解](../core/commands.md)

## 🔧 故障排除

### 如果您仍在使用 CommandAPI

如果您的插件仍在使用 CommandAPI 并遇到问题：

### 问题描述

如果您在运行使用 CommandAPI 的插件时遇到以下错误：

```
java.lang.NoClassDefFoundError: dev/jorel/commandapi/CommandAPICommand
```

这表示 CommandAPI 依赖配置不正确。

### 解决方案

#### 1. 安装 CommandAPI 插件

首先，确保您的服务器上安装了 CommandAPI 插件：

1. 从 [CommandAPI GitHub Releases](https://github.com/CommandAPI/CommandAPI/releases/latest) 下载最新版本的 `CommandAPI-XXX.jar`
2. 将下载的 jar 文件放入服务器的 `plugins` 文件夹
3. 重启服务器

#### 2. 插件依赖配置

所有使用 CommandAPI 的插件都必须在 `plugin.yml` 中声明对 CommandAPI 的依赖。

**注意**：以下插件信息仅适用于尚未迁移到 Cloud 框架的旧版本插件。

#### 构建配置示例

在插件的 `build.gradle.kts` 中：

```kotlin
// 配置插件元数据
tasks.processResources {
    val pluginProperties = mapOf(
        "version" to project.version,
        "group" to project.group,
        "name" to "YourPlugin",
        "main" to "your.package.YourPlugin",
        "apiVersion" to "1.20",
        "description" to project.description,
        "website" to "https://newnan.city",
        "authors" to listOf("Sttot", "NSrank", "AI"),
        "prefix" to "YourPlugin",
        "load" to "STARTUP",
        "depend" to listOf("CommandAPI"), // 重要：声明 CommandAPI 依赖
        "softdepend" to emptyList<String>(),
        "loadbefore" to emptyList<String>()
    )

    // ... 其余配置
}
```

生成的 `plugin.yml` 将包含：

```yaml
name: YourPlugin
version: 2.0.0
main: your.package.YourPlugin
api-version: 1.20
depend: [CommandAPI]
```

### 3. 开发环境配置

在开发环境中，确保正确添加了 CommandAPI 依赖：

```kotlin
dependencies {
    // CommandAPI 核心
    compileOnly("dev.jorel:commandapi-bukkit-core:10.0.1")

    // 如果使用 Kotlin DSL
    compileOnly("dev.jorel:commandapi-bukkit-kotlin:10.0.1")
}
```

### 4. 验证安装

重新构建并部署插件后，检查服务器日志：

1. CommandAPI 插件应该在您的插件之前加载
2. 您的插件应该成功加载，没有 `NoClassDefFoundError`

### 5. 常见问题

#### Q: 为什么需要 CommandAPI 插件？
A: CommandAPI 使用插件架构，需要在服务器上安装 CommandAPI 插件来提供运行时支持。

#### Q: 可以将 CommandAPI 打包到插件中吗？
A: 不推荐。CommandAPI 设计为独立插件，多个插件可以共享同一个 CommandAPI 实例。

#### Q: 如何检查 CommandAPI 是否正确安装？
A: 在服务器控制台运行 `/plugins` 命令，应该看到 CommandAPI 在插件列表中。

### 6. 相关链接

**新的命令系统（推荐）**：
- [命令系统详解](../core/commands.md)
- [最佳实践指南](../core/best-practices.md#命令系统最佳实践)
- [Cloud 官方文档](https://cloud.incendo.org/)

**CommandAPI（仅供参考）**：
- [CommandAPI 官方文档](https://commandapi.jorel.dev/)
- [CommandAPI GitHub](https://github.com/CommandAPI/CommandAPI)
- [CommandAPI 安装指南](https://commandapi.jorel.dev/user-setup/install.html)

---

**建议**：对于新项目，强烈推荐使用 Cloud 命令框架而不是 CommandAPI。
