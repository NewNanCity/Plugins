# 任务调度系统

Core 模块提供了现代化的任务调度系统，基于 ITaskHandler 接口设计，提供类似 CompletableFuture 的 API，支持链式调用、依赖管理和组合任务。

## 🎯 核心特性

### ITaskHandler API
- **类型安全** - 完整的泛型支持和编译时检查
- **链式调用** - 支持 `thenApply`、`thenRunSync`、`thenRunAsync` 等方法
- **依赖管理** - 任务可以依赖其他任务的完成
- **组合任务** - 支持 ALL/ANY 模式的任务组合
- **自动生命周期** - 任务自动绑定到插件/模块生命周期

## 🚀 基础用法

### 基本任务调度

```kotlin
class MyPlugin : BasePlugin() {

    override fun onPluginEnable() {
        // 同步任务
        runSync {
            logger.info("同步任务执行")
        }

        // 异步任务
        runAsync {
            val data = loadDataFromDatabase()
            processData(data)
        }

        // 延迟任务
        runSyncLater(20L) { // 1秒后
            server.broadcastMessage("延迟消息")
        }

        // 重复任务
        runSyncRepeating(0L, 20L) { // 每秒
            updatePlayerDisplays()
        }

        // 异步重复任务
        runAsyncRepeating(0L, 20L * 60) { // 每分钟
            performMaintenanceTasks()
        }
    }
}
```

### 链式调用

```kotlin
class MyPlugin : BasePlugin() {

    override fun onPluginEnable() {
        // 任务链式处理
        runAsync {
            loadDataFromDatabase()
        }.thenApply { data ->
            processData(data)
        }.thenRunSync { processedData ->
            updateGameState(processedData)
        }.handle { result, exception ->
            if (exception != null) {
                logger.error("任务执行失败", exception)
                getDefaultResult()
            } else {
                result
            }
        }
    }
}
```

## 🔗 依赖管理

### 基础依赖

```kotlin
class MyPlugin : BasePlugin() {

    private fun setupDependentTasks() {
        // 基础依赖 - 顺序执行
        val configTask = runAsync { loadConfiguration() }
        val dbTask = runAsync(dependencies = listOf(configTask)) { handler ->
            val config = configTask.getNow(null)!!
            connectToDatabase(config.dbUrl)
        }

        // 多重依赖 - 等待多个任务完成
        val userTask = runAsync { loadUserData() }
        val permTask = runAsync { loadPermissions() }
        val initTask = runAsync(dependencies = listOf(userTask, permTask, dbTask)) { handler ->
            val users = userTask.getNow(null)!!
            val permissions = permTask.getNow(null)!!
            val database = dbTask.getNow(null)!!
            initializeSystem(users, permissions, database)
        }
    }
}
```

### 组合任务

```kotlin
class MyPlugin : BasePlugin() {

    private fun setupCombinedTasks() {
        val task1 = runAsync { loadFromSource1() }
        val task2 = runAsync { loadFromSource2() }
        val task3 = runAsync { loadFromSource3() }

        // ALL 模式 - 等待所有任务完成
        val allTask = combinedTaskHandlers(CombindMode.ALL, task1, task2, task3)
        allTask.thenRunSync { _ ->
            logger.info("所有任务完成")
            val result1 = task1.getNow(null)!!
            val result2 = task2.getNow(null)!!
            val result3 = task3.getNow(null)!!
            processCombinedResults(result1, result2, result3)
        }

        // ANY 模式 - 任意一个任务完成即可
        val anyTask = combinedTaskHandlers(CombindMode.ANY, task1, task2, task3)
        anyTask.thenRunSync { _ ->
            logger.info("至少一个任务完成")
            // 处理第一个完成的任务结果
        }
    }
}
```

## ⚡ 最佳实践

### 1. 非阻塞操作

```kotlin
class MyPlugin : BasePlugin() {

    private fun demonstrateNonBlocking() {
        val task = runAsync { loadData() }

        // ✅ 推荐：使用 getNow() 进行非阻塞检查
        val result = task.getNow(null)
        if (result != null) {
            processResult(result)
        } else {
            task.thenRunSync { data -> processResult(data) }
        }

        // ❌ 避免：使用 get() 会阻塞线程
        // val result = task.get() // 这会阻塞当前线程！
    }
}
```

### 2. 错误处理

```kotlin
class MyPlugin : BasePlugin() {

    private fun setupErrorHandling() {
        // 在任务内部处理异常
        val safeTask = runAsync {
            try {
                riskyOperation()
            } catch (e: Exception) {
                logger.error("操作失败", e)
                getDefaultValue()
            }
        }

        // 使用 handle 方法处理结果和异常
        runAsync {
            loadCriticalData()
        }.handle { result, exception ->
            if (exception != null) {
                logger.error("关键数据加载失败", exception)
                getDefaultData()
            } else {
                result
            }
        }
    }
}
```

### 3. 任务生命周期管理

```kotlin
class MyPlugin : BasePlugin() {
    private val longRunningTasks = mutableListOf<ITaskHandler<*>>()

    private fun setupLifecycleManagedTasks() {
        // 跟踪长期运行的任务
        val monitoringTask = runAsyncRepeating(0L, 20L * 30) { // 每30秒
            performSystemMonitoring()
        }
        longRunningTasks.add(monitoringTask)

        val cleanupTask = runAsyncRepeating(0L, 20L * 300) { // 每5分钟
            performCleanupTasks()
        }
        longRunningTasks.add(cleanupTask)

        // 任务状态监控
        runSyncRepeating(0L, 20L * 60) { // 每分钟检查
            longRunningTasks.removeAll { task ->
                when {
                    task.isCompleted() -> {
                        logger.info("长期任务已完成")
                        true
                    }
                    task.isCancelled() -> {
                        logger.info("长期任务已取消")
                        true
                    }
                    else -> false
                }
            }
        }
    }

    override fun close() {
        // 清理时取消所有长期任务
        longRunningTasks.forEach { task ->
            if (!task.isCompleted()) {
                task.cancel(true)
            }
        }
        longRunningTasks.clear()
        super.close()
    }
}
```

## 🔧 在 BaseModule 中使用

```kotlin
class DataModule(moduleName: String, val plugin: MyPlugin) : BaseModule(moduleName, plugin) {

    override fun onInit() {
        // 模块中的任务调度
        runAsyncRepeating(0L, 20L * 60) { // 每分钟
            cleanupExpiredData()
        }

        // 复杂的数据处理流程
        runAsync {
            loadRawData()
        }.thenApply { rawData ->
            validateData(rawData)
        }.thenApply { validData ->
            transformData(validData)
        }.thenRunSync { finalData ->
            applyDataToGame(finalData)
        }.handle { result, exception ->
            if (exception != null) {
                logger.error("数据处理失败", exception)
                handleDataProcessingFailure()
            }
        }
    }

    private fun cleanupExpiredData() {
        // 清理过期数据的逻辑
    }

    private fun loadRawData(): RawData {
        // 加载原始数据
        return RawData()
    }

    private fun validateData(data: RawData): ValidData {
        // 验证数据
        return ValidData()
    }

    private fun transformData(data: ValidData): FinalData {
        // 转换数据
        return FinalData()
    }

    private fun applyDataToGame(data: FinalData) {
        // 应用数据到游戏
    }

    private fun handleDataProcessingFailure() {
        // 处理数据处理失败
    }
}
```

## 🎯 使用场景指南

### 适合使用任务调度器的场景

✅ **计算密集型任务**
```kotlin
runAsync {
    val result = performComplexCalculation(largeDataSet)
    cacheCalculationResult(result)
}
```

✅ **简单的后台计算**
```kotlin
runAsync {
    val stats = calculateServerStatistics()
    updateStatisticsDisplay(stats)
}
```

✅ **需要返回值的计算**
```kotlin
runAsync {
    loadDataFromFile()
}.thenApply { data ->
    processData(data)
}.thenRunSync { result ->
    updateUI(result)
}
```

✅ **简单的定时/延迟任务**
```kotlin
runSyncLater(20L) { // 1秒后
    showWelcomeMessage()
}

runSyncRepeating(0L, 20L) { // 每秒
    updatePlayerScoreboards()
}
```

### 不适合的场景

❌ **复杂的多步骤异步流程** - 推荐使用任务链 thenApply/thenCompose
❌ **IO密集型任务** - 任务调度器的异步执行同样高效
❌ **需要结构化并发的场景** - 任务调度器提供生命周期管理

## ⚠️ 注意事项

### 1. 避免阻塞调用

```kotlin
// ❌ 错误：使用 get() 会阻塞线程
val task = runAsync { loadData() }
val result = task.get() // 阻塞！

// ✅ 正确：使用 getNow() 或回调
val result = task.getNow(null)
if (result != null) {
    processResult(result)
} else {
    task.thenRunSync { data -> processResult(data) }
}
```

### 2. 正确的异常处理

```kotlin
// ✅ 在任务内部处理异常
runAsync {
    try {
        riskyOperation()
    } catch (e: Exception) {
        logger.error("操作失败", e)
        getDefaultValue()
    }
}
```

### 3. 资源清理

```kotlin
// 任务会自动绑定到插件/模块生命周期
// 插件禁用时自动取消，无需手动清理
```

---

**相关文档：** [⚡ 事件处理](events.md) | [💡 最佳实践](best-practices.md)
