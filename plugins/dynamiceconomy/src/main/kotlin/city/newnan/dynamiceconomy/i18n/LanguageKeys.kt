package city.newnan.dynamiceconomy.i18n

/**
 * DynamicEconomy插件语言键常量
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
            const val OPERATION_FAILED = "<%core.error.operation_failed%>"
            const val INSUFFICIENT_FUNDS = "<%core.error.insufficient_funds%>"
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

        object Stats {
            const val DESCRIPTION = "<%commands.stats.description%>"
            const val WEALTH_HEADER = "<%commands.stats.wealth_header%>"
            const val ECONOMY_HEADER = "<%commands.stats.economy_header%>"
            const val COMMODITY_HEADER = "<%commands.stats.commodity_header%>"
        }

        object Commodity {
            const val LIST_DESCRIPTION = "<%commands.commodity.list.description%>"
            const val INFO_DESCRIPTION = "<%commands.commodity.info.description%>"
            const val BUY_DESCRIPTION = "<%commands.commodity.buy.description%>"
            const val SELL_DESCRIPTION = "<%commands.commodity.sell.description%>"
            const val NAME_ARG = "<%commands.commodity.name.arg%>"
            const val AMOUNT_ARG = "<%commands.commodity.amount.arg%>"
        }

        object Issue {
            const val DESCRIPTION = "<%commands.issue.description%>"
            const val SUCCESS = "<%commands.issue.success%>"
            const val FAILED = "<%commands.issue.failed%>"
            const val LOG_SUCCESS = "<%commands.issue.log_success%>"
            const val LOG_FAILED = "<%commands.issue.log_failed%>"
            const val AMOUNT_ARG = "<%commands.issue.amount.arg%>"
        }

        object UpdateIndex {
            const val DESCRIPTION = "<%commands.update_index.description%>"
            const val SUCCESS = "<%commands.update_index.success%>"
            const val FAILED = "<%commands.update_index.failed%>"
            const val LOG_SUCCESS = "<%commands.update_index.log_success%>"
            const val LOG_FAILED = "<%commands.update_index.log_failed%>"
        }

        object ReloadIssuance {
            const val DESCRIPTION = "<%commands.reload_issuance.description%>"
            const val SUCCESS = "<%commands.reload_issuance.success%>"
            const val FAILED = "<%commands.reload_issuance.failed%>"
            const val LOG_SUCCESS = "<%commands.reload_issuance.log_success%>"
            const val LOG_FAILED = "<%commands.reload_issuance.log_failed%>"
        }
    }

    // ==================== 业务领域层 (Business Domain Layer) ====================
    object Wealth {
        const val TOTAL_WEALTH = "<%wealth.total_wealth%>"
        const val RESOURCE_COUNT = "<%wealth.resource_count%>"
    }

    object Economy {
        const val CURRENCY_ISSUANCE = "<%economy.currency_issuance%>"
        const val NATIONAL_TREASURY = "<%economy.national_treasury%>"
        const val INSUFFICIENT_TREASURY = "<%economy.insufficient_treasury%>"
        const val OWNER_ACCOUNT_SET = "<%economy.owner_account_set%>"
    }

    object Commodity {
        const val BUY_SUCCESS = "<%commodity.buy_success%>"
        const val SELL_SUCCESS = "<%commodity.sell_success%>"
        const val NOT_FOUND = "<%commodity.not_found%>"
        const val INSUFFICIENT_STOCK = "<%commodity.insufficient_stock%>"
        const val BUY_PRICE = "<%commodity.buy_price%>"
        const val SELL_PRICE = "<%commodity.sell_price%>"
        const val STOCK = "<%commodity.stock%>"
    }

    // ==================== 日志系统层 (Logging Layer) ====================
    object Log {
        object Info {
            const val WEALTH_CALCULATED = "<%log.info.wealth_calculated%>"
            const val COMMODITY_TRADED = "<%log.info.commodity_traded%>"
        }

        object Warning {
            const val VAULT_NOT_FOUND = "<%log.warning.vault_not_found%>"
        }

        object Error {
            const val VAULT_ERROR = "<%log.error.vault_error%>"
            const val WEALTH_CALCULATION_ERROR = "<%log.error.wealth_calculation_error%>"
            const val COMMODITY_ERROR = "<%log.error.commodity_error%>"
            const val ECONOMY_ERROR = "<%log.error.economy_error%>"
        }
    }

    // ==================== 事件层 (Events Layer) ====================
    object Events {
        // 预留，将来用于经济事件消息键
    }

    // ==================== 图形界面层 (GUI Layer) ====================
    object Gui {
        // 预留，将来用于经济系统图形界面消息键
    }
}
