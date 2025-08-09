package city.newnan.externalbook

import city.newnan.core.base.BasePlugin
import city.newnan.core.config.CorePluginConfig
import city.newnan.config.extensions.configManager
import city.newnan.core.event.subscribeEvent
import city.newnan.core.scheduler.runAsync
import city.newnan.core.scheduler.runSync
import city.newnan.externalbook.book.Book
import city.newnan.externalbook.book.Librarian
import city.newnan.externalbook.book.findBookID
import city.newnan.externalbook.book.impl.JsonLibrarian
import city.newnan.externalbook.book.impl.MySqlLibrarian
import city.newnan.externalbook.book.initializeBookNbtKey
import city.newnan.externalbook.book.readBook
import city.newnan.externalbook.book.updateBook
import city.newnan.externalbook.book.updateBookID
import city.newnan.externalbook.commands.CommandRegistry
import city.newnan.externalbook.config.ExternalBookConfig
import city.newnan.externalbook.config.StorageMode
import city.newnan.externalbook.i18n.LanguageKeys
import city.newnan.i18n.extensions.setupLanguageManager
import com.github.f4b6a3.ulid.Ulid
import org.bukkit.Material
import org.bukkit.block.Lectern
import org.bukkit.entity.EntityType
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta
import java.util.*

/**
 * 外部书籍插件主类
 *
 * 现代化的书籍管理插件，提供书籍导入、导出、发布和管理功能
 * 基于项目标准架构重构，提供完整的生命周期管理和资源管理
 *
 * @author NewNanCity
 * @since 1.0.0
 */
class ExternalBookPlugin : BasePlugin() {

    companion object {
        lateinit var instance: ExternalBookPlugin
            private set
    }

    var librarian: Librarian? = null
    private lateinit var commandRegistry: CommandRegistry

    override fun getCoreConfig(): CorePluginConfig {
        return getPluginConfig().getCoreConfig()
    }

    fun getPluginConfig(): ExternalBookConfig {
        configManager.touchWithMerge("config.yml", createBackup = true)
        return configManager.parse<ExternalBookConfig>("config.yml")
    }

    override fun onPluginLoad() {
        instance = this
        logger.info("ExternalBook Plugin loading...")

        // 初始化NBT键
        initializeBookNbtKey(this)
    }

    override fun onPluginEnable() {
        logger.info("ExternalBook Plugin enabling...")

        try {
            // 注册事件处理器（不可重载的功能）
            registerEventHandlers()

            // 初始化命令注册器
            commandRegistry = CommandRegistry(this)

            // 调用重载方法处理可重载的功能
            reloadPlugin()

            logger.info("ExternalBook Plugin enabled successfully")

        } catch (e: Exception) {
            logger.error("ExternalBook Plugin enable failed", e)
            server.pluginManager.disablePlugin(this)
        }
    }

    override fun reloadPlugin() {
        try {
            // 1. 清理配置缓存（必需，否则无法从磁盘加载最新的文件）
            configManager.clearCache()

            // 2. 重新设置语言管理器（必需）
            setupLanguageManager(
                languageFiles = mapOf(
                    Locale.SIMPLIFIED_CHINESE to "lang/zh_CN.yml",
                    Locale.US to "lang/en_US.yml"
                ),
                majorLanguage = Locale.SIMPLIFIED_CHINESE,
                defaultLanguage = Locale.US
            )

            // 3. 重新加载 Librarian（如果已初始化）
            val currentLibrarianType = getPluginConfig().storage.mode
            if (librarian != null && currentLibrarianType != librarian!!.mode) {
                librarian?.close()
                librarian = null
            }
            if (librarian == null) {
                librarian = createLibrarian()
            }
            librarian?.reload()

            // 4. 重载所有BaseModule子模块（必需）
            super.reloadPlugin()

            logger.info(LanguageKeys.Core.Plugin.RELOADED)
        } catch (e: Exception) {
            logger.error(LanguageKeys.Core.Plugin.RELOAD_FAILED, e)
            throw e
        }
    }

    override fun onPluginDisable() {
        // 所有绑定的资源会自动清理
    }

    /**
     * 根据配置创建 Librarian 实例
     */
    private fun createLibrarian(): Librarian {
        val config = getPluginConfig()

        return when (config.storage.mode) {
            StorageMode.DATABASE -> {
                logger.info("Using MySQL storage for books")
                MySqlLibrarian(this)
            }
            StorageMode.FILE -> {
                logger.info("Using JSON file storage for books")
                JsonLibrarian(this)
            }
        }
    }

    /**
     * 尝试打开书籍
     *
     * @param player 玩家
     * @param bookId 书籍UUID
     * @param itemStack 物品堆栈
     * @param oldUuid 旧的UUID（用于更新）
     * @param callback 回调函数，接收是否成功打开书籍的结果
     */
    fun tryOpenBook(
        player: Player,
        bookId: Ulid,
        itemStack: ItemStack,
        oldUuid: UUID? = null,
        callback: ((Book?) -> Unit)? = null
    ) {
        // 异步执行数据库查询，避免阻塞主线程
        runAsync {
            try {
                val book = librarian?.get(bookId)
                if (book != null) {
                    // 回到主线程进行书籍打开操作
                    runSync {
                        try {
                            // 打开书籍阅读界面
                            book.readBook(player)
                            if (oldUuid != null) {
                                itemStack.updateBookID(oldUuid, bookId)
                            }
                            logger.debug("Player ${player.name} read book: ${book.title}")
                            callback?.invoke(book)
                        } catch (e: Exception) {
                            logger.error("Failed to open book for player ${player.name}", e)
                            messager.printf(player, LanguageKeys.Events.BOOK_OPEN_FAILED)
                            callback?.invoke(null)
                        }
                    }
                } else {
                    // 书籍不存在
                    runSync {
                        logger.warn("Player ${player.name} tried to read non-existent book: $bookId")
                        messager.printf(player, LanguageKeys.Events.BOOK_NOT_FOUND)
                        callback?.invoke(null)
                    }
                }
            } catch (e: Exception) {
                // 数据库查询异常
                runSync {
                    logger.error("Failed to query book for player ${player.name}", e)
                    messager.printf(player, LanguageKeys.Events.BOOK_QUERY_FAILED)
                    callback?.invoke(null)
                }
            }
        }
    }

    /**
     * 注册事件处理器
     */
    private fun registerEventHandlers() {
        logger.info("Registering event handlers...")

        // 处理玩家拿着书，然后对着 空气/非容器物品/上面放了书的讲台 右键
        subscribeEvent<PlayerInteractEvent> {
            priority(EventPriority.HIGHEST)
            filter { it.hasItem() } // 检查手里是否有物品
            filter { it.useItemInHand() != Event.Result.DENY } // 检查是否允许使用手里物品
            filter {
                it.action == Action.RIGHT_CLICK_AIR ||
                        (it.action == Action.RIGHT_CLICK_BLOCK && it.clickedBlock!!.state !is InventoryHolder) ||
                        (it.action == Action.RIGHT_CLICK_BLOCK && it.clickedBlock!!.type == Material.LECTERN && (it.clickedBlock!!.state as Lectern).inventory.getItem(0).run {
                            this != null && this.type != Material.AIR
                        })
            }
            handler { event ->
                // 查找书籍ID
                val bookItem = event.item!!
                val ids = bookItem.findBookID(writable = false)
                if (ids.first == null && ids.second == null) {
                    return@handler
                }
                event.isCancelled = true
                event.setUseInteractedBlock(Event.Result.DENY)
                event.setUseItemInHand(Event.Result.DENY)
                runAsync {
                    val uuid = ids.first
                    val ulid = ids.second ?: uuid?.let { librarian?.getUlid(it) }
                    if (ulid != null) {
                        tryOpenBook(event.player, ulid, bookItem, uuid) { book ->
                            if (book == null) return@tryOpenBook
                            val bookMeta = bookItem.itemMeta as BookMeta? ?: return@tryOpenBook
                            runSync {
                                val newMeta = bookMeta.updateBook(book, addModifyInfo = false)
                                event.hand?.let { p0 ->
                                    if (event.player.inventory.getItem(p0) != bookItem) return@let
                                    bookItem.itemMeta = newMeta
                                    event.player.inventory.setItem(p0, bookItem)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 处理玩家拿着书之外的东西右键讲台上的书籍阅读
        subscribeEvent<PlayerInteractEvent> {
            priority(EventPriority.HIGHEST)
            filter { it.useInteractedBlock() != Event.Result.DENY }
            // 检查是否是右键点击讲台且玩家没有潜行
            filter { !it.player.isSneaking }
            filter { it.action == Action.RIGHT_CLICK_BLOCK && it.clickedBlock?.type == Material.LECTERN }
            filter { it.item?.type != Material.WRITTEN_BOOK }
            handler { event ->
                val lectern = event.clickedBlock!!.state as Lectern
                val bookItem = lectern.inventory.getItem(0) ?: return@handler
                val ids = bookItem.findBookID(writable = false)
                if (ids.first == null && ids.second == null) {
                    return@handler
                }
                event.isCancelled = true
                event.setUseInteractedBlock(Event.Result.DENY)
                event.setUseItemInHand(Event.Result.DENY)
                runAsync {
                    val uuid = ids.first
                    val ulid = ids.second ?: uuid?.let { librarian?.getUlid(it) }
                    if (ulid != null) {
                        tryOpenBook(event.player, ulid, bookItem, uuid) { book ->
                            if (book == null) return@tryOpenBook
                            val bookMeta = bookItem.itemMeta as BookMeta? ?: return@tryOpenBook
                            runSync {
                                val newMeta = bookMeta.updateBook(book, addModifyInfo = false)
                                event.hand?.let { p0 ->
                                    if (event.player.inventory.getItem(p0) != bookItem) return@let
                                    bookItem.itemMeta = newMeta
                                    event.player.inventory.setItem(p0, bookItem)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 处理物品框中的书籍阅读
        subscribeEvent<PlayerInteractEntityEvent> {
            priority(EventPriority.HIGHEST)
            filter { !it.isCancelled }
            // 检查是否是右键点击物品框且玩家没有潜行
            filter { !it.player.isSneaking }
            filter { it.rightClicked.type == EntityType.ITEM_FRAME || it.rightClicked.type == EntityType.GLOW_ITEM_FRAME }
            handler { event ->
                val itemFrame = event.rightClicked as ItemFrame
                val bookItem = itemFrame.item
                val ids = bookItem.findBookID(writable = false)
                if (ids.first == null && ids.second == null) {
                    return@handler
                }
                event.isCancelled = true
                runAsync {
                    val ids = bookItem.findBookID(writable = false)
                    val uuid = ids.first
                    val ulid = ids.second ?: uuid?.let { librarian?.getUlid(it) }
                    if (ulid != null) {
                        tryOpenBook(event.player, ulid, bookItem, uuid) { book ->
                            if (book == null) return@tryOpenBook
                            val bookMeta = bookItem.itemMeta as BookMeta? ?: return@tryOpenBook
                            runSync {
                                val newMeta = bookMeta.updateBook(book, addModifyInfo = false)
                                event.hand.let { p0 ->
                                    if (event.player.inventory.getItem(p0) != bookItem) return@let
                                    bookItem.itemMeta = newMeta
                                    event.player.inventory.setItem(p0, bookItem)
                                }
                            }
                        }
                    }
                }
            }
        }
        logger.info("Event handlers registered")
    }
}
