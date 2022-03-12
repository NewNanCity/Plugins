package city.newnan.betterbook

import city.newnan.violet.config.ConfigManager
import city.newnan.violet.message.MessageManager
import co.aikar.commands.PaperCommandManager
import me.lucko.helper.Events
import me.lucko.helper.plugin.ExtendedJavaPlugin
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Lectern
import org.bukkit.entity.EntityType
import org.bukkit.entity.ItemFrame
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerEditBookEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta

class BetterBook : ExtendedJavaPlugin() {
    init { INSTANCE = this }
    companion object {
        lateinit var INSTANCE: BetterBook
            private set
    }

    internal val configManager: ConfigManager by lazy { ConfigManager(this).also { it.bindWith(this) } }
    internal val messageManager: MessageManager by lazy { MessageManager(this).also { it.bindWith(this) } }
    private val commandManager: PaperCommandManager by lazy { PaperCommandManager(this) }

    override fun enable() {
        configManager.startCleanService() touch "config.yml"
        messageManager setPlayerPrefix "[牛腩书局]"
        commandManager.registerCommand(BetterBookCommand)
        Librarian.enable().bindWith(this)
        Events.subscribe(PlayerInteractEvent::class.java, EventPriority.MONITOR)
            .filter { it.hasItem() }
            .filter { it.action == Action.RIGHT_CLICK_AIR || it.action == Action.RIGHT_CLICK_BLOCK }
            .handler { event ->
                if (event.action == Action.RIGHT_CLICK_BLOCK &&
                    event.material == Material.WRITTEN_BOOK &&
                    event.clickedBlock!!.type == Material.LECTERN) return@handler;
                event.item!!.findBookUUID(writable = false)?.also{
                    Librarian[it]?.readBook(event.player)?.also { event.isCancelled = true }
                }
            }
            .bindWith(this)

        Events.subscribe(PlayerInteractEvent::class.java, EventPriority.MONITOR)
            .filter { it.action == Action.RIGHT_CLICK_BLOCK && it.clickedBlock!!.type == Material.LECTERN }
            .filter { !it.player.isSneaking }
            .handler { event ->
                (event.clickedBlock!!.state as Lectern).inventory.getItem(0)?.findBookUUID()
                    ?.also { Librarian[it]?.readBook(event.player)?.also { event.isCancelled = true } }
            }
            .bindWith(this)

        Events.subscribe(PlayerInteractEntityEvent::class.java, EventPriority.MONITOR)
            .filter { !it.player.isSneaking }
            .filter { it.rightClicked.type == EntityType.ITEM_FRAME }
            .handler { event ->
                (event.rightClicked as ItemFrame).item.findBookUUID()?.also {
                    Librarian[it]?.readBook(event.player)?.also { event.isCancelled = true }
                }
            }
            .bindWith(this)
    }

    override fun disable() {
        commandManager.unregisterCommand(BetterBookCommand)
    }
}