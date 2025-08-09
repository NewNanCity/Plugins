package city.newnan.core.logging.formatter

import city.newnan.core.logging.LogEntry
import city.newnan.core.utils.text.ComponentProcessor

/**
 * 简单日志格式化器
 *
 * 提供标准的日志格式：[时间戳] [级别] 消息
 */
class SimpleLogFormatter(
    private val timestampPattern: String = "yyyy-MM-dd HH:mm:ss",
    private val includeSource: Boolean = true,
    private val includeThread: Boolean = false,
    private val transformColorSymbols: Boolean = false
) : LogFormatter {

    override val name: String = "simple"

    override fun format(entry: LogEntry): String {
        val timestamp = entry.getFormattedTimestamp(timestampPattern)
        val level = entry.level.displayName

        val parts = mutableListOf<String>()
        parts.add("[$timestamp]")
        parts.add("[$level]")

        if (includeSource && entry.source != null) {
            parts.add("[${entry.source}]")
        }

        if (includeThread) {
            parts.add("[${entry.threadName}]")
        }

        if (transformColorSymbols) {
            // 替换颜色符号
            parts.add(ComponentProcessor.stripLegacySymbol(entry.message))
        } else {
            // 直接添加消息
            parts.add(entry.message)
        }

        return parts.joinToString(" ")
    }
}
