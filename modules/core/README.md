# Core模块

核心模块，提供所有插件的基础功能。基于helper库的设计思想，实现了完整的资源生命周期管理体系。

## 功能特性

### Terminable体系

基于helper库的terminable模式，提供强大的资源生命周期管理：

#### 核心接口
- **Terminable** - 可终止资源接口，扩展了AutoCloseable
- **TerminableConsumer** - 资源消费者，管理多个Terminable
- **CompositeTerminable** - 组合资源管理器，支持LIFO关闭顺序

#### 实用工具
- **TerminableTask** - 可终止的Bukkit任务包装器
- **TerminableListener** - 可终止的事件监听器包装器
- **扩展函数** - 简化常用操作的便利函数

### BaseModule模块化架构

现代化的模块开发基类，提供完整的模块化解决方案：

#### 核心特性
- **自动资源管理** - 同时实现Terminable和TerminableConsumer
- **模块级上下文** - 事件、任务、协程绑定到模块而非插件
- **层次化管理** - 支持子模块嵌套和自动生命周期管理
- **完整生命周期** - onInit、onReload、onClose三阶段管理

#### 构造器选项
- **插件子模块** - `BaseModule(moduleName, plugin)`
- **嵌套子模块** - `BaseModule(moduleName, parentModule)`
- **自定义构造** - `BaseModule(moduleName, plugin, logger, messager)`

### 函数式事件处理

融合helper库的事件处理精华，提供现代化的事件订阅API：

#### 核心特性
- **链式API** - 流畅的事件订阅配置
- **过滤器系统** - 丰富的预定义过滤器
- **自动过期** - 支持时间和次数限制
- **异常处理** - 完善的错误处理机制
- **生命周期管理** - 自动绑定到插件生命周期

#### 预定义过滤器
- `EventFilters.ignoreCancelled()` - 忽略已取消的事件
- `EventFilters.ignoreSameBlock()` - 忽略相同方块的移动
- `EventFilters.playerHasPermission()` - 权限检查
- `EventFilters.playerIsOp()` - OP状态检查
- 更多过滤器...

### 增强的调度器系统

结合Kotlin协程，提供现代化的异步编程体验：

#### 调度器特性
- **统一API** - 同步和异步调度的一致接口
- **构建器模式** - 流畅的任务配置API
- **协程集成** - 原生Kotlin协程支持
- **自动管理** - 任务自动绑定到插件生命周期

#### 协程支持
- **BukkitDispatchers** - Bukkit专用协程调度器
- **主线程调度** - 安全的主线程协程执行
- **异步调度** - 高效的异步协程处理
- **上下文切换** - 便捷的线程间切换

### 文本处理系统

整合了helper库的text和text3功能，提供现代化的文本处理API：

#### 核心特性
- **颜色代码处理** - 支持传统颜色代码(&)和十六进制颜色代码(&#RRGGBB)
- **Component支持** - 兼容Kyori Text和Adventure API的Component处理
- **向后兼容** - 完全兼容helper库的text和text3 API
- **现代化API** - 提供Kotlin风格的扩展函数和DSL
- **安全处理** - 所有操作都有空值检查和异常处理

#### 核心组件
- **TextProcessor** - 基础文本处理功能
- **ComponentProcessor** - Component相关处理
- **Text** - 统一API入口，兼容helper库

### 工具集合

整合了原utils模块的所有实用工具，提供完整的开发工具集：

#### 核心工具
- **ReflectionUtils** - 安全的反射操作，支持NMS和CraftBukkit类访问
- **ItemBuilder** - 链式调用构建ItemStack，支持名称、描述、附魔等
- **LocationUtils** - 位置计算、距离测量、区域检测、安全传送等
- **PlayerUtils** - 玩家状态管理、物品操作、权限检查等
- **SkullUtils** - 创建各种类型的玩家头颅和自定义材质头颅
- **TextUtils** - 文本格式化、验证、进度条等（委托给Text类处理基础功能）
- **MinecraftVersion** - Minecraft版本解析、比较和兼容性检查

#### 特性
- **类型安全** - 完整的类型注解和空安全检查
- **扩展函数** - 提供便捷的Kotlin扩展函数
- **异常安全** - 所有操作都有异常处理，避免插件崩溃
- **性能优化** - 反射操作支持缓存，提高性能

### 增强的BasePlugin基类

所有插件的基类，提供：
- **自动资源管理** - 基于Terminable模式的资源管理
- **生命周期管理** - onLoad、onEnable、onDisable三阶段管理
- **服务注册发现** - 简化的Bukkit服务管理
- **事件监听器管理** - 自动注册和注销
- **任务调度** - 便利的任务创建方法
- **异常处理** - 完善的错误处理和日志记录
- **属性访问器** - 现代化的属性访问方式
- **公共API** - 支持外部类调用核心功能

#### 设计模式

BasePlugin采用了统一的属性访问模式：

- **Kotlin优先**：使用属性访问器（如`logger`、`performanceMonitor`、`messager`、`coroutineScope`）
- **Java兼容**：Kotlin属性访问器会自动生成getter方法，特殊情况下提供额外方法（如`getEnhancedLogger()`）

**内部实现**：
- 使用`private lateinit var _propertyName`存储实际实例
- 提供`public val propertyName get() = _propertyName`属性访问器
- Kotlin属性访问器会自动生成对应的getter方法供Java使用

**特殊说明**：
- 由于JavaPlugin已有`getLogger()`方法返回`java.util.logging.Logger`，我们的增强Logger使用`getEnhancedLogger()`方法提供Java兼容
- 其他属性（`performanceMonitor`、`messager`、`coroutineScope`）会自动生成对应的getter方法

**使用建议**：
- **Kotlin代码**：优先使用属性访问器（`logger.info(...)`、`performanceMonitor.monitor(...)`）
- **Java代码**：使用自动生成的getter方法（`getEnhancedLogger().info(...)`、`getPerformanceMonitor().monitor(...)`）

## 使用示例

### 基础插件

```kotlin
class YourPlugin : BasePlugin() {
    override fun onPluginLoad() {
        // 插件加载阶段（可选）
        logger.info("插件正在加载...")
    }

    override fun onPluginEnable() {
        // 插件启用逻辑
        logger.info("插件已启用")

        // 使用属性访问器（推荐方式）
        performanceMonitor.monitor("startup") {
            // 监控启动性能
            initializePlugin()
        }

        // 注册事件监听器（自动管理生命周期）
        registerListener(MyListener())

        // 创建定时任务（自动管理生命周期）
        runTaskTimer(20L, 20L) {
            // 每秒执行一次
            logger.info("定时任务执行")
        }

        // 绑定自定义资源
        bind(MyCustomResource())

        // 绑定模块（模块实现Terminable接口）
        bind(MyModule())

        // 在协程中执行异步操作
        coroutineScope.launch {
            // 异步初始化
            initializeAsync()
        }
    }

    override fun onPluginDisable() {
        // 插件禁用逻辑
        logger.info("插件已禁用")
        // 所有绑定的资源会自动清理
    }

    private fun initializePlugin() {
        // 初始化逻辑
    }

    private suspend fun initializeAsync() {
        // 异步初始化逻辑
    }
}
```

#### 属性访问器

BasePlugin提供了现代化的属性访问器，推荐使用这些属性而不是传统的getter方法：

```kotlin
class YourPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 推荐：使用属性访问器（Kotlin优先）
        logger.info("使用logger属性")
        performanceMonitor.monitor("task") { /* 任务代码 */ }

        // 在协程中执行异步任务
        coroutineScope.launch {
            // 异步操作
        }
    }
}
```

```java
// Java兼容使用
public class MyJavaPlugin extends BasePlugin {
    @Override
    protected void onPluginEnable() {
        // Java使用getter方法
        getEnhancedLogger().info("Java兼容方式");
        getPerformanceMonitor().monitor("task", () -> {
            // 任务代码
            return null;
        });

    }
}
```

**可用的属性访问器：**
- `logger: Logger` - 日志记录器（Kotlin优先）/ `getEnhancedLogger()` - Java兼容
- `messager: MessageManager` - 消息管理器（Kotlin优先）/ `getMessager()` - Java兼容（自动生成）
- `performanceMonitor: PerformanceMonitor` - 性能监控器（Kotlin优先）/ `getPerformanceMonitor()` - Java兼容（自动生成）
- `coroutineScope: CoroutineScope` - 协程作用域（Kotlin优先）/ `getCoroutineScope()` - Java兼容（自动生成）

#### 公共API方法

以下方法现在是公共的，可以在外部类中调用：

```kotlin
// 在其他类中使用BasePlugin的功能
class MyManager(private val plugin: YourPlugin) {
    fun doSomething() {
        // 添加事件订阅
        val subscription = plugin.subscribeEvent<PlayerJoinEvent> { event ->
            // 处理事件
        }
        plugin.addEventSubscription(subscription)

        // 添加调度任务
        val task = plugin.runTaskTimer(20L, 20L) {
            // 定时任务
        }
        plugin.addScheduledTask(task)
    }
}
```

### 自定义Terminable资源

```kotlin
class MyCustomResource : Terminable {
    private val connection = createDatabaseConnection()

    override fun close() {
        connection.close()
        logger.info("数据库连接已关闭")
    }

    override fun isClosed(): Boolean = connection.isClosed
}
```

### BaseModule模块化设计（推荐）

```kotlin
class DatabaseModule(moduleName: String, plugin: BasePlugin) : BaseModule(moduleName, plugin) {
    private lateinit var dataSource: DataSource

    override fun onInit() {
        logger.info("DatabaseModule initializing...")

        // 创建数据源
        dataSource = createDataSource()

        // 定期清理任务（绑定到模块）
        runAsyncRepeating(0L, 20L * 60) {
            dataSource.evictIdleConnections()
        }

        // 注册服务
        plugin.provideService(DataSource::class.java, dataSource)
    }

    override fun onReload() {
        logger.info("DatabaseModule reloading...")
        // 重新加载数据库配置
    }

    override fun onClose() {
        logger.info("DatabaseModule closing...")
        if (::dataSource.isInitialized) {
            dataSource.close()
        }
    }
}

// 在插件中使用
class MyPlugin : BasePlugin() {
    private val databaseModule: DatabaseModule by lazy {
        DatabaseModule("DatabaseModule", this)  // 自动绑定
    }

    override fun onPluginEnable() {
        // 访问时自动初始化
        databaseModule.setupDatabase()
        reloadPlugin()
    }

    override fun reloadPlugin() {
        // 重载所有子模块
        super.reloadPlugin()
    }
}
```

### 传统模块化设计（兼容）

```kotlin
class LegacyDatabaseModule(
    private val plugin: BasePlugin
) : Terminable {
    private val dataSource = createDataSource()

    init {
        // 创建定期清理任务
        plugin.runTaskTimerAsync(0L, 20L * 60) {
            dataSource.evictIdleConnections()
        }

        // 注册服务
        plugin.provideService(DataSource::class.java, dataSource)
    }

    override fun close() {
        dataSource.close()
    }

    override fun isClosed(): Boolean = dataSource.isClosed
}
```

### 函数式事件处理

提供两种API风格：**Kotlin DSL**（推荐）和**Java兼容工厂函数**：

#### Kotlin DSL风格（推荐）

```kotlin
class EventExamplePlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 简单事件订阅（Kotlin DSL）
        events<PlayerJoinEvent> { event ->
            event.player.sendMessage("欢迎加入服务器！")
        }

        // 带过滤器的事件处理（Kotlin DSL）
        events<PlayerMoveEvent> {
            filter(EventFilters.ignoreCancelled())
            filter(EventFilters.ignoreSameBlock())
            filter(EventFilters.playerIsNotOp())
            handler { event ->
                // 只有非OP玩家移动到新方块时才触发
                logger.info("玩家移动到新方块")
            }
        }

        // 自动过期的监听器（Kotlin DSL）
        events<PlayerJoinEvent> {
            expireAfter(5) // 处理5次后自动注销
            handler { event ->
                event.player.sendMessage("前5个加入的玩家！")
            }
        }
    }
}
```

#### Java兼容工厂函数

```kotlin
class JavaCompatiblePlugin : BasePlugin() {
    override fun onPluginEnable() {
        // Java兼容的工厂函数
        subscribeEvent(PlayerJoinEvent::class.java) { event ->
            event.player.sendMessage("欢迎加入服务器！")
        }

        // 带优先级的事件订阅
        subscribeEvent(PlayerMoveEvent::class.java, EventPriority.HIGH) { event ->
            // 处理事件
        }
    }
}
```

### 增强的任务调度

提供两种API风格：**Kotlin DSL**（推荐）和**Java兼容工厂函数**：

#### Kotlin DSL风格（推荐）

```kotlin
class TaskExamplePlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 简单任务调度（Kotlin DSL）
        tasks {
            sync { logger.info("同步任务") }
        }

        tasks {
            async { logger.info("异步任务") }
        }

        // 延迟和重复任务（Kotlin DSL）
        tasks {
            sync {
                delay(20) // 1秒后执行
                run { logger.info("延迟同步任务") }
            }
        }

        tasks {
            async {
                delay(5, TimeUnit.SECONDS)
                repeat(30, TimeUnit.SECONDS)
                run { logger.info("重复异步任务") }
            }
        }

        // 协程支持
        tasks {
            coroutine {
                logger.info("异步协程开始")
                delay(1000) // 异步等待

                withSync {
                    // 切换到主线程
                    logger.info("现在在主线程")
                }
            }
        }
    }
}
```

#### Java兼容工厂函数

```kotlin
class JavaCompatiblePlugin : BasePlugin() {
    override fun onPluginEnable() {
        // Java兼容的工厂函数
        runSync { logger.info("同步任务") }
        runAsync { logger.info("异步任务") }
        runSyncLater(20L) { logger.info("1秒后执行") }

        // 重复任务
        runSyncRepeating(0L, 20L) { task ->
            logger.info("重复同步任务")
        }

        runAsyncRepeating(0L, 100L) { task ->
            logger.info("重复异步任务")
        }
    }
}
```

### 文本处理

提供两种API风格：**Kotlin扩展函数**（推荐）和**Java兼容静态方法**：

#### Kotlin扩展函数风格（推荐）

```kotlin
import city.newnan.core.utils.text.*

class TextExamplePlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 基础颜色代码处理
        val colorized = "&aHello &bWorld!".colorize()
        val stripped = colorized.stripColor()
        val decolorized = colorized.decolorize()

        // Component处理
        val component = "&aWelcome!".toComponent()
        val legacy = component.fromComponent()

        // 消息发送
        player.sendColorizedMessage("&aWelcome to the server!")
        player.sendComponentMessage("&bHello World!")

        // 批量发送
        listOf(player1, player2).sendMessage("&cBroadcast!")

        // 字符串连接
        val joined = Text.joinNewline("Line 1", "Line 2", "Line 3")

        // 格式化
        val formatted = "&aPlayer: {0}, Level: {1}".formatAndColorize("Steve", 25)
    }
}
```

#### Java兼容静态方法

```kotlin
import city.newnan.core.utils.text.Text

class JavaCompatiblePlugin : BasePlugin() {
    override fun onPluginEnable() {
        // Java兼容的静态方法
        val colorized = Text.colorize("&aHello World!")
        val joined = Text.joinNewline("Line 1", "Line 2")
        Text.sendMessage(player, "&aWelcome!")

        // Component处理
        val component = Text.fromLegacy("&aHello")
        val legacy = Text.toLegacy(component)

        // 检查Component支持
        if (Text.isComponentSupported()) {
            // 使用Component功能
        }
    }
}
```

### 工具集合使用

提供丰富的实用工具，简化常见开发任务：

#### 物品构建器

```kotlin
import city.newnan.core.utils.*

class ItemExamplePlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 创建简单物品
        val sword = ItemBuilder.of(Material.DIAMOND_SWORD)
            .name("&c传奇之剑")
            .lore("&7一把传说中的剑", "&7攻击力: &c+10")
            .enchant(Enchantment.DAMAGE_ALL, 5)
            .hideEnchants()
            .unbreakable(true)
            .build()

        // 使用扩展函数
        val helmet = Material.DIAMOND_HELMET.toBuilder()
            .name("&b钻石头盔")
            .hideAll()
            .build()

        // 快速创建
        val apple = ItemBuilder.create(Material.APPLE, "&aGolden Apple", "&7恢复生命值")
    }
}
```

#### 位置和玩家工具

```kotlin
class UtilsExamplePlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 位置工具
        val distance = LocationUtils.distance(loc1, loc2)
        val safeLocation = LocationUtils.findSafeLocation(dangerousLoc)
        val nearbyPlayers = LocationUtils.getPlayersAround(center, 10.0)

        // 使用扩展函数
        val distance2D = loc1.distance2DTo(loc2)
        val isSafe = location.isSafe()
        val serialized = location.serialize()

        // 玩家工具
        val hasSpace = PlayerUtils.hasInventorySpace(player, 5)
        PlayerUtils.giveItem(player, sword)
        PlayerUtils.heal(player)
        PlayerUtils.teleportSafely(player, destination)

        // 使用扩展函数
        player.giveItem(apple)
        player.heal()
        val itemCount = player.countItem(Material.DIAMOND)
    }
}
```

#### 头颅和反射工具

```kotlin
class AdvancedUtilsPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 头颅工具
        val playerSkull = SkullUtils.createPlayerSkull(player)
        val customSkull = SkullUtils.createTextureSkull("texture_url_here")
        val base64Skull = SkullUtils.createSkullFromBase64("base64_data")

        // 使用扩展函数
        val skull1 = player.getSkull()
        val skull2 = "texture_hash".toSkull()
        val skull3 = UUID.randomUUID().toSkull()

        // 反射工具（安全操作）
        val nmsClass = ReflectionUtils.getMinecraftClass("EntityPlayer")
        val field = ReflectionUtils.getDeclaredField(player.javaClass, "handle")
        val method = ReflectionUtils.getMethod(player.javaClass, "getHandle")

        // 安全调用
        val result: String? = ReflectionUtils.safeInvoke(method, player)
        val value: Int? = ReflectionUtils.safeGet(field, player)
        val success = ReflectionUtils.safeSet(field, player, newValue)
    }
}
```

#### 版本检查和文本工具

```kotlin
class VersionAndTextPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 版本检查
        val currentVersion = MinecraftVersion.RUNTIME_VERSION
        if (currentVersion.isAfter(MinecraftVersion.v1_20)) {
            logger.info("支持新版本特性")
        }

        // 文本工具（委托给Text类）
        val progress = TextUtils.progressBar(75.0, 100.0, 20, '█', '░', "&a", "&7")
        val centered = TextUtils.center("标题", 50, '-')
        val formatted = TextUtils.formatNumber(1234567)

        // 使用扩展函数
        val colorized = "&aHello World!".colorize()
        val truncated = "Very long text here".truncate(10)
        val titleCase = "hello_world".toTitleCase()
    }
}
```

#### Java兼容工厂函数

```kotlin
class JavaCompatiblePlugin : BasePlugin() {
    override fun onPluginEnable() {
        // Java兼容的工厂函数
        runSync { logger.info("同步任务") }
        runAsync { logger.info("异步任务") }
        runSyncLater(20L) { logger.info("1秒后执行") }

        // 重复任务
        runSyncRepeating(0L, 20L) { task ->
            logger.info("重复同步任务")
        }

        runAsyncRepeating(0L, 100L) { task ->
            logger.info("重复异步任务")
        }
    }
}
```

## 设计理念

Core模块基于以下设计原则：

### 1. 资源生命周期管理
- 所有资源都应该有明确的生命周期
- 资源应该在适当的时候自动清理
- 防止内存泄漏和资源泄漏

### 2. 模块化设计
- 功能应该组织成独立的模块
- 模块之间应该低耦合、高内聚
- 支持插件式的功能扩展

### 3. 异常安全
- 所有操作都应该是异常安全的
- 异常应该被适当地处理和记录
- 不应该因为单个组件的失败而影响整个系统

### 4. 便利性
- 提供简洁的API
- 减少样板代码
- 支持链式调用和函数式编程

## 模块重构说明

### 已合并的模块
- **common模块** - 已完全合并到core模块
- **logging模块** - 已合并到core模块，提供增强的日志功能
- **cache模块** - 已合并到core模块，提供多种缓存实现
- **utils模块** - 已完全合并到core模块，提供完整的工具集合

### 配置系统

Core模块提供了统一的配置管理系统，支持部分实现：

```kotlin
class YourPlugin : BasePlugin() {
    override fun getCoreConfig(): CorePluginConfig {
        return object : CorePluginConfig() {
            // 只需要重写你需要自定义的配置
            override fun getLoggingConfig(): LoggingConfig = object : LoggingConfig() {
                override val debugEnabled: Boolean = true
                override val fileLoggingEnabled: Boolean = false
            }

            override fun getMessageConfig(): MessageConfig = object : MessageConfig() {
                override val playerPrefix: String = "&7[&6YourPlugin&7] "
            }

            // 不需要重写的配置会使用默认值
            // getPerformanceConfig() 和 getCleanupConfig() 会使用默认实现
        }
    }
}
```

#### 部分实现支持

CorePluginConfig支持部分实现，你只需要重写需要自定义的配置部分：

```kotlin
// 最小实现 - 所有配置都使用默认值
class MinimalConfig : CorePluginConfig()

// 部分实现 - 只自定义日志配置
class PartialConfig : CorePluginConfig() {
    override fun getLoggingConfig(): LoggingConfig = object : LoggingConfig() {
        override val debugEnabled: Boolean = true
    }
    // 其他配置使用默认值
}

// 完整实现 - 自定义所有配置
class FullConfig : CorePluginConfig() {
    override fun getLoggingConfig(): LoggingConfig = MyLoggingConfig()
    override fun getMessageConfig(): MessageConfig = MyMessageConfig()
    override fun getPerformanceConfig(): PerformanceConfig = MyPerformanceConfig()
    override fun getCleanupConfig(): CleanupConfig = MyCleanupConfig()
}
```

#### 默认配置值

如果不重写配置方法，将使用以下默认值：

- **LoggingConfig**: 调试关闭，文件日志关闭，保留7天
- **MessageConfig**: 无前缀，多语言关闭，默认中文
- **PerformanceConfig**: 性能监控关闭，阈值100ms
- **CleanupConfig**: 自动清理开启，间隔30秒

### 配置解耦
Core模块不再直接依赖Bukkit的config系统，而是通过`CorePluginConfig`基类提供配置模板：
- 用户可以继承`CorePluginConfig`来提供自定义配置
- 避免了与config模块的耦合
- 提供了默认值以防止配置错误

### 新增功能
- **Terminable体系** - 完整的资源生命周期管理
- **插件化Logger系统** - 支持多种输出方式的日志系统
- **MessageManager消息管理** - 专注于用户交互和即时反馈
- **配置模板** - `CorePluginConfig`基类提供标准化配置
- **缓存系统** - 提供多种缓存实现，支持LRU、LFU和无限容量缓存
- **文本处理系统** - 整合helper库text和text3功能，提供现代化文本处理API
- **工具集合** - 整合utils模块的所有实用工具，包括反射、物品构建、位置计算等

## 插件化Logger系统

新的Logger系统采用插件化设计，支持多种输出方式，并集成了i18n国际化功能：

### 支持的提供者
- **BukkitConsoleLoggerProvider**: Bukkit控制台输出
- **LogFileLoggerProvider**: 文件日志输出
- **JsonlFileLoggerProvider**: JSONL格式文件输出

### 国际化集成

Logger系统支持可选的i18n国际化功能，当设置了StringFormatter时会自动进行本地化处理，否则使用英文回退：

```kotlin
import city.newnan.core.logging.Logger
import city.newnan.i18n.LanguageManager

class MyPlugin : BasePlugin() {
    private lateinit var languageManager: LanguageManager

    override fun onPluginEnable() {
        // 设置语言管理器（使用BasePlugin的configManager属性）
        languageManager = LanguageManager(this, configManager)
            .register(Locale.SIMPLIFIED_CHINESE, "lang/zh_CN.yml")
            .register(Locale.US, "lang/en_US.yml")
            .setMajorLanguage(Locale.SIMPLIFIED_CHINESE)
            .setDefaultLanguage(Locale.US)

        // 通过BasePlugin统一设置语言提供者，会自动应用到Logger和MessageManager
        setLanguageProvider(languageManager)

        // 现在所有日志消息都会自动国际化
        logger.info("<%plugin.enabled%>")  // 会查找语言文件中的plugin.enabled键
        logger.warn("<%config.outdated%>")
        logger.error("<%database.connection_failed%>")
    }

    /**
     * 重载配置方法 - 所有插件都必须重写此抽象方法
     */
    override fun reloadPlugin() {
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

### 基本使用

```kotlin
import city.newnan.core.logging.Logger
import city.newnan.core.logging.provider.JsonlFileLoggerProvider

class MyPlugin : BasePlugin() {

    override fun onPluginEnable() {
        // 使用属性访问器获取Logger实例（Kotlin优先方式）
        logger.enableJsonlLogging()

        // 基本日志记录（支持i18n）
        logger.info("<%plugin.enabled%>")
        logger.warn("<%config.warning%>")
        logger.error("<%error.occurred%>", RuntimeException("示例异常"))
        logger.debug("<%debug.info%>")

        // 特殊日志类型（自动使用i18n模板）
        logger.performance("<%database.query%>", 150L)  // 使用<%logger.performance%>模板
        logger.playerAction("Player1", "<%player.login%>", "IP: 192.168.1.1")  // 使用<%logger.player_action%>模板
        logger.adminAction("Admin1", "<%admin.kick_player%>", "Player2", "\<%violation.behavior%\>")  // 使用<%logger.admin_action%>模板

        // 使用性能监控器（属性访问器方式）
        performanceMonitor.monitor("<%database.initialization%>") {
            // 初始化数据库
        }

        // 使用消息管理器（属性访问器方式）
        messager.printf(player, "<%welcome.message%>", player.name)
    }
}
```

#### Java兼容使用

```java
public class MyJavaPlugin extends BasePlugin {
    @Override
    protected void onPluginEnable() {
        // Java使用getter方法
        getEnhancedLogger().info("<%plugin.enabled%>");
        getPerformanceMonitor().monitor("database_init", () -> {
            // 初始化数据库
            return null;
        });
        getMessager().printf(player, "<%welcome.message%>", player.getName());
    }
}
```

### 语言文件示例

创建语言文件 `lang/zh_CN.yml`：

```yaml
plugin:
  enabled: "插件已启用"
  disabled: "插件已禁用"

logger:
  debug_enabled: "调试模式已启用"
  debug_disabled: "调试模式已禁用"
  jsonl_enabled: "JSONL日志已启用"
  cleanup_completed: "日志清理完成"
  shutting_down: "日志系统正在关闭..."
  performance: "性能统计: {0} 耗时 {1}ms"
  player_action: "玩家操作: {0} -> {1}"
  player_action_with_details: "玩家操作: {0} -> {1} ({2})"
  admin_action: "管理员操作: {0} -> {1}"
  admin_action_with_target: "管理员操作: {0} -> {1} 目标:{2}"
  admin_action_with_details: "管理员操作: {0} -> {1} ({2})"
  admin_action_full: "管理员操作: {0} -> {1} 目标:{2} ({3})"

config:
  warning: "配置文件需要更新"
  outdated: "配置文件已过时"

error:
  occurred: "发生错误"

database:
  connection_failed: "数据库连接失败"
```

对应的英文文件 `lang/en_US.yml`：

```yaml
plugin:
  enabled: "Plugin enabled"
  disabled: "Plugin disabled"

logger:
  debug_enabled: "Debug mode enabled"
  debug_disabled: "Debug mode disabled"
  jsonl_enabled: "JSONL logging enabled"
  cleanup_completed: "Log cleanup completed"
  shutting_down: "Logger system shutting down..."
  performance: "Performance: {0} took {1}ms"
  player_action: "Player action: {0} -> {1}"
  player_action_with_details: "Player action: {0} -> {1} ({2})"
  admin_action: "Admin action: {0} -> {1}"
  admin_action_with_target: "Admin action: {0} -> {1} target:{2}"
  admin_action_with_details: "Admin action: {0} -> {1} ({2})"
  admin_action_full: "Admin action: {0} -> {1} target:{2} ({3})"

config:
  warning: "Configuration file needs update"
  outdated: "Configuration file is outdated"

error:
  occurred: "An error occurred"

database:
  connection_failed: "Database connection failed"
```

## MessageManager消息管理系统

MessageManager专注于用户交互和即时反馈，与Logger形成互补关系：

### 功能特点
- **用户交互**: 专注于玩家消息和命令反馈
- **格式化输出**: 支持颜色代码和参数格式化
- **多语言支持**: 可插拔的语言提供者
- **独立设计**: 直接使用Bukkit Logger，避免调用LoggerProvider
- **默认集成**: BasePlugin自动注册MessageManager

### 基本使用

```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 创建并设置语言提供者（推荐方式）
        val languageManager = LanguageManager(this, configManager)
            .register(Locale.SIMPLIFIED_CHINESE, "lang/zh_CN.yml")
            .register(Locale.US, "lang/en_US.yml")
            .setMajorLanguage(Locale.SIMPLIFIED_CHINESE)
            .setDefaultLanguage(Locale.US)

        // 通过BasePlugin统一设置，会自动应用到Logger和MessageManager
        setLanguageProvider(languageManager)

        // 日志记录（使用Logger）
        logger.info("<%plugin.initialized%>")
        logger.warn("<%config.needs_update%>")
        logger.error("<%database.connection_failed%>")

        // 调试消息（根据配置决定是否显示）
        logger.debug("<%debug.info%>")
    }

    /**
     * 重载配置 - 标准实现
     */
    override fun reloadPlugin() {
        try {
            // 重新加载配置和语言文件
            // 具体实现根据插件需求而定
            logger.info("<%config.reloaded%>")
        } catch (e: Exception) {
            logger.error("<%config.reload_failed%>", e)
            throw e
        }
    }

    // 在命令处理中使用
    fun onCommand(sender: CommandSender, args: Array<String>) {
        // 向玩家发送消息
        messager.printf(sender, "<%command.success%>")

        // 格式化消息
        messager.printf(sender, "<%player.level_display%>", "Player1", 25)

        // 向控制台发送管理员操作记录
        messager.printf("<%admin.command_executed%>", sender.name, args.joinToString(" "))
    }
}
```

### 配置MessageManager

通过重写`getCoreConfig()`方法来配置MessageManager：

```kotlin
class MyPlugin : BasePlugin() {
    override fun getCoreConfig(): CorePluginConfig = MyPluginConfig()
}

class MyPluginConfig : CorePluginConfig() {
    override fun getMessageConfig(): MessageConfig = object : MessageConfig() {
        override val playerPrefix: String = "&7[&6MyPlugin&7] "
        override val consolePrefix: String = "[MyPlugin] "
    }
}
```

## 缓存系统

Core模块提供了完整的缓存系统，支持多种缓存策略：

### 支持的缓存类型

#### 1. LRU缓存 (Least Recently Used)
最近最少使用缓存，适用于有时间局部性的访问模式：

```kotlin
import city.newnan.core.cache.LRUCache

class DataService {
    private val cache = LRUCache<String, UserData>(100) // 容量为100

    fun getUserData(userId: String): UserData? {
        return cache.getOrPut(userId) {
            // 从数据库加载用户数据
            loadUserDataFromDatabase(userId)
        }
    }
}
```

#### 2. LFU缓存 (Least Frequently Used)
最少使用频率缓存，适用于访问模式相对稳定的场景：

```kotlin
import city.newnan.core.cache.LFUCache

class ConfigService {
    private val cache = LFUCache<String, ConfigData>(50) // 容量为50

    fun getConfig(key: String): ConfigData? {
        return cache.getOrPut(key) {
            // 从配置文件加载
            loadConfigFromFile(key)
        }
    }
}
```

#### 3. 无限容量缓存 (InfiniteCache)
基于HashMap的无限容量缓存，适用于不需要内存限制的场景：

```kotlin
import city.newnan.core.cache.InfiniteCache

class TranslationService {
    private val cache = InfiniteCache<String, String>(0) // 容量参数仅用于接口兼容

    fun translate(key: String, language: String): String {
        val cacheKey = "$language:$key"
        return cache.getOrPut(cacheKey) {
            // 从翻译文件加载
            loadTranslation(key, language)
        }
    }
}
```

### 缓存接口

所有缓存实现都遵循统一的`Cache<K, V>`接口：

```kotlin
interface Cache<K, V> {
    // 基本操作
    fun put(key: K, value: V): V?
    fun get(key: K): V?
    fun remove(key: K): V?
    fun clear()

    // 属性
    val size: Int
    val capacity: Int
    val keys: Set<K>
    val values: Collection<V>
    val entries: Set<Map.Entry<K, V>>

    // 便利方法
    fun getOrDefault(key: K, defaultValue: V): V
    fun getOrPut(key: K, defaultValue: () -> V): V
    fun forEach(action: (key: K, value: V) -> Unit)

    // 操作符重载
    operator fun get(key: K): V?
    operator fun set(key: K, value: V)
}
```

### 使用建议

1. **LRU缓存**: 适用于热点数据缓存，如用户会话、最近访问的文件等
2. **LFU缓存**: 适用于访问频率差异明显的数据，如配置项、静态资源等
3. **无限缓存**: 适用于数据量可控且需要长期保存的场景，如翻译文本、计算结果等

### 性能特点

| 缓存类型 | 时间复杂度 | 空间复杂度 | 适用场景     |
| -------- | ---------- | ---------- | ------------ |
| LRU      | O(1)       | O(n)       | 时间局部性强 |
| LFU      | O(log n)   | O(n)       | 频率差异明显 |
| Infinite | O(1)       | O(n)       | 无容量限制   |

## 依赖关系

Core模块是最基础的模块，只依赖：
- PaperMC API
- Kotlin标准库

其他模块可以依赖Core模块来获得基础功能：
- `:modules:config` - 高级配置管理
- `:modules:database` - 数据库支持
- `:modules:utils` - 工具类
- `:modules:network` - 网络功能
- 等等...

## 📚 文档

- [快速开始](../../docs/core/quick-start.md) - 快速上手指南
- [BaseModule模块化架构](../../docs/core/base-module.md) - 现代化模块开发指南 ⭐
- [最佳实践](../../docs/core/best-practices.md) - 开发最佳实践
- [事件系统教程](../../docs/core/event-system-tutorial.md) - 详细的事件处理教程
- [调度器教程](../../docs/core/scheduler-tutorial.md) - 任务调度详细教程
- [协程系统文档](../../docs/core/coroutines.md) - 协程使用指南
- [故障排除](../../docs/core/troubleshooting.md) - 常见问题解决方案
