package city.newnan.bettercommandblock.i18n

/**
 * BetterCommandBlock 插件语言键常量类
 *
 * 遵循五层架构的 i18n Key 分类体系，使用 <%key%> 格式便于模板替换。
 *
 * 五层架构分类：
 * 1. 核心系统层 (Core System Layer) - 插件生命周期、配置管理、系统级错误
 * 2. 命令系统层 (Command System Layer) - 命令处理、参数验证、帮助系统
 * 3. 图形界面层 (GUI Layer) - GUI界面、按钮、提示信息（本插件暂无GUI）
 * 4. 业务领域层 (Business Domain Layer) - 命令方块安全、查看功能、扩展命令
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
            const val COMMAND_BLOCK_ONLY = "<%core.error.command_block_only%>"
            const val OPERATION_FAILED = "<%core.error.operation_failed%>"
        }
    }

    // ==================== 命令系统层 (Command System Layer) ====================
    object Commands {
        object Help {
            const val DESCRIPTION = "<%commands.help.description%>"
            const val QUERY = "<%commands.help.query%>"
        }

        object Reload {
            const val SUCCESS = "<%commands.reload.success%>"
            const val FAILED = "<%commands.reload.failed%>"
            const val DESCRIPTION = "<%commands.reload.description%>"
        }

        object Pick {
            const val SUCCESS = "<%commands.pick.success%>"
            const val NO_CONTAINER_DOWN = "<%commands.pick.no_container_down%>"
            const val NO_CONTAINER_UP = "<%commands.pick.no_container_up%>"
            const val SOURCE_EMPTY = "<%commands.pick.source_empty%>"
            const val TARGET_FULL = "<%commands.pick.target_full%>"
            const val INVALID_TYPE = "<%commands.pick.invalid_type%>"
            const val INVALID_DIRECTION = "<%commands.pick.invalid_direction%>"
            const val DESCRIPTION = "<%commands.pick.description%>"
        }

        object Scoreboard {
            const val SUCCESS = "<%commands.scoreboard.success%>"
            const val OBJECTIVE_NOT_FOUND = "<%commands.scoreboard.objective_not_found%>"
            const val TARGET_NOT_FOUND = "<%commands.scoreboard.target_not_found%>"
            const val INVALID_MODE = "<%commands.scoreboard.invalid_mode%>"
            const val DESCRIPTION = "<%commands.scoreboard.description%>"
        }

        object Execute {
            const val SUCCESS = "<%commands.execute.success%>"
            const val BLOCKED_COMMAND = "<%commands.execute.blocked_command%>"
            const val ENTITY_NOT_FOUND = "<%commands.execute.entity_not_found%>"
            const val RECURSIVE_EXECUTE = "<%commands.execute.recursive_execute%>"
            const val DESCRIPTION = "<%commands.execute.description%>"
        }
    }

    // ==================== 图形界面层 (GUI Layer) ====================
    object Gui {
        // 预留，本插件主要通过命令交互
    }

    // ==================== 业务领域层 (Business Domain Layer) ====================
    object Firewall {
        object Commands {
            object Status {
                const val SUCCESS = "<%firewall.commands.status.success%>"
                const val DESCRIPTION = "<%firewall.commands.status.description%>"
            }

            object Stats {
                const val SUCCESS = "<%firewall.commands.stats.success%>"
                const val RESET_SUCCESS = "<%firewall.commands.stats.reset_success%>"
                const val DESCRIPTION = "<%firewall.commands.stats.description%>"
            }

            object Test {
                const val COMMAND_SAFE = "<%firewall.commands.test.command_safe%>"
                const val COMMAND_BLOCKED = "<%firewall.commands.test.command_blocked%>"
                const val DESCRIPTION = "<%firewall.commands.test.description%>"
            }

            object Reload {
                const val SUCCESS = "<%firewall.commands.reload.success%>"
                const val FAILED = "<%firewall.commands.reload.failed%>"
                const val DESCRIPTION = "<%firewall.commands.reload.description%>"
            }
        }
    }

    object View {
        const val COMMAND_CONTENT = "<%view.command_content%>"
    }

    // ==================== 事件层 (Events Layer) ====================
    object Events {
        // 预留，将来用于命令方块交互事件消息键
    }

    // ==================== 日志系统层 (Logging Layer) ====================
    object Log {
        object Info {
            const val COMMAND_REGISTERED = "<%log.info.command_registered%>"
        }

        object Error {
            const val INITIALIZATION_FAILED = "<%log.error.initialization_failed%>"
            const val COMMAND_EXECUTION_FAILED = "<%log.error.command_execution_failed%>"
            const val EVENT_HANDLING_FAILED = "<%log.error.event_handling_failed%>"
        }

        object Warning {
            const val UNSAFE_OPERATION = "<%log.warning.unsafe_operation%>"
        }

        object Debug {
            const val PLAYER_INTERACTION = "<%log.debug.player_interaction%>"
        }
    }
}
