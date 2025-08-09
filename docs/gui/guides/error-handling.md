# 错误处理和调试

GUI1提供了完善的错误处理和调试功能，基于core模块的强大Logger系统，帮助你快速定位和解决问题。

## 🛡️ 错误处理机制

### 自动错误捕获

GUI1会自动捕获并处理以下类型的错误：

```kotlin
// 组件渲染错误
slotComponent(x = 4, y = 2) {
    render {
        // 如果这里抛出异常，GUI1会自动捕获并记录
        val item = itemUtil.createItemStack(Material.DIAMOND) {
            name("测试物品")
        }
        
        // 即使出错，也不会影响其他组件的正常工作
        if (someCondition) {
            throw RuntimeException("模拟错误")
        }
        
        item
    }
}
```

### 错误恢复机制

当组件渲染失败时，GUI1会：
1. 保留上次成功渲染的物品
2. 记录详细的错误信息
3. 继续渲染其他组件
4. 不影响整个GUI的正常运行

## 📊 日志系统

### 使用GuiLogger

GUI1提供了专门的日志记录器，基于core模块的Logger：

```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 获取GUI专用的日志记录器
        val guiLogger = this.guiLogger
        
        // 记录GUI相关信息
        guiLogger.logInfo("GUI系统初始化完成")
        
        // 记录警告
        guiLogger.logWarning("检测到潜在的性能问题")
        
        // 记录调试信息
        guiLogger.logDebug("玩家 ${player.name} 打开了主菜单")
    }
}
```

### 错误日志类型

GUI1会自动记录以下类型的错误：

#### 组件渲染错误
```kotlin
// 当组件渲染失败时，会自动记录：
// - 组件类型和位置
// - 页面信息
// - 玩家信息
// - 错误堆栈
// - 上下文信息
```

#### 事件处理错误
```kotlin
slotComponent(x = 4, y = 2) {
    onLeftClick {
        // 如果事件处理器抛出异常，会自动记录
        throw RuntimeException("事件处理错误")
    }
}
```

#### 生命周期错误
```kotlin
// 页面创建、打开、关闭过程中的错误都会被记录
```

### 查看错误统计

```kotlin
// 获取错误统计报告
val report = guiLogger.generateErrorReport()
logger.info(report)

// 获取具体的错误统计
val errorStats = guiLogger.getErrorStats()
val warningStats = guiLogger.getWarningStats()

// 清空统计
guiLogger.clearStats()
```

## 🔍 调试技巧

### 1. 启用调试模式

在core模块配置中启用调试模式：

```kotlin
override fun getCoreConfig(): CorePluginConfig {
    return CorePluginConfig.builder()
        .loggingConfig(
            CorePluginConfig.LoggingConfig.builder()
                .debugEnabled(true)
                .fileLoggingEnabled(true)
                .build()
        )
        .build()
}
```

### 2. 添加调试信息

```kotlin
slotComponent(x = 4, y = 2) {
    render { context ->
        guiLogger.logDebug("渲染组件", mapOf(
            "slot" to context.slot,
            "x" to context.x,
            "y" to context.y,
            "oldItem" to (context.oldItem?.type?.name ?: "null")
        ))
        
        itemUtil.createItemStack(Material.DIAMOND)
    }
    
    onLeftClick { context ->
        guiLogger.logDebug("处理点击事件", mapOf(
            "player" to context.player.name,
            "clickType" to context.clickType.name,
            "slot" to context.slot
        ))
    }
}
```

### 3. 使用断点调试

```kotlin
slotComponent(x = 4, y = 2) {
    render {
        // 在关键位置添加日志
        logger.info("开始渲染组件...")
        
        val item = itemUtil.createItemStack(Material.DIAMOND) {
            name("测试物品")
        }
        
        logger.info("组件渲染完成")
        item
    }
}
```

### 4. 监控性能

```kotlin
slotComponent(x = 4, y = 2) {
    render {
        // 使用性能监控
        performanceMonitor.monitor("组件渲染") {
            // 耗时操作
            Thread.sleep(100)
            itemUtil.createItemStack(Material.DIAMOND)
        }
    }
}
```

## 🚨 常见错误和解决方案

### 1. 组件渲染失败

**错误现象**：GUI中某些位置显示空白或错误的物品

**可能原因**：
- render函数返回null
- 物品创建过程中抛出异常
- 材质类型不存在

**解决方案**：
```kotlin
slotComponent(x = 4, y = 2) {
    render { context ->
        try {
            // 确保总是返回有效的物品
            itemUtil.createItemStack(Material.DIAMOND) {
                name("安全的物品")
            }
        } catch (e: Exception) {
            // 提供备用物品
            guiLogger.logComponentRenderError(this, context.slot, e)
            itemUtil.createItemStack(Material.BARRIER) {
                name("&c渲染错误")
                lore("&7请检查日志")
            }
        }
    }
}
```

### 2. 事件处理器无响应

**错误现象**：点击按钮没有反应

**可能原因**：
- 事件处理器抛出异常
- 事件被其他组件拦截
- 权限不足

**解决方案**：
```kotlin
slotComponent(x = 4, y = 2) {
    onLeftClick { context ->
        try {
            // 添加日志确认事件被触发
            guiLogger.logDebug("按钮被点击", mapOf(
                "player" to context.player.name,
                "slot" to context.slot
            ))
            
            // 检查权限
            if (!context.player.hasPermission("myplugin.use")) {
                context.player.sendMessage("&c权限不足！")
                return@onLeftClick
            }
            
            // 执行操作
            context.player.sendMessage("&a操作成功！")
            
        } catch (e: Exception) {
            guiLogger.logEventHandlingError(this, context.event, e, "LEFT_CLICK")
            context.player.sendMessage("&c操作失败，请查看日志")
        }
    }
}
```

### 3. 内存泄漏

**错误现象**：服务器内存持续增长

**可能原因**：
- 没有正确关闭Session
- 组件没有正确清理
- 事件监听器没有注销

**解决方案**：
```kotlin
// 确保正确使用生命周期管理
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // GUI1会自动管理生命周期，无需手动清理
        // 但要确保不要手动创建不受管理的资源
    }
    
    override fun onPluginDisable() {
        // BasePlugin会自动清理所有绑定的资源
        logger.info("插件关闭，资源已自动清理")
    }
}
```

### 4. 跨插件GUI冲突

**错误现象**：不同插件的GUI互相干扰

**解决方案**：
```kotlin
// 使用命名Session避免冲突
fun showMyPluginGUI(player: Player) {
    val session = getSession(player, "myplugin:main")
    val page = createPage(player, "我的插件", InventoryType.CHEST, 27)
    
    session.push(page)
    page.show()
}
```

## 📋 调试检查清单

当遇到问题时，按以下顺序检查：

### 1. 检查日志
```bash
# 查看最新的错误日志
tail -f plugins/YourPlugin/logs/plugin-$(date +%Y-%m-%d).log
```

### 2. 验证基础配置
- [ ] 插件正确继承BasePlugin
- [ ] GUI1模块正确添加到依赖
- [ ] 没有版本冲突

### 3. 检查代码逻辑
- [ ] render函数总是返回有效物品
- [ ] 事件处理器有适当的异常处理
- [ ] 权限检查正确实现

### 4. 测试环境
- [ ] 在测试服务器上复现问题
- [ ] 检查是否与其他插件冲突
- [ ] 验证Minecraft版本兼容性

## 🛠️ 高级调试工具

### 1. GUI状态检查器

```kotlin
fun debugGUIState(player: Player) {
    val session = SessionStorage.getSession(player)
    if (session != null) {
        logger.info("玩家 ${player.name} 的GUI状态:")
        logger.info("- Session大小: ${session.size()}")
        logger.info("- 当前页面: ${session.current()?.title ?: "无"}")
        
        session.current()?.let { page ->
            logger.info("- 页面组件数量: ${page.components.size}")
            page.components.forEach { component ->
                logger.info("  - 组件: ${component::class.simpleName}")
            }
        }
    } else {
        logger.info("玩家 ${player.name} 没有活动的GUI Session")
    }
}
```

### 2. 性能分析器

```kotlin
fun analyzeGUIPerformance(player: Player) {
    val session = getDefaultSession(player)
    
    performanceMonitor.monitor("GUI渲染性能分析") {
        session.current()?.let { page ->
            page.update() // 强制重新渲染所有组件
        }
    }
}
```

### 3. 错误模拟器

```kotlin
fun simulateErrors() {
    // 模拟各种错误情况进行测试
    slotComponent(x = 4, y = 2) {
        render {
            if (Random.nextBoolean()) {
                throw RuntimeException("模拟渲染错误")
            }
            itemUtil.createItemStack(Material.DIAMOND)
        }
    }
}
```

## 💡 最佳实践

1. **总是处理异常**：在关键代码路径添加try-catch
2. **使用详细日志**：记录足够的上下文信息
3. **定期检查错误统计**：监控系统健康状况
4. **测试边界情况**：测试各种异常情况
5. **保持代码简洁**：复杂的逻辑更容易出错

通过这些调试技巧和错误处理机制，你可以构建更稳定、更可靠的GUI应用！
