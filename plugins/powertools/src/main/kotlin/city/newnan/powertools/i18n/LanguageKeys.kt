package city.newnan.powertools.i18n

/**
 * PowerTools语言键常量类
 *
 * 遵循五层架构的 i18n Key 分类体系，使用 <%key%> 格式便于模板替换。
 *
 * 五层架构分类：
 * 1. 核心系统层 (Core System Layer) - 插件生命周期、配置管理、系统级错误
 * 2. 命令系统层 (Command System Layer) - 命令处理、参数验证、帮助系统
 * 3. 图形界面层 (GUI Layer) - GUI界面、按钮、提示信息
 * 4. 业务领域层 (Business Domain Layer) - 具体业务逻辑、事件处理
 * 5. 日志系统层 (Logging Layer) - 日志消息
 *
 * @author NewNanCity
 * @since 2.0.0
 */
object LanguageKeys {

    // ==================== 核心系统层 (Core System Layer) ====================
    object Core {
        object Plugin {
            const val RELOAD_FAILED = "<%core.plugin.reload_failed%>"
        }

        object Error {
            const val PLAYER_ONLY = "<%core.error.player_only%>"
        }
    }

    // ==================== 命令系统层 (Command System Layer) ====================
    object Commands {
        object Help {
            const val DESCRIPTION = "<%commands.help.description%>"
            const val HEADER = "<%commands.help.header%>"
            const val FOOTER = "<%commands.help.footer%>"
            const val QUERY = "<%commands.help.query%>"
        }

        object Main {
            const val DESCRIPTION = "<%commands.main.description%>"
        }

        object Reload {
            const val DESCRIPTION = "<%commands.reload.description%>"
            const val SUCCESS = "<%commands.reload.success%>"
            const val FAILED = "<%commands.reload.failed%>"
            const val LOG_SUCCESS = "<%commands.reload.log_success%>"
            const val LOG_FAILED = "<%commands.reload.log_failed%>"
        }

        object Skull {
            const val DESCRIPTION = "<%commands.skull.description%>"
            const val URL_DESCRIPTION = "<%commands.skull.url.description%>"
            const val PLAYER_DESCRIPTION = "<%commands.skull.player.description%>"
            const val URL_ARG = "<%commands.skull.url.arg%>"
            const val PLAYER_ARG = "<%commands.skull.player.arg%>"
            const val URL_OR_PLAYER_ARG = "<%commands.skull.url_or_player.arg%>"
            const val SUCCESS = "<%commands.skull.success%>"
            const val INVENTORY_FULL = "<%commands.skull.inventory_full%>"
            const val PLAYER_NOT_FOUND = "<%commands.skull.player_not_found%>"
            const val INVALID_URL = "<%commands.skull.invalid_url%>"
            const val FEATURE_DISABLED = "<%commands.skull.feature_disabled%>"
        }
    }

    // ==================== 日志系统层 (Logging Layer) ====================
    object Log {
        // 当前版本暂无专用日志，预留扩展
    }

    // ==================== 图形界面层 (GUI Layer) ====================
    object Gui {
        // 当前版本暂无GUI功能，预留扩展
    }

    // ==================== 事件系统层 (Events Layer) ====================
    object Events {
        // 当前版本暂无专用事件，预留扩展
    }
}
