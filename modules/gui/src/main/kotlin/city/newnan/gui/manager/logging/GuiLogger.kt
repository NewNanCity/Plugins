package city.newnan.gui.manager.logging

import city.newnan.gui.component.IComponent
import city.newnan.gui.page.Page
import org.bukkit.entity.Player
import org.bukkit.event.Event
import java.time.LocalDateTime

/**
 * GUI日志记录器
 *
 * 为GUI模块提供详细的错误日志记录功能。
 * 基于IGuiLoggerProvider接口实现，提供GUI专用的日志记录方法。
 * 记录组件渲染错误、事件处理错误、生命周期错误等。
 *
 * 特性：
 * - 基于IGuiloggerProvider接口的灵活实现
 * - 分类错误日志记录
 * - 详细的上下文信息
 * - 错误统计和分析
 * - 支持国际化和多种输出格式
 */
class GuiLogger(private val loggerProvider: IGuiLoggerProvider) {

    // 错误统计
    private val errorStats = mutableMapOf<String, Int>()
    private val warningStats = mutableMapOf<String, Int>()

    /**
     * 错误类型
     */
    enum class ErrorType {
        COMPONENT_RENDER,
        EVENT_HANDLING,
        LIFECYCLE,
        SCHEDULER,
        ITEM_CREATION,
        SESSION_MANAGEMENT,
        INVENTORY_OPERATION,
        UNKNOWN
    }

    /**
     * 格式化context信息为字符串
     */
    private fun formatContext(context: Map<String, Any>): String {
        if (context.isEmpty()) return ""

        return context.entries.joinToString(
            prefix = " [",
            postfix = "]",
            separator = ", "
        ) { (key, value) ->
            "$key=$value"
        }
    }

    /**
     * 记录组件渲染错误
     */
    fun logComponentRenderError(
        component: IComponent<*>,
        slot: Int,
        error: Throwable,
        context: Map<String, Any> = emptyMap()
    ) {
        val errorKey = "COMPONENT_RENDER_${component::class.simpleName}"
        errorStats[errorKey] = errorStats.getOrDefault(errorKey, 0) + 1

        val contextMap = buildMap {
            put("component_type", component::class.simpleName ?: "Unknown")
            put("page_title", component.page.title)
            put("player_name", component.page.player.name)
            put("slot", slot)
            put("error_type", error::class.simpleName ?: "Unknown")
            put("error_category", ErrorType.COMPONENT_RENDER.name)
            putAll(context)
        }

        val message = "Component render error: component=${component::class.simpleName}, page=${component.page.title}, player=${component.page.player.name}, slot=$slot${formatContext(contextMap)}"

        loggerProvider.error(message, error)
    }

    /**
     * 记录页面事件处理错误
     */
    fun logPageEventError(
        event: Event,
        error: Throwable,
        eventType: String = "UNKNOWN",
        context: Map<String, Any> = emptyMap()
    ) {
        val errorKey = "PAGE_EVENT_${eventType}"
        errorStats[errorKey] = errorStats.getOrDefault(errorKey, 0) + 1

        val contextMap = buildMap {
            put("event_type", event::class.simpleName ?: "Unknown")
            put("event_handler", eventType)
            put("error_type", error::class.simpleName ?: "Unknown")
            put("error_category", ErrorType.EVENT_HANDLING.name)
            putAll(context)
        }

        val message = "Page event error: event=${event::class.simpleName}, handler=$eventType${formatContext(contextMap)}"

        loggerProvider.error(message, error)
    }

    /**
     * 记录组件事件处理错误
     */
    fun logEventHandlingError(
        component: IComponent<*>,
        event: Event,
        error: Throwable,
        eventType: String = "UNKNOWN",
        context: Map<String, Any> = emptyMap()
    ) {
        val errorKey = "EVENT_HANDLING_${eventType}"
        errorStats[errorKey] = errorStats.getOrDefault(errorKey, 0) + 1

        val contextMap = buildMap {
            put("component_type", component::class.simpleName ?: "Unknown")
            put("page_title", component.page.title)
            put("player_name", component.page.player.name)
            put("event_type", event::class.simpleName ?: "Unknown")
            put("event_handler", eventType)
            put("error_type", error::class.simpleName ?: "Unknown")
            put("error_category", ErrorType.EVENT_HANDLING.name)
            putAll(context)
        }

        val message = "Event handling error: component=${component::class.simpleName}, event=${event::class.simpleName}, handler=$eventType${formatContext(contextMap)}"

        loggerProvider.error(message, error)
    }

    /**
     * 记录组件关闭错误
     */
    fun logComponentCloseError(
        component: IComponent<*>,
        error: Throwable,
        context: Map<String, Any> = emptyMap()
    ) {
        val errorKey = "COMPONENT_CLOSE_${component::class.simpleName}"
        errorStats[errorKey] = errorStats.getOrDefault(errorKey, 0) + 1

        val contextMap = buildMap {
            put("component_type", component::class.simpleName ?: "Unknown")
            put("page_title", component.page.title)
            put("player_name", component.page.player.name)
            put("error_type", error::class.simpleName ?: "Unknown")
            put("error_category", ErrorType.LIFECYCLE.name)
            putAll(context)
        }

        val message = "Component close error: component=${component::class.simpleName}, page=${component.page.title}, player=${component.page.player.name}${formatContext(contextMap)}"

        loggerProvider.error(message, error)
    }

    /**
     * 记录页面生命周期错误
     */
    fun logPageLifecycleError(
        page: Page,
        operation: String,
        error: Throwable,
        context: Map<String, Any> = emptyMap()
    ) {
        val errorKey = "PAGE_LIFECYCLE_${operation}"
        errorStats[errorKey] = errorStats.getOrDefault(errorKey, 0) + 1

        val contextMap = buildMap {
            put("page_type", page::class.simpleName ?: "Unknown")
            put("page_title", page.title)
            put("player_name", page.player.name)
            put("operation", operation)
            put("error_type", error::class.simpleName ?: "Unknown")
            put("error_category", ErrorType.LIFECYCLE.name)
            putAll(context)
        }

        val message = "Page lifecycle error: page=${page::class.simpleName}, operation=$operation, player=${page.player.name}${formatContext(contextMap)}"

        loggerProvider.error(message, error)
    }

    /**
     * 记录调度器错误
     */
    fun logSchedulerError(
        player: Player? = null,
        taskType: String,
        error: Throwable,
        context: Map<String, Any> = emptyMap()
    ) {
        val errorKey = "SCHEDULER_${taskType}"
        errorStats[errorKey] = errorStats.getOrDefault(errorKey, 0) + 1

        val contextMap = buildMap {
            put("player_name", player?.name ?: "null")
            put("task_type", taskType)
            put("error_type", error::class.simpleName ?: "Unknown")
            put("error_category", ErrorType.SCHEDULER.name)
            putAll(context)
        }

        val message = when (player) {
            null -> "Scheduler error: task=$taskType${formatContext(contextMap)}"
            else -> "Scheduler error: player=${player.name}, task=$taskType${formatContext(contextMap)}"
        }

        loggerProvider.error(message, error)
    }

    /**
     * 记录物品创建错误
     */
    fun logItemCreationError(
        player: Player,
        itemType: String,
        error: Throwable,
        context: Map<String, Any> = emptyMap()
    ) {
        val errorKey = "ITEM_CREATION_${itemType}"
        errorStats[errorKey] = errorStats.getOrDefault(errorKey, 0) + 1

        val contextMap = buildMap {
            put("player_name", player.name)
            put("item_type", itemType)
            put("error_type", error::class.simpleName ?: "Unknown")
            put("error_category", ErrorType.ITEM_CREATION.name)
            putAll(context)
        }

        val message = "Item creation error: player=${player.name}, item=$itemType${formatContext(contextMap)}"

        loggerProvider.error(message, error)
    }

    /**
     * 记录会话管理错误
     */
    fun logSessionError(
        player: Player,
        operation: String,
        error: Throwable,
        context: Map<String, Any> = emptyMap()
    ) {
        val errorKey = "SESSION_${operation}"
        errorStats[errorKey] = errorStats.getOrDefault(errorKey, 0) + 1

        val contextMap = buildMap {
            put("player_name", player.name)
            put("operation", operation)
            put("error_type", error::class.simpleName ?: "Unknown")
            put("error_category", ErrorType.SESSION_MANAGEMENT.name)
            putAll(context)
        }

        val message = "Session management error: player=${player.name}, operation=$operation${formatContext(contextMap)}"

        loggerProvider.error(message, error)
    }

    /**
     * 记录库存操作错误
     */
    fun logInventoryError(
        player: Player,
        operation: String,
        error: Throwable,
        context: Map<String, Any> = emptyMap()
    ) {
        val errorKey = "INVENTORY_${operation}"
        errorStats[errorKey] = errorStats.getOrDefault(errorKey, 0) + 1

        val contextMap = buildMap {
            put("player_name", player.name)
            put("operation", operation)
            put("error_type", error::class.simpleName ?: "Unknown")
            put("error_category", ErrorType.INVENTORY_OPERATION.name)
            putAll(context)
        }

        val message = "Inventory operation error: player=${player.name}, operation=$operation${formatContext(contextMap)}"

        loggerProvider.error(message, error)
    }

    /**
     * 记录一般错误
     */
    fun logError(
        errorType: ErrorType,
        message: String,
        error: Throwable? = null,
        context: Map<String, Any> = emptyMap()
    ) {
        val errorKey = "GENERAL_${errorType.name}"
        errorStats[errorKey] = errorStats.getOrDefault(errorKey, 0) + 1

        val contextMap = buildMap {
            put("error_type", errorType.name)
            put("error_category", "GENERAL")
            putAll(context)
        }

        val logMessage = "GUI error: type=${errorType.name}, message=$message${formatContext(contextMap)}"

        if (error != null) {
            loggerProvider.error(logMessage, error)
        } else {
            loggerProvider.error(logMessage)
        }
    }

    /**
     * 记录警告
     */
    fun logWarning(
        message: String,
        context: Map<String, Any> = emptyMap()
    ) {
        val warningKey = "WARNING_GENERAL"
        warningStats[warningKey] = warningStats.getOrDefault(warningKey, 0) + 1

        val contextMap = buildMap {
            put("warning_category", "GENERAL")
            putAll(context)
        }

        val logMessage = "GUI warning: $message${formatContext(contextMap)}"

        loggerProvider.warn(logMessage)
    }

    /**
     * 记录信息
     */
    fun logInfo(
        message: String,
        context: Map<String, Any> = emptyMap()
    ) {
        val contextMap = buildMap {
            put("info_category", "GENERAL")
            putAll(context)
        }

        val logMessage = "GUI info: $message${formatContext(contextMap)}"

        loggerProvider.info(logMessage)
    }

    /**
     * 记录调试信息
     */
    fun logDebug(
        message: String,
        context: Map<String, Any> = emptyMap()
    ) {
        val contextMap = buildMap {
            put("debug_category", "GENERAL")
            putAll(context)
        }

        val logMessage = "GUI debug: $message${formatContext(contextMap)}"

        loggerProvider.debug(logMessage)
    }

    /**
     * 记录组件覆盖信息
     */
    fun logComponentOverride(
        page: Page,
        slot: Int,
        previousComponent: IComponent<*>,
        newComponent: IComponent<*>
    ) {
        val contextMap = buildMap {
            put("page_title", page.title)
            put("player_name", page.player.name)
            put("slot", slot)
            put("previous_component", previousComponent::class.simpleName ?: "Unknown")
            put("new_component", newComponent::class.simpleName ?: "Unknown")
            put("debug_category", "COMPONENT_OVERRIDE")
        }

        val message = "Component override: page=${page.title}, slot=$slot, " +
                "previous=${previousComponent::class.simpleName}, " +
                "new=${newComponent::class.simpleName}${formatContext(contextMap)}"

        loggerProvider.debug(message)
    }

    /**
     * 记录槽位跳过信息
     */
    fun logSlotSkipped(
        component: IComponent<*>,
        slot: Int,
        ownerComponent: IComponent<*>,
        reason: String
    ) {
        val contextMap = buildMap {
            put("component_type", component::class.simpleName ?: "Unknown")
            put("page_title", component.page.title)
            put("player_name", component.page.player.name)
            put("slot", slot)
            put("owner_component", ownerComponent::class.simpleName ?: "Unknown")
            put("skip_reason", reason)
            put("debug_category", "SLOT_SKIP")
        }

        val message = "Slot skipped: component=${component::class.simpleName}, slot=$slot, " +
                "owner=${ownerComponent::class.simpleName}, reason=$reason${formatContext(contextMap)}"

        loggerProvider.debug(message)
    }

    /**
     * 记录页面渲染错误
     */
    fun logPageRenderError(
        page: Page,
        slot: Int? = null,
        error: Throwable,
        context: Map<String, Any> = emptyMap()
    ) {
        val errorKey = "PAGE_RENDER_${page::class.simpleName}"
        errorStats[errorKey] = errorStats.getOrDefault(errorKey, 0) + 1

        val contextMap = buildMap {
            put("page_type", page::class.simpleName ?: "Unknown")
            put("page_title", page.title)
            put("player_name", page.player.name)
            if (slot != null) {
                put("slot", slot)
            }
            put("error_type", error::class.simpleName ?: "Unknown")
            put("error_category", ErrorType.COMPONENT_RENDER.name)
            putAll(context)
        }

        val message = buildString {
            append("Page render error: page=${page::class.simpleName}, player=${page.player.name}")
            if (slot != null) {
                append(", slot=$slot")
            }
            append(formatContext(contextMap))
        }

        loggerProvider.error(message, error)
    }

    /**
     * 获取错误统计
     */
    fun getErrorStats(): Map<String, Int> {
        return errorStats.toMap()
    }

    /**
     * 获取警告统计
     */
    fun getWarningStats(): Map<String, Int> {
        return warningStats.toMap()
    }

    /**
     * 清空统计
     */
    fun clearStats() {
        errorStats.clear()
        warningStats.clear()
    }

    /**
     * 生成错误报告
     */
    fun generateErrorReport(): String {
        return buildString {
            appendLine("=== GUI Error Statistics Report ===")
            appendLine("Generated at: ${LocalDateTime.now()}")
            appendLine()

            appendLine("Error Statistics:")
            if (errorStats.isEmpty()) {
                appendLine("  No errors recorded")
            } else {
                errorStats.entries.sortedByDescending { it.value }.forEach { (type, count) ->
                    appendLine("  $type: $count occurrences")
                }
            }
            appendLine()

            appendLine("Warning Statistics:")
            if (warningStats.isEmpty()) {
                appendLine("  No warnings recorded")
            } else {
                warningStats.entries.sortedByDescending { it.value }.forEach { (type, count) ->
                    appendLine("  $type: $count occurrences")
                }
            }

            appendLine("====================================")
        }
    }

    /**
     * 创建GuiLogger实例的工厂方法
     */
    companion object {
        fun create(loggerProvider: IGuiLoggerProvider): GuiLogger {
            return GuiLogger(loggerProvider)
        }
    }
}
