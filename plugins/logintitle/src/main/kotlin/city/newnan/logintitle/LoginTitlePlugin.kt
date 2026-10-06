package city.newnan.logintitle

import city.newnan.core.base.BasePlugin
import city.newnan.core.config.CorePluginConfig
import city.newnan.core.logging.LogLevel
import city.newnan.logintitle.modules.LoginTitleModule

/**
 * LoginTitle 登录标题插件
 *
 * 这是一个最小的插件示例，功能非常简单：
 * - 监听玩家登录事件
 * - 对所有玩家显示标题
 * - 标题持续10秒钟
 * - 不使用配置文件和国际化
 *
 * @author AI
 * @since 1.0.0
 */
class LoginTitlePlugin : BasePlugin() {

    companion object {
        lateinit var instance: LoginTitlePlugin
            private set
    }

    /**
     * 登录标题模块
     */
    private lateinit var loginTitleModule: LoginTitleModule

    override fun onPluginEnable() {
        instance = this

        // 初始化模块
        loginTitleModule = LoginTitleModule("LoginTitleModule", this)

        logger.info("LoginTitle plugin enabled successfully!")
    }

    override fun onPluginDisable() {
        logger.info("LoginTitle plugin disabled!")
        // 所有绑定的资源会自动清理
    }

    /**
     * 返回硬编码的核心配置
     */
    override fun getCoreConfig(): CorePluginConfig = CorePluginConfig.build {
        // 基础日志配置
        logging.logLevel = LogLevel.INFO
        logging.fileLoggingEnabled = false

        // 消息配置
        message.playerPrefix = "[LoginTitle] "
        message.consolePrefix = "[LoginTitle] "
    }
}
