package city.newnan.core.utils.text

object ComponentParseModeDetector {

    const val SECTION_CHAR = '\u00A7'

    const val AMPERSAND_CHAR = '&'

    /**
     * 猜测字符串到底是什么格式
     */
    fun detect(text: String): ComponentParseMode {
        if (text.contains(SECTION_CHAR)) return ComponentParseMode.Legacy
        if (guessIfMiniMessage(text)) return ComponentParseMode.MiniMessage
        if (guessIfLegacy(text)) return ComponentParseMode.Legacy
        return ComponentParseMode.Plain
    }

    /**
     * 高效检查字符是否为合法的 Legacy 颜色代码字符。
     *
     * @param c 要检查的字符
     * @return 如果字符是合法的 Legacy 颜色代码字符返回 true，否则返回 false
     */
    fun isValidLegacyColorCodeRange(c: Char): Boolean {
        return when (c) {
            in '0'..'9' -> true
            in 'A'..'F' -> true
            in 'a'..'f' -> true
            'K', 'k', 'L', 'l', 'M', 'm', 'N', 'n', 'O', 'o', 'R', 'r', 'X', 'x', '#' -> true
            else -> false
        }
    }

    fun guessIfLegacy(text: String): Boolean {
        for (i in 0 until text.length - 1) {
            val char = text[i]
            if (char == AMPERSAND_CHAR && isValidLegacyColorCodeRange(text[i + 1])) {
                return true
            }
        }
        return false
    }

    /**
     * 高效检查字符是否为 MiniMessage 标签的有效字符。
     *
     * @param c 要检查的字符
     * @return 如果字符是 MiniMessage 标签的有效字符返回 true，否则返回 false
     */
    fun isValidMiniMessageTagChar(c: Char): Boolean =
        c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c == '_' || c == '-'

    /**
     * 检测字符串是否包含 MiniMessage 语法特征。
     *
     * 此函数高效扫描字符串，寻找 MiniMessage 语法存在的证据，而非完整验证语法正确性。
     * 当检测到以下任一特征时返回 true：
     * 1. 有效的转义序列：\<, \>, \:
     * 2. 有效的标签结构：<tag> 或 <tag:param> 形式（标签内部无空格，除非在引号内）
     * 3. 引号包裹的内容（存在引号即视为特征）
     *
     * 算法特点：
     * - 单次遍历，O(n) 时间复杂度
     * - 短路优化：发现首个特征立即返回
     * - 无内存分配（零堆开销）
     * - 严格标签规则：标签内部不允许空格（除非在引号内）
     *
     * @param input 要检测的字符串
     * @return 如果字符串包含 MiniMessage 语法特征返回 true，否则返回 false
     */
    fun guessIfMiniMessage(input: String): Boolean {
        var i = 0
        while (i < input.length) {
            when (val c = input[i]) {
                // 处理转义序列：\<, \>, \: 的存在即表明使用 MiniMessage
                '\\' -> {
                    if (i + 1 < input.length) {
                        when (input[i + 1]) {
                            // 检测到有效转义序列（\<, \>, \:）
                            '<', '>', ':' -> return true
                            // 处理双反斜杠
                            '\\' -> i++  // 跳过下一个反斜杠
                        }
                    }
                    i += 2  // 跳过转义字符和下一个字符
                    continue
                }

                // 检测标签起始：<tag> 或 <tag:param>
                '<' -> {
                    var pos = i + 1  // 跳过 '<'
                    var hasValidTagChar = false  // 标签名有效性标志
                    var inQuotes: Char? = null  // 记录当前是否在引号内（'或"）

                    // 1. 检查可选前缀：/, !, ?, #
                    if (pos < input.length) {
                        when (input[pos]) {
                            '/', '!', '?', '#' -> {
                                // 跳过前缀
                                pos++
                                // 前缀后必须紧跟有效标签字符
                                if (pos < input.length && isValidMiniMessageTagChar(input[pos])) {
                                    hasValidTagChar = true
                                    pos++
                                }
                            }
                            else -> {
                                // 没有前缀时，当前字符必须是有效标签字符
                                if (isValidMiniMessageTagChar(input[pos])) {
                                    hasValidTagChar = true
                                    pos++
                                }
                            }
                        }
                    }

                    // 2. 检查标签名和参数部分
                    if (hasValidTagChar) {
                        while (pos < input.length) {
                            val currentChar = input[pos]

                            // 处理引号状态（开始/结束）
                            if (inQuotes != null) {
                                if (currentChar == inQuotes) {
                                    // 结束引号
                                    inQuotes = null
                                } else if (currentChar == '\\' && pos + 1 < input.length) {
                                    // 引号内的转义序列
                                    pos += 2  // 跳过转义字符和转义后的字符
                                    continue
                                }
                                pos++
                                continue
                            }

                            when (currentChar) {
                                // 开始引号
                                '"', '\'' -> {
                                    inQuotes = currentChar
                                    pos++
                                }
                                // 有效标签名字符
                                in 'a'..'z', in 'A'..'Z', in '0'..'9', '_', '-' -> pos++
                                // 参数分隔符
                                ':' -> pos++
                                // 标签结束符
                                '>' -> return true
                                // 空格 - 严格模式下不允许
                                ' ' -> break  // 遇到空格，终止标签检测
                                // 无效字符
                                else -> break
                            }
                        }
                    }
                }

                // 处理引号包裹的内容：存在引号即视为特征
                '"', '\'' -> {
                    val quote = c
                    var pos = i + 1
                    var escaped = false

                    while (pos < input.length) {
                        when {
                            escaped -> {
                                // 转义序列后字符，重置转义状态
                                escaped = false
                                pos++
                            }
                            input[pos] == '\\' -> {
                                // 转义字符
                                escaped = true
                                pos++
                            }
                            input[pos] == quote -> {
                                // 结束引号
                                return true  // 发现成对引号即返回
                            }
                            else -> pos++
                        }
                    }
                }
            }
            i++  // 移动到下一个字符
        }
        return false
    }
}