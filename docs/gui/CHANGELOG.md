# GUI1 模块更新日志

## 2025-06-20 - BorderFillComponent 和槽位覆盖修复

### 🎯 主要更新

#### 1. 新增 BorderFillComponent 组件
全新的边框填充组件，支持在指定矩形区域的边框填充物品。

**核心特性:**
- 灵活的边框配置：可选择填充哪些边（上、下、左、右）
- 边框类型识别：区分角落和边的不同位置
- 完整的事件处理：支持各种点击事件
- 丰富的DSL支持：提供便利的DSL方法
- 性能优化：支持智能缓存和渲染优化

**基本用法:**
```kotlin
// 创建完整边框
page.fullBorder(0, 0, 9, 6) {
    decorativeBorder(
        borderMaterial = Material.GRAY_STAINED_GLASS_PANE,
        cornerMaterial = Material.BLACK_STAINED_GLASS_PANE
    )
    onClick { context ->
        // 边框点击处理
    }
}

// 创建部分边框
page.borderFillComponent(1, 1, 7, 4,
    fillTop = true,
    fillBottom = true,
    fillLeft = false,
    fillRight = false
) {
    fillMaterial(Material.BLUE_STAINED_GLASS_PANE, "§b边框装饰")
}
```

#### 2. 槽位覆盖问题修复
修复了组件间槽位覆盖导致的渲染和事件处理问题。

**修复内容:**
- **渲染逻辑修复**: 确保只有拥有槽位的组件才会渲染
- **事件处理修复**: 确保事件只发送给正确的组件
- **调试支持**: 添加详细的日志记录帮助调试覆盖问题

**技术实现:**
```kotlin
// 渲染时检查槽位所有权
val ownerComponent = page.getComponentBySlot(slot)
if (ownerComponent == this) {
    // 只有拥有者才渲染
    renderSlot(context)
}

// 事件处理时检查槽位所有权
val ownerComponent = page.getComponentBySlot(context.slot)
if (ownerComponent == this && containsSlot(context.slot)) {
    // 只有拥有者才处理事件
    eventHandlers.handleClick(context)
}
```

#### 3. BorderFillComponent DSL 扩展
为BorderFillComponent提供了丰富的DSL方法。

**DSL方法:**
- `fillMaterial()` - 快速填充边框材料
- `borderMaterials()` - 基于边框类型的不同材料填充
- `decorativeBorder()` - 创建装饰性边框
- `glowingBorder()` - 创建发光边框
- `onClickByBorderType()` - 基于边框类型的点击处理

**示例:**
```kotlin
borderFillComponent(0, 0, 9, 6) {
    // 不同类型的边框材料
    borderMaterials(
        topMaterial = Material.RED_STAINED_GLASS_PANE,
        bottomMaterial = Material.BLUE_STAINED_GLASS_PANE,
        leftMaterial = Material.GREEN_STAINED_GLASS_PANE,
        rightMaterial = Material.YELLOW_STAINED_GLASS_PANE,
        cornerMaterial = Material.BLACK_STAINED_GLASS_PANE
    )

    // 基于边框类型的点击处理
    onClickByBorderType { context, borderType, relativeX, relativeY ->
        when (borderType) {
            BorderType.TOP -> player.sendMessage("点击了上边框")
            BorderType.CORNER -> player.sendMessage("点击了角落")
            else -> player.sendMessage("点击了边框")
        }
    }
}
```

### 📚 文档更新

#### 1. API文档更新
- 更新了 `docs/gui1/api/components.md` 添加BorderFillComponent完整API文档
- 更新了 `docs/gui1/README.md` 添加BorderFillComponent介绍
- 更新了 `docs/gui1/quick-start.md` 包含BorderFillComponent快速开始示例

#### 2. 最佳实践更新
- 添加了槽位覆盖问题的最佳实践指南
- 更新了组件设计的建议
- 添加了BorderFillComponent的使用建议

#### 3. 新增示例文档
- `docs/gui1/examples/border-components.md` - BorderFillComponent使用示例
- 更新了现有示例以展示新功能

### 🔧 技术实现细节

#### 1. BorderType 枚举
```kotlin
enum class BorderType {
    TOP, BOTTOM, LEFT, RIGHT,
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
    UNKNOWN
}
```

#### 2. BorderFillRenderContext
```kotlin
data class BorderFillRenderContext(
    override val x: Int,
    override val y: Int,
    override val slot: Int,
    override val oldItem: ItemStack?,
    val relativeX: Int,
    val relativeY: Int,
    val borderType: BorderType
) : RenderContext()
```

#### 3. 槽位所有权检查
通过 `page.getComponentBySlot(slot)` 方法确定槽位的真正拥有者，避免覆盖问题。

### 🎨 使用场景

#### 1. 装饰性边框
```kotlin
// 创建美观的GUI边框
page.fullBorder(0, 0, 9, 6) {
    decorativeBorder(
        borderMaterial = Material.GRAY_STAINED_GLASS_PANE,
        cornerMaterial = Material.BLACK_STAINED_GLASS_PANE,
        borderName = "§7边框",
        cornerName = "§8角落"
    )
}
```

#### 2. 功能性边框
```kotlin
// 创建可点击的导航边框
page.borderFillComponent(0, 0, 9, 1) { // 顶部边框
    fillMaterial(Material.BLUE_STAINED_GLASS_PANE, "§b导航栏")
    onClick { context ->
        // 导航功能
        when (context.slot) {
            0 -> openPreviousPage()
            8 -> openNextPage()
            else -> showNavigationMenu()
        }
    }
}
```

#### 3. 动态边框
```kotlin
// 根据状态变化的边框
page.borderFillComponent(0, 0, 9, 6) {
    renderByBorderType { borderType, x, y, relativeX, relativeY ->
        val material = when (player.gameMode) {
            GameMode.CREATIVE -> Material.GOLD_BLOCK
            GameMode.SURVIVAL -> Material.IRON_BLOCK
            else -> Material.STONE
        }
        ItemStack(material)
    }
}
```

### 💡 最佳实践建议

1. **避免槽位覆盖**: 设计组件时注意槽位范围，避免不必要的覆盖
2. **合理使用边框**: BorderFillComponent适合装饰和导航，不适合复杂的内容展示
3. **性能考虑**: 大型边框组件建议启用智能缓存
4. **事件处理**: 利用边框类型信息提供更精确的交互体验
5. **视觉设计**: 使用不同材料区分边框的不同部分

### 🔄 向后兼容性

所有更改都保持了向后兼容性：
- 现有组件的行为保持不变
- 槽位覆盖修复不影响正常使用的组件
- 新的BorderFillComponent是额外功能，不影响现有代码

这次更新显著增强了GUI1模块的组件系统，提供了更强大的边框处理能力和更可靠的槽位管理机制。

---

## 2025-06-20 - ItemBuilder Function 参数和头颅功能增强

### 🎯 主要更新

#### 1. ItemBuilder Function 参数支持
为所有物品创建方法添加了可选的 `function: ItemBuilder.() -> Unit = {}` 参数，允许在创建物品时进行高级自定义配置。

**影响的方法:**
- `ItemUtil` 中的所有物品创建方法
- `ItemDsl` 中的所有DSL函数
- 所有GUI模板方法（按钮、分隔符、边框等）

**示例用法:**
```kotlin
// 创建带附魔的按钮
val enchantedButton = ItemUtil.button(Material.DIAMOND_SWORD, "强化武器") {
    enchant(Enchantment.DAMAGE_ALL, 5)
    enchant(Enchantment.FIRE_ASPECT, 2)
    flag(ItemFlag.HIDE_ENCHANTS)
    unbreakable(true)
}

// 创建发光的导航按钮
val glowingBack = ItemUtil.backButton("返回上级") {
    enchant(Enchantment.LURE, 1)
    flag(ItemFlag.HIDE_ENCHANTS)
}
```

#### 2. 头颅功能增强
新增了多种头颅创建方法，支持URL材质和Base64材质。

**新增方法:**
- `ItemUtil.urlSkull()` - 通过材质URL创建头颅
- `ItemUtil.customSkull()` - 通过Base64数据创建头颅
- `OfflinePlayer.getSkull()` - 扩展函数
- `String.toSkull()` - 字符串转头颅扩展函数
- `URL.toSkull()` - URL转头颅扩展函数

**示例用法:**
```kotlin
// URL头颅
val customHead1 = ItemUtil.urlSkull("abc123def456", "自定义头颅") {
    addLore("材质ID: abc123def456")
    enchant(Enchantment.LURE, 1)
    flag(ItemFlag.HIDE_ENCHANTS)
}

// Base64头颅
val base64Head = ItemUtil.customSkull("eyJ0ZXh0dXJlcyI6...", "Base64头颅") {
    addLore("Base64编码的头颅材质")
}

// 扩展函数用法
val playerHead = player.getSkull(1) {
    name(Component.text("玩家: ${player.name}").color(NamedTextColor.GOLD))
    addLore("在线时间: ${getOnlineTime(player)}")
}

val textureHead = "abc123def456".toSkull(1) {
    name(Component.text("材质头颅").color(NamedTextColor.AQUA))
}
```

### 📚 文档更新

#### 1. 最佳实践文档更新
- 添加了 "ItemBuilder 函数参数最佳实践" 章节
- 更新了所有示例代码以展示新的function参数用法
- 添加了头颅物品的高级用法示例
- 更新了性能优化建议

#### 2. 新增文档
- `docs/gui1/examples/skull-items.md` - 头颅物品使用示例
- `docs/gui1/api/items.md` - 物品工具API完整文档

#### 3. API文档更新
- 更新了 `docs/gui1/api/README.md` 中的物品创建方法引用
- 添加了新的头颅功能API说明

### 🧪 测试支持
创建了 `ItemUtilTest.kt` 测试类，包含：
- function参数功能测试
- 头颅创建方法测试
- 所有模板方法的function参数测试
- 可选参数测试

### 🔧 技术实现细节

#### 1. 向后兼容性
所有更改都保持了向后兼容性：
- function参数都有默认值 `{}`
- 现有的方法签名保持不变
- 现有代码无需修改即可继续工作

#### 2. 头颅实现
- 使用core模块的 `SkullUtils` 进行头颅材质设置
- 通过反射安全地设置GameProfile
- 支持URL自动补全（材质ID -> 完整URL）
- 错误处理：设置失败时保持默认头颅

#### 3. 代码质量
- 所有新方法都有完整的KDoc文档
- 遵循项目的编码规范
- 使用类型安全的方法签名
- 适当的错误处理

### 🎨 使用场景

#### 1. 增强的物品自定义
```kotlin
// 创建特殊效果的物品
val magicSword = ItemUtil.create(Material.DIAMOND_SWORD, "魔法剑") {
    enchant(Enchantment.DAMAGE_ALL, 10)
    enchant(Enchantment.FIRE_ASPECT, 3)
    flag(ItemFlag.HIDE_ENCHANTS)
    unbreakable(true)
    addLore("§c传说级武器")
    addLore("§7攻击力: +50")
}
```

#### 2. 丰富的头颅应用
```kotlin
// 玩家列表GUI
players.forEach { player ->
    slotComponent(x, y) {
        render {
            player.getSkull(1) {
                name(Component.text(player.name).color(NamedTextColor.YELLOW))
                addLore("等级: ${player.level}")
                addLore("血量: ${player.health.toInt()}/${player.maxHealth.toInt()}")
                if (player.isOp) {
                    enchant(Enchantment.LURE, 1)
                    flag(ItemFlag.HIDE_ENCHANTS)
                }
            }
        }
    }
}
```

#### 3. 装饰性头颅商店
```kotlin
// 使用自定义材质创建装饰头颅
val creeperHead = ItemUtil.urlSkull("creeper_texture_id", "苦力怕头颅") {
    addLore("价格: 100金币")
    addLore("稀有度: 普通")
}
```

### 💡 最佳实践建议

1. **性能优化**: 缓存常用的复杂物品，避免重复创建
2. **材质来源**: 使用可靠的材质来源，确保材质URL有效
3. **错误处理**: 为头颅创建添加异常处理
4. **用户体验**: 为头颅添加有意义的描述和交互提示
5. **视觉效果**: 合理使用附魔光效和物品标志增强视觉效果

### 🔄 迁移指南

现有代码无需修改，但可以选择性地利用新功能：

```kotlin
// 旧代码（仍然有效）
val button = ItemUtil.button(Material.STONE, "按钮")

// 新代码（可选升级）
val enhancedButton = ItemUtil.button(Material.STONE, "按钮") {
    enchant(Enchantment.LURE, 1)
    flag(ItemFlag.HIDE_ENCHANTS)
}
```

这次更新大大增强了GUI1模块的物品创建能力，为开发者提供了更灵活和强大的工具来创建丰富的用户界面。
