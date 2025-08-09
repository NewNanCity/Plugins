package city.newnan.gui.session

import city.newnan.core.terminable.Terminable
import city.newnan.gui.manager.logging.GuiLogger
import city.newnan.gui.page.Page
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * GUI会话
 *
 * 每个玩家可以拥有多个session，类似浏览器中的session概念。
 * Session维护一个page实例栈，支持：
 * - 打开新窗口：将新page压入栈顶
 * - 回退操作：从栈中弹出page
 * - 历史记录：支持窗口历史的回滚和前进
 *
 * 特性：
 * - 线程安全：使用读写锁保护栈操作
 * - 生命周期管理：实现Terminable接口
 * - 最大深度限制：防止栈溢出
 * - 自动清理：page失效时自动清理
 */
class Session(
    val player: Player,
    val name: String
) : Terminable {

    // page栈 - 使用MutableList实现栈结构
    private val pageStack = mutableListOf<Page>()

    // 读写锁保护栈操作
    private val lock = ReentrantReadWriteLock()

    // 会话状态
    private var closed = false

    // 最大栈深度限制
    private val maxDepth = 50

    // 当前显示状态
    private var visible = false

    /**
     * 压入新页面到栈顶
     * 新页面将成为当前页面
     */
    fun push(page: Page) {
        checkNotClosed()

        // 主线程执行
        SessionStorage.runInMainThread {
            lock.write {
                // 在锁内检查栈深度限制
                if (pageStack.size >= maxDepth) {
                    throw IllegalStateException("Session stack overflow: maximum depth $maxDepth exceeded")
                }

                // 隐藏当前页面
                if (pageStack.isNotEmpty()) {
                    pageStack.last().hideInternal()
                }

                // 初始化新页面
                page.initInternal()

                // 添加新页面
                pageStack.add(page)

                // 如果session当前可见，显示新页面
                if (visible) {
                    page.showInternal()
                }
            }
        }
    }

    /**
     * 弹出栈顶页面
     * 被弹出的页面会被销毁
     */
    fun pop() {
        SessionStorage.runInMainThread {
            lock.write {
                checkNotClosed()

                if (pageStack.isEmpty()) {
                    return@runInMainThread
                }

                // 移除栈顶页面
                val poppedPage = pageStack.removeLastOrNull()

                // 在锁内销毁页面，确保原子性
                poppedPage?.destroyInternal()

                // 显示新的栈顶页面
                if (pageStack.isNotEmpty() && visible) {
                    pageStack.last().showInternal()
                }

                poppedPage
            }
        }
    }

    /**
     * 替换栈顶页面
     * 如果栈为空，等同于push操作
     */
    fun replace(page: Page) {
        checkNotClosed()

        if (pageStack.isEmpty()) {
            push(page)
        } else {
            SessionStorage.runInMainThread {
                lock.write {
                    val oldPage = pageStack.removeLastOrNull()

                    // 销毁当前栈顶页面
                    oldPage?.destroyInternal()

                    // 初始化新页面
                    page.initInternal()

                    // 添加新页面
                    pageStack.add(page)

                    // 如果session当前可见，显示新页面
                    if (visible) {
                        page.showInternal()
                    }
                }
            }
        }
    }

    /**
     * 获取栈大小
     */
    fun size(): Int {
        return lock.read { pageStack.size }
    }

    /**
     * 检查栈是否为空
     */
    fun isEmpty(): Boolean {
        return lock.read { pageStack.isEmpty() }
    }

    /**
     * 显示会话
     * 显示当前栈顶页面
     */
    fun show() {
        lock.write {
            checkNotClosed()
            visible = true

            if (pageStack.isNotEmpty()) {
                val page = pageStack.last()
                SessionStorage.runInMainThread {
                    page.showInternal()
                }
            }
        }
    }

    /**
     * 隐藏会话
     * 隐藏当前栈顶页面
     */
    fun hide() {
        lock.write {
            checkNotClosed()
            visible = false

            if (pageStack.isNotEmpty()) {
                val page = pageStack.last()
                SessionStorage.runInMainThread {
                    page.hideInternal()
                }
            }
        }
    }

    /**
     * 跳转到指定位置的页面
     * index为0表示栈底，-1表示栈顶
     */
    fun goto(index: Int) {
        val actualIndex = lock.read {
            checkNotClosed()

            val actualIndex = if (index < 0) {
                pageStack.size + index
            } else {
                index
            }

            if (actualIndex < 0 || actualIndex >= pageStack.size) {
                throw IndexOutOfBoundsException("Invalid page index: $index (stack size: ${pageStack.size})")
            }

            actualIndex
        }

        if (actualIndex == pageStack.size - 1) return

        SessionStorage.runInMainThread {
            lock.write {
                // 隐藏当前页面，如果当前页面需要隐藏
                if (visible && actualIndex != pageStack.size - 1) {
                    pageStack.lastOrNull()?.hideInternal()
                }

                // 移除目标页面之后的所有页面
                while (pageStack.size > actualIndex + 1) {
                    val removedPage = pageStack.removeLastOrNull()
                    removedPage?.destroyInternal()
                }

                // 显示目标页面
                if (visible) {
                    pageStack.lastOrNull()?.showInternal()
                }
            }
        }
    }

    /**
     * 获取当前页面（栈顶）
     * 如果栈为空则返回null
     */
    fun current(): Page? {
        return lock.read { pageStack.lastOrNull() }
    }

    /**
     * 获取指定位置的页面
     * index为0表示栈底，-1表示栈顶
     */
    fun getPage(index: Int): Page? {
        return lock.read {
            val actualIndex = if (index < 0) {
                pageStack.size + index
            } else {
                index
            }

            if (actualIndex < 0 || actualIndex >= pageStack.size) {
                null
            } else {
                pageStack[actualIndex]
            }
        }
    }

    /**
     * 获取所有页面的只读列表
     */
    fun getAllPages(): List<Page> {
        return lock.read { pageStack.toList() }
    }

    /**
     * 检查会话是否已关闭
     */
    override fun isClosed(): Boolean = closed

    /**
     * 关闭会话
     * 清空所有页面并释放资源
     */
    override fun close() {
        lock.write {
            if (closed) return@write

            closed = true
            visible = false

            clearPages()
        }
    }

    /**
     * 清空会话中的所有页面
     * 与close()不同，这个方法不会关闭session本身
     */
    fun clearPages() {
        Bukkit.getLogger().info("Closing session $name of ${player.name}")
        if (closed) return
        lock.write {
            // 销毁所有页面
            val pages = pageStack.toList()
            pageStack.clear()

            SessionStorage.runInMainThread {
                pages.forEach { page ->
                    try {
                        page.destroyInternal()
                    } catch (e: Exception) {
                        page.guiManager.logger.logError(GuiLogger.ErrorType.LIFECYCLE, "Error destroying page", e, mapOf(
                            "playerName" to player.name,
                            "pageType" to (page::class.simpleName ?: "Unknown")
                        ))
                    }
                }
            }
        }
    }

    /**
     * 检查会话是否未关闭
     * 如果已关闭则抛出异常
     */
    private fun checkNotClosed() {
        if (closed) {
            throw IllegalStateException("Session is closed")
        }
    }

    /**
     * 获取会话信息字符串
     */
    override fun toString(): String {
        return "Session(player=${player.name}, name=$name, size=${size()}, visible=$visible, closed=$closed)"
    }
}
