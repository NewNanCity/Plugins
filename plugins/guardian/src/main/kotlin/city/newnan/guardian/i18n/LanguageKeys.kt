package city.newnan.guardian.i18n

/**
 * Guardian 插件语言键常量类
 *
 * 遵循五层架构的 i18n Key 分类体系，使用 <%key%> 格式便于模板替换。
 *
 * 五层架构分类：
 * 1. 核心系统层 (Core System Layer) - 插件生命周期、配置管理、系统级错误
 * 2. 命令系统层 (Command System Layer) - 命令处理、参数验证、帮助系统
 * 3. 图形界面层 (GUI Layer) - GUI界面、按钮、提示信息
 * 4. 业务领域层 (Business Domain Layer) - 玩家管理、小镇管理、服务器管理
 * 5. 日志系统层 (Logging Layer) - 日志消息
 *
 * @author Guardian Team
 * @since 1.0.0
 */
object LanguageKeys {

    // ==================== 核心系统层 (Core System Layer) ====================
    object Core {
        object Plugin {
            const val RELOADING = "<%core.plugin.reloading%>"
            const val RELOADED = "<%core.plugin.reloaded%>"
        }

        object Error {
            const val PLAYER_ONLY = "<%core.error.player_only%>"
            const val OPERATION_FAILED = "<%core.error.operation_failed%>"
            const val PLAYER_NOT_FOUND = "<%core.error.player_not_found%>"
        }
    }

    // ==================== 命令系统层 (Command System Layer) ====================
    object Commands {
        object Common {
            const val NO_PERMISSION = "<%commands.common.no_permission%>"
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
            const val FAILED = "<%commands.reload.failed%>"
            const val LOG_SUCCESS = "<%commands.reload.log_success%>"
            const val LOG_FAILED = "<%commands.reload.log_failed%>"
        }



        object Judgemental {
            const val DESCRIPTION = "<%commands.judgemental.description%>"
            const val ENABLED = "<%commands.judgemental.enabled%>"
            const val DISABLED = "<%commands.judgemental.disabled%>"
            const val NOT_AUTHORIZED = "<%commands.judgemental.not_authorized%>"
            const val LOG_ENABLED = "<%commands.judgemental.log_enabled%>"
            const val LOG_DISABLED = "<%commands.judgemental.log_disabled%>"
            const val LOG_FAILED = "<%commands.judgemental.log_failed%>"
        }

        object JudgementList {
            const val DESCRIPTION = "<%commands.judgement_list.description%>"
            const val HEADER = "<%commands.judgement_list.header%>"
            const val EMPTY = "<%commands.judgement_list.empty%>"
            const val LOG_SUCCESS = "<%commands.judgement_list.log_success%>"
        }

        object JudgementAdd {
            const val DESCRIPTION = "<%commands.judgement_add.description%>"
            const val PLAYER = "<%commands.judgement_add.player%>"
            const val SUCCESS = "<%commands.judgement_add.success%>"
            const val ALREADY_EXISTS = "<%commands.judgement_add.already_exists%>"
            const val PLAYER_NOT_FOUND = "<%commands.judgement_add.player_not_found%>"
            const val LOG_SUCCESS = "<%commands.judgement_add.log_success%>"
            const val LOG_FAILED = "<%commands.judgement_add.log_failed%>"
        }

        object JudgementRemove {
            const val DESCRIPTION = "<%commands.judgement_remove.description%>"
            const val PLAYER = "<%commands.judgement_remove.player%>"
            const val SUCCESS = "<%commands.judgement_remove.success%>"
            const val NOT_EXISTS = "<%commands.judgement_remove.not_exists%>"
            const val PLAYER_NOT_FOUND = "<%commands.judgement_remove.player_not_found%>"
            const val LOG_SUCCESS = "<%commands.judgement_remove.log_success%>"
            const val LOG_FAILED = "<%commands.judgement_remove.log_failed%>"
        }

        object Town {
            const val DESCRIPTION = "<%commands.town.description%>"
            const val TOWN_NAME = "<%commands.town.town_name%>"
            const val NOT_FOUND = "<%commands.town.not_found%>"
            const val NO_TOWN = "<%commands.town.no_town%>"
            const val GUI_OPENING = "<%commands.town.gui_opening%>"
            const val GUI_FAILED = "<%commands.town.gui_failed%>"
            const val LOG_SUCCESS = "<%commands.town.log_success%>"
            const val LOG_FAILED = "<%commands.town.log_failed%>"
        }

        object Lookup {
            const val DESCRIPTION = "<%commands.lookup.description%>"
            const val PLAYER = "<%commands.lookup.player%>"
            const val HEADER = "<%commands.lookup.header%>"
            const val FOOTER = "<%commands.lookup.footer%>"
            const val PLAYER_NOT_FOUND = "<%commands.lookup.player_not_found%>"
            const val NOT_BOUND = "<%commands.lookup.not_bound%>"
            const val NAME_LABEL = "<%commands.lookup.name_label%>"
            const val UUID_LABEL = "<%commands.lookup.uuid_label%>"
            const val CASH_LABEL = "<%commands.lookup.cash_label%>"
            const val BED_SPAWN_LABEL = "<%commands.lookup.bed_spawn_label%>"
            const val BED_SPAWN_NONE = "<%commands.lookup.bed_spawn_none%>"
            const val FIRST_PLAYED_LABEL = "<%commands.lookup.first_played_label%>"
            const val LAST_PLAYED_LABEL = "<%commands.lookup.last_played_label%>"
            const val PLAY_TIME_LABEL = "<%commands.lookup.play_time_label%>"
            const val NEVER_PLAYED = "<%commands.lookup.never_played%>"
            const val QQ_LABEL = "<%commands.lookup.qq_label%>"
            const val GUILD_LABEL = "<%commands.lookup.guild_label%>"
            const val DISCORD_LABEL = "<%commands.lookup.discord_label%>"
            const val BAN_STATUS_NONE = "<%commands.lookup.ban_status_none%>"
            const val BAN_STATUS_TEMP = "<%commands.lookup.ban_status_temp%>"
            const val BAN_STATUS_PERM = "<%commands.lookup.ban_status_perm%>"
            const val TOWN_LABEL = "<%commands.lookup.town_label%>"
            const val TOWN_NONE = "<%commands.lookup.town_none%>"
            const val IP_LIST_LABEL = "<%commands.lookup.ip_list_label%>"
            const val STATUS_JOINED = "<%commands.lookup.status_joined%>"
            const val STATUS_NOT_JOINED = "<%commands.lookup.status_not_joined%>"
            const val INFO_NONE = "<%commands.lookup.info_none%>"
            const val LOG_SUCCESS = "<%commands.lookup.log_success%>"
            const val LOG_FAILED = "<%commands.lookup.log_failed%>"
        }
    }

    // ==================== 图形界面层 (GUI Layer) ====================
    object Gui {
        object Town {
            const val TITLE = "<%gui.town.title%>"
            const val LEVEL_CANCELLED = "<%gui.town.level_cancelled%>"
            const val LEADER_LABEL = "<%gui.town.leader_label%>"
            const val QQ_GROUP_LABEL = "<%gui.town.qq_group_label%>"
            const val PREVIOUS_PAGE = "<%gui.town.previous_page%>"
            const val NEXT_PAGE = "<%gui.town.next_page%>"
            const val CLOSE = "<%gui.town.close%>"
            const val QQ_LABEL = "<%gui.town.qq_label%>"
            const val BAN_LABEL = "<%gui.town.ban_label%>"
            const val BAN_YES = "<%gui.town.ban_yes%>"
            const val BAN_NO = "<%gui.town.ban_no%>"
            const val RIGHT_CLICK_REMOVE = "<%gui.town.right_click_remove%>"
            const val UNKNOWN = "<%gui.town.unknown%>"
            const val ADD_MEMBER_BUTTON = "<%gui.town.add_member_button%>"
            const val INPUT_PLAYER_NAME = "<%gui.town.input_player_name%>"
            const val OPERATION_CANCELLED = "<%gui.town.operation_cancelled%>"
            const val PLAYER_NOT_FOUND = "<%gui.town.player_not_found%>"
            const val PLAYER_ALREADY_IN_TOWN = "<%gui.town.player_already_in_town%>"
            const val PLAYER_ADDED_SUCCESS = "<%gui.town.player_added_success%>"
            const val PLAYER_REMOVED_SUCCESS = "<%gui.town.player_removed_success%>"
            const val INPUT_IN_PROGRESS = "<%gui.town.input_in_progress%>"
            const val MEMBER_LORE_EDITABLE = "<%gui.town.member_lore_editable%>"
            const val MEMBER_LORE_READONLY = "<%gui.town.member_lore_readonly%>"
        }
    }

    // ==================== 业务领域层 (Business Domain Layer) ====================
    object Player {
        object Judgemental {
            const val FAKE_LOGOUT_BROADCAST_MESSAGE = "<%player.judgemental.fake_logout_broadcast_message%>"
            const val FAKE_LOGIN_BROADCAST_MESSAGE = "<%player.judgemental.fake_login_broadcast_message%>"
        }
    }

    // ==================== 事件层 (Events Layer) ====================
    object Events {
        // 预留，将来用于守护系统事件消息键
    }

    // ==================== 日志系统层 (Logging Layer) ====================
    object Log {
        object Info {
            const val PLUGIN_LOADED = "<%log.info.plugin_loaded%>"
        }

        object Error {
            const val INITIALIZATION_FAILED = "<%log.error.initialization_failed%>"
        }
    }
}
