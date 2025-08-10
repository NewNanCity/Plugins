# I18N 模块快速开始

> 📋 **状态**: 文档规划中，内容正在完善

## 概述

I18N 模块提供了完整的国际化和本地化支持，让您的插件能够支持多种语言。本页面将指导您快速上手使用 I18N 模块。

## 快速开始步骤

### 1. 添加依赖

```kotlin
// 在您的插件中添加 i18n 模块依赖
dependencies {
    implementation(project(":modules:i18n"))
}
```

### 2. 创建语言文件

在插件的 `resources/languages/` 目录下创建语言文件：

```yaml
# languages/zh_CN.yml
messages:
  welcome: "欢迎来到服务器，{0}！"
  goodbye: "再见，{0}！"
  level-up: "恭喜！你升到了 {0} 级！"

commands:
  help: "帮助命令"
  reload: "重载配置"

errors:
  no-permission: "你没有权限执行此命令"
  player-not-found: "找不到玩家：{0}"
```

```yaml
# languages/en_US.yml
messages:
  welcome: "Welcome to the server, {0}!"
  goodbye: "Goodbye, {0}!"
  level-up: "Congratulations! You reached level {0}!"

commands:
  help: "Help command"
  reload: "Reload configuration"

errors:
  no-permission: "You don't have permission to execute this command"
  player-not-found: "Player not found: {0}"
```

### 3. 在插件中使用

```kotlin
class MyPlugin : BasePlugin() {
    
    override fun onPluginEnable() {
        super.onPluginEnable()
        
        // 发送欢迎消息
        server.onlinePlayers.forEach { player ->
            val message = messageManager.sprintf(
                player, 
                "messages.welcome", 
                player.name
            )
            player.sendMessage(message)
        }
    }
}
```

### 4. 在命令中使用

```kotlin
@Command("greet")
class GreetCommand : BaseCommand() {
    
    @Default
    fun greet(sender: CommandSender, @Argument("player") target: Player) {
        val message = messageManager.sprintf(
            sender,
            "messages.welcome",
            target.name
        )
        sender.sendMessage(message)
    }
}
```

## 语言键命名规范

推荐使用层次化的语言键命名：

```yaml
# 推荐的命名结构
plugin:
  name: "我的插件"
  version: "1.0.0"

messages:
  player:
    join: "玩家 {0} 加入了游戏"
    quit: "玩家 {0} 离开了游戏"
  
commands:
  teleport:
    success: "传送成功！"
    failed: "传送失败：{0}"
    
errors:
  common:
    no-permission: "权限不足"
    invalid-args: "参数错误"
```

## 支持的语言格式

- **YAML** - 推荐格式，支持层次结构
- **JSON** - 标准格式，易于程序处理
- **Properties** - 传统格式，简单直接

## 动态语言切换

```kotlin
// 为玩家设置语言
fun setPlayerLanguage(player: Player, language: String) {
    messageManager.setPlayerLanguage(player, language)
    
    val confirmMessage = messageManager.sprintf(
        player,
        "settings.language-changed",
        language
    )
    player.sendMessage(confirmMessage)
}

// 获取玩家当前语言
fun getPlayerLanguage(player: Player): String {
    return messageManager.getPlayerLanguage(player)
}
```

## 相关文档

- [📖 模块介绍](intro.md) - 了解 I18N 模块的核心概念
- [📝 命名规范](naming-conventions.md) - 语言键命名最佳实践
- [🔧 配置指南](configuration.md) - 详细配置说明

## 下一步

- [基础概念](concepts.md) - 深入了解国际化系统设计
- [语言模板](templates.md) - 了解语言模板系统
- [API 参考](api-reference.md) - 完整的 API 文档

---

**📝 注意**: 此文档正在完善中，如有疑问请参考 [README](README.md) 或查看 [示例代码](examples.md)。
