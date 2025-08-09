package city.newnan.core.logging.provider

import city.newnan.core.logging.LogEntry
import city.newnan.core.logging.LogLevel
import city.newnan.core.logging.formatter.JsonLogFormatter
import java.io.File
import java.io.FileWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

/**
 * JSONL格式文件日志输出提供者
 *
 * 将日志以JSON Lines格式输出到文件，每行一个JSON对象
 * 便于日志分析工具（如ELK Stack）处理
 */
class JsonlFileLoggerProvider(
    private val logDirectory: File,
    private val filePrefix: String = "",
    override var minimumLevel: LogLevel = LogLevel.INFO,
    private val maxRetentionDays: Int = 0
) : LoggerProvider {

    override val name: String = "jsonl-file"

    private val formatter = JsonLogFormatter()
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

    override fun supportsLevel(level: LogLevel): Boolean {
        return level.shouldLog(minimumLevel)
    }

    override fun log(entry: LogEntry) {
        if (closed || !supportsLevel(entry.level)) return

        lock.write {
            try {
                ensureLogFile()
                writeToFile(entry)
            } catch (e: Exception) {
                System.err.println("Failed to write to JSONL log file: ${e.message}")
            }
        }
    }

    private fun ensureLogFile() {
        val today = LocalDate.now()
        if (currentDate == null || !currentDate!!.isEqual(today)) {
            currentDate = today
            val dateStr = today.format(dateFormatter)
            currentLogFile = File(logDirectory, "$filePrefix$dateStr.jsonl")
        }
    }

    private fun writeToFile(entry: LogEntry) {
        val logFile = currentLogFile ?: return

        FileWriter(logFile, true).use { writer ->
            val jsonLine = formatter.format(entry)
            writer.write(jsonLine)
            writer.write("\n")
        }
    }

    /**
     * 清理旧的日志文件
     */
    private fun cleanupOldLogs() {
        if (maxRetentionDays <= 0) return

        val cutoffDate = LocalDate.now().minusDays(maxRetentionDays.toLong())

        logDirectory.listFiles()?.forEach { file ->
            if (file.isFile && file.name.startsWith(filePrefix) && file.name.endsWith(".jsonl")) {
                try {
                    val dateStr = file.name.removePrefix(filePrefix).removeSuffix(".jsonl")
                    val fileDate = LocalDate.parse(dateStr, dateFormatter)

                    if (fileDate.isBefore(cutoffDate)) {
                        file.delete()
                    }
                } catch (e: DateTimeParseException) {
                    // 忽略无法解析日期的文件
                }
            }
        }
    }

    override fun flush() {
        // 文件写入是立即的，不需要额外的flush操作
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
