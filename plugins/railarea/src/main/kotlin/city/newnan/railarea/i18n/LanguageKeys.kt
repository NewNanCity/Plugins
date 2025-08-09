package city.newnan.railarea.i18n

/**
 * RailArea插件语言键
 *
 * 使用五层架构分类和<%key%>格式统一管理所有语言键
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
    }

    /**
     * GUI相关语言键
     */
    object Gui {

        /**
         * 通用GUI元素
         */
        object Common {
            const val BACK = "<%gui.common.back%>"
            const val PREVIOUS_PAGE = "<%gui.common.previous_page%>"
            const val NEXT_PAGE = "<%gui.common.next_page%>"
            const val CYCLE_LINE = "<%gui.common.cycle_line%>"
            const val NO_CYCLE_LINE = "<%gui.common.no_cycle_line%>"
        }

        /**
         * Legacy GUI - 线路列表
         */
        object RailLines {
            const val TITLE_EDIT = "<%gui.rail_lines.title_edit%>"
            const val TITLE_SELECT = "<%gui.rail_lines.title_select%>"
            const val ADD_LINE = "<%gui.rail_lines.add_line%>"
            const val LINE_NAME = "<%gui.rail_lines.line_name%>"
            const val LINE_LORE = "<%gui.rail_lines.line_lore%>"
            const val LINE_OPERATIONS_EDIT = "<%gui.rail_lines.line_operations_edit%>"
            const val LINE_OPERATIONS_SELECT = "<%gui.rail_lines.line_operations_select%>"
            const val LINE_ADDED = "<%gui.rail_lines.line_added%>"
            const val LINE_UPDATED = "<%gui.rail_lines.line_updated%>"
            const val LINE_DELETED = "<%gui.rail_lines.line_deleted%>"
            const val DELETE_CONFIRM = "<%gui.rail_lines.delete_confirm%>"
            const val FREE_AREA_MANAGEMENT = "<%gui.rail_lines.free_area_management%>"
            const val FREE_AREA_MANAGEMENT_LORE = "<%gui.rail_lines.free_area_management_lore%>"
        }

        /**
         * Legacy GUI - 线路详情
         */
        object RailLine {
            const val TITLE_EDIT = "<%gui.rail_line.title_edit%>"
            const val TITLE_SELECT = "<%gui.rail_line.title_select%>"
            const val ADD_STATION = "<%gui.rail_line.add_station%>"
            const val ADD_STATION_LORE = "<%gui.rail_line.add_station_lore%>"
            const val LEFT_RETURN_YES = "<%gui.rail_line.left_return_yes%>"
            const val LEFT_RETURN_NO = "<%gui.rail_line.left_return_no%>"
            const val RIGHT_RETURN_YES = "<%gui.rail_line.right_return_yes%>"
            const val RIGHT_RETURN_NO = "<%gui.rail_line.right_return_no%>"
            const val RETURN_DESCRIPTION = "<%gui.rail_line.return_description%>"
            const val STATION_NAME = "<%gui.rail_line.station_name%>"
            const val STATION_LORE = "<%gui.rail_line.station_lore%>"
            const val STATION_OPERATIONS_EDIT = "<%gui.rail_line.station_operations_edit%>"
            const val STATION_OPERATIONS_SELECT = "<%gui.rail_line.station_operations_select%>"
            const val STATION_OPERATIONS_SHIFT_LEFT = "<%gui.rail_line.station_operations_shift_left%>"
            const val STATION_OPERATIONS_SHIFT_RIGHT = "<%gui.rail_line.station_operations_shift_right%>"
            const val STATION_ADDED = "<%gui.rail_line.station_added%>"
            const val STATION_REMOVED = "<%gui.rail_line.station_removed%>"
            const val REMOVE_STATION_CONFIRM = "<%gui.rail_line.remove_station_confirm%>"
            const val LEFT_RETURN_UPDATED = "<%gui.rail_line.left_return_updated%>"
            const val RIGHT_RETURN_UPDATED = "<%gui.rail_line.right_return_updated%>"
        }

        /**
         * Legacy GUI - 站点管理
         */
        object Station {
            const val TITLE = "<%gui.station.title%>"
            const val CREATE_STATION = "<%gui.station.create_station%>"
            const val CREATE_STATION_LORE = "<%gui.station.create_station_lore%>"
            const val STATION_LORE = "<%gui.station.station_lore%>"
            const val STATION_OPERATIONS = "<%gui.station.station_operations%>"
            const val STATION_DELETE_OPERATION = "<%gui.station.delete_operation%>"
            const val STATION_DELETE_OPERATION_IN_USE = "<%gui.station.delete_operation_in_use%>"
            const val STATION_CREATED = "<%gui.station.station_created%>"
            const val STATION_UPDATED = "<%gui.station.station_updated%>"
            const val STATION_DELETED = "<%gui.station.station_deleted%>"
            const val DELETE_CONFIRM = "<%gui.station.delete_confirm%>"
            const val STATION_IN_USE = "<%gui.station.station_in_use%>"
        }
        /**
         * Legacy GUI - 方向选择
         */
        object Reverse {
            const val TITLE = "<%gui.reverse.title%>"
            const val SELECTION_NAME = "<%gui.reverse.selection_name%>"
        }

        /**
         * Legacy GUI - 区域管理
         */
        object RailArea {
            const val TITLE = "<%gui.rail_area.title%>"
            const val ADD_AREA = "<%gui.rail_area.add_area%>"
            const val ADD_AREA_LORE = "<%gui.rail_area.add_area_lore%>"
            const val AREA_NAME = "<%gui.rail_area.area_name%>"
            const val AREA_LORE = "<%gui.rail_area.area_lore%>"
            const val AREA_ADDED = "<%gui.rail_area.area_added%>"
            const val AREA_UPDATED = "<%gui.rail_area.area_updated%>"
            const val AREA_DELETED = "<%gui.rail_area.area_deleted%>"
            const val AREA_UPDATE_FAILED = "<%gui.rail_area.area_update_failed%>"
            const val AREA_DELETE_FAILED = "<%gui.rail_area.area_delete_failed%>"
            const val AREA_ADD_FAILED = "<%gui.rail_area.area_add_failed%>"
            const val DELETE_CONFIRM = "<%gui.rail_area.delete_confirm%>"
            const val TELEPORTED = "<%gui.rail_area.teleported%>"
            const val TELEPORT_FAILED = "<%gui.rail_area.teleport_failed%>"
            const val START_STATION = "<%gui.rail_area.start_station%>"
            const val PREVIOUS_STATION = "<%gui.rail_area.previous_station%>"
            const val END_STATION = "<%gui.rail_area.end_station%>"
            const val NEXT_STATION = "<%gui.rail_area.next_station%>"
        }

        /**
         * Legacy GUI - 游离区域
         */
        object FreeArea {
            const val TITLE = "<%gui.free_area.title%>"
            const val AREA_LORE = "<%gui.free_area.area_lore%>"
            const val AREA_UPDATED = "<%gui.free_area.area_updated%>"
            const val AREA_DELETED = "<%gui.free_area.area_deleted%>"
            const val DELETE_CONFIRM = "<%gui.free_area.delete_confirm%>"
            const val TELEPORTED = "<%gui.free_area.teleported%>"
            const val TELEPORT_FAILED = "<%gui.free_area.teleport_failed%>"
            const val UPDATE_FAILED = "<%gui.free_area.update_failed%>"
            const val DELETE_FAILED = "<%gui.free_area.delete_failed%>"
        }
    }

    /**
     * 输入处理器相关语言键
     */
    object Input {
        object Common {
            const val CANCELLED = "<%input.common.cancelled%>"
            const val CHAT_INPUT_BUSY = "<%input.common.chat_input_busy%>"
            const val UNKNOWN_COMMAND = "<%input.common.unknown_command%>"
        }

        object Station {
            const val PROMPT = "<%input.station.prompt%>"
            const val HELP = "<%input.station.help%>"
            const val EDIT_PROMPT = "<%input.station.edit_prompt%>"
            const val NAME_SET = "<%input.station.name_set%>"
            const val NAME_EXISTS = "<%input.station.name_exists%>"
            const val NAME_REQUIRED = "<%input.station.name_required%>"
            const val SAVE_FAILED = "<%input.station.save_failed%>"
        }

        object RailLine {
            const val PROMPT = "<%input.rail_line.prompt%>"
            const val HELP = "<%input.rail_line.help%>"
            const val NAME_SET = "<%input.rail_line.name_set%>"
            const val NAME_EXISTS = "<%input.rail_line.name_exists%>"
            const val NAME_REQUIRED = "<%input.rail_line.name_required%>"
            const val COLOR_SET = "<%input.rail_line.color_set%>"
            const val COLOR_REQUIRED = "<%input.rail_line.color_required%>"
            const val COLOR_FORMAT_ERROR = "<%input.rail_line.color_format_error%>"
            const val NAME_COLOR_REQUIRED = "<%input.rail_line.name_color_required%>"
            const val SAVE_FAILED = "<%input.rail_line.save_failed%>"
        }

        object Area {
            const val PROMPT = "<%input.area.prompt%>"
            const val EDIT_PROMPT = "<%input.area.edit_prompt%>"
            const val PREVIEW = "<%input.area.preview%>"
            const val HELP = "<%input.area.help%>"
            const val PREVIEW_NOT_SET = "<%input.area.preview_not_set%>"
            const val STATION_SELECTED = "<%input.area.station_selected%>"
            const val DIRECTION_SELECTED = "<%input.area.direction_selected%>"
            const val WORLDEDIT_NOT_INSTALLED = "<%input.area.worldedit_not_installed%>"
            const val SELECT_AREA_FIRST = "<%input.area.select_area_first%>"
            const val WRONG_WORLD = "<%input.area.wrong_world%>"
            const val SINGLE_POINT_ERROR = "<%input.area.single_point_error%>"
            const val AREA_SET = "<%input.area.area_set%>"
            const val AREA_FORMAT_ERROR = "<%input.area.area_format_error%>"
            const val WORLD_NOT_EXISTS = "<%input.area.world_not_exists%>"
            const val MUST_IN_AREA_WORLD = "<%input.area.must_in_area_world%>"
            const val DIRECTION_SET = "<%input.area.direction_set%>"
            const val FACE_CARDINAL_DIRECTION = "<%input.area.face_cardinal_direction%>"
            const val SELECT_RAIL_FIRST = "<%input.area.select_rail_first%>"
            const val STOP_POINT_SET = "<%input.area.stop_point_set%>"
            const val STOP_FORMAT_ERROR = "<%input.area.stop_format_error%>"
            const val MUST_BE_RAIL = "<%input.area.must_be_rail%>"
            const val STOP_MUST_IN_AREA = "<%input.area.stop_must_in_area%>"
            const val STATION_REQUIRED = "<%input.area.station_required%>"
            const val AREA_RANGE_REQUIRED = "<%input.area.area_range_required%>"
            const val STOP_POINT_REQUIRED = "<%input.area.stop_point_required%>"
            const val DIRECTION_REQUIRED = "<%input.area.direction_required%>"
            const val CREATE_FAILED = "<%input.area.create_failed%>"
            const val COMMANDS = "<%input.area.commands%>"
        }
    }

    // ==================== 命令系统层 (Command System Layer) ====================
    object Commands {
        object Common {
            const val PLAYER_ONLY = "<%commands.common.player_only%>"
        }

        object Help {
            const val DESCRIPTION = "<%commands.help.description%>"
            const val QUERY = "<%commands.help.query%>"
        }

        object Main {
            const val DESCRIPTION = "<%commands.main.description%>"
            const val GUI_OPENED = "<%commands.railarea.gui_opened%>"
            const val GUI_FAILED = "<%commands.railarea.gui_failed%>"
            const val LOG_SUCCESS = "<%commands.railarea.log_success%>"
            const val LOG_FAILED = "<%commands.railarea.log_failed%>"
        }

        object Reload {
            const val DESCRIPTION = "<%commands.reload.description%>"
            const val SUCCESS = "<%commands.reload.success%>"
            const val FAILED = "<%commands.reload.failed%>"
            const val LOG_SUCCESS = "<%commands.reload.log_success%>"
            const val LOG_FAILED = "<%commands.reload.log_failed%>"
        }

        object NewArea {
            const val DESCRIPTION = "<%commands.new_area.description%>"
            const val AREA_ADDED = "<%commands.new_area.area_added%>"
            const val AREA_ADD_FAILED = "<%commands.new_area.area_add_failed%>"
        }
    }

    // ==================== 日志系统层 (Logging Layer) ====================
    object Log {
        // 当前版本暂无专用日志，预留扩展
    }

    // ==================== 事件系统层 (Events Layer) ====================
    object Events {
        object Core {
            const val ARRIVED_TITLE = "<%events.core.arrived_title%>"
            const val ARRIVED_SUBTITLE_SOLO = "<%events.core.arrived_subtitle_solo%>"
            const val ARRIVED_SUBTITLE_SWITCHABLE = "<%events.core.arrived_subtitle_switchable%>"
            const val UNDER_BOARD_TITLE = "<%events.core.under_board_title%>"
            const val UNDER_BOARD_SUBTITLE_TERMINAL = "<%events.core.under_board_subtitle_terminal%>"
            const val UNDER_BOARD_SUBTITLE = "<%events.core.under_board_subtitle%>"
            const val START_TITLE = "<%events.core.start_title%>"
            const val START_SUBTITLE_SWITCHABLE = "<%events.core.start_subtitle_switchable%>"
            const val START_SUBTITLE_SOLO = "<%events.core.start_subtitle_solo%>"
            const val TERMINAL_ACTIONBAR = "<%events.core.terminal_actionbar%>"
            const val NON_TERMINAL_ACTIONBAR = "<%events.core.non_terminal_actionbar%>"
            const val ARRIVED_TERMINAL_MESSAGE = "<%events.core.arrived_terminal_message%>"
            const val START_MESSAGE = "<%events.core.start_message%>"
            const val WAITING_MESSAGE = "<%events.core.waiting_message%>"
        }
    }
}
