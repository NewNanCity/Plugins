package city.newnan.core.logging.formatter

import city.newnan.core.logging.LogEntry

/**
 * 日志格式化器接口
 * 
 * 负责将LogEntry格式化为字符串输出
 */
interface LogFormatter {
    
    /**
     * 格式化日志条目
     * @param entry 日志条目
     * @return 格式化后的字符串
     */
    fun format(entry: LogEntry): String
    
    /**
     * 格式化器名称
     */
    val name: String
}
