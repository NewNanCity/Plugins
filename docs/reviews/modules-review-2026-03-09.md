# Modules 层代码审查报告

**审查日期**: 2026-03-09
**审查范围**: `modules/core`, `modules/config`, `modules/gui`, `modules/i18n`, `modules/network`
**发现问题总数**: 45 项（Critical: 7, High: 20, Medium: 13, Low: 5）

---

## 目录

- [一、modules/core](#一modulescore)
- [二、modules/config](#二modulesconfig)
- [三、modules/gui](#三modulesgui)
- [四、modules/i18n](#四modulesi18n)
- [五、modules/network](#五modulesnetwork)
- [六、问题总表](#六问题总表)

---

## 一、modules/core

### CORE-01 [Critical] ITaskHandler 链式调用 NPE 风险

**文件**: `modules/core/src/main/kotlin/city/newnan/core/scheduler/ITaskHandler.kt`
**行号**: 所有 `thenRun*` 方法（约 L97-L150）

所有链式调用方法使用 `getNow(null)!!` 模式：

```kotlin
fun <U> thenRunSync(function: (T, ITaskHandler<U>) -> U): ITaskHandler<U> =
    getScheduler().runSync<U>(listOf(this)) { handler ->
        function(getNow(null)!!, handler)  // NPE 陷阱
    }
```

当前置任务失败或被取消时，`getNow(null)` 返回 `null`，`!!` 直接抛出 NPE。此模式出现在约 12 个方法中，影响整个链式任务系统。

**解决方案**: 在链式调用前检查前置任务状态，失败时传播异常而非 NPE：

```kotlin
// 方案 A：安全版本，前置任务失败时取消后续
fun <U> thenRunSync(function: (T, ITaskHandler<U>) -> U): ITaskHandler<U> =
    getScheduler().runSync<U>(listOf(this)) { handler ->
        val previousResult = getNow(null)
        if (previousResult == null && isFailed()) {
            handler.cancel(false)
            throw IllegalStateException("Previous task failed, cannot chain")
        }
        function(previousResult as T, handler) // 注意：T 可能本身是可空的
    }

// 方案 B（推荐）：将链式方法签名改为接收 T?，允许调用方自行处理 null
fun <U> thenRunSync(function: (T?, ITaskHandler<U>) -> U): ITaskHandler<U> =
    getScheduler().runSync<U>(listOf(this)) { handler ->
        function(getNow(null), handler)
    }
```

需同步修改所有 12 个 `thenRun*` 方法。

---

### CORE-02 [High] CombinedTaskHandler 状态机逻辑缺陷

**文件**: `modules/core/src/main/kotlin/city/newnan/core/scheduler/CombinedTaskHandler.kt`
**行号**: L63-L67

```kotlin
override fun start(forced: Boolean) {
    if (!forced && getState() != TaskState.RUNNING) return
    scheduler.onTaskCompleted(taskId, this)
    _internalFinished = true
}
```

`start()` 的逻辑是：当 `forced=false` 时，仅在状态为 `RUNNING` 时执行。而 `RUNNING` 状态的含义是 "SUCCESS 但 `_internalFinished` 为 false"（见 `getState()` L36-L39）。问题在于：

1. `getState()` 和 `_internalFinished` 赋值之间存在竞态窗口
2. `CombindMode` 拼写错误（应为 `CombinedMode`）
3. `get()` / `getNow()` 永远返回 `null`，意味着链式调用后续任务收到的一定是空值

**解决方案**:

```kotlin
// 1. 修复拼写
enum class CombinedMode {  // 重命名（注意影响调用方）
    ALL, ANY
}

// 2. 修复 start() 的竞态问题
override fun start(forced: Boolean) {
    synchronized(this) {  // 保护状态转换
        if (!forced && getState() != TaskState.RUNNING) return
        _internalFinished = true
        scheduler.onTaskCompleted(taskId, this)
    }
}

// 3. 如果 CombinedTaskHandler 需要传递结果，应收集子任务结果
//    如果不需要结果（Unit 类型），保持 null 返回并更新泛型为 Unit
```

---

### CORE-03 [High] EventSubscription 注册与关闭的竞态条件

**文件**: `modules/core/src/main/kotlin/city/newnan/core/event/EventSubscription.kt`
**行号**: L37-L52

```kotlin
internal fun register() {
    if (closed.get()) {
        throw IllegalStateException("Subscription is already closed")
    }
    listener = object : Listener {}
    plugin.server.pluginManager.registerEvent(...)
}
```

`closed` 检查与 `listener` 赋值/注册之间没有原子保证。虽然 Bukkit 事件注册通常在主线程，但 `close()` 可以从任何线程调用，可能导致 listener 注册后立即被 `close()` 遗漏清理。

**解决方案**:

```kotlin
internal fun register() {
    // 使用 synchronized 保证 register 和 close 的互斥
    synchronized(this) {
        if (closed.get()) {
            throw IllegalStateException("Subscription is already closed")
        }
        listener = object : Listener {}
        plugin.server.pluginManager.registerEvent(
            eventClass, listener!!, priority,
            { _, event -> ... },
            plugin
        )
    }
}

override fun close() {
    if (closed.compareAndSet(false, true)) {
        synchronized(this) {  // 与 register 互斥
            listener?.let { HandlerList.unregisterAll(it) }
            listener = null
        }
    }
}
```

---

### CORE-04 [High] BaseModule 必须手动调用 init() 无编译时保证

**文件**: `modules/core/src/main/kotlin/city/newnan/core/base/BaseModule.kt`
**行号**: L60-L68

```kotlin
fun init() {
    if (initialized) return
    try {
        onReload()
        onInit()
    } catch (e: Exception) {
        throw ModuleInitializationException("Failed to initialize module: $moduleName", e)
    }
    initialized = true
}
```

子类必须在 `init {}` 块中手动调用 `init()`，否则模块处于未初始化状态但不会报错。这是一个设计约束，无编译时保证，容易被遗忘。

**建议**: 考虑使用工厂方法模式或在构造函数中通过模板方法自动调用。

**解决方案**: 使用工厂模式强制初始化（不破坏现有子类结构）：

```kotlin
// 方案 A：在 addChild() 时自动初始化
fun <T : BaseModule> addChild(child: T): T {
    if (hasChild(child)) return child
    childModules.computeIfAbsent(child::class.java) { CopyOnWriteArrayList() }.add(child)
    if (!child.initialized) child.init()  // 自动初始化
    bind(child)
    return child
}

// 方案 B：别名构造函数中自动初始化（推荐）
constructor(moduleName: String, plugin: BasePlugin): this(
    moduleName, plugin, plugin.logger, plugin.messager
) {
    plugin.addChild(this)
    init()  // 构造完成后自动初始化
}
```

---

### CORE-05 [Medium] LogFileLoggerProvider 未指定字符集

**文件**: `modules/core/src/main/kotlin/city/newnan/core/logging/provider/LogFileLoggerProvider.kt`
**行号**: L68-L70

```kotlin
FileWriter(logFile, true).use { writer ->
    PrintWriter(writer).use { printer ->
```

`FileWriter` 使用平台默认字符集。在不同操作系统上部署时，日志文件编码不一致会导致乱码。

**建议**: 使用 `Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.APPEND)`。

**解决方案**:

```kotlin
// 替换
FileWriter(logFile, true).use { writer ->
    PrintWriter(writer).use { printer ->

// 为
import java.nio.charset.StandardCharsets
import java.nio.file.StandardOpenOption

Files.newBufferedWriter(
    logFile.toPath(),
    StandardCharsets.UTF_8,
    StandardOpenOption.CREATE,
    StandardOpenOption.APPEND
).use { writer ->
    PrintWriter(writer).use { printer ->
```

---

### CORE-06 [Medium] AbstractCompositeTerminable 不去重

**文件**: `modules/core/src/main/kotlin/city/newnan/core/terminable/AbstractCompositeTerminable.kt`
**行号**: L35

同一个 `AutoCloseable` 被 `bind()` 两次会被关闭两次。虽然在文档中有说明，但这是一个容易忽略的陷阱。双重关闭可能导致异常或资源状态不一致。

**解决方案**: 使用 `IdentityHashMap` 作为去重集合，在 `bind()` 时检查：

```kotlin
// 在 AbstractCompositeTerminable 中新增去重
private val boundResources = Collections.newSetFromMap(IdentityHashMap<AutoCloseable, Boolean>())

override fun <T : AutoCloseable> bind(terminable: T): T {
    if (!boundResources.add(terminable)) {
        return terminable  // 已绑定，跳过
    }
    // 原有的添加到栈的逻辑...
    return terminable
}
```

---

### CORE-07 [Medium] MessageManager 前缀烘焙在非 Component 模式下使用错误前缀

**文件**: `modules/core/src/main/kotlin/city/newnan/core/message/MessageManager.kt`
**行号**: L161-L165

```kotlin
ComponentParseMode.Legacy -> sendTo.sendMessage(_bakedConsolePrefixLegacy + ...)
```

在 `Legacy` 模式中向玩家发送消息却使用了 `_bakedConsolePrefixLegacy`（控制台前缀），应该使用 `_bakedPlayerPrefixLegacy`。

**解决方案**: 修改 `MessageManager.kt` L188-L190 中 Legacy 分支（发送给玩家的分支，非 Console 分支）：

```kotlin
// 修复前
ComponentParseMode.Legacy -> sendTo.sendMessage(_bakedConsolePrefixLegacy +
    (stringFormatter?.sprintfLegacy(provider, finalParseMode, formatText, *params) ?: formatText.toLegacy(finalParseMode))
)

// 修复后：使用玩家前缀
ComponentParseMode.Legacy -> sendTo.sendMessage(_bakedPlayerPrefixLegacy +
    (stringFormatter?.sprintfLegacy(provider, finalParseMode, formatText, *params) ?: formatText.toLegacy(finalParseMode))
)
```

同理检查 `ComponentParseMode.Auto` 分支（L193）是否也使用了错误前缀。

---

### CORE-08 [Low] Logger 异常仅输出到 stderr

**文件**: `modules/core/src/main/kotlin/city/newnan/core/logging/Logger.kt`

Logger 提供者失败时，错误仅通过 `System.err.println` 输出。在 Minecraft 服务器环境中，stderr 可能不被监控，导致关键日志基础设施故障无人发觉。

**解决方案**: 使用 Bukkit Logger 作为 fallback：

```kotlin
} catch (e: Exception) {
    Bukkit.getLogger().severe("[${plugin.name}] Logger provider failed: ${e.message}")
}
```

---

### CORE-09 [Low] CombindMode 拼写错误

**文件**: `modules/core/src/main/kotlin/city/newnan/core/scheduler/CombinedTaskHandler.kt` L5

`CombindMode` 应为 `CombinedMode`。这是一个公共 API，后续修改会 break 现有调用方。

**解决方案**: 使用 `@Deprecated` 渐进迁移：

```kotlin
@Deprecated("Typo: use CombinedMode", replaceWith = ReplaceWith("CombinedMode"))
typealias CombindMode = CombinedMode

enum class CombinedMode {
    ALL, ANY
}
```

---

## 二、modules/config

### CFG-01 [High] Config.parsePath() 尾部转义字符处理不当

**文件**: `modules/config/src/main/kotlin/city/newnan/config/Config.kt`
**行号**: L56-L82

```kotlin
char == ESCAPE_CHAR[0] -> {
    escaped = true
}
```

当路径以反斜杠结尾（如 `"a.b\\"`）时，`escaped` 标志在循环结束后仍为 `true`，反斜杠字符被丢弃而非保留。

**解决方案**: 在循环结束后处理残余的 escaped 状态：

```kotlin
// 在 for 循环之后、添加最后一段之前
if (escaped) {
    // 尾部反斜杠视为字面字符
    currentPart.append(ESCAPE_CHAR)
    escaped = false
}

if (currentPart.isNotEmpty()) {
    parts.add(currentPart.toString())
}
```

---

### CFG-02 [High] ConfigManager.get() 非 ObjectNode 时数据丢失

**文件**: `modules/config/src/main/kotlin/city/newnan/config/ConfigManager.kt`
**行号**: L304-L307

```kotlin
val node = if (file.length() == 0L) {
    mapper.createObjectNode()
} else {
    mapper.readTree(file) as? ObjectNode ?: mapper.createObjectNode()
}
```

若配置文件根节点是 `ArrayNode`（如 JSON 数组 `[1,2,3]`），安全转型失败后返回空 `ObjectNode`，文件内容被**静默丢弃**。

**解决方案**: 对非 ObjectNode 抛出明确异常：

```kotlin
val node = if (file.length() == 0L) {
    mapper.createObjectNode()
} else {
    val tree = mapper.readTree(file)
    tree as? ObjectNode
        ?: throw ConfigFormatException.InvalidStructureException(
            "Config file '$configPath' root must be an object, got ${tree.nodeType}"
        )
}
```

---

### CFG-03 [High] Region.getBlockPositions() 无大小限制可致 OOM

**文件**: `modules/config/src/main/kotlin/city/newnan/config/serializers/RegionSerializers.kt`
**行号**: L100-L110

```kotlin
fun getBlockPositions(): List<BlockPosition> {
    val positions = mutableListOf<BlockPosition>()
    for (x in minBlock.x..maxBlock.x) {
        for (y in minBlock.y..maxBlock.y) {
            for (z in minBlock.z..maxBlock.z) {
                positions.add(BlockPosition(x, y, z, min.world))
            }
        }
    }
    return positions
}
```

一个 100×256×100 的区域将创建 256 万个 `BlockPosition` 对象。没有容量检查，极端情况可导致 JVM OOM。

**解决方案**: 改用 `Sequence` 惰性返回 + 添加安全上限：

```kotlin
companion object {
    const val MAX_BLOCK_POSITIONS = 1_000_000L  // 100万上限
}

fun getBlockPositions(): Sequence<BlockPosition> = sequence {
    val dx = (maxBlock.x - minBlock.x + 1).toLong()
    val dy = (maxBlock.y - minBlock.y + 1).toLong()
    val dz = (maxBlock.z - minBlock.z + 1).toLong()
    val totalBlocks = dx * dy * dz
    require(totalBlocks <= MAX_BLOCK_POSITIONS) {
        "Region too large: $totalBlocks blocks (max $MAX_BLOCK_POSITIONS)"
    }
    for (x in minBlock.x..maxBlock.x) {
        for (y in minBlock.y..maxBlock.y) {
            for (z in minBlock.z..maxBlock.z) {
                yield(BlockPosition(x, y, z, min.world))
            }
        }
    }
}
```

注意：返回类型从 `List` 改为 `Sequence`，调用方需适配。如不想破坏 API，可保持 `List` 但加上 `require` 检查。

---

### CFG-04 [Medium] JsonExtensions 路径解析与 Config 的不一致

**文件**: `modules/config/src/main/kotlin/city/newnan/config/extensions/JsonExtensions.kt`
**行号**: L26-L32

```kotlin
operator fun JsonNode.get(path: String): JsonNode? {
    val pathParts = path.split(".")
    ...
}
```

`JsonExtensions` 使用简单的 `split(".")`，不支持 `Config.kt` 中实现的反斜杠转义机制。两者是同一模块内对同一概念的不一致实现，开发者可能混用导致路径解析行为不同。

**解决方案**: 将 `Config.parsePath()` 抽取为模块级共享工具函数，`JsonExtensions` 复用：

```kotlin
// 新增: ConfigPathUtils.kt
object ConfigPathUtils {
    fun parsePath(path: String): List<String> {
        // 从 Config.Companion.parsePath 迁移
        // ...
    }
}

// JsonExtensions.kt 中复用
operator fun JsonNode.get(path: String): JsonNode? {
    val pathParts = ConfigPathUtils.parsePath(path)
    var currentNode: JsonNode = this
    for (part in pathParts) {
        currentNode = currentNode.get(part) ?: return null
    }
    return currentNode
}
```

---

### CFG-05 [Medium] ConfigManager 缓存 key 构造可能冲突

**文件**: `modules/config/src/main/kotlin/city/newnan/config/ConfigManager.kt`
**行号**: L286

```kotlin
val cacheKey = "${file.canonicalPath}:${(Config::class as Any).javaClass.canonicalName}"
```

`(Config::class as Any).javaClass.canonicalName` 始终返回固定值，对于同一文件不同类型 `parse<T>()` 的缓存 key **不会区分类型**。如果先 `parse<TypeA>()` 再 `parse<TypeB>()` 同一文件，可能返回错误的类型。

**解决方案**: 在 `parseInternal` 中使用包含目标类型的 cache key：

```kotlin
fun <T : Any> parseInternal(
    configPath: String,
    typeRef: TypeReference<T>,
    format: String?,
    useCache: Boolean
): T {
    val file = File(plugin.dataFolder, configPath)
    // 使用目标类型名做 cache key 的一部分
    val cacheKey = "${file.canonicalPath}:${typeRef.type.typeName}"
    // ...
}
```

对于 `get()` 方法，cache key 也应使用 `Config::class.java.canonicalName`（去掉多余的 `as Any`）。

---

### CFG-06 [Medium] deepMerge 递归每层 deepCopy 导致 O(n²)

**文件**: `modules/config/src/main/kotlin/city/newnan/config/ConfigManager.kt`

`deepMerge` 方法在每层递归都对 `existing` 调用 `deepCopy()`，对于深层嵌套的大配置文件会导致 O(n²) 的时间和空间消耗。

**解决方案**: 仅在叶子节点需要保留时做 copy，避免中间层重复复制：

```kotlin
private fun deepMerge(existing: ObjectNode, defaults: ObjectNode): ObjectNode {
    defaults.fields().forEach { (key, defaultValue) ->
        val existingValue = existing.get(key)
        if (existingValue == null) {
            // existing 中不存在此 key，直接设置 default 的深拷贝
            existing.set<JsonNode>(key, defaultValue.deepCopy())
        } else if (existingValue is ObjectNode && defaultValue is ObjectNode) {
            // 递归合并，不做中间层 deepCopy
            deepMerge(existingValue, defaultValue)
        }
        // else: existing 中的值保持不变
    }
    return existing
}
```

---

### CFG-07 [Low] InventorySerializers 将 IOException 转为 RuntimeException

**文件**: `modules/config/src/main/kotlin/city/newnan/config/serializers/InventorySerializers.kt`

将 `IOException` 包装为 `RuntimeException` 抛出，调用方无法区分具体错误类型。建议创建自定义异常。

**解决方案**:

```kotlin
class InventorySerializationException(
    message: String, cause: Throwable? = null
) : RuntimeException(message, cause)

// 在序列化器中使用
throw InventorySerializationException("Failed to serialize inventory", e)
```

---

## 三、modules/gui

### GUI-01 [Critical] SessionStorage.performCleanup() NPE 崩溃

**文件**: `modules/gui/src/main/kotlin/city/newnan/gui/session/SessionStorage.kt`
**行号**: L281

```kotlin
val offlinePlayers = chatInputHandlers.keys.filter { !Bukkit.getPlayer(it)?.isOnline!! }
```

`Bukkit.getPlayer(it)` 返回 `null` 时，`?.isOnline` 返回 `null`，随后 `!!` 直接抛出 NPE。此方法是定时清理任务的核心，崩溃将导致整个清理机制失效，引发内存泄漏。

**修复**:
```kotlin
val offlinePlayers = chatInputHandlers.keys.filter { Bukkit.getPlayer(it)?.isOnline != true }
```

---

### GUI-02 [Critical] SessionStorage.currentGuiManager 读写锁死锁

**文件**: `modules/gui/src/main/kotlin/city/newnan/gui/session/SessionStorage.kt`
**行号**: L53-L68

```kotlin
val currentGuiManager: GuiManager?
    get() = lock.read {
        if (_currentGuiManager == null) {
            lock.write {  // 在持有读锁时请求写锁 → 死锁！
                ...
            }
        }
        _currentGuiManager
    }
```

`ReentrantReadWriteLock` **不允许**在持有读锁时升级为写锁。此代码在并发场景下将导致线程永久阻塞（死锁），冻结服务器主线程。

**修复**: 不要在 `lock.read` 内部调用 `lock.write`。先释放读锁，直接获取写锁后执行双重检查。

**解决方案**: 完整重写此属性：

```kotlin
val currentGuiManager: GuiManager?
    get() {
        // 先尝试读锁快速路径
        lock.read {
            _currentGuiManager?.let { return it }
        }
        // 读锁已释放，使用写锁初始化
        lock.write {
            // 双重检查
            if (_currentGuiManager == null) {
                while (guiManagers.isNotEmpty()) {
                    val candidate = guiManagers.firstOrNull() ?: break
                    if (!Bukkit.getPluginManager().isPluginEnabled(candidate.plugin)) {
                        guiManagers.remove(candidate)
                    } else {
                        _currentGuiManager = candidate
                        break
                    }
                }
            }
            return _currentGuiManager
        }
    }
```

---

### GUI-03 [High] Session.goto() 索引计算与使用分离导致竞态

**文件**: `modules/gui/src/main/kotlin/city/newnan/gui/session/Session.kt`
**行号**: L200-L230

```kotlin
fun goto(index: Int) {
    val actualIndex = lock.read { ... }  // 读锁中计算
    // 此处释放了读锁，pageStack 可能被其他线程修改
    if (actualIndex == pageStack.size - 1) return
    SessionStorage.runInMainThread {
        lock.write {
            // actualIndex 此时可能已失效
            ...
        }
    }
}
```

在读锁释放后、写锁获取前，`pageStack` 可能被其他线程修改，导致 `actualIndex` 越界或指向错误页面。

**解决方案**: 将索引计算和使用放在同一个 `lock.write` 块中：

```kotlin
fun goto(index: Int) {
    SessionStorage.runInMainThread {
        lock.write {
            checkNotClosed()

            val actualIndex = if (index < 0) {
                pageStack.size + index
            } else {
                index
            }

            if (actualIndex < 0 || actualIndex >= pageStack.size) {
                throw IndexOutOfBoundsException("Invalid page index: $index (stack size: ${pageStack.size})")
            }

            if (actualIndex == pageStack.size - 1) return@write

            if (visible) {
                pageStack.lastOrNull()?.hideInternal()
            }

            while (pageStack.size > actualIndex + 1) {
                val removedPage = pageStack.removeLastOrNull()
                removedPage?.destroyInternal()
            }

            if (visible) {
                pageStack.lastOrNull()?.showInternal()
            }
        }
    }
}
```

---

### GUI-04 [High] BasePage.initInternal() 在 builder() 之前标记 initialized

**文件**: `modules/gui/src/main/kotlin/city/newnan/gui/page/BasePage.kt`
**行号**: L91-L101

```kotlin
override fun initInternal() {
    if (initialized) return
    try {
        onInit()
        initialized = true  // ← 在 builder() 之前标记
        builder()            // ← 如果此处抛异常
        eventHandlers.handleEvent(InitEventContext(player))
    } catch (e: Exception) { ... throw e }
}
```

如果 `builder()` 抛出异常，页面已被标记为 `initialized = true`，后续的 `checkInitialized()` 检查将通过，但页面实际处于不完整的初始化状态。

**解决方案**: 将 `initialized = true` 移到所有操作完成之后：

```kotlin
override fun initInternal() {
    if (initialized) return
    try {
        onInit()
        builder()
        eventHandlers.handleEvent(InitEventContext(player))
        initialized = true  // ← 移到最后，所有初始化成功后才标记
    } catch (e: Exception) {
        // initialized 仍为 false，重新初始化是安全的
        logger.logPageLifecycleError(...)
        throw e
    }
}
```

---

### GUI-05 [High] Session.push() 异常不回滚

**文件**: `modules/gui/src/main/kotlin/city/newnan/gui/session/Session.kt`
**行号**: L53-L71

```kotlin
page.initInternal()   // 如果此处抛异常
pageStack.add(page)   // 不会执行到这里 ✓ 但...
```

虽然 `initInternal()` 抛异常时 page 不会被加入栈，但之前执行的 `pageStack.last().hideInternal()` 已隐藏了旧页面。异常后旧页面不会被重新显示，用户看到空白界面。

**解决方案**: 在 push 中添加异常恢复：

```kotlin
fun push(page: Page) {
    checkNotClosed()
    SessionStorage.runInMainThread {
        lock.write {
            if (pageStack.size >= maxDepth) {
                throw IllegalStateException("Session stack overflow: maximum depth $maxDepth exceeded")
            }

            val previousPage = pageStack.lastOrNull()

            // 隐藏当前页面
            previousPage?.hideInternal()

            try {
                page.initInternal()
                pageStack.add(page)
                if (visible) {
                    page.showInternal()
                }
            } catch (e: Exception) {
                // 恢复旧页面显示
                if (visible) {
                    previousPage?.showInternal()
                }
                throw e
            }
        }
    }
}
```

---

### GUI-06 [High] 组件槽位重叠静默覆盖

**文件**: `modules/gui/src/main/kotlin/city/newnan/gui/page/BasePage.kt`
**行号**: L217-L240

```kotlin
component.getSlots().forEach { slot ->
    val previousComponent = slotComponentMap[slot]
    if (previousComponent != null && previousComponent != component) {
        logger.logComponentOverride(...)  // 仅日志
    }
    slotComponentMap[slot] = component     // 静默覆盖
}
```

当两个组件注册了相同的槽位时，旧组件的事件处理器静默失效，但旧组件仍在 `_components` 列表中不会被移除或通知。这可能导致旧组件的资源无法释放，且行为对开发者不透明。

**解决方案**: 抛出异常阻止重叠，或在 DEBUG 环境下发出警告：

```kotlin
// 方案 A（严格模式）：检测到重叠时抛出异常
component.getSlots().forEach { slot ->
    val previousComponent = slotComponentMap[slot]
    if (previousComponent != null && previousComponent != component) {
        throw IllegalStateException(
            "Slot $slot already occupied by ${previousComponent::class.simpleName}, " +
            "cannot register ${component::class.simpleName}"
        )
    }
    slotComponentMap[slot] = component
}

// 方案 B（宽松模式）：自动移除旧组件的冲突槽位
component.getSlots().forEach { slot ->
    val previousComponent = slotComponentMap[slot]
    if (previousComponent != null && previousComponent != component) {
        logger.logComponentOverride(...)
        // 从旧组件的槽位映射中移除此槽位
        // 如果旧组件无剩余槽位，则从 _components 中移除
    }
    slotComponentMap[slot] = component
}
```

---

### GUI-07 [High] removeComponent() 槽位清理逻辑错误

**文件**: `modules/gui/src/main/kotlin/city/newnan/gui/page/BasePage.kt`
**行号**: L260-L268

```kotlin
component.getSlots().forEach { slot ->
    if (slotComponentMap[slot] != component) return  // ← 这是 return 而非 continue
    slotComponentMap.remove(slot)
    ...
}
```

`return` 在 `forEach` lambda 中的行为等同于 `continue`（跳过当前迭代），所以实际逻辑是正确的（`return@forEach`）。但如果开发者误读为提前终止整个方法，可能在维护时引入 bug。建议改为显式的 `return@forEach` 以提高可读性。

**解决方案**: 改为显式标签或 for 循环：

```kotlin
// 方案 A：显式标签
component.getSlots().forEach { slot ->
    if (slotComponentMap[slot] != component) return@forEach
    slotComponentMap.remove(slot)
    ...
}

// 方案 B：改用 for 循环 + continue（更清晰）
for (slot in component.getSlots()) {
    if (slotComponentMap[slot] != component) continue
    slotComponentMap.remove(slot)
    ...
}
```

---

### GUI-08 [High] clearPages() 关闭状态检查逻辑错误

**文件**: `modules/gui/src/main/kotlin/city/newnan/gui/session/Session.kt`
**行号**: L295-L300

```kotlin
fun clearPages() {
    Bukkit.getLogger().info("Closing session $name of ${player.name}")
    if (closed) return  // ← 这里检查 closed
    lock.write { ... }
}
```

问题：`close()` 方法（L279）先设置 `closed = true` 再调用 `clearPages()`。这意味着从 `close()` 调用到 `clearPages()` 时，`if (closed) return` 会直接返回，**页面永远不会被清理**。

这是一个严重的资源泄漏 bug。

**解决方案**: 重构 `close()` 中的调用顺序，或让 `clearPages()` 不检查 `closed`：

```kotlin
// 方案 A（推荐）：close() 直接内联清理逻辑
override fun close() {
    lock.write {
        if (closed) return@write
        closed = true
        visible = false

        // 直接清理页面，不调用 clearPages()
        val pages = pageStack.toList()
        pageStack.clear()
        SessionStorage.runInMainThread {
            pages.forEach { page ->
                try {
                    page.destroyInternal()
                } catch (e: Exception) {
                    page.guiManager.logger.logError(...)
                }
            }
        }
    }
}

// 方案 B：clearPages() 添加内部调用参数
private fun clearPagesInternal() {
    // 不检查 closed，仅由 close() 和 clearPages() 内部调用
    lock.write {
        val pages = pageStack.toList()
        pageStack.clear()
        SessionStorage.runInMainThread {
            pages.forEach { ... }
        }
    }
}

fun clearPages() {
    if (closed) return
    clearPagesInternal()
}

override fun close() {
    lock.write {
        if (closed) return@write
        closed = true
        visible = false
        clearPagesInternal()  // 不受 closed 标志影响
    }
}
```

---

### GUI-09 [Medium] Session.clearPages() 使用错误的 Logger

**文件**: `modules/gui/src/main/kotlin/city/newnan/gui/session/Session.kt` L295

使用 `Bukkit.getLogger()` 而非 GuiManager 的 logger，破坏日志一致性。

**解决方案**: 移除或替换为 GuiManager logger（需要 Session 持有 GuiManager 引用）：

```kotlin
// 如果 Session 能访问到 guiManager：
guiManager.logger.logInfo("Closing session $name of ${player.name}")
// 否则直接移除此调试日志
```

---

### GUI-10 [Medium] SmartCache 每实例创建线程

**文件**: `modules/gui/src/main/kotlin/city/newnan/gui/optimization/SmartCache.kt` L30-L33

每个 `SmartCache` 实例创建独立的 `ScheduledExecutorService`。大量 GUI 页面场景下会造成线程膨胀。

**解决方案**: 使用 `companion object` 共享清理线程池：

```kotlin
class SmartCache<K, V>(...) {
    companion object {
        private val sharedCleanupExecutor: ScheduledExecutorService =
            Executors.newSingleThreadScheduledExecutor { r ->
                Thread(r, "SmartCache-Cleanup").apply { isDaemon = true }
            }
    }

    private val cleanupTask: ScheduledFuture<*>

    init {
        cleanupTask = sharedCleanupExecutor.scheduleAtFixedRate(
            ::cleanup, cleanupIntervalMs, cleanupIntervalMs, TimeUnit.MILLISECONDS
        )
    }

    fun close() {
        cleanupTask.cancel(false)  // 取消本实例的清理任务，不关闭共享线程池
        cache.clear()
    }
}
```

---

### GUI-11 [Medium] GlobalEventListener 注册顺序导致多 GuiManager 事件冲突

**文件**: `modules/gui/src/main/kotlin/city/newnan/gui/event/GlobalEventListener.kt` L67-L107

所有事件都有 `filter { SessionStorage.currentGuiManager == guiManager }` 过滤条件。这意味着只有"当前" GuiManager 的事件处理器会生效，其他插件注册的 GuiManager 的事件将被完全忽略。

**解决方案**: 如果设计本意是多 GuiManager 共存，应改为基于 Session 查找对应的 GuiManager：

```kotlin
// 将事件路由到与玩家 Session 关联的 GuiManager，而非全局 currentGuiManager
filter {
    val session = SessionStorage.getSession(it.whoClicked as? Player ?: return@filter false)
    session?.current()?.guiManager == guiManager
}
```

如果设计本意是单 GuiManager 运行，则应在文档中明确标注此限制。

---

### GUI-12 [Low] Session.pop() 末尾无用表达式

**文件**: `modules/gui/src/main/kotlin/city/newnan/gui/session/Session.kt` L94

`pop()` 的 lambda 最后一行是 `poppedPage`，这个值从未被返回或使用。

**解决方案**: 移除无用的 `poppedPage` 表达式，或将 `pop()` 改为返回被弹出的页面：

```kotlin
// 如果不需要返回值，直接移除最后一行
poppedPage?.destroyInternal()
if (pageStack.isNotEmpty() && visible) {
    pageStack.last().showInternal()
}
// 删除: poppedPage
```

---

## 四、modules/i18n

### I18N-01 [Critical] LanguageManager.processTemplate() 正则匹配器状态损坏

**文件**: `modules/i18n/src/main/kotlin/city/newnan/i18n/LanguageManager.kt`
**行号**: L282-L305

```kotlin
var processedText = rawText
val matcher = templatePattern.matcher(processedText)
while (matcher.find()) {
    val templateVar = matcher.group(0)
    val key = matcher.group(1)
    val replacement = findTranslation(key)
    if (replacement != null) {
        processedText = processedText.replace(templateVar, replacement)
        // ⚠️ matcher 仍在原始 rawText 上操作，但 processedText 已改变！
    }
}
```

`matcher` 基于原始 `processedText` 创建。在循环中通过 `String.replace()` 修改了 `processedText`，但 `matcher` 的内部状态（偏移量等）仍指向原始字符串。这会导致：

1. 如果替换文本改变了字符串长度，后续 `matcher.find()` 的位置偏移不正确
2. 如果替换文本本身包含 `<%...%>` 模板标记，不会被二次处理（可能是期望行为，也可能不是）
3. `matchCount` 变量计算后从未被使用

**修复**: 使用 `Matcher.replaceAll()` 或 `StringBuffer` + `Matcher.appendReplacement()`/`appendTail()`。

**解决方案**: 使用 `appendReplacement` / `appendTail` 正确处理替换：

```kotlin
fun processTemplate(rawText: String): String {
    val matcher = templatePattern.matcher(rawText)
    val sb = StringBuffer()
    while (matcher.find()) {
        val key = matcher.group(1)
        val replacement = findTranslation(key)
        if (replacement != null) {
            // Matcher.quoteReplacement 防止 replacement 中 $ 和 \ 被误解析
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement))
        }
    }
    matcher.appendTail(sb)
    return sb.toString()
}
```

此方案确保 matcher 的内部偏移始终正确，且不会对替换文本中的模板标记进行二次处理。

---

### I18N-02 [High] reloadAll() 错误收集但不传播

**文件**: `modules/i18n/src/main/kotlin/city/newnan/i18n/LanguageManager.kt`
**行号**: L233-L252

```kotlin
val errors = mutableListOf<Exception>()
languageMap.forEach { (locale, language) ->
    try { language.reload() }
    catch (e: LanguageReloadException) { errors.add(e) }
}
// errors 仅用于日志计数，调用方无法得知哪些语言加载失败
```

调用 `reloadAll()` 后，即使所有语言加载失败，方法仍返回 `this` 且不抛异常。

**解决方案**: 在方法结束时抛出聚合异常：

```kotlin
fun reloadAll(): LanguageManager {
    val errors = mutableListOf<LanguageReloadException>()
    languageMap.forEach { (locale, language) ->
        try {
            language.reload()
        } catch (e: LanguageReloadException) {
            errors.add(e)
            logger.warning("语言 $locale 加载失败: ${e.message}")
        }
    }
    if (errors.isNotEmpty()) {
        throw LanguageReloadException(
            "Failed to reload ${errors.size} language(s): ${errors.joinToString { it.message ?: "" }}",
            errors.first()
        ).apply {
            errors.drop(1).forEach { addSuppressed(it) }
        }
    }
    return this
}
```

---

### I18N-03 [High] 非 TerminableConsumer 插件的资源泄漏

**文件**: `modules/i18n/src/main/kotlin/city/newnan/i18n/LanguageManager.kt` L63-L66

```kotlin
if (plugin is TerminableConsumer) {
    bindWith(plugin)
}
```

若插件不是 `TerminableConsumer`，`LanguageManager` 不绑定生命周期，缓存和语言对象永远不会被清理。

**解决方案**: 强制要求 `TerminableConsumer`，或提供手动 `close()` 方法：

```kotlin
// 方案 A（推荐）：将参数类型改为 TerminableConsumer
class LanguageManager(val plugin: TerminableConsumer) : Terminable {
    init {
        bindWith(plugin)  // 总是自动绑定
    }
    // ...
}

// 方案 B：保留兼容性但添加 close() 并输出警告
class LanguageManager(val plugin: Plugin) : Terminable {
    init {
        if (plugin is TerminableConsumer) {
            bindWith(plugin)
        } else {
            plugin.logger.warning(
                "LanguageManager created without TerminableConsumer, " +
                "you must call close() manually in onDisable()"
            )
        }
    }

    override fun close() {
        languageMap.clear()
        // 清理缓存等资源
    }
}
```

---

### I18N-04 [Medium] guessMajorLanguage() 在语言注册前调用

**文件**: `modules/i18n/src/main/kotlin/city/newnan/i18n/LanguageManager.kt` L68-L70

`init {}` 块中调用 `guessMajorLanguage()`，但此时 `languageMap` 为空（注册在构造后进行），所以此方法永远不会成功匹配。

**修复**: 在所有语言注册完成后再调用，或改为惰性检测。

**解决方案**: 延迟到首次使用时猜测，并在语言注册时重置：

```kotlin
private var _majorLanguage: Locale? = null
private var majorLanguageDirty = true

val majorLanguage: Locale
    get() {
        if (majorLanguageDirty) {
            _majorLanguage = guessMajorLanguage()
            majorLanguageDirty = false
        }
        return _majorLanguage ?: defaultLocale
    }

fun registerLanguage(locale: Locale, language: Language): LanguageManager {
    languageMap[locale] = language
    majorLanguageDirty = true  // 标记需要重新猜测
    return this
}
```

---

## 五、modules/network

### NET-01 [Critical] NetworkDSL.createHttpClient() 忽略配置参数

**文件**: `modules/network/src/main/kotlin/city/newnan/network/NetworkDSL.kt`
**行号**: L218-L221

```kotlin
private fun createHttpClient(name: String, config: HttpClientConfig): HttpClient {
    return getHttpClient(name)  // config 参数完全被忽略！
}
```

用户通过 `HttpClientConfigBuilder` 配置的 `userAgent`、`headers`、`auth`、`logging` 等全部被丢弃，实际创建的是默认客户端。

**解决方案**: `createHttpClient` 应将 config 传递给实际的客户端创建逻辑：

```kotlin
private fun createHttpClient(name: String, config: HttpClientConfig): HttpClient {
    return HttpClientManager.getOrCreateClient(name) {
        // 将 config 应用到 HttpClient
        if (config.userAgent != null) {
            install(UserAgent) { agent = config.userAgent }
        }
        config.defaultHeaders.forEach { (key, value) ->
            defaultRequest { header(key, value) }
        }
        if (config.auth != null) {
            install(Auth) { config.auth.apply(this) }
        }
        if (config.logging) {
            install(Logging) { level = LogLevel.ALL }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = config.timeout.inWholeMilliseconds
            connectTimeoutMillis = config.connectTimeout.inWholeMilliseconds
        }
    }
}
```

---

### NET-02 [Critical] NetworkDSL 临时客户端泄漏

**文件**: `modules/network/src/main/kotlin/city/newnan/network/NetworkDSL.kt`
**行号**: L40-L55

```kotlin
suspend fun get(url: String, ...): HttpResponse {
    val httpClient = client ?: getHttpClient()  // 创建新客户端但不存储
    return httpClient.get(url, block)
}
```

当 `client` 为 `null` 时（未调用 `client {}` 配置），每次请求都通过 `getHttpClient()` 获取/创建客户端。虽然 `getHttpClient()` 有全局缓存不会真正泄漏，但临时 `NetworkDSLBuilder` 实例（如 `httpGet()` 函数中的）每次都创建新的 builder 却不绑定生命周期。

**解决方案**: `NetworkDSLBuilder` 应复用已有客户端或延迟获取：

```kotlin
class NetworkDSLBuilder(private val plugin: BasePlugin) {
    private val client: HttpClient by lazy {
        // 延迟获取并缓存，避免重复创建
        getHttpClient(plugin.name)
    }

    suspend fun get(url: String, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
        return client.get(url, block)
    }

    suspend fun post(url: String, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
        return client.post(url, block)
    }
}
```

---

### NET-03 [Critical] 简化 HTTP 方法每次创建新 Builder 且不绑定

**文件**: `modules/network/src/main/kotlin/city/newnan/network/NetworkDSL.kt`
**行号**: L188-L200

```kotlin
suspend fun BasePlugin.httpGet(url: String, ...): HttpResponse {
    val builder = NetworkDSLBuilder(this)  // 每次调用新建 builder
    return builder.get(url, block)
}
```

每次调用 `httpGet()` / `httpPost()` 都创建新的 `NetworkDSLBuilder`，builder 中的 `client` 有可能绑定到 plugin lifecycle 的 terminable 列表中（如果调用过 `client {}`），但这里没有调用，所以走到 fallback 的 `getHttpClient()`。这虽然不直接泄漏但设计混乱。

**解决方案**: 在 `BasePlugin` 上缓存 builder 实例：

```kotlin
// 在 BasePlugin 中以扩展属性缓存
private val pluginBuilderCache = ConcurrentHashMap<String, NetworkDSLBuilder>()

suspend fun BasePlugin.httpGet(url: String, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
    val builder = pluginBuilderCache.getOrPut(this.name) { NetworkDSLBuilder(this) }
    return builder.get(url, block)
}

suspend fun BasePlugin.httpPost(url: String, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
    val builder = pluginBuilderCache.getOrPut(this.name) { NetworkDSLBuilder(this) }
    return builder.post(url, block)
}
```

或更好的方式——直接让 `httpGet/httpPost` 使用共享客户端而不创建 builder：

```kotlin
suspend fun BasePlugin.httpGet(url: String, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
    return getHttpClient(this.name).get(url, block)
}
```

---

### NET-04 [High] 全局 HttpClientManager 无插件隔离

**文件**: `modules/network/src/main/kotlin/city/newnan/network/NetworkExtensions.kt` L16

```kotlin
private val httpClientManagers = ConcurrentHashMap<String, HttpClientManager>()
```

所有插件共享同一个全局 `ConcurrentHashMap`。插件 A 和插件 B 调用 `getHttpClientManager("default")` 会得到同一个实例。一个插件关闭时调用 `closeAll()` 会影响另一个插件。

**解决方案**: 使用插件名称作为命名空间隔离：

```kotlin
private val httpClientManagers = ConcurrentHashMap<String, HttpClientManager>()

fun getHttpClientManager(pluginName: String, name: String = "default"): HttpClientManager {
    val key = "${pluginName}:${name}"
    return httpClientManagers.getOrPut(key) { HttpClientManager(name) }
}

fun closeAllForPlugin(pluginName: String) {
    val prefix = "${pluginName}:"
    httpClientManagers.keys.filter { it.startsWith(prefix) }.forEach { key ->
        httpClientManagers.remove(key)?.close()
    }
}
```

---

### NET-05 [High] HttpClientManager OkHttp 超时配置可能类型不匹配

**文件**: `modules/network/src/main/kotlin/city/newnan/network/HttpClientManager.kt`
**行号**: L155-L160

```kotlin
engine {
    config {
        connectTimeout(config.connectTimeout)   // Duration
        readTimeout(config.socketTimeout)       // Duration
        writeTimeout(config.socketTimeout)      // Duration
    }
}
```

`engine { config {} }` 中的 `config` 变量名与外部构造方法参数 `config: HttpClientConfig` 重名。在 `config {}` lambda 内部，`config` 指向的是 OkHttp 的 `OkHttpClient.Builder`，而 `config.connectTimeout` 调用的是 `OkHttpClient.Builder.connectTimeout(Duration)` ——这里恰好参数类型兼容。但变量遮蔽使代码极易误读和出错。

**解决方案**: 重命名参数避免遮蔽：

```kotlin
fun createClient(clientConfig: HttpClientConfig): HttpClient {
    return HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(clientConfig.connectTimeout)
                readTimeout(clientConfig.socketTimeout)
                writeTimeout(clientConfig.socketTimeout)
            }
        }
        // ...
    }
}
```

---

### NET-06 [High] SerializationType.BOTH 安装两个序列化器

**文件**: `modules/network/src/main/kotlin/city/newnan/network/HttpClientManager.kt` L88-L96

同时安装 kotlinx JSON 和 Jackson 序列化器。Ktor 的 `ContentNegotiation` 按注册顺序匹配，第二个序列化器（Jackson）对于相同 Content-Type 不会被使用。API 暗示"两者都用"，但实际行为只用第一个。

**解决方案**: 为不同的 Content-Type 注册不同的序列化器，或移除 `BOTH` 选项：

```kotlin
// 方案 A：移除 BOTH，强制选择一个
enum class SerializationType {
    KOTLINX_JSON,
    JACKSON
    // 移除 BOTH — 同一 Content-Type 不能有两个序列化器
}

// 方案 B：如果确实需要两者，为不同类型注册
SerializationType.BOTH -> {
    json(kotlinxSerializer)  // application/json 用 kotlinx
    register(ContentType.Application.Xml, JacksonConverter(jacksonMapper))  // XML 用 Jackson
}
```

并在文档中注明：对于 `application/json`，仅首个注册的序列化器生效。

---

### NET-07 [High] 全模块使用 println 替代日志系统

**文件**: `modules/network/src/main/kotlin/city/newnan/network/HttpClientManager.kt` L108, L166, L175
**文件**: `modules/network/src/main/kotlin/city/newnan/network/NetworkExtensions.kt` L88
**文件**: `modules/network/src/main/kotlin/city/newnan/network/TerminableNetworkExtensions.kt` L76

整个 network 模块使用 `println()` 替代正式的日志系统输出信息和错误。在生产环境中无法通过日志级别控制、过滤或重定向。

**解决方案**: 注入 `Logger` 实例，或使用 SLF4J/Bukkit logger：

```kotlin
class HttpClientManager(
    private val name: String,
    private val logger: java.util.logging.Logger = java.util.logging.Logger.getLogger("NetworkModule")
) {
    fun someOperation() {
        // 替换所有 println
        logger.info("HTTP client '$name' created")       // 替代 println(...)
        logger.warning("Request failed: ${e.message}")    // 替代 println("Error: ...")
        logger.log(Level.SEVERE, "Fatal error", e)        // 替代 e.printStackTrace()
    }
}

// NetworkExtensions 中
private val logger = java.util.logging.Logger.getLogger("NetworkExtensions")
```

---

### NET-08 [High] asyncHttpRequest() 异常被吞没

**文件**: `modules/network/src/main/kotlin/city/newnan/network/NetworkExtensions.kt` L85-L91

```kotlin
fun asyncHttpRequest(scope: CoroutineScope, block: suspend CoroutineScope.() -> Unit) {
    scope.launch {
        try { block() }
        catch (e: Exception) { println("异步 HTTP 请求执行失败: ${e.message}") }
    }
}
```

异常仅 println，调用方无法获知请求失败。

**解决方案**: 添加错误回调参数，并使用 logger 记录：

```kotlin
fun asyncHttpRequest(
    scope: CoroutineScope,
    onError: ((Exception) -> Unit)? = null,
    block: suspend CoroutineScope.() -> Unit
): Job {
    return scope.launch {
        try {
            block()
        } catch (e: CancellationException) {
            throw e  // 协程取消不应捕获
        } catch (e: Exception) {
            logger.log(Level.WARNING, "异步 HTTP 请求执行失败", e)
            onError?.invoke(e)
                ?: throw e  // 没有错误处理器时重新抛出
        }
    }
}
```

---

### NET-09 [High] TerminableNetworkExtensions 每次请求创建新 Wrapper

**文件**: `modules/network/src/main/kotlin/city/newnan/network/TerminableNetworkExtensions.kt`
**行号**: 所有 `terminableGet`/`terminablePost` 等方法

```kotlin
fun TerminableConsumer.terminableGet(...): Job {
    val client = createTerminableHttpClient(clientName)  // 每次新建！
    return client.executeRequest(...)
}
```

每次 HTTP 请求都创建新的 `TerminableHttpClientWrapper` 并 `bind()` 到消费者，导致：
- 内存膨胀（每个 wrapper 都绑定一个关闭回调）
- 违反连接池复用原则

**解决方案**: 按 `TerminableConsumer` 缓存 wrapper 实例：

```kotlin
private val wrapperCache = ConcurrentHashMap<TerminableConsumer, ConcurrentHashMap<String, TerminableHttpClientWrapper>>()

fun TerminableConsumer.getOrCreateTerminableClient(
    clientName: String = "default"
): TerminableHttpClientWrapper {
    val consumerWrappers = wrapperCache.getOrPut(this) { ConcurrentHashMap() }
    return consumerWrappers.getOrPut(clientName) {
        TerminableHttpClientWrapper(clientName, getHttpClient(clientName)).also { wrapper ->
            bind(wrapper)  // 仅首次创建时绑定
            // 在 close 时清理缓存
            bind(Terminable {
                consumerWrappers.remove(clientName)
                if (consumerWrappers.isEmpty()) {
                    wrapperCache.remove(this)
                }
            })
        }
    }
}

fun TerminableConsumer.terminableGet(url: String, clientName: String = "default", ...): Job {
    val client = getOrCreateTerminableClient(clientName)
    return client.executeRequest(...)
}
```

---

### NET-10 [High] SimpleNetworkMonitor.close() 为空实现

**文件**: `modules/network/src/main/kotlin/city/newnan/network/NetworkDSL.kt` L165-L167

```kotlin
override fun close() {
    // 简单实现，实际应该停止监控任务
}
```

监控器在关闭时不执行任何操作，绑定的回调和资源不会被释放。

**解决方案**: 实现 `close()` 中的资源清理和活跃任务取消：

```kotlin
class SimpleNetworkMonitor : NetworkMonitor {
    private val activeJobs = mutableListOf<Job>()
    @Volatile
    private var closed = false

    fun trackJob(job: Job) {
        synchronized(activeJobs) {
            activeJobs.add(job)
            // 自动清理已完成的 job
            activeJobs.removeAll { it.isCompleted }
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        synchronized(activeJobs) {
            activeJobs.forEach { job ->
                job.cancel(CancellationException("NetworkMonitor closed"))
            }
            activeJobs.clear()
        }
        // 清理回调等其他资源
    }
}
```

---

### NET-11 [High] downloadToByteArray 下载后才检查大小

**文件**: `modules/network/src/main/kotlin/city/newnan/network/NetworkUtils.kt` L95-L104

```kotlin
val response = client.get(url)  // 先完整下载
val contentLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
if (contentLength != null && contentLength > maxSize) {
    throw IllegalArgumentException(...)  // 下载完才检查
}
return response.readBytes()  // 又读一遍
```

1. 响应先被完整接收（`client.get`）后才检查 `Content-Length`
2. 如果服务器不发送 `Content-Length` 头，大小检查完全无效
3. `response.readBytes()` 将整个响应体读入内存

**建议**: 使用 streaming 方式下载，边读边检查大小。

**解决方案**: 使用 streaming 下载并逐块检查累积大小：

```kotlin
suspend fun downloadToByteArray(
    client: HttpClient, url: String, maxSize: Long = 10 * 1024 * 1024
): ByteArray {
    return client.prepareGet(url).execute { response ->
        // 先检查 Content-Length（如果有的话）
        val contentLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
        if (contentLength != null && contentLength > maxSize) {
            throw IllegalArgumentException(
                "Content-Length $contentLength exceeds max size $maxSize"
            )
        }

        val channel: ByteReadChannel = response.body()
        val buffer = ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        var totalRead = 0L

        while (!channel.isClosedForRead) {
            val read = channel.readAvailable(chunk)
            if (read <= 0) break
            totalRead += read
            if (totalRead > maxSize) {
                throw IllegalArgumentException(
                    "Download size $totalRead exceeds max size $maxSize (streaming check)"
                )
            }
            buffer.write(chunk, 0, read)
        }

        buffer.toByteArray()
    }
}
```

---

### NET-12 [Medium] 延迟测量精度不足

**文件**: `modules/network/src/main/kotlin/city/newnan/network/NetworkUtils.kt` L57-58

使用 `System.currentTimeMillis()` 测量延迟，精度仅 1ms。对于快速请求，结果可能为 0ms。应使用 `System.nanoTime()`。

**解决方案**: 替换为 `nanoTime()`：

```kotlin
val startNano = System.nanoTime()
val response = client.get(url)
val latencyMs = (System.nanoTime() - startNano) / 1_000_000.0
// latencyMs 为 Double 类型，可精确到微秒级
```

---

### NET-13 [Medium] HttpRequestDsl.bodyOrNull() 吞没所有异常

**文件**: `modules/network/src/main/kotlin/city/newnan/network/HttpRequestDsl.kt`

```kotlin
suspend inline fun <reified T> HttpResponse.bodyOrNull(): T? {
    return try { body<T>() } catch (e: Exception) { null }
}
```

反序列化错误、类型不匹配、网络错误全部返回 `null`，调试极其困难。

**解决方案**: 仅捕获特定的序列化异常，其他异常应传播；同时记录警告日志：

```kotlin
suspend inline fun <reified T> HttpResponse.bodyOrNull(): T? {
    return try {
        body<T>()
    } catch (e: kotlinx.serialization.SerializationException) {
        logger.log(Level.FINE, "Failed to deserialize response body to ${T::class.simpleName}", e)
        null
    } catch (e: com.fasterxml.jackson.core.JacksonException) {
        logger.log(Level.FINE, "Failed to deserialize response body to ${T::class.simpleName}", e)
        null
    }
    // 其他异常（网络错误、IllegalStateException 等）不捕获，让调用方处理
}
```

---

## 六、问题总表

| ID      | 模块    | 严重度   | 类别       | 摘要                                      |
| ------- | ------- | -------- | ---------- | ----------------------------------------- |
| CORE-01 | core    | Critical | NPE        | ITaskHandler 链式调用 `getNow(null)!!`    |
| CORE-02 | core    | High     | 逻辑缺陷   | CombinedTaskHandler 状态机竞态            |
| CORE-03 | core    | High     | 竞态条件   | EventSubscription 注册/关闭竞态           |
| CORE-04 | core    | High     | 设计缺陷   | BaseModule.init() 无编译时保证            |
| CORE-05 | core    | Medium   | 编码规范   | LogFileLoggerProvider 未指定字符集        |
| CORE-06 | core    | Medium   | 设计缺陷   | CompositeTerminable 不去重                |
| CORE-07 | core    | Medium   | 逻辑错误   | MessageManager 使用控制台前缀发送玩家消息 |
| CORE-08 | core    | Low      | 可观测性   | Logger 异常仅到 stderr                    |
| CORE-09 | core    | Low      | 拼写       | CombindMode 拼写错误                      |
| CFG-01  | config  | High     | 边界条件   | parsePath 尾部转义字符丢失                |
| CFG-02  | config  | High     | 数据安全   | 非 ObjectNode 数据静默丢弃                |
| CFG-03  | config  | High     | 资源安全   | Region.getBlockPositions 可致 OOM         |
| CFG-04  | config  | Medium   | API 一致性 | JsonExtensions 与 Config 路径解析不一致   |
| CFG-05  | config  | Medium   | 缓存       | 缓存 key 不区分目标类型                   |
| CFG-06  | config  | Medium   | 性能       | deepMerge 递归 deepCopy O(n²)             |
| CFG-07  | config  | Low      | 异常设计   | IOException 转 RuntimeException           |
| GUI-01  | gui     | Critical | NPE        | performCleanup `?.isOnline!!`             |
| GUI-02  | gui     | Critical | 死锁       | currentGuiManager 读锁内请求写锁          |
| GUI-03  | gui     | High     | 竞态条件   | goto() 索引计算与使用分离                 |
| GUI-04  | gui     | High     | 初始化     | initInternal 过早标记 initialized         |
| GUI-05  | gui     | High     | 异常恢复   | push 异常后旧页面未恢复显示               |
| GUI-06  | gui     | High     | 设计缺陷   | 组件槽位重叠静默覆盖                      |
| GUI-07  | gui     | High     | 可读性     | removeComponent 中 return 歧义            |
| GUI-08  | gui     | High     | 资源泄漏   | clearPages 在 close 流程中不执行          |
| GUI-09  | gui     | Medium   | 日志       | clearPages 使用 Bukkit.getLogger          |
| GUI-10  | gui     | Medium   | 线程       | SmartCache 每实例独立线程                 |
| GUI-11  | gui     | Medium   | 设计缺陷   | 多 GuiManager 仅活跃一个                  |
| GUI-12  | gui     | Low      | 代码质量   | pop() 末尾无用表达式                      |
| I18N-01 | i18n    | Critical | 逻辑错误   | processTemplate 正则状态损坏              |
| I18N-02 | i18n    | High     | 错误处理   | reloadAll 错误不传播                      |
| I18N-03 | i18n    | High     | 资源泄漏   | 非 Terminable 插件不绑定生命周期          |
| I18N-04 | i18n    | Medium   | 初始化顺序 | guessMajorLanguage 在注册前调用           |
| NET-01  | network | Critical | 逻辑错误   | createHttpClient 忽略 config 参数         |
| NET-02  | network | Critical | 资源管理   | NetworkDSL 临时客户端不绑定               |
| NET-03  | network | Critical | 设计缺陷   | httpGet/httpPost 每次新建 builder         |
| NET-04  | network | High     | 架构缺陷   | 全局共享无插件隔离                        |
| NET-05  | network | High     | 变量遮蔽   | OkHttp engine config 变量名遮蔽           |
| NET-06  | network | High     | API 设计   | BOTH 序列化类型名不符实                   |
| NET-07  | network | High     | 规范       | 全模块 println 替代日志                   |
| NET-08  | network | High     | 错误处理   | asyncHttpRequest 异常吞没                 |
| NET-09  | network | High     | 资源浪费   | terminable 方法每次新建 wrapper           |
| NET-10  | network | High     | 资源泄漏   | SimpleNetworkMonitor.close 空实现         |
| NET-11  | network | High     | 逻辑错误   | 下载完成后才检查大小                      |
| NET-12  | network | Medium   | 精度       | 延迟测量用 currentTimeMillis              |
| NET-13  | network | Medium   | 错误处理   | bodyOrNull 吞没所有异常                   |

---

## 优先修复建议

### 必须立即修复（Critical）
1. **GUI-01**: `performCleanup` NPE — 单行修复
2. **GUI-02**: `currentGuiManager` 死锁 — 重写属性访问逻辑
3. **GUI-08**: `clearPages` 在 `close` 中不执行 — 调整 `closed` 标志设置时机
4. **I18N-01**: 模板处理正则损坏 — 使用 `Matcher.appendReplacement`
5. **CORE-01**: 链式任务 NPE — 在 `thenRun*` 中添加前置任务状态检查
6. **NET-01**: 配置参数被忽略 — 传递 config 到实际客户端创建

### 高优先级（High）
7. 重构 network 模块的全局状态管理，添加插件隔离
8. 重构 `terminableGet` 等方法，复用 wrapper 而非每次新建
9. 全模块 `println` 替换为正式日志系统
10. GUI Session 的 `goto()` 竞态修复

### 中期改进（Medium）
11. Config 路径解析统一
12. SmartCache 共享线程池
13. 日志字符集指定
