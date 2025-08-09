# 生命周期管理

Core 模块提供了完整的生命周期管理体系，包括插件三阶段生命周期、模块生命周期、资源自动管理和配置重载机制。

## 🎯 核心概念

### 插件生命周期
- **onPluginLoad** - 插件加载阶段，基础初始化
- **onPluginEnable** - 插件启用阶段，主要功能初始化
- **onPluginDisable** - 插件禁用阶段，资源清理
- **reloadPlugin** - 配置重载，可重载功能的重新初始化

### 模块生命周期
- **onInit** - 模块初始化，绑定事件和任务
- **onReload** - 模块重载，重新加载配置
- **onClose** - 模块关闭，资源清理（自动调用）

## 🚀 插件生命周期

### 标准生命周期实现

```kotlin
class MyPlugin : BasePlugin() {

    // 1. 加载阶段 - 插件类加载时调用
    override fun onPluginLoad() {
        // 基础初始化，不依赖其他插件
        logger.info("插件正在加载...")
        initializeBasicComponents()
    }

    // 2. 启用阶段 - 插件启用时调用
    override fun onPluginEnable() {
        logger.info("插件正在启用...")

        // 检查依赖（不可重载）
        if (!checkDependencies()) {
            logger.error("依赖检查失败")
            server.pluginManager.disablePlugin(this)
            return
        }

        // 注册命令（不可重载）
        registerCommands()

        // 注册事件监听器（不可重载）
        registerEventListeners()

        // 调用重载方法处理可重载功能
        reloadPlugin()

        logger.info("插件启用完成")
    }

    // 3. 禁用阶段 - 插件禁用时调用
    override fun onPluginDisable() {
        // 可选的清理逻辑
        logger.info("插件正在禁用...")
        saveImportantData()
        // 大部分资源会自动清理
    }

    // 重载方法 - 配置重载时调用
    override fun reloadPlugin() {
        try {
            logger.info("正在重载配置...")

            // 1. 清理配置缓存（必需）
            configManager.clearCache()

            // 2. 重新设置语言管理器（必需）
            setupLanguageManager(
                languageFiles = mapOf(
                    Locale.SIMPLIFIED_CHINESE to "lang/zh_CN.yml",
                    Locale.US to "lang/en_US.yml"
                ),
                majorLanguage = Locale.SIMPLIFIED_CHINESE,
                defaultLanguage = Locale.US
            )

            // 3. 重载所有子模块（必需）
            super.reloadPlugin()

            logger.info("配置重载完成")
        } catch (e: Exception) {
            logger.error("配置重载失败", e)
            throw e
        }
    }
}
```

### 生命周期最佳实践

```kotlin
class MyPlugin : BasePlugin() {

    override fun onPluginEnable() {
        // ✅ 在启用阶段进行的操作

        // 1. 依赖检查（不可重载）
        if (!setupDependencies()) {
            logger.error("依赖检查失败")
            server.pluginManager.disablePlugin(this)
            return
        }

        // 2. 注册命令（不可重载）
        registerCommands()

        // 3. 注册事件监听器（不可重载）
        registerEventListeners()

        // 4. 调用重载方法（可重载）
        reloadPlugin()
    }

    override fun reloadPlugin() {
        // ✅ 在重载方法中进行的操作

        // 1. 清理配置缓存
        configManager.clearCache()

        // 2. 重新设置语言管理器
        setupLanguageManager()

        // 3. 重载模块配置
        super.reloadPlugin() // 重载所有子模块

        // 4. 重新初始化可变功能
        setupDynamicFeatures()
    }

    private fun setupDependencies(): Boolean {
        // 检查必需的依赖插件
        return server.pluginManager.getPlugin("Vault") != null
    }

    private fun registerCommands() {
        // 注册命令（不可重载）
    }

    private fun registerEventListeners() {
        // 注册事件监听器（不可重载）
    }

    private fun setupDynamicFeatures() {
        // 设置可重载的功能
    }
}
```

## 📦 模块生命周期

### BaseModule 生命周期

```kotlin
class PlayerModule(moduleName: String, val plugin: MyPlugin) : BaseModule(moduleName, plugin) {

    override fun onInit() {
        // 模块初始化逻辑
        logger.info("PlayerModule 正在初始化...")

        // 绑定事件（自动管理生命周期）
        subscribeEvent<PlayerJoinEvent> { event ->
            handlePlayerJoin(event.player)
        }

        subscribeEvent<PlayerQuitEvent> { event ->
            handlePlayerQuit(event.player)
        }

        // 绑定任务（自动管理生命周期）
        runAsyncRepeating(0L, 20L * 60) {
            cleanupPlayerData()
        }

        logger.info("PlayerModule 初始化完成")
    }

    override fun onReload() {
        // 模块重载逻辑
        logger.info("PlayerModule 正在重载...")

        // 重新加载模块配置
        reloadPlayerConfig()

        // 清理缓存
        clearPlayerCache()

        logger.info("PlayerModule 重载完成")
    }

    override fun onClose() {
        // 可选的清理逻辑
        logger.info("PlayerModule 正在关闭...")

        // 保存重要数据
        savePlayerData()

        // 调用父类清理
        super.onClose()
    }

    private fun handlePlayerJoin(player: Player) {
        // 玩家加入处理
    }

    private fun handlePlayerQuit(player: Player) {
        // 玩家退出处理
    }

    private fun cleanupPlayerData() {
        // 清理玩家数据
    }

    private fun reloadPlayerConfig() {
        // 重载玩家配置
    }

    private fun clearPlayerCache() {
        // 清理玩家缓存
    }

    private fun savePlayerData() {
        // 保存玩家数据
    }
}
```

### 模块层次化管理

```kotlin
class EconomyModule(moduleName: String, val plugin: MyPlugin) : BaseModule(moduleName, plugin) {

    // 子模块
    private lateinit var bankModule: BankModule
    private lateinit var shopModule: ShopModule

    override fun onInit() {
        logger.info("EconomyModule 正在初始化...")

        // 初始化子模块
        bankModule = BankModule("BankModule", this)
        shopModule = ShopModule("ShopModule", this)

        // 调用子模块方法
        bankModule.setupBankSystem()
        shopModule.setupShopSystem()

        logger.info("EconomyModule 初始化完成")
    }

    override fun onReload() {
        // 父模块重载时，子模块会自动重载
        logger.info("EconomyModule 正在重载...")

        // 重载经济配置
        plugin.getEconomyConfig().let { config ->
            applyEconomyConfig(config)
        }

        logger.info("EconomyModule 重载完成")
    }
}

// 子模块
class BankModule(moduleName: String, parentModule: BaseModule) : BaseModule(moduleName, parentModule) {

    override fun onInit() {
        logger.info("BankModule 正在初始化...")

        // 子模块的事件和任务也绑定到自己
        subscribeEvent<PlayerInteractEvent> { event ->
            handleBankInteraction(event)
        }

        logger.info("BankModule 初始化完成")
    }

    fun setupBankSystem() {
        // 银行系统设置
    }

    private fun handleBankInteraction(event: PlayerInteractEvent) {
        // 银行交互处理
    }
}
```

## ♻️ 资源自动管理

### 自动绑定机制

```kotlin
class MyPlugin : BasePlugin() {

    override fun onPluginEnable() {
        // 所有资源自动绑定到插件生命周期

        // 事件监听器自动绑定
        subscribeEvent<PlayerJoinEvent> { event ->
            // 插件禁用时自动注销
        }

        // 任务调度自动绑定
        runSyncRepeating(0L, 20L) {
            // 插件禁用时自动取消
        }

        // 自定义资源绑定
        val customResource = CustomResource()
        bind(customResource) // 插件禁用时自动调用 close()
    }
}

class CustomResource : Terminable {
    override fun close() {
        // 资源清理逻辑
    }
}
```

### 模块级资源管理

```kotlin
class DataModule(moduleName: String, val plugin: MyPlugin) : BaseModule(moduleName, plugin) {

    override fun onInit() {
        // 模块级资源管理
        val databaseConnection = DatabaseConnection()
        val cacheManager = CacheManager()

        // 绑定到模块（模块关闭时自动清理）
        bind(databaseConnection)
        bind(cacheManager)

        // 事件绑定到模块
        subscribeEvent<PlayerJoinEvent> { event ->
            // 模块关闭时自动注销
        }

        // 任务绑定到模块
        runAsyncRepeating(0L, 20L * 60) {
            // 模块关闭时自动取消
        }
    }
}
```

## 🔄 配置重载机制

### 重载流程

```kotlin
class MyPlugin : BasePlugin() {

    override fun reloadPlugin() {
        try {
            logger.info("开始重载配置...")

            // 第一步：清理配置缓存
            configManager.clearCache()
            logger.debug("配置缓存已清理")

            // 第二步：重新设置语言管理器
            setupLanguageManager(
                languageFiles = mapOf(
                    Locale.SIMPLIFIED_CHINESE to "lang/zh_CN.yml",
                    Locale.US to "lang/en_US.yml"
                ),
                majorLanguage = Locale.SIMPLIFIED_CHINESE,
                defaultLanguage = Locale.US
            )
            logger.debug("语言管理器已重新设置")

            // 第三步：重载插件特定配置
            reloadPluginSpecificConfig()
            logger.debug("插件配置已重载")

            // 第四步：重载所有子模块
            super.reloadPlugin()
            logger.debug("所有子模块已重载")

            logger.info("配置重载完成")

        } catch (e: Exception) {
            logger.error("配置重载失败", e)
            throw e
        }
    }

    private fun reloadPluginSpecificConfig() {
        // 重载插件特定的配置
        val config = getPluginConfig()
        applyConfiguration(config)
    }
}
```

### 模块重载

```kotlin
class ConfigurableModule(moduleName: String, val plugin: MyPlugin) : BaseModule(moduleName, plugin) {

    private var moduleConfig: ModuleConfig? = null

    override fun onInit() {
        // 初始化时加载配置
        loadConfig()
        setupWithConfig()
    }

    override fun onReload() {
        // 重载时重新加载配置
        logger.info("正在重载模块配置...")

        try {
            loadConfig()
            setupWithConfig()
            logger.info("模块配置重载完成")
        } catch (e: Exception) {
            logger.error("模块配置重载失败", e)
            throw e
        }
    }

    private fun loadConfig() {
        moduleConfig = plugin.configManager.parse<ModuleConfig>("modules/${moduleName.lowercase()}.yml")
    }

    private fun setupWithConfig() {
        moduleConfig?.let { config ->
            if (config.enabled) {
                // 根据配置设置模块
                applyModuleConfig(config)
            }
        }
    }
}
```

## 🛡️ 异常安全的生命周期

### 安全的初始化

```kotlin
class MyPlugin : BasePlugin() {

    override fun onPluginEnable() {
        try {
            // 检查依赖
            if (!checkDependencies()) {
                throw IllegalStateException("依赖检查失败")
            }

            // 初始化组件
            initializeComponents()

            // 注册功能
            registerFeatures()

            // 重载配置
            reloadPlugin()

            logger.info("插件启用成功")

        } catch (e: Exception) {
            logger.error("插件启用失败", e)

            // 禁用插件
            server.pluginManager.disablePlugin(this)
            throw e
        }
    }

    override fun reloadPlugin() {
        try {
            logger.info("正在重载配置...")

            // 重载逻辑
            performReload()

            logger.info("配置重载成功")

        } catch (e: Exception) {
            logger.error("配置重载失败", e)

            // 可以选择回滚到默认配置
            loadDefaultConfiguration()
            throw e
        }
    }
}
```

### 模块异常处理

```kotlin
class RobustModule(moduleName: String, val plugin: MyPlugin) : BaseModule(moduleName, plugin) {

    override fun onInit() {
        try {
            logger.info("模块正在初始化...")

            // 可能失败的初始化
            initializeCriticalComponents()

            logger.info("模块初始化成功")

        } catch (e: Exception) {
            logger.error("模块初始化失败", e)

            // 提供降级功能
            initializeFallbackComponents()
        }
    }

    override fun onReload() {
        try {
            logger.info("模块正在重载...")

            // 重载逻辑
            reloadModuleConfig()

            logger.info("模块重载成功")

        } catch (e: Exception) {
            logger.error("模块重载失败", e)

            // 保持当前状态，不影响运行
            logger.warning("模块将继续使用当前配置")
        }
    }
}
```

---

**相关文档：** [📦 BaseModule](base-module.md) | [♻️ 资源管理](terminable.md) | [🔧 BasePlugin](base-plugin.md)
