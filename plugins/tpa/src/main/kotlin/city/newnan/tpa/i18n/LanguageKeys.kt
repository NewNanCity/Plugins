package city.newnan.tpa.i18n

/**
 * TPA插件语言键常量类
 *
 * 遵循五层架构的 i18n Key 分类体系，使用 <%key%> 格式便于模板替换。
 *
 * 五层架构分类：
 * 1. 核心系统层 (Core System Layer) - 插件生命周期、配置管理、系统级错误
 * 2. 命令系统层 (Command System Layer) - 命令处理、参数验证、帮助系统
 * 3. 图形界面层 (GUI Layer) - GUI界面、按钮、提示信息
 * 4. 业务领域层 (Business Domain Layer) - TPA传送逻辑、会话管理、屏蔽系统
 * 5. 日志系统层 (Logging Layer) - 日志消息
 *
 * @author AI Assistant
 * @since 2.0.0
 */
object LanguageKeys {

    // ==================== 核心系统层 (Core System Layer) ====================
    object Core {
        object Plugin {
            const val RELOAD_FAILED = "<%core.plugin.reload_failed%>"
        }

        object Error {
            const val OPERATION_FAILED = "<%core.error.operation_failed%>"
        }
    }

    // ==================== 命令系统层 (Command System Layer) ====================
    object Commands {
        object Common {
            const val PLAYER_ONLY = "<%commands.common.player_only%>"
        }

        object Main {
            const val DESCRIPTION = "<%commands.main.description%>"
        }

        object Help {
            const val DESCRIPTION = "<%commands.help.description%>"
            const val QUERY = "<%commands.help.query%>"
        }

        object Reload {
            const val DESCRIPTION = "<%commands.reload.description%>"
            const val SUCCESS = "<%commands.reload.success%>"
            const val FAILED = "<%commands.reload.failed%>"
            const val LOG_SUCCESS = "<%commands.reload.log_success%>"
            const val LOG_FAILED = "<%commands.reload.log_failed%>"
        }

        object TPAThere {
            const val DESCRIPTION = "<%commands.tpa_there.description%>"
            const val PLAYER_ARG = "<%commands.tpa_there.player_arg%>"
            const val SELF_REQUEST = "<%commands.tpa_there.self_request%>"
        }

        object TPAHere {
            const val DESCRIPTION = "<%commands.tpa_here.description%>"
            const val PLAYER_ARG = "<%commands.tpa_here.player_arg%>"
            const val SELF_REQUEST = "<%commands.tpa_here.self_request%>"
        }

        object Accept {
            const val DESCRIPTION = "<%commands.accept.description%>"
            const val ID_ARG = "<%commands.accept.id_arg%>"
            const val REQUEST_EXPIRED = "<%commands.accept.request_expired%>"
        }

        object Reject {
            const val DESCRIPTION = "<%commands.reject.description%>"
            const val ID_ARG = "<%commands.reject.id_arg%>"
            const val REQUEST_EXPIRED = "<%commands.reject.request_expired%>"
        }

        object Block {
            const val DESCRIPTION = "<%commands.block.description%>"
            const val PLAYER_ARG = "<%commands.block.player_arg%>"
            const val SELF_BLOCK = "<%commands.block.self_block%>"
            const val SUCCESS = "<%commands.block.success%>"
            const val HINT = "<%commands.block.hint%>"
            const val USAGE_HINT = "<%commands.block.usage_hint%>"
            const val ALREADY_BLOCKED = "<%commands.block.already_blocked%>"
        }

        object Unblock {
            const val DESCRIPTION = "<%commands.unblock.description%>"
            const val PLAYER_ARG = "<%commands.unblock.player_arg%>"
            const val SELF_UNBLOCK = "<%commands.unblock.self_unblock%>"
            const val SUCCESS = "<%commands.unblock.success%>"
            const val NOT_BLOCKED = "<%commands.unblock.not_blocked%>"
        }

        object TPA {
            const val COOLDOWN_ACTIVE = "<%commands.tpa.cooldown_active%>"
            const val TARGET_BLOCKED_YOU = "<%commands.tpa.target_blocked_you%>"
            const val REQUESTER_WORLD_BLOCKED = "<%commands.tpa.requester_world_blocked%>"
            const val TARGET_WORLD_BLOCKED = "<%commands.tpa.target_world_blocked%>"
        }

        object Response {
            const val NOT_YOUR_REQUEST = "<%commands.response.not_your_request%>"
            const val REQUEST_EXPIRED = "<%commands.response.request_expired%>"
            const val ACCEPTED = "<%commands.response.accepted%>"
            const val REJECTED = "<%commands.response.rejected%>"
        }
    }

    // ==================== 图形界面层 (GUI Layer) ====================
    object Gui {
        object Common {
            const val BACK = "<%gui.common.back%>"
            const val PREVIOUS_PAGE = "<%gui.common.previous_page%>"
            const val NEXT_PAGE = "<%gui.common.next_page%>"
        }

        object PlayerList {
            const val TITLE = "<%gui.player_list.title%>"
            const val PLAYER_ENTRY_NAME = "<%gui.player_list.player_entry_name%>"
            const val PLAYER_ENTRY_LORE = "<%gui.player_list.player_entry_lore%>"
            const val NO_PLAYERS_HINT_NAME = "<%gui.player_list.no_players_hint_name%>"
            const val NO_PLAYERS_HINT_LORE = "<%gui.player_list.no_players_hint_lore%>"
            const val SHOW_BLOCKED_PLAYERS_NAME = "<%gui.player_list.show_blocked_players_name%>"
            const val SHOW_BLOCKED_PLAYERS_LORE = "<%gui.player_list.show_blocked_players_lore%>"
        }

        object BlockList {
            const val TITLE = "<%gui.block_list.title%>"
            const val UNBLOCK_LORE = "<%gui.block_list.unblock_lore%>"
            const val EMPTY_LIST_NAME = "<%gui.block_list.empty_list_name%>"
        }
    }

    // ==================== 业务领域层 (Business Domain Layer) ====================
    object TPA {
        const val SEPARATOR = "<%tpa.separator%>"

        object Request {
            const val INCOMING_TPA = "<%tpa.request.incoming_tpa%>"
            const val INCOMING_TPAHERE = "<%tpa.request.incoming_tpahere%>"
        }

        object Teleport {
            const val COUNTDOWN_FROM = "<%tpa.teleport.countdown_from%>"
            const val COUNTDOWN_TO = "<%tpa.teleport.countdown_to%>"
            const val SUCCESS_FROM = "<%tpa.teleport.success_from%>"
            const val SUCCESS_TO = "<%tpa.teleport.success_to%>"
            const val CANCELLED_OFFLINE = "<%tpa.teleport.cancelled_offline%>"
            const val CANCELLED_WORLD = "<%tpa.teleport.cancelled_world%>"
        }

        object Session {
            const val ACCEPTED = "<%tpa.session.accepted%>"
            const val REJECTED = "<%tpa.session.rejected%>"
        }
    }

    // ==================== 日志系统层 (Logging Layer) ====================
    object Log {
        object Info {
            const val MODULE_INITIALIZED = "<%log.info.module_initialized%>"
        }

        object Error {
            const val CONFIG_ERROR = "<%log.error.config_error%>"
            const val TELEPORT_ERROR = "<%log.error.teleport_error%>"
        }
    }

    // ==================== 事件系统层 (Events Layer) ====================
    object Events {
        // 当前版本暂无专用事件，预留扩展
    }
}
