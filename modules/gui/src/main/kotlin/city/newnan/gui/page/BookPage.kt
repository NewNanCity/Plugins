package city.newnan.gui.page

import city.newnan.core.terminable.CompositeTerminable
import city.newnan.core.utils.text.toMiniMessage
import city.newnan.gui.component.IComponent
import city.newnan.gui.event.*
import city.newnan.gui.manager.logging.GuiLogger
import city.newnan.gui.manager.GuiManager
import city.newnan.gui.manager.scheduler.GuiScheduler
import city.newnan.gui.session.Session
import city.newnan.gui.session.SessionStorage
import net.kyori.adventure.text.Component as AdventureComponent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerEditBookEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 书本页面
 *
 * 特殊的Page类型，用于展示书本内容而不是传统的Inventory界面。
 * 支持多页显示、翻页功能，解决长文本内容展示问题，支持富文本格式。
 *
 * 特性：
 * - 基于Minecraft书本功能的富文本显示
 * - 支持多页内容和翻页导航
 * - 自动分页处理长文本
 * - 支持Adventure Component格式
 * - 不占用玩家背包槽位
 */
open class BookPage(
    override val player: Player,
    override val session: Session,
    override val guiManager: GuiManager,
    override val title: AdventureComponent,
    private val bookTitle: AdventureComponent = title,
    private val author: AdventureComponent?,
    private val editable: Boolean = false,
    private val bookBuilder: BookPage.() -> Unit = {}
) : Page {

    // 书本页面内容
    private val _pages = mutableListOf<AdventureComponent>()
    val pages: List<AdventureComponent> get() = _pages.toList()

    // 当前页码（从0开始）
    private var currentPageIndex = 0

    // 组件列表（BookPage不使用传统组件，但保持接口兼容）
    private val _components = CopyOnWriteArrayList<IComponent<*>>()
    override val components: List<IComponent<*>> get() = _components.toList()

    // 页面状态
    private var _isVisible = false
    override val isVisible: Boolean get() = _isVisible

    private var closed = false
    private var initialized = false

    // 资源管理器
    private val terminableRegistry = CompositeTerminable.create()

    // GUI日志记录器
    val guiLogger: GuiLogger
        get() = guiManager.logger

    // 任务调度器
    override val scheduler: GuiScheduler by lazy {
        GuiScheduler(guiManager)
    }

    // GUI管理器
    override val logger: GuiLogger
        get() = guiManager.logger

    // 页面级别的事件处理器
    internal val eventHandlers = EventHandlers()

    // 编辑状态
    private var isEditing = false
    private var editingSlot = -1

    // BookPage不使用传统的Inventory
    override val inventoryType: InventoryType = InventoryType.CHEST
    override val size: Int? = null

    /**
     * 添加页面内容
     */
    fun addPage(content: AdventureComponent) {
        checkNotClosed()
        _pages.add(content)
    }

    /**
     * 添加页面内容（字符串）
     */
    fun addPage(content: String) {
        addPage(AdventureComponent.text(content))
    }

    /**
     * 设置所有页面内容
     */
    fun setPages(pages: List<AdventureComponent>) {
        checkNotClosed()
        _pages.clear()
        _pages.addAll(pages)
        currentPageIndex = 0
    }

    /**
     * 设置所有页面内容（字符串列表）
     */
    fun setPagesFromStrings(pages: List<String>) {
        setPages(pages.map { AdventureComponent.text(it) })
    }

    /**
     * 清空所有页面
     */
    fun clearPages() {
        checkNotClosed()
        _pages.clear()
        currentPageIndex = 0
    }

    /**
     * 获取当前页码
     */
    fun getCurrentPage(): Int = currentPageIndex

    /**
     * 获取总页数
     */
    fun getTotalPages(): Int = _pages.size

    /**
     * 跳转到指定页
     */
    fun goToPage(pageIndex: Int): Boolean {
        checkNotClosed()
        val newIndex = pageIndex.coerceIn(0, _pages.size - 1)
        if (newIndex != currentPageIndex && _pages.isNotEmpty()) {
            currentPageIndex = newIndex
            if (_isVisible) {
                showBook()
            }
            return true
        }
        return false
    }

    /**
     * 下一页
     */
    fun nextPage(): Boolean {
        return if (currentPageIndex < _pages.size - 1) {
            goToPage(currentPageIndex + 1)
        } else {
            false
        }
    }

    /**
     * 上一页
     */
    fun previousPage(): Boolean {
        return if (currentPageIndex > 0) {
            goToPage(currentPageIndex - 1)
        } else {
            false
        }
    }

    /**
     * 第一页
     */
    fun firstPage(): Boolean {
        return goToPage(0)
    }

    /**
     * 最后一页
     */
    fun lastPage(): Boolean {
        return goToPage(_pages.size - 1)
    }

    /**
     * 检查是否有下一页
     */
    fun hasNextPage(): Boolean = currentPageIndex < _pages.size - 1

    /**
     * 检查是否有上一页
     */
    fun hasPreviousPage(): Boolean = currentPageIndex > 0

    // ==================== 生命周期方法 ====================

    /**
     * 内部初始化方法，由Session调用
     * 仅在页面创建时调用一次
     */
    override fun initInternal() {
        if (initialized) return

        try {
            // 初始化资源
            onInit()
            // 调用BookPage特定的builder
            bookBuilder()
            initialized = true
            eventHandlers.handleEvent(InitEventContext(player))
        } catch (e: Exception) {
            guiLogger.logPageLifecycleError(
                page = this,
                operation = "INIT",
                error = e,
                context = mapOf(
                    "pageType" to (this::class.simpleName ?: "Unknown"),
                    "bookTitle" to bookTitle.toMiniMessage(),
                    "author" to (author?.toMiniMessage() ?: "")
                )
            )
            throw e
        }
    }

    /**
     * 内部显示方法，由Session调用
     * Page不应该直接调用此方法
     */
    override fun showInternal() {
        checkNotClosed()
        checkInitialized()

        if (!_isVisible) {
            try {
                _isVisible = true
                onShow()
                showBook()
            } catch (e: Exception) {
                guiLogger.logPageLifecycleError(
                    page = this,
                    operation = "SHOW",
                    error = e,
                    context = mapOf(
                        "pageType" to (this::class.simpleName ?: "Unknown"),
                        "bookTitle" to bookTitle.toMiniMessage(),
                        "author" to (author?.toMiniMessage() ?: ""),
                        "pagesCount" to _pages.size
                    )
                )
                _isVisible = false
                throw e
            }
        }
    }

    /**
     * 内部隐藏方法，由Session调用
     * Page不应该直接调用此方法
     */
    override fun hideInternal() {
        if (_isVisible) {
            try {
                _isVisible = false
                onHide()
                // 关闭玩家当前打开的书本（如果有）
                if (player.openInventory.topInventory.type == InventoryType.LECTERN) {
                    player.closeInventory()
                }
            } catch (e: Exception) {
                guiLogger.logPageLifecycleError(
                    page = this,
                    operation = "HIDE",
                    error = e,
                    context = mapOf(
                        "pageType" to (this::class.simpleName ?: "Unknown"),
                        "bookTitle" to bookTitle
                    )
                )
                throw e
            }
        }
    }

    override fun update() {
        checkNotClosed()
        checkInitialized()
        if (_isVisible) {
            showBook()
        }
    }

    override fun renderSlots(slots: List<Int>) {

    }

    /**
     * 显示书本给玩家
     */
    private fun showBook() {
        if (_pages.isEmpty()) {
            // 如果没有页面，显示空书
            val emptyBook = createBookItem(listOf(AdventureComponent.text("")))
            player.openBook(emptyBook)
        } else {
            val book = createBookItem(_pages)
            player.openBook(book)
        }
    }

    /**
     * 创建书本ItemStack
     */
    private fun createBookItem(pages: List<AdventureComponent>): ItemStack {
        val book = ItemStack(if (editable && !isEditing) Material.WRITABLE_BOOK else Material.WRITTEN_BOOK)
        val meta = book.itemMeta as BookMeta

        if (!editable || isEditing) {
            // 只读模式或正在编辑时设置标题和作者
            meta.title(bookTitle)
            meta.author(author)
        }

        meta.generation = if (editable) BookMeta.Generation.ORIGINAL else BookMeta.Generation.COPY_OF_COPY

        meta.pages(pages)
        book.itemMeta = meta
        return book
    }

    // Page接口实现（BookPage不使用传统组件系统）

    override fun addComponent(component: IComponent<*>) {
        // BookPage不支持传统组件
        throw UnsupportedOperationException("BookPage does not support traditional components")
    }

    override fun removeComponent(component: IComponent<*>) {
        // BookPage不支持传统组件
        throw UnsupportedOperationException("BookPage does not support traditional components")
    }

    override fun getComponentBySlot(slot: Int): IComponent<*>? {
        // BookPage不支持传统组件
        return null
    }

    override fun clearComponents() {
        // BookPage不支持传统组件，但保持接口兼容
    }

    override fun getPositionInSession(): Int {
        return session.getAllPages().indexOf(this)
    }

    override fun handleEvent(context: EventContext<*>) {
        eventHandlers.handleEvent(context)
    }

    /**
     * 处理书籍翻页事件
     */
    private fun handleBookPageTurn(context: ClickEventContext): Boolean {
        val item = context.item
        if (item != null && (item.type == Material.WRITTEN_BOOK || item.type == Material.WRITABLE_BOOK)) {
            val bookMeta = item.itemMeta as? BookMeta ?: return false

            val isNextPage = context.isRightClick
            val bookPageTurnContext = BookPageTurnEventContext(
                event = context.event,
                player = context.player,
                book = item,
                bookMeta = bookMeta,
                currentPage = getCurrentPage(),
                isNextPage = isNextPage
            )

            eventHandlers.handleBookPageTurn(bookPageTurnContext)
            return true
        }
        return false
    }

    /**
     * 开始编辑书籍
     */
    fun startEditing(slot: Int = 0): Boolean {
        if (!editable || isEditing) return false

        isEditing = true
        editingSlot = slot

        // 给玩家一本可编辑的书
        val editableBook = createBookItem(_pages)
        player.inventory.setItem(slot, editableBook)

        return true
    }

    /**
     * 停止编辑书籍
     */
    fun stopEditing(): Boolean {
        if (!isEditing) return false

        isEditing = false
        val slot = editingSlot
        editingSlot = -1

        // 移除玩家背包中的书
        if (slot >= 0 && slot < player.inventory.size) {
            player.inventory.setItem(slot, null)
        }

        return true
    }

    /**
     * 处理书籍编辑完成
     */
    fun handleBookEdited(event: PlayerEditBookEvent) {
        if (!editable || !isEditing) return

        val newBookMeta = event.newBookMeta
        val previousBookMeta = event.previousBookMeta

        val context = BookEditEventContext(
            event = event,
            player = player,
            slot = event.slot,
            previousBookMeta = previousBookMeta,
            newBookMeta = newBookMeta,
            isSigning = event.isSigning
        )

        if (event.isSigning && newBookMeta != null) {
            // 书籍签名完成
            val signContext = BookSignEventContext(
                event = event,
                player = player,
                slot = event.slot,
                title = PlainTextComponentSerializer.plainText().serialize(newBookMeta.title() ?: AdventureComponent.text("")),
                author = PlainTextComponentSerializer.plainText().serialize(newBookMeta.author() ?: AdventureComponent.text("")),
                pages = newBookMeta.pages().map { PlainTextComponentSerializer.plainText().serialize(it) }
            )
            eventHandlers.handleBookSign(signContext)

            // 更新页面内容
            if (signContext.pages.isNotEmpty()) {
                setPagesFromStrings(signContext.pages)
            }
        } else if (newBookMeta != null) {
            // 书籍内容编辑
            eventHandlers.handleBookEdit(context)

            // 更新页面内容
            val newPages = newBookMeta.pages().map { PlainTextComponentSerializer.plainText().serialize(it) }
            if (newPages.isNotEmpty()) {
                setPagesFromStrings(newPages)
            }
        }

        stopEditing()
    }

    override fun chatInput(hide: Boolean, handler: (input: String) -> Boolean): Boolean {
        val previousChatInputHandler = SessionStorage.getChatInputHandler(player)
        return if (previousChatInputHandler == null) {
            if (hide) session.hide()
            SessionStorage.setChatInputHandler(player, handler)
            true
        } else {
            false
        }
    }

    // ==================== 检查方法 ====================

    /**
     * 检查页面是否未关闭
     */
    private fun checkNotClosed() {
        if (closed) {
            throw IllegalStateException("BookPage is already closed")
        }
    }

    /**
     * 检查页面是否已初始化
     */
    private fun checkInitialized() {
        if (!initialized) {
            throw IllegalStateException("BookPage is not initialized")
        }
    }

    // ==================== 生命周期回调方法，子类可重写 ====================

    /**
     * 页面初始化时调用
     */
    protected open fun onInit() {}

    /**
     * 页面显示时调用
     */
    protected open fun onShow() {
        // 子类可以重写此方法
    }

    /**
     * 页面隐藏时调用
     */
    protected open fun onHide() {
        // 子类可以重写此方法
    }

    /**
     * 页面销毁时调用
     */
    protected open fun onDestroy() {
        // 子类可以重写此方法
    }

    // Terminable接口实现

    override fun destroyInternal() {
        if (closed) return

        try {
            // 标记为已关闭
            closed = true

            // 停止编辑（如果正在编辑）
            if (isEditing) {
                stopEditing()
            }

            // 隐藏页面
            if (_isVisible) {
                hideInternal()
            }

            eventHandlers.handleEvent(DestroyEventContext(player))

            // 关闭资源
            try {
                terminableRegistry.close()
            } catch (e: Exception) {
                guiLogger.logPageLifecycleError(
                    page = this,
                    operation = "DESTROY_RESOURCES",
                    error = e,
                    context = mapOf(
                        "pageType" to (this::class.simpleName ?: "Unknown"),
                        "bookTitle" to bookTitle
                    )
                )
            }

            // 调用子类清理逻辑
            onDestroy()

        } catch (e: Exception) {
            guiLogger.logPageLifecycleError(
                page = this,
                operation = "DESTROY",
                error = e,
                context = mapOf(
                    "pageType" to (this::class.simpleName ?: "Unknown"),
                    "bookTitle" to bookTitle.toMiniMessage(),
                    "author" to (author?.toMiniMessage() ?: "")
                )
            )
            throw e
        }
    }

    override fun toString(): String {
        return "BookPage(title='${title.toMiniMessage()}', bookTitle='${bookTitle.toMiniMessage()}', author='${author?.toMiniMessage() ?: ""}', pages=${_pages.size}, currentPage=$currentPageIndex, visible=$_isVisible, player=${player.name})"
    }
}
