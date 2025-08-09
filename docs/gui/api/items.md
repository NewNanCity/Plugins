# 物品工具API

GUI 模块提供了强大的物品创建和管理工具，**原生支持 i18n 国际化**。所有方法都支持可选的`function: ItemBuilder.() -> Unit`参数，允许进行高级自定义。

## 🌐 i18n 集成特性

### 自动文本处理
- **直接使用 i18n 模板**：`name("<%gui.button.confirm%>")` 自动处理语言映射
- **格式解析**：支持 MiniMessage 和 Legacy 格式的自动识别
- **参数替换**：通过 `format()` 方法支持参数替换

### 使用方式对比
```kotlin
// 旧方式（仍然支持）
item(Material.DIAMOND) {
    name(plugin.messager.sprintf("<%gui.button.confirm%>"))
    lore(plugin.messager.sprintf("<%gui.button.hint%>"))
}

// 新方式（推荐）
item(Material.DIAMOND) {
    name("<%gui.button.confirm%>")  // 直接使用i18n模板
    lore("<%gui.button.hint%>")     // 自动处理格式和语言映射
}
```

## 📦 ItemUtil 核心方法

### item (推荐使用)
创建物品构建器，支持链式调用和 i18n 集成。

```kotlin
// 在组件中使用
fun BaseComponent.item(material: Material): ItemBuilder
fun BaseComponent.item(itemStack: ItemStack): ItemBuilder
```

**i18n 集成示例:**
```kotlin
slotComponent(0, 0) {
    render {
        item(Material.DIAMOND_SWORD) {
            // 直接使用i18n模板
            name("<%gui.weapon.legendary_sword%>")
            lore("<%gui.weapon.sword_description%>")

            // 使用format方法进行参数替换
            addLore(format("<%gui.weapon.damage%>", 50))

            enchant(Enchantment.DAMAGE_ALL, 5)
            flag(ItemFlag.HIDE_ENCHANTS)
        }
    }
}
```

### create (兼容方法)
创建基本物品，支持名称、描述和自定义配置。

```kotlin
fun create(
    material: Material,
    function: ItemBuilder.() -> Unit = {}
): ItemStack
```

**示例:**
```kotlin
// 使用i18n模板
val sword = itemUtil.create(Material.DIAMOND_SWORD) {
    name("<%gui.weapon.legendary_sword%>")
    lore("<%gui.weapon.sword_description%>")
}

// 参数替换
val shield = itemUtil.create(Material.SHIELD) {
    name(format("<%gui.armor.shield_level%>", 5))
    lore(format("<%gui.armor.defense%>", 50))
}
```

## 🎭 头颅物品方法

### skull (玩家头颅)
创建玩家头颅，支持多种输入类型。

```kotlin
// 使用OfflinePlayer
fun skull(
    player: OfflinePlayer,
    displayName: String? = null,
    function: ItemBuilder.() -> Unit = {}
): ItemStack

// 使用UUID
fun skull(
    uuid: UUID,
    displayName: String? = null,
    function: ItemBuilder.() -> Unit = {}
): ItemStack

// 使用玩家名称
fun skull(
    playerName: String,
    displayName: String? = null,
    function: ItemBuilder.() -> Unit = {}
): ItemStack
```

**i18n 集成示例:**
```kotlin
// 在组件中使用
slotComponent(0, 0) {
    render {
        skull(player) {
            // 直接使用i18n模板
            name("<%gui.player.info_title%>")
            lore("<%gui.player.click_hint%>")

            // 使用format进行参数替换
            addLore(format("<%gui.player.level%>", player.level))
            addLore(format("<%gui.player.health%>", player.health, player.maxHealth))
        }
    }
}

// 使用玩家名称
skull("Notch") {
    name("<%gui.player.creator%>")
    lore("<%gui.player.creator_description%>")
    enchant(Enchantment.LURE, 1)
    flag(ItemFlag.HIDE_ENCHANTS)
}
```

### urlSkull (URL材质头颅)
使用材质URL创建自定义头颅。

```kotlin
fun urlSkull(
    textureUrl: String,
    displayName: String? = null,
    function: ItemBuilder.() -> Unit = {}
): ItemStack
```

**参数说明:**
- `textureUrl`: 材质URL或材质ID
  - 完整URL: `"http://textures.minecraft.net/texture/abc123"`
  - 材质ID: `"abc123"` (自动补全为完整URL)

**i18n 集成示例:**
```kotlin
// 在组件中使用 - 上一页按钮
slotComponent(2, 5) {
    render {
        urlSkull("37aee9a75bf0df7897183015cca0b2a7d755c63388ff01752d5f4419fc645") {
            // 直接使用i18n模板
            name("<%gui.pagination.previous_page%>")
            lore("<%gui.pagination.previous_hint%>")
        }
    }
}

// 下一页按钮
slotComponent(6, 5) {
    render {
        urlSkull("682ad1b9cb4dd21259c0d75aa315ff389c3cef752be3949338164bac84a96e") {
            name("<%gui.pagination.next_page%>")
            lore("<%gui.pagination.next_hint%>")
            enchant(Enchantment.LURE, 1)
            flag(ItemFlag.HIDE_ENCHANTS)
        }
    }
}
```

### customSkull (Base64头颅)
使用Base64编码的材质数据创建头颅。

```kotlin
fun customSkull(
    base64Texture: String,
    displayName: String? = null,
    function: ItemBuilder.() -> Unit = {}
): ItemStack
```

**示例:**
```kotlin
val base64Texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWJjMTIzIn19fQ=="
val base64Head = ItemUtil.customSkull(base64Texture, "Base64头颅") {
    addLore("Base64编码的头颅材质")
    amount(1)
}
```

## 🎨 GUI模板方法

所有GUI模板方法都支持`function`参数进行自定义。

### 基础模板

```kotlin
// 分隔符
fun separator(
    material: Material = Material.GRAY_STAINED_GLASS_PANE,
    name: String = " ",
    function: ItemBuilder.() -> Unit = {}
): ItemStack

// 边框
fun border(
    material: Material = Material.BLACK_STAINED_GLASS_PANE,
    function: ItemBuilder.() -> Unit = {}
): ItemStack

// 空槽位
fun emptySlot(
    material: Material = Material.LIGHT_GRAY_STAINED_GLASS_PANE,
    function: ItemBuilder.() -> Unit = {}
): ItemStack
```

### 按钮模板

```kotlin
// 通用按钮
fun button(
    material: Material,
    name: String,
    lore: List<String> = emptyList(),
    function: ItemBuilder.() -> Unit = {}
): ItemStack

// 导航按钮
fun navigation(
    material: Material,
    name: String,
    description: String? = null,
    function: ItemBuilder.() -> Unit = {}
): ItemStack

// 翻页按钮
fun pageButton(
    isNext: Boolean,
    currentPage: Int,
    totalPages: Int,
    function: ItemBuilder.() -> Unit = {}
): ItemStack
```

### 状态按钮

```kotlin
// 关闭按钮
fun closeButton(name: String = "关闭", function: ItemBuilder.() -> Unit = {}): ItemStack

// 返回按钮
fun backButton(name: String = "返回", function: ItemBuilder.() -> Unit = {}): ItemStack

// 确认按钮
fun confirmButton(name: String = "确认", function: ItemBuilder.() -> Unit = {}): ItemStack

// 取消按钮
fun cancelButton(name: String = "取消", function: ItemBuilder.() -> Unit = {}): ItemStack
```

### 信息模板

```kotlin
// 信息物品
fun info(
    material: Material,
    name: String,
    lore: List<String> = emptyList(),
    function: ItemBuilder.() -> Unit = {}
): ItemStack

// 警告物品
fun warning(
    material: Material = Material.BARRIER,
    name: String,
    lore: List<String> = emptyList(),
    function: ItemBuilder.() -> Unit = {}
): ItemStack

// 成功物品
fun success(
    material: Material = Material.LIME_DYE,
    name: String,
    lore: List<String> = emptyList(),
    function: ItemBuilder.() -> Unit = {}
): ItemStack

// 加载中物品
fun loading(name: String = "加载中...", function: ItemBuilder.() -> Unit = {}): ItemStack

// 错误物品
fun error(
    name: String = "加载失败",
    description: String = "点击重试",
    function: ItemBuilder.() -> Unit = {}
): ItemStack
```

## ✨ 扩展函数

### 头颅扩展函数

```kotlin
// 玩家头颅扩展
fun OfflinePlayer.getSkull(
    amount: Int = 1,
    function: ItemBuilder.() -> Unit = {}
): ItemStack

// 字符串转头颅（材质ID或URL）
fun String.toSkull(
    amount: Int = 1,
    function: ItemBuilder.() -> Unit = {}
): ItemStack

// URL转头颅
fun URL.toSkull(
    amount: Int = 1,
    function: ItemBuilder.() -> Unit = {}
): ItemStack
```

**示例:**
```kotlin
// 使用扩展函数
val playerHead = player.getSkull(1) {
    name(Component.text("玩家: ${player.name}").color(NamedTextColor.GOLD))
    addLore("在线时间: ${getOnlineTime(player)}")
}

val textureHead = "abc123def456".toSkull(1) {
    name(Component.text("材质头颅").color(NamedTextColor.AQUA))
    addLore("材质ID: abc123def456")
}
```

### 物品修饰扩展

```kotlin
// 添加发光效果
fun ItemStack.glowing(function: ItemBuilder.() -> Unit = {}): ItemStack

// 修改物品属性
fun ItemStack.modify(builder: ItemBuilder.() -> Unit): ItemStack

// 设置数量
fun ItemStack.withAmount(amount: Int): ItemStack
```

## 🔧 高级用法

### function参数的强大功能

`function`参数允许你使用ItemBuilder的所有方法：

```kotlin
val advancedItem = ItemUtil.create(Material.DIAMOND_SWORD, "高级武器") {
    // 添加附魔
    enchant(Enchantment.DAMAGE_ALL, 5)
    enchant(Enchantment.FIRE_ASPECT, 2)

    // 隐藏标志
    flag(ItemFlag.HIDE_ENCHANTS)
    flag(ItemFlag.HIDE_ATTRIBUTES)

    // 设置属性
    unbreakable(true)
    amount(1)

    // 添加描述
    addLore("攻击力: +50")
    addLore("火焰附加: +20")
    addLore("")
    addLore("§c传说级武器")

    // 自定义meta
    meta { meta ->
        // 进行更复杂的meta操作
    }
}
```

### 性能优化建议

```kotlin
// ✅ 缓存常用物品
object ItemCache {
    private val cache = mutableMapOf<String, ItemStack>()

    fun getButton(key: String): ItemStack {
        return cache.getOrPut(key) {
            ItemUtil.button(Material.STONE, "按钮") {
                enchant(Enchantment.LURE, 1)
                flag(ItemFlag.HIDE_ENCHANTS)
            }
        }
    }
}

// ❌ 避免重复创建
// 每次都创建新物品，性能较差
slotComponent(x = 4, y = 2) {
    render {
        ItemUtil.create(Material.DIAMOND, "昂贵的物品") {
            // 复杂的创建逻辑...
        }
    }
}
```

通过这些API，你可以创建丰富多样的GUI物品，提升用户界面的视觉效果和交互体验。
