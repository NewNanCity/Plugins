package city.newnan.bettercommandblock.firewall.validators

import city.newnan.bettercommandblock.firewall.scanner.CommandScanner
import city.newnan.bettercommandblock.firewall.trie.CommandTrie
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

/**
 * ExecuteValidator单元测试
 *
 * 测试execute命令验证器的各种功能，包括：
 * - 基础execute命令验证
 * - 子命令递归验证
 * - 参数验证
 * - 深度限制
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class ExecuteValidatorTest {
    
    private lateinit var rootTrie: CommandTrie
    private lateinit var validator: ExecuteValidator
    private lateinit var strictValidator: ExecuteValidator
    private lateinit var permissiveValidator: ExecuteValidator
    
    @BeforeEach
    fun setUp() {
        // 设置根命令树
        rootTrie = CommandTrie()
        rootTrie.addCommand("say hello")
        rootTrie.addCommand("give @s dirt")
        rootTrie.addCommand("tp ~ ~ ~")
        
        validator = ExecuteValidator(rootTrie)
        strictValidator = ExecuteValidator.createStrict(rootTrie)
        permissiveValidator = ExecuteValidator.createPermissive(rootTrie)
    }
    
    @Test
    fun `test basic execute run command`() {
        // 基础的execute run命令
        assertTrue(validator.validate(CommandScanner("run say hello")))
        assertTrue(validator.validate(CommandScanner("run give @s dirt")))
        assertTrue(validator.validate(CommandScanner("run tp ~ ~ ~")))
        
        // 不安全的子命令
        assertFalse(validator.validate(CommandScanner("run dangerous_command")))
        assertFalse(validator.validate(CommandScanner("run give @a diamond")))
    }
    
    @Test
    fun `test execute as subcommand`() {
        // 有效的as子命令
        assertTrue(validator.validate(CommandScanner("as @s run say hello")))
        
        // 无效的目标选择器（默认只允许@s）
        assertFalse(validator.validate(CommandScanner("as @a run say hello")))
        assertFalse(validator.validate(CommandScanner("as @e run say hello")))
        
        // 缺少run关键字
        assertFalse(validator.validate(CommandScanner("as @s say hello")))
    }
    
    @Test
    fun `test execute at subcommand`() {
        // 有效的at子命令
        assertTrue(validator.validate(CommandScanner("at @s run say hello")))
        
        // 无效的目标选择器
        assertFalse(validator.validate(CommandScanner("at @a run say hello")))
        assertFalse(validator.validate(CommandScanner("at @e run say hello")))
    }
    
    @Test
    fun `test execute positioned subcommand`() {
        // 有效的positioned子命令
        assertTrue(validator.validate(CommandScanner("positioned 0 64 0 run say hello")))
        assertTrue(validator.validate(CommandScanner("positioned ~ ~ ~ run say hello")))
        assertTrue(validator.validate(CommandScanner("positioned ^ ^ ^ run say hello")))
        
        // 超出范围的坐标
        assertFalse(validator.validate(CommandScanner("positioned 1001 0 0 run say hello")))
        assertFalse(validator.validate(CommandScanner("positioned 0 0 -1001 run say hello")))
        
        // 无效的坐标格式
        assertFalse(validator.validate(CommandScanner("positioned invalid 0 0 run say hello")))
        
        // 坐标数量不足
        assertFalse(validator.validate(CommandScanner("positioned 0 0 run say hello")))
    }
    
    @Test
    fun `test execute facing subcommand`() {
        // facing坐标
        assertTrue(validator.validate(CommandScanner("facing 0 64 0 run say hello")))
        assertTrue(validator.validate(CommandScanner("facing ~ ~ ~ run say hello")))
        
        // facing entity
        assertTrue(validator.validate(CommandScanner("facing entity @s run say hello")))
        assertTrue(validator.validate(CommandScanner("facing entity @s eyes run say hello")))
        assertTrue(validator.validate(CommandScanner("facing entity @s feet run say hello")))
        
        // 无效的facing entity目标
        assertFalse(validator.validate(CommandScanner("facing entity @a run say hello")))
        
        // 无效的anchor
        assertFalse(validator.validate(CommandScanner("facing entity @s invalid run say hello")))
        
        // 超出范围的坐标
        assertFalse(validator.validate(CommandScanner("facing 1001 0 0 run say hello")))
    }
    
    @Test
    fun `test execute rotated subcommand`() {
        // 有效的旋转角度
        assertTrue(validator.validate(CommandScanner("rotated 0 0 run say hello")))
        assertTrue(validator.validate(CommandScanner("rotated 90 -45 run say hello")))
        assertTrue(validator.validate(CommandScanner("rotated ~ ~ run say hello")))
        assertTrue(validator.validate(CommandScanner("rotated ~90 ~-45 run say hello")))
        
        // 超出范围的角度
        assertFalse(validator.validate(CommandScanner("rotated 361 0 run say hello")))
        assertFalse(validator.validate(CommandScanner("rotated 0 -361 run say hello")))
        
        // 无效的角度格式
        assertFalse(validator.validate(CommandScanner("rotated invalid 0 run say hello")))
        assertFalse(validator.validate(CommandScanner("rotated 0 invalid run say hello")))
        
        // 角度数量不足
        assertFalse(validator.validate(CommandScanner("rotated 0 run say hello")))
    }
    
    @Test
    fun `test execute in subcommand`() {
        // 有效的维度
        assertTrue(validator.validate(CommandScanner("in minecraft:overworld run say hello")))
        assertTrue(validator.validate(CommandScanner("in minecraft:the_nether run say hello")))
        assertTrue(validator.validate(CommandScanner("in minecraft:the_end run say hello")))
        assertTrue(validator.validate(CommandScanner("in overworld run say hello")))
        assertTrue(validator.validate(CommandScanner("in the_nether run say hello")))
        assertTrue(validator.validate(CommandScanner("in the_end run say hello")))
        
        // 无效的维度
        assertFalse(validator.validate(CommandScanner("in invalid_dimension run say hello")))
        assertFalse(validator.validate(CommandScanner("in custom:dimension run say hello")))
    }
    
    @Test
    fun `test execute align subcommand`() {
        // 有效的轴参数
        assertTrue(validator.validate(CommandScanner("align xyz run say hello")))
        assertTrue(validator.validate(CommandScanner("align xy run say hello")))
        assertTrue(validator.validate(CommandScanner("align x run say hello")))
        assertTrue(validator.validate(CommandScanner("align y run say hello")))
        assertTrue(validator.validate(CommandScanner("align z run say hello")))
        assertTrue(validator.validate(CommandScanner("align xz run say hello")))
        assertTrue(validator.validate(CommandScanner("align yz run say hello")))
        
        // 无效的轴参数
        assertFalse(validator.validate(CommandScanner("align invalid run say hello")))
        assertFalse(validator.validate(CommandScanner("align xyzw run say hello")))
        assertFalse(validator.validate(CommandScanner("align abc run say hello")))
        assertFalse(validator.validate(CommandScanner("align run say hello"))) // 空轴参数
    }
    
    @Test
    fun `test execute anchored subcommand`() {
        // 有效的锚点
        assertTrue(validator.validate(CommandScanner("anchored eyes run say hello")))
        assertTrue(validator.validate(CommandScanner("anchored feet run say hello")))
        
        // 无效的锚点
        assertFalse(validator.validate(CommandScanner("anchored invalid run say hello")))
        assertFalse(validator.validate(CommandScanner("anchored head run say hello")))
    }
    
    @Test
    fun `test dangerous subcommands`() {
        // 危险的子命令应该被拒绝
        assertFalse(validator.validate(CommandScanner("if block ~ ~ ~ air run say hello")))
        assertFalse(validator.validate(CommandScanner("unless entity @a run say hello")))
        assertFalse(validator.validate(CommandScanner("store result score test objective run say hello")))
    }
    
    @Test
    fun `test complex execute chains`() {
        // 复杂的execute链
        assertTrue(validator.validate(CommandScanner("as @s at @s positioned ~ ~1 ~ run say hello")))
        assertTrue(validator.validate(CommandScanner("as @s positioned 0 64 0 facing ~ ~ ~ run give @s dirt")))
        assertTrue(validator.validate(CommandScanner("positioned ~ ~ ~ rotated ~ ~ anchored eyes run tp ~ ~ ~")))
        
        // 包含危险元素的链
        assertFalse(validator.validate(CommandScanner("as @a at @s run say hello"))) // @a不安全
        assertFalse(validator.validate(CommandScanner("as @s positioned 1001 0 0 run say hello"))) // 坐标超出范围
    }
    
    @Test
    fun `test recursion depth limit`() {
        // 设置一个包含execute命令的根树
        val recursiveTrie = CommandTrie()
        recursiveTrie.addCommand("execute as @s run say hello")
        
        val recursiveValidator = ExecuteValidator(recursiveTrie, maxDepth = 3)
        
        // 应该在达到深度限制前被拒绝
        assertFalse(recursiveValidator.validate(CommandScanner("run execute as @s run execute as @s run execute as @s run say hello")))
    }
    
    @Test
    fun `test strict validator`() {
        // 严格验证器有更严格的限制
        assertTrue(strictValidator.validate(CommandScanner("as @s run say hello")))
        
        // 坐标范围更严格
        assertFalse(strictValidator.validate(CommandScanner("positioned 101 0 0 run say hello")))
        
        // 不允许相对坐标
        assertFalse(strictValidator.validate(CommandScanner("positioned ~ ~ ~ run say hello")))
        
        // 深度限制更严格
        assertEquals(3, strictValidator.getMaxDepth())
    }
    
    @Test
    fun `test permissive validator`() {
        // 宽松验证器允许更多选择器
        assertTrue(permissiveValidator.validate(CommandScanner("as @p run say hello")))
        assertTrue(permissiveValidator.validate(CommandScanner("at @p run say hello")))
        
        // 允许玩家名
        assertTrue(permissiveValidator.validate(CommandScanner("as player123 run say hello")))
    }
    
    @Test
    fun `test incomplete execute commands`() {
        // 不完整的execute命令
        assertFalse(validator.validate(CommandScanner("as @s"))) // 缺少run
        assertFalse(validator.validate(CommandScanner("positioned 0 0 0"))) // 缺少run
        assertFalse(validator.validate(CommandScanner("facing 0 0 0"))) // 缺少run
        assertFalse(validator.validate(CommandScanner("as"))) // 缺少参数和run
        assertFalse(validator.validate(CommandScanner(""))) // 空命令
    }
    
    @Test
    fun `test invalid execute syntax`() {
        // 无效的execute语法
        assertFalse(validator.validate(CommandScanner("invalid_subcommand run say hello")))
        assertFalse(validator.validate(CommandScanner("as run say hello"))) // 缺少目标
        assertFalse(validator.validate(CommandScanner("positioned run say hello"))) // 缺少坐标
        assertFalse(validator.validate(CommandScanner("facing run say hello"))) // 缺少目标或坐标
    }
    
    @Test
    fun `test validator configuration`() {
        assertEquals(ExecuteValidator.DEFAULT_MAX_DEPTH, validator.getMaxDepth())
        assertEquals(0, validator.getCurrentDepth()) // 初始深度为0
        
        assertNotNull(validator.getSelectorValidator())
        assertNotNull(validator.getCoordinateValidator())
        
        assertEquals(3, strictValidator.getMaxDepth())
        assertTrue(strictValidator.getSelectorValidator() is SelectorValidator)
        assertTrue(strictValidator.getCoordinateValidator() is CoordinateValidator)
    }
    
    @Test
    fun `test validator description and toString`() {
        val description = validator.getDescription()
        assertTrue(description.contains("Execute validator"))
        assertTrue(description.contains("maxDepth="))
        assertTrue(description.contains("currentDepth="))
        
        val stringRepresentation = validator.toString()
        assertTrue(stringRepresentation.contains("ExecuteValidator"))
        assertTrue(stringRepresentation.contains("maxDepth="))
        assertTrue(stringRepresentation.contains("currentDepth="))
    }
    
    @Test
    fun `test statistics tracking`() {
        // 执行一些验证
        validator.validate(CommandScanner("as @s run say hello"))
        validator.validate(CommandScanner("as @a run say hello"))
        validator.validate(CommandScanner("positioned 0 0 0 run give @s dirt"))
        
        val stats = validator.getStatistics()
        assertEquals(3L, stats["validationCount"])
        assertTrue(stats["acceptCount"] as Long > 0)
        assertTrue(stats["rejectCount"] as Long > 0)
    }
    
    @Test
    fun `test validator enable disable`() {
        // 禁用验证器应该总是通过
        validator.setEnabled(false)
        assertTrue(validator.validate(CommandScanner("as @a run dangerous_command")))
        assertTrue(validator.validate(CommandScanner("if block ~ ~ ~ air run say hello")))
        
        // 重新启用
        validator.setEnabled(true)
        assertFalse(validator.validate(CommandScanner("as @a run say hello")))
    }
}
