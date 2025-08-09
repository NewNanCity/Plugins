package city.newnan.bettercommandblock.firewall.validators

import city.newnan.bettercommandblock.firewall.scanner.CommandScanner
import city.newnan.bettercommandblock.firewall.trie.CommandTrie

/**
 * Execute命令验证器
 *
 * 验证execute命令的复杂结构，支持递归验证子命令。
 * Execute命令是Minecraft中最复杂的命令之一，支持多种子命令和条件。
 *
 * 支持的功能：
 * - 递归子命令验证
 * - 执行深度限制
 * - 条件子命令验证
 * - 目标选择器验证
 * - 坐标参数验证
 *
 * @param rootTrie 根命令树，用于递归验证子命令
 * @param maxDepth 最大递归深度
 * @param selectorValidator 选择器验证器
 * @param coordinateValidator 坐标验证器
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class ExecuteValidator(
    private val rootTrie: CommandTrie,
    private val maxDepth: Int = DEFAULT_MAX_DEPTH,
    private val selectorValidator: SelectorValidator = SelectorValidator.createStrict(),
    private val coordinateValidator: CoordinateValidator = CoordinateValidator()
) : AbstractValidator("ExecuteValidator", "Validates execute command structure") {

    companion object {
        /**
         * 默认最大递归深度
         */
        const val DEFAULT_MAX_DEPTH = 10

        /**
         * Execute命令的子命令类型
         */
        enum class ExecuteSubcommand {
            // 执行条件
            IF, UNLESS,

            // 执行上下文
            AS, AT, POSITIONED, ROTATED, FACING, IN,

            // 执行动作
            RUN,

            // 数据操作
            STORE,

            // 对齐
            ALIGN,

            // 锚点
            ANCHORED
        }

        /**
         * 安全的execute子命令（不需要特殊验证）
         */
        val SAFE_SUBCOMMANDS = setOf(
            "run"
        )

        /**
         * 需要目标验证的子命令
         */
        val TARGET_SUBCOMMANDS = setOf(
            "as", "at"
        )

        /**
         * 需要坐标验证的子命令
         */
        val COORDINATE_SUBCOMMANDS = setOf(
            "positioned", "facing"
        )

        /**
         * 危险的子命令（可能被滥用）
         */
        val DANGEROUS_SUBCOMMANDS = setOf(
            "store", "if", "unless"
        )

        /**
         * 创建严格的execute验证器
         */
        fun createStrict(rootTrie: CommandTrie): ExecuteValidator {
            return ExecuteValidator(
                rootTrie = rootTrie,
                maxDepth = 3,
                selectorValidator = SelectorValidator(
                    allowedSelectors = setOf("@s"),
                    maxRange = 0.0,
                    allowPlayerNames = false,
                    maxTargetCount = 1
                ),
                coordinateValidator = CoordinateValidator(
                    maxRange = 100.0,
                    allowRelative = false,
                    allowLocal = false
                )
            )
        }

        /**
         * 创建宽松的execute验证器
         */
        fun createPermissive(rootTrie: CommandTrie): ExecuteValidator {
            return ExecuteValidator(
                rootTrie = rootTrie,
                maxDepth = DEFAULT_MAX_DEPTH,
                selectorValidator = SelectorValidator(
                    allowedSelectors = setOf("@s", "@p"),
                    maxRange = 100.0,
                    allowPlayerNames = true,
                    maxTargetCount = 1
                ),
                coordinateValidator = CoordinateValidator()
            )
        }
    }

    /**
     * 当前递归深度
     */
    private var currentDepth = 0

    override fun doValidate(scanner: CommandScanner): Boolean {
        currentDepth = 0
        return validateExecuteCommand(scanner)
    }

    /**
     * 验证execute命令
     *
     * @param scanner 命令扫描器
     * @return 如果命令安全则返回true
     */
    private fun validateExecuteCommand(scanner: CommandScanner): Boolean {
        // 检查递归深度
        if (currentDepth >= maxDepth) {
            return false
        }

        currentDepth++

        try {
            while (true) {
                val token = scanner.nextToken() ?: break

                when (token.lowercase()) {
                    "run" -> {
                        // 遇到run关键字，验证后续的子命令
                        return validateSubCommand(scanner)
                    }

                    "as" -> {
                        if (!validateAsSubcommand(scanner)) return false
                    }

                    "at" -> {
                        if (!validateAtSubcommand(scanner)) return false
                    }

                    "positioned" -> {
                        if (!validatePositionedSubcommand(scanner)) return false
                    }

                    "facing" -> {
                        if (!validateFacingSubcommand(scanner)) return false
                    }

                    "rotated" -> {
                        if (!validateRotatedSubcommand(scanner)) return false
                    }

                    "in" -> {
                        if (!validateInSubcommand(scanner)) return false
                    }

                    "align" -> {
                        if (!validateAlignSubcommand(scanner)) return false
                    }

                    "anchored" -> {
                        if (!validateAnchoredSubcommand(scanner)) return false
                    }

                    // 危险的子命令直接拒绝
                    "if", "unless", "store" -> {
                        return false
                    }

                    else -> {
                        // 未知的子命令
                        return false
                    }
                }
            }

            // 如果没有遇到run关键字，命令不完整
            return false

        } finally {
            currentDepth--
        }
    }

    /**
     * 验证子命令
     *
     * @param scanner 命令扫描器
     * @return 如果子命令安全则返回true
     */
    private fun validateSubCommand(scanner: CommandScanner): Boolean {
        val subCommand = scanner.remaining()
        if (subCommand.isEmpty()) return false

        // 使用根命令树递归验证子命令
        return rootTrie.isCommandSafe(subCommand)
    }

    /**
     * 验证as子命令
     */
    private fun validateAsSubcommand(scanner: CommandScanner): Boolean {
        // as需要一个目标选择器参数
        return selectorValidator.validate(scanner)
    }

    /**
     * 验证at子命令
     */
    private fun validateAtSubcommand(scanner: CommandScanner): Boolean {
        // at需要一个目标选择器参数
        return selectorValidator.validate(scanner)
    }

    /**
     * 验证positioned子命令
     */
    private fun validatePositionedSubcommand(scanner: CommandScanner): Boolean {
        // positioned需要三个坐标参数
        return coordinateValidator.validate(scanner)
    }

    /**
     * 验证facing子命令
     */
    private fun validateFacingSubcommand(scanner: CommandScanner): Boolean {
        val nextToken = scanner.nextToken() ?: return false

        return when (nextToken.lowercase()) {
            "entity" -> {
                // facing entity <target> [<anchor>]
                if (!selectorValidator.validate(scanner)) return false

                // 可选的anchor参数
                val anchor = scanner.nextToken() ?: return true
                return isValidAnchor(anchor)
            }
            else -> {
                // facing <pos> - 需要三个坐标
                // 将token放回扫描器
                val originalIndex = scanner.getCurrentIndex()
                scanner.setIndex(originalIndex - nextToken.length - 1)
                coordinateValidator.validate(scanner)
            }
        }
    }

    /**
     * 验证rotated子命令
     */
    private fun validateRotatedSubcommand(scanner: CommandScanner): Boolean {
        // rotated需要两个角度参数（y-rot x-rot）
        val yRot = scanner.nextToken()
        val xRot = scanner.nextToken()

        return yRot != null && xRot != null &&
               isValidRotation(yRot) && isValidRotation(xRot)
    }

    /**
     * 验证in子命令
     */
    private fun validateInSubcommand(scanner: CommandScanner): Boolean {
        val dimension = scanner.nextToken() ?: return false

        // 验证维度名称
        return isValidDimension(dimension)
    }

    /**
     * 验证align子命令
     */
    private fun validateAlignSubcommand(scanner: CommandScanner): Boolean {
        val axes = scanner.nextToken() ?: return false

        // 验证轴参数（如 xyz, xy, z等）
        return isValidAxes(axes)
    }

    /**
     * 验证anchored子命令
     */
    private fun validateAnchoredSubcommand(scanner: CommandScanner): Boolean {
        val anchor = scanner.nextToken() ?: return false

        return isValidAnchor(anchor)
    }

    /**
     * 检查是否为有效的锚点
     */
    private fun isValidAnchor(anchor: String): Boolean {
        return anchor.lowercase() in setOf("eyes", "feet")
    }

    /**
     * 检查是否为有效的旋转角度
     */
    private fun isValidRotation(rotation: String): Boolean {
        // 支持相对角度（~）和绝对角度
        if (rotation.startsWith("~")) {
            val offset = rotation.substring(1)
            if (offset.isEmpty()) return true

            return try {
                val value = offset.toDouble()
                value >= -360.0 && value <= 360.0
            } catch (e: NumberFormatException) {
                false
            }
        }

        return try {
            val value = rotation.toDouble()
            value >= -360.0 && value <= 360.0
        } catch (e: NumberFormatException) {
            false
        }
    }

    /**
     * 检查是否为有效的维度
     */
    private fun isValidDimension(dimension: String): Boolean {
        val validDimensions = setOf(
            "minecraft:overworld",
            "minecraft:the_nether",
            "minecraft:the_end",
            "overworld",
            "the_nether",
            "the_end"
        )

        return dimension.lowercase() in validDimensions
    }

    /**
     * 检查是否为有效的轴参数
     */
    private fun isValidAxes(axes: String): Boolean {
        // 轴参数只能包含x、y、z字符
        return axes.lowercase().all { it in setOf('x', 'y', 'z') } && axes.isNotEmpty()
    }



    /**
     * 获取最大递归深度
     */
    fun getMaxDepth(): Int = maxDepth

    /**
     * 获取当前递归深度
     */
    fun getCurrentDepth(): Int = currentDepth

    /**
     * 获取选择器验证器
     */
    fun getSelectorValidator(): SelectorValidator = selectorValidator

    /**
     * 获取坐标验证器
     */
    fun getCoordinateValidator(): CoordinateValidator = coordinateValidator

    override fun getDescription(): String {
        return "Execute validator: maxDepth=$maxDepth, currentDepth=$currentDepth"
    }

    override fun toString(): String {
        return "ExecuteValidator(maxDepth=$maxDepth, currentDepth=$currentDepth)"
    }
}
