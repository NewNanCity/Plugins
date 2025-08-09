package city.newnan.deathcost.i18n

/**
 * DeathCost 语言键常量类
 *
 * 遵循五层架构的 i18n Key 分类体系，使用 <%key%> 格式便于模板替换。
 *
 * 五层架构分类：
 * 1. 核心系统层 (Core System Layer) - 插件生命周期、配置管理、系统级错误
 * 2. 命令系统层 (Command System Layer) - 命令处理、参数验证、帮助系统
 * 3. 图形界面层 (GUI Layer) - GUI界面、按钮、提示信息
 * 4. 业务领域层 (Business Domain Layer) - 死亡扣费、经济系统、权限管理
 * 5. 日志系统层 (Logging Layer) - 日志消息
 *
 * @author NewNanCity
 * @since 2.0.0
 */
object LanguageKeys {

    // ==================== 核心系统层 (Core System Layer) ====================
    object Core {
        object Config {
            const val TARGET_ACCOUNT_SET = "<%logger.config.target_account_set%>"
            const val RELOAD_FAILED = "<%plugin.config.reload_failed%>"
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
        }

        object Status {
            const val DESCRIPTION = "<%commands.status.description%>"
            const val HEADER = "<%commands.status.header%>"
            const val VERSION = "<%commands.status.version%>"
            const val ENABLED = "<%commands.status.enabled%>"
            const val COST_MODE = "<%commands.status.cost_mode%>"
            const val MODE_SIMPLE = "<%commands.status.mode_simple%>"
            const val MODE_COMPLEX = "<%commands.status.mode_complex%>"
            const val SIMPLE_COST = "<%commands.status.simple_cost%>"
            const val COST_FIXED = "<%commands.status.cost_fixed%>"
            const val COST_PERCENT = "<%commands.status.cost_percent%>"
            const val COMPLEX_TIERS = "<%commands.status.complex_tiers%>"
            const val TARGET_ACCOUNT = "<%commands.status.target_account%>"
            const val NO_TARGET = "<%commands.status.no_target%>"
            const val PLAYER_MESSAGE = "<%commands.status.player_message%>"
            const val BROADCAST_MESSAGE = "<%commands.status.broadcast_message%>"
            const val CONSOLE_MESSAGE = "<%commands.status.console_message%>"
            const val ERROR = "<%commands.status.error%>"
            const val LOG_ERROR = "<%commands.status.log_error%>"
        }
    }

    // ==================== 图形界面层 (GUI Layer) ====================
    object Gui {
        // 当前版本暂无GUI功能，预留扩展
    }

    // ==================== 业务领域层 (Business Domain Layer) ====================
    object Business {
        object Death {
            const val COST_DEDUCTED = "<%death.cost_deducted%>"
            const val BROADCAST_MESSAGE = "<%death.broadcast_message%>"
            const val CONSOLE_MESSAGE = "<%death.console_message%>"
            const val NO_COST = "<%death.no_cost%>"
        }

        object Common {
            const val ENABLED = "<%common.enabled%>"
            const val DISABLED = "<%common.disabled%>"
        }

        object Module {
            const val INITIALIZED = "<%logger.module.initialized%>"
            const val RELOADED = "<%logger.module.reloaded%>"
            const val CONFIG_LOAD_FAILED = "<%logger.module.config_load_failed%>"
        }

        object Warning {
            const val SIMPLE_MODE_NO_CONFIG = "<%warning.simple_mode_no_config%>"
            const val COMPLEX_MODE_NO_CONFIG = "<%warning.complex_mode_no_config%>"
        }
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
