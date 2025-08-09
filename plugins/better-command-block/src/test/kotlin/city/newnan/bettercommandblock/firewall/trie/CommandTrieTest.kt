package city.newnan.bettercommandblock.firewall.trie

import city.newnan.bettercommandblock.firewall.validators.Validator
import city.newnan.bettercommandblock.firewall.scanner.CommandScanner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

/**
 * CommandTrie单元测试
 *
 * 测试命令前缀树管理器的各种功能，包括：
 * - 命令添加和移除
 * - 命令匹配和验证
 * - 统计信息
 * - 线程安全性
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class CommandTrieTest {

    private lateinit var trie: CommandTrie
    private lateinit var mockValidator: Validator

    @BeforeEach
    fun setUp() {
        trie = CommandTrie()
        mockValidator = object : Validator {
            override fun validate(scanner: CommandScanner): Boolean = true
            override fun getName(): String = "MockValidator"
        }
    }

    @Test
    fun `test initial state`() {
        assertTrue(trie.isEmpty())
        assertEquals(1, trie.size()) // 只有根节点
        assertTrue(trie.getAllCommands().isEmpty())

        val stats = trie.getStatistics()
        assertEquals(0L, stats["totalValidations"])
        assertEquals(0L, stats["totalMatches"])
        assertEquals(0L, stats["totalRejections"])
    }

    @Test
    fun `test addCommand with token list`() {
        val tokens = listOf("say", "hello")
        trie.addCommand(tokens)

        assertFalse(trie.isEmpty())
        assertTrue(trie.size() > 1)

        val commands = trie.getAllCommands()
        assertEquals(1, commands.size)
        assertEquals(tokens, commands[0])
    }

    @Test
    fun `test addCommand with string`() {
        trie.addCommand("say hello world")

        assertFalse(trie.isEmpty())

        val commands = trie.getAllCommands()
        assertEquals(1, commands.size)
        assertEquals(listOf("say", "hello", "world"), commands[0])
    }

    @Test
    fun `test addCommand with validator`() {
        trie.addCommand("execute", mockValidator)

        assertTrue(trie.isCommandSafe("execute"))

        val commands = trie.getAllCommands()
        assertEquals(1, commands.size)
        assertEquals(listOf("execute"), commands[0])
    }

    @Test
    fun `test addCommand with metadata`() {
        val metadata = mapOf("description" to "Say command", "permission" to "minecraft.command.say")
        trie.addCommand("say", metadata = metadata)

        assertTrue(trie.isCommandSafe("say"))
    }

    @Test
    fun `test multiple commands`() {
        trie.addCommand("say hello")
        trie.addCommand("give @s dirt")
        trie.addCommand("tp ~ ~ ~")

        assertEquals(3, trie.getAllCommands().size)

        assertTrue(trie.isCommandSafe("say hello"))
        assertTrue(trie.isCommandSafe("give @s dirt"))
        assertTrue(trie.isCommandSafe("tp ~ ~ ~"))

        assertFalse(trie.isCommandSafe("say goodbye"))
        assertFalse(trie.isCommandSafe("give @a diamond"))
    }

    @Test
    fun `test overlapping commands`() {
        trie.addCommand("say hello")
        trie.addCommand("say hello world")
        trie.addCommand("say goodbye")

        assertEquals(3, trie.getAllCommands().size)

        assertTrue(trie.isCommandSafe("say hello"))
        assertTrue(trie.isCommandSafe("say hello world"))
        assertTrue(trie.isCommandSafe("say goodbye"))

        assertFalse(trie.isCommandSafe("say"))
        assertFalse(trie.isCommandSafe("say hello there"))
    }

    @Test
    fun `test removeCommand with token list`() {
        val tokens = listOf("say", "hello")
        trie.addCommand(tokens)

        assertTrue(trie.isCommandSafe("say hello"))

        assertTrue(trie.removeCommand(tokens))
        assertFalse(trie.isCommandSafe("say hello"))

        // 尝试移除不存在的命令
        assertFalse(trie.removeCommand(tokens))
    }

    @Test
    fun `test removeCommand with string`() {
        trie.addCommand("say hello world")

        assertTrue(trie.isCommandSafe("say hello world"))

        assertTrue(trie.removeCommand("say hello world"))
        assertFalse(trie.isCommandSafe("say hello world"))

        // 尝试移除不存在的命令
        assertFalse(trie.removeCommand("say hello world"))
    }

    @Test
    fun `test removeCommand with overlapping paths`() {
        trie.addCommand("say hello")
        trie.addCommand("say hello world")

        assertTrue(trie.isCommandSafe("say hello"))
        assertTrue(trie.isCommandSafe("say hello world"))

        // 移除较长的命令
        assertTrue(trie.removeCommand("say hello world"))
        assertTrue(trie.isCommandSafe("say hello"))
        assertFalse(trie.isCommandSafe("say hello world"))

        // 移除较短的命令
        assertTrue(trie.removeCommand("say hello"))
        assertFalse(trie.isCommandSafe("say hello"))
    }

    @Test
    fun `test isCommandSafe basic matching`() {
        trie.addCommand("say hello")
        trie.addCommand("give @s dirt")

        assertTrue(trie.isCommandSafe("say hello"))
        assertTrue(trie.isCommandSafe("give @s dirt"))

        assertFalse(trie.isCommandSafe("say goodbye"))
        assertFalse(trie.isCommandSafe("give @a diamond"))
        assertFalse(trie.isCommandSafe("tp ~ ~ ~"))
    }

    @Test
    fun `test isCommandSafe with case sensitivity`() {
        trie.addCommand("say hello")

        // CommandScanner默认启用大小写规范化
        assertTrue(trie.isCommandSafe("SAY HELLO"))
        assertTrue(trie.isCommandSafe("Say Hello"))
        assertTrue(trie.isCommandSafe("sAy HeLLo"))
    }

    @Test
    fun `test isCommandSafe with extra whitespace`() {
        trie.addCommand("say hello")

        // CommandScanner默认启用空白符压缩
        assertTrue(trie.isCommandSafe("say    hello"))
        assertTrue(trie.isCommandSafe("  say hello  "))
        assertTrue(trie.isCommandSafe("say\thello"))
        assertTrue(trie.isCommandSafe("say\nhello"))
    }

    @Test
    fun `test isCommandSafe with validator`() {
        val alwaysAcceptValidator = object : Validator {
            override fun validate(scanner: CommandScanner): Boolean = true
            override fun getName(): String = "AlwaysAccept"
        }

        val alwaysRejectValidator = object : Validator {
            override fun validate(scanner: CommandScanner): Boolean = false
            override fun getName(): String = "AlwaysReject"
        }

        trie.addCommand("execute", alwaysAcceptValidator)
        trie.addCommand("dangerous", alwaysRejectValidator)

        assertTrue(trie.isCommandSafe("execute"))
        assertFalse(trie.isCommandSafe("dangerous"))
    }

    @Test
    fun `test clear`() {
        trie.addCommand("say hello")
        trie.addCommand("give @s dirt")
        trie.addCommand("tp ~ ~ ~")

        assertFalse(trie.isEmpty())
        assertEquals(3, trie.getAllCommands().size)

        trie.clear()

        assertTrue(trie.isEmpty())
        assertEquals(0, trie.getAllCommands().size)
        assertEquals(1, trie.size()) // 只剩根节点

        // 验证统计信息也被重置
        val stats = trie.getStatistics()
        assertEquals(0L, stats["totalValidations"])
        assertEquals(0L, stats["totalMatches"])
        assertEquals(0L, stats["totalRejections"])
    }

    @Test
    fun `test size calculation`() {
        assertEquals(1, trie.size()) // 根节点

        trie.addCommand("say")
        assertEquals(2, trie.size()) // 根节点 + say节点

        trie.addCommand("say hello")
        assertEquals(3, trie.size()) // 根节点 + say节点 + hello节点

        trie.addCommand("give")
        assertEquals(4, trie.size()) // 根节点 + say节点 + hello节点 + give节点
    }

    @Test
    fun `test getAllCommands`() {
        assertTrue(trie.getAllCommands().isEmpty())

        trie.addCommand("say hello")
        trie.addCommand("give @s dirt")
        trie.addCommand("tp ~ ~ ~")

        val commands = trie.getAllCommands()
        assertEquals(3, commands.size)

        assertTrue(commands.contains(listOf("say", "hello")))
        assertTrue(commands.contains(listOf("give", "@s", "dirt")))
        assertTrue(commands.contains(listOf("tp", "~", "~", "~")))
    }

    @Test
    fun `test statistics tracking`() {
        trie.addCommand("say hello")
        trie.addCommand("give @s dirt")

        // 初始统计
        var stats = trie.getStatistics()
        assertEquals(0L, stats["totalValidations"])
        assertEquals(0L, stats["totalMatches"])
        assertEquals(0L, stats["totalRejections"])

        // 执行一些验证
        assertTrue(trie.isCommandSafe("say hello"))  // 匹配
        assertFalse(trie.isCommandSafe("say goodbye")) // 拒绝
        assertTrue(trie.isCommandSafe("give @s dirt")) // 匹配

        stats = trie.getStatistics()
        assertEquals(3L, stats["totalValidations"])
        assertEquals(2L, stats["totalMatches"])
        assertEquals(1L, stats["totalRejections"])
    }

    @Test
    fun `test resetStatistics`() {
        trie.addCommand("say hello")

        // 执行一些验证
        trie.isCommandSafe("say hello")
        trie.isCommandSafe("say goodbye")

        var stats = trie.getStatistics()
        assertTrue(stats["totalValidations"]!! > 0)

        // 重置统计
        trie.resetStatistics()

        stats = trie.getStatistics()
        assertEquals(0L, stats["totalValidations"])
        assertEquals(0L, stats["totalMatches"])
        assertEquals(0L, stats["totalRejections"])
    }

    @Test
    fun `test empty command handling`() {
        assertFalse(trie.isCommandSafe(""))
        assertFalse(trie.isCommandSafe("   "))
        assertFalse(trie.isCommandSafe("\t\n"))
    }

    @Test
    fun `test partial command matching`() {
        trie.addCommand("say hello world")

        // 部分命令不应该匹配
        assertFalse(trie.isCommandSafe("say"))
        assertFalse(trie.isCommandSafe("say hello"))

        // 完整命令应该匹配
        assertTrue(trie.isCommandSafe("say hello world"))

        // 超出的命令不应该匹配
        assertFalse(trie.isCommandSafe("say hello world extra"))
    }

    @Test
    fun `test toString`() {
        trie.addCommand("say hello")
        trie.isCommandSafe("say hello")
        trie.isCommandSafe("say goodbye")

        val stringRepresentation = trie.toString()
        assertTrue(stringRepresentation.contains("CommandTrie"))
        assertTrue(stringRepresentation.contains("size="))
        assertTrue(stringRepresentation.contains("validations="))
        assertTrue(stringRepresentation.contains("matches="))
        assertTrue(stringRepresentation.contains("rejections="))
    }

    @Test
    fun `test concurrent access`() {
        // 基础的并发测试
        val threads = mutableListOf<Thread>()

        // 添加命令的线程
        repeat(5) { i ->
            threads.add(Thread {
                trie.addCommand("command$i test")
            })
        }

        // 验证命令的线程
        repeat(10) { i ->
            threads.add(Thread {
                trie.isCommandSafe("command${i % 5} test")
            })
        }

        // 启动所有线程
        threads.forEach { it.start() }

        // 等待所有线程完成
        threads.forEach { it.join() }

        // 验证结果
        val stats = trie.getStatistics()
        assertTrue(stats["totalValidations"]!! > 0)
    }
}
