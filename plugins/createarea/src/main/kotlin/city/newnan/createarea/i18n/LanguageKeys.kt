package city.newnan.createarea.i18n

/**
 * CreateArea语言键常量类
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
            const val ENABLED = "<%core.plugin.enabled%>"
            const val DISABLED = "<%core.plugin.disabled%>"
            const val RELOADING = "<%core.plugin.reloading%>"
            const val RELOADED = "<%core.plugin.reloaded%>"
            const val RELOAD_FAILED = "<%core.plugin.reload_failed%>"
        }

        object Error {
            const val NO_PERMISSION = "<%core.error.no_permission%>"
            const val PLAYER_ONLY = "<%core.error.player_only%>"
            const val OPERATION_FAILED = "<%core.error.operation_failed%>"
            const val INVALID_ARGS = "<%core.error.invalid_args%>"
            const val PLAYER_NOT_FOUND = "<%core.error.player_not_found%>"
            const val WORLD_NOT_FOUND = "<%core.error.world_not_found%>"
        }

        object Success {
            const val OPERATION_COMPLETED = "<%core.success.operation_completed%>"
        }
    }

    // ==================== 命令系统层 (Command System Layer) ====================
    object Commands {
        object Help {
            const val DESCRIPTION = "<%commands.help.description%>"
            const val HEADER = "<%commands.help.header%>"
            const val FOOTER = "<%commands.help.footer%>"
        }

        object Reload {
            const val DESCRIPTION = "<%commands.reload.description%>"
            const val SUCCESS = "<%commands.reload.success%>"
            const val FAILED = "<%commands.reload.failed%>"
            const val LOG_SUCCESS = "<%commands.reload.log_success%>"
            const val LOG_FAILED = "<%commands.reload.log_failed%>"
        }

        object Tp {
            const val DESCRIPTION = "<%commands.tp.description%>"
            const val PLAYER_ARG = "<%commands.tp.player_arg%>"
            const val SUCCESS_SELF = "<%commands.tp.success_self%>"
            const val SUCCESS_OTHER = "<%commands.tp.success_other%>"
            const val AREA_NOT_FOUND_SELF = "<%commands.tp.area_not_found_self%>"
            const val AREA_NOT_FOUND_OTHER = "<%commands.tp.area_not_found_other%>"
        }

        object Set {
            const val DESCRIPTION = "<%commands.set.description%>"
            const val PLAYER_ARG = "<%commands.set.player_arg%>"
            const val SUCCESS_SELF = "<%commands.set.success_self%>"
            const val SUCCESS_OTHER = "<%commands.set.success_other%>"
            const val NO_SELECTION = "<%commands.set.no_selection%>"
            const val WORLDEDIT_NOT_FOUND = "<%commands.set.worldedit_not_found%>"
        }

        object Delete {
            const val DESCRIPTION = "<%commands.delete.description%>"
            const val PLAYER_ARG = "<%commands.delete.player_arg%>"
            const val SUCCESS_SELF = "<%commands.delete.success_self%>"
            const val SUCCESS_OTHER = "<%commands.delete.success_other%>"
            const val AREA_NOT_FOUND_SELF = "<%commands.delete.area_not_found_self%>"
            const val AREA_NOT_FOUND_OTHER = "<%commands.delete.area_not_found_other%>"
        }

        object Gui {
            const val DESCRIPTION = "<%commands.gui.description%>"
        }
    }

    // ==================== 图形界面层 (GUI Layer) ====================
    object Gui {
        object Common {
            const val BACK = "<%gui.common.back%>"
            const val CLOSE = "<%gui.common.close%>"
            const val CONFIRM = "<%gui.common.confirm%>"
            const val PREVIOUS_PAGE = "<%gui.common.previous_page%>"
            const val NEXT_PAGE = "<%gui.common.next_page%>"
        }

        object AreaList {
            const val TITLE = "<%gui.area_list.title%>"
            const val AREA_ENTRY = "<%gui.area_list.area_entry%>"
            const val NO_AREAS = "<%gui.area_list.no_areas%>"
        }
    }

    // ==================== 业务领域层 (Business Domain Layer) ====================
    object Area {
        const val CREATED = "<%area.created%>"
        const val DELETED = "<%area.deleted%>"
        const val TELEPORTED = "<%area.teleported%>"
        const val PERMISSION_UPDATED = "<%area.permission_updated%>"
        const val DYNMAP_SYNCED = "<%area.dynmap_synced%>"
    }

    object Permission {
        const val GROUP_ADDED = "<%permission.group_added%>"
        const val GROUP_REMOVED = "<%permission.group_removed%>"
        const val CHECK_FAILED = "<%permission.check_failed%>"
    }

    // ==================== 事件层 (Events Layer) ====================
    object Events {
        // 预留，将来用于创造区域事件消息键
    }

    // ==================== 日志系统层 (Logging Layer) ====================
    object Log {
        object Info {
            const val PLUGIN_LOADED = "<%log.info.plugin_loaded%>"
            const val CONFIG_LOADED = "<%log.info.config_loaded%>"
            const val AREA_CREATED = "<%log.info.area_created%>"
            const val AREA_DELETED = "<%log.info.area_deleted%>"
            const val DYNMAP_CONNECTED = "<%log.info.dynmap_connected%>"
        }

        object Error {
            const val INITIALIZATION_FAILED = "<%log.error.initialization_failed%>"
            const val SERVICE_ERROR = "<%log.error.service_error%>"
            const val WORLDEDIT_ERROR = "<%log.error.worldedit_error%>"
            const val DYNMAP_ERROR = "<%log.error.dynmap_error%>"
            const val VAULT_ERROR = "<%log.error.vault_error%>"
        }

        object Warning {
            const val WORLDEDIT_NOT_FOUND = "<%log.warning.worldedit_not_found%>"
            const val DYNMAP_NOT_FOUND = "<%log.warning.dynmap_not_found%>"
            const val VAULT_NOT_FOUND = "<%log.warning.vault_not_found%>"
        }
    }
}
