package city.newnan.core.logging.provider

import city.newnan.core.logging.LogEntry
import city.newnan.core.logging.LogLevel
import city.newnan.core.logging.formatter.LogFormatter
import city.newnan.core.logging.formatter.SimpleLogFormatter
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

/**
 * 文件日志输出提供者
 *
 * 将日志输出到文件，支持按日期分割日志文件
 */
class LogFileLoggerProvider(
    private val logDirectory: File,
    private val filePrefix: String = "",
    private val formatter: LogFormatter = SimpleLogFormatter(),
    override var minimumLevel: LogLevel = LogLevel.INFO,
    private val maxRetentionDays: Int = 0
) : LoggerProvider {

    override val name: String = "file"

    private val lock = ReentrantReadWriteLock()
    private var currentLogFile: File? = null
    private var currentDate: LocalDate? = null
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    @Volatile
    private var closed = false

    init {
        if (!logDirectory.exists()) {
            logDirectory.mkdirs()
        }
    }

    override fun initialize() {
        cleanupOldLogs()
    }

    override fun log(entry: LogEntry) {
        if (closed || !supportsLevel(entry.level)) return

        lock.write {
            try {
                ensureLogFile()
                writeToFile(entry)
            } catch (e: Exception) {
                // 避免日志记录本身出错导致的循环
                System.err.println("Failed to write to log file: ${e.message}")
            }
        }
    }

    private fun ensureLogFile() {
        val today = LocalDate.now()
        if (currentDate == null || !currentDate!!.isEqual(today)) {
            currentDate = today
            val dateStr = today.format(dateFormatter)
            currentLogFile = File(logDirectory, "$filePrefix$dateStr.log")
        }
    }

    private fun writeToFile(entry: LogEntry) {
        val logFile = currentLogFile ?: return

        FileWriter(logFile, true).use { writer ->
            PrintWriter(writer).use { printer ->
                val formattedMessage = formatter.format(entry)
                printer.println(formattedMessage)

                // 如果有异常信息，也写入文件
                if (entry.hasThrowable()) {
                    entry.throwable?.printStackTrace(printer)
                }
            }
        }
    }

    override fun flush() {
        // 文件写入是立即的，不需要额外的flush操作
    }

    /**
     * 清理旧的日志文件
     */
    private fun cleanupOldLogs() {
        if (maxRetentionDays <= 0) return

        val cutoffDate = LocalDate.now().minusDays(maxRetentionDays.toLong())

        logDirectory.listFiles()?.forEach { file ->
            if (file.isFile && file.name.startsWith(filePrefix) && file.name.endsWith(".log")) {
                try {
                    // 从文件名提取日期
                    val dateStr = file.name.removePrefix(filePrefix).removeSuffix(".log")
                    val fileDate = LocalDate.parse(dateStr, dateFormatter)

                    if (fileDate.isBefore(cutoffDate)) {
                        file.delete()
                    }
                } catch (_: DateTimeParseException) {
                    // 忽略无法解析日期的文件
                }
            }
        }
    }

    override fun isAvailable(): Boolean {
        return logDirectory.exists() && logDirectory.canWrite()
    }

    override fun close() {
        lock.write {
            closed = true
        }
    }

    override fun isClosed(): Boolean = closed
}
