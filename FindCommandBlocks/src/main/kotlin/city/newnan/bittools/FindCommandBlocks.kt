package city.newnan.bittools

import city.newnan.violet.config.ConfigManager
import city.newnan.violet.message.MessageManager
import co.aikar.commands.PaperCommandManager
import me.lucko.helper.plugin.ExtendedJavaPlugin

class FindCommandBlocks : ExtendedJavaPlugin() {
    companion object {
        lateinit var INSTANCE: FindCommandBlocks
            private set
    }
    init { INSTANCE = this }

    internal val configManager: ConfigManager by lazy { ConfigManager(this) }
    internal val messageManager: MessageManager by lazy { MessageManager(this) }
    private val commandManager: PaperCommandManager by lazy { PaperCommandManager(this) }

    override fun enable() {

    }
}