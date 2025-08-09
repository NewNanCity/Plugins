# I18n 模块用户教程

欢迎使用现代化的国际化框架！本教程将带您从零开始学习如何使用 I18n 模块构建多语言支持系统。

## ⚠️ 关键概念 - 动态字段说明

**重要**：I18n 模块默认**不包含**任何内置的动态字段。文档中看到的所有变量（如 `{0}`、`{1}` 等位置参数）需要在使用时传递具体值。

- ✅ **内置功能**：基础文本替换、多语言回退、缓存机制
- ❌ **非内置功能**：任何动态字段（需要传参支持）
- 🔧 **使用方式**：通过位置参数 `{0}`, `{1}` 传递动态值

## 📚 文档目录

### 基础教程
- [📖 介绍](intro.md) - 5分钟快速了解I18n模块
- [🚀 快速开始](quick-start.md) - 第一个多语言配置
- [🎯 基础概念](concepts.md) - 核心概念详解

### 功能指南
- [🌍 多语言支持](languages.md) - 语言注册和管理
- [📝 模板系统](templates.md) - {0},{1}格式的位置参数
- [🔄 回退机制](fallback.md) - 三级回退策略
- [🔥 动态重载](hot-reload.md) - 运行时重新加载语言文件
- [💾 缓存优化](caching.md) - 高性能缓存机制
- [📄 多格式支持](formats.md) - 继承Config模块的格式支持

### 规范和模板
- [📋 i18n Key命名规范](naming-conventions.md) - 统一的键名规范和最佳实践
- [📄 通用模板文件](template.yml) - 中文模板文件
- [📄 英文模板文件](template_en.yml) - 英文模板文件

### 高级主题
- [👤 用户语言](user-language.md) - 每个玩家的语言设置
- [🔄 动态切换](language-switching.md) - 运行时语言切换
- [🌐 翻译API](translation-api.md) - 在线翻译服务集成
- [📊 使用统计](statistics.md) - 语言使用统计分析

### 参考资料
- [📋 API参考](api-reference.md) - 完整API文档
- [💡 最佳实践](best-practices.md) - 开发建议和模式
- [🔧 故障排除](troubleshooting.md) - 常见问题解决
- [📝 示例代码](examples.md) - 完整示例集合

## 🎯 快速导航

### 我想要...
- **创建第一个多语言配置** → [快速开始](quick-start.md)
- **注册多种语言** → [多语言支持](languages.md)
- **使用模板变量** → [模板系统](templates.md)
- **设置回退语言** → [回退机制](fallback.md)
- **热重载语言文件** → [动态重载](hot-reload.md)
- **优化性能** → [缓存优化](caching.md)
- **支持不同格式** → [多格式支持](formats.md)
- **解决问题** → [故障排除](troubleshooting.md)

## 🆕 最新特性

- **多格式支持** - 继承Config模块的多格式支持（JSON、YAML、TOML等）
- **模板替换** - 支持`{0}`, `{1}`格式的位置参数替换
- **三级回退** - 主语言 → 默认语言 → 原文本的智能回退
- **动态重载** - 支持运行时重新加载语言文件
- **缓存优化** - 内置缓存机制提升性能
- **类型安全** - 完整的Kotlin类型支持
- **资源管理** - 实现Terminable接口，自动清理资源
- **插件集成** - 与BasePlugin和MessageManager无缝集成

## 🌍 支持的语言格式

> **⚠️ 重要说明**：以下示例中的所有动态字段（如`{0}`、`{1}`、`{2}`等位置参数）需要在使用时传递具体值。I18n 模块使用位置参数进行文本替换。

### 核心格式
```yaml
# zh_CN.yml (简体中文)
welcome:
  message: "欢迎 {0} 来到服务器！"
  first_join: "这是 {0} 第一次加入服务器"

server:
  status: "服务器状态：{0}"
  players: "在线玩家：{0}/{1}"

economy:
  balance: "您的余额：{0} 金币"
  transfer: "成功转账 {0} 金币给 {1}"
```

```json
// en_US.json (English)
{
  "welcome": {
    "message": "Welcome {0} to the server!",
    "first_join": "This is {0}'s first time joining"
  },
  "server": {
    "status": "Server status: {0}",
    "players": "Online players: {0}/{1}"
  },
  "economy": {
    "balance": "Your balance: {0} coins",
    "transfer": "Successfully transferred {0} coins to {1}"
  }
}
```

### 扩展格式
```toml
# ja_JP.toml (日本語)
[welcome]
message = "ようこそ {0} さん！"
first_join = "{0} さんの初回参加です"

[server]
status = "サーバー状態：{0}"
players = "オンラインプレイヤー：{0}/{1}"
```

## 🚀 快速预览

### 基础用法
```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 简单的语言管理器配置
        setupLanguageManager(
            languageFiles = mapOf(
                Locale.SIMPLIFIED_CHINESE to "lang/zh_CN.yml",
                Locale.US to "lang/en_US.yml",
                Locale.JAPAN to "lang/ja_JP.toml"
            ),
            mergeWithTemplate = true,
            createBackup = false,
            majorLanguage = Locale.SIMPLIFIED_CHINESE,
            defaultLanguage = Locale.US
        )

        // 使用多语言消息
        messager.info(player, "<%welcome.message%>", player.name)
    }
}
```

### 位置参数替换
```kotlin
override fun onPluginEnable() {
    // 使用位置参数进行文本替换
    // 语言文件中：welcome.message: "欢迎 {0} 来到 {1}！"
    messager.info(player, "<%welcome.message%>", player.name, server.name)
    // 结果："欢迎 Steve 来到 我的服务器！"

    // 语言文件中：server.players: "在线玩家：{0}/{1}"
    messager.info(player, "<%server.players%>", server.onlinePlayers.size, server.maxPlayers)
    // 结果："在线玩家：5/20"

    // 语言文件中：economy.transfer: "成功转账 {0} 金币给 {1}"
    messager.info(player, "<%economy.transfer%>", 100, targetPlayer.name)
    // 结果："成功转账 100 金币给 Alex"
}
```

### 动态语言切换
```kotlin
// 为特定玩家设置语言
languageManager.setPlayerLanguage(player, Locale.US)

// 获取玩家的语言设置
val playerLanguage = languageManager.getPlayerLanguage(player)

// 使用玩家语言发送消息，传递参数
val message = languageManager.provideLanguage("<%welcome.message%>", player, player.name)
player.sendMessage(message)
```

### 回退机制
```kotlin
// 配置回退策略
languageManager
    .setMajorLanguage(Locale.SIMPLIFIED_CHINESE)  // 主语言
    .setDefaultLanguage(Locale.US)                // 默认语言

// 查找顺序：
// 1. 玩家设置的语言（如果有）
// 2. 主语言（简体中文）
// 3. 默认语言（英语）
// 4. 原始文本（如果都找不到）
```

### 动态重载
```kotlin
// 重新加载所有语言文件
languageManager.reload()

// 重新加载特定语言
languageManager.reloadLanguage(Locale.SIMPLIFIED_CHINESE)

// 监听文件变化自动重载
languageManager.enableAutoReload(true)
```

### 缓存优化
```kotlin
// 配置缓存策略
languageManager.setCacheConfig(
    type = CacheType.LRU,
    capacity = 1000,
    expireAfterAccess = Duration.ofMinutes(30)
)

// 清理缓存
languageManager.clearCache()

// 获取缓存统计
val stats = languageManager.getCacheStats()
logger.info("缓存命中率: ${stats.hitRate * 100}%")
```

## 🔧 集成方式

### 与MessageManager集成
```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 使用 setupLanguageManager 配置语言管理器
        setupLanguageManager(
            languageFiles = mapOf(
                Locale.SIMPLIFIED_CHINESE to "lang/zh_CN.yml",
                Locale.US to "lang/en_US.yml"
            ),
            majorLanguage = Locale.SIMPLIFIED_CHINESE,
            defaultLanguage = Locale.US
        )

        // 现在MessageManager会自动使用多语言，支持位置参数
        messager.info(player, "<%welcome.message%>", player.name)
        messager.warning(player, "<%error.insufficient_permission%>")
    }
}
```

### 独立使用
```kotlin
class MyService {
    private val languageManager = LanguageManager(plugin, configManager)

    fun sendWelcomeMessage(player: Player) {
        val message = languageManager.provideLanguage("<%welcome.message%>", player, player.name)
        player.sendMessage(message)
    }

    fun getLocalizedText(key: String, locale: Locale, vararg args: Any): String {
        return languageManager.getText(key, locale, *args)
    }
}
```

## 🤝 贡献

如果您发现文档中的错误或有改进建议，欢迎提交Issue或Pull Request。

---

**开始您的I18n开发之旅** → [📖 介绍](intro.md)
