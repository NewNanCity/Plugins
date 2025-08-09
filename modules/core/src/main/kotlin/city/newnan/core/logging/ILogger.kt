package city.newnan.core.logging

import city.newnan.core.utils.text.StringFormatter

interface ILogger {
    val stringFormatter: StringFormatter?

    /**
     * 记录日志条目
     * @param level 日志级别
     * @param message 消息
     * @param throwable 异常（可选）
     * @param context 上下文信息（可选）
     */
    fun log(level: LogLevel, message: String, throwable: Throwable? = null, context: Map<String, Any> = emptyMap())

    /**
     * 记录信息级别日志
     */
    fun info(message: String, vararg args: Any) {
        val formattedMessage = stringFormatter?.sprintfPlain(
            useProvider = true,
            formatText = message,
            params = args
        ) ?: message
        log(LogLevel.INFO, formattedMessage)
    }

    /**
     * 记录信息级别日志(别称)
     */
    fun log(message: String, vararg args: Any) {
        val formattedMessage = stringFormatter?.sprintfPlain(
            useProvider = true,
            formatText = message,
            params = args
        ) ?: message
        log(LogLevel.INFO, formattedMessage)
    }

    /**
     * 记录警告级别日志
     */
    fun warn(message: String, vararg args: Any) {
        val formattedMessage = stringFormatter?.sprintfPlain(
            useProvider = true,
            formatText = message,
            params = args
        ) ?: message
        log(LogLevel.WARN, formattedMessage)
    }

    /**
     * 记录警告级别日志（别名）
     */
    fun warning(message: String, vararg args: Any) {
        warn(message, *args)
    }

    /**
     * 记录错误级别日志
     */
    fun error(message: String, throwable: Throwable? = null, vararg args: Any) {
        val formattedMessage = stringFormatter?.sprintfPlain(
            useProvider = true,
            formatText = message,
            params = args
        ) ?: message
        log(LogLevel.ERROR, formattedMessage, throwable)
    }

    /**
     * 记录错误级别日志
     */
    fun severe(message: String, throwable: Throwable? = null, vararg args: Any) {
        error(message, throwable, *args)
    }

    /**
     * 记录调试级别日志
     */
    fun debug(message: String, vararg args: Any) {
        val formattedMessage = stringFormatter?.sprintfPlain(
            useProvider = true,
            formatText = message,
            params = args
        ) ?: message
        log(LogLevel.DEBUG, formattedMessage)
    }

    /**
     * 记录性能日志
     */
    fun performance(operation: String, timeMs: Long) {
        val context = mapOf(
            "operation" to operation,
            "timeMs" to timeMs,
            "type" to "performance"
        )
        log(LogLevel.PERFORMANCE, "Performance: $operation took ${timeMs}ms", context = context)
    }

    /**
     * 记录玩家操作日志
     */
    fun playerAction(playerName: String, action: String, details: String = "") {
        val message = if (details.isNotEmpty()) {
            "Player action: $playerName -> $action ($details)"
        } else {
            "Player action: $playerName -> $action"
        }
        val context = mapOf(
            "player" to playerName,
            "action" to action,
            "details" to details,
            "type" to "player_action"
        )
        log(LogLevel.PLAYER, message, context = context)
    }

    /**
     * 记录管理员操作日志
     */
    fun adminAction(adminName: String, action: String, target: String = "", details: String = "") {
        val message = when {
            target.isNotEmpty() && details.isNotEmpty() ->
                "Admin action: $adminName -> $action target:$target ($details)"
            target.isNotEmpty() ->
                "Admin action: $adminName -> $action target:$target"
            details.isNotEmpty() ->
                "Admin action: $adminName -> $action ($details)"
            else ->
                "Admin action: $adminName -> $action"
        }
        val context = mapOf(
            "admin" to adminName,
            "action" to action,
            "target" to target,
            "details" to details,
            "type" to "admin_action"
        )
        log(LogLevel.ADMIN, message, context = context)
    }

    /**
     * 刷新日志
     */
    fun flush() {}
}