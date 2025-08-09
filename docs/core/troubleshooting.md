# Core 模块故障排除

本文档收集了使用 Core 模块时常见的问题和解决方案。

## 🚨 常见错误

### 1. 资源未正确清理

**问题描述：**
```
[WARNING] 插件禁用时发现未清理的资源
[ERROR] Memory leak detected: EventSubscription not terminated
```

**原因分析：**
- 没有使用 `bind()` 方法绑定资源
- 手动创建的资源没有实现 `Terminable` 接口
- 在插件外部创建的资源没有正确管理

**解决方案：**
```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // ✅ 正确：使用 bind() 绑定资源
        val myResource = MyCustomResource()
        bind(myResource)

        // ✅ 正确：事件订阅自动绑定
        subscribeEvent<PlayerJoinEvent> { event ->
            // 处理逻辑
        }

        // ❌ 错误：手动创建未绑定的资源
        // val task = server.scheduler.runTaskTimer(this, {}, 0L, 20L)

        // ✅ 正确：使用 Core 的任务调度
        runSyncRepeating(0L, 20L) { task ->
            // 任务逻辑
        }
    }
}
```

### 2. 异常处理和错误捕获
        // }
    }
}
```

### 3. 事件处理器异常

**问题描述：**
```
[ERROR] Exception in event handler
[ERROR] Event processing failed for PlayerJoinEvent
```

**原因分析：**
- 事件处理器中抛出未捕获的异常
- 访问了可能为 null 的对象
- 在异步上下文中执行了同步操作

**解决方案：**
```kotlin
override fun onPluginEnable() {
    // ✅ 正确：异常安全的事件处理
    subscribeEvent<PlayerJoinEvent> { event ->
        try {
            val player = event.player

            // 安全的异步操作
            runAsync {
                // 异步逻辑
                val data = loadPlayerData(player.uniqueId)

                // 回到主线程更新UI
                runSync {
                    updatePlayerDisplay(player, data)
                }
            }
        } catch (e: Exception) {
            logger.error("处理玩家加入事件失败", e)
        }
    }

    // ❌ 错误：可能抛出异常的处理器
    // subscribeEvent<PlayerJoinEvent> { event ->
    //     val data = someRiskyOperation() // 可能抛出异常
    //     event.player.sendMessage(data.toString()) // 如果 data 为 null 会出错
    // }
}
```

### 4. 任务调度问题

**问题描述：**
```
[WARNING] Task execution delayed
[ERROR] Task queue overflow
```

**原因分析：**
- 在主线程执行耗时操作
- 任务创建过多导致队列溢出
- 同步和异步任务混用不当

**解决方案：**
```kotlin
override fun onPluginEnable() {
    // ✅ 正确：耗时操作使用异步
    runAsync {
        val heavyData = performHeavyCalculation()

        // 回到主线程更新游戏状态
        runSync {
            updateGameState(heavyData)
        }
    }

    // ✅ 正确：合理的任务频率
    runSyncRepeating(0L, 20L * 5) { // 每5秒执行一次
        // 轻量级的定期任务
        checkPlayerStatus()
    }

    // ❌ 错误：在主线程执行耗时操作
    // runSync {
    //     Thread.sleep(5000) // 会阻塞主线程
    // }

    // ❌ 错误：过于频繁的任务
    // runSyncRepeating(0L, 1L) { // 每tick执行，可能导致性能问题
    //     heavyOperation()
    // }
}
```

## 🔧 性能问题

### 1. 内存泄漏

**症状：**
- 服务器内存使用持续增长
- 插件重载后内存不释放
- OutOfMemoryError 异常

**诊断方法：**
```kotlin
override fun onPluginEnable() {
    // 启用资源监控
    runSyncRepeating(0L, 20L * 60) { // 每分钟检查一次
        val stats = getResourceStats()
        logger.info("资源统计: $stats")

        // 检查是否有资源泄漏
        if (stats.inactiveCount > 100) {
            logger.warning("检测到可能的资源泄漏")
            // 手动清理
            cleanupInactiveResources()
        }
    }
}
```

**解决方案：**
- 确保所有资源都通过 `bind()` 绑定
- 定期检查资源统计
- 使用内存分析工具（如 JProfiler）

### 2. 事件处理性能

**症状：**
- 服务器TPS下降
- 事件处理延迟
- 玩家操作响应慢

**优化方案：**
```kotlin
override fun onPluginEnable() {
    // ✅ 优化：使用过滤器减少不必要的处理
    subscribeEvent<PlayerMoveEvent>()
        .filter { event ->
            // 只处理移动距离大于1格的事件
            event.from.distance(event.to ?: return@filter false) > 1.0
        }
        .handler { event ->
            // 处理逻辑
        }

    // ✅ 优化：批量处理
    val pendingUpdates = mutableListOf<Player>()

    subscribeEvent<PlayerJoinEvent> { event ->
        pendingUpdates.add(event.player)
    }

    // 每秒批量处理一次
    runSyncRepeating(0L, 20L) {
        if (pendingUpdates.isNotEmpty()) {
            processBatchUpdates(pendingUpdates.toList())
            pendingUpdates.clear()
        }
    }
}
```

## 🐛 调试技巧

### 1. 启用详细日志

```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 启用调试模式
        if (config.getBoolean("debug", false)) {
            // 设置日志级别
            logger.level = Level.FINE

            // 启用事件调试
            subscribeEvent<Event>()
                .monitor { event, duration ->
                    if (duration > 50) { // 超过50ms的事件
                        logger.warning("事件处理耗时: ${event.javaClass.simpleName} - ${duration}ms")
                    }
                }
        }
    }
}
```

### 2. 资源监控

```kotlin
override fun onPluginEnable() {
    // 定期输出资源统计
    runSyncRepeating(0L, 20L * 30) { // 每30秒
        val stats = getResourceStats()
        logger.info("""
            资源统计:
            - 活跃事件订阅: ${stats.activeEvents}
            - 活跃任务: ${stats.activeTasks}
            - 总资源数: ${stats.totalResources}
        """.trimIndent())
    }
}
```

### 3. 异常追踪

```kotlin
override fun onPluginEnable() {
    // 全局异常处理器
    Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
        logger.severe("未捕获的异常在线程 ${thread.name}: ${exception.message}")
        exception.printStackTrace()
    }

    // 任务异常处理
    runAsync {
        try {
            // 可能出错的代码
            riskyOperation()
        } catch (e: Exception) {
            logger.error("任务异常", e)
            // 发送错误报告
            sendErrorReport(e)
        }
    }
}
```

## 📋 检查清单

### 插件启动检查
- [ ] 所有资源都通过 `bind()` 绑定
- [ ] 事件处理器有异常处理
- [ ] 任务调度使用正确的线程
- [ ] 配置文件正确加载
- [ ] 依赖模块正常初始化

### 插件运行检查
- [ ] 内存使用稳定
- [ ] 没有资源泄漏警告
- [ ] 事件处理性能正常
- [ ] 任务执行无延迟
- [ ] 日志无异常信息

### 插件关闭检查
- [ ] 所有资源正确清理
- [ ] 数据库连接关闭
- [ ] 文件句柄释放
- [ ] 内存完全释放

## 🆘 获取帮助

如果以上解决方案都无法解决您的问题，请：

1. **查看完整日志** - 包括启动、运行和关闭时的所有日志
2. **提供复现步骤** - 详细描述如何重现问题
3. **环境信息** - 服务器版本、插件版本、Java版本等
4. **提交Issue** - 在项目仓库提交详细的问题报告

---

**返回文档首页** → [📚 Core模块文档](README.md)
