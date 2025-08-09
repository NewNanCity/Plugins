package city.newnan.config

import city.newnan.config.formats.ConfigFormatRegistry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * 简单的配置测试类
 */
class SimpleConfigTest {
    
    @Test
    fun `test format registry initialization`() {
        // 测试格式注册表初始化
        val supportedFormats = ConfigFormatRegistry.getRegisteredFormats()
        
        // 应该包含核心格式
        assertTrue(supportedFormats.contains("json"))
        assertTrue(supportedFormats.contains("yaml"))
        
        // 测试核心格式可用性
        val jsonPlugin = ConfigFormatRegistry.getByFormat("json")
        assertNotNull(jsonPlugin)
        assertTrue(jsonPlugin!!.isAvailable())
        assertTrue(jsonPlugin.isCoreFormat)
        
        val yamlPlugin = ConfigFormatRegistry.getByFormat("yaml")
        assertNotNull(yamlPlugin)
        assertTrue(yamlPlugin!!.isAvailable())
        assertTrue(yamlPlugin.isCoreFormat)
    }
    
    @Test
    fun `test format plugin properties`() {
        val jsonPlugin = ConfigFormatRegistry.getByFormat("json")
        assertNotNull(jsonPlugin)
        
        assertEquals("JSON", jsonPlugin!!.formatName)
        assertTrue(jsonPlugin.supportedExtensions.contains("json"))
        assertTrue(jsonPlugin.isCoreFormat)
        assertTrue(jsonPlugin.isAvailable())
    }
    
    @Test
    fun `test optional format availability`() {
        // 测试可选格式的可用性检查
        val tomlPlugin = ConfigFormatRegistry.getByFormat("toml")
        assertNotNull(tomlPlugin)
        assertFalse(tomlPlugin!!.isCoreFormat)
        
        // TOML 格式在测试环境中应该可用（因为我们添加了测试依赖）
        assertTrue(tomlPlugin.isAvailable())
    }
    
    @Test
    fun `test unknown format`() {
        val unknownPlugin = ConfigFormatRegistry.getByFormat("unknown")
        assertNull(unknownPlugin)
    }
    
    @Test
    fun `test extension mapping`() {
        val jsonPlugin = ConfigFormatRegistry.getByExtension("json")
        assertNotNull(jsonPlugin)
        assertEquals("JSON", jsonPlugin!!.formatName)
        
        val yamlPlugin = ConfigFormatRegistry.getByExtension("yml")
        assertNotNull(yamlPlugin)
        assertEquals("YAML", yamlPlugin!!.formatName)
        
        val yaml2Plugin = ConfigFormatRegistry.getByExtension("yaml")
        assertNotNull(yaml2Plugin)
        assertEquals("YAML", yaml2Plugin!!.formatName)
    }
    
    @Test
    fun `test available plugins`() {
        val availablePlugins = ConfigFormatRegistry.getAvailablePlugins()
        assertTrue(availablePlugins.isNotEmpty())
        
        // 核心格式应该始终可用
        val coreFormats = availablePlugins.filter { it.isCoreFormat }
        assertTrue(coreFormats.any { it.formatName == "JSON" })
        assertTrue(coreFormats.any { it.formatName == "YAML" })
    }
    
    @Test
    fun `test core plugins`() {
        val corePlugins = ConfigFormatRegistry.getCorePlugins()
        assertEquals(2, corePlugins.size)
        
        val formatNames = corePlugins.map { it.formatName }.toSet()
        assertTrue(formatNames.contains("JSON"))
        assertTrue(formatNames.contains("YAML"))
    }
}
