package city.newnan.core.logging.formatter

import city.newnan.core.logging.LogEntry
import java.time.format.DateTimeFormatter

/**
 * JSON日志格式化器
 * 
 * 将日志条目格式化为JSON格式，便于日志分析工具处理
 */
class JsonLogFormatter : LogFormatter {
    
    override val name: String = "json"
    
    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
    
    override fun format(entry: LogEntry): String {
        val json = StringBuilder()
        json.append("{")
        
        // 基础字段
        json.append("\"timestamp\":\"${entry.timestamp.format(timestampFormatter)}\",")
        json.append("\"level\":\"${entry.level.displayName}\",")
        json.append("\"message\":\"${escapeJson(entry.message)}\",")
        json.append("\"thread\":\"${entry.threadName}\"")
        
        // 可选字段
        if (entry.source != null) {
            json.append(",\"source\":\"${escapeJson(entry.source)}\"")
        }
        
        if (entry.hasThrowable()) {
            json.append(",\"exception\":\"${escapeJson(entry.getStackTrace() ?: "")}\"")
        }
        
        // 上下文信息
        if (entry.context.isNotEmpty()) {
            json.append(",\"context\":{")
            val contextEntries = entry.context.entries.map { (key, value) ->
                "\"${escapeJson(key)}\":\"${escapeJson(value.toString())}\""
            }
            json.append(contextEntries.joinToString(","))
            json.append("}")
        }
        
        json.append("}")
        return json.toString()
    }
    
    /**
     * 转义JSON字符串中的特殊字符
     */
    private fun escapeJson(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
