package city.newnan.gui.component

import city.newnan.gui.component.borderfill.BorderFillRenderContext
import city.newnan.gui.component.linefill.LineFillDirection
import city.newnan.gui.component.linefill.LineFillRenderContext
import city.newnan.gui.component.patternfill.PatternFillRenderContext
import city.newnan.gui.component.rectfill.RectFillRenderContext
import city.newnan.gui.component.singlestorageslot.SingleStorageSlotRenderContext

/**
 * 组件导出文件
 *
 * 提供所有组件的类型别名和导入便利
 * 使用时可以直接导入此包来获取所有组件
 */

// 重新导出组件类
typealias PaginatedComponent<T> = city.newnan.gui.component.paginated.PaginatedComponent<T>
typealias PaginatedRenderContext<T> = city.newnan.gui.component.paginated.PaginatedRenderContext<T>

typealias SingleSlotComponent = city.newnan.gui.component.singleslot.SingleSlotComponent
typealias SingleSlotRenderContext = city.newnan.gui.component.singleslot.SingleSlotRenderContext

typealias RectFillComponent = city.newnan.gui.component.rectfill.RectFillComponent
typealias RectFillRenderContext = RectFillRenderContext

typealias SingleStorageSlotComponent = city.newnan.gui.component.singlestorageslot.SingleStorageSlotComponent
typealias SingleStorageSlotRenderContext = SingleStorageSlotRenderContext

typealias LineFillComponent = city.newnan.gui.component.linefill.LineFillComponent
typealias LineFillRenderContext = LineFillRenderContext
typealias LineFillDirection = LineFillDirection

typealias ScrollableComponent<T> = city.newnan.gui.component.scrollable.ScrollableComponent<T>
typealias ScrollableRenderContext<T> = city.newnan.gui.component.scrollable.ScrollableRenderContext<T>
typealias ScrollChangeContext = city.newnan.gui.component.scrollable.ScrollChangeContext

typealias BorderFillComponent = city.newnan.gui.component.borderfill.BorderFillComponent
typealias BorderFillRenderContext = BorderFillRenderContext
typealias BorderType = city.newnan.gui.component.borderfill.BorderType

typealias PatternFillComponent = city.newnan.gui.component.patternfill.PatternFillComponent
typealias PatternFillRenderContext = PatternFillRenderContext
