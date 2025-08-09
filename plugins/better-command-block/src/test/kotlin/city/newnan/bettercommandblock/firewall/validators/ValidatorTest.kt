package city.newnan.bettercommandblock.firewall.validators

import city.newnan.bettercommandblock.firewall.scanner.CommandScanner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

/**
 * Validator接口和AbstractValidator基类的单元测试
 *
 * 测试验证器的基础功能，包括：
 * - 基本验证接口
 * - 统计信息跟踪
 * - 启用/禁用功能
 * - 组合验证器
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class ValidatorTest {
    
    private lateinit var testValidator: TestValidator
    private lateinit var alwaysAcceptValidator: TestValidator
    private lateinit var alwaysRejectValidator: TestValidator
    
    @BeforeEach
    fun setUp() {
        testValidator = TestValidator("TestValidator", true)
        alwaysAcceptValidator = TestValidator("AlwaysAccept", true)
        alwaysRejectValidator = TestValidator("AlwaysReject", false)
    }
    
    /**
     * 测试用的验证器实现
     */
    private class TestValidator(
        name: String,
        private val shouldAccept: Boolean
    ) : AbstractValidator(name) {
        
        override fun doValidate(scanner: CommandScanner): Boolean {
            return shouldAccept
        }
    }
    
    @Test
    fun `test basic validator interface`() {
        assertEquals("TestValidator", testValidator.getName())
        assertTrue(testValidator.getDescription().contains("TestValidator"))
        assertEquals("1.0.0", testValidator.getVersion())
        assertTrue(testValidator.isEnabled())
    }
    
    @Test
    fun `test validator enable disable`() {
        assertTrue(testValidator.isEnabled())
        
        testValidator.setEnabled(false)
        assertFalse(testValidator.isEnabled())
        
        testValidator.setEnabled(true)
        assertTrue(testValidator.isEnabled())
    }
    
    @Test
    fun `test validation with enabled validator`() {
        val scanner = CommandScanner("test command")
        
        assertTrue(alwaysAcceptValidator.validate(scanner))
        assertFalse(alwaysRejectValidator.validate(scanner))
    }
    
    @Test
    fun `test validation with disabled validator`() {
        val scanner = CommandScanner("test command")
        
        // 禁用验证器应该总是返回true（通过）
        alwaysRejectValidator.setEnabled(false)
        assertTrue(alwaysRejectValidator.validate(scanner))
        
        alwaysAcceptValidator.setEnabled(false)
        assertTrue(alwaysAcceptValidator.validate(scanner))
    }
    
    @Test
    fun `test statistics tracking`() {
        val scanner = CommandScanner("test command")
        
        // 初始统计
        var stats = testValidator.getStatistics()
        assertEquals(0L, stats["validationCount"])
        assertEquals(0L, stats["acceptCount"])
        assertEquals(0L, stats["rejectCount"])
        assertEquals(0.0, stats["acceptRate"])
        
        // 执行验证
        testValidator.validate(scanner)
        
        stats = testValidator.getStatistics()
        assertEquals(1L, stats["validationCount"])
        assertEquals(1L, stats["acceptCount"])
        assertEquals(0L, stats["rejectCount"])
        assertEquals(1.0, stats["acceptRate"])
        
        // 执行更多验证
        alwaysRejectValidator.validate(scanner)
        alwaysRejectValidator.validate(scanner)
        
        stats = alwaysRejectValidator.getStatistics()
        assertEquals(2L, stats["validationCount"])
        assertEquals(0L, stats["acceptCount"])
        assertEquals(2L, stats["rejectCount"])
        assertEquals(0.0, stats["acceptRate"])
    }
    
    @Test
    fun `test statistics reset`() {
        val scanner = CommandScanner("test command")
        
        // 执行一些验证
        testValidator.validate(scanner)
        testValidator.validate(scanner)
        
        var stats = testValidator.getStatistics()
        assertTrue(stats["validationCount"] as Long > 0)
        
        // 重置统计
        testValidator.resetStatistics()
        
        stats = testValidator.getStatistics()
        assertEquals(0L, stats["validationCount"])
        assertEquals(0L, stats["acceptCount"])
        assertEquals(0L, stats["rejectCount"])
        assertEquals(0.0, stats["acceptRate"])
    }
    
    @Test
    fun `test isSafeSelector utility method`() {
        // 通过反射访问protected方法进行测试
        val method = AbstractValidator::class.java.getDeclaredMethod("isSafeSelector", String::class.java)
        method.isAccessible = true
        
        // 安全的选择器
        assertTrue(method.invoke(testValidator, "@s") as Boolean)
        
        // 不安全的选择器
        assertFalse(method.invoke(testValidator, "@a") as Boolean)
        assertFalse(method.invoke(testValidator, "@e") as Boolean)
        assertFalse(method.invoke(testValidator, "@p") as Boolean)
        assertFalse(method.invoke(testValidator, "@r") as Boolean)
        
        // 玩家名（简单检查）
        assertTrue(method.invoke(testValidator, "player123") as Boolean)
        assertTrue(method.invoke(testValidator, "test_user") as Boolean)
        
        // 无效的玩家名
        assertFalse(method.invoke(testValidator, "toolongplayername123456") as Boolean)
        assertFalse(method.invoke(testValidator, "invalid-name") as Boolean)
        
        // null值
        assertFalse(method.invoke(testValidator, null) as Boolean)
    }
    
    @Test
    fun `test isSafeCoordinate utility method`() {
        val method = AbstractValidator::class.java.getDeclaredMethod("isSafeCoordinate", String::class.java, Double::class.java)
        method.isAccessible = true
        
        // 相对坐标总是安全的
        assertTrue(method.invoke(testValidator, "~", 1000.0) as Boolean)
        assertTrue(method.invoke(testValidator, "~10", 1000.0) as Boolean)
        assertTrue(method.invoke(testValidator, "~-5", 1000.0) as Boolean)
        
        // 安全范围内的绝对坐标
        assertTrue(method.invoke(testValidator, "0", 1000.0) as Boolean)
        assertTrue(method.invoke(testValidator, "500", 1000.0) as Boolean)
        assertTrue(method.invoke(testValidator, "-500", 1000.0) as Boolean)
        
        // 超出安全范围的绝对坐标
        assertFalse(method.invoke(testValidator, "1001", 1000.0) as Boolean)
        assertFalse(method.invoke(testValidator, "-1001", 1000.0) as Boolean)
        
        // 无效的坐标格式
        assertFalse(method.invoke(testValidator, "invalid", 1000.0) as Boolean)
        assertFalse(method.invoke(testValidator, null, 1000.0) as Boolean)
    }
    
    @Test
    fun `test isSafeNumber utility method`() {
        val method = AbstractValidator::class.java.getDeclaredMethod("isSafeNumber", String::class.java, Int::class.java, Int::class.java)
        method.isAccessible = true
        
        // 安全范围内的数字
        assertTrue(method.invoke(testValidator, "0", 0, 100) as Boolean)
        assertTrue(method.invoke(testValidator, "50", 0, 100) as Boolean)
        assertTrue(method.invoke(testValidator, "100", 0, 100) as Boolean)
        
        // 超出范围的数字
        assertFalse(method.invoke(testValidator, "-1", 0, 100) as Boolean)
        assertFalse(method.invoke(testValidator, "101", 0, 100) as Boolean)
        
        // 无效的数字格式
        assertFalse(method.invoke(testValidator, "invalid", 0, 100) as Boolean)
        assertFalse(method.invoke(testValidator, null, 0, 100) as Boolean)
    }
    
    @Test
    fun `test skipTokens utility method`() {
        val scanner = CommandScanner("token1 token2 token3 token4")
        val method = AbstractValidator::class.java.getDeclaredMethod("skipTokens", CommandScanner::class.java, Int::class.java)
        method.isAccessible = true
        
        // 跳过2个token
        val skipped = method.invoke(testValidator, scanner, 2) as Int
        assertEquals(2, skipped)
        
        // 验证跳过后的位置
        assertEquals("token3", scanner.nextToken())
        
        // 尝试跳过超过剩余token数量
        val remainingSkipped = method.invoke(testValidator, scanner, 5) as Int
        assertEquals(1, remainingSkipped) // 只剩1个token
    }
    
    @Test
    fun `test composite validator AND mode`() {
        val validators = listOf(alwaysAcceptValidator, alwaysAcceptValidator)
        val compositeValidator = CompositeValidator(validators, CompositeValidator.CompositeMode.AND)
        
        val scanner = CommandScanner("test command")
        
        // 所有验证器都接受，应该返回true
        assertTrue(compositeValidator.validate(scanner))
        
        // 添加一个拒绝的验证器
        val mixedValidators = listOf(alwaysAcceptValidator, alwaysRejectValidator)
        val mixedComposite = CompositeValidator(mixedValidators, CompositeValidator.CompositeMode.AND)
        
        // 有一个验证器拒绝，应该返回false
        assertFalse(mixedComposite.validate(scanner))
    }
    
    @Test
    fun `test composite validator OR mode`() {
        val validators = listOf(alwaysRejectValidator, alwaysRejectValidator)
        val compositeValidator = CompositeValidator(validators, CompositeValidator.CompositeMode.OR)
        
        val scanner = CommandScanner("test command")
        
        // 所有验证器都拒绝，应该返回false
        assertFalse(compositeValidator.validate(scanner))
        
        // 添加一个接受的验证器
        val mixedValidators = listOf(alwaysRejectValidator, alwaysAcceptValidator)
        val mixedComposite = CompositeValidator(mixedValidators, CompositeValidator.CompositeMode.OR)
        
        // 有一个验证器接受，应该返回true
        assertTrue(mixedComposite.validate(scanner))
    }
    
    @Test
    fun `test composite validator empty list`() {
        val emptyComposite = CompositeValidator(emptyList())
        val scanner = CommandScanner("test command")
        
        // 空的验证器列表应该返回true
        assertTrue(emptyComposite.validate(scanner))
    }
    
    @Test
    fun `test composite validator description`() {
        val validators = listOf(alwaysAcceptValidator, alwaysRejectValidator)
        val compositeValidator = CompositeValidator(validators, CompositeValidator.CompositeMode.AND)
        
        val description = compositeValidator.getDescription()
        assertTrue(description.contains("Composite validator"))
        assertTrue(description.contains("AND"))
        assertTrue(description.contains("AlwaysAccept"))
        assertTrue(description.contains("AlwaysReject"))
    }
    
    @Test
    fun `test validator toString`() {
        val scanner = CommandScanner("test command")
        testValidator.validate(scanner)
        
        val stringRepresentation = testValidator.toString()
        assertTrue(stringRepresentation.contains("Validator"))
        assertTrue(stringRepresentation.contains("TestValidator"))
        assertTrue(stringRepresentation.contains("enabled=true"))
        assertTrue(stringRepresentation.contains("validations=1"))
    }
}
