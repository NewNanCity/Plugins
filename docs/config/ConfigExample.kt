@file:Suppress("unused")

package city.newnan.config.examples

import city.newnan.config.ConfigManager
import org.bukkit.plugin.java.JavaPlugin

/**
 * 配置管理器使用示例
 *
 * 展示如何使用 ConfigManager 进行配置管理
 * 注意：这个示例使用JavaPlugin，如果使用BasePlugin则应该使用configManager属性
 *
 * @author Gk0Wk
 * @since 1.0.0
 */
class ConfigExample : JavaPlugin() {

    private lateinit var configManager: ConfigManager

    override fun onEnable() {
        // 初始化配置管理器（JavaPlugin需要手动创建）
        configManager = ConfigManager(this)

        // 示例 1: 基本配置操作
        basicConfigExample()

        // 示例 2: 类型安全配置
        typeSafeConfigExample()

        // 示例 3: 多格式支持
        multiFormatExample()

        // 示例 4: 配置验证和迁移
        configValidationExample()
    }

    /**
     * 基本配置操作示例
     */
    private fun basicConfigExample() {
        logger.info("<%config.example.basic_header%>")

        // 获取配置文件
        val config = configManager.get("config.yml")

        // 读取配置值
        val serverName = config.get("server.name", "Default Server")
        val maxPlayers = config.get("server.max-players", 20)
        val enableDebug = config.get("debug.enabled", false)

        logger.info("<%config.example.server_name%>", serverName)
        logger.info("<%config.example.max_players%>", maxPlayers)
        logger.info("<%config.example.debug_mode%>", enableDebug)

        // 修改配置值
        config.set("server.last-startup", System.currentTimeMillis())
            .set("server.version", description.version)
            .set("stats.total-startups", config.get("stats.total-startups", 0) + 1)
            .save()

        logger.info("<%config.example.updated_saved%>")
    }

    /**
     * 类型安全配置示例
     */
    private fun typeSafeConfigExample() {
        logger.info("<%config.example.type_safe_header%>")

        // 定义配置数据类
        data class DatabaseConfig(
            val host: String = "localhost",
            val port: Int = 3306,
            val database: String = "minecraft",
            val username: String = "root",
            val password: String = "",
            val ssl: Boolean = false
        )

        data class ServerConfig(
            val name: String = "Default Server",
            val maxPlayers: Int = 20,
            val motd: String = "Welcome to our server!",
            val features: List<String> = listOf("pvp", "economy"),
            val database: DatabaseConfig = DatabaseConfig()
        )

        // 创建默认配置
        val defaultConfig = ServerConfig(
            name = "My Awesome Server",
            maxPlayers = 50,
            motd = "Welcome to My Awesome Server!",
            features = listOf("pvp", "economy", "shops", "quests"),
            database = DatabaseConfig(
                host = "localhost",
                port = 3306,
                database = "myserver",
                username = "minecraft",
                password = "secret123",
                ssl = true
            )
        )

        // 保存配置
        configManager.save(defaultConfig, "server.yml")
        logger.info("<%config.example.default_server_saved%>")

        // 读取配置
        val serverConfig = configManager.parse<ServerConfig>("server.yml")
        logger.info("<%config.example.server_config_loaded%>", serverConfig)

        // 修改配置
        val updatedConfig = serverConfig.copy(
            maxPlayers = 100,
            features = serverConfig.features + "minigames"
        )
        configManager.save(updatedConfig, "server.yml")
        logger.info("<%config.example.server_config_updated%>")
    }

    /**
     * 多格式支持示例
     */
    private fun multiFormatExample() {
        logger.info("<%config.example.multi_format_header%>")

        // 检查支持的格式
        val supportedFormats = configManager.getSupportedFormats()
        logger.info("<%config.example.supported_formats%>")
        supportedFormats.forEach { (format, available) ->
            val status = if (available) "✓ 可用" else "✗ 缺失依赖"
            logger.info("<%config.example.format_status%>", format, status)
        }

        // 创建测试数据
        val testData = mapOf(
            "name" to "Test Configuration",
            "version" to "1.0.0",
            "settings" to mapOf(
                "debug" to true,
                "timeout" to 30,
                "features" to listOf("feature1", "feature2", "feature3")
            )
        )

        // 保存为不同格式
        val availableFormats = configManager.getAvailableFormats()
        availableFormats.forEach { format ->
            try {
                val fileName = "test-config.${format.lowercase()}"
                configManager.save(testData, fileName, format)
                logger.info("<%config.example.format_saved%>", format, fileName)
            } catch (e: Exception) {
                logger.warning("<%config.example.format_save_failed%>", format, e.message)
            }
        }

        // 格式转换示例
        if (availableFormats.contains("JSON") && availableFormats.contains("YAML")) {
            val yamlConfig = configManager.get("test-config.yaml")
            val jsonConfig = yamlConfig.clone("test-config-converted.json", "JSON")
            logger.info("<%config.example.yaml_to_json_converted%>")
        }
    }

    /**
     * 配置验证和迁移示例
     */
    private fun configValidationExample() {
        logger.info("<%config.example.validation_header%>")

        val config = configManager.get("config.yml")

        // 配置验证
        val requiredKeys = listOf("server.name", "server.max-players", "database.host")
        val missingKeys = requiredKeys.filter { !config.has(it) }

        if (missingKeys.isNotEmpty()) {
            logger.warning("<%config.example.missing_keys%>", missingKeys.joinToString(", "))

            // 自动补充缺失的配置
            missingKeys.forEach { key ->
                when (key) {
                    "server.name" -> config.set(key, "Default Server")
                    "server.max-players" -> config.set(key, 20)
                    "database.host" -> config.set(key, "localhost")
                }
            }
            config.save()
            logger.info("<%config.example.missing_keys_added%>")
        }

        // 配置迁移示例
        if (config.has("old-setting")) {
            val oldValue = config.get("old-setting", "")
            config.set("new-setting", oldValue)
                .remove("old-setting")
                .save()
            logger.info("<%config.example.old_setting_migrated%>")
        }

        // 配置版本检查
        val configVersion = config.get("config-version", 1)
        val currentVersion = 2

        if (configVersion < currentVersion) {
            logger.info("<%config.example.version_upgrade_start%>", configVersion, currentVersion)

            // 执行配置升级逻辑
            when (configVersion) {
                1 -> {
                    // 从版本 1 升级到版本 2
                    config.set("new-feature.enabled", true)
                        .set("config-version", 2)
                    logger.info("<%config.example.version_upgraded%>", 1, 2)
                }
            }

            config.save()
            logger.info("<%config.example.upgrade_completed%>")
        }
    }

    override fun onDisable() {
        // 清理资源
        if (::configManager.isInitialized) {
            configManager.close()
            logger.info("<%config.example.manager_closed%>")
        }
    }
}
