package city.newnan.betterbook

import city.newnan.betterbook.book.Librarian
import city.newnan.betterbook.book.applyBook
import city.newnan.betterbook.book.findBookUUID
import city.newnan.violet.config.ConfigManager2
import city.newnan.violet.message.MessageManager
import co.aikar.commands.PaperCommandManager
import me.lucko.helper.Events
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.plugin.ExtendedJavaPlugin
import org.bukkit.Material
import org.bukkit.block.Lectern
import org.bukkit.entity.EntityType
import org.bukkit.entity.ItemFrame
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.meta.BookMeta

class PluginMain : ExtendedJavaPlugin() {
    companion object {
        lateinit var INSTANCE: PluginMain
            private set
    }
    init { INSTANCE = this }

    val configManager: ConfigManager2 by lazy {
        ConfigManager2(this).apply {
            setCache(ConfigManager2.CacheType.GreedyDualSize, 16)
        }
    }
    internal val messageManager: MessageManager by lazy { MessageManager(this) }
    private val commandManager: PaperCommandManager by lazy { PaperCommandManager(this) }

    override fun enable() {
        messageManager setPlayerPrefix "§7[§6牛腩书局§7] §f"
        commandManager.enableUnstableAPI("help")
        commandManager.registerCommand(Commands)
        Librarian.enable()
        Events.subscribe(PlayerInteractEvent::class.java, EventPriority.MONITOR)
            .filter(EventFilters.ignoreCancelled())
            .filter { it.hasItem() }
            .filter { it.action == Action.RIGHT_CLICK_AIR || it.action == Action.RIGHT_CLICK_BLOCK }
            .handler { event ->
                if (event.action == Action.RIGHT_CLICK_BLOCK &&
                    event.material == Material.WRITTEN_BOOK &&
                    event.clickedBlock!!.type == Material.LECTERN) return@handler;
                event.item!!.findBookUUID(writable = false)?.also{ uuid ->
                    Librarian[uuid]?.also {
                        it.readBook(event.player)
                        event.isCancelled = true
                        event.item!!.itemMeta = (event.item!!.itemMeta as BookMeta?)
                            ?.applyBook(it, uuid, toWrittenBook = true, addModifyInfo = false)
                    }
                }
            }
            .bindWith(this)

        Events.subscribe(PlayerInteractEvent::class.java, EventPriority.MONITOR)
            .filter(EventFilters.ignoreCancelled())
            .filter { it.action == Action.RIGHT_CLICK_BLOCK && it.clickedBlock!!.type == Material.LECTERN }
            .filter { !it.player.isSneaking }
            .handler { event ->
                (event.clickedBlock!!.state as Lectern).inventory.getItem(0)?.findBookUUID()
                    ?.also { Librarian[it]?.readBook(event.player)?.also { event.isCancelled = true } }
            }
            .bindWith(this)

        Events.subscribe(PlayerInteractEntityEvent::class.java, EventPriority.MONITOR)
            .filter(EventFilters.ignoreCancelled())
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
        commandManager.unregisterCommands()
    }
}