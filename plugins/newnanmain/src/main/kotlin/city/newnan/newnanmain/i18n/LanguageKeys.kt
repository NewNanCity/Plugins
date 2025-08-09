package city.newnan.newnanmain.i18n

/**
 * NewNanMain插件语言键常量
 * 遵循五层架构的i18n Key分类体系
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
            const val WORLD_NOT_FOUND = "<%core.error.world_not_found%>"
        }
    }

    // ==================== 命令系统层 (Command System Layer) ====================
    object Commands {
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

        object Prefix {
            const val SET_DESCRIPTION = "<%commands.prefix.set.description%>"
            const val REMOVE_DESCRIPTION = "<%commands.prefix.remove.description%>"
            const val ACTIVATE_DESCRIPTION = "<%commands.prefix.activate.description%>"
            const val DEACTIVATE_DESCRIPTION = "<%commands.prefix.deactivate.description%>"
            const val PLAYER_ARG = "<%commands.prefix.player.arg%>"
            const val NAMESPACE_ARG = "<%commands.prefix.namespace.arg%>"
            const val KEY_ARG = "<%commands.prefix.key.arg%>"
            const val SET_SUCCESS = "<%commands.prefix.set.success%>"
            const val REMOVE_SUCCESS = "<%commands.prefix.remove.success%>"
            const val ACTIVATE_SUCCESS = "<%commands.prefix.activate.success%>"
            const val DEACTIVATE_SUCCESS = "<%commands.prefix.deactivate.success%>"
            const val NOT_FOUND = "<%commands.prefix.not_found%>"
        }

        object Gui {
            const val DESCRIPTION = "<%commands.gui.description%>"
            const val OPENED = "<%commands.gui.opened%>"
        }
    }

    // ==================== 业务领域层 (Business Domain Layer) ====================
    object Prefix {
        const val REMOVED = "<%prefix.removed%>"
        const val ACTIVATED = "<%prefix.activated%>"
    }

    object Teleport {
        const val SUCCESS = "<%teleport.success%>"
        const val FAILED = "<%teleport.failed%>"
        const val POINT_NOT_FOUND = "<%teleport.point_not_found%>"
        const val NO_PERMISSION = "<%teleport.no_permission%>"
        const val COOLDOWN = "<%teleport.cooldown%>"
    }

    // ==================== 日志系统层 (Logging Layer) ====================
    object Log {
        object Info {
            const val PREFIX_UPDATED = "<%log.info.prefix_updated%>"
            const val TELEPORT_EXECUTED = "<%log.info.teleport_executed%>"
        }

        object Warning {
            const val VAULT_NOT_FOUND = "<%log.warning.vault_not_found%>"
        }

        object Error {
            const val VAULT_ERROR = "<%log.error.vault_error%>"
            const val PREFIX_ERROR = "<%log.error.prefix_error%>"
            const val TELEPORT_ERROR = "<%log.error.teleport_error%>"
        }
    }

    // ==================== 图形界面层 (GUI Layer) ====================
    object Gui {
        object Main {
            const val TITLE = "<%gui.main.title%>"
            const val TELEPORT = "<%gui.main.teleport%>"
            const val PREFIX = "<%gui.main.prefix%>"
            const val FLY = "<%gui.main.fly%>"
            const val TPA = "<%gui.main.tpa%>"
            const val BOOK = "<%gui.main.book%>"
            const val CREATE_AREA = "<%gui.main.create_area%>"
            const val CHARITY = "<%gui.main.charity%>"
            const val ACHIEVEMENT = "<%gui.main.achievement%>"
            const val TOWN = "<%gui.main.town%>"
            const val GUIDE = "<%gui.main.guide%>"
            const val INSPECT = "<%gui.main.inspect%>" // 熊服查询 CO I
            const val ADMIN = "<%gui.main.admin%>"
        }

        object Admin {
            const val TITLE = "<%gui.admin.title%>"
            const val TELEPORT_SYSTEM = "<%gui.admin.teleport_system%>"
            const val PREFIX_SYSTEM = "<%gui.admin.prefix_system%>"
            const val BOOK_SYSTEM = "<%gui.admin.book_system%>"
            const val CREATE_AREA = "<%gui.admin.create_area%>"
            const val RAIL_SYSTEM = "<%gui.admin.rail_system%>"
        }

        object Teleport {
            const val TITLE = "<%gui.teleport.title%>"
            const val NO_POINTS = "<%gui.teleport.no_points%>"
            const val MANAGE_TITLE = "<%gui.teleport.manage_title%>"
            const val BUILTIN_BED_NAME = "<%gui.teleport.builtin.bed.name%>"
            const val BUILTIN_BED_DESC = "<%gui.teleport.builtin.bed.desc%>"
            const val BUILTIN_HOME_NAME = "<%gui.teleport.builtin.home.name%>"
            const val BUILTIN_HOME_DESC = "<%gui.teleport.builtin.home.desc%>"
            const val BUILTIN_RESOURCE_NAME = "<%gui.teleport.builtin.resource.name%>"
            const val BUILTIN_RESOURCE_DESC = "<%gui.teleport.builtin.resource.desc%>"
            const val BUILTIN_RESOURCE_NETHER_NAME = "<%gui.teleport.builtin.resource_nether.name%>"
            const val BUILTIN_RESOURCE_NETHER_DESC = "<%gui.teleport.builtin.resource_nether.desc%>"
            const val WORLD_LINE = "<%gui.teleport.world_line%>" // {0}
            const val COORDS_LINE = "<%gui.teleport.coords_line%>" // {0} {1} {2}
            const val PERMISSION_LINE = "<%gui.teleport.permission_line%>" // {0}
            const val COOLDOWN_LINE = "<%gui.teleport.cooldown_line%>" // {0}
            const val ACTION_TELEPORT = "<%gui.teleport.action.teleport%>"
            const val ACTION_EDIT = "<%gui.teleport.action.edit%>"
            const val ACTION_DELETE = "<%gui.teleport.action.delete%>"
            const val ACTION_LEFT_CLICK_TELEPORT = "<%gui.teleport.action.left_click_teleport%>"
            const val ACTION_LEFT_CLICK = "<%gui.teleport.action.left_click%>"
            const val ACTION_RIGHT_CLICK_EDIT = "<%gui.teleport.action.right_click_edit%>"
            const val ACTION_SHIFT_RIGHT_CLICK_DELETE = "<%gui.teleport.action.shift_right_click_delete%>"
            const val ADD_POINT = "<%gui.teleport.add_point.title%>"
            const val ADD_POINT_DESC_1 = "<%gui.teleport.add_point.desc1%>"
            const val ADD_POINT_DESC_2 = "<%gui.teleport.add_point.desc2%>"
            const val ADD_POINT_DESC_ACTION = "<%gui.teleport.add_point.action%>"
            const val POINT_DESC = "<%gui.teleport.point_desc%>" // {0}
            const val DELETE_CONFIRM = "<%gui.teleport.delete_confirm%>" // {0}
            const val ADDED_SUCCESS = "<%gui.teleport.added_success%>" // {0}
            const val UPDATED_SUCCESS = "<%gui.teleport.updated_success%>" // {0}
            const val DELETED_SUCCESS = "<%gui.teleport.deleted_success%>" // {0}
            const val NAME_EXISTS = "<%gui.teleport.name_exists%>" // {0}
        }

        object Prefix {
            const val TITLE = "<%gui.prefix.title%>"
            const val NO_PREFIXES = "<%gui.prefix.no_prefixes%>"
            const val PLAYER_INFO_CURRENT = "<%gui.prefix.player.info.current%>" // {0}
            const val PLAYER_INFO_CURRENT_NONE = "<%gui.prefix.player.info.current_none%>"
            const val PLAYER_INFO_AVAILABLE_COUNT = "<%gui.prefix.player.info.available_count%>" // {0}
            const val PLAYER_INFO_SELECT_PROMPT = "<%gui.prefix.player.info.select_prompt%>"
            const val ITEM_NAMESPACE_LINE = "<%gui.prefix.item.namespace_line%>" // {0}
            const val ITEM_KEY_LINE = "<%gui.prefix.item.key_line%>" // {0}
            const val ITEM_STATUS_ENABLED = "<%gui.prefix.item.status_enabled%>"
            const val ITEM_STATUS_DISABLED = "<%gui.prefix.item.status_disabled%>"
            const val ITEM_ACTION_DISABLE = "<%gui.prefix.item.action.disable%>"
            const val ITEM_ACTION_ENABLE = "<%gui.prefix.item.action.enable%>"
            const val CLEAN_INVALID = "<%gui.prefix.clean_invalid%>" // {0}
            const val GLOBAL_MANAGE_TITLE = "<%gui.prefix.global.manage_title%>" // {0} namespace
            const val GLOBAL_NAMESPACE_MANAGE_TITLE = "<%gui.prefix.global.namespace_manage_title%>"
            const val GLOBAL_NAMESPACE_ITEM_COUNT = "<%gui.prefix.global.namespace.item_count%>" // {0}
            const val GLOBAL_NAMESPACE_ITEM_ACTION_VIEW = "<%gui.prefix.global.namespace.item.action.view%>"
            const val GLOBAL_NAMESPACE_ITEM_ACTION_DELETE = "<%gui.prefix.global.namespace.item.action.delete%>"
            const val GLOBAL_NAMESPACE_ADD = "<%gui.prefix.global.namespace.add.title%>"
            const val GLOBAL_NAMESPACE_ADD_DESC1 = "<%gui.prefix.global.namespace.add.desc1%>"
            const val GLOBAL_NAMESPACE_ADD_DESC2 = "<%gui.prefix.global.namespace.add.desc2%>"
            const val GLOBAL_NAMESPACE_ADD_ACTION = "<%gui.prefix.global.namespace.add.action%>"
            const val GLOBAL_NAMESPACE_DELETE_CONFIRM = "<%gui.prefix.global.namespace.delete_confirm%>" // {0}
            const val GLOBAL_PREFIX_ITEM_NAME = "<%gui.prefix.global.item.name%>" // {0}
            const val GLOBAL_PREFIX_ITEM_CONTENT = "<%gui.prefix.global.item.content%>" // {0}
            const val GLOBAL_PREFIX_ITEM_ACTION_EDIT = "<%gui.prefix.global.item.action.edit%>"
            const val GLOBAL_PREFIX_ITEM_ACTION_DELETE = "<%gui.prefix.global.item.action.delete%>"
            const val GLOBAL_PREFIX_ADD = "<%gui.prefix.global.add.title%>"
            const val GLOBAL_PREFIX_ADD_DESC1 = "<%gui.prefix.global.add.desc1%>" // {0} namespace
            const val GLOBAL_PREFIX_ADD_DESC2 = "<%gui.prefix.global.add.desc2%>"
            const val GLOBAL_PREFIX_ADD_ACTION = "<%gui.prefix.global.add.action%>"
            const val GLOBAL_PREFIX_DELETE_CONFIRM = "<%gui.prefix.global.delete_confirm%>" // {0}
            const val GLOBAL_PREFIX_ADDED = "<%gui.prefix.global.added%>" // {0}
            const val GLOBAL_PREFIX_UPDATED = "<%gui.prefix.global.updated%>" // {0}
            const val GLOBAL_PREFIX_DELETED = "<%gui.prefix.global.deleted%>" // {0}
            const val GLOBAL_NAMESPACE_CREATED = "<%gui.prefix.global.namespace_status.created%>" // {0}
            const val GLOBAL_NAMESPACE_EXISTS = "<%gui.prefix.global.namespace_status.exists%>" // {0}
            const val GLOBAL_NAMESPACE_DELETED = "<%gui.prefix.global.namespace_status.deleted%>" // {0}
        }

        object Common {
            const val BACK = "<%gui.common.back%>"
            const val NEXT = "<%gui.common.next%>"
            const val PREVIOUS = "<%gui.common.previous%>"
            const val PREVIOUS_PAGE = "<%gui.common.previous_page%>"
            const val NEXT_PAGE = "<%gui.common.next_page%>"
        }
    }

    // ==================== 事件层 (Events Layer) ====================
    object Events {
        // 预留，将来用于玩家交互/生命周期事件消息键
    }

    // ==================== 输入处理层 (Input Layer) ====================
    object Input {
        object Common {
            const val CHAT_INPUT_BUSY = "<%input.common.chat_input_busy%>"
            const val CANCELLED = "<%input.common.cancelled%>"
            const val UNKNOWN_COMMAND = "<%input.common.unknown_command%>"
        }

        object Teleport {
            const val HELP = "<%input.teleport.help%>"
            const val PROMPT = "<%input.teleport.prompt%>"
            const val EDIT_PROMPT = "<%input.teleport.edit_prompt%>"
            const val NAME_REQUIRED = "<%input.teleport.name_required%>"
            const val NAME_EXISTS = "<%input.teleport.name_exists%>"
            const val NAME_SET = "<%input.teleport.name_set%>"
            const val LOCATION_REQUIRED = "<%input.teleport.location_required%>"
            const val LOCATION_SET = "<%input.teleport.location_set%>"
            const val ICON_REQUIRED = "<%input.teleport.icon_required%>"
            const val ICON_SET = "<%input.teleport.icon_set%>"
            const val PERMISSION_SET = "<%input.teleport.permission_set%>"
            const val SAVE_FAILED = "<%input.teleport.save_failed%>"
        }

        object Prefix {
            const val HELP = "<%input.prefix.help%>"
            const val PROMPT = "<%input.prefix.prompt%>"
            const val EDIT_PROMPT = "<%input.prefix.edit_prompt%>"
            const val NAME_REQUIRED = "<%input.prefix.name_required%>"
            const val NAME_SET = "<%input.prefix.name_set%>"
            const val TEXT_REQUIRED = "<%input.prefix.text_required%>"
            const val TEXT_SET = "<%input.prefix.text_set%>"
            const val SAVE_FAILED = "<%input.prefix.save_failed%>"
        }

        object Namespace {
            const val HELP = "<%input.namespace.help%>"
            const val PROMPT = "<%input.namespace.prompt%>"
            const val EDIT_PROMPT = "<%input.namespace.edit_prompt%>"
            const val NAME_REQUIRED = "<%input.namespace.name_required%>"
            const val NAME_EXISTS = "<%input.namespace.name_exists%>"
            const val NAME_SET = "<%input.namespace.name_set%>"
            const val SAVE_FAILED = "<%input.namespace.save_failed%>"
        }
    }
}
