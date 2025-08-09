package city.newnan.gui.dsl

import city.newnan.core.base.BasePlugin
import city.newnan.core.terminable.terminable
import city.newnan.gui.manager.GuiManager
import city.newnan.gui.page.BasePage
import city.newnan.gui.page.BookPage
import city.newnan.gui.page.ChestPage
import city.newnan.gui.session.Session
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType

/**
 * GUI管理器属性扩展
 *
 * 为BasePlugin添加GUI管理器支持，使用属性访问器模式
 */
private val guiManagerMap = mutableMapOf<BasePlugin, GuiManager>()

/**
 * 获取或创建GUI管理器
 * 使用属性访问器模式，自动初始化和生命周期绑定
 */
val BasePlugin.guiManager: GuiManager
    get() = guiManagerMap.getOrPut(this) {
        val manager = GuiManager(this)

        // 在插件禁用时清理（使用bind方法注册清理回调）
        bind(terminable {
            manager.close()
            guiManagerMap.remove(this@guiManager)
        })

        manager
    }

/**
 * DSL扩展方法：打开页面到默认session
 *
 * 等同于：
 * - guiManager.createPage(player, title, inventoryType, size, builder)
 * - guiManager.openPageOnDefaultSession(page)
 *
 * @param inventoryType 容器类型
 * @param size 容器大小（仅对CHEST类型有效）
 * @param player 玩家
 * @param title 页面标题
 * @param builder 页面构建器
 */
fun BasePlugin.openPage(
    inventoryType: InventoryType,
    size: Int? = null,
    player: Player,
    title: String = "",
    builder: BasePage.() -> Unit = {}
): BasePage {
    val page = guiManager.createPage(player, title, inventoryType, size, null, builder)

    // 打开页面
    guiManager.openPageOnDefaultSession(page)

    return page
}

/**
 * DSL扩展方法：打开页面到默认session - Component版本
 *
 * 等同于：
 * - guiManager.createPage(player, title, inventoryType, size, builder)
 * - guiManager.openPageOnDefaultSession(page)
 *
 * @param inventoryType 容器类型
 * @param size 容器大小（仅对CHEST类型有效）
 * @param player 玩家
 * @param title 页面标题
 * @param builder 页面构建器
 */
fun BasePlugin.openPage(
    inventoryType: InventoryType,
    size: Int? = null,
    player: Player,
    title: Component,
    builder: BasePage.() -> Unit = {}
): BasePage {
    val page = guiManager.createPage(player, title, inventoryType, size, null, builder)

    // 打开页面
    guiManager.openPageOnDefaultSession(page)

    return page
}

/**
 * DSL扩展方法：创建CHEST页面（不自动打开）
 */
fun BasePlugin.createChestPage(
    player: Player,
    title: String,
    size: Int,
    session: Session? = null,
    builder: ChestPage.() -> Unit = {}
): ChestPage {
    return guiManager.createChestPage(player, title, size, session, builder)
}

/**
 * DSL扩展方法：创建CHEST页面（不自动打开）- Component版本
 */
fun BasePlugin.createChestPage(
    player: Player,
    title: Component,
    size: Int,
    session: Session? = null,
    builder: ChestPage.() -> Unit = {}
): ChestPage {
    return guiManager.createChestPage(player, title, size, session, builder)
}

/**
 * DSL扩展方法：创建页面（不自动打开）
 */
fun BasePlugin.createPage(
    player: Player,
    title: String,
    inventoryType: InventoryType,
    size: Int? = null,
    session: Session? = null,
    builder: BasePage.() -> Unit = {}
): BasePage {
    return guiManager.createPage(player, title, inventoryType, size, session, builder)
}

/**
 * DSL扩展方法：创建页面（不自动打开）- Component版本
 */
fun BasePlugin.createPage(
    player: Player,
    title: Component,
    inventoryType: InventoryType,
    size: Int? = null,
    session: Session? = null,
    builder: BasePage.() -> Unit = {}
): BasePage {
    return guiManager.createPage(player, title, inventoryType, size, session, builder)
}

/**
 * DSL扩展方法：创建书本页面（不自动打开）
 */
fun BasePlugin.createBookPage(
    player: Player,
    title: String,
    bookTitle: String = title,
    author: String = "Server",
    session: Session? = null,
    builder: BookPage.() -> Unit = {}
): BookPage {
    return guiManager.createBookPage(player, title, bookTitle, author, session, builder)
}

/**
 * DSL扩展方法：创建书本页面（不自动打开）- Component版本
 */
fun BasePlugin.createBookPage(
    player: Player,
    title: Component,
    bookTitle: Component = title,
    author: Component,
    session: Session? = null,
    builder: BookPage.() -> Unit = {}
): BookPage {
    return guiManager.createBookPage(player, title, bookTitle, author, session, builder)
}

/**
 * DSL扩展方法：打开书本页面到默认session
 */
fun BasePlugin.openBookPage(
    player: Player,
    title: String,
    bookTitle: String = title,
    author: String = "Server",
    builder: BookPage.() -> Unit = {}
): BookPage {
    val page = createBookPage(player, title, bookTitle, author, null, builder)

    // 打开页面
    guiManager.openPageOnDefaultSession(page)

    return page
}

/**
 * DSL扩展方法：打开书本页面到默认session - Component版本
 */
fun BasePlugin.openBookPage(
    player: Player,
    title: Component,
    bookTitle: Component = title,
    author: Component,
    builder: BookPage.() -> Unit = {}
): BookPage {
    val page = createBookPage(player, title, bookTitle, author, null, builder)

    // 打开页面
    guiManager.openPageOnDefaultSession(page)

    return page
}

/**
 * DSL扩展方法：获取玩家的默认session
 */
fun BasePlugin.getDefaultSession(player: Player): Session {
    return guiManager.getDefaultSession(player)
}

/**
 * DSL扩展方法：获取玩家的指定session
 */
fun BasePlugin.getSession(player: Player, sessionName: String): Session {
    return guiManager.getSession(player, sessionName)
}

/**
 * DSL扩展方法：获取聊天输入
 */
fun BasePlugin.chatInput(
    player: Player,
    hide: Boolean = true,
    handler: (input: String) -> Boolean
): Boolean {
    return guiManager.chatInput(player, hide, handler)
}
