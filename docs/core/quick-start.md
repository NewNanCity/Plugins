# Core 模块快速开始

## 📦 引入模块

### 1. 添加依赖

在您的插件 `build.gradle.kts` 中添加：

```kotlin
dependencies {
    // Core 模块（必需）
    api(project(":core"))

    // PaperMC API
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}
```

### 2. 配置 Kotlin

确保您的项目支持 Kotlin：

```kotlin
plugins {
    kotlin("jvm") version "2.2.0"
}

kotlin {
    jvmToolchain(17)
}
```

## 🚀 第一个 Core 插件

### 1. 创建插件主类

```kotlin
package com.example.myplugin

import city.newnan.core.base.BasePlugin
import org.bukkit.event.player.PlayerJoinEvent

class MyPlugin : BasePlugin() {

    override fun onPluginLoad() {
        // 插件加载阶段（可选）
        logger.info("插件正在加载...")
    }

    override fun onPluginEnable() {
        logger.info("插件已启用！")

        // 注册事件监听器（不可重载的功能）
        subscribeEvent<PlayerJoinEvent> { event ->
            event.player.sendMessage("欢迎来到服务器！")
        }

        // 调用重载方法处理可重载的功能
        reloadPlugin()

        // 启动定时任务（不可重载的功能）
        runSyncRepeating(0L, 20L) { task ->
            // 每秒执行一次
            logger.info("在线玩家数: ${server.onlinePlayers.size}")
        }
    }

    override fun onPluginDisable() {
        // 插件禁用逻辑
        logger.info("插件已禁用")
        // 所有资源自动清理，无需手动处理
    }

    /**
     * 重载插件配置 - 必须实现
     */
    override fun reloadPlugin() {
        try {
            logger.info("正在重载配置...")

            // 重新设置语言管理器
            setupLanguageManager()

            // 其他可重载的逻辑...

            logger.info("配置重载完成！")
        } catch (e: Exception) {
            logger.error("配置重载失败", e)
            throw e
        }
    }

    private fun setupLanguageManager() {
        // 语言管理器设置逻辑
        // 详见下面的示例
    }
}
```

### 2. 配置 plugin.yml

```yaml
name: MyPlugin
version: 1.0.0
main: com.example.myplugin.MyPlugin
api-version: 1.21
author: YourName
description: 我的第一个 Core 插件

# 如果使用了其他模块，添加软依赖
softdepend: []
```

### 3. 构建和测试

```bash
# 构建插件
./gradlew build

# 生成的 JAR 文件位于 build/libs/ 目录
```

## 🎯 核心概念快速体验

### 自动资源管理

```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 创建自定义资源
        val myResource = MyCustomResource()

        // 绑定到插件生命周期 - 插件禁用时自动清理
        bind(myResource)

        // 或者使用 bind 绑定模块
        bind(MyModule())
    }
}

// 自定义资源实现 Terminable 接口
class MyCustomResource : Terminable {
    override fun close() {
        // 清理逻辑
        println("资源已清理")
    }
}

// 模块实现 Terminable 接口
class MyModule(private val plugin: BasePlugin) : Terminable {
    init {
        // 模块初始化逻辑
        plugin.bind(SomeResource())
    }

    override fun close() {
        // 模块清理逻辑
    }
}
```

### 事件处理

```kotlin
override fun onPluginEnable() {
    // 基础事件订阅
    subscribeEvent<PlayerJoinEvent> { event ->
        event.player.sendMessage("欢迎！")
    }

    // 带过滤器的事件处理
    subscribeEvent<PlayerJoinEvent>()
        .filter { it.player.isOp }
        .handler { event ->
            event.player.sendMessage("管理员欢迎！")
        }

    // 限制次数的事件处理
    subscribeEvent<PlayerJoinEvent>()
        .expireAfter(5) // 只处理前5次
        .handler { event ->
            event.player.sendMessage("限时欢迎！")
        }

    // 限制时间的事件处理
    subscribeEvent<PlayerJoinEvent>()
        .expireAfter(Duration.ofMinutes(10)) // 10分钟后过期
        .handler { event ->
            event.player.sendMessage("时限欢迎！")
        }
}
```

### 任务调度

```kotlin
override fun onPluginEnable() {
    // 同步任务
    runSync {
        // 在主线程执行
        logger.info("<%task.sync_executed%>")
    }

    // 异步任务
    runAsync {
        // 在异步线程执行
        logger.info("<%task.async_executed%>")
    }

    // 延迟任务
    runSyncLater(20L) { // 1秒后执行
        logger.info("<%task.delayed_executed%>")
    }

    // 重复任务
    runSyncRepeating(0L, 20L) { task ->
        // 每秒执行一次
        logger.info("<%task.repeating_executed%>")

        // 可以在任务内部取消
        if (someCondition) {
            task.cancel()
        }
    }
}
```

### 消息系统和国际化

```kotlin
override fun onPluginEnable() {
    // 设置语言管理器（必需）
    setupLanguageManager()

    // 发送消息给玩家
    messager.printf(player, "<%message.info%>")

    // 日志记录到控制台
    logger.info("<%console.info%>")
    logger.warn("<%console.warning%>")
    logger.error("<%console.error%>")

    // 格式化消息
    messager.printf(player, true, true, "<%player.level_info%>", player.name, 10)
}

private fun setupLanguageManager() {
    val languageManager = LanguageManager(this, configManager)
        .register(Locale.SIMPLIFIED_CHINESE, "lang/zh_CN.yml")
        .register(Locale.US, "lang/en_US.yml")
        .setMajorLanguage(Locale.SIMPLIFIED_CHINESE)
        .setDefaultLanguage(Locale.US)

    // 通过BasePlugin统一设置语言提供者
    setLanguageProvider(languageManager)
}

/**
 * 重载配置 - 必须实现
 */
override fun reloadPlugin() {
    try {
        logger.info("<%config.reloading%>")

        // 重新设置语言管理器
        setupLanguageManager()

        // 其他重载逻辑...

        logger.info("<%config.reloaded%>")
    } catch (e: Exception) {
        logger.error("<%config.reload_failed%>", e)
        throw e
    }
}
```

## 🔧 常用工具

### ItemBuilder

```kotlin
import city.newnan.core.utils.ItemBuilder

val item = ItemBuilder(Material.DIAMOND_SWORD)
    .name("&6传奇之剑")
    .lore("&7这是一把传奇武器", "&7攻击力: &c+10")
    .enchant(Enchantment.SHARPNESS, 5)
    .unbreakable(true)
    .build()
```

### LocationUtils

```kotlin
import city.newnan.core.utils.LocationUtils

// 计算距离
val distance = LocationUtils.distance(loc1, loc2)

// 安全传送
LocationUtils.safeTeleport(player, targetLocation)

// 检查区域
val inRegion = LocationUtils.isInRegion(location, corner1, corner2)
```

### PlayerUtils

```kotlin
import city.newnan.core.utils.PlayerUtils

// 给予物品
PlayerUtils.giveItem(player, item)

// 检查背包空间
val hasSpace = PlayerUtils.hasInventorySpace(player, 5)

// 播放音效
PlayerUtils.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP)
```

## 🎯 下一步

现在您已经掌握了 Core 模块的基础用法，可以继续学习：

- [🎯 基础概念](concepts.md) - 深入理解核心概念
- [🔧 BasePlugin](base-plugin.md) - 详细了解插件基类
- [♻️ Terminable体系](terminable.md) - 掌握资源管理
- [⚡ 事件处理](events.md) - 高级事件处理技巧
- [⏰ 任务调度](scheduler.md) - 深入任务调度系统

---

**继续学习** → [🎯 基础概念](concepts.md)
