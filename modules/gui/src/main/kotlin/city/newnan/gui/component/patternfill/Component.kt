package city.newnan.gui.component.patternfill

import city.newnan.core.utils.text.toMiniMessage
import city.newnan.gui.component.base.BaseComponent
import city.newnan.gui.component.base.BaseRenderContext
import city.newnan.gui.event.EventHandlers
import city.newnan.gui.page.BasePage
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * 模式填充组件
 *
 * 基于字符模式的填充组件，特性：
 * - 字符串模式定义
 * - 每个字符代表不同的物品类型
 * - 空格表示空槽位
 * - 灵活的模式布局
 * - 字符到物品的映射
 */
class PatternFillComponent(
    page: BasePage,
    private val startX: Int,
    private val startY: Int,
    private val pattern: List<String>
) : BaseComponent<PatternFillRenderContext>(page) {

    // 事件处理器
    override val eventHandlers = EventHandlers()

    // 字符到渲染函数的映射
    private val charRenderMap = mutableMapOf<Char, RenderFunction<PatternFillRenderContext>>()

    // 模式的宽度和高度
    private val _width: Int = pattern.maxOfOrNull { it.length } ?: 0
    private val _height: Int = pattern.size

    // 计算所有槽位
    private val _slots: List<Int> by lazy {
        val result = mutableListOf<Int>()
        val inventoryWidth = page.getInventoryWidth()

        for (y in 0 until _height) {
            val row = pattern.getOrNull(y) ?: ""
            for (x in 0 until _width) {
                val char = row.getOrNull(x) ?: ' '
                // 只有非空格字符才添加到槽位列表
                if (char != ' ') {
                    val slot = (startY + y) * inventoryWidth + (startX + x)
                    if (slot < page.inventory.size) {
                        result.add(slot)
                    }
                }
            }
        }
        result
    }

    // 字符到槽位的映射
    private val charSlotMap: Map<Char, List<Int>> by lazy {
        val result = mutableMapOf<Char, MutableList<Int>>()
        val inventoryWidth = page.getInventoryWidth()

        for (y in 0 until _height) {
            val row = pattern.getOrNull(y) ?: ""
            for (x in 0 until _width) {
                val char = row.getOrNull(x) ?: ' '
                if (char != ' ') {
                    val slot = (startY + y) * inventoryWidth + (startX + x)
                    if (slot < page.inventory.size) {
                        result.getOrPut(char) { mutableListOf() }.add(slot)
                    }
                }
            }
        }
        result
    }

    init {
        // 验证参数
        if (pattern.isEmpty()) {
            throw IllegalArgumentException("Pattern cannot be empty")
        }

        if (_width <= 0 || _height <= 0) {
            throw IllegalArgumentException("Pattern must have positive dimensions: width=$_width, height=$_height")
        }

        if (startX < 0 || startY < 0) {
            throw IllegalArgumentException("Start coordinates must be non-negative: startX=$startX, startY=$startY")
        }

        // 验证模式是否在inventory范围内
        val inventoryWidth = page.getInventoryWidth()
        val inventoryHeight = page.inventory.size / inventoryWidth

        if (startX + _width > inventoryWidth || startY + _height > inventoryHeight) {
            throw IllegalArgumentException(
                "Pattern area exceeds inventory bounds: " +
                "area=($startX,$startY,${startX + _width - 1},${startY + _height - 1}), " +
                "inventory=(0,0,${inventoryWidth - 1},${inventoryHeight - 1})"
            )
        }
    }

    /**
     * 设置字符的渲染函数
     */
    fun setCharRender(char: Char, function: RenderFunction<PatternFillRenderContext>) {
        charRenderMap[char] = function
    }

    /**
     * 设置字符的固定物品
     */
    fun setCharItem(char: Char, item: ItemStack?) {
        setCharRender(char) { item }
    }

    /**
     * 批量设置字符渲染
     */
    fun setCharRenders(vararg pairs: Pair<Char, RenderFunction<PatternFillRenderContext>>) {
        pairs.forEach { (char, function) ->
            setCharRender(char, function)
        }
    }

    /**
     * 批量设置字符物品
     */
    fun setCharItems(vararg pairs: Pair<Char, ItemStack?>) {
        pairs.forEach { (char, item) ->
            setCharItem(char, item)
        }
    }

    /**
     * 填充物品映射（自动处理已有物品）
     * 如果槽位已有物品（oldItem不为null），则返回原有物品，否则使用对应字符的新物品
     */
    fun fillWithItems(charItemMap: Map<Char, ItemStack>) {
        charItemMap.forEach { (char, item) ->
            setCharRender(char) { context -> context.oldItem ?: item }
        }
    }

    /**
     * 填充物品映射（自动处理已有物品）
     * 如果槽位已有物品（oldItem不为null），则返回原有物品，否则使用对应字符的新物品
     */
    fun fillWithMaterials(charMaterialMap: Map<Char, Material>) {
        val charItemMap = charMaterialMap.mapValues { (_, material) -> ItemStack(material) }
        fillWithItems(charItemMap)
    }

    /**
     * 获取字符在模式中的所有槽位
     */
    fun getCharSlots(char: Char): List<Int> {
        return charSlotMap[char] ?: emptyList()
    }

    /**
     * 获取所有使用的字符
     */
    fun getUsedChars(): Set<Char> {
        return charSlotMap.keys
    }

    /**
     * 获取模式字符串
     */
    fun getPattern(): List<String> = pattern.toList()

    /**
     * 获取起始X坐标
     */
    fun getStartX(): Int = startX

    /**
     * 获取起始Y坐标
     */
    fun getStartY(): Int = startY

    /**
     * 获取宽度
     */
    fun getWidth(): Int = _width

    /**
     * 获取高度
     */
    fun getHeight(): Int = _height

    /**
     * 检查坐标是否在模式范围内
     */
    fun containsCoordinate(x: Int, y: Int): Boolean {
        val relativeX = x - startX
        val relativeY = y - startY
        if (relativeX < 0 || relativeY < 0 || relativeY >= _height) return false

        val row = pattern.getOrNull(relativeY) ?: return false
        if (relativeX >= row.length) return false

        return row[relativeX] != ' '
    }

    /**
     * 获取指定坐标的字符
     */
    fun getCharAt(x: Int, y: Int): Char? {
        val relativeX = x - startX
        val relativeY = y - startY
        if (relativeX < 0 || relativeY < 0 || relativeY >= _height) return null

        val row = pattern.getOrNull(relativeY) ?: return null
        if (relativeX >= row.length) return null

        val char = row[relativeX]
        return if (char == ' ') null else char
    }

    override fun getSlots(): List<Int> {
        return _slots
    }

    override fun getRenderContext(slot: Int): PatternFillRenderContext? {
        return getRenderContext(slot, null)
    }

    override fun getRenderContext(slot: Int, oldItem: ItemStack?): PatternFillRenderContext? {
        if (!_slots.contains(slot)) return null

        val inventoryWidth = page.getInventoryWidth()
        val x = slot % inventoryWidth
        val y = slot / inventoryWidth
        val relativeX = x - startX
        val relativeY = y - startY

        val char = getCharAt(x, y) ?: return null
        val charIndex = getCharSlots(char).indexOf(slot)

        return PatternFillRenderContext(
            x = x,
            y = y,
            slot = slot,
            oldItem = oldItem,
            relativeX = relativeX,
            relativeY = relativeY,
            char = char,
            charIndex = charIndex
        )
    }

    override fun renderSlot(context: PatternFillRenderContext): ItemStack? {
        return charRenderMap[context.char]?.invoke(context)
    }

    override fun toString(): String {
        return "PatternFillComponent(startX=$startX, startY=$startY, width=$_width, height=$_height, chars=${getUsedChars()}, slots=${_slots.size}, page=${page.title.toMiniMessage()})"
    }
}

/**
 * 模式填充渲染上下文
 */
data class PatternFillRenderContext(
    override val x: Int,
    override val y: Int,
    override val slot: Int,
    override val oldItem: ItemStack?,
    val relativeX: Int,
    val relativeY: Int,
    val char: Char,
    val charIndex: Int
) : BaseRenderContext()

/**
 * 渲染函数类型别名
 */
typealias RenderFunction<T> = (T) -> ItemStack?
