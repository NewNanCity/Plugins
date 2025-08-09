package city.newnan.bettercommandblock.firewall.validators

import city.newnan.bettercommandblock.firewall.scanner.CommandScanner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

/**
 * CoordinateValidator单元测试
 *
 * 测试坐标验证器的各种功能，包括：
 * - 绝对坐标验证
 * - 相对坐标验证
 * - 局部坐标验证
 * - 坐标范围限制
 * - 多维坐标验证
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class CoordinateValidatorTest {

    private lateinit var validator: CoordinateValidator
    private lateinit var strictValidator: CoordinateValidator
    private lateinit var validator2D: CoordinateValidator
    private lateinit var validator1D: CoordinateValidator

    @BeforeEach
    fun setUp() {
        validator = CoordinateValidator()
        strictValidator = CoordinateValidator.createStrict(100.0)
        validator2D = validator.create2D()
        validator1D = validator.create1D()
    }

    @Test
    fun `test absolute coordinates validation`() {
        // 有效的绝对坐标
        assertTrue(validator.validate(CommandScanner("0 0 0")))
        assertTrue(validator.validate(CommandScanner("100 64 -100")))
        assertTrue(validator.validate(CommandScanner("500 128 500")))
        assertTrue(validator.validate(CommandScanner("1000 256 -1000")))

        // 超出范围的绝对坐标
        assertFalse(validator.validate(CommandScanner("1001 0 0")))
        assertFalse(validator.validate(CommandScanner("0 0 -1001")))
        assertFalse(validator.validate(CommandScanner("2000 0 0")))
    }

    @Test
    fun `test relative coordinates validation`() {
        // 纯相对坐标
        assertTrue(validator.validate(CommandScanner("~ ~ ~")))

        // 带偏移的相对坐标
        assertTrue(validator.validate(CommandScanner("~10 ~-5 ~100")))
        assertTrue(validator.validate(CommandScanner("~0 ~0 ~0")))
        assertTrue(validator.validate(CommandScanner("~1000 ~-1000 ~500")))

        // 超出范围的相对坐标偏移
        assertFalse(validator.validate(CommandScanner("~1001 ~ ~")))
        assertFalse(validator.validate(CommandScanner("~ ~-1001 ~")))
    }

    @Test
    fun `test local coordinates validation`() {
        // 纯局部坐标
        assertTrue(validator.validate(CommandScanner("^ ^ ^")))

        // 带偏移的局部坐标
        assertTrue(validator.validate(CommandScanner("^10 ^-5 ^100")))
        assertTrue(validator.validate(CommandScanner("^0 ^0 ^0")))
        assertTrue(validator.validate(CommandScanner("^1000 ^-1000 ^500")))

        // 超出范围的局部坐标偏移
        assertFalse(validator.validate(CommandScanner("^1001 ^ ^")))
        assertFalse(validator.validate(CommandScanner("^ ^-1001 ^")))
    }

    @Test
    fun `test mixed coordinate types`() {
        // 混合坐标类型
        assertTrue(validator.validate(CommandScanner("~ 100 ^10")))
        assertTrue(validator.validate(CommandScanner("0 ~ ^")))
        assertTrue(validator.validate(CommandScanner("^5 ~-10 200")))

        // 混合坐标中的无效值
        assertFalse(validator.validate(CommandScanner("~ 1001 ^")))
        assertFalse(validator.validate(CommandScanner("^1001 ~ 0")))
    }

    @Test
    fun `test strict validator`() {
        // 严格验证器不允许相对和局部坐标
        assertFalse(strictValidator.validate(CommandScanner("~ ~ ~")))
        assertFalse(strictValidator.validate(CommandScanner("^ ^ ^")))
        assertFalse(strictValidator.validate(CommandScanner("~10 ~-5 ~100")))

        // 只允许绝对坐标
        assertTrue(strictValidator.validate(CommandScanner("0 0 0")))
        assertTrue(strictValidator.validate(CommandScanner("100 64 -100")))

        // 超出范围的绝对坐标
        assertFalse(strictValidator.validate(CommandScanner("101 0 0")))
    }

    @Test
    fun `test 2D coordinate validation`() {
        // 2D坐标只需要两个参数
        assertTrue(validator2D.validate(CommandScanner("0 0")))
        assertTrue(validator2D.validate(CommandScanner("~ ~")))
        assertTrue(validator2D.validate(CommandScanner("^ ^")))
        assertTrue(validator2D.validate(CommandScanner("100 -100")))

        // 超出范围
        assertFalse(validator2D.validate(CommandScanner("1001 0")))

        // 参数数量不匹配（这个测试可能需要调整，因为validate方法会尝试读取指定数量的坐标）
        // 注意：这里的行为取决于CommandScanner的实现
    }

    @Test
    fun `test 1D coordinate validation`() {
        // 1D坐标只需要一个参数
        assertTrue(validator1D.validate(CommandScanner("0")))
        assertTrue(validator1D.validate(CommandScanner("~")))
        assertTrue(validator1D.validate(CommandScanner("^")))
        assertTrue(validator1D.validate(CommandScanner("100")))
        assertTrue(validator1D.validate(CommandScanner("~50")))

        // 超出范围
        assertFalse(validator1D.validate(CommandScanner("1001")))
        assertFalse(validator1D.validate(CommandScanner("~1001")))
    }

    @Test
    fun `test invalid coordinate formats`() {
        // 无效的坐标格式
        assertFalse(validator.validate(CommandScanner("invalid 0 0")))
        assertFalse(validator.validate(CommandScanner("0 invalid 0")))
        assertFalse(validator.validate(CommandScanner("0 0 invalid")))

        // 空坐标
        assertFalse(validator.validate(CommandScanner("")))
        assertFalse(validator.validate(CommandScanner("   ")))

        // 不完整的坐标
        assertFalse(validator.validate(CommandScanner("0")))
        assertFalse(validator.validate(CommandScanner("0 0")))
    }

    @Test
    fun `test world border limits`() {
        val largeRangeValidator = CoordinateValidator(maxRange = CoordinateValidator.WORLD_BORDER_LIMIT + 1000)

        // 在世界边界内
        assertTrue(largeRangeValidator.validate(CommandScanner("29999999 0 -29999999")))

        // 超出世界边界
        assertFalse(largeRangeValidator.validate(CommandScanner("30000001 0 0")))
        assertFalse(largeRangeValidator.validate(CommandScanner("0 0 -30000001")))
    }

    @Test
    fun `test decimal coordinates`() {
        // 小数坐标
        assertTrue(validator.validate(CommandScanner("0.5 10.25 -5.75")))
        assertTrue(validator.validate(CommandScanner("~0.5 ~-10.25 ~5.75")))
        assertTrue(validator.validate(CommandScanner("^0.5 ^-10.25 ^5.75")))

        // 超出范围的小数坐标
        assertFalse(validator.validate(CommandScanner("1000.1 0 0")))
        assertFalse(validator.validate(CommandScanner("~1000.1 ~ ~")))
    }

    @Test
    fun `test validateCoordinateRange method`() {
        // 有效的坐标列表
        assertTrue(validator.validateCoordinateRange(listOf("0", "0", "0")))
        assertTrue(validator.validateCoordinateRange(listOf("~", "~", "~")))
        assertTrue(validator.validateCoordinateRange(listOf("^", "^", "^")))
        assertTrue(validator.validateCoordinateRange(listOf("100", "~50", "^-25")))

        // 无效的坐标列表
        assertFalse(validator.validateCoordinateRange(listOf("1001", "0", "0")))
        assertFalse(validator.validateCoordinateRange(listOf("invalid", "0", "0")))

        // 错误的坐标数量
        assertFalse(validator.validateCoordinateRange(listOf("0", "0"))) // 只有2个坐标
        assertFalse(validator.validateCoordinateRange(listOf("0", "0", "0", "0"))) // 4个坐标

        // 2D验证器
        assertTrue(validator2D.validateCoordinateRange(listOf("0", "0")))
        assertFalse(validator2D.validateCoordinateRange(listOf("0", "0", "0")))
    }

    @Test
    fun `test calculateDistance method`() {
        // 有效的距离计算
        val distance1 = validator.calculateDistance(listOf("0", "0", "0"), listOf("3", "4", "0"))
        assertEquals(5.0, distance1!!, 0.001) // 3-4-5三角形

        val distance2 = validator.calculateDistance(listOf("0", "0", "0"), listOf("0", "0", "0"))
        assertEquals(0.0, distance2!!, 0.001)

        // 包含相对坐标的情况
        assertNull(validator.calculateDistance(listOf("~", "0", "0"), listOf("0", "0", "0")))
        assertNull(validator.calculateDistance(listOf("0", "^", "0"), listOf("0", "0", "0")))

        // 无效的坐标数量
        assertNull(validator.calculateDistance(listOf("0", "0"), listOf("0", "0", "0")))
        assertNull(validator.calculateDistance(listOf("0", "0", "0"), listOf("0", "0")))

        // 无效的坐标格式
        assertNull(validator.calculateDistance(listOf("invalid", "0", "0"), listOf("0", "0", "0")))
    }

    @Test
    fun `test validator configuration`() {
        assertEquals(CoordinateValidator.DEFAULT_MAX_RANGE, validator.getMaxRange())
        assertTrue(validator.isRelativeAllowed())
        assertTrue(validator.isLocalAllowed())
        assertEquals(3, validator.getCoordinateCount())

        assertEquals(100.0, strictValidator.getMaxRange())
        assertFalse(strictValidator.isRelativeAllowed())
        assertFalse(strictValidator.isLocalAllowed())
        assertEquals(3, strictValidator.getCoordinateCount())

        assertEquals(2, validator2D.getCoordinateCount())
        assertEquals(1, validator1D.getCoordinateCount())
    }

    @Test
    fun `test validator description and toString`() {
        val description = validator.getDescription()
        assertTrue(description.contains("Coordinate validator"))
        assertTrue(description.contains("range="))
        assertTrue(description.contains("relative="))
        assertTrue(description.contains("local="))
        assertTrue(description.contains("count="))

        val stringRepresentation = validator.toString()
        assertTrue(stringRepresentation.contains("CoordinateValidator"))
        assertTrue(stringRepresentation.contains("maxRange="))
        assertTrue(stringRepresentation.contains("allowRelative="))
        assertTrue(stringRepresentation.contains("allowLocal="))
        assertTrue(stringRepresentation.contains("coordinateCount="))
    }

    @Test
    fun `test edge cases`() {
        // 边界值测试
        assertTrue(validator.validate(CommandScanner("1000 1000 1000")))
        assertTrue(validator.validate(CommandScanner("-1000 -1000 -1000")))
        assertFalse(validator.validate(CommandScanner("1000.1 0 0")))
        assertFalse(validator.validate(CommandScanner("-1000.1 0 0")))

        // 特殊数值
        assertTrue(validator.validate(CommandScanner("0.0 0.0 0.0")))
        assertTrue(validator.validate(CommandScanner("~0.0 ~0.0 ~0.0")))
        assertTrue(validator.validate(CommandScanner("^0.0 ^0.0 ^0.0")))

        // 科学计数法（可能不被支持）
        assertFalse(validator.validate(CommandScanner("1e3 0 0")))
        assertFalse(validator.validate(CommandScanner("1E3 0 0")))
    }

    @Test
    fun `test statistics tracking`() {
        // 执行一些验证
        validator.validate(CommandScanner("0 0 0"))
        validator.validate(CommandScanner("1001 0 0"))
        validator.validate(CommandScanner("~ ~ ~"))

        val stats = validator.getStatistics()
        assertEquals(3L, stats["validationCount"])
        assertTrue(stats["acceptCount"] as Long > 0)
        assertTrue(stats["rejectCount"] as Long > 0)
    }

    @Test
    fun `test validator enable disable`() {
        // 禁用验证器应该总是通过
        validator.setEnabled(false)
        assertTrue(validator.validate(CommandScanner("1001 1001 1001")))
        assertTrue(validator.validate(CommandScanner("invalid invalid invalid")))

        // 重新启用
        validator.setEnabled(true)
        assertFalse(validator.validate(CommandScanner("1001 1001 1001")))
    }
}
