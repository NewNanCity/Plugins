package city.newnan.core.logging

/**
 * 性能监控工具
 *
 * 提供代码执行时间监控功能，帮助识别性能瓶颈
 */
class PerformanceMonitor(internal val logger: Logger) {

    /**
     * 监控代码块执行时间
     *
     * @param operation 操作名称
     * @param block 要监控的代码块
     * @return 代码块的返回值
     */
    fun <T> monitor(operation: String, block: () -> T): T {
        val startTime = System.currentTimeMillis()
        return try {
            block()
        } finally {
            val endTime = System.currentTimeMillis()
            logger.performance(operation, endTime - startTime)
        }
    }

    /**
     * 监控异步操作
     *
     * @param operation 操作名称
     * @return Timer实例，用于手动停止计时
     */
    fun startTimer(operation: String): Timer {
        return Timer(operation, logger)
    }

    /**
     * 监控方法执行时间
     */
    fun <T> time(operation: String, block: () -> T): T {
        val startTime = System.nanoTime()
        return try {
            block()
        } finally {
            val endTime = System.nanoTime()
            val timeMs = (endTime - startTime) / 1_000_000
            logger.performance(operation, timeMs)
        }
    }
}

/**
 * 计时器
 *
 * 用于手动控制计时的开始和结束
 */
class Timer(private val operation: String, private val logger: Logger) {
    private val startTime = System.currentTimeMillis()
    @Volatile
    private var stopped = false

    /**
     * 停止计时并记录结果
     */
    fun stop() {
        if (!stopped) {
            logger.performance(operation, getElapsedTime())
            stopped = true
        }
    }

    /**
     * 获取已经过的时间（毫秒），但不停止计时
     */
    fun getElapsedTime(): Long {
        return System.currentTimeMillis() - startTime
    }
}

/**
 * 性能监控扩展函数
 */

/**
 * 为任意代码块添加性能监控
 */
fun <T> Logger.withPerformanceMonitoring(operation: String, block: () -> T): T {
    val monitor = PerformanceMonitor(this)
    return monitor.time(operation, block)
}

/**
 * 创建性能监控器
 */
fun Logger.createPerformanceMonitor(): PerformanceMonitor {
    return PerformanceMonitor(this)
}
