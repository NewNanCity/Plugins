package city.newnan.bettercommandblock.firewall.validators

import city.newnan.bettercommandblock.firewall.scanner.CommandScanner

/**
 * 命令验证器接口
 *
 * 定义命令验证的基本契约。验证器用于对特定类型的命令进行深度验证，
 * 支持复杂的验证逻辑，如参数检查、权限验证、安全性检查等。
 *
 * 验证器的设计原则：
 * - 单一职责：每个验证器只负责一种类型的验证
 * - 可组合：多个验证器可以组合使用
 * - 高性能：验证过程应该尽可能高效
 * - 安全优先：当不确定时应该选择拒绝
 *
 * @author NewNanCity
 * @since 2.0.0
 */
interface Validator {
    
    /**
     * 验证命令是否安全
     *
     * @param scanner 命令扫描器，包含待验证的命令内容
     * @return 如果命令安全则返回true，否则返回false
     */
    fun validate(scanner: CommandScanner): Boolean
    
    /**
     * 获取验证器名称
     * 用于日志记录和调试
     *
     * @return 验证器名称
     */
    fun getName(): String
    
    /**
     * 获取验证器描述
     * 用于配置文档和用户界面
     *
     * @return 验证器描述
     */
    fun getDescription(): String {
        return "Command validator: ${getName()}"
    }
    
    /**
     * 获取验证器版本
     * 用于兼容性检查和更新管理
     *
     * @return 验证器版本
     */
    fun getVersion(): String {
        return "1.0.0"
    }
    
    /**
     * 检查验证器是否启用
     * 允许运行时禁用特定验证器
     *
     * @return 如果验证器启用则返回true
     */
    fun isEnabled(): Boolean {
        return true
    }
}

/**
 * 抽象验证器基类
 *
 * 提供验证器的通用实现和工具方法，简化具体验证器的开发。
 * 包含常用的验证逻辑和性能优化。
 *
 * @param name 验证器名称
 * @param description 验证器描述
 * @param enabled 是否启用
 */
abstract class AbstractValidator(
    private val name: String,
    private val description: String = "Command validator: $name",
    private var enabled: Boolean = true
) : Validator {
    
    /**
     * 验证统计信息
     */
    private var validationCount = 0L
    private var acceptCount = 0L
    private var rejectCount = 0L
    private var lastValidationTime = 0L
    
    override fun getName(): String = name
    
    override fun getDescription(): String = description
    
    override fun isEnabled(): Boolean = enabled
    
    /**
     * 设置验证器启用状态
     *
     * @param enabled 是否启用
     */
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }
    
    override fun validate(scanner: CommandScanner): Boolean {
        if (!enabled) {
            return true // 禁用时默认通过
        }
        
        val startTime = System.nanoTime()
        validationCount++
        
        try {
            val result = doValidate(scanner)
            if (result) {
                acceptCount++
            } else {
                rejectCount++
            }
            return result
        } finally {
            lastValidationTime = System.nanoTime() - startTime
        }
    }
    
    /**
     * 执行具体的验证逻辑
     * 子类需要实现此方法
     *
     * @param scanner 命令扫描器
     * @return 验证结果
     */
    protected abstract fun doValidate(scanner: CommandScanner): Boolean
    
    /**
     * 获取验证统计信息
     *
     * @return 统计信息映射
     */
    fun getStatistics(): Map<String, Any> {
        return mapOf(
            "name" to name,
            "enabled" to enabled,
            "validationCount" to validationCount,
            "acceptCount" to acceptCount,
            "rejectCount" to rejectCount,
            "acceptRate" to if (validationCount > 0) acceptCount.toDouble() / validationCount else 0.0,
            "lastValidationTimeNs" to lastValidationTime
        )
    }
    
    /**
     * 重置统计信息
     */
    fun resetStatistics() {
        validationCount = 0L
        acceptCount = 0L
        rejectCount = 0L
        lastValidationTime = 0L
    }
    
    /**
     * 检查token是否为安全的选择器
     *
     * @param token 要检查的token
     * @return 如果是安全选择器则返回true
     */
    protected fun isSafeSelector(token: String?): Boolean {
        if (token == null) return false
        
        // 只允许安全的选择器
        return when (token) {
            "@s" -> true // 自己
            "@p" -> false // 最近的玩家（可能不安全）
            "@a" -> false // 所有玩家（不安全）
            "@e" -> false // 所有实体（不安全）
            "@r" -> false // 随机玩家（不安全）
            else -> {
                // 检查是否为玩家名（简单检查）
                token.matches(Regex("[a-zA-Z0-9_]{1,16}"))
            }
        }
    }
    
    /**
     * 检查坐标是否安全
     *
     * @param coord 坐标字符串
     * @param maxRange 最大允许范围
     * @return 如果坐标安全则返回true
     */
    protected fun isSafeCoordinate(coord: String?, maxRange: Double = 1000.0): Boolean {
        if (coord == null) return false
        
        // 相对坐标总是安全的
        if (coord.startsWith("~")) return true
        
        // 检查绝对坐标范围
        try {
            val value = coord.toDouble()
            return value >= -maxRange && value <= maxRange
        } catch (e: NumberFormatException) {
            return false
        }
    }
    
    /**
     * 检查数值是否在安全范围内
     *
     * @param value 数值字符串
     * @param min 最小值
     * @param max 最大值
     * @return 如果数值安全则返回true
     */
    protected fun isSafeNumber(value: String?, min: Int = 0, max: Int = 1000): Boolean {
        if (value == null) return false
        
        try {
            val num = value.toInt()
            return num in min..max
        } catch (e: NumberFormatException) {
            return false
        }
    }
    
    /**
     * 跳过指定数量的token
     *
     * @param scanner 扫描器
     * @param count 要跳过的token数量
     * @return 实际跳过的token数量
     */
    protected fun skipTokens(scanner: CommandScanner, count: Int): Int {
        var skipped = 0
        repeat(count) {
            if (scanner.nextToken() != null) {
                skipped++
            }
        }
        return skipped
    }
    
    override fun toString(): String {
        return "Validator(name='$name', enabled=$enabled, validations=$validationCount)"
    }
}

/**
 * 组合验证器
 *
 * 将多个验证器组合成一个验证器，支持AND和OR逻辑。
 *
 * @param validators 子验证器列表
 * @param mode 组合模式
 */
class CompositeValidator(
    private val validators: List<Validator>,
    private val mode: CompositeMode = CompositeMode.AND
) : AbstractValidator("CompositeValidator", "Composite validator with ${validators.size} sub-validators") {
    
    enum class CompositeMode {
        AND, // 所有验证器都必须通过
        OR   // 至少一个验证器通过
    }
    
    override fun doValidate(scanner: CommandScanner): Boolean {
        if (validators.isEmpty()) return true
        
        return when (mode) {
            CompositeMode.AND -> {
                validators.all { validator ->
                    // 为每个验证器创建独立的扫描器副本
                    val subScanner = scanner.createSubScanner()
                    validator.validate(subScanner)
                }
            }
            CompositeMode.OR -> {
                validators.any { validator ->
                    // 为每个验证器创建独立的扫描器副本
                    val subScanner = scanner.createSubScanner()
                    validator.validate(subScanner)
                }
            }
        }
    }
    
    override fun getDescription(): String {
        return "Composite validator (${mode.name}) with ${validators.size} sub-validators: ${validators.joinToString { it.getName() }}"
    }
}
