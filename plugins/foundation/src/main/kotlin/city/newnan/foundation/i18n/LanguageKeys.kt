package city.newnan.foundation.i18n

/**
 * Foundation 语言键常量类
 *
 * 遵循五层架构的 i18n Key 分类体系，使用 <%key%> 格式便于模板替换。
 *
 * 五层架构分类：
 * 1. 核心系统层 (Core System Layer) - 插件生命周期、配置管理、系统级错误
 * 2. 命令系统层 (Command System Layer) - 命令处理、参数验证、帮助系统
 * 3. 图形界面层 (GUI Layer) - GUI界面、按钮、提示信息
 * 4. 业务领域层 (Business Domain Layer) - 基金管理、转账系统、事件处理
 * 5. 日志系统层 (Logging Layer) - 日志消息
 *
 * @author NewNanCity
 * @since 2.0.0
 */
object LanguageKeys {

    // ==================== 核心系统层 (Core System Layer) ====================
    object Core {
        object Plugin {
            const val TARGET_ACCOUNT_SET = "<%foundation.plugin.target_account_set%>"
            const val TARGET_ACCOUNT_NOT_SET = "<%foundation.plugin.target_account_not_set%>"
        }

        object Config {
            const val RELOAD_FAILED = "<%foundation.config.reload_failed%>"
        }

        object Error {
            const val NO_PERMISSION = "<%error.no_permission%>"
            const val FAILED = "<%error.operation_failed%>"
        }
    }

    // ==================== 错误消息层 (Error Messages Layer) ====================
    object Error {
        // 目前暂无专用错误消息，预留扩展
    }

    // ==================== 命令系统层 (Command System Layer) ====================
    object Commands {
        object Common {

        }

        object Help {
            const val DESCRIPTION = "<%commands.help.description%>"
            const val QUERY = "<%commands.help.query%>"
        }

        object Main {
            const val DESCRIPTION = "<%commands.main.description%>"
        }

        object Reload {
            const val DESCRIPTION = "<%commands.reload.description%>"
            const val SUCCESS = "<%commands.reload.success%>"
        }

        object Query {
            const val DESCRIPTION = "<%commands.query.description%>"
            const val PLAYER = "<%commands.query.player%>"
            const val HEADER = "<%commands.query.header%>"
            const val NO_RECORD = "<%commands.query.no_record%>"
            const val ACTIVE_DONATIONS = "<%commands.query.active_donations%>"
            const val PASSIVE_DONATIONS = "<%commands.query.passive_donations%>"
            const val TOTAL_DONATIONS = "<%commands.query.total_donations%>"
            const val LOG_FAILED = "<%commands.query.log_failed%>"
        }

        object Donate {
            const val DESCRIPTION = "<%commands.donate.description%>"
            const val AMOUNT = "<%commands.donate.amount%>"
            const val PLAYER_OR_SELECTOR = "<%commands.donate.player_or_selector%>"
            const val PLAYER_ONLY = "<%commands.donate.player_only%>"
            const val INVALID_AMOUNT = "<%commands.donate.invalid_amount%>"
            const val NO_TARGET_ACCOUNT = "<%commands.donate.no_target_account%>"
            const val INSUFFICIENT_BALANCE = "<%commands.donate.insufficient_balance%>"
            const val WITHDRAW_FAILED = "<%commands.donate.withdraw_failed%>"
            const val DEPOSIT_FAILED = "<%commands.donate.deposit_failed%>"
            const val SUCCESS = "<%commands.donate.success%>"
        }

        object Balance {
            const val DESCRIPTION = "<%commands.balance.description%>"
            const val HEADER = "<%commands.balance.header%>"
            const val ACCOUNT = "<%commands.balance.account%>"
            const val AMOUNT = "<%commands.balance.amount%>"
            const val ACTIVE_DONATIONS = "<%commands.balance.active_donations%>"
            const val PASSIVE_DONATIONS = "<%commands.balance.passive_donations%>"
            const val LOG_FAILED = "<%commands.balance.log_failed%>"
            const val FOUNDATION_ACCOUNT_NOT_SET = "<%commands.balance.foundation_account_not_set%>"
        }

        object Allocate {
            const val DESCRIPTION = "<%commands.allocate.description%>"
            const val PLAYER = "<%commands.allocate.player%>"
            const val AMOUNT = "<%commands.allocate.amount%>"
            const val REASON = "<%commands.allocate.reason%>"
            const val INVALID_AMOUNT = "<%commands.allocate.invalid_amount%>"
            const val INVALID_REASON = "<%commands.allocate.invalid_reason%>"
            const val NO_TARGET_ACCOUNT = "<%commands.allocate.no_target_account%>"
            const val INSUFFICIENT_FOUNDATION_BALANCE = "<%commands.allocate.insufficient_foundation_balance%>"
            const val FAILED = "<%commands.allocate.failed%>"
            const val SUCCESS = "<%commands.allocate.success%>"
            const val LOG_SUCCESS = "<%commands.allocate.log_success%>"
        }

        object Top {
            const val DESCRIPTION = "<%commands.top.description%>"
            const val HEADER = "<%commands.top.header%>"
            const val ENTRY = "<%commands.top.entry%>"
            const val PAGE = "<%commands.top.page%>"
            const val Console = "<%commands.top.console%>"
            const val INVALID_PAGE = "<%commands.top.invalid_page%>"
            const val OUT_OF_RANGE = "<%commands.top.out_of_range%>"
            const val LOG_FAILED = "<%commands.top.log_failed%>"
        }

        object Error {
            const val OPERATION_FAILED = "<%commands.error.operation_failed%>"
        }
    }

    // ==================== 图形界面层 (GUI Layer) ====================
    object Gui {
        object Common {
            const val BACK = "<%gui.common.back%>"
            const val BACK_DESC = "<%gui.common.back_desc%>"
            const val NEXT_PAGE = "<%gui.common.next_page%>"
            const val PREVIOUS_PAGE = "<%gui.common.previous_page%>"

            // Lore constants
            const val BACK_LORE = "<%gui.common.back_lore%>"
        }

        object Top {
            const val TITLE = "<%gui.top.title%>"
            const val RANK = "<%gui.top.rank%>"
            const val RANK_SELF = "<%gui.top.rank_self%>"
            const val DETAILED_STATS_HEADER = "<%gui.top.detailed_stats_header%>"
            const val PLAYER_ACTIVE = "<%gui.top.player_active%>"
            const val PLAYER_DETAILS_HEADER = "<%gui.top.player_details_header%>"
            const val PLAYER_PASSIVE = "<%gui.top.player_passive%>"
            const val PLAYER_TOTAL = "<%gui.top.player_total%>"
            const val STATS = "<%gui.top.stats%>"
            const val STATS_TOTAL_PLAYERS = "<%gui.top.stats_total_players%>"
            const val STATS_TOTAL_ACTIVE = "<%gui.top.stats_total_active%>"
            const val STATS_TOTAL_PASSIVE = "<%gui.top.stats_total_passive%>"
            const val STATS_GRAND_TOTAL = "<%gui.top.stats_grand_total%>"
            const val STATS_AVERAGE = "<%gui.top.stats_average%>"
            const val OPERATION_FAILED = "<%gui.top.operation_failed%>"

            // Lore constants for multi-line tooltips
            const val RANK_LORE = "<%gui.top.rank_lore%>"
            const val PAGE_LORE = "<%gui.top.page_lore%>"
            const val STATS_LORE = "<%gui.top.stats_lore%>"
        }
    }

    // ==================== 业务领域层 (Business Domain Layer) ====================
    object Business {
        // 目前暂无专用业务领域，预留扩展
    }

    // ==================== 日志系统层 (Logging Layer) ====================
    object Log {
        // 目前暂无专用日志，预留扩展
    }

    // ==================== 事件系统层 (Events Layer) ====================
    object Events {
        // 目前暂无专用事件，预留扩展
    }
}
