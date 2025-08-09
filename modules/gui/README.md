# GUI模块

新一代GUI模块，基于现代化设计理念重构，提供强大而灵活的GUI开发体验。

## 🎯 核心概念

GUI模块包含五个核心概念：**Session**、**Page**、**Component**、**Item**、**Event**

### Session 会话系统
- 每个玩家拥有多个session，类似浏览器中的session概念
- Session维护一个page实例栈，支持打开新窗口、回退操作、历史记录
- 支持跨插件GUI协作

### Page 页面系统
- Page是GUI界面的最小单位，每个page对应session栈中的一个元素
- 支持所有Bukkit InventoryType容器类型
- 完整的生命周期管理：创建、打开、关闭、销毁

### Component 组件系统
- Component是页面的组件，类似浏览器DOM元素
- 基于密封类的类型安全渲染上下文
- 支持事件冒泡机制：item → component → page

### Event 事件系统
- 基于Bukkit inventory事件包的完整封装
- 支持事件冒泡和传播控制
- 类型安全的事件上下文

## 🚀 快速开始

### 基础使用

```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 使用DSL创建GUI
        openPage(InventoryType.CHEST, 54, player, "示例GUI") {
            // 创建单槽组件
            slotComponent(x = 1, y = 1) { component ->
                var clickCount = 0

                render { context ->
                    itemUtil.createHeadItemStack(player) {
                        name("点击我")
                        lore("你已经点击了 $clickCount 次")
                    }
                }

                onLeftClick { context ->
                    context.player.sendMessage("你左键点击了！")
                    clickCount++
                    component.update()
                }
            }
        }
    }
}
```

### 会话管理

```kotlin
// 获取默认session
val session = getDefaultSession(player)

// 获取命名session
val namedSession = getSession(player, "my-session")

// 手动管理页面栈
session.push(page)
session.pop()
session.replace(newPage)
session.goto(0) // 跳转到栈底
```

### 聊天输入

```kotlin
page.chatInput(hide = true) { input ->
    if (input == "confirm") {
        player.sendMessage("已确认！")
        page.show()
        true // 结束输入
    } else {
        player.sendMessage("请输入 'confirm' 确认")
        false // 继续等待输入
    }
}
```

## 📦 架构特性

### 全局共享机制
- **SessionStorage单例**：全局管理所有数据，线程安全
- **插件隔离与协作**：支持跨插件GUI协作，自动资源清理
- **事件处理机制**：责任链模式，避免重复处理

### 生命周期管理
- **自动资源管理**：基于Terminable模式的完整资源管理
- **BasePlugin集成**：自动创建并绑定guiManager
- **懒加载**：用户需要时才创建session

### 类型安全
- **密封类渲染上下文**：编译时检查参数类型
- **泛型事件上下文**：类型安全的事件处理
- **DSL支持**：现代化的Kotlin DSL API

## 🎨 支持的容器类型

### 基础容器
- **CHEST**：箱子（9-54槽，可变尺寸）
- **DISPENSER**：发射器（9槽）
- **DROPPER**：投掷器（9槽）
- **HOPPER**：漏斗（5槽）
- **SHULKER_BOX**：潜影盒（27槽）
- **BARREL**：木桶（27槽）
- **ENDER_CHEST**：末影箱（27槽）

### 工作台类
- **WORKBENCH**：工作台
- **ANVIL**：铁砧
- **SMITHING**：锻造台
- **ENCHANTING**：附魔台
- **GRINDSTONE**：砂轮
- **CARTOGRAPHY**：制图台
- **LOOM**：织布机
- **STONECUTTER**：切石机

### 熔炉类
- **FURNACE**：熔炉
- **BLAST_FURNACE**：高炉
- **SMOKER**：烟熏炉

### 特殊容器
- **BREWING**：酿造台
- **BEACON**：信标
- **LECTERN**：讲台
- **CRAFTER**：合成器

## 🧩 组件类型

### 已实现
- **SingleSlotComponent**：单槽组件，支持完整事件处理
- **RectFillComponent**：矩形区域填充，支持棋盘模式、渐变填充等
- **PatternFillComponent**：基于字符模式的填充，支持复杂图案设计
- **PaginatedComponent**：分页显示组件，支持大量数据的分页展示

### 新增实现 ✅
- **LineFillComponent**：线性布局，支持水平线和垂直线
- **InfinitePaginatedComponent**：无限滚动分页，支持动态数据加载
- **ScrollableComponent**：可滚动组件，按行滚动提供平滑体验
- **SingleStorageSlotComponent**：可交互的存储槽，支持物品存取和验证
- **BookPage**：书本页面，支持多页内容和富文本格式

## ⚡ 事件支持

### 🛡️ 物品保护机制（重要安全特性）

**设计原则**：GUI中的物品是**只读展示**，用户不能通过任何方式移动或修改物品。

**保护措施**：
- ❌ **禁止拖拽**：用户无法拖拽GUI中的物品
- ❌ **禁止Shift点击**：无法快速移动物品到背包
- ❌ **禁止丢弃**：无法丢弃GUI中的物品
- ❌ **禁止数字键交换**：无法用数字键快速交换物品
- ❌ **禁止放置物品**：无法从背包拖物品到GUI槽位
- ❌ **禁止取走物品**：无法从GUI槽位取走物品到光标
- ✅ **允许点击事件**：仍然可以触发按钮功能和交互逻辑

**例外情况**：只有专门的`StorageComponent`（未来实现）才允许物品交互

### 核心Inventory事件
- **InventoryClickEvent** → onClick、onRightClick、onShiftClick等
- **InventoryDragEvent** → onDrag、onDragSingle、onDragEven
- **InventoryCloseEvent** → onClose
- **InventoryOpenEvent** → onOpen

### 制作相关事件
- **CraftItemEvent** → onCraft
- **PrepareItemCraftEvent** → onPrepareCraft
- **PrepareAnvilEvent** → onPrepareAnvil
- **PrepareGrindstoneEvent** → onPrepareGrindstone
- **PrepareSmithingEvent** → onPrepareSmithing
- **SmithItemEvent** → onSmithItem

### 熔炉相关事件
- **FurnaceSmeltEvent** → onFurnaceSmelt
- **FurnaceBurnEvent** → onFurnaceBurn
- **FurnaceExtractEvent** → onFurnaceExtract
- **FurnaceStartSmeltEvent** → onFurnaceStartSmelt

### 酿造相关事件
- **BrewEvent** → onBrew
- **BrewingStandFuelEvent** → onBrewingFuel

### 特殊Inventory事件
- **InventoryMoveItemEvent** → onItemMove
- **InventoryPickupItemEvent** → onPickupItem
- **InventoryCreativeEvent** → onCreative

### 其他事件
- **TradeSelectEvent** → onTradeSelect

## 🔧 开发状态

### Phase 1 - 核心架构 ✅
- [x] SessionStorage + Session + GuiManager
- [x] BasePage + BaseComponent
- [x] 基础事件系统
- [x] 简单的DSL支持
- [x] BasePlugin集成

### Phase 2 - 完整功能 ✅
- [x] 核心Component实现（RectFill, PatternFill, Paginated）
- [x] 新增Component实现（LineFill, InfinitePaginated, Scrollable, SingleStorageSlot）
- [x] 特殊Page类型（BookPage）
- [x] 完整事件封装和全局事件监听器（20+种事件类型）
- [x] 全面的DSL支持和便利方法
- [x] 所有容器类型支持（20+种）
- [x] 丰富的使用示例和新组件演示

### Phase 3 - 扩展功能 📋
- [ ] 高级组件（Scrollable, InfinitePaginated等）
- [ ] 性能优化和测试覆盖
- [ ] 完整文档和最佳实践指南
- [ ] GUI模板系统和主题支持

## 🆕 新功能使用示例

### BookPage - 书本页面

```kotlin
// 创建多页书本
openBookPage(player, "帮助手册", "服务器指南", "管理员") {
    addPage(Component.text("欢迎来到服务器！").color(NamedTextColor.BLUE))
    addPage(Component.text("基础命令：\n/spawn - 回到出生点\n/home - 回到家"))
    addPage(Component.text("规则说明：\n1. 禁止恶意破坏\n2. 尊重其他玩家"))
}
```

### LineFillComponent - 线性布局

```kotlin
openPage(InventoryType.CHEST, 54, player, "线性组件示例") {
    // 创建水平分割线
    horizontalLine(0, 2, 9) {
        setMaterial(Material.GRAY_STAINED_GLASS_PANE)
        onLeftClick { context ->
            context.player.sendMessage("点击了水平线！")
        }
    }

    // 创建垂直分割线
    verticalLine(4, 0, 6) {
        setMaterial(Material.BLACK_STAINED_GLASS_PANE)
    }
}
```

### ScrollableComponent - 可滚动组件

```kotlin
val testData = (1..100).map { "物品 $it" }

openPage(InventoryType.CHEST, 54, player, "滚动示例") {
    scrollableComponent(1, 1, 7, 4, testData) {
        render { context ->
            ItemStack(Material.DIAMOND).apply {
                itemMeta = itemMeta?.apply {
                    setDisplayName("§b${context.item}")
                    lore = listOf("§7全局索引: ${context.globalIndex}")
                }
            }
        }

        onDataItemClick { context, item, index, globalIndex ->
            context.player.sendMessage("§b点击了: $item")
        }
    }

    // 滚动控制按钮
    slotComponent(0, 0) {
        render { ItemStack(Material.GREEN_WOOL).apply {
            itemMeta = itemMeta?.apply { setDisplayName("§a向上滚动") }
        }}
        onLeftClick { context ->
            val component = components.filterIsInstance<ScrollableComponent<String>>().firstOrNull()
            component?.scrollUp()
        }
    }
}
```

### SingleStorageSlotComponent - 可交互存储槽

```kotlin
openPage(InventoryType.CHEST, 27, player, "存储槽示例") {
    storageSlotComponent(2, 1) {
        // 设置物品验证器 - 只允许特定物品
        setItemValidator { item ->
            item == null || item.type in listOf(Material.DIAMOND, Material.GOLD_INGOT)
        }

        // 监听物品变化
        onItemChange { oldItem, newItem ->
            val oldName = oldItem?.type?.name ?: "空"
            val newName = newItem?.type?.name ?: "空"
            player.sendMessage("§e物品变化: $oldName -> $newName")
        }

        // 存储槽点击事件
        onStorageClick { context, storedItem, cursorItem ->
            context.player.sendMessage("§6存储槽交互")
        }
    }
}
```

### InfinitePaginatedComponent - 无限分页

```kotlin
openPage(InventoryType.CHEST, 54, player, "无限分页示例") {
    infinitePaginatedComponent<String>(1, 1, 7, 4) {
        // 设置数据提供者
        setNextPageProvider { page ->
            // 模拟动态数据加载
            (0 until 28).map { "第${page}页-物品${it}" }
        }

        render { context ->
            ItemStack(Material.PAPER).apply {
                itemMeta = itemMeta?.apply {
                    setDisplayName("§a${context.item}")
                    lore = listOf("§7页码: ${context.pageIndex}")
                }
            }
        }

        onDataItemClick { context, item, index, globalIndex ->
            context.player.sendMessage("§a点击了: $item")
        }
    }
}
```

## 📚 相关文档

- [设计文档](../../new-gui.md)
- [TODO列表](../../new-gui-todos.md)
- [最佳实践](docs/best-practices.md) (待创建)
- [新组件示例](src/test/kotlin/city/newnan/gui/example/NewComponentsExample.kt)

## ✅ 实现状态总结

### 已完成的核心功能
- ✅ **完整的架构实现**: Session、Page、Component、Event 系统全部实现
- ✅ **20+ 种容器类型支持**: 从基础箱子到特殊工作台全覆盖
- ✅ **8 种组件类型**: 单槽、矩形填充、模式填充、分页、线性、滚动、无限分页、存储槽
- ✅ **完整的事件系统**: 20+ 种 Bukkit 事件的封装和处理
- ✅ **物品保护机制**: 确保 GUI 物品安全，防止意外移动或丢失
- ✅ **生命周期管理**: 自动资源清理和内存管理
- ✅ **DSL 语法支持**: 现代化的 Kotlin DSL API
- ✅ **异步渲染**: 支持复杂组件的异步渲染
- ✅ **错误处理**: 完整的错误日志和异常处理机制
- ✅ **国际化支持**: 多语言文本处理

### 编译状态
- ✅ **主要代码编译成功**: 所有核心功能代码已通过编译
- ⚠️ **测试代码待修复**: 测试文件需要更新以匹配新的 API 设计
- ✅ **构建系统正常**: Gradle 构建配置完整

### 下一步计划
1. **修复测试代码**: 更新测试文件以匹配当前 API
2. **性能优化**: 添加缓存和渲染优化
3. **文档完善**: 创建详细的 API 文档和最佳实践指南
4. **示例项目**: 创建完整的示例插件展示所有功能

## 🤝 贡献

GUI模块的核心功能已经实现完成，欢迎贡献代码和建议！特别欢迎：
- 测试代码的修复和完善
- 性能优化建议
- 新组件类型的实现
- 文档和示例的改进
