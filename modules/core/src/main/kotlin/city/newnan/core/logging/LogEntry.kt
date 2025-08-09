package city.newnan.core.logging

import java.time.LocalDateTime

/**
 * 日志条目数据类
 * 
 * 包含完整的日志信息，用于在不同的LoggerProvider之间传递
 */
data class LogEntry(
    /**
     * 日志级别
     */
    val level: LogLevel,
    
    /**
     * 日志消息
     */
    val message: String,
    
    /**
     * 时间戳
     */
    val timestamp: LocalDateTime = LocalDateTime.now(),
    
    /**
     * 异常信息（可选）
     */
    val throwable: Throwable? = null,
    
    /**
     * 日志来源（插件名称等）
     */
    val source: String? = null,
    
    /**
     * 额外的上下文信息
     */
    val context: Map<String, Any> = emptyMap(),
    
    /**
     * 线程名称
     */
    val threadName: String = Thread.currentThread().name
) {
    
    /**
     * 获取格式化的时间戳字符串
     */
    fun getFormattedTimestamp(pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
        return timestamp.format(java.time.format.DateTimeFormatter.ofPattern(pattern))
    }
    
    /**
     * 检查是否包含异常信息
     */
    fun hasThrowable(): Boolean = throwable != null
    
    /**
     * 获取完整的异常堆栈信息
     */
    fun getStackTrace(): String? {
        return throwable?.let { t ->
            val sw = java.io.StringWriter()
            val pw = java.io.PrintWriter(sw)
            t.printStackTrace(pw)
            sw.toString()
        }
    }
}
