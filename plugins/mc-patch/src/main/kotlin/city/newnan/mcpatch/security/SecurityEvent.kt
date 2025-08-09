package city.newnan.mcpatch.security

import org.bukkit.entity.Player
import org.bukkit.Location
import java.time.LocalDateTime

/**
 * 安全事件类型枚举
 */
enum class SecurityEventType {
    WORLD_DOWNLOADER_DETECTED,
    CONTRABAND_FOUND,
    CRASH_ATTEMPT_BLOCKED
}

/**
 * 安全事件严重程度枚举
 */
enum class SecuritySeverity(val level: Int, val displayName: String) {
    INFO(1, "Info"),
    LOW(2, "Low"),
    MEDIUM(3, "Medium"),
    HIGH(4, "High"),
    CRITICAL(5, "Critical");

    fun isMoreSevereThan(other: SecuritySeverity): Boolean {
        return this.level > other.level
    }
}

/**
 * 安全事件数据类
 */
data class SecurityEvent(
    val type: SecurityEventType,
    val severity: SecuritySeverity,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val player: Player? = null,
    val location: Location? = null,
    val message: String,
    val details: Map<String, Any> = emptyMap(),
    val moduleName: String
) {
    /**
     * 获取格式化的事件描述
     */
    fun getFormattedDescription(): String {
        val playerInfo = player?.let { " [Player: ${it.name}]" } ?: ""
        val locationInfo = location?.let { " [Location: ${it.world?.name}(${it.blockX},${it.blockY},${it.blockZ})]" } ?: ""
        return "[$severity] $message$playerInfo$locationInfo"
    }

    /**
     * 获取事件的唯一标识符
     */
    fun getEventId(): String {
        return "${timestamp}_${type}_${player?.uniqueId ?: "system"}"
    }

    /**
     * 检查事件是否需要立即处理
     */
    fun requiresImmediateAction(): Boolean {
        return severity == SecuritySeverity.CRITICAL || severity == SecuritySeverity.HIGH
    }
}
