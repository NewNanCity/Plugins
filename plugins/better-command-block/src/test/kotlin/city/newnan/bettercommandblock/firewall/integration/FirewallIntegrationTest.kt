package city.newnan.bettercommandblock.firewall.integration

import city.newnan.bettercommandblock.firewall.CommandBlockFirewallModule
import city.newnan.bettercommandblock.firewall.config.FirewallConfig
import city.newnan.bettercommandblock.firewall.trie.CommandTrie
import city.newnan.bettercommandblock.firewall.validators.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito.*

/**
 * 防火墙集成测试
 *
 * 测试防火墙模块的完整集成功能，包括：
 * - 模块初始化
 * - 配置加载
 * - 命令验证流程
 * - 统计信息收集
 * - 错误处理
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class FirewallIntegrationTest {
    
    private lateinit var mockPlugin: city.newnan.bettercommandblock.BetterCommandBlockPlugin
    private lateinit var firewallModule: CommandBlockFirewallModule
    private lateinit var testConfig: FirewallConfig
    
    @BeforeEach
    fun setUp() {
        // 创建模拟的插件实例
        mockPlugin = mock(city.newnan.bettercommandblock.BetterCommandBlockPlugin::class.java)
        
        // 创建测试配置
        testConfig = FirewallConfig(
            enabled = true,
            whitelistCommands = setOf(
                "say", "tell", "give", "tp", "execute"
            ),
            safeItems = setOf(
                "minecraft:dirt", "dirt", "minecraft:stone", "stone"
            ),
            maxItemQuantity = 64,
            allowCustomNamespaces = false,
            maxCoordinateRange = 1000.0,
            allowRelativeCoordinates = true,
            allowLocalCoordinates = true,
            allowedSelectors = setOf("@s"),
            maxSelectorRange = 100.0,
            allowPlayerNames = true,
            maxTargetCount = 1,
            maxExecuteDepth = 10,
            destroyBlockedCommandBlocks = true
        )
        
        // 模拟插件配置
        val mockConfig = mock(city.newnan.bettercommandblock.config.BetterCommandBlockConfig::class.java)
        `when`(mockConfig.firewall).thenReturn(testConfig)
        `when`(mockPlugin.getPluginConfig()).thenReturn(mockConfig)
        
        // 模拟数据文件夹
        val mockDataFolder = mock(java.io.File::class.java)
        `when`(mockDataFolder.exists()).thenReturn(true)
        `when`(mockPlugin.dataFolder).thenReturn(mockDataFolder)
        
        // 注意：由于CommandBlockFirewallModule依赖BaseModule，
        // 在实际测试中可能需要更复杂的模拟设置
        // 这里我们主要测试核心逻辑
    }
    
    @Test
    fun `test firewall initialization`() {
        // 测试防火墙初始化过程
        // 注意：这个测试可能需要调整，因为实际的初始化涉及BaseModule
        
        val commandTrie = CommandTrie()
        
        // 测试命令树构建
        for (command in testConfig.whitelistCommands) {
            commandTrie.addCommand(command)
        }
        
        assertFalse(commandTrie.isEmpty())
        assertEquals(testConfig.whitelistCommands.size, commandTrie.getAllCommands().size)
    }
    
    @Test
    fun `test command validation workflow`() {
        val commandTrie = CommandTrie()
        
        // 构建测试命令树
        commandTrie.addCommand("say hello")
        commandTrie.addCommand("give", ItemValidator(testConfig.safeItems, testConfig.maxItemQuantity))
        commandTrie.addCommand("tp", CoordinateValidator(testConfig.maxCoordinateRange))
        
        // 测试安全命令
        assertTrue(commandTrie.isCommandSafe("say hello"))
        
        // 测试不安全命令
        assertFalse(commandTrie.isCommandSafe("dangerous_command"))
        
        // 测试带验证器的命令
        assertTrue(commandTrie.isCommandSafe("give minecraft:dirt 1"))
        assertFalse(commandTrie.isCommandSafe("give minecraft:command_block 1"))
        
        assertTrue(commandTrie.isCommandSafe("tp 0 64 0"))
        assertFalse(commandTrie.isCommandSafe("tp 2000 64 0"))
    }
    
    @Test
    fun `test validator integration`() {
        // 测试验证器集成
        val itemValidator = ItemValidator(
            safeItems = setOf("minecraft:dirt", "dirt"),
            maxQuantity = 64,
            allowCustomNamespaces = false
        )
        
        val coordinateValidator = CoordinateValidator(
            maxRange = 1000.0,
            allowRelative = true,
            allowLocal = true
        )
        
        val selectorValidator = SelectorValidator(
            allowedSelectors = setOf("@s"),
            maxRange = 100.0,
            allowPlayerNames = true,
            maxTargetCount = 1
        )
        
        // 测试验证器功能
        assertTrue(itemValidator.validate(city.newnan.bettercommandblock.firewall.scanner.CommandScanner("minecraft:dirt 32")))
        assertFalse(itemValidator.validate(city.newnan.bettercommandblock.firewall.scanner.CommandScanner("minecraft:command_block 1")))
        
        assertTrue(coordinateValidator.validate(city.newnan.bettercommandblock.firewall.scanner.CommandScanner("0 64 0")))
        assertFalse(coordinateValidator.validate(city.newnan.bettercommandblock.firewall.scanner.CommandScanner("2000 64 0")))
        
        assertTrue(selectorValidator.validate(city.newnan.bettercommandblock.firewall.scanner.CommandScanner("@s")))
        assertFalse(selectorValidator.validate(city.newnan.bettercommandblock.firewall.scanner.CommandScanner("@a")))
    }
    
    @Test
    fun `test execute validator integration`() {
        val commandTrie = CommandTrie()
        commandTrie.addCommand("say hello")
        commandTrie.addCommand("give @s dirt")
        
        val executeValidator = ExecuteValidator(
            rootTrie = commandTrie,
            maxDepth = 3,
            selectorValidator = SelectorValidator.createStrict(),
            coordinateValidator = CoordinateValidator()
        )
        
        // 测试简单的execute命令
        assertTrue(executeValidator.validate(city.newnan.bettercommandblock.firewall.scanner.CommandScanner("as @s run say hello")))
        
        // 测试不安全的execute命令
        assertFalse(executeValidator.validate(city.newnan.bettercommandblock.firewall.scanner.CommandScanner("as @a run say hello")))
        
        // 测试复杂的execute命令
        assertTrue(executeValidator.validate(city.newnan.bettercommandblock.firewall.scanner.CommandScanner("as @s positioned 0 64 0 run say hello")))
        assertFalse(executeValidator.validate(city.newnan.bettercommandblock.firewall.scanner.CommandScanner("as @s positioned 2000 64 0 run say hello")))
    }
    
    @Test
    fun `test statistics collection`() {
        val commandTrie = CommandTrie()
        commandTrie.addCommand("say hello")
        commandTrie.addCommand("tell @s test")
        
        // 执行一些验证
        assertTrue(commandTrie.isCommandSafe("say hello"))
        assertFalse(commandTrie.isCommandSafe("dangerous_command"))
        assertTrue(commandTrie.isCommandSafe("tell @s test"))
        assertFalse(commandTrie.isCommandSafe("another_dangerous_command"))
        
        // 检查统计信息
        val stats = commandTrie.getStatistics()
        assertEquals(4L, stats["totalValidations"])
        assertEquals(2L, stats["totalMatches"])
        assertEquals(2L, stats["totalRejections"])
    }
    
    @Test
    fun `test configuration validation`() {
        // 测试配置验证
        assertTrue(testConfig.enabled)
        assertTrue(testConfig.whitelistCommands.contains("say"))
        assertTrue(testConfig.safeItems.contains("minecraft:dirt"))
        assertEquals(64, testConfig.maxItemQuantity)
        assertEquals(1000.0, testConfig.maxCoordinateRange)
        assertTrue(testConfig.allowRelativeCoordinates)
        assertTrue(testConfig.allowedSelectors.contains("@s"))
        assertEquals(10, testConfig.maxExecuteDepth)
    }
    
    @Test
    fun `test error handling`() {
        val commandTrie = CommandTrie()
        
        // 测试空命令处理
        assertFalse(commandTrie.isCommandSafe(""))
        assertFalse(commandTrie.isCommandSafe("   "))
        
        // 测试无效命令处理
        assertFalse(commandTrie.isCommandSafe("invalid_command_that_does_not_exist"))
        
        // 测试验证器异常处理
        val faultyValidator = object : Validator {
            override fun validate(scanner: city.newnan.bettercommandblock.firewall.scanner.CommandScanner): Boolean {
                throw RuntimeException("Test exception")
            }
            override fun getName(): String = "FaultyValidator"
        }
        
        commandTrie.addCommand("faulty", faultyValidator)
        
        // 验证器异常应该被捕获，命令应该被拒绝
        assertFalse(commandTrie.isCommandSafe("faulty test"))
    }
    
    @Test
    fun `test concurrent access`() {
        val commandTrie = CommandTrie()
        commandTrie.addCommand("say hello")
        commandTrie.addCommand("tell @s test")
        
        val threads = mutableListOf<Thread>()
        val results = mutableListOf<Boolean>()
        
        // 创建多个线程同时访问命令树
        repeat(10) { i ->
            threads.add(Thread {
                val command = if (i % 2 == 0) "say hello" else "dangerous_command"
                val result = commandTrie.isCommandSafe(command)
                synchronized(results) {
                    results.add(result)
                }
            })
        }
        
        // 启动所有线程
        threads.forEach { it.start() }
        
        // 等待所有线程完成
        threads.forEach { it.join() }
        
        // 验证结果
        assertEquals(10, results.size)
        assertEquals(5, results.count { it }) // 5个安全命令
        assertEquals(5, results.count { !it }) // 5个危险命令
    }
    
    @Test
    fun `test memory usage`() {
        val commandTrie = CommandTrie()
        
        // 添加大量命令测试内存使用
        repeat(1000) { i ->
            commandTrie.addCommand("command$i test")
        }
        
        // 验证命令树大小
        assertTrue(commandTrie.size() > 1000) // 应该有很多节点
        assertEquals(1000, commandTrie.getAllCommands().size)
        
        // 测试清理功能
        commandTrie.clear()
        assertTrue(commandTrie.isEmpty())
        assertEquals(1, commandTrie.size()) // 只剩根节点
    }
}
