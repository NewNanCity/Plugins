package city.newnan.core.message

import city.newnan.core.utils.text.ComponentParseMode
import city.newnan.core.utils.text.LanguageProvider
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender

/**
 * 消息发送器接口
 * 
 * 基于MessageManager的API设计，为模块提供消息发送抽象
 * 
 * 核心方法对应MessageManager的功能：
 * - sprintf: 格式化字符串
 * - printf: 格式化并发送消息
 * - 前缀设置和语言提供者管理
 */
interface IMessager {

    fun sprintfPlain(formatText: String, vararg params: Any?): String =
        sprintfPlain(true, formatText, *params)

    fun sprintfPlain(provider: Boolean, formatText: String, vararg params: Any?): String

    fun sprintfLegacy(formatText: String, vararg params: Any?): String =
        sprintfLegacy(true, null, formatText, *params)

    fun sprintfLegacy(provider: Boolean, formatText: String, vararg params: Any?): String =
        sprintfLegacy(provider, null, formatText, *params)

    fun sprintfLegacy(parseMode: ComponentParseMode?, formatText: String, vararg params: Any?): String =
        sprintfLegacy(true, parseMode, formatText, *params)

    fun sprintfLegacy(provider: Boolean, parseMode: ComponentParseMode?, formatText: String, vararg params: Any?): String

    /**
     * 格式化字符串（sprintf风格）
     * @param provider 是否使用语言提供者
     * @param parseMode 解析模式，null为默认解析模式
     * @param formatText 格式化文本
     * @param params 参数
     * @return 格式化后的字符串
     */
    fun sprintf(provider: Boolean, parseMode: ComponentParseMode?, formatText: String, vararg params: Any?): Component

    /**
     * 格式化字符串（默认启用颜色代码处理）
     */
    fun sprintf(provider: Boolean, formatText: String, vararg params: Any?): Component =
        sprintf(provider, null, formatText, *params)

    /**
     * 格式化字符串（默认启用语言提供者和颜色代码处理）
     */
    fun sprintf(formatText: String, vararg params: Any?): Component =
        sprintf(provider = true, parseMode = null, formatText, *params)
    
    /**
     * 格式化并输出消息（printf风格）
     * @param sendTo 接收者，null表示控制台
     * @param prefix 是否添加前缀
     * @param provider 是否使用语言提供者
     * @param parseMode 解析模式，null为默认解析模式
     * @param formatText 格式化文本
     * @param params 参数
     */
    fun printf(
        sendTo: CommandSender?, prefix: Boolean, provider: Boolean,
        parseMode: ComponentParseMode?, formatText: String, vararg params: Any?
    ): IMessager
    
    /**
     * 格式化并输出消息的重载方法
     */
    fun printf(prefix: Boolean, provider: Boolean, formatText: String, vararg params: Any?): IMessager =
        printf(sendTo = null, prefix = prefix, provider = provider, null, formatText = formatText, *params)

    fun printf(sendTo: CommandSender?, prefix: Boolean, formatText: String, vararg params: Any?): IMessager =
        printf(sendTo = sendTo, prefix = prefix, provider = true, null, formatText = formatText, *params)

    fun printf(sendTo: CommandSender?, formatText: String, vararg params: Any?): IMessager =
        printf(sendTo = sendTo, prefix = sendTo != null && sendTo !is ConsoleCommandSender, provider = true, null, formatText = formatText, *params)

    fun printf(formatText: String, vararg params: Any?): IMessager =
        printf(sendTo = null, prefix = false, provider = true, null, formatText = formatText, *params)
    
    /**
     * 设置向玩家输出信息时的前缀
     * @param prefix 前缀字符串
     */
    fun setPlayerPrefix(prefix: String): IMessager
    
    /**
     * 设置向控制台输出信息时的前缀
     * @param prefix 前缀字符串
     */
    fun setConsolePrefix(prefix: String): IMessager
    
    /**
     * 设置多语言服务提供者
     * @param languageProvider 多语言服务提供者，需实现LanguageProvider接口
     */
    fun setLanguageProvider(languageProvider: LanguageProvider?): IMessager
    
    /**
     * 获取当前设置的语言提供者
     */
    fun getLanguageProvider(): LanguageProvider?
}