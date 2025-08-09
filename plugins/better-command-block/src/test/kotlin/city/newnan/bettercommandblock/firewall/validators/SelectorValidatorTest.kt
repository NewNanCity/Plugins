package city.newnan.bettercommandblock.firewall.validators

import city.newnan.bettercommandblock.firewall.scanner.CommandScanner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

/**
 * SelectorValidator单元测试
 *
 * 测试选择器验证器的各种功能，包括：
 * - 基础选择器验证
 * - 选择器参数验证
 * - 玩家名验证
 * - 安全性检查
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class SelectorValidatorTest {

    private lateinit var validator: SelectorValidator
    private lateinit var strictValidator: SelectorValidator
    private lateinit var permissiveValidator: SelectorValidator

    @BeforeEach
    fun setUp() {
        validator = SelectorValidator()
        strictValidator = SelectorValidator.createStrict()
        permissiveValidator = SelectorValidator.createPermissive()
    }

    @Test
    fun `test basic selector validation`() {
        // 默认只允许@s
        assertTrue(validator.validate(CommandScanner("@s")))
        assertFalse(validator.validate(CommandScanner("@p")))
        assertFalse(validator.validate(CommandScanner("@a")))
        assertFalse(validator.validate(CommandScanner("@e")))
        assertFalse(validator.validate(CommandScanner("@r")))
    }

    @Test
    fun `test strict validator`() {
        // 严格验证器只允许@s，不允许玩家名
        assertTrue(strictValidator.validate(CommandScanner("@s")))
        assertFalse(strictValidator.validate(CommandScanner("@p")))
        assertFalse(strictValidator.validate(CommandScanner("@a")))
        assertFalse(strictValidator.validate(CommandScanner("player123")))
        assertFalse(strictValidator.validate(CommandScanner("test_user")))
    }

    @Test
    fun `test permissive validator`() {
        // 宽松验证器允许@s和@p，以及玩家名
        assertTrue(permissiveValidator.validate(CommandScanner("@s")))
        assertTrue(permissiveValidator.validate(CommandScanner("@p")))
        assertFalse(permissiveValidator.validate(CommandScanner("@a")))
        assertFalse(permissiveValidator.validate(CommandScanner("@e")))
        assertTrue(permissiveValidator.validate(CommandScanner("player123")))
        assertTrue(permissiveValidator.validate(CommandScanner("test_user")))
    }

    @Test
    fun `test player name validation`() {
        // 有效的玩家名
        assertTrue(validator.validate(CommandScanner("player")))
        assertTrue(validator.validate(CommandScanner("player123")))
        assertTrue(validator.validate(CommandScanner("test_user")))
        assertTrue(validator.validate(CommandScanner("Player_123")))
        assertTrue(validator.validate(CommandScanner("a")))
        assertTrue(validator.validate(CommandScanner("1234567890123456"))) // 16字符

        // 无效的玩家名
        assertFalse(validator.validate(CommandScanner(""))) // 空字符串
        assertFalse(validator.validate(CommandScanner("12345678901234567"))) // 超过16字符
        assertFalse(validator.validate(CommandScanner("player-name"))) // 包含连字符
        assertFalse(validator.validate(CommandScanner("player.name"))) // 包含点
        assertFalse(validator.validate(CommandScanner("player name"))) // 包含空格
        assertFalse(validator.validate(CommandScanner("player@name"))) // 包含@符号
    }

    @Test
    fun `test selector with parameters`() {
        // 带参数的@s选择器
        assertTrue(validator.validate(CommandScanner("@s[gamemode=creative]")))
        assertTrue(validator.validate(CommandScanner("@s[distance=..10]")))
        assertTrue(validator.validate(CommandScanner("@s[x=0,y=64,z=0]")))
        assertTrue(validator.validate(CommandScanner("@s[limit=1]")))

        // 危险的参数
        assertFalse(validator.validate(CommandScanner("@s[type=player]")))
        assertFalse(validator.validate(CommandScanner("@s[name=test]")))
        assertFalse(validator.validate(CommandScanner("@s[tag=admin]")))
        assertFalse(validator.validate(CommandScanner("@s[nbt={test:1}]")))
    }

    @Test
    fun `test distance parameter validation`() {
        // 有效的距离参数
        assertTrue(validator.validate(CommandScanner("@s[distance=..10]")))
        assertTrue(validator.validate(CommandScanner("@s[distance=5..]")))
        assertTrue(validator.validate(CommandScanner("@s[distance=5..10]")))
        assertTrue(validator.validate(CommandScanner("@s[distance=50]")))
        assertTrue(validator.validate(CommandScanner("@s[distance=..100]")))

        // 超出范围的距离参数
        assertFalse(validator.validate(CommandScanner("@s[distance=..101]")))
        assertFalse(validator.validate(CommandScanner("@s[distance=200..]")))
        assertFalse(validator.validate(CommandScanner("@s[distance=50..200]")))

        // 无效的距离格式
        assertFalse(validator.validate(CommandScanner("@s[distance=invalid]")))
        assertFalse(validator.validate(CommandScanner("@s[distance=..invalid]")))
    }

    @Test
    fun `test coordinate parameter validation`() {
        // 有效的坐标参数
        assertTrue(validator.validate(CommandScanner("@s[x=0]")))
        assertTrue(validator.validate(CommandScanner("@s[y=64]")))
        assertTrue(validator.validate(CommandScanner("@s[z=-100]")))
        assertTrue(validator.validate(CommandScanner("@s[x=~10]")))
        assertTrue(validator.validate(CommandScanner("@s[x=0,y=64,z=100]")))

        // 超出范围的坐标
        assertFalse(validator.validate(CommandScanner("@s[x=1001]")))
        assertFalse(validator.validate(CommandScanner("@s[y=-1001]")))
        assertFalse(validator.validate(CommandScanner("@s[z=2000]")))

        // 无效的坐标格式
        assertFalse(validator.validate(CommandScanner("@s[x=invalid]")))
    }

    @Test
    fun `test range parameter validation`() {
        // 有效的范围参数
        assertTrue(validator.validate(CommandScanner("@s[dx=10]")))
        assertTrue(validator.validate(CommandScanner("@s[dy=-5]")))
        assertTrue(validator.validate(CommandScanner("@s[dz=100]")))
        assertTrue(validator.validate(CommandScanner("@s[dx=0,dy=0,dz=0]")))

        // 超出范围的参数
        assertFalse(validator.validate(CommandScanner("@s[dx=1001]")))
        assertFalse(validator.validate(CommandScanner("@s[dy=-1001]")))

        // 无效的范围格式
        assertFalse(validator.validate(CommandScanner("@s[dx=invalid]")))
    }

    @Test
    fun `test limit parameter validation`() {
        // 有效的限制参数
        assertTrue(validator.validate(CommandScanner("@s[limit=1]")))
        assertTrue(validator.validate(CommandScanner("@s[c=1]")))

        // 超出范围的限制
        assertFalse(validator.validate(CommandScanner("@s[limit=2]"))) // 默认最大为1
        assertFalse(validator.validate(CommandScanner("@s[limit=10]")))
        assertFalse(validator.validate(CommandScanner("@s[limit=0]")))
        assertFalse(validator.validate(CommandScanner("@s[limit=-1]")))

        // 无效的限制格式
        assertFalse(validator.validate(CommandScanner("@s[limit=invalid]")))
    }

    @Test
    fun `test sort parameter validation`() {
        // 有效的排序参数
        assertTrue(validator.validate(CommandScanner("@s[sort=nearest]")))
        assertTrue(validator.validate(CommandScanner("@s[sort=furthest]")))
        assertTrue(validator.validate(CommandScanner("@s[sort=random]")))
        assertTrue(validator.validate(CommandScanner("@s[sort=arbitrary]")))

        // 无效的排序参数
        assertFalse(validator.validate(CommandScanner("@s[sort=invalid]")))
        assertFalse(validator.validate(CommandScanner("@s[sort=custom]")))
    }

    @Test
    fun `test gamemode parameter validation`() {
        // 有效的游戏模式参数
        assertTrue(validator.validate(CommandScanner("@s[gamemode=survival]")))
        assertTrue(validator.validate(CommandScanner("@s[gamemode=creative]")))
        assertTrue(validator.validate(CommandScanner("@s[gamemode=adventure]")))
        assertTrue(validator.validate(CommandScanner("@s[gamemode=spectator]")))
        assertTrue(validator.validate(CommandScanner("@s[gamemode=0]")))
        assertTrue(validator.validate(CommandScanner("@s[gamemode=1]")))
        assertTrue(validator.validate(CommandScanner("@s[gamemode=2]")))
        assertTrue(validator.validate(CommandScanner("@s[gamemode=3]")))

        // 无效的游戏模式参数
        assertFalse(validator.validate(CommandScanner("@s[gamemode=invalid]")))
        assertFalse(validator.validate(CommandScanner("@s[gamemode=4]")))
        assertFalse(validator.validate(CommandScanner("@s[gamemode=-1]")))
    }

    @Test
    fun `test level parameter validation`() {
        // 有效的等级参数
        assertTrue(validator.validate(CommandScanner("@s[level=0]")))
        assertTrue(validator.validate(CommandScanner("@s[level=50]")))
        assertTrue(validator.validate(CommandScanner("@s[level=100]")))
        assertTrue(validator.validate(CommandScanner("@s[level=..50]")))
        assertTrue(validator.validate(CommandScanner("@s[level=10..]")))
        assertTrue(validator.validate(CommandScanner("@s[level=10..50]")))

        // 超出范围的等级参数
        assertFalse(validator.validate(CommandScanner("@s[level=101]")))
        assertFalse(validator.validate(CommandScanner("@s[level=-1]")))
        assertFalse(validator.validate(CommandScanner("@s[level=..101]")))

        // 无效的等级格式
        assertFalse(validator.validate(CommandScanner("@s[level=invalid]")))
    }

    @Test
    fun `test multiple parameters`() {
        // 有效的多参数组合
        assertTrue(validator.validate(CommandScanner("@s[gamemode=creative,distance=..10]")))
        assertTrue(validator.validate(CommandScanner("@s[x=0,y=64,z=0,distance=..5]")))
        assertTrue(validator.validate(CommandScanner("@s[gamemode=survival,level=10..50,limit=1]")))

        // 包含危险参数的组合
        assertFalse(validator.validate(CommandScanner("@s[gamemode=creative,type=player]")))
        assertFalse(validator.validate(CommandScanner("@s[distance=..10,name=test]")))
    }

    @Test
    fun `test invalid selector formats`() {
        // 无效的选择器格式
        assertFalse(validator.validate(CommandScanner("@")))
        assertFalse(validator.validate(CommandScanner("@invalid")))
        assertFalse(validator.validate(CommandScanner("@s["))) // 未闭合的括号
        assertFalse(validator.validate(CommandScanner("@s]"))) // 错误的括号
        assertFalse(validator.validate(CommandScanner("@s[invalid"))) // 未闭合的括号

        // 空参数
        assertFalse(validator.validate(CommandScanner("@s[]")))
    }

    @Test
    fun `test validator configuration`() {
        assertEquals(SelectorValidator.DEFAULT_ALLOWED_SELECTORS, validator.getAllowedSelectors())
        assertEquals(SelectorValidator.DEFAULT_MAX_RANGE, validator.getMaxRange())
        assertTrue(validator.isPlayerNamesAllowed())
        assertEquals(SelectorValidator.DEFAULT_MAX_TARGET_COUNT, validator.getMaxTargetCount())

        assertEquals(setOf("@s"), strictValidator.getAllowedSelectors())
        assertEquals(0.0, strictValidator.getMaxRange())
        assertFalse(strictValidator.isPlayerNamesAllowed())
        assertEquals(1, strictValidator.getMaxTargetCount())

        assertEquals(setOf("@s", "@p"), permissiveValidator.getAllowedSelectors())
        assertTrue(permissiveValidator.isPlayerNamesAllowed())
    }

    @Test
    fun `test validator description and toString`() {
        val description = validator.getDescription()
        assertTrue(description.contains("Selector validator"))
        assertTrue(description.contains("allowed="))
        assertTrue(description.contains("range="))
        assertTrue(description.contains("playerNames="))
        assertTrue(description.contains("maxTargets="))

        val stringRepresentation = validator.toString()
        assertTrue(stringRepresentation.contains("SelectorValidator"))
        assertTrue(stringRepresentation.contains("allowed="))
        assertTrue(stringRepresentation.contains("maxRange="))
        assertTrue(stringRepresentation.contains("playerNames="))
        assertTrue(stringRepresentation.contains("maxTargets="))
    }

    @Test
    fun `test edge cases`() {
        // 边界值测试
        assertTrue(validator.validate(CommandScanner("@s[distance=..100]")))
        assertFalse(validator.validate(CommandScanner("@s[distance=..100.1]")))

        // 特殊字符在玩家名中
        assertFalse(validator.validate(CommandScanner("player@test")))
        assertFalse(validator.validate(CommandScanner("player#test")))
        assertFalse(validator.validate(CommandScanner("player$test")))

        // 大小写敏感性（注意：CommandScanner会规范化大小写）
        assertTrue(validator.validate(CommandScanner("Player123")))
        assertTrue(validator.validate(CommandScanner("PLAYER")))
    }

    @Test
    fun `test statistics tracking`() {
        // 执行一些验证
        validator.validate(CommandScanner("@s"))
        validator.validate(CommandScanner("@a"))
        validator.validate(CommandScanner("player123"))

        val stats = validator.getStatistics()
        assertEquals(3L, stats["validationCount"])
        assertTrue(stats["acceptCount"] as Long > 0)
        assertTrue(stats["rejectCount"] as Long > 0)
    }

    @Test
    fun `test validator enable disable`() {
        // 禁用验证器应该总是通过
        validator.setEnabled(false)
        assertTrue(validator.validate(CommandScanner("@a")))
        assertTrue(validator.validate(CommandScanner("@e")))
        assertTrue(validator.validate(CommandScanner("invalid-player-name")))

        // 重新启用
        validator.setEnabled(true)
        assertFalse(validator.validate(CommandScanner("@a")))
    }
}
