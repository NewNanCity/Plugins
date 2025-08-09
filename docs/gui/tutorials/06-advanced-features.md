# GUI模块调度器生命周期管理

本文档说明GUI模块中调度器的生命周期管理和最佳实践。

## 🏗️ 架构设计

### Component与Page的Scheduler关系

在GUI模块中，Component和Page共享同一个scheduler实例：

```kotlin
// Page拥有scheduler实例
class BasePage : Page {
    override val scheduler: GuiScheduler by lazy {
        GuiScheduler(guiManager.plugin, guiManager.logger)
    }
}

// Component直接使用Page的scheduler
class BaseComponent : Component {
    override val scheduler: GuiScheduler
        get() = page.scheduler  // 直接返回page的scheduler
}
```

### 生命周期一致性

由于Component只能添加到Page而不能独立存在，它们的生命周期天然一致：

1. **创建阶段**：Component通过`page.addComponent()`添加到Page
2. **运行阶段**：Component与Page共同存在，共享资源
3. **销毁阶段**：Page关闭时，所有Component自动清理

## ✅ 优势

### 1. 资源统一管理
- 所有调度任务都绑定到Page的scheduler
- Page关闭时自动清理所有相关资源
- 避免资源泄漏和重复管理

### 2. 性能优化
- 减少scheduler实例数量
- 降低内存占用
- 统一的任务调度和管理

### 3. 一致的API
- Component和Page使用相同的调度器API
- 开发体验一致
- 易于理解和维护

## 🔧 使用示例

### 在Component中使用调度器

```kotlin
slotComponent(x = 1, y = 1) {
    render {
        button(Material.CLOCK, "定时任务")
    }

    onLeftClick { context ->
        // 直接使用调度器方法
        runSyncLater(20L) {
            context.player.sendMessage("1秒后执行")
        }

        // 重复任务
        runSyncRepeating(0L, 20L) { task ->
            context.player.sendMessage("每秒执行")
            // 任务会在page关闭时自动清理
        }
    }
}
```

### 在Page中使用调度器

```kotlin
openPage(InventoryType.CHEST, 54, player) {
    title("示例页面")

    // Page级别的调度任务
    runSyncLater(60L) {
        player.sendMessage("页面打开3秒后")
    }

    // 添加组件...
}
```

### 验证Scheduler共享

```kotlin
slotComponent(x = 2, y = 2) {
    onLeftClick { context ->
        val componentScheduler = this.scheduler
        val pageScheduler = page.scheduler

        // 验证是否是同一个实例
        val isSameInstance = componentScheduler === pageScheduler
        context.player.sendMessage("共享scheduler: $isSameInstance") // true

        // 查看活跃任务数
        context.player.sendMessage("活跃任务数: ${scheduler.getActiveTaskCount()}")
    }
}
```

## 🛡️ 资源管理

### 自动清理机制

当Page关闭时，scheduler会自动清理所有资源：

```kotlin
class GuiScheduler : Terminable {
    private val activeTasks = mutableSetOf<ITaskHandler<*>>()

    override fun close() {
        // 取消所有任务
        activeTasks.forEach { it.close() }
        activeTasks.clear()
    }
}
```

### Component清理

Component关闭时不需要手动清理scheduler：

```kotlin
class BaseComponent : Component {
    override fun close() {
        // 不需要关闭scheduler，因为它由page管理
        // scheduler会在page关闭时自动清理

        // 只需要清理component特有的资源
        clearRenderCache()
        terminableRegistry.closeAndReportException()
    }
}
```

## 📋 最佳实践

### 1. 任务生命周期
- ✅ 在Component或Page中自由使用调度器方法
- ✅ 依赖自动清理机制，无需手动管理
- ❌ 不要手动关闭scheduler

### 2. 长期任务
- ✅ 使用重复任务进行定期更新
- ⚠️ 注意任务会在page关闭时自动停止

### 3. 错误处理
- ✅ 使用try-catch处理任务中的异常
- ✅ 利用ITaskHandler的链式调用进行错误处理

## 🔗 相关文档

- [调度器API参考](../core/schedule.md)
- [GUI最佳实践](best-practices.md)
