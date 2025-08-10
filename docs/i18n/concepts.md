# I18N 模块基础概念

> 📋 **状态**: 文档规划中，内容正在完善

## 核心概念

### 国际化 (Internationalization, I18N)

国际化是指设计和开发软件产品以支持多种语言和地区的过程。I18N 模块提供了完整的国际化支持。

### 本地化 (Localization, L10N)

本地化是指将软件产品适配到特定语言和地区的过程，包括翻译文本、调整格式等。

### 语言管理器 (LanguageManager)

LanguageManager 是 I18N 模块的核心组件，负责：
- 语言文件的加载和管理
- 语言键的解析和格式化
- 玩家语言偏好的管理
- 动态语言切换支持

### 消息管理器 (MessageManager)

MessageManager 提供了便捷的消息处理功能：

```kotlin
// 基本用法
val message = messageManager.sprintf(player, "messages.welcome", playerName)

// 支持多参数
val levelMessage = messageManager.sprintf(
    player, 
    "messages.level-up", 
    newLevel, 
    experience
)

// 支持组件格式
val component = messageManager.sprintfComponent(
    player,
    "messages.colored-text"
)
```

## 语言文件结构

### 层次化组织

推荐使用层次化的语言键结构：

```yaml
# 推荐的结构
plugin:
  name: "插件名称"
  
messages:
  player:
    join: "玩家加入"
    quit: "玩家离开"
  system:
    loading: "加载中"
    
commands:
  help: "帮助"
  reload: "重载"
  
errors:
  permission: "权限不足"
  not-found: "未找到"
```

### 命名规范

- 使用小写字母和连字符
- 采用层次化结构
- 保持一致的命名风格
- 避免过深的嵌套

```yaml
# 好的命名
messages:
  player-join: "玩家加入"
  level-up: "升级消息"
  
# 不推荐的命名
Messages:
  PlayerJoin: "玩家加入"
  levelUp: "升级消息"
```

## 参数格式化

### 位置参数

使用 `{0}`, `{1}`, `{2}` 等表示参数位置：

```yaml
messages:
  welcome: "欢迎 {0} 来到 {1} 服务器！"
  level-info: "玩家 {0} 当前等级 {1}，经验 {2}"
```

```kotlin
// 使用示例
messageManager.sprintf(player, "messages.welcome", playerName, serverName)
messageManager.sprintf(player, "messages.level-info", name, level, exp)
```

### 格式化选项

支持多种格式化选项：

```yaml
# 数字格式化
numbers:
  currency: "¥{0,number,#,##0.00}"
  percentage: "{0,number,percent}"
  
# 日期格式化
dates:
  short: "{0,date,short}"
  long: "{0,date,long}"
  custom: "{0,date,yyyy-MM-dd HH:mm:ss}"
```

## 语言检测和切换

### 自动语言检测

系统可以根据多种方式检测玩家语言：

1. **玩家设置** - 玩家主动设置的语言偏好
2. **客户端语言** - 从客户端获取的语言信息
3. **地理位置** - 基于 IP 地址的地理位置推断
4. **默认语言** - 服务器配置的默认语言

### 语言切换机制

```kotlin
// 设置玩家语言
fun setPlayerLanguage(player: Player, language: String) {
    languageManager.setPlayerLanguage(player.uniqueId, language)
    
    // 发送确认消息
    val message = messageManager.sprintf(
        player,
        "settings.language-changed",
        getLanguageDisplayName(language)
    )
    player.sendMessage(message)
}

// 获取玩家语言
fun getPlayerLanguage(player: Player): String {
    return languageManager.getPlayerLanguage(player.uniqueId)
        ?: languageManager.getDefaultLanguage()
}
```

## 缓存和性能

### 语言文件缓存

- 语言文件在启动时加载到内存
- 支持热重载，无需重启服务器
- 使用 LRU 缓存优化频繁访问的语言键

### 格式化缓存

```kotlin
// 缓存格式化结果
class MessageCache {
    private val cache = ConcurrentHashMap<String, Component>()
    
    fun getCachedMessage(key: String, args: Array<Any>): Component? {
        val cacheKey = "$key:${args.contentHashCode()}"
        return cache[cacheKey]
    }
    
    fun putCachedMessage(key: String, args: Array<Any>, result: Component) {
        val cacheKey = "$key:${args.contentHashCode()}"
        cache[cacheKey] = result
    }
}
```

## 回退机制

### 语言回退链

当某个语言键在当前语言中不存在时，系统会按以下顺序查找：

1. **当前语言** - 玩家设置的语言
2. **基础语言** - 同语言族的基础语言（如 zh_CN → zh）
3. **默认语言** - 服务器配置的默认语言
4. **英语** - 通用的回退语言
5. **语言键本身** - 最后的回退选项

```kotlin
fun resolveMessage(player: Player, key: String): String {
    val playerLang = getPlayerLanguage(player)
    
    // 尝试当前语言
    languageManager.getMessage(playerLang, key)?.let { return it }
    
    // 尝试基础语言
    val baseLang = getBaseLanguage(playerLang)
    languageManager.getMessage(baseLang, key)?.let { return it }
    
    // 尝试默认语言
    val defaultLang = languageManager.getDefaultLanguage()
    languageManager.getMessage(defaultLang, key)?.let { return it }
    
    // 尝试英语
    languageManager.getMessage("en_US", key)?.let { return it }
    
    // 返回键本身
    return key
}
```

## 多格式支持

### YAML 格式 (推荐)

```yaml
messages:
  welcome: "欢迎 {0}！"
  level-up: "恭喜升级到 {0} 级！"
```

### JSON 格式

```json
{
  "messages": {
    "welcome": "欢迎 {0}！",
    "level-up": "恭喜升级到 {0} 级！"
  }
}
```

### Properties 格式

```properties
messages.welcome=欢迎 {0}！
messages.level-up=恭喜升级到 {0} 级！
```

## 相关文档

- [🚀 快速开始](quick-start.md) - 快速上手指南
- [📝 命名规范](naming-conventions.md) - 语言键命名规范
- [📄 语言模板](templates.md) - 标准语言模板

---

**📝 注意**: 此文档正在完善中，更多概念说明请参考 [API 参考](api-reference.md)。
