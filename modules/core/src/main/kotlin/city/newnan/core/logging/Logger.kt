package city.newnan.core.logging

import city.newnan.core.terminable.Terminable
import city.newnan.core.logging.provider.LoggerProvider
import city.newnan.core.utils.text.ComponentParseMode
import city.newnan.core.utils.text.LanguageProvider
import city.newnan.core.utils.text.StringFormatter
import city.newnan.core.utils.text.toComponent
import city.newnan.core.utils.text.toLegacy
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 增强的日志工具类
 *
 * 基于插件化设计，提供完整的日志记录功能，包括：
 * - 标准日志记录（info、warn、error、debug）
 * - 插件化输出方式（控制台、文件、JSON等）
 * - 格式化输出能力
 * - 性能监控
 * - 玩家和管理员操作记录
 * - Terminable支持，自动资源管理
 *
 * 支持多种LoggerProvider：
 * - BukkitConsoleLoggerProvider：Bukkit控制台输出
 * - LogFileLoggerProvider：文件日志输出
 * - JsonlFileLoggerProvider：JSONL格式文件输出
 */
class Logger(
    val sourceName: String,
    override var stringFormatter: StringFormatter? = null
) : Terminable, ILogger {

    private val providers = CopyOnWriteArrayList<LoggerProvider>()

    init {
        // 如果没有提供StringFormatter，则创建一个默认的
        this.stringFormatter = stringFormatter ?: StringFormatter()
    }

    /**
     * 设置多语言服务提供者
     * @param languageProvider 多语言服务提供者，需实现LanguageProvider接口
     */
    infix fun setLanguageProvider(languageProvider: LanguageProvider?): Logger = this.also {
        if (stringFormatter == null) {
            stringFormatter = StringFormatter(languageProvider)
        } else {
            stringFormatter!!.languageProvider = languageProvider
        }
    }

    /**
     * 添加日志提供者
     * @param provider 日志提供者
     */
    fun addProvider(provider: LoggerProvider) {
        provider.initialize()
        providers.add(provider)
    }

    /**
     * 移除日志提供者
     * @param provider 日志提供者
     */
    fun removeProvider(provider: LoggerProvider) {
        if (providers.remove(provider)) {
            provider.close()
        }
    }

    /**
     * 获取所有日志提供者
     */
    fun getProviders(): List<LoggerProvider> = providers.toList()

    /**
     * 记录日志条目
     * @param level 日志级别
     * @param message 消息
     * @param throwable 异常（可选）
     * @param context 上下文信息（可选）
     */
    override fun log(level: LogLevel, message: String, throwable: Throwable?, context: Map<String, Any>) {
        val entry = LogEntry(
            level = level,
            message = message.toLegacy(ComponentParseMode.Auto),
            throwable = throwable,
            source = sourceName,
            context = context
        )

        providers.forEach { provider ->
            try {
                if (provider.isAvailable()) {
                    provider.log(entry)
                }
            } catch (e: Exception) {
                // 避免日志记录本身出错导致的循环
                System.err.println("Logger provider ${provider.name} failed: ${e.message}")
            }
        }
    }

    /**
     * 刷新所有日志提供者
     */
    override fun flush() {
        providers.forEach { provider ->
            try {
                provider.flush()
            } catch (e: Exception) {
                System.err.println("Failed to flush provider ${provider.name}: ${e.message}")
            }
        }
    }

    // Terminable 实现
    override fun close() {
        // Logger本身不应该产生日志输出，避免循环依赖
        providers.forEach { provider ->
            try {
                provider.close()
            } catch (e: Exception) {
                System.err.println("Failed to close provider ${provider.name}: ${e.message}")
            }
        }
        providers.clear()
    }

    override fun isClosed(): Boolean = providers.isEmpty()
}
