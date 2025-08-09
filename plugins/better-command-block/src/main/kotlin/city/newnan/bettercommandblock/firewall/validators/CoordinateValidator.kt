package city.newnan.bettercommandblock.firewall.validators

import city.newnan.bettercommandblock.firewall.scanner.CommandScanner
import kotlin.math.abs

/**
 * 坐标验证器
 *
 * 验证命令中的坐标参数是否在安全范围内。
 * 主要用于验证tp、setblock、fill等命令中的坐标，防止对远距离位置进行操作。
 *
 * 支持的功能：
 * - 绝对坐标范围限制
 * - 相对坐标支持（~坐标总是安全的）
 * - 局部坐标支持（^坐标）
 * - 三维坐标验证
 * - 可配置的坐标范围
 *
 * @param maxRange 最大允许的绝对坐标范围
 * @param allowRelative 是否允许相对坐标
 * @param allowLocal 是否允许局部坐标
 * @param coordinateCount 需要验证的坐标数量（默认3个：x, y, z）
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class CoordinateValidator(
    private val maxRange: Double = DEFAULT_MAX_RANGE,
    private val allowRelative: Boolean = true,
    private val allowLocal: Boolean = true,
    private val coordinateCount: Int = 3
) : AbstractValidator("CoordinateValidator", "Validates coordinate parameters") {
    
    companion object {
        /**
         * 默认最大坐标范围
         */
        const val DEFAULT_MAX_RANGE = 1000.0
        
        /**
         * 世界边界限制（Minecraft世界边界）
         */
        const val WORLD_BORDER_LIMIT = 30000000.0
    }
    
    override fun doValidate(scanner: CommandScanner): Boolean {
        // 验证指定数量的坐标
        repeat(coordinateCount) { index ->
            val coordinate = scanner.nextToken()
            if (coordinate == null || !isCoordinateSafe(coordinate)) {
                return false
            }
        }
        
        return true
    }
    
    /**
     * 检查单个坐标是否安全
     *
     * @param coordinate 坐标字符串
     * @return 如果坐标安全则返回true
     */
    private fun isCoordinateSafe(coordinate: String): Boolean {
        return when {
            // 相对坐标（~）
            coordinate.startsWith("~") -> {
                if (!allowRelative) return false
                validateRelativeCoordinate(coordinate)
            }
            
            // 局部坐标（^）
            coordinate.startsWith("^") -> {
                if (!allowLocal) return false
                validateLocalCoordinate(coordinate)
            }
            
            // 绝对坐标
            else -> validateAbsoluteCoordinate(coordinate)
        }
    }
    
    /**
     * 验证相对坐标
     *
     * @param coordinate 相对坐标字符串（如 ~, ~10, ~-5）
     * @return 如果坐标有效则返回true
     */
    private fun validateRelativeCoordinate(coordinate: String): Boolean {
        // 纯相对坐标（~）总是安全的
        if (coordinate == "~") return true
        
        // 带偏移的相对坐标（~10, ~-5）
        val offset = coordinate.substring(1)
        if (offset.isEmpty()) return true
        
        return try {
            val offsetValue = offset.toDouble()
            // 相对坐标的偏移也应该在合理范围内
            abs(offsetValue) <= maxRange
        } catch (e: NumberFormatException) {
            false
        }
    }
    
    /**
     * 验证局部坐标
     *
     * @param coordinate 局部坐标字符串（如 ^, ^10, ^-5）
     * @return 如果坐标有效则返回true
     */
    private fun validateLocalCoordinate(coordinate: String): Boolean {
        // 纯局部坐标（^）总是安全的
        if (coordinate == "^") return true
        
        // 带偏移的局部坐标（^10, ^-5）
        val offset = coordinate.substring(1)
        if (offset.isEmpty()) return true
        
        return try {
            val offsetValue = offset.toDouble()
            // 局部坐标的偏移也应该在合理范围内
            abs(offsetValue) <= maxRange
        } catch (e: NumberFormatException) {
            false
        }
    }
    
    /**
     * 验证绝对坐标
     *
     * @param coordinate 绝对坐标字符串
     * @return 如果坐标在安全范围内则返回true
     */
    private fun validateAbsoluteCoordinate(coordinate: String): Boolean {
        return try {
            val value = coordinate.toDouble()
            
            // 检查是否在配置的范围内
            if (abs(value) > maxRange) {
                return false
            }
            
            // 检查是否在世界边界内
            if (abs(value) > WORLD_BORDER_LIMIT) {
                return false
            }
            
            true
        } catch (e: NumberFormatException) {
            false
        }
    }
    
    /**
     * 创建用于验证2D坐标的验证器（如 /setworldspawn）
     *
     * @param maxRange 最大坐标范围
     * @return 2D坐标验证器
     */
    fun create2D(maxRange: Double = this.maxRange): CoordinateValidator {
        return CoordinateValidator(
            maxRange = maxRange,
            allowRelative = allowRelative,
            allowLocal = allowLocal,
            coordinateCount = 2
        )
    }
    
    /**
     * 创建用于验证单个坐标的验证器
     *
     * @param maxRange 最大坐标范围
     * @return 单坐标验证器
     */
    fun create1D(maxRange: Double = this.maxRange): CoordinateValidator {
        return CoordinateValidator(
            maxRange = maxRange,
            allowRelative = allowRelative,
            allowLocal = allowLocal,
            coordinateCount = 1
        )
    }
    
    /**
     * 创建严格的坐标验证器（不允许相对和局部坐标）
     *
     * @param maxRange 最大坐标范围
     * @return 严格坐标验证器
     */
    fun createStrict(maxRange: Double = this.maxRange): CoordinateValidator {
        return CoordinateValidator(
            maxRange = maxRange,
            allowRelative = false,
            allowLocal = false,
            coordinateCount = coordinateCount
        )
    }
    
    /**
     * 验证坐标范围是否合理
     *
     * @param coordinates 坐标列表
     * @return 如果坐标范围合理则返回true
     */
    fun validateCoordinateRange(coordinates: List<String>): Boolean {
        if (coordinates.size != coordinateCount) return false
        
        return coordinates.all { coordinate ->
            isCoordinateSafe(coordinate)
        }
    }
    
    /**
     * 计算两个坐标点之间的距离（仅用于绝对坐标）
     *
     * @param coord1 第一个坐标点
     * @param coord2 第二个坐标点
     * @return 距离，如果包含相对坐标则返回null
     */
    fun calculateDistance(coord1: List<String>, coord2: List<String>): Double? {
        if (coord1.size != 3 || coord2.size != 3) return null
        
        try {
            // 检查是否都是绝对坐标
            val values1 = coord1.map { coord ->
                if (coord.startsWith("~") || coord.startsWith("^")) return null
                coord.toDouble()
            }
            val values2 = coord2.map { coord ->
                if (coord.startsWith("~") || coord.startsWith("^")) return null
                coord.toDouble()
            }
            
            // 计算3D距离
            val dx = values1[0] - values2[0]
            val dy = values1[1] - values2[1]
            val dz = values1[2] - values2[2]
            
            return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
        } catch (e: NumberFormatException) {
            return null
        }
    }
    
    /**
     * 获取最大坐标范围
     *
     * @return 最大范围
     */
    fun getMaxRange(): Double = maxRange
    
    /**
     * 检查是否允许相对坐标
     *
     * @return 如果允许则返回true
     */
    fun isRelativeAllowed(): Boolean = allowRelative
    
    /**
     * 检查是否允许局部坐标
     *
     * @return 如果允许则返回true
     */
    fun isLocalAllowed(): Boolean = allowLocal
    
    /**
     * 获取坐标数量
     *
     * @return 坐标数量
     */
    fun getCoordinateCount(): Int = coordinateCount
    
    override fun getDescription(): String {
        return "Coordinate validator: range=±$maxRange, relative=$allowRelative, local=$allowLocal, count=$coordinateCount"
    }
    
    override fun toString(): String {
        return "CoordinateValidator(maxRange=$maxRange, allowRelative=$allowRelative, allowLocal=$allowLocal, coordinateCount=$coordinateCount)"
    }
}
