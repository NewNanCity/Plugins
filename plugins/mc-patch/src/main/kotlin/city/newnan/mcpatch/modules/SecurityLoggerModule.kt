package city.newnan.mcpatch.modules

import city.newnan.core.base.BaseModule
import city.newnan.mcpatch.MCPatchPlugin
import city.newnan.mcpatch.i18n.LanguageKeys
import city.newnan.mcpatch.security.SecurityEvent
import city.newnan.mcpatch.security.SecuritySeverity
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 安全日志模块
 *
 * 负责记录和管理所有安全相关事件，提供：
 * - 安全事件记录到文件
 * - 实时监控和通知
 * - 日志轮转和清理
 * - 安全事件查询
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class SecurityLoggerModule(
    moduleName: String,
    val plugin: MCPatchPlugin
) : BaseModule(moduleName, plugin) {

    companion object {
        private const val MAX_MEMORY_LOGS = 1000
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }

    // 内存中的安全事件队列（用于快速查询最近的事件）
    private val recentEvents = ConcurrentLinkedQueue<SecurityEvent>()
    
    // 日志文件
    private lateinit var logFile: File
    
    // 配置缓存
    private var fileLoggingEnabled: Boolean = true
    private var realTimeMonitoring: Boolean = true
    private var logFilePrefix: String = "MCPatch_Security_"

    init {
        init()
    }

    override fun onInit() {
        logger.info(LanguageKeys.Log.Info.MODULE_INITIALIZED, moduleName)
        
        // 初始化日志文件
        initializeLogFile()
        
        logger.info("SecurityLoggerModule initialized")
    }

    override fun onReload() {
        logger.info("SecurityLoggerModule reloading...")
        
        // 重新加载配置
        val config = plugin.getPluginConfig()
        fileLoggingEnabled = config.logging.fileLoggingEnabled
        realTimeMonitoring = config.logging.realTimeMonitoring
        logFilePrefix = config.logging.logFilePrefix
        
        // 重新初始化日志文件（如果前缀改变了）
        initializeLogFile()
        
        logger.debug("SecurityLoggerModule configuration reloaded")
    }

    /**
     * 初始化日志文件
     */
    private fun initializeLogFile() {
        val logsDir = File(plugin.dataFolder, "logs")
        if (!logsDir.exists()) {
            logsDir.mkdirs()
        }
        
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        logFile = File(logsDir, "${logFilePrefix}${timestamp}.log")
        
        if (!logFile.exists()) {
            logFile.createNewFile()
            // 写入日志文件头部
            logFile.appendText("# MCPatch Security Log - ${LocalDateTime.now().format(DATE_FORMATTER)}\n")
            logFile.appendText("# Format: [TIMESTAMP] [SEVERITY] [MODULE] MESSAGE\n\n")
        }
    }

    /**
     * 记录安全事件
     */
    fun logSecurityEvent(event: SecurityEvent) {
        try {
            // 添加到内存队列
            recentEvents.offer(event)
            
            // 保持内存队列大小
            while (recentEvents.size > MAX_MEMORY_LOGS) {
                recentEvents.poll()
            }
            
            // 写入文件日志
            if (fileLoggingEnabled) {
                writeToFile(event)
            }
            
            // 实时监控通知
            if (realTimeMonitoring && event.requiresImmediateAction()) {
                notifyAdministrators(event)
            }
            
            logger.debug(LanguageKeys.Log.Info.SECURITY_EVENT_LOGGED)
            
        } catch (e: Exception) {
            logger.error("Failed to log security event", e)
        }
    }

    /**
     * 写入文件日志
     */
    private fun writeToFile(event: SecurityEvent) {
        try {
            val logEntry = formatLogEntry(event)
            logFile.appendText("$logEntry\n")
        } catch (e: Exception) {
            logger.error("Failed to write security event to file", e)
        }
    }

    /**
     * 格式化日志条目
     */
    private fun formatLogEntry(event: SecurityEvent): String {
        val timestamp = event.timestamp.format(DATE_FORMATTER)
        val playerInfo = event.player?.let { "[${it.name}]" } ?: "[SYSTEM]"
        val locationInfo = event.location?.let { 
            "[${it.world?.name}(${it.blockX},${it.blockY},${it.blockZ})]" 
        } ?: ""
        
        return "[$timestamp] [${event.severity}] [${event.moduleName}] $playerInfo$locationInfo ${event.message}"
    }

    /**
     * 通知管理员
     */
    private fun notifyAdministrators(event: SecurityEvent) {
        val message = "§c[MCPatch] §f${event.getFormattedDescription()}"
        
        // 通知所有在线的管理员
        plugin.server.onlinePlayers
            .filter { it.hasPermission("mc-patch.admin") }
            .forEach { admin ->
                admin.sendMessage(message)
            }
        
        // 记录到控制台
        when (event.severity) {
            SecuritySeverity.CRITICAL -> logger.error(event.message)
            SecuritySeverity.HIGH -> logger.warn(event.message)
            else -> logger.info(event.message)
        }
    }

    /**
     * 获取最近的安全事件
     */
    fun getRecentEvents(limit: Int = 50): List<SecurityEvent> {
        return recentEvents.toList().takeLast(limit)
    }

    /**
     * 获取指定严重程度的事件
     */
    fun getEventsBySeverity(severity: SecuritySeverity, limit: Int = 50): List<SecurityEvent> {
        return recentEvents.filter { it.severity == severity }.takeLast(limit)
    }

    /**
     * 清理旧日志文件
     */
    fun cleanupOldLogs(daysToKeep: Int = 7) {
        try {
            val logsDir = File(plugin.dataFolder, "logs")
            if (!logsDir.exists()) return
            
            val cutoffTime = LocalDateTime.now().minusDays(daysToKeep.toLong())
            
            logsDir.listFiles()?.forEach { file ->
                if (file.name.startsWith(logFilePrefix) && file.name.endsWith(".log")) {
                    // 从文件名提取日期并检查是否过期
                    // 这里简化处理，实际可以更精确地解析文件名中的日期
                    if (file.lastModified() < cutoffTime.toEpochSecond(java.time.ZoneOffset.UTC) * 1000) {
                        file.delete()
                        logger.info("Deleted old log file: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to cleanup old logs", e)
        }
    }
}
