package city.newnan.bettercommandblock.firewall.validators

import city.newnan.bettercommandblock.firewall.scanner.CommandScanner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

/**
 * ItemValidator单元测试
 *
 * 测试物品验证器的各种功能，包括：
 * - 安全物品验证
 * - 危险物品拦截
 * - NBT数据检查
 * - 数量限制
 * - 命名空间处理
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class ItemValidatorTest {
    
    private lateinit var validator: ItemValidator
    private lateinit var strictValidator: ItemValidator
    private lateinit var permissiveValidator: ItemValidator
    
    @BeforeEach
    fun setUp() {
        validator = ItemValidator()
        strictValidator = ItemValidator(
            safeItems = setOf("minecraft:dirt", "dirt"),
            maxQuantity = 1,
            allowCustomNamespaces = false
        )
        permissiveValidator = ItemValidator(
            safeItems = ItemValidator.DEFAULT_SAFE_ITEMS + setOf("custom:item"),
            maxQuantity = 128,
            allowCustomNamespaces = true
        )
    }
    
    @Test
    fun `test safe items validation`() {
        // 测试默认安全物品
        assertTrue(validator.validate(CommandScanner("minecraft:dirt")))
        assertTrue(validator.validate(CommandScanner("dirt")))
        assertTrue(validator.validate(CommandScanner("minecraft:stone")))
        assertTrue(validator.validate(CommandScanner("stone")))
        assertTrue(validator.validate(CommandScanner("minecraft:bread")))
        assertTrue(validator.validate(CommandScanner("bread")))
    }
    
    @Test
    fun `test dangerous items rejection`() {
        // 测试危险物品被拒绝
        assertFalse(validator.validate(CommandScanner("minecraft:command_block")))
        assertFalse(validator.validate(CommandScanner("command_block")))
        assertFalse(validator.validate(CommandScanner("minecraft:structure_block")))
        assertFalse(validator.validate(CommandScanner("structure_block")))
        assertFalse(validator.validate(CommandScanner("minecraft:barrier")))
        assertFalse(validator.validate(CommandScanner("barrier")))
        assertFalse(validator.validate(CommandScanner("minecraft:bedrock")))
        assertFalse(validator.validate(CommandScanner("bedrock")))
    }
    
    @Test
    fun `test case insensitive validation`() {
        assertTrue(validator.validate(CommandScanner("MINECRAFT:DIRT")))
        assertTrue(validator.validate(CommandScanner("Dirt")))
        assertTrue(validator.validate(CommandScanner("MineCraft:Stone")))
        
        assertFalse(validator.validate(CommandScanner("MINECRAFT:COMMAND_BLOCK")))
        assertFalse(validator.validate(CommandScanner("Command_Block")))
    }
    
    @Test
    fun `test items with NBT data`() {
        // 安全的物品，没有危险NBT
        assertTrue(validator.validate(CommandScanner("minecraft:dirt{test:1}")))
        
        // 包含危险NBT的物品
        assertFalse(validator.validate(CommandScanner("minecraft:dirt{Command:\"say hello\"}")))
        assertFalse(validator.validate(CommandScanner("minecraft:stone{command:\"give @a diamond\"}")))
        assertFalse(validator.validate(CommandScanner("minecraft:bread{CustomName:\"test\"}")))
        assertFalse(validator.validate(CommandScanner("minecraft:apple{Enchantments:[{id:sharpness,lvl:5}]}")))
    }
    
    @Test
    fun `test quantity validation`() {
        // 有效数量
        assertTrue(validator.validate(CommandScanner("minecraft:dirt 1")))
        assertTrue(validator.validate(CommandScanner("minecraft:dirt 32")))
        assertTrue(validator.validate(CommandScanner("minecraft:dirt 64")))
        
        // 无效数量（超过默认最大值64）
        assertFalse(validator.validate(CommandScanner("minecraft:dirt 65")))
        assertFalse(validator.validate(CommandScanner("minecraft:dirt 100")))
        assertFalse(validator.validate(CommandScanner("minecraft:dirt 0")))
        assertFalse(validator.validate(CommandScanner("minecraft:dirt -1")))
    }
    
    @Test
    fun `test custom quantity limits`() {
        // 严格验证器最大数量为1
        assertTrue(strictValidator.validate(CommandScanner("dirt 1")))
        assertFalse(strictValidator.validate(CommandScanner("dirt 2")))
        
        // 宽松验证器最大数量为128
        assertTrue(permissiveValidator.validate(CommandScanner("dirt 128")))
        assertFalse(permissiveValidator.validate(CommandScanner("dirt 129")))
    }
    
    @Test
    fun `test namespace handling`() {
        // 默认验证器不允许自定义命名空间
        assertFalse(validator.validate(CommandScanner("custom:item")))
        assertFalse(validator.validate(CommandScanner("mymod:special_item")))
        
        // 严格验证器不允许自定义命名空间
        assertFalse(strictValidator.validate(CommandScanner("custom:item")))
        
        // 宽松验证器允许自定义命名空间
        assertTrue(permissiveValidator.validate(CommandScanner("custom:item")))
    }
    
    @Test
    fun `test minecraft namespace`() {
        // minecraft命名空间总是被处理
        assertTrue(validator.validate(CommandScanner("minecraft:dirt")))
        assertTrue(strictValidator.validate(CommandScanner("minecraft:dirt")))
        assertTrue(permissiveValidator.validate(CommandScanner("minecraft:dirt")))
        
        // 不在安全列表中的minecraft物品
        assertFalse(validator.validate(CommandScanner("minecraft:unknown_item")))
        assertFalse(strictValidator.validate(CommandScanner("minecraft:stone"))) // stone不在严格列表中
    }
    
    @Test
    fun `test empty and invalid input`() {
        assertFalse(validator.validate(CommandScanner("")))
        assertFalse(validator.validate(CommandScanner("   ")))
        
        // 无效的物品格式
        assertFalse(validator.validate(CommandScanner("invalid::")))
        assertFalse(validator.validate(CommandScanner(":")))
    }
    
    @Test
    fun `test NBT parsing`() {
        // 测试NBT解析功能
        val testCases = mapOf(
            "minecraft:dirt{test:1}" to Pair("minecraft:dirt", "{test:1}"),
            "stone{complex:{nested:true}}" to Pair("stone", "{complex:{nested:true}}"),
            "bread" to Pair("bread", null),
            "apple{}" to Pair("apple", "{}")
        )
        
        // 通过反射访问私有方法进行测试
        val method = ItemValidator::class.java.getDeclaredMethod("parseItemToken", String::class.java)
        method.isAccessible = true
        
        for ((input, expected) in testCases) {
            val result = method.invoke(validator, input) as Pair<String, String?>
            assertEquals(expected, result, "Failed to parse: $input")
        }
    }
    
    @Test
    fun `test dangerous NBT detection`() {
        val method = ItemValidator::class.java.getDeclaredMethod("isNbtSafe", String::class.java)
        method.isAccessible = true
        
        // 安全的NBT
        assertTrue(method.invoke(validator, "{test:1}") as Boolean)
        assertTrue(method.invoke(validator, "{color:red}") as Boolean)
        assertTrue(method.invoke(validator, "{durability:100}") as Boolean)
        
        // 危险的NBT
        assertFalse(method.invoke(validator, "{Command:\"say hello\"}") as Boolean)
        assertFalse(method.invoke(validator, "{command:\"give @a diamond\"}") as Boolean)
        assertFalse(method.invoke(validator, "{CustomName:\"test\"}") as Boolean)
        assertFalse(method.invoke(validator, "{Enchantments:[]}") as Boolean)
        assertFalse(method.invoke(validator, "{give:\"test\"}") as Boolean)
    }
    
    @Test
    fun `test validator configuration`() {
        assertEquals(ItemValidator.DEFAULT_SAFE_ITEMS, validator.getSafeItems())
        assertEquals(ItemValidator.DEFAULT_MAX_QUANTITY, validator.getMaxQuantity())
        assertFalse(validator.isCustomNamespacesAllowed())
        
        assertEquals(setOf("minecraft:dirt", "dirt"), strictValidator.getSafeItems())
        assertEquals(1, strictValidator.getMaxQuantity())
        assertFalse(strictValidator.isCustomNamespacesAllowed())
        
        assertTrue(permissiveValidator.getSafeItems().contains("custom:item"))
        assertEquals(128, permissiveValidator.getMaxQuantity())
        assertTrue(permissiveValidator.isCustomNamespacesAllowed())
    }
    
    @Test
    fun `test validator description and toString`() {
        val description = validator.getDescription()
        assertTrue(description.contains("Item validator"))
        assertTrue(description.contains("safe items"))
        assertTrue(description.contains("max quantity"))
        
        val stringRepresentation = validator.toString()
        assertTrue(stringRepresentation.contains("ItemValidator"))
        assertTrue(stringRepresentation.contains("safeItems="))
        assertTrue(stringRepresentation.contains("maxQuantity="))
        assertTrue(stringRepresentation.contains("customNamespaces="))
    }
    
    @Test
    fun `test complex scenarios`() {
        // 复杂的NBT结构
        assertFalse(validator.validate(CommandScanner("minecraft:dirt{display:{Name:\"test\",Lore:[\"line1\"]}}")))
        
        // 多个参数
        assertTrue(validator.validate(CommandScanner("minecraft:dirt 32")))
        assertFalse(validator.validate(CommandScanner("minecraft:dirt 65")))
        
        // 边界情况
        assertTrue(validator.validate(CommandScanner("minecraft:dirt 1")))
        assertTrue(validator.validate(CommandScanner("minecraft:dirt 64")))
        assertFalse(validator.validate(CommandScanner("minecraft:dirt 0")))
        
        // 命名空间边界情况
        assertFalse(validator.validate(CommandScanner(":item")))
        assertFalse(validator.validate(CommandScanner("namespace:")))
    }
    
    @Test
    fun `test statistics tracking`() {
        // 执行一些验证
        validator.validate(CommandScanner("minecraft:dirt"))
        validator.validate(CommandScanner("minecraft:command_block"))
        validator.validate(CommandScanner("minecraft:stone"))
        
        val stats = validator.getStatistics()
        assertEquals(3L, stats["validationCount"])
        assertTrue(stats["acceptCount"] as Long > 0)
        assertTrue(stats["rejectCount"] as Long > 0)
    }
    
    @Test
    fun `test validator enable disable`() {
        // 禁用验证器应该总是通过
        validator.setEnabled(false)
        assertTrue(validator.validate(CommandScanner("minecraft:command_block")))
        assertTrue(validator.validate(CommandScanner("dangerous:item")))
        
        // 重新启用
        validator.setEnabled(true)
        assertFalse(validator.validate(CommandScanner("minecraft:command_block")))
    }
}
