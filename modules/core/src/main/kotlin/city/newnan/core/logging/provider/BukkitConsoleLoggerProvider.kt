package city.newnan.core.logging.provider

import city.newnan.core.logging.LogEntry
import city.newnan.core.logging.LogLevel
import city.newnan.core.utils.text.ComponentProcessor
import org.bukkit.plugin.Plugin
import java.util.logging.Level
import java.util.logging.Logger as JavaLogger

/**
 * Bukkit控制台日志输出提供者
 *
 * 将日志输出到Bukkit的控制台系统
 */
class BukkitConsoleLoggerProvider(
    plugin: Plugin,
    override var minimumLevel: LogLevel = LogLevel.INFO
) : LoggerProvider {

    override val name: String = "bukkit-console"

    private val logger: JavaLogger = plugin.logger

    override fun log(entry: LogEntry) {
        if (!supportsLevel(entry.level)) return

        val formattedMessage = ComponentProcessor.colorizeLegacy(entry.message)

        when (entry.level) {
            LogLevel.DEBUG -> {
                // 调试信息使用INFO级别，但添加DEBUG前缀
                logger.info("[DEBUG] $formattedMessage")
            }
            LogLevel.INFO, LogLevel.PERFORMANCE, LogLevel.PLAYER -> {
                logger.info(formattedMessage)
            }
            LogLevel.WARN, LogLevel.ADMIN -> {
                logger.warning(formattedMessage)
            }
            LogLevel.ERROR -> {
                if (entry.hasThrowable()) {
                    logger.log(Level.SEVERE, formattedMessage, entry.throwable)
                } else {
                    logger.severe(formattedMessage)
                }
            }
        }
    }

    override fun flush() {
        // Bukkit控制台不需要刷新，输出是立即的
    }

    override fun initialize() {
        // Bukkit控制台不需要初始化
    }

    override fun isAvailable(): Boolean {
        return true // Bukkit控制台总是可用的
    }

    override fun close() {
        // Bukkit控制台不需要关闭
    }

    override fun isClosed(): Boolean = false
}
