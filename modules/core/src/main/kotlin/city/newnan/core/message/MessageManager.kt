package city.newnan.core.message

import city.newnan.core.terminable.Terminable
import city.newnan.core.utils.text.ComponentParseMode
import city.newnan.core.utils.text.ComponentParseModeDetector
import city.newnan.core.utils.text.ComponentProcessor
import city.newnan.core.utils.text.LanguageProvider
import city.newnan.core.utils.text.StringFormatter
import city.newnan.core.utils.text.toComponent
import city.newnan.core.utils.text.toLegacy
import city.newnan.core.utils.text.toMiniMessage
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import java.text.MessageFormat
import java.util.logging.Logger as JavaLogger

/**
 * 消息管理器
 *
 * 专注于格式化各种消息并向目标接收者输出，包括：
 * - 玩家消息和命令反馈
 * - 控制台管理员消息
 * - 多语言支持
 * - 颜色代码处理
 *
 * 与Logger的区别：
 * - MessageManager专注于用户交互和即时反馈
 * - Logger专注于日志记录和持久化存储
 * - MessageManager直接使用Bukkit的Logger，避免调用LoggerProvider
 */
class MessageManager(
    private val bukkitLogger: JavaLogger,
    private var stringFormatter: StringFormatter? = null,
    private var playerPrefix: String = "",
    private var consolePrefix: String = "",
    private val defaultComponentParseMode: ComponentParseMode = ComponentParseMode.Auto
) : IMessager, Terminable {

    private var _bakedPlayerPrefixComponent: Component? = null
    private lateinit var _bakedPlayerPrefixLegacy: String
    private lateinit var _bakedPlayerPrefixMiniMessage: String
    private lateinit var _bakedPlayerPrefixPlain: String
    private lateinit var _bakedConsolePrefixLegacy: String

    init {
        // 如果没有提供StringFormatter，则创建一个默认的
        stringFormatter = stringFormatter ?: StringFormatter()
        bakePlayerPrefix()
        bakeConsolePrefix()
    }

    private fun bakePlayerPrefix() {
        if (ComponentProcessor.isComponentSupported) {
            _bakedPlayerPrefixComponent = playerPrefix.toComponent()
            _bakedPlayerPrefixLegacy = _bakedPlayerPrefixComponent!!.toLegacy()
            _bakedPlayerPrefixMiniMessage = _bakedPlayerPrefixComponent!!.toMiniMessage()
            _bakedPlayerPrefixPlain = ComponentProcessor.stripLegacySymbol(_bakedPlayerPrefixLegacy)
            return
        }
        _bakedPlayerPrefixComponent = null
        _bakedPlayerPrefixLegacy = playerPrefix.toLegacy(ComponentParseMode.Legacy)
        _bakedPlayerPrefixMiniMessage = playerPrefix
        _bakedPlayerPrefixPlain = ComponentProcessor.stripLegacySymbol(_bakedPlayerPrefixLegacy)
    }

    private fun bakeConsolePrefix() {
        if (ComponentProcessor.isComponentSupported) {
            _bakedConsolePrefixLegacy = consolePrefix.toComponent().toLegacy()
            return
        }
        _bakedConsolePrefixLegacy = consolePrefix.toLegacy(ComponentParseMode.Legacy)
    }

    /**
     * 设置向玩家输出信息时的前缀
     * @param prefix 前缀字符串
     */
    override fun setPlayerPrefix(prefix: String): IMessager = this.also {
        playerPrefix = prefix
        bakePlayerPrefix()
    }

    /**
     * 设置向控制台输出信息时的前缀
     * @param prefix 前缀字符串
     */
    override fun setConsolePrefix(prefix: String): IMessager = this.also {
        consolePrefix = prefix
        bakeConsolePrefix()
    }

    /**
     * 设置StringFormatter
     * @param formatter StringFormatter
     */
    fun setStringFormatter(formatter: StringFormatter?): MessageManager = this.also {
        this.stringFormatter = formatter
    }

    /**
     * 设置多语言服务提供者
     * @param languageProvider 多语言服务提供者，需实现LanguageProvider接口
     */
    override fun setLanguageProvider(languageProvider: LanguageProvider?): IMessager = this.also {
        if (stringFormatter != null) {
            stringFormatter!!.languageProvider = languageProvider
        } else {
            stringFormatter = StringFormatter(languageProvider)
        }
    }

    /**
     * 获取当前设置的语言提供者
     */
    override fun getLanguageProvider(): LanguageProvider? = stringFormatter?.languageProvider

    override fun sprintfLegacy(provider: Boolean, parseMode: ComponentParseMode?, formatText: String, vararg params: Any?): String {
        val finalParseMode = parseMode ?: defaultComponentParseMode
        if (stringFormatter != null) return stringFormatter!!.sprintfLegacy(provider, finalParseMode, formatText, *params)
        return if (params.isEmpty()) formatText.toLegacy(finalParseMode) else
            MessageFormat.format(formatText, *params.map { it.toString() }.toTypedArray()).toLegacy(finalParseMode)
    }

    override fun sprintfPlain(provider: Boolean, formatText: String, vararg params: Any?): String {
        if (stringFormatter != null) return stringFormatter!!.sprintfPlain(provider, formatText, *params)
        return if (params.isEmpty()) formatText else
            MessageFormat.format(formatText, *params.map { it.toString() }.toTypedArray())
    }

    /**
     * 格式化字符串
     * @param provider 是否使用语言提供者
     * @param parseMode 解析模式
     * @param formatText 格式化文本
     * @param params 参数
     * @return 格式化后的字符串
     */
    override fun sprintf(provider: Boolean, parseMode: ComponentParseMode?, formatText: String, vararg params: Any?): Component {
        val finalParseMode = parseMode ?: defaultComponentParseMode
        if (stringFormatter != null) return stringFormatter!!.sprintf(provider, finalParseMode, formatText, *params)
        return if (params.isEmpty()) formatText.toComponent(finalParseMode) else
            MessageFormat.format(formatText, *params.map { it.toString() }.toTypedArray()).toComponent(finalParseMode)
    }

    /**
     * 格式化并输出消息
     * @param sendTo 接收者，null表示控制台
     * @param prefix 是否添加前缀
     * @param provider 是否使用语言提供者
     * @param parseMode 解析模式
     * @param formatText 格式化文本
     * @param params 参数
     */
    override fun printf(
        sendTo: CommandSender?,
        prefix: Boolean,
        provider: Boolean,
        parseMode: ComponentParseMode?,
        formatText: String,
        vararg params: Any?
    ): IMessager {
        // 格式化
        var finalParseMode = parseMode ?: defaultComponentParseMode

        // 分对象输出
        if (sendTo == null || sendTo is ConsoleCommandSender) {
            // 以兼容模式输出
            val legacyText =
                stringFormatter?.sprintfLegacy(provider, finalParseMode, formatText, *params)
                ?: formatText.toLegacy(finalParseMode)
            // 样式码转换+输出
            bukkitLogger.info(if (prefix) _bakedConsolePrefixLegacy + legacyText else legacyText)
        } else {
            // 考虑没有Component方法的回滚情况
            if (ComponentProcessor.isComponentSupported) {
                // 以Component模式输出
                val component = stringFormatter?.sprintf(provider, finalParseMode, formatText, *params)
                    ?: formatText.toComponent(finalParseMode)
                sendTo.sendMessage(if (prefix && _bakedPlayerPrefixComponent != null) _bakedPlayerPrefixComponent!!.append(component) else component)
            } else {
                // 主动猜测类型
                finalParseMode = if (finalParseMode == ComponentParseMode.Auto) ComponentParseModeDetector.detect(formatText) else finalParseMode
                when (finalParseMode) {
                    ComponentParseMode.Plain -> sendTo.sendPlainMessage(_bakedPlayerPrefixPlain +
                            (stringFormatter?.sprintfPlain(provider, formatText, *params) ?: formatText)
                    )
                    ComponentParseMode.Legacy -> sendTo.sendMessage(_bakedConsolePrefixLegacy +
                            (stringFormatter?.sprintfLegacy(provider, finalParseMode, formatText, *params) ?: formatText.toLegacy(finalParseMode))
                    )
                    ComponentParseMode.MiniMessage -> sendTo.sendRichMessage(_bakedPlayerPrefixMiniMessage +
                            (stringFormatter?.sprintfPlain(provider, formatText, *params) ?: formatText)
                    )
                    ComponentParseMode.Auto -> sendTo.sendMessage(_bakedConsolePrefixLegacy + formatText) // Never happen
                }
            }
        }
        return this
    }

    override fun close() {
        // MessageManager本身不需要特殊的清理操作
    }
}
