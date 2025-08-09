package city.newnan.gui.manager

import city.newnan.core.base.BaseModule
import city.newnan.core.base.BasePlugin
import city.newnan.core.terminable.CompositeTerminable
import city.newnan.core.terminable.Terminable
import city.newnan.core.terminable.TerminableConsumer
import city.newnan.core.utils.text.ComponentParseMode

import city.newnan.gui.manager.logging.GuiLogger
import city.newnan.gui.manager.logging.IGuiLoggerProvider
import city.newnan.gui.manager.logging.wrapCoreLogger
import city.newnan.gui.manager.scheduler.GuiScheduler
import city.newnan.gui.manager.text.IGuiTextPreprocessor
import city.newnan.gui.manager.text.NoOpGuiTextPreprocessor
import city.newnan.gui.page.*
import city.newnan.gui.session.Session
import city.newnan.gui.session.SessionStorage
import city.newnan.gui.util.getDefaultInventorySize
import city.newnan.gui.util.ItemUtil
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.plugin.Plugin

/**
 * GUI管理器
 *
 * 每个插件的GUI功能入口，负责：
 * - 与SessionStorage的集成
 * - Page创建和生命周期管理
 * - ChatInput功能
 * - 玩家退出事件处理
 * - 与BasePlugin的集成
 *
 * 特性：
 * - 继承TerminableConsumer，支持资源管理
 * - 自动注册到SessionStorage
 * - 支持跨插件GUI协作
 */
class GuiManager(
    val plugin: Plugin,
    private val loggerProvider: IGuiLoggerProvider,
    val textPreprocessor: IGuiTextPreprocessor = NoOpGuiTextPreprocessor(),
) : TerminableConsumer, Terminable {

    // =================== 别名构造器 ===================
    constructor(plugin: BasePlugin) : this(
        plugin,
        loggerProvider = wrapCoreLogger(plugin.logger),
        textPreprocessor = object : IGuiTextPreprocessor {
            override fun processPlain(text: String, vararg args: Any?): String {
                return plugin.stringFormatter.sprintfPlain(true, text, *args)
            }
            override fun processLegacy(text: String, parseMode: ComponentParseMode, vararg args: Any?): String =
                plugin.stringFormatter.sprintfLegacy(true, parseMode, text, *args)

            override fun processComponent(text: String, parseMode: ComponentParseMode, vararg args: Any?): Component =
                plugin.stringFormatter.sprintf(true, parseMode, text, *args)
        }
    )

    constructor(parentModule: BaseModule) : this(
        parentModule.bukkitPlugin,
        loggerProvider = wrapCoreLogger(parentModule.logger),
        textPreprocessor = object : IGuiTextPreprocessor {
            override fun processPlain(text: String, vararg args: Any?): String {
                return parentModule.messager.sprintfPlain(true, text, *args)
            }
            override fun processLegacy(text: String, parseMode: ComponentParseMode, vararg args: Any?): String =
                parentModule.messager.sprintfLegacy(true, parseMode, text, *args)

            override fun processComponent(text: String, parseMode: ComponentParseMode, vararg args: Any?): Component =
                parentModule.messager.sprintf(true, parseMode, text, *args)
        }
    )

    // =================== 核心属性 ===================

    // 资源管理器
    private val terminableRegistry = CompositeTerminable.create()

    // 管理器状态
    private var initialized = false
    private var closed = false

    val itemUtil: ItemUtil by lazy { ItemUtil(this) }

    // 日志记录器
    val logger by lazy { GuiLogger(loggerProvider) }

    // 任务调度器
    val scheduler by lazy { GuiScheduler(this) }

    // =================== 生命周期 ===================

    /**
     * 初始化管理器
     * 注册到SessionStorage，事件监听由SessionStorage统一管理
     */
    init {
        checkNotClosed()

        // 注册到SessionStorage，SessionStorage会管理全局事件监听器
        SessionStorage.registerGuiManager(this)

        initialized = true
        logger.logInfo("GuiManager initialized for plugin ${plugin.name}")
    }

    override fun close() {
        if (closed) return

        // 从SessionStorage注销
        SessionStorage.unregisterGuiManager(this)

        // 清理所有资源
        terminableRegistry.closeAndReportException()

        closed = true
        initialized = false

        logger.logInfo("GuiManager shutdown for plugin ${plugin.name}")
    }

    // =================== 业务方法 ===================

    /**
     * 获取玩家的默认session
     * 如果不存在则创建
     */
    fun getDefaultSession(player: Player): Session {
        return SessionStorage.getOrCreateSession(player, SessionStorage.DEFAULT_SESSION_NAME)
    }

    /**
     * 获取玩家的指定session
     * 如果不存在则创建
     */
    fun getSession(player: Player, sessionName: String): Session {
        return SessionStorage.getOrCreateSession(player, sessionName)
    }

    /**
     * 创建CHEST类型的页面
     */
    fun createChestPage(
        player: Player,
        title: String,
        size: Int,
        session: Session? = null,
        builder: ChestPage.() -> Unit = {}
    ): ChestPage =
        createChestPage(player, format(title), size, session, builder)

    /**
     * 创建CHEST类型的页面
     */
    fun createChestPage(
        player: Player,
        title: Component,
        size: Int,
        session: Session? = null,
        builder: ChestPage.() -> Unit = {}
    ): ChestPage {
        checkNotClosed()
        val actualSession = session ?: getDefaultSession(player)
        return ChestPage(player, actualSession, this, title, size, builder)
    }

    /**
     * 创建书本页面
     */
    fun createBookPage(
        player: Player,
        title: String,
        bookTitle: String = title,
        author: String = "Server",
        session: Session? = null,
        builder: BookPage.() -> Unit = {}
    ): BookPage =
        createBookPage(
            player,
            format(title),
            format(bookTitle),
            format(author),
            session,
            builder
        )

    /**
     * 创建书本页面
     */
    fun createBookPage(
        player: Player,
        title: Component,
        bookTitle: Component = title,
        author: Component,
        session: Session? = null,
        builder: BookPage.() -> Unit = {}
    ): BookPage {
        checkNotClosed()

        val actualSession = session ?: getDefaultSession(player)
        return BookPage(
            player,
            actualSession,
            this,
            title,
            bookTitle,
            author,
            false,
            builder
        )
    }

    /**
     * 创建指定类型的页面
     */
    fun createPage(
        player: Player,
        title: String,
        inventoryType: InventoryType,
        size: Int? = null,
        session: Session? = null,
        builder: BasePage.() -> Unit = {}
    ): BasePage =
        createPage(player, format(title), inventoryType, size, session, builder)

    /**
     * 创建指定类型的页面
     */
    fun createPage(
        player: Player,
        title: Component,
        inventoryType: InventoryType,
        size: Int? = null,
        session: Session? = null,
        builder: BasePage.() -> Unit = {}
    ): BasePage {
        checkNotClosed()

        val actualSession = session ?: getDefaultSession(player)

        return when (inventoryType) {
            InventoryType.CHEST -> {
                val actualSize = size ?: 27
                ChestPage(
                    player,
                    actualSession,
                    this,
                    title,
                    actualSize,
                    builder as ChestPage.() -> Unit
                )
            }
            // 其他所有支持的容器类型都使用BasePage
            InventoryType.DISPENSER,
            InventoryType.DROPPER,
            InventoryType.HOPPER,
            InventoryType.WORKBENCH,
            InventoryType.FURNACE,
            InventoryType.BLAST_FURNACE,
            InventoryType.SMOKER,
            InventoryType.BREWING,
            InventoryType.ANVIL,
            InventoryType.SMITHING,
            InventoryType.ENCHANTING,
            InventoryType.GRINDSTONE,
            InventoryType.CARTOGRAPHY,
            InventoryType.LOOM,
            InventoryType.STONECUTTER,
            InventoryType.BEACON,
            InventoryType.LECTERN,
            InventoryType.SHULKER_BOX,
            InventoryType.BARREL,
            InventoryType.ENDER_CHEST,
            InventoryType.COMPOSTER,
            InventoryType.CHISELED_BOOKSHELF,
            InventoryType.JUKEBOX -> {
                // 使用BasePage的匿名子类实现
                object : BasePage(
                    player,
                    actualSession,
                    this,
                    title,
                    inventoryType,
                    size ?: getDefaultInventorySize(inventoryType),
                    builder
                ) {
                    override val inventory: Inventory by lazy {
                        Bukkit.createInventory(null, inventoryType, title)
                    }
                }
            }
            // 不支持的容器类型
            InventoryType.CREATIVE,
            InventoryType.CRAFTING,
            InventoryType.MERCHANT,
            InventoryType.PLAYER -> {
                throw UnsupportedOperationException("Inventory type $inventoryType cannot be created (system-only)")
            }
            else -> {
                throw UnsupportedOperationException("Inventory type $inventoryType is not supported")
            }
        }
    }

    /**
     * 打开页面到默认session
     * 等同于 getDefaultSession(player).push(page) + session.show()
     */
    fun openPageOnDefaultSession(page: Page) {
        checkNotClosed()

        val session = getDefaultSession(page.player)
        session.push(page)
        session.show()
    }

    /**
     * 获取聊天输入
     * @param player 玩家
     * @param hide 是否隐藏当前GUI
     * @param handler 获取到输入后的回调函数，返回true则结束获取输入，返回false则继续获取输入并处理
     * @return 如果先前已经有其他输入请求，则不会开始获取输入，而返回false，反之则返回true，开始等待输入
     */
    fun chatInput(player: Player, hide: Boolean = true, handler: (input: String) -> Boolean): Boolean {
        checkNotClosed()

        val previousHandler = SessionStorage.getChatInputHandler(player)
        return if (previousHandler == null) {
            if (hide) {
                val session = SessionStorage.getSession(player)
                session?.hide()
            }
            SessionStorage.setChatInputHandler(player, handler)
            true
        } else {
            false
        }
    }

    // TerminableConsumer 实现
    override fun <T : AutoCloseable> bind(terminable: T): T {
        return terminableRegistry.bind(terminable)
    }

    // Terminable 实现
    override fun isClosed(): Boolean = closed

    /**
     * 检查管理器是否未关闭
     */
    private fun checkNotClosed() {
        if (closed) {
            throw IllegalStateException("GuiManager is closed")
        }
    }

    override fun toString(): String {
        return "GuiManager(plugin=${plugin.name}, initialized=$initialized, closed=$closed)"
    }

    /**
     * 格式化文本，包括语言映射、参数替换和自动格式解析
     *
     * @param text 待格式化的文本
     * @param args 格式化参数
     * @param parseMode 解析模式
     * @return 格式化后的组件
     */
    fun format(text: String, vararg args: Any, parseMode: ComponentParseMode = ComponentParseMode.Auto): Component {
        return textPreprocessor.processComponent(text, parseMode, *args)
    }

    /**
     * 格式化文本，包括语言映射、参数替换和自动格式解析
     *
     * @param text 待格式化的文本
     * @param args 格式化参数
     * @param parseMode 解析模式
     * @return 格式化后的Legacy字符串
     */
    fun formatLegacy(text: String, vararg args: Any, parseMode: ComponentParseMode = ComponentParseMode.Auto): String {
        return textPreprocessor.processLegacy(text, parseMode, *args)
    }

    /**
     * 格式化文本，包括语言映射和参数替换，不包含自动格式解析
     *
     * @param text 待格式化的文本
     * @param args 格式化参数
     * @return 格式化后的字符串
     */
    fun formatPlain(text: String, vararg args: Any): String {
        return textPreprocessor.processPlain(text, *args)
    }
}
