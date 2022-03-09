package city.newnan.mcpatch

import city.newnan.violet.message.MessageManager
import city.newnan.mcpatch.addon.AntiWorldDownloader
import city.newnan.mcpatch.addon.Contraband
import city.newnan.mcpatch.addon.DispenserPatch
import me.lucko.helper.plugin.ExtendedJavaPlugin


class MCPatch : ExtendedJavaPlugin() {
    public override fun enable() {
        INSTANCE = this
        messageManager = MessageManager(this)
            .setPlayerPrefix("[MCPatch]")
        // Plugin startup logic
        DispenserPatch.init()
        AntiWorldDownloader.init()
        Contraband.init()
    }

    public override fun disable() {
        // Plugin shutdown logic
    }

    companion object {
        var INSTANCE: MCPatch? = null
        var messageManager: MessageManager? = null
    }
}
