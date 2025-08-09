package city.newnan.mcron.i18n

/**
 * MCron 语言键常量类
 *
 * 遵循五层架构的 i18n Key 分类体系，使用 <%key%> 格式便于模板替换。
 *
 * 五层架构分类：
 * 1. 核心系统层 (Core System Layer) - 插件生命周期、配置管理、系统级错误
 * 2. 命令系统层 (Command System Layer) - 命令处理、参数验证、帮助系统
 * 3. 图形界面层 (GUI Layer) - GUI界面、按钮、提示信息
 * 4. 业务领域层 (Business Domain Layer) - 定时任务管理、Cron表达式、调度器
 * 5. 日志系统层 (Logging Layer) - 日志消息
 *
 * @author NewNanCity
 * @since 1.0.0
 */
object LanguageKeys {

    // ==================== 核心系统层 (Core System Layer) ====================
    object Core {
        object Plugin {
            const val LISTENERS_REGISTERED = "<%listeners.registered%>"
        }

        object Config {
            const val RELOAD_FAILED = "<%mcron.config.reload_failed%>"
        }
    }

    // ==================== 命令系统层 (Command System Layer) ====================
    object Commands {
        object Executed {
            const val COMMAND = "<%commands.executed%>"
            const val FAILED = "<%commands.execution_failed%>"
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
            const val SUCCESS = "<%reload_success%>"
            const val FAILED = "<%commands.reload.failed%>"
            const val LOG_SUCCESS = "<%commands.reload.log_success%>"
            const val LOG_FAILED = "<%commands.reload.log_failed%>"
        }
    }

    // ==================== 图形界面层 (GUI Layer) ====================
    object Gui {
        // 暂无GUI
    }

    // ==================== 业务领域层 (Business Domain Layer) ====================
    object Business {
        object Server {
            const val READY_TASK_FAILED = "<%mcron.server.ready_task_failed%>"
            const val LOADED = "<%mcron.server.loaded%>"
        }

        object Common {
            const val INVALID_SLEEP_TIME = "<%commands.invalid_sleep_time%>"
        }
    }

    // ==================== 日志系统层 (Logging Layer) ====================
    object Log {
        // 暂无专用日志
    }

    // ==================== 事件系统层 (Events Layer) ====================
    object Events {
        // 暂无专用事件
    }
}
