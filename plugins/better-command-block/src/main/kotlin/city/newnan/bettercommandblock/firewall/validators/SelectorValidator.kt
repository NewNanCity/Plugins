package city.newnan.bettercommandblock.firewall.validators

import city.newnan.bettercommandblock.firewall.scanner.CommandScanner

/**
 * 选择器验证器
 *
 * 验证命令中的目标选择器是否安全。
 * 主要用于验证@p、@a、@e、@r、@s等选择器，防止对大量实体或不当目标进行操作。
 *
 * 支持的功能：
 * - 基础选择器类型检查
 * - 选择器参数验证
 * - 范围限制
 * - 实体类型限制
 * - 玩家名验证
 *
 * @param allowedSelectors 允许的选择器类型集合
 * @param maxRange 选择器最大范围
 * @param allowPlayerNames 是否允许直接使用玩家名
 * @param maxTargetCount 最大目标数量
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class SelectorValidator(
    private val allowedSelectors: Set<String> = DEFAULT_ALLOWED_SELECTORS,
    private val maxRange: Double = DEFAULT_MAX_RANGE,
    private val allowPlayerNames: Boolean = true,
    private val maxTargetCount: Int = DEFAULT_MAX_TARGET_COUNT
) : AbstractValidator("SelectorValidator", "Validates target selectors") {

    companion object {
        /**
         * 默认允许的选择器
         * 只包含相对安全的选择器
         */
        val DEFAULT_ALLOWED_SELECTORS = setOf("@s")

        /**
         * 所有可能的选择器类型
         */
        val ALL_SELECTORS = setOf("@p", "@a", "@e", "@r", "@s")

        /**
         * 危险的选择器（影响范围大）
         */
        val DANGEROUS_SELECTORS = setOf("@a", "@e")

        /**
         * 默认最大范围
         */
        const val DEFAULT_MAX_RANGE = 100.0

        /**
         * 默认最大目标数量
         */
        const val DEFAULT_MAX_TARGET_COUNT = 1

        /**
         * 危险的选择器参数
         */
        val DANGEROUS_SELECTOR_ARGS = setOf(
            "type", "name", "tag", "team", "scores", "advancements",
            "nbt", "level", "gamemode", "x_rotation", "y_rotation"
        )

        /**
         * 创建严格的选择器验证器（只允许@s）
         */
        fun createStrict(): SelectorValidator {
            return SelectorValidator(
                allowedSelectors = setOf("@s"),
                maxRange = 0.0,
                allowPlayerNames = false,
                maxTargetCount = 1
            )
        }

        /**
         * 创建宽松的选择器验证器
         */
        fun createPermissive(): SelectorValidator {
            return SelectorValidator(
                allowedSelectors = setOf("@s", "@p"),
                maxRange = DEFAULT_MAX_RANGE,
                allowPlayerNames = true,
                maxTargetCount = DEFAULT_MAX_TARGET_COUNT
            )
        }
    }

    override fun doValidate(scanner: CommandScanner): Boolean {
        val selectorToken = scanner.nextToken() ?: return false

        return when {
            // 检查是否为选择器
            selectorToken.startsWith("@") -> validateSelector(selectorToken)

            // 检查是否为玩家名
            allowPlayerNames -> validatePlayerName(selectorToken)

            // 不允许玩家名时拒绝
            else -> false
        }
    }

    /**
     * 验证选择器
     *
     * @param selector 选择器字符串（如 @s, @p[distance=..10]）
     * @return 如果选择器安全则返回true
     */
    private fun validateSelector(selector: String): Boolean {
        // 解析选择器类型和参数
        val (selectorType, selectorArgs) = parseSelectorToken(selector)

        // 检查选择器类型是否被允许
        if (!allowedSelectors.contains(selectorType)) {
            return false
        }

        // 如果有参数，验证参数
        if (selectorArgs != null) {
            return validateSelectorArguments(selectorArgs)
        }

        return true
    }

    /**
     * 验证玩家名
     *
     * @param playerName 玩家名字符串
     * @return 如果玩家名有效则返回true
     */
    private fun validatePlayerName(playerName: String): Boolean {
        // 基础的玩家名格式检查
        if (playerName.length > 16 || playerName.length < 1) {
            return false
        }

        // 检查字符是否有效（字母、数字、下划线）
        if (!playerName.matches(Regex("[a-zA-Z0-9_]+"))) {
            return false
        }

        return true
    }

    /**
     * 解析选择器token
     *
     * @param selector 选择器字符串
     * @return Pair<选择器类型, 选择器参数>
     */
    private fun parseSelectorToken(selector: String): Pair<String, String?> {
        val bracketStart = selector.indexOf('[')

        return if (bracketStart != -1) {
            val selectorType = selector.substring(0, bracketStart)
            val selectorArgs = selector.substring(bracketStart + 1, selector.lastIndexOf(']'))
            Pair(selectorType, selectorArgs)
        } else {
            Pair(selector, null)
        }
    }

    /**
     * 验证选择器参数
     *
     * @param args 选择器参数字符串
     * @return 如果参数安全则返回true
     */
    private fun validateSelectorArguments(args: String): Boolean {
        // 解析参数
        val parameters = parseSelectorArguments(args)

        for ((key, value) in parameters) {
            if (!validateSelectorParameter(key, value)) {
                return false
            }
        }

        return true
    }

    /**
     * 解析选择器参数
     *
     * @param args 参数字符串
     * @return 参数映射
     */
    private fun parseSelectorArguments(args: String): Map<String, String> {
        val parameters = mutableMapOf<String, String>()

        // 简单的参数解析（不处理嵌套结构）
        val parts = args.split(',')
        for (part in parts) {
            val trimmed = part.trim()
            val equalIndex = trimmed.indexOf('=')
            if (equalIndex != -1) {
                val key = trimmed.substring(0, equalIndex).trim()
                val value = trimmed.substring(equalIndex + 1).trim()
                parameters[key] = value
            }
        }

        return parameters
    }

    /**
     * 验证单个选择器参数
     *
     * @param key 参数键
     * @param value 参数值
     * @return 如果参数安全则返回true
     */
    private fun validateSelectorParameter(key: String, value: String): Boolean {
        return when (key.lowercase()) {
            // 距离参数
            "distance" -> validateDistanceParameter(value)

            // 坐标参数
            "x", "y", "z" -> isSafeCoordinate(value, maxRange)

            // 坐标范围参数
            "dx", "dy", "dz" -> validateRangeParameter(value)

            // 数量限制
            "limit", "c" -> validateLimitParameter(value)

            // 排序参数（相对安全）
            "sort" -> validateSortParameter(value)

            // 游戏模式（可能安全）
            "gamemode" -> validateGamemodeParameter(value)

            // 等级参数
            "level" -> validateLevelParameter(value)

            // 危险参数直接拒绝
            "type", "name", "tag", "team", "scores", "advancements", "nbt" -> false

            // 未知参数默认拒绝
            else -> false
        }
    }

    /**
     * 验证距离参数
     */
    private fun validateDistanceParameter(value: String): Boolean {
        // 支持范围格式：..10, 5.., 5..10
        return try {
            when {
                value.startsWith("..") -> {
                    val maxDist = value.substring(2).toDouble()
                    maxDist <= maxRange
                }
                value.endsWith("..") -> {
                    val minDist = value.substring(0, value.length - 2).toDouble()
                    minDist <= maxRange
                }
                value.contains("..") -> {
                    val parts = value.split("..")
                    if (parts.size == 2) {
                        val minDist = parts[0].toDouble()
                        val maxDist = parts[1].toDouble()
                        minDist <= maxRange && maxDist <= maxRange
                    } else false
                }
                else -> {
                    val dist = value.toDouble()
                    dist <= maxRange
                }
            }
        } catch (e: NumberFormatException) {
            false
        }
    }

    /**
     * 验证范围参数
     */
    private fun validateRangeParameter(value: String): Boolean {
        return try {
            val range = value.toDouble()
            kotlin.math.abs(range) <= maxRange
        } catch (e: NumberFormatException) {
            false
        }
    }

    /**
     * 验证数量限制参数
     */
    private fun validateLimitParameter(value: String): Boolean {
        return try {
            val limit = value.toInt()
            limit in 1..maxTargetCount
        } catch (e: NumberFormatException) {
            false
        }
    }

    /**
     * 验证排序参数
     */
    private fun validateSortParameter(value: String): Boolean {
        val allowedSorts = setOf("nearest", "furthest", "random", "arbitrary")
        return allowedSorts.contains(value.lowercase())
    }

    /**
     * 验证游戏模式参数
     */
    private fun validateGamemodeParameter(value: String): Boolean {
        val allowedGamemodes = setOf("survival", "creative", "adventure", "spectator", "0", "1", "2", "3")
        return allowedGamemodes.contains(value.lowercase())
    }

    /**
     * 验证等级参数
     */
    private fun validateLevelParameter(value: String): Boolean {
        return try {
            when {
                value.startsWith("..") -> {
                    val maxLevel = value.substring(2).toInt()
                    maxLevel in 0..100
                }
                value.endsWith("..") -> {
                    val minLevel = value.substring(0, value.length - 2).toInt()
                    minLevel in 0..100
                }
                value.contains("..") -> {
                    val parts = value.split("..")
                    if (parts.size == 2) {
                        val minLevel = parts[0].toInt()
                        val maxLevel = parts[1].toInt()
                        minLevel in 0..100 && maxLevel in 0..100
                    } else false
                }
                else -> {
                    val level = value.toInt()
                    level in 0..100
                }
            }
        } catch (e: NumberFormatException) {
            false
        }
    }



    /**
     * 获取允许的选择器
     */
    fun getAllowedSelectors(): Set<String> = allowedSelectors.toSet()

    /**
     * 获取最大范围
     */
    fun getMaxRange(): Double = maxRange

    /**
     * 检查是否允许玩家名
     */
    fun isPlayerNamesAllowed(): Boolean = allowPlayerNames

    /**
     * 获取最大目标数量
     */
    fun getMaxTargetCount(): Int = maxTargetCount

    override fun getDescription(): String {
        return "Selector validator: allowed=${allowedSelectors.joinToString()}, range=$maxRange, playerNames=$allowPlayerNames, maxTargets=$maxTargetCount"
    }

    override fun toString(): String {
        return "SelectorValidator(allowed=$allowedSelectors, maxRange=$maxRange, playerNames=$allowPlayerNames, maxTargets=$maxTargetCount)"
    }
}
