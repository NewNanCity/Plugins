package city.newnan.core.utils.text

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * Component文本处理器
 *
 * 提供对现代化Component API的支持，兼容Kyori Text和Adventure API。
 * 由于不同版本的Bukkit可能使用不同的Component实现，这里提供抽象接口。
 *
 * @author NewNanCity
 * @since 1.0.0
 */
object ComponentProcessor {
    const val SECTION_CHAR = '\u00A7'

    const val AMPERSAND_CHAR = '&'

    private val legacyComponentSerializers = mutableMapOf<Char, LegacyComponentSerializer>()

    /**
     * 检查是否支持Component API
     */
    val isComponentSupported: Boolean by lazy {
        try {
            Class.forName("net.kyori.adventure.text.Component")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    /**
     * 自动解析文本为Component对象
     *
     * @param text 待解析的文本
     * @param parseMode 解析模式（默认为自动检测）
     * @return Component对象
     */
    fun autoParse(text: String, parseMode: ComponentParseMode = ComponentParseMode.Auto): Component {
        return when (if (parseMode == ComponentParseMode.Auto) ComponentParseModeDetector.detect(text) else parseMode) {
            ComponentParseMode.Plain -> Component.text(text)
            ComponentParseMode.Legacy -> fromLegacy(colorizeLegacy(text))
            ComponentParseMode.MiniMessage -> if (isComponentSupported) fromMiniMessage(text) else Component.text(text)
            ComponentParseMode.Auto -> Component.text(text) // Never happen
        }
    }

    /**
     * 从 Legacy 文本创建Component
     *
     * @param input Legacy格式的文本
     * @param character 颜色代码字符（默认为§）
     * @return Component对象
     */
    fun fromLegacy(input: String, character: Char = SECTION_CHAR): Component =
        legacyComponentSerializers.getOrPut(character) {
            LegacyComponentSerializer.legacy(character)
        }.deserialize(input)

    /**
     * 将 Component 转换为 Legacy 文本
     *
     * @param component Component对象
     * @param character 颜色代码字符（默认为§）
     * @return Legacy格式的文本
     */
    fun toLegacy(component: Component, character: Char = SECTION_CHAR): String =
        legacyComponentSerializers.getOrPut(character) {
            LegacyComponentSerializer.legacy(character)
        }.serialize(component)

    /**
     * 从 MiniMessage格式的文本创建Component
     *
     * @param input MiniMessage格式的文本
     * @return Component对象
     */
    fun fromMiniMessage(input: String, parser: MiniMessage? = null): Component =
        (parser ?: MiniMessage.miniMessage()).deserialize(input)

    /**
     * 将 Component 转换为 MiniMessage 格式的文本
     *
     * @param component Component对象
     * @return MiniMessage格式的文本
     */
    fun toMiniMessage(component: Component, serializer: MiniMessage? = null): String =
        (serializer ?: MiniMessage.miniMessage()).serialize(component)

    /**
     * 将 Component 转换为纯文本
     *
     * @param component Component对象
     * @return 纯文本
     */
    fun toPlain(component: Component): String {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component)
    }

    /**
     * 移除所有 Legacy 格式代码
     *
     * @param text 待处理的文本
     * @return 处理后的文本
     */
    fun stripLegacySymbol(text: String): String {
        val result = StringBuilder()
        var i = 0
        while (i < text.length) {
            if (i < text.length - 1 && (text[i] == SECTION_CHAR || text[i] == AMPERSAND_CHAR) && ComponentParseModeDetector.isValidLegacyColorCodeRange(text[i + 1])) {
                i += 2 // 跳过颜色代码
            } else {
                result.append(text[i++])
            }
        }
        return result.toString()
    }

    /**
     * 转换 Legacy 格式代码
     *
     * @param from 源字符
     * @param to 目标字符
     * @param textToTranslate 待处理的文本
     * @return 处理后的文本
     */
    fun translateLegacySymbol(from: Char, to: Char, textToTranslate: String): String {
        val chars = textToTranslate.toCharArray()
        var i = 0
        val end = chars.size - 1
        while (i < end) {
            if (chars[i] == from && ComponentParseModeDetector.isValidLegacyColorCodeRange(chars[i + 1])) {
                chars[i] = to
                i += 2
            } else {
                i++
            }
        }
        return String(chars)
    }

    fun colorizeLegacy(text: String): String {
        return translateLegacySymbol(AMPERSAND_CHAR, SECTION_CHAR, text)
    }
}

// ==================== 扩展函数 ====================

/**
 * 将字符串转换为Component对象
 *
 * @receiver 待转换的字符串
 * @return Component对象
 */
fun String.toComponent(parseMode: ComponentParseMode = ComponentParseMode.Auto): Component =
    ComponentProcessor.autoParse(this, parseMode)

/**
 * 将Component对象转换为Legacy格式的字符串
 *
 * @receiver 待转换的Component对象
 * @param character 颜色代码字符（默认为§）
 * @return Legacy格式的字符串
 */
fun Component.toLegacy(character: Char = ComponentProcessor.SECTION_CHAR): String = ComponentProcessor.toLegacy(this, character)

/**
 * 将Component对象转换为纯文本
 *
 * @receiver 待转换的Component对象
 * @return 纯文本
 */
fun Component.toPlain(): String = ComponentProcessor.toPlain(this)

/**
 * 将Component对象转换为MiniMessage格式的字符串
 *
 * @receiver 待转换的Component对象
 * @return MiniMessage格式的字符串
 */
fun Component.toMiniMessage(): String = ComponentProcessor.toMiniMessage(this)

fun String.toLegacy(parseMode: ComponentParseMode) =
    if (parseMode == ComponentParseMode.Legacy) ComponentProcessor.colorizeLegacy(this)
    else if (ComponentProcessor.isComponentSupported) toComponent(parseMode).toLegacy()
    else ComponentProcessor.colorizeLegacy(this)
