package city.newnan.feefly.i18n

/**
 * FeeFly 语言键常量类
 *
 * 遵循五层架构的 i18n Key 分类体系，使用 <%key%> 格式便于模板替换。
 *
 * 五层架构分类：
 * 1. 核心系统层 (Core System Layer) - 插件生命周期、配置管理、系统级错误
 * 2. 命令系统层 (Command System Layer) - 命令处理、参数验证、帮助系统
 * 3. 图形界面层 (GUI Layer) - GUI界面、按钮、提示信息
 * 4. 业务领域层 (Business Domain Layer) - 飞行管理、经济系统、事件处理
 * 5. 日志系统层 (Logging Layer) - 日志消息
 *
 * @author NewNanCity
 * @since 2.0.0
 */
object LanguageKeys {

    // ==================== 核心系统层 (Core System Layer) ====================
    object Core {

        object Config {
            const val RELOAD_FAILED = "<%feefly.config.reload_failed%>"
        }

        object Error {
            const val NO_PERMISSION = "<%core.error.no_permission%>"
            const val OPERATION_FAILED = "<%core.error.operation_failed%>"
        }
    }

    // ==================== 命令系统层 (Command System Layer) ====================
    object Commands {
        object Help {
            const val DESCRIPTION = "<%commands.help.description%>"
            const val QUERY = "<%commands.help.query%>"
        }

        object Fly {
            const val DESCRIPTION = "<%commands.fly.description%>"
            const val PLAYER_OR_SELECTOR = "<%commands.fly.player_or_selector%>"
            const val PLAYER_ONLY = "<%commands.fly.player_only%>"
            const val TOGGLED_FOR_PLAYER = "<%feefly.command.toggled_for_player%>"
            const val LOG_TOGGLED_SELF = "<%commands.fly.log_toggled_self%>"
            const val LOG_TOGGLED_BY_ADMIN = "<%commands.fly.log_toggled_by_admin%>"
        }

        object List {
            const val DESCRIPTION = "<%commands.list.description%>"
            const val FLYING_COUNT = "<%feefly.command.flying_count%>"
            const val DETAILED_INFO_HEADER = "<%feefly.command.detailed_info_header%>"
            const val PLAYER_DETAIL = "<%feefly.command.player_detail%>"
            const val NO_FLYING_PLAYERS = "<%feefly.command.no_flying_players%>"
        }

        object Reload {
            const val DESCRIPTION = "<%commands.reload.description%>"
            const val SUCCESS = "<%feefly.command.reload_success%>"
            const val FAILED = "<%commands.reload.failed%>"
            const val LOG_SUCCESS = "<%commands.reload.log_success%>"
            const val LOG_FAILED = "<%commands.reload.log_failed%>"
        }
    }

    // ==================== 图形界面层 (GUI Layer) ====================
    object Gui {
        // 当前版本暂无GUI功能，预留扩展
    }

    // ==================== 业务领域层 (Business Domain Layer) ====================
    object Business {
        object Economy {
            const val TARGET_ACCOUNT_SET = "<%feefly.economy.target_account_set%>"
        }

        object Fly {
            const val STARTED = "<%feefly.fly.started%>"
            const val ENDED = "<%feefly.fly.ended%>"
            const val NORMAL_STARTED = "<%feefly.fly.normal_started%>"
            const val NORMAL_ENDED = "<%feefly.fly.normal_ended%>"
            const val FREE_FLIGHT = "<%feefly.fly.free_flight%>"
            const val STATUS = "<%feefly.fly.status%>"
            const val LOW_BALANCE_TITLE = "<%feefly.fly.low_balance_title%>"
            const val LOW_BALANCE_SUBTITLE = "<%feefly.fly.low_balance_subtitle%>"
            const val STATE_RESTORED = "<%feefly.fly.state_restored%>"
            const val STATE_RESTORE_FAILED = "<%feefly.fly.state_restore_failed%>"
        }

        object Error {
            const val CREATIVE_MODE = "<%feefly.error.creative_mode%>"
            const val ALREADY_FLYING = "<%feefly.error.already_flying%>"
            const val ALREADY_IN_FEE_FLYING = "<%feefly.error.already_in_fee_flying%>"
            const val INSUFFICIENT_BALANCE = "<%feefly.error.insufficient_balance%>"
            const val COMMAND_COOLDOWN = "<%feefly.error.command_cooldown%>"
        }

        object Time {
            const val SECONDS = "<%feefly.time.seconds%>"
            const val MINUTES_SECONDS = "<%feefly.time.minutes_seconds%>"
            const val HOURS_MINUTES_SECONDS = "<%feefly.time.hours_minutes_seconds%>"
            const val DAYS_HOURS_MINUTES_SECONDS = "<%feefly.time.days_hours_minutes_seconds%>"
        }
    }

    // ==================== 日志系统层 (Logging Layer) ====================
    object Log {

        object Error {
            const val SERVICE_ERROR = "<%log.error.service_error%>"
        }
    }

    // ==================== 事件系统层 (Events Layer) ====================
    object Events {
        // 当前版本暂无专用事件，预留扩展
    }
}
