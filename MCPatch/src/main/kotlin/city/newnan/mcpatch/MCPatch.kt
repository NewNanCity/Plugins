package city.newnan.mcpatch

import city.newnan.violet.message.MessageManager
import city.newnan.mcpatch.addon.AntiWorldDownloader
import city.newnan.mcpatch.addon.Contraband
import city.newnan.mcpatch.addon.DispenserPatch
import me.lucko.helper.plugin.ExtendedJavaPlugin


class MCPatch : ExtendedJavaPlugin() {
    val messageManager: MessageManager by lazy { MessageManager(this) setPlayerPrefix "[MCPatch]" }
    companion object {
        lateinit var INSTANCE: MCPatch
    }
    init { INSTANCE = this }

    private val addons = hashMapOf<IMCPatchAddon, Boolean>(
        AntiWorldDownloader to true,
        Contraband to true,
    )

    override fun enable() {
        // Plugin startup logic
        addons.forEach { (addon, enable) -> if (enable) addon.enable() }
    }
}
