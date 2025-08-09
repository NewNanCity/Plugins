package city.newnan.gui.manager.text

import city.newnan.core.utils.text.ComponentParseMode
import net.kyori.adventure.text.Component
import java.text.MessageFormat

interface IGuiTextPreprocessor {
    fun processPlain(text: String, vararg args: Any?): String
    fun processLegacy(text: String, parseMode: ComponentParseMode, vararg args: Any?): String
    fun processComponent(text: String, parseMode: ComponentParseMode, vararg args: Any?): Component
}

class NoOpGuiTextPreprocessor : IGuiTextPreprocessor {
    override fun processPlain(text: String, vararg args: Any?): String {
        return if (args.isEmpty()) text else
            MessageFormat.format(text, *args.map { it.toString() }.toTypedArray())
    }
    override fun processLegacy(text: String, parseMode: ComponentParseMode, vararg args: Any?): String {
        return if (args.isEmpty()) text else
            MessageFormat.format(text, *args.map { it.toString() }.toTypedArray())
    }
    override fun processComponent(text: String, parseMode: ComponentParseMode, vararg args: Any?): Component {
        return Component.text(text)
    }
}
