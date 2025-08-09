package city.newnan.core.logging

/**
 * 日志级别枚举
 */
enum class LogLevel(val displayName: String, val priority: Int) {
    /**
     * 调试级别 - 详细的调试信息
     */
    DEBUG("DEBUG", 0),
    
    /**
     * 信息级别 - 一般信息
     */
    INFO("INFO", 1),
    
    /**
     * 警告级别 - 警告信息
     */
    WARN("WARN", 2),
    
    /**
     * 错误级别 - 错误信息
     */
    ERROR("ERROR", 3),
    
    /**
     * 性能级别 - 性能统计信息
     */
    PERFORMANCE("PERF", 1),
    
    /**
     * 玩家操作级别 - 玩家行为记录
     */
    PLAYER("PLAYER", 1),
    
    /**
     * 管理员操作级别 - 管理员行为记录
     */
    ADMIN("ADMIN", 2);
    
    /**
     * 检查当前级别是否应该被记录
     * @param minimumLevel 最小记录级别
     * @return 是否应该记录
     */
    fun shouldLog(minimumLevel: LogLevel): Boolean {
        return this.priority >= minimumLevel.priority
    }
}
