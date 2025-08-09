package city.newnan.gui.page

import city.newnan.core.utils.text.toComponent
import city.newnan.core.utils.text.toMiniMessage
import city.newnan.gui.manager.logging.GuiLogger
import city.newnan.gui.manager.GuiManager
import city.newnan.gui.session.Session
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.*
import org.bukkit.inventory.Inventory

/**
 * 箱子类型页面
 *
 * 支持可变尺寸的箱子容器（9-54槽，必须是9的倍数）
 * 这是最常用的GUI页面类型
 */
class ChestPage(
    player: Player,
    session: Session,
    override val guiManager: GuiManager,
    title: net.kyori.adventure.text.Component,
    size: Int,
    private val chestBuilder: ChestPage.() -> Unit = {}
) : BasePage(player, session, guiManager, title, InventoryType.CHEST, if (size <= 0) 27 else size) {

    init {
        // 验证箱子尺寸
        if (size !in 9..54 || size % 9 != 0) {
            throw IllegalArgumentException("CHEST inventory size must be between 9-54 and divisible by 9, got: $size")
        }
    }


    // Bukkit Inventory实例
    override val inventory: Inventory by lazy {
        createInventory()
    }

    // GUI日志记录器
    override val logger: GuiLogger
        get() = guiManager.logger

    /**
     * 创建Bukkit Inventory实例
     */
    private fun createInventory(): Inventory {
        return when (inventoryType) {
            InventoryType.CHEST -> {
                if (size !in 9..54 || size % 9 != 0) {
                    throw IllegalArgumentException("CHEST inventory size must be between 9-54 and divisible by 9, got: $size")
                }
                Bukkit.createInventory(null, size, title)
            }
            else -> {
                Bukkit.createInventory(null, inventoryType, title)
            }
        }
    }

    /**
     * 获取箱子的行数
     */
    val rows: Int = size / 9

    /**
     * 获取箱子的列数（固定为9）
     */
    val columns: Int = 9

    /**
     * 检查坐标是否有效
     */
    fun isValidCoordinate(x: Int, y: Int): Boolean {
        return x in 0 until columns && y in 0 until rows
    }

    /**
     * 将坐标转换为槽位索引
     */
    fun coordinateToSlot(x: Int, y: Int): Int {
        if (!isValidCoordinate(x, y)) {
            throw IllegalArgumentException("Invalid coordinate: ($x, $y) for chest size $size")
        }
        return y * columns + x
    }

    /**
     * 将槽位索引转换为坐标
     */
    fun slotToCoordinate(slot: Int): Pair<Int, Int> {
        if (slot < 0 || slot >= size) {
            throw IllegalArgumentException("Invalid slot: $slot for chest size $size")
        }
        val x = slot % columns
        val y = slot / columns
        return Pair(x, y)
    }

    /**
     * 重写初始化方法，调用ChestPage特定的builder
     */
    override fun initInternal() {
        // 先调用父类的初始化
        super.initInternal()

        try {
            // 调用ChestPage特定的builder
            chestBuilder()
        } catch (e: Exception) {
            logger.logPageLifecycleError(
                page = this,
                operation = "CHEST_BUILDER",
                error = e,
                context = mapOf(
                    "pageType" to "ChestPage",
                    "size" to size,
                    "rows" to rows
                )
            )
            throw e
        }
    }

    override fun toString(): String {
        return "ChestPage(player=${player.name}, title=${title.toMiniMessage()}, size=$size, rows=$rows, visible=$isVisible, components=${components.size})"
    }
}