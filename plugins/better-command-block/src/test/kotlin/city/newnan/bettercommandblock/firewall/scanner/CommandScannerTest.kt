package city.newnan.bettercommandblock.firewall.scanner

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

/**
 * CommandScanner单元测试
 *
 * 测试流式命令扫描器的各种功能，包括：
 * - 基础分词功能
 * - 大小写处理
 * - 空白符处理
 * - Unicode字符处理
 * - 边界情况处理
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class CommandScannerTest {
    
    private lateinit var scanner: CommandScanner
    
    @BeforeEach
    fun setUp() {
        // 每个测试前重置
    }
    
    @Test
    fun `test basic tokenization`() {
        scanner = CommandScanner("say hello world")
        
        assertEquals("say", scanner.nextToken())
        assertEquals("hello", scanner.nextToken())
        assertEquals("world", scanner.nextToken())
        assertNull(scanner.nextToken())
    }
    
    @Test
    fun `test empty command`() {
        scanner = CommandScanner("")
        
        assertNull(scanner.nextToken())
    }
    
    @Test
    fun `test whitespace only command`() {
        scanner = CommandScanner("   \t  \n  ")
        
        assertNull(scanner.nextToken())
    }
    
    @Test
    fun `test single token`() {
        scanner = CommandScanner("say")
        
        assertEquals("say", scanner.nextToken())
        assertNull(scanner.nextToken())
    }
    
    @Test
    fun `test case normalization enabled`() {
        scanner = CommandScanner("SAY Hello WORLD", normalizeCase = true)
        
        assertEquals("say", scanner.nextToken())
        assertEquals("hello", scanner.nextToken())
        assertEquals("world", scanner.nextToken())
    }
    
    @Test
    fun `test case normalization disabled`() {
        scanner = CommandScanner("SAY Hello WORLD", normalizeCase = false)
        
        assertEquals("SAY", scanner.nextToken())
        assertEquals("Hello", scanner.nextToken())
        assertEquals("WORLD", scanner.nextToken())
    }
    
    @Test
    fun `test multiple spaces`() {
        scanner = CommandScanner("say    hello     world")
        
        assertEquals("say", scanner.nextToken())
        assertEquals("hello", scanner.nextToken())
        assertEquals("world", scanner.nextToken())
        assertNull(scanner.nextToken())
    }
    
    @Test
    fun `test tabs and newlines`() {
        scanner = CommandScanner("say\thello\nworld\r\ntest")
        
        assertEquals("say", scanner.nextToken())
        assertEquals("hello", scanner.nextToken())
        assertEquals("world", scanner.nextToken())
        assertEquals("test", scanner.nextToken())
    }
    
    @Test
    fun `test zero width characters`() {
        // 包含零宽空格的命令
        val commandWithZeroWidth = "say\u200Bhello\uFEFFworld"
        scanner = CommandScanner(commandWithZeroWidth)
        
        assertEquals("say", scanner.nextToken())
        assertEquals("hello", scanner.nextToken())
        assertEquals("world", scanner.nextToken())
    }
    
    @Test
    fun `test escape characters`() {
        scanner = CommandScanner("say hello\\ world test\\nvalue")
        
        assertEquals("say", scanner.nextToken())
        assertEquals("hello\\ world", scanner.nextToken())
        assertEquals("test\\nvalue", scanner.nextToken())
    }
    
    @Test
    fun `test remaining method`() {
        scanner = CommandScanner("execute as @s run say hello")
        
        assertEquals("execute", scanner.nextToken())
        assertEquals("as", scanner.nextToken())
        assertEquals("@s", scanner.nextToken())
        assertEquals("run", scanner.nextToken())
        
        assertEquals("say hello", scanner.remaining())
    }
    
    @Test
    fun `test remaining with empty result`() {
        scanner = CommandScanner("say hello")
        
        assertEquals("say", scanner.nextToken())
        assertEquals("hello", scanner.nextToken())
        
        assertEquals("", scanner.remaining())
    }
    
    @Test
    fun `test hasNext method`() {
        scanner = CommandScanner("say hello")

        assertEquals("say", scanner.nextToken())

        assertEquals("hello", scanner.nextToken())

        assertNull(scanner.nextToken())
    }
    
    @Test
    fun `test getCurrentIndex and setIndex`() {
        scanner = CommandScanner("say hello world")
        
        assertEquals(0, scanner.getCurrentIndex())
        
        scanner.nextToken() // "say"
        val indexAfterFirst = scanner.getCurrentIndex()
        assertTrue(indexAfterFirst > 0)
        
        scanner.nextToken() // "hello"
        val indexAfterSecond = scanner.getCurrentIndex()
        assertTrue(indexAfterSecond > indexAfterFirst)
        
        // 重置到第一个token后的位置
        scanner.setIndex(indexAfterFirst)
        assertEquals("hello", scanner.nextToken())
    }
    
    @Test
    fun `test reset method`() {
        scanner = CommandScanner("say hello world")
        
        assertEquals("say", scanner.nextToken())
        assertEquals("hello", scanner.nextToken())
        
        scanner.reset()
        assertEquals(0, scanner.getCurrentIndex())
        assertEquals("say", scanner.nextToken())
    }
    
    @Test
    fun `test createSubScanner`() {
        scanner = CommandScanner("execute as @s run say hello")
        
        // 跳过前面的token
        scanner.nextToken() // "execute"
        scanner.nextToken() // "as"
        scanner.nextToken() // "@s"
        scanner.nextToken() // "run"
        
        val subScanner = scanner.createSubScanner()
        assertEquals("say", subScanner.nextToken())
        assertEquals("hello", subScanner.nextToken())
        assertNull(subScanner.nextToken())
    }
    
    @Test
    fun `test minecraft selectors`() {
        scanner = CommandScanner("execute as @s at @p run tp @a ~ ~ ~")
        
        assertEquals("execute", scanner.nextToken())
        assertEquals("as", scanner.nextToken())
        assertEquals("@s", scanner.nextToken())
        assertEquals("at", scanner.nextToken())
        assertEquals("@p", scanner.nextToken())
        assertEquals("run", scanner.nextToken())
        assertEquals("tp", scanner.nextToken())
        assertEquals("@a", scanner.nextToken())
        assertEquals("~", scanner.nextToken())
        assertEquals("~", scanner.nextToken())
        assertEquals("~", scanner.nextToken())
    }
    
    @Test
    fun `test complex minecraft command`() {
        scanner = CommandScanner("execute as @s[gamemode=creative] at ~ ~1 ~ run give @s minecraft:diamond_sword{Enchantments:[{id:sharpness,lvl:5}]} 1")
        
        val tokens = mutableListOf<String>()
        while (true) {
            val token = scanner.nextToken() ?: break
            tokens.add(token)
        }
        
        assertTrue(tokens.contains("execute"))
        assertTrue(tokens.contains("@s[gamemode=creative]"))
        assertTrue(tokens.contains("minecraft:diamond_sword{enchantments:[{id:sharpness,lvl:5}]}"))
    }
    
    @Test
    fun `test getOriginalCommand`() {
        val originalCommand = "say hello world"
        scanner = CommandScanner(originalCommand)
        
        assertEquals(originalCommand, scanner.getOriginalCommand())
        
        // 扫描不应该改变原始命令
        scanner.nextToken()
        assertEquals(originalCommand, scanner.getOriginalCommand())
    }
    
    @Test
    fun `test toString method`() {
        scanner = CommandScanner("say hello", normalizeCase = true, collapseSpaces = false)
        
        val stringRepresentation = scanner.toString()
        assertTrue(stringRepresentation.contains("CommandScanner"))
        assertTrue(stringRepresentation.contains("say hello"))
        assertTrue(stringRepresentation.contains("normalizeCase=true"))
        assertTrue(stringRepresentation.contains("collapseSpaces=false"))
    }
    
    @Test
    fun `test edge case - only spaces between tokens`() {
        scanner = CommandScanner("say                    hello")
        
        assertEquals("say", scanner.nextToken())
        assertEquals("hello", scanner.nextToken())
        assertNull(scanner.nextToken())
    }
    
    @Test
    fun `test edge case - command ending with spaces`() {
        scanner = CommandScanner("say hello        ")
        
        assertEquals("say", scanner.nextToken())
        assertEquals("hello", scanner.nextToken())
        assertNull(scanner.nextToken())
    }
    
    @Test
    fun `test edge case - command starting with spaces`() {
        scanner = CommandScanner("        say hello")
        
        assertEquals("say", scanner.nextToken())
        assertEquals("hello", scanner.nextToken())
        assertNull(scanner.nextToken())
    }
}
