package city.newnan.mcpatch.i18n

/**
 * MCPatch 插件语言键常量类
 *
 * 遵循五层架构的 i18n Key 分类体系，使用 <%key%> 格式便于模板替换。
 *
 * 五层架构分类：
 * 1. 核心系统层 (Core System Layer) - 插件生命周期、配置管理、系统级错误
 * 2. 命令系统层 (Command System Layer) - 命令处理、参数验证、帮助系统
 * 3. 图形界面层 (GUI Layer) - GUI界面、按钮、提示信息（本插件暂无GUI）
 * 4. 业务领域层 (Business Domain Layer) - 安全模块、防护功能、检测逻辑
 * 5. 日志系统层 (Logging Layer) - 日志消息、安全事件记录
 *
 * @author NewNanCity
 * @since 2.0.0
 */
object LanguageKeys {

    // ==================== 核心系统层 (Core System Layer) ====================
    object Core {
        object Plugin {
            const val DISABLED = "<%core.plugin.disabled%>"
            const val RELOAD_FAILED = "<%core.plugin.reload_failed%>"
        }
    }

    // ==================== 命令系统层 (Command System Layer) ====================
    object Commands {
        // 暂时没有自定义命令，预留扩展
    }

    // ==================== 图形界面层 (GUI Layer) ====================
    object Gui {
        // 当前版本暂无GUI功能，预留扩展
    }

    // ==================== 业务领域层 (Business Domain Layer) ====================
    object Security {
        object AntiWorldDownload {
            const val PLAYER_KICKED = "<%security.anti_world_download.player_kicked%>"
            const val VIOLATION_LOGGED = "<%security.anti_world_download.violation_logged%>"
        }

        object Contraband {
            const val ITEM_REMOVED = "<%security.contraband.item_removed%>"
            const val ITEMS_FOUND = "<%security.contraband.items_found%>"
        }

        object AntiCrash {
            const val DISPENSER_BLOCKED = "<%security.anti_crash.dispenser_blocked%>"
        }
    }

    // ==================== 日志系统层 (Logging Layer) ====================
    object Log {
        object Info {
            const val MODULE_INITIALIZED = "<%log.info.module_initialized%>"
            const val SECURITY_EVENT_LOGGED = "<%log.info.security_event_logged%>"
        }
    }

    // ==================== 事件系统层 (Events Layer) ====================
    object Events {
        // 当前版本暂无专用事件，预留扩展
    }
}
