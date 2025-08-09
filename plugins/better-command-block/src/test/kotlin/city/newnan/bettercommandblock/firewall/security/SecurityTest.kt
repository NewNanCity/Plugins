package city.newnan.bettercommandblock.firewall.security

import city.newnan.bettercommandblock.firewall.scanner.CommandScanner
import city.newnan.bettercommandblock.firewall.trie.CommandTrie
import city.newnan.bettercommandblock.firewall.validators.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

/**
 * 防火墙安全测试
 *
 * 测试各种绕过尝试和安全漏洞，包括：
 * - Unicode同形字攻击
 * - 零宽字符绕过
 * - 大小写绕过
 * - 空白符绕过
 * - 命令注入尝试
 * - 递归深度攻击
 * - 内存耗尽攻击
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class SecurityTest {
    
    private lateinit var commandTrie: CommandTrie
    private lateinit var itemValidator: ItemValidator
    private lateinit var coordinateValidator: CoordinateValidator
    private lateinit var selectorValidator: SelectorValidator
    private lateinit var executeValidator: ExecuteValidator
    
    @BeforeEach
    fun setUp() {
        commandTrie = CommandTrie()
        
        // 设置验证器
        itemValidator = ItemValidator()
        coordinateValidator = CoordinateValidator()
        selectorValidator = SelectorValidator()
        executeValidator = ExecuteValidator(commandTrie)
        
        // 构建测试命令树
        buildSecureCommandTrie()
    }
    
    private fun buildSecureCommandTrie() {
        // 只添加安全命令
        commandTrie.addCommand("say hello")
        commandTrie.addCommand("tell @s test")
        commandTrie.addCommand("give", itemValidator)
        commandTrie.addCommand("tp", coordinateValidator)
        commandTrie.addCommand("execute", executeValidator)
    }
    
    @Test
    fun `test unicode homograph attacks`() {
        // Unicode同形字攻击测试
        val homographAttacks = listOf(
            "ѕay hello", // 使用西里尔字母 's'
            "sаy hello", // 使用西里尔字母 'a'
            "say һello", // 使用西里尔字母 'h'
            "ｓay hello", // 使用全角字符
            "say ｈello", // 混合全角字符
            "ѕау hello", // 完全使用西里尔字母
            "𝐬ay hello", // 使用数学字母
            "𝓈ay hello", // 使用数学脚本字母
        )
        
        for (attack in homographAttacks) {
            assertFalse(
                commandTrie.isCommandSafe(attack),
                "Unicode homograph attack should be blocked: $attack"
            )
        }
    }
    
    @Test
    fun `test zero width character bypass`() {
        // 零宽字符绕过测试
        val zeroWidthAttacks = listOf(
            "say\u200B hello", // 零宽空格
            "s\uFEFFay hello", // 字节顺序标记
            "sa\u200Cy hello", // 零宽非连接符
            "say\u200D hello", // 零宽连接符
            "say\u2060 hello", // 词连接符
            "say\u180E hello", // 蒙古文元音分隔符
            "s\u200Ba\u200Cy\u200D hello", // 多个零宽字符
        )
        
        for (attack in zeroWidthAttacks) {
            // 零宽字符应该被过滤，命令应该被正确识别
            assertTrue(
                commandTrie.isCommandSafe(attack),
                "Zero width characters should be filtered: $attack"
            )
        }
    }
    
    @Test
    fun `test case bypass attempts`() {
        // 大小写绕过测试
        val caseBypassAttempts = listOf(
            "SAY hello",
            "Say Hello",
            "sAy HeLLo",
            "GIVE @s dirt",
            "Give @S Dirt",
            "TP 0 64 0",
            "Tp 0 64 0",
            "EXECUTE as @s run say hello",
            "Execute As @S Run Say Hello"
        )
        
        for (attempt in caseBypassAttempts) {
            // 大小写应该被规范化，安全命令应该被允许
            assertTrue(
                commandTrie.isCommandSafe(attempt),
                "Case normalization should work: $attempt"
            )
        }
        
        // 测试危险命令的大小写绕过
        val dangerousCaseAttempts = listOf(
            "DANGEROUS_COMMAND",
            "Dangerous_Command",
            "dAnGeRoUs_CoMmAnD"
        )
        
        for (attempt in dangerousCaseAttempts) {
            assertFalse(
                commandTrie.isCommandSafe(attempt),
                "Dangerous commands should be blocked regardless of case: $attempt"
            )
        }
    }
    
    @Test
    fun `test whitespace bypass attempts`() {
        // 空白符绕过测试
        val whitespaceBypassAttempts = listOf(
            "say    hello", // 多个空格
            "say\thello", // 制表符
            "say\nhello", // 换行符
            "say\rhello", // 回车符
            "say\u00A0hello", // 不间断空格
            "say\u2000hello", // En quad
            "say\u2001hello", // Em quad
            "say\u2002hello", // En space
            "say\u2003hello", // Em space
            "   say   hello   ", // 前后空格
            "\t\tsay\t\thello\t\t", // 前后制表符
        )
        
        for (attempt in whitespaceBypassAttempts) {
            assertTrue(
                commandTrie.isCommandSafe(attempt),
                "Whitespace normalization should work: $attempt"
            )
        }
    }
    
    @Test
    fun `test command injection attempts`() {
        // 命令注入尝试测试
        val injectionAttempts = listOf(
            "say hello; dangerous_command",
            "say hello && dangerous_command",
            "say hello || dangerous_command",
            "say hello | dangerous_command",
            "say hello & dangerous_command",
            "say hello\ndangerous_command",
            "say hello\rdangerous_command",
            "say hello$(dangerous_command)",
            "say hello`dangerous_command`",
            "say hello{dangerous_command}",
            "say hello[dangerous_command]",
        )
        
        for (attempt in injectionAttempts) {
            // 命令注入应该被阻止（因为不匹配安全命令模式）
            assertFalse(
                commandTrie.isCommandSafe(attempt),
                "Command injection should be blocked: $attempt"
            )
        }
    }
    
    @Test
    fun `test item validator security`() {
        // 物品验证器安全测试
        val dangerousItemAttempts = listOf(
            "minecraft:command_block 1",
            "command_block 1",
            "minecraft:structure_block 1",
            "structure_block 1",
            "minecraft:barrier 1",
            "barrier 1",
            "minecraft:bedrock 1",
            "bedrock 1",
            "minecraft:spawner 1",
            "spawner 1",
            "minecraft:debug_stick 1",
            "debug_stick 1"
        )
        
        for (attempt in dangerousItemAttempts) {
            val scanner = CommandScanner(attempt)
            assertFalse(
                itemValidator.validate(scanner),
                "Dangerous item should be blocked: $attempt"
            )
        }
        
        // NBT注入测试
        val nbtInjectionAttempts = listOf(
            "minecraft:dirt{Command:\"dangerous_command\"} 1",
            "minecraft:stone{command:\"give @a diamond\"} 1",
            "minecraft:bread{CustomName:\"§cDangerous\"} 1",
            "minecraft:apple{Enchantments:[{id:sharpness,lvl:32767}]} 1",
            "minecraft:dirt{give:\"test\"} 1",
            "minecraft:stone{summon:\"test\"} 1",
            "minecraft:bread{execute:\"test\"} 1"
        )
        
        for (attempt in nbtInjectionAttempts) {
            val scanner = CommandScanner(attempt)
            assertFalse(
                itemValidator.validate(scanner),
                "NBT injection should be blocked: $attempt"
            )
        }
        
        // 数量溢出测试
        val quantityOverflowAttempts = listOf(
            "minecraft:dirt 65",
            "minecraft:dirt 100",
            "minecraft:dirt 2147483647", // Integer.MAX_VALUE
            "minecraft:dirt -1",
            "minecraft:dirt 0"
        )
        
        for (attempt in quantityOverflowAttempts) {
            val scanner = CommandScanner(attempt)
            assertFalse(
                itemValidator.validate(scanner),
                "Quantity overflow should be blocked: $attempt"
            )
        }
    }
    
    @Test
    fun `test coordinate validator security`() {
        // 坐标验证器安全测试
        val coordinateOverflowAttempts = listOf(
            "1001 0 0", // 超出范围
            "0 0 -1001", // 超出范围
            "2147483647 0 0", // Integer.MAX_VALUE
            "0 2147483647 0",
            "0 0 -2147483648", // Integer.MIN_VALUE
            "30000001 0 0", // 超出世界边界
            "0 0 -30000001"
        )
        
        for (attempt in coordinateOverflowAttempts) {
            val scanner = CommandScanner(attempt)
            assertFalse(
                coordinateValidator.validate(scanner),
                "Coordinate overflow should be blocked: $attempt"
            )
        }
        
        // 无效坐标格式测试
        val invalidCoordinateAttempts = listOf(
            "invalid 0 0",
            "0 invalid 0",
            "0 0 invalid",
            "NaN 0 0",
            "Infinity 0 0",
            "-Infinity 0 0",
            "1e999 0 0", // 科学计数法溢出
            "0x1000 0 0" // 十六进制
        )
        
        for (attempt in invalidCoordinateAttempts) {
            val scanner = CommandScanner(attempt)
            assertFalse(
                coordinateValidator.validate(scanner),
                "Invalid coordinate format should be blocked: $attempt"
            )
        }
    }
    
    @Test
    fun `test selector validator security`() {
        // 选择器验证器安全测试
        val dangerousSelectorAttempts = listOf(
            "@a", // 所有玩家
            "@e", // 所有实体
            "@r", // 随机玩家
            "@p", // 最近玩家（默认不允许）
            "@s[type=!player]", // 类型过滤
            "@s[name=admin]", // 名称过滤
            "@s[tag=op]", // 标签过滤
            "@s[team=admin]", // 团队过滤
            "@s[nbt={test:1}]", // NBT过滤
            "@s[scores={test=1}]", // 分数过滤
            "@s[advancements={test=true}]" // 进度过滤
        )
        
        for (attempt in dangerousSelectorAttempts) {
            val scanner = CommandScanner(attempt)
            assertFalse(
                selectorValidator.validate(scanner),
                "Dangerous selector should be blocked: $attempt"
            )
        }
        
        // 范围溢出测试
        val rangeOverflowAttempts = listOf(
            "@s[distance=..101]", // 超出最大范围
            "@s[distance=200..]",
            "@s[x=1001]", // 坐标超出范围
            "@s[y=-1001]",
            "@s[dx=1001]", // 范围超出限制
            "@s[limit=2]" // 超出目标数量限制
        )
        
        for (attempt in rangeOverflowAttempts) {
            val scanner = CommandScanner(attempt)
            assertFalse(
                selectorValidator.validate(scanner),
                "Range overflow should be blocked: $attempt"
            )
        }
    }
    
    @Test
    fun `test execute validator security`() {
        // Execute验证器安全测试
        val dangerousExecuteAttempts = listOf(
            "as @a run dangerous_command", // 危险选择器
            "as @s run dangerous_command", // 危险子命令
            "positioned 2000 0 0 run say hello", // 坐标超出范围
            "if block ~ ~ ~ air run say hello", // 危险子命令
            "unless entity @a run say hello", // 危险子命令
            "store result score test objective run say hello" // 危险子命令
        )
        
        for (attempt in dangerousExecuteAttempts) {
            val scanner = CommandScanner(attempt)
            assertFalse(
                executeValidator.validate(scanner),
                "Dangerous execute command should be blocked: $attempt"
            )
        }
    }
    
    @Test
    fun `test recursion depth attacks`() {
        // 递归深度攻击测试
        val recursiveTrie = CommandTrie()
        recursiveTrie.addCommand("execute", ExecuteValidator(recursiveTrie, maxDepth = 3))
        
        val deepRecursionAttempts = listOf(
            "run execute as @s run execute as @s run execute as @s run execute as @s run say hello",
            "as @s run execute as @s run execute as @s run execute as @s run say hello"
        )
        
        for (attempt in deepRecursionAttempts) {
            assertFalse(
                recursiveTrie.isCommandSafe("execute $attempt"),
                "Deep recursion should be blocked: $attempt"
            )
        }
    }
    
    @Test
    fun `test memory exhaustion attacks`() {
        // 内存耗尽攻击测试
        val largeTrie = CommandTrie()
        
        // 尝试添加大量命令
        val startTime = System.currentTimeMillis()
        val maxTime = 5000 // 5秒超时
        
        var commandCount = 0
        while (System.currentTimeMillis() - startTime < maxTime && commandCount < 100000) {
            largeTrie.addCommand("command$commandCount arg1 arg2 arg3")
            commandCount++
        }
        
        println("Added $commandCount commands in ${System.currentTimeMillis() - startTime}ms")
        
        // 验证系统仍然响应
        assertTrue(largeTrie.isCommandSafe("command0 arg1 arg2 arg3"))
        assertFalse(largeTrie.isCommandSafe("dangerous_command"))
        
        // 验证内存使用合理
        val runtime = Runtime.getRuntime()
        runtime.gc()
        val memoryUsed = runtime.totalMemory() - runtime.freeMemory()
        val memoryPerCommand = memoryUsed.toDouble() / commandCount
        
        println("Memory used: ${memoryUsed / 1024 / 1024}MB")
        println("Memory per command: ${memoryPerCommand}bytes")
        
        // 内存使用应该合理
        assertTrue(memoryPerCommand < 10000, "Memory usage per command should be reasonable")
    }
    
    @Test
    fun `test edge case inputs`() {
        // 边界情况输入测试
        val edgeCaseInputs = listOf(
            "", // 空字符串
            "   ", // 只有空格
            "\t\n\r", // 只有空白符
            "\u0000", // 空字符
            "\uFFFF", // 最大Unicode字符
            "a".repeat(10000), // 超长字符串
            "say " + "hello ".repeat(1000), // 超长命令
            "say\u0001\u0002\u0003", // 控制字符
            "say\u007F\u0080\u0081" // 边界字符
        )
        
        for (input in edgeCaseInputs) {
            // 边界情况不应该导致异常
            assertDoesNotThrow {
                commandTrie.isCommandSafe(input)
            }
        }
    }
}
