package city.newnan.gui.manager.logging

/**
 * GUI日志记录提供者接口
 *
 * 为GUI模块提供日志记录功能的抽象接口。
 * 实现此接口的类需要提供各种级别的日志记录方法。
 *
 * 支持的日志级别：
 * - ERROR: 错误信息，包含异常堆栈
 * - WARN: 警告信息
 * - INFO: 信息记录
 * - DEBUG: 调试信息
 *
 * @author NewNanCity
 * @since 2.0.0
 */
interface IGuiLoggerProvider {

    /**
     * 记录错误信息（带异常）
     *
     * @param message 错误消息
     * @param error 异常对象
     */
    fun error(message: String, error: Throwable)

    /**
     * 记录错误信息
     *
     * @param message 错误消息
     */
    fun error(message: String)

    /**
     * 记录警告信息
     *
     * @param message 警告消息
     */
    fun warn(message: String)

    /**
     * 记录普通信息
     *
     * @param message 信息消息
     */
    fun info(message: String)

    /**
     * 记录调试信息
     *
     * @param message 调试消息
     */
    fun debug(message: String)
}

fun wrapCoreLogger(logger: city.newnan.core.logging.ILogger): IGuiLoggerProvider {
    return object : IGuiLoggerProvider {
        override fun error(message: String, error: Throwable) {
            logger.error(message, error)
        }

        override fun error(message: String) {
            logger.error(message)
        }

        override fun warn(message: String) {
            logger.warn(message)
        }

        override fun info(message: String) {
            logger.info(message)
        }

        override fun debug(message: String) {
            logger.debug(message)
        }
    }
}