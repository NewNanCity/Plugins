package city.newnan.mcpatch

import city.newnan.violet.message.MessageManager
import city.newnan.mcpatch.addon.AntiWorldDownloader
import city.newnan.mcpatch.addon.Contraband
import me.lucko.helper.plugin.ExtendedJavaPlugin


class PluginMain : ExtendedJavaPlugin() {
    val messageManager: MessageManager by lazy { MessageManager(this) setPlayerPrefix "[MCPatch]" }
    companion object {
        lateinit var INSTANCE: PluginMain
    }
    init { INSTANCE = this }

    private val addons = hashMapOf(
        AntiWorldDownloader to true,
        Contraband to true,
    )

    override fun enable() {
        // Plugin startup logic
        addons.forEach { (addon, enable) -> if (enable) addon.enable() }
    }
}
