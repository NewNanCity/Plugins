# Config 模块示例代码

> 📋 **状态**: 文档规划中，内容正在完善

## 基础示例

### 简单插件配置

```kotlin
// 配置类定义
data class SimplePluginConfig(
    val pluginName: String = "MyPlugin",
    val version: String = "1.0.0",
    val enabled: Boolean = true,
    val debugMode: Boolean = false
) : BasePluginConfig()

// 插件中使用
class SimplePlugin : BasePlugin() {
    private lateinit var config: SimplePluginConfig
    
    override fun onPluginEnable() {
        config = configManager.getPluginConfig()
        logger.info("插件 ${config.pluginName} v${config.version} 已启用")
        
        if (config.debugMode) {
            logger.info("调试模式已启用")
        }
    }
}
```

### 复杂配置结构

```kotlin
// 嵌套配置类
data class ServerConfig(
    val name: String = "Minecraft Server",
    val port: Int = 25565,
    val maxPlayers: Int = 20
)

data class DatabaseConfig(
    val host: String = "localhost",
    val port: Int = 3306,
    val database: String = "minecraft",
    val username: String = "root",
    val password: String = ""
)

data class FeatureConfig(
    val pvp: Boolean = true,
    val flight: Boolean = false,
    val chat: ChatConfig = ChatConfig()
)

data class ChatConfig(
    val enableColors: Boolean = true,
    val maxLength: Int = 256,
    val cooldown: Long = 1000
)

// 主配置类
data class ComplexPluginConfig(
    val server: ServerConfig = ServerConfig(),
    val database: DatabaseConfig = DatabaseConfig(),
    val features: FeatureConfig = FeatureConfig()
) : BasePluginConfig()
```

## 高级示例

### 配置验证示例

```kotlin
data class ValidatedConfig(
    val serverPort: Int = 25565,
    val playerLimit: Int = 100,
    val serverName: String = "My Server"
) : BasePluginConfig() {
    
    override fun validate(): List<String> {
        val errors = mutableListOf<String>()
        
        // 端口范围验证
        if (serverPort !in 1024..65535) {
            errors.add("服务器端口必须在 1024-65535 范围内，当前值: $serverPort")
        }
        
        // 玩家数量验证
        if (playerLimit < 1 || playerLimit > 1000) {
            errors.add("玩家限制必须在 1-1000 范围内，当前值: $playerLimit")
        }
        
        // 服务器名称验证
        if (serverName.isBlank()) {
            errors.add("服务器名称不能为空")
        } else if (serverName.length > 50) {
            errors.add("服务器名称长度不能超过 50 个字符")
        }
        
        return errors
    }
}
```

### 配置迁移示例

```kotlin
data class MigratableConfig(
    val newFeature: String = "default",
    val renamedProperty: Int = 100,
    override val version: String = "2.0.0"
) : BasePluginConfig() {
    
    override fun migrate(oldVersion: String): BasePluginConfig {
        return when (oldVersion) {
            "1.0.0" -> migrateFrom1_0_0()
            "1.5.0" -> migrateFrom1_5_0()
            else -> this
        }
    }
    
    private fun migrateFrom1_0_0(): MigratableConfig {
        logger.info("从版本 1.0.0 迁移配置")
        return this.copy(
            newFeature = "migrated_from_1_0_0",
            renamedProperty = 50 // 旧版本的默认值
        )
    }
    
    private fun migrateFrom1_5_0(): MigratableConfig {
        logger.info("从版本 1.5.0 迁移配置")
        return this.copy(
            newFeature = "migrated_from_1_5_0"
        )
    }
}
```

### 动态配置重载示例

```kotlin
class DynamicConfigPlugin : BasePlugin() {
    private var config: DynamicConfig = DynamicConfig()
    
    override fun onPluginEnable() {
        loadConfig()
        
        // 监听配置文件变化
        configManager.watchConfigFile { path ->
            if (path.endsWith("config.yml")) {
                reloadConfig()
            }
        }
    }
    
    private fun loadConfig() {
        try {
            config = configManager.getPluginConfig()
            applyConfig()
            logger.info("配置加载成功")
        } catch (e: Exception) {
            logger.error("配置加载失败，使用默认配置", e)
            config = DynamicConfig()
        }
    }
    
    private fun reloadConfig() {
        logger.info("检测到配置文件变化，重新加载...")
        val oldConfig = config
        loadConfig()
        
        // 比较配置变化
        if (oldConfig.serverName != config.serverName) {
            logger.info("服务器名称已更改: ${oldConfig.serverName} -> ${config.serverName}")
        }
    }
    
    private fun applyConfig() {
        // 应用配置到各个组件
        serverManager.updateServerName(config.serverName)
        playerManager.updateMaxPlayers(config.maxPlayers)
    }
}

data class DynamicConfig(
    val serverName: String = "Dynamic Server",
    val maxPlayers: Int = 20,
    val enableAutoReload: Boolean = true
) : BasePluginConfig()
```

## 多格式配置示例

### YAML 配置文件

```yaml
# config.yml
server:
  name: "我的 Minecraft 服务器"
  port: 25565
  max-players: 100

database:
  host: "localhost"
  port: 3306
  name: "minecraft_db"
  username: "mc_user"

features:
  pvp: true
  flight: false
  chat:
    enable-colors: true
    max-length: 256
    cooldown: 1000

# 插件特定配置
plugin:
  debug-mode: false
  auto-save-interval: 300
  language: "zh_CN"
```

### JSON 配置文件

```json
{
  "server": {
    "name": "我的 Minecraft 服务器",
    "port": 25565,
    "maxPlayers": 100
  },
  "database": {
    "host": "localhost",
    "port": 3306,
    "name": "minecraft_db",
    "username": "mc_user"
  },
  "features": {
    "pvp": true,
    "flight": false,
    "chat": {
      "enableColors": true,
      "maxLength": 256,
      "cooldown": 1000
    }
  },
  "plugin": {
    "debugMode": false,
    "autoSaveInterval": 300,
    "language": "zh_CN"
  }
}
```

## 实际应用示例

### 经济插件配置

```kotlin
data class EconomyConfig(
    val currency: CurrencyConfig = CurrencyConfig(),
    val banking: BankingConfig = BankingConfig(),
    val shops: ShopConfig = ShopConfig()
) : BasePluginConfig()

data class CurrencyConfig(
    val name: String = "金币",
    val symbol: String = "¥",
    val startingBalance: Double = 1000.0,
    val maxBalance: Double = 1000000.0
)

data class BankingConfig(
    val enableBanks: Boolean = true,
    val interestRate: Double = 0.05,
    val maxLoanAmount: Double = 50000.0
)

data class ShopConfig(
    val enablePlayerShops: Boolean = true,
    val maxShopsPerPlayer: Int = 5,
    val shopRentCost: Double = 100.0
)
```

## 相关文档

- [🚀 快速开始](quick-start.md) - 基本使用方法
- [💡 基础概念](concepts.md) - 配置系统概念
- [📋 API 参考](api-reference.md) - 完整 API 文档
- [💡 最佳实践](best-practices.md) - 开发建议

---

**📝 注意**: 此文档正在完善中，更多示例请参考项目源码中的测试用例。
