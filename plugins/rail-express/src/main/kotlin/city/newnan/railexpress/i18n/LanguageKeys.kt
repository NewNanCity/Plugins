package city.newnan.railexpress.i18n

/**
 * RailExpress 语言键常量类
 *
 * 遵循五层架构的 i18n Key 分类体系，使用 <%key%> 格式便于模板替换。
 *
 * 五层架构分类：
 * 1. 核心系统层 (Core System Layer) - 插件生命周期、配置管理、系统级错误
 * 2. 命令系统层 (Command System Layer) - 命令处理、参数验证、帮助系统
 * 3. 图形界面层 (GUI Layer) - GUI界面、按钮、提示信息
 * 4. 业务领域层 (Business Domain Layer) - 矿车速度控制、世界配置、事件处理
 * 5. 日志系统层 (Logging Layer) - 日志消息
 *
 * @author NewNanCity
 * @since 2.0.0
 */
object LanguageKeys {

    // ==================== 核心系统层 (Core System Layer) ====================
    object Core {
        object Plugin {
            const val EVENTS_REGISTERED = "<%railexpress.plugin.events_registered%>"
        }

        object Config {
            const val RELOAD_FAILED = "<%railexpress.config.reload_failed%>"
            const val WORLDS_LOADED = "<%railexpress.config.worlds_loaded%>"
        }
    }

    // ==================== 命令系统层 (Command System Layer) ====================
    object Commands {
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
    }

    // ==================== 图形界面层 (GUI Layer) ====================
    object Gui {
        // 当前版本暂无GUI功能，预留扩展
    }

    // ==================== 业务领域层 (Business Domain Layer) ====================
    object Business {
        // 当前版本暂无专用业务领域，预留扩展
    }

    // ==================== 日志系统层 (Logging Layer) ====================
    object Log {
        // 当前版本暂无专用日志，预留扩展
    }

    // ==================== 事件系统层 (Events Layer) ====================
    object Events {
        // 当前版本暂无专用事件，预留扩展
    }
}
