package city.newnan.core.logging.provider

import city.newnan.core.logging.LogLevel
import city.newnan.core.logging.LogEntry
import city.newnan.core.terminable.Terminable

/**
 * 日志输出提供者接口
 * 
 * 实现插件化的日志输出方式，支持多种输出目标：
 * - 控制台输出
 * - 文件输出
 * - 网络输出
 * - 数据库输出等
 */
interface LoggerProvider : Terminable {
    
    /**
     * 提供者名称，用于标识和配置
     */
    val name: String

    /**
     * 最小日志级别
     */
    var minimumLevel: LogLevel
    
    /**
     * 是否支持指定的日志级别
     * @param level 日志级别
     * @return 是否支持
     */
    fun supportsLevel(level: LogLevel): Boolean = level.shouldLog(minimumLevel)

    /**
     * 设置日志级别
     */
    infix fun setLogLevel(level: LogLevel): LoggerProvider = this.also {
        minimumLevel = level
    }

    /**
     * 输出日志条目
     * @param entry 日志条目
     */
    fun log(entry: LogEntry)
    
    /**
     * 刷新缓冲区（如果有）
     */
    fun flush() {}
    
    /**
     * 初始化提供者
     */
    fun initialize() {}
    
    /**
     * 检查提供者是否可用
     * @return 是否可用
     */
    fun isAvailable(): Boolean = true
}
