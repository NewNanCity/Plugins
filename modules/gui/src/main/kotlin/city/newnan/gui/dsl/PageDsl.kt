package city.newnan.gui.dsl

import city.newnan.core.utils.text.ComponentParseMode
import city.newnan.gui.component.*
import city.newnan.gui.dataprovider.IDataProvider
import city.newnan.gui.page.BasePage
import net.kyori.adventure.text.Component as AdventureComponent

/**
 * Page DSL扩展
 *
 * 提供便利的DSL方法来构建GUI页面
 */

/**
 * 创建单槽组件
 *
 * @param x X坐标（列，从0开始）
 * @param y Y坐标（行，从0开始）
 * @param builder 组件构建器
 * @return 创建的组件
 */
fun BasePage.slotComponent(
    x: Int,
    y: Int,
    builder: SingleSlotComponent.() -> Unit = {}
): SingleSlotComponent {
    val component = SingleSlotComponent(this, x, y)
    component.builder()
    addComponent(component)
    return component
}

/**
 * 批量创建单槽组件
 *
 * @param positions 位置列表，每个位置是(x, y)坐标对
 * @param builder 应用到每个组件的构建器
 * @return 创建的组件列表
 */
fun BasePage.slotComponents(
    positions: List<Pair<Int, Int>>,
    builder: SingleSlotComponent.(x: Int, y: Int) -> Unit = { _, _ -> }
): List<SingleSlotComponent> {
    return positions.map { (x, y) ->
        slotComponent(x, y) {
            builder(x, y)
        }
    }
}

/**
 * 在指定区域创建单槽组件网格
 *
 * @param startX 起始X坐标
 * @param startY 起始Y坐标
 * @param width 宽度
 * @param height 高度
 * @param builder 应用到每个组件的构建器
 * @return 创建的组件列表
 */
fun BasePage.slotGrid(
    startX: Int,
    startY: Int,
    width: Int,
    height: Int,
    builder: SingleSlotComponent.(x: Int, y: Int) -> Unit = { _, _ -> }
): List<SingleSlotComponent> {
    val components = mutableListOf<SingleSlotComponent>()

    for (y in startY until startY + height) {
        for (x in startX until startX + width) {
            val component = slotComponent(x, y) {
                builder(x, y)
            }
            components.add(component)
        }
    }

    return components
}

/**
 * 创建边框组件
 *
 * @param width 页面宽度
 * @param height 页面高度
 * @param builder 应用到边框组件的构建器
 * @return 创建的边框组件列表
 */
fun BasePage.border(
    width: Int,
    height: Int,
    builder: SingleSlotComponent.(x: Int, y: Int, isBorder: Boolean) -> Unit = { _, _, _ -> }
): List<SingleSlotComponent> {
    val components = mutableListOf<SingleSlotComponent>()

    for (y in 0 until height) {
        for (x in 0 until width) {
            val isBorder = x == 0 || x == width - 1 || y == 0 || y == height - 1
            if (isBorder) {
                val component = slotComponent(x, y) {
                    builder(x, y, true)
                }
                components.add(component)
            }
        }
    }

    return components
}

/**
 * 创建矩形填充组件
 *
 * @param startX 起始X坐标
 * @param startY 起始Y坐标
 * @param width 宽度
 * @param height 高度
 * @param builder 组件构建器
 * @return 创建的组件
 */
fun BasePage.rectFillComponent(
    startX: Int,
    startY: Int,
    width: Int,
    height: Int,
    builder: RectFillComponent.() -> Unit = {}
): RectFillComponent {
    val component = RectFillComponent(this, startX, startY, width, height)
    component.builder()
    addComponent(component)
    return component
}

/**
 * 创建边框填充组件
 *
 * @param startX 起始X坐标
 * @param startY 起始Y坐标
 * @param width 宽度
 * @param height 高度
 * @param builder 组件构建器
 * @return 创建的组件
 */
fun BasePage.borderFillComponent(
    startX: Int,
    startY: Int,
    width: Int,
    height: Int,
    builder: BorderFillComponent.() -> Unit = {}
): BorderFillComponent {
    val component = BorderFillComponent(this, startX, startY, width, height)
    component.builder()
    addComponent(component)
    return component
}

/**
 * 创建模式填充组件
 *
 * @param startX 起始X坐标
 * @param startY 起始Y坐标
 * @param pattern 字符模式列表
 * @param builder 组件构建器
 * @return 创建的组件
 */
fun BasePage.patternFillComponent(
    startX: Int,
    startY: Int,
    pattern: List<String>,
    builder: PatternFillComponent.() -> Unit = {}
): PatternFillComponent {
    val component = PatternFillComponent(this, startX, startY, pattern)
    component.builder()
    addComponent(component)
    return component
}

/**
 * 创建模式填充组件（字符串数组版本）
 */
fun BasePage.patternFillComponent(
    startX: Int,
    startY: Int,
    vararg pattern: String,
    builder: PatternFillComponent.() -> Unit = {}
): PatternFillComponent {
    return patternFillComponent(startX, startY, pattern.toList(), builder)
}

/**
 * 创建分页组件
 *
 * @param startX 起始X坐标
 * @param startY 起始Y坐标
 * @param width 宽度
 * @param height 高度
 * @param dataProvider 数据提供器
 * @param builder 组件构建器
 * @return 创建的组件
 */
fun <T> BasePage.paginatedComponent(
    startX: Int,
    startY: Int,
    width: Int,
    height: Int,
    dataProvider: IDataProvider<T>,
    builder: PaginatedComponent<T>.() -> Unit = {}
): PaginatedComponent<T> {
    val component = PaginatedComponent(this, startX, startY, width, height, dataProvider)
    component.builder()
    addComponent(component)
    return component
}

/**
 * 创建分页组件（便利方法，从List创建）
 *
 * @param startX 起始X坐标
 * @param startY 起始Y坐标
 * @param width 宽度
 * @param height 高度
 * @param data 数据列表
 * @param builder 组件构建器
 * @return 创建的组件
 */
fun <T> BasePage.paginatedComponent(
    startX: Int,
    startY: Int,
    width: Int,
    height: Int,
    data: List<T>,
    builder: PaginatedComponent<T>.() -> Unit = {}
): PaginatedComponent<T> {
    val component = PaginatedComponent(this, startX, startY, width, height, data)
    component.builder()
    addComponent(component)
    return component
}

/**
 * 创建分页组件（便利方法，从List getter创建）
 *
 * @param startX 起始X坐标
 * @param startY 起始Y坐标
 * @param width 宽度
 * @param height 高度
 * @param data 获取数据的函数
 * @param builder 组件构建器
 * @return 创建的组件
 */
fun <T> BasePage.paginatedComponent(
    startX: Int,
    startY: Int,
    width: Int,
    height: Int,
    data: () -> List<T>,
    builder: PaginatedComponent<T>.() -> Unit = {}
): PaginatedComponent<T> {
    val component = PaginatedComponent(this, startX, startY, width, height, data)
    component.builder()
    addComponent(component)
    return component
}

/**
 * 创建线性填充组件
 *
 * @param startX 起始X坐标
 * @param startY 起始Y坐标
 * @param length 长度
 * @param direction 方向
 * @param builder 组件构建器
 * @return 创建的组件
 */
fun BasePage.lineFillComponent(
    startX: Int,
    startY: Int,
    length: Int,
    direction: LineFillDirection,
    builder: LineFillComponent.() -> Unit = {}
): LineFillComponent {
    val component = LineFillComponent(this, startX, startY, length, direction)
    component.builder()
    addComponent(component)
    return component
}

/**
 * 创建水平线组件
 */
fun BasePage.horizontalLine(
    startX: Int,
    y: Int,
    length: Int,
    builder: LineFillComponent.() -> Unit = {}
): LineFillComponent {
    return lineFillComponent(startX, y, length, LineFillDirection.HORIZONTAL, builder)
}

/**
 * 创建垂直线组件
 */
fun BasePage.verticalLine(
    x: Int,
    startY: Int,
    length: Int,
    builder: LineFillComponent.() -> Unit = {}
): LineFillComponent {
    return lineFillComponent(x, startY, length, LineFillDirection.VERTICAL, builder)
}

/**
 * 创建可滚动组件（从List）
 *
 * @param startX 起始X坐标
 * @param startY 起始Y坐标
 * @param width 宽度
 * @param height 高度
 * @param data 数据列表
 * @param builder 组件构建器
 * @return 创建的组件
 */
fun <T> BasePage.scrollableComponent(
    startX: Int,
    startY: Int,
    width: Int,
    height: Int,
    data: List<T> = emptyList(),
    builder: ScrollableComponent<T>.() -> Unit = {}
): ScrollableComponent<T> {
    val component = ScrollableComponent(this, startX, startY, width, height, data)
    component.builder()
    addComponent(component)
    return component
}

/**
 * 创建可滚动组件（从DataProvider）
 *
 * @param startX 起始X坐标
 * @param startY 起始Y坐标
 * @param width 宽度
 * @param height 高度
 * @param dataProvider 数据提供器
 * @param builder 组件构建器
 * @return 创建的组件
 */
fun <T> BasePage.scrollableComponent(
    startX: Int,
    startY: Int,
    width: Int,
    height: Int,
    dataProvider: IDataProvider<T>,
    builder: ScrollableComponent<T>.() -> Unit = {}
): ScrollableComponent<T> {
    val component = ScrollableComponent(this, startX, startY, width, height, dataProvider)
    component.builder()
    addComponent(component)
    return component
}

/**
 * 创建可滚动组件（从函数）
 *
 * @param startX 起始X坐标
 * @param startY 起始Y坐标
 * @param width 宽度
 * @param height 高度
 * @param data 数据提供函数
 * @param builder 组件构建器
 * @return 创建的组件
 */
fun <T> BasePage.scrollableComponent(
    startX: Int,
    startY: Int,
    width: Int,
    height: Int,
    data: () -> List<T>,
    builder: ScrollableComponent<T>.() -> Unit = {}
): ScrollableComponent<T> {
    val component = ScrollableComponent(this, startX, startY, width, height, data)
    component.builder()
    addComponent(component)
    return component
}

/**
 * 创建单个存储槽组件
 *
 * @param x X坐标
 * @param y Y坐标
 * @param builder 组件构建器
 * @return 创建的组件
 */
fun BasePage.storageSlotComponent(
    x: Int,
    y: Int,
    builder: SingleStorageSlotComponent.() -> Unit = {}
): SingleStorageSlotComponent {
    val component = SingleStorageSlotComponent(this, x, y)
    component.builder()
    addComponent(component)
    return component
}

fun BasePage.format(text: String, vararg args: Any, parseMode: ComponentParseMode = ComponentParseMode.Auto): AdventureComponent {
    return guiManager.format(text, *args, parseMode = parseMode)
}

fun BasePage.formatLegacy(text: String, vararg args: Any, parseMode: ComponentParseMode = ComponentParseMode.Auto): String {
    return guiManager.formatLegacy(text, *args, parseMode = parseMode)
}

fun BasePage.formatPlain(text: String, vararg args: Any): String {
    return guiManager.formatPlain(text, *args)
}
