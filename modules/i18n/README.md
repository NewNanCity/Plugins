# I18n Module

国际化模块，提供完整的多语言支持系统。

## 功能特性

### 🌍 核心特性

基于配置模块的多语言支持系统，提供：

- **多格式支持**: 继承config模块的多格式支持（JSON、YAML、TOML等）
- **模板替换**: 支持 `<%key%>` 格式的模板变量替换
- **回退机制**: 主语言 → 默认语言 → 原文本的三级回退
- **动态重载**: 支持运行时重新加载语言文件
- **缓存优化**: 内置缓存机制提升性能

### 🔧 技术特性

- **类型安全**: 完整的Kotlin类型支持
- **资源管理**: 实现Terminable接口，自动清理资源
- **插件集成**: 与BasePlugin无缝集成
- **配置驱动**: 基于config模块的统一配置管理

## 模块结构

```
modules/i18n/
├── src/main/kotlin/city/newnan/i18n/
│   ├── LanguageManager.kt             # 语言管理器
│   ├── Language.kt                    # 单语言文件封装
│   ├── I18nConfig.kt                  # 国际化配置
│   ├── exceptions/                    # 异常定义
│   │   └── I18nExceptions.kt
│   └── examples/                      # 使用示例
│       └── I18nExample.kt
├── src/test/kotlin/                   # 测试代码
├── README.md                          # 模块文档
└── build.gradle.kts                   # 构建配置
```

## 依赖关系

```
i18n
├── core (api)                         # 核心功能
├── config (api)                       # 配置管理
└── paper-api (compileOnly)            # Bukkit/Paper API
```

## 使用示例

### 基础用法

```kotlin
class MyPlugin : BasePlugin() {
    private lateinit var languageManager: LanguageManager

    override fun onPluginEnable() {
        // 创建语言管理器（使用BasePlugin的configManager属性）
        languageManager = LanguageManager(this, configManager)
            .register(Locale.SIMPLIFIED_CHINESE, "lang/zh_CN.yml")
            .register(Locale.US, "lang/en_US.yml")
            .setMajorLanguage(Locale.SIMPLIFIED_CHINESE)
            .setDefaultLanguage(Locale.US)

        // 通过BasePlugin统一设置语言提供者
        setLanguageProvider(languageManager)

        // 使用多语言消息
        messager.info("<%welcome.message%>")
    }

    /**
     * 重载配置方法 - 推荐实现
     */
    fun reloadPlugin() {
        try {
            // 重新加载语言管理器
            languageManager.reload()

            // 重新设置语言提供者
            setLanguageProvider(languageManager)

            logger.info("<%config.reloaded%>")
        } catch (e: Exception) {
            logger.error("<%config.reload_failed%>", e)
            throw e
        }
    }
}
```

### 高级配置

```kotlin
// 支持多种配置格式
languageManager
    .register(Locale.SIMPLIFIED_CHINESE, "lang/zh_CN.json")
    .register(Locale.US, "lang/en_US.toml")
    .register(Locale.JAPAN, "lang/ja_JP.xml")
```

### 模板变量

语言文件示例 (`lang/zh_CN.yml`):
```yaml
welcome:
  message: "欢迎 <%player%> 来到服务器！"
  first_join: "这是 <%player%> 第一次加入服务器"

server:
  status: "服务器状态：<%status%>"
  players: "在线玩家：<%current%>/<%max%>"
```

使用示例：
```kotlin
// 简单替换
val message = languageManager.provideLanguage("<%welcome.message%>")

// 与MessageManager集成
messager.printf(player, true, true, "<%welcome.message%>")
```

## 设计原则

### 1. 模块化设计
- 独立的国际化功能模块
- 清晰的接口定义
- 最小化模块间耦合

### 2. 配置驱动
- 基于config模块的统一配置管理
- 支持多种配置格式
- 运行时配置重载

### 3. 性能优化
- 内置缓存机制
- 懒加载策略
- 批量操作支持

### 4. 易用性
- 简洁的API设计
- 链式调用支持
- 完善的文档和示例

## 扩展计划

作为独立的国际化模块，未来可能的扩展：

- **多语言检测**: 自动检测用户语言偏好，每个玩家对应的语言设置可以不同
- **动态语言切换**: 运行时切换语言
- **语言包热更新**: 支持在线更新语言包
- **翻译API集成**: 集成在线翻译服务
- **语言统计**: 提供详细的使用统计
