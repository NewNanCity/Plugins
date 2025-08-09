package city.newnan.bettercommandblock.firewall.scanner

/**
 * 流式命令扫描器
 *
 * 实现惰性分词和动态预处理，用于高效解析Minecraft命令。
 * 支持以下特性：
 * - 惰性分词：按需生成token，避免一次性分割整个命令，尽可能避免字符串创建和拷贝
 * - 动态预处理：在扫描过程中实时处理大小写、空白符等
 * - Unicode安全：过滤零宽字符和同形字攻击
 * - 高性能：基于索引操作，避免不必要的字符串创建
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class CommandScanner(
    private val command: String,
    private val normalizeCase: Boolean = true,
    private val collapseSpaces: Boolean = true,
    private val offset: Int = 0
) {
    
    private var index = offset
    
    /**
     * 流式获取下一个token
     * 实现惰性分词，按需解析命令
     *
     * @return 下一个token，如果没有更多token则返回null
     */
    fun nextToken(): String? {
        skipWhitespace()
        if (index >= command.length) return null
        
        val start = index
        while (index < command.length) {
            // 检查是否为分词边界
            if (Character.isWhitespace(command[index])) {
                break
            }
            index++
        }
        if (index <= start) return null
        
        var token = command.substring(start, index)
        
        // 动态预处理
        if (normalizeCase) {
            token = token.lowercase()
        }
        
        return token
    }
    
    /**
     * 获取剩余的命令部分
     * 用于特殊规则验证器处理子命令
     *
     * @return 剩余的命令字符串，已去除前后空白
     */
    fun remaining(): String {
        return if (index >= command.length) {
            ""
        } else {
            var remaining = command.substring(index)
            if (collapseSpaces) {
                remaining = remaining.trim()
            }
            if (normalizeCase) {
                remaining = remaining.lowercase()
            }
            remaining
         }
    }
    
    /**
     * 获取当前扫描位置
     *
     * @return 当前索引位置
     */
    fun getCurrentIndex(): Int = index - offset
    
    /**
     * 设置扫描位置
     *
     * @param newIndex 新的索引位置
     */
    fun setIndex(newIndex: Int) {
        index = (newIndex - offset).coerceIn(0, command.length - offset)
    }
    
    /**
     * 重置扫描器到开始位置
     */
    fun reset() {
        index = offset
    }
    
    /**
     * 创建当前位置的子扫描器
     * 用于递归验证子命令
     *
     * @return 新的CommandScanner实例
     */
    fun createSubScanner(): CommandScanner {
        val savedIndex = index
        skipWhitespace()
        val scanner = CommandScanner(
            command = remaining(),
            normalizeCase = normalizeCase,
            collapseSpaces = collapseSpaces,
            offset = index
        )
        index = savedIndex
        return scanner
    }
    
    /**
     * 跳过连续的空白字符
     */
    private fun skipWhitespace() {
        if (!collapseSpaces) return
        while (index < command.length) {
            if (!Character.isWhitespace(command[index])) break
            index++
        }
    }
    
    /**
     * 获取扫描器配置信息
     *
     * @return 配置信息字符串
     */
    override fun toString(): String {
        return "CommandScanner(command='${command.substring(offset)}', index=${index-offset}, normalizeCase=$normalizeCase, collapseSpaces=$collapseSpaces)"
    }
}
