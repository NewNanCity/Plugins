package city.newnan.config

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import io.mockk.*
import org.bukkit.plugin.Plugin
import java.io.File
import java.nio.file.Path

/**
 * 配置文件合并功能测试
 */
class ConfigMergeTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var mockPlugin: Plugin
    private lateinit var configManager: ConfigManager

    @BeforeEach
    fun setUp() {
        mockPlugin = mockk<Plugin>()
        every { mockPlugin.dataFolder } returns tempDir.toFile()
        every { mockPlugin.logger } returns mockk<java.util.logging.Logger>()

        configManager = ConfigManager(mockPlugin)
    }

    @Test
    fun `test deep merge preserves existing values`() {
        // 创建现有配置文件
        val configFile = File(tempDir.toFile(), "test.yml")
        configFile.writeText("""
            server:
              name: "My Custom Server"
              port: 25565
            features:
              - "pvp"
        """.trimIndent())

        // 创建模板配置
        val templateContent = """
            server:
              name: "Default Server"
              port: 25565
              maxPlayers: 100
              motd: "Welcome!"
            features:
              - "pvp"
              - "economy"
            database:
              host: "localhost"
              port: 3306
        """.trimIndent()

        // 模拟资源文件
        every { mockPlugin.getResource("test.yml") } returns templateContent.byteInputStream()

        // 执行合并
        val result = configManager.touchWithMerge("test.yml")

        // 验证结果
        assertFalse(result, "Should return false when file was modified")

        // 读取合并后的配置
        val config = configManager.get("test.yml")

        // 验证现有值被保留
        assertEquals("My Custom Server", config.get("server.name", ""))
        assertEquals(25565, config.get("server.port", 0))

        // 验证新值被添加
        assertEquals(100, config.get("server.maxPlayers", 0))
        assertEquals("Welcome!", config.get("server.motd", ""))
        assertEquals("localhost", config.get("database.host", ""))
        assertEquals(3306, config.get("database.port", 0))
    }

    @Test
    fun `test merge with non-existing file creates new file`() {
        val templateContent = """
            server:
              name: "Default Server"
              port: 25565
        """.trimIndent()

        every { mockPlugin.getResource("new.yml") } returns templateContent.byteInputStream()

        val result = configManager.touchWithMerge("new.yml")

        assertFalse(result, "Should return false when new file was created")
        assertTrue(File(tempDir.toFile(), "new.yml").exists())
    }

    @Test
    fun `test merge with identical files returns true`() {
        val content = """
            server:
              name: "Test Server"
              port: 25565
        """.trimIndent()

        // 创建相同的现有文件和模板
        val configFile = File(tempDir.toFile(), "identical.yml")
        configFile.writeText(content)

        every { mockPlugin.getResource("identical.yml") } returns content.byteInputStream()

        val result = configManager.touchWithMerge("identical.yml")

        assertTrue(result, "Should return true when no changes were needed")
    }
}
