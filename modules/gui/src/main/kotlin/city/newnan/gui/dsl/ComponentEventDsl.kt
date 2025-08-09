package city.newnan.gui.dsl

import city.newnan.gui.component.base.BaseComponent
import city.newnan.gui.component.singleslot.SingleSlotComponent
import city.newnan.gui.component.rectfill.RectFillComponent
import city.newnan.gui.component.borderfill.BorderFillComponent
import city.newnan.gui.component.linefill.LineFillComponent
import city.newnan.gui.component.patternfill.PatternFillComponent
import city.newnan.gui.component.scrollable.ScrollableComponent
import city.newnan.gui.component.paginated.PaginatedComponent
import city.newnan.gui.component.singlestorageslot.SingleStorageSlotComponent

/**
 * Component的去抖配置DSL扩展
 *
 * 提供简洁的DSL语法来设置组件的去抖配置
 */

// 去抖配置DSL
/**
 * 设置去抖时长
 * @param timeMs 去抖时长（毫秒）
 */
fun BaseComponent<*>.debounceTime(timeMs: Long) {
    eventHandlers.debounceTimeMs = timeMs
}

/**
 * 启用点击事件去抖
 */
fun BaseComponent<*>.enableClickDebounce() {
    eventHandlers.enableClickDebounce = true
}

/**
 * 禁用点击事件去抖
 */
fun BaseComponent<*>.disableClickDebounce() {
    eventHandlers.enableClickDebounce = false
}

/**
 * 设置点击事件去抖开关
 * @param enabled 是否启用点击事件去抖
 */
fun BaseComponent<*>.clickDebounce(enabled: Boolean) {
    eventHandlers.enableClickDebounce = enabled
}

/**
 * 去抖配置DSL块
 */
fun BaseComponent<*>.debounce(block: ComponentDebounceConfig.() -> Unit) {
    val config = ComponentDebounceConfig(eventHandlers)
    config.block()
}

/**
 * 组件去抖配置类
 */
class ComponentDebounceConfig(private val eventHandlers: city.newnan.gui.event.EventHandlers) {
    /**
     * 设置去抖时长
     */
    var timeMs: Long
        get() = eventHandlers.debounceTimeMs
        set(value) {
            eventHandlers.debounceTimeMs = value
        }

    /**
     * 设置点击事件去抖开关
     */
    var clickEnabled: Boolean
        get() = eventHandlers.enableClickDebounce
        set(value) {
            eventHandlers.enableClickDebounce = value
        }
}

// 特定组件类型的扩展方法
// SingleSlotComponent 扩展
fun SingleSlotComponent.debounceTime(timeMs: Long) = (this as BaseComponent<*>).debounceTime(timeMs)
fun SingleSlotComponent.enableClickDebounce() = (this as BaseComponent<*>).enableClickDebounce()
fun SingleSlotComponent.disableClickDebounce() = (this as BaseComponent<*>).disableClickDebounce()
fun SingleSlotComponent.clickDebounce(enabled: Boolean) = (this as BaseComponent<*>).clickDebounce(enabled)
fun SingleSlotComponent.debounce(block: ComponentDebounceConfig.() -> Unit) = (this as BaseComponent<*>).debounce(block)

// RectFillComponent 扩展
fun RectFillComponent.debounceTime(timeMs: Long) = (this as BaseComponent<*>).debounceTime(timeMs)
fun RectFillComponent.enableClickDebounce() = (this as BaseComponent<*>).enableClickDebounce()
fun RectFillComponent.disableClickDebounce() = (this as BaseComponent<*>).disableClickDebounce()
fun RectFillComponent.clickDebounce(enabled: Boolean) = (this as BaseComponent<*>).clickDebounce(enabled)
fun RectFillComponent.debounce(block: ComponentDebounceConfig.() -> Unit) = (this as BaseComponent<*>).debounce(block)

// BorderFillComponent 扩展
fun BorderFillComponent.debounceTime(timeMs: Long) = (this as BaseComponent<*>).debounceTime(timeMs)
fun BorderFillComponent.enableClickDebounce() = (this as BaseComponent<*>).enableClickDebounce()
fun BorderFillComponent.disableClickDebounce() = (this as BaseComponent<*>).disableClickDebounce()
fun BorderFillComponent.clickDebounce(enabled: Boolean) = (this as BaseComponent<*>).clickDebounce(enabled)
fun BorderFillComponent.debounce(block: ComponentDebounceConfig.() -> Unit) = (this as BaseComponent<*>).debounce(block)

// LineFillComponent 扩展
fun LineFillComponent.debounceTime(timeMs: Long) = (this as BaseComponent<*>).debounceTime(timeMs)
fun LineFillComponent.enableClickDebounce() = (this as BaseComponent<*>).enableClickDebounce()
fun LineFillComponent.disableClickDebounce() = (this as BaseComponent<*>).disableClickDebounce()
fun LineFillComponent.clickDebounce(enabled: Boolean) = (this as BaseComponent<*>).clickDebounce(enabled)
fun LineFillComponent.debounce(block: ComponentDebounceConfig.() -> Unit) = (this as BaseComponent<*>).debounce(block)

// PatternFillComponent 扩展
fun PatternFillComponent.debounceTime(timeMs: Long) = (this as BaseComponent<*>).debounceTime(timeMs)
fun PatternFillComponent.enableClickDebounce() = (this as BaseComponent<*>).enableClickDebounce()
fun PatternFillComponent.disableClickDebounce() = (this as BaseComponent<*>).disableClickDebounce()
fun PatternFillComponent.clickDebounce(enabled: Boolean) = (this as BaseComponent<*>).clickDebounce(enabled)
fun PatternFillComponent.debounce(block: ComponentDebounceConfig.() -> Unit) = (this as BaseComponent<*>).debounce(block)

// ScrollableComponent 扩展
fun <T> ScrollableComponent<T>.debounceTime(timeMs: Long) = (this as BaseComponent<*>).debounceTime(timeMs)
fun <T> ScrollableComponent<T>.enableClickDebounce() = (this as BaseComponent<*>).enableClickDebounce()
fun <T> ScrollableComponent<T>.disableClickDebounce() = (this as BaseComponent<*>).disableClickDebounce()
fun <T> ScrollableComponent<T>.clickDebounce(enabled: Boolean) = (this as BaseComponent<*>).clickDebounce(enabled)
fun <T> ScrollableComponent<T>.debounce(block: ComponentDebounceConfig.() -> Unit) = (this as BaseComponent<*>).debounce(block)

// PaginatedComponent 扩展
fun <T> PaginatedComponent<T>.debounceTime(timeMs: Long) = (this as BaseComponent<*>).debounceTime(timeMs)
fun <T> PaginatedComponent<T>.enableClickDebounce() = (this as BaseComponent<*>).enableClickDebounce()
fun <T> PaginatedComponent<T>.disableClickDebounce() = (this as BaseComponent<*>).disableClickDebounce()
fun <T> PaginatedComponent<T>.clickDebounce(enabled: Boolean) = (this as BaseComponent<*>).clickDebounce(enabled)
fun <T> PaginatedComponent<T>.debounce(block: ComponentDebounceConfig.() -> Unit) = (this as BaseComponent<*>).debounce(block)



// SingleStorageSlotComponent 扩展
fun SingleStorageSlotComponent.debounceTime(timeMs: Long) = (this as BaseComponent<*>).debounceTime(timeMs)
fun SingleStorageSlotComponent.enableClickDebounce() = (this as BaseComponent<*>).enableClickDebounce()
fun SingleStorageSlotComponent.disableClickDebounce() = (this as BaseComponent<*>).disableClickDebounce()
fun SingleStorageSlotComponent.clickDebounce(enabled: Boolean) = (this as BaseComponent<*>).clickDebounce(enabled)
fun SingleStorageSlotComponent.debounce(block: ComponentDebounceConfig.() -> Unit) = (this as BaseComponent<*>).debounce(block)
