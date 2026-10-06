# 构建系统详解

## 概述

项目使用 Gradle Kotlin DSL 多项目构建，通过 buildSrc 中的约定插件统一管理所有子项目。

## BuildSrc 结构

```
buildSrc/src/main/kotlin/
├── Versions.kt                      # 集中版本管理
├── Dependencies.kt                  # 组织化依赖定义
└── newnancity-plugin.gradle.kts     # 插件约定配置
```

### Versions.kt
集中管理所有版本号，避免硬编码。

### Dependencies.kt
按功能组织依赖定义，包括：
- `Command` — Cloud 命令框架依赖
- `Database` — Exposed ORM、HikariCP
- `Config` — Jackson 多格式支持
- 其他外部依赖

### newnancity-plugin.gradle.kts
插件约定插件，为所有子项目提供统一配置：
- 应用 Kotlin、Shadow、RunPaper 插件
- 配置 Java 21 toolchain + Java 17 字节码目标
- 设置 Paper 仓库和核心依赖
- 提供 `configurePluginMetadata()` 函数生成 plugin.yml
- 处理依赖重定位避免冲突

## 构建约定模式

遵循 `Versions.kt → Dependencies.kt → build.gradle.kts` 模式：

1. **Versions.kt**：定义版本常量
2. **Dependencies.kt**：引用版本常量定义依赖字符串
3. **build.gradle.kts**：引用依赖字符串，避免硬编码

## Shadow JAR 配置

- **依赖重定位**：防止插件间冲突
- **服务文件合并**：保留 META-INF services
- **最小化打包**：只包含使用到的依赖

## Gradle 优化

- **并行执行**：启用以加快构建
- **配置缓存**：提升构建性能
- **Daemon 禁用**：CI/CD 兼容性

## 创建新插件

1. 在 `settings.gradle.kts` 中添加子项目
2. 创建 `build.gradle.kts`，应用 `newnancity-plugin` 约定
3. 主类继承 `BasePlugin`
4. 使用 `configurePluginMetadata()` 生成 plugin.yml
5. 实现必须的抽象方法

## 模块选择指南

| 模块         | 用途                  | 包大小参考 |
| ------------ | --------------------- | ---------- |
| **Core**     | 必需，BasePlugin 基础 | —          |
| **Config**   | 高级配置需求          | ~2.35MB    |
| **Database** | 数据持久化            | ~14MB      |
| **GUI**      | 现代 GUI 界面         | —          |
| **I18N**     | 多语言支持            | —          |
| **Network**  | HTTP/REST 集成        | —          |
