package city.newnan.mcpatch.addon

import com.google.common.io.ByteStreams
import city.newnan.mcpatch.PluginMain
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener


object AntiWorldDownloader: PluginMessageListener, IMCPatchAddon {
    override val addonName = "AntiWorldDownloader"

    override fun enable() {
        PluginMain.INSTANCE.let {
            Bukkit.getMessenger().also { messenger ->
                messenger.registerOutgoingPluginChannel(it, "WDL|CONTROL")
                messenger.registerIncomingPluginChannel(it, "WDL|INIT", this)
                messenger.registerIncomingPluginChannel(it, "WDL|REQUEST", this)
            }
        }
    }

    override fun close() {
        PluginMain.INSTANCE.let {
            Bukkit.getMessenger().also { messenger ->
                messenger.unregisterOutgoingPluginChannel(it, "WDL|CONTROL")
                messenger.unregisterIncomingPluginChannel(it, "WDL|INIT", this)
                messenger.unregisterIncomingPluginChannel(it, "WDL|REQUEST", this)
            }
        }
    }

    /**
     * A method that will be thrown when a PluginMessageSource sends a plugin
     * message on a registered channel.
     *
     * @param channel Channel that the message was sent through.
     * @param player  Source of the message.
     * @param message The raw message that was sent.
     */
    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        if ("WDL|INIT" == channel || "WDL|REQUEST" == channel) {
            PluginMain.INSTANCE.let {
                var output = ByteStreams.newDataOutput()
                output.writeInt(1)
                output.writeBoolean(false)
                output.writeInt(0)
                output.writeBoolean(false)
                output.writeBoolean(false)
                output.writeBoolean(false)
                output.writeBoolean(false)
                player.sendPluginMessage(it, "WDL|CONTROL", output.toByteArray())

                output = ByteStreams.newDataOutput()
                output.writeInt(3)
                output.writeBoolean(false)
                output.writeUTF("请勿使用WDL插件")
                player.sendPluginMessage(it, "WDL|CONTROL", output.toByteArray())
            }
            player.kickPlayer("请勿使用WDL插件")
            PluginMain.INSTANCE.messageManager.printf("玩家 {0} 使用了WDL", player.name)
        }
    }
}