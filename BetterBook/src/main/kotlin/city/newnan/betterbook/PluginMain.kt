package city.newnan.betterbook

import city.newnan.betterbook.book.Librarian
import city.newnan.betterbook.book.applyBook
import city.newnan.betterbook.book.findBookUUID
import city.newnan.violet.config.ConfigManager2
import city.newnan.violet.gui.GuiManager
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
import java.util.*

class PluginMain : ExtendedJavaPlugin() {
    companion object {
        lateinit var INSTANCE: PluginMain
            private set
    }
    init { INSTANCE = this }

    val configManager: ConfigManager2 by lazy {
        ConfigManager2(this).apply {
            setCache(ConfigManager2.CacheType.LFU, 16)
        }
    }
    val gui: GuiManager by lazy { GuiManager(this) }
    val message: MessageManager by lazy { MessageManager(this) }
    private val command: PaperCommandManager by lazy { PaperCommandManager(this) }

    override fun enable() {
        message setPlayerPrefix "§7[§6牛腩书局§7] §f"

        command.enableUnstableAPI("help")
        command.registerCommand(Commands)
        command.locales.setDefaultLocale(Locale.SIMPLIFIED_CHINESE)

        Librarian.enable()

        // 直接阅读
        Events.subscribe(PlayerInteractEvent::class.java, EventPriority.MONITOR)
            .filter(EventFilters.ignoreCancelled())
            .filter { it.hasItem() }
            .filter { it.action == Action.RIGHT_CLICK_AIR
                    || (it.action == Action.RIGHT_CLICK_BLOCK && it.clickedBlock!!.type != Material.LECTERN) }
            .handler { event ->
                event.item!!.findBookUUID(writable = false)?.also{ bookId ->
                    Librarian[bookId]?.also {
                        it.readBook(event.player)
                        event.item!!.itemMeta = (event.item!!.itemMeta as BookMeta?)
                            ?.applyBook(it, bookId, toWrittenBook = true, addModifyInfo = false)
                    } ?: run {
                        message.printf(event.player, "§c书目已不存在,因此被销毁!")
                        event.player.inventory.removeItem(event.item!!)
                    }
                    event.isCancelled = true
                }
            }
            .bindWith(this)

        // 讲台阅读
        Events.subscribe(PlayerInteractEvent::class.java, EventPriority.MONITOR)
            .filter(EventFilters.ignoreCancelled())
            .filter { !it.player.isSneaking }
            .filter { it.action == Action.RIGHT_CLICK_BLOCK && it.clickedBlock!!.type == Material.LECTERN }
            .handler { event ->
                (event.clickedBlock!!.state as Lectern).inventory.getItem(0)?.findBookUUID()?.also { bookId ->
                    Librarian[bookId]?.readBook(event.player) ?: run {
                        message.printf(event.player, "§c书目已不存在,因此被销毁!")
                        (event.clickedBlock!!.state as Lectern).inventory.setItem(0, null)
                    }
                    event.isCancelled = true
                }
            }
            .bindWith(this)

        // 物品框阅读
        Events.subscribe(PlayerInteractEntityEvent::class.java, EventPriority.MONITOR)
            .filter(EventFilters.ignoreCancelled())
            .filter { !it.player.isSneaking }
            .filter { it.rightClicked.type == EntityType.ITEM_FRAME }
            .handler { event ->
                (event.rightClicked as ItemFrame).item.findBookUUID()?.also { bookId ->
                    Librarian[bookId]?.readBook(event.player) ?: run {
                        message.printf(event.player, "§c书目已不存在,因此被销毁!")
                        (event.rightClicked as ItemFrame).setItem(null)
                    }
                    event.isCancelled = true
                }
            }
            .bindWith(this)
    }

    override fun disable() {
        command.unregisterCommands()
        configManager.cache?.clear()
    }
}