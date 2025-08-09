# GUI 模块核心概念

理解GUI模块的核心概念是掌握这个框架的关键。本文将详细介绍五个核心概念及其关系。

## 🏗️ 架构概览

GUI模块基于五个核心概念构建：

```
Session (会话)
    ├── Page (页面)
    │   ├── Component (组件)
    │   │   ├── Item (物品)
    │   │   └── Event (事件)
    │   └── Event (事件)
    └── Navigation (导航)
```

### 设计理念

GUI模块采用现代化的架构设计，借鉴了Web开发的最佳实践：

- **声明式编程**：使用DSL描述界面结构，关注"是什么"而非"怎么做"
- **组件化设计**：通过组合不同Component构建复杂界面
- **生命周期管理**：自动绑定资源生命周期，防止内存泄漏
- **事件驱动**：基于事件的交互模型，支持事件冒泡机制

## 📱 Session (会话系统)

### 概念理解
Session类似于浏览器中的标签页，每个玩家可以有多个Session，每个Session维护一个Page栈。

### 核心特性
- **页面栈管理**：支持push、pop、replace操作
- **导航历史**：可以前进、后退、跳转到指定页面
- **自动清理**：玩家离线时自动清理所有Session
- **跨插件共享**：支持不同插件的GUI之间导航

### 使用示例
```kotlin
// 获取玩家的默认Session
val session = getDefaultSession(player)

// 创建新页面并压入栈
val page = createPage(player, "新页面", InventoryType.CHEST, 27)
session.push(page)

// 显示当前页面
session.show()

// 返回上一页
session.pop()

// 跳转到指定位置
session.goto(0) // 跳转到第一页
```

### Session方法详解
```kotlin
interface Session {
    fun push(page: Page)           // 压入新页面到栈顶
    fun pop(): Page?               // 弹出并销毁栈顶页面，显示下一页
    fun replace(page: Page)        // 替换栈顶页面（销毁旧页面）
    fun show()                     // 显示当前栈顶页面
    fun hide()                     // 隐藏当前栈顶页面
    fun goto(index: Int)           // 跳转到指定页面，销毁其后的所有页面
    fun current(): Page?           // 获取当前栈顶页面
    fun size(): Int                // 获取栈大小
    fun clear()                    // 清空所有页面（销毁所有页面）
    fun close()                    // 关闭Session（销毁所有页面并关闭Session）
}
```

## 📄 Page (页面系统)

### 概念理解
Page是GUI的基本单位，对应一个Minecraft inventory界面。每个Page必须属于某个Session。

### 页面类型

#### 标准容器页面
```kotlin
// CHEST页面（可变大小）
createPage(player, "箱子", InventoryType.CHEST, 54)

// 工作台页面
createPage(player, "工作台", InventoryType.WORKBENCH)

// 熔炉页面
createPage(player, "熔炉", InventoryType.FURNACE)

// 铁砧页面
createPage(player, "铁砧", InventoryType.ANVIL)
```

#### 特殊页面
```kotlin
// 书本页面
createBookPage(player, "说明书", "游戏指南", "服务器")
```

### 页面生命周期
```kotlin
// 1. 创建 - 实例化页面对象
val page = createPage(player, "测试", InventoryType.CHEST, 27)

// 2. 打开 - 显示给玩家
page.show()

// 3. 隐藏 - 隐藏界面但保持在Session栈中
page.hide()

// 4. 关闭 - 从Session中移除并销毁页面
page.close() // 外部方法，会调用session.pop()并销毁页面

// 5. 自动关闭 - 当玩家关闭容器时自动触发
// 页面会监听InventoryCloseEvent并自动调用page.close()
```

## 🧩 Component (组件系统)

### 概念理解
Component是页面的组成部分，负责渲染特定区域的物品和处理交互事件。

### 组件类型

#### 基础组件
```kotlin
// 单槽组件 - 最简单的组件
slotComponent(x = 4, y = 2) {
    render { item(Material.DIAMOND) }
    onLeftClick { player.sendMessage("点击了钻石！") }
}
```

#### 填充组件
```kotlin
// 矩形填充组件
rectFillComponent(x = 0, y = 0, width = 9, height = 1) {
    render { item(Material.GRAY_STAINED_GLASS_PANE) }
}

// 模式填充组件
patternFillComponent(
    pattern = listOf(
        "aaa",
        "aba", 
        "aaa"
    )
) {
    setItem('a') { item(Material.STONE) }
    setItem('b') { item(Material.DIAMOND) }
}
```

#### 高级组件
```kotlin
// 分页组件
paginatedComponent(x = 0, y = 1, width = 9, height = 4) {
    items(playerList) // 设置数据源

    render { player, index ->
        skull(player) {
            name("&e${player.name}")
        }
    }

    onItemClick { player, clickedPlayer ->
        // 处理点击事件
    }
}
```

## 🎨 Item (物品渲染)

### 概念理解
Item是GUI中显示的最小单元，通过ItemUtil创建和管理。

### 物品创建
```kotlin
// 基础物品
item(Material.DIAMOND) {
    name("&b钻石")
    lore("&7珍贵的宝石")
    amount(5)
}

// 玩家头颅
skull(player) {
    name("&e${player.name}")
    lore("&7点击查看详情")
}

// 附魔物品
item(Material.DIAMOND_SWORD) {
    name("&6神剑")
    enchant(Enchantment.SHARPNESS, 5)
    enchant(Enchantment.UNBREAKING, 3)
    hideEnchants() // 隐藏附魔描述
}
```

## ⚡ Event (事件系统)

### 概念理解
Event处理用户与GUI的交互，支持事件冒泡机制：item → component → page。

### 事件类型
```kotlin
// 点击事件
onLeftClick { context -> }      // 左键点击
onRightClick { context -> }     // 右键点击
onShiftClick { context -> }     // Shift+点击
onMiddleClick { context -> }    // 中键点击
onDoubleClick { context -> }    // 双击

// 拖拽事件
onDrag { context -> }           // 拖拽
onDragSingle { context -> }     // 单个拖拽
onDragEven { context -> }       // 平均拖拽

// 生命周期事件
onOpen { context -> }           // 打开
onClose { context -> }          // 关闭
```

### 事件上下文
```kotlin
onLeftClick { context ->
    val player = context.player         // 点击的玩家
    val slot = context.slot             // 点击的槽位
    val clickType = context.clickType   // 点击类型
    val item = context.item             // 点击的物品
    val event = context.event           // 原始Bukkit事件

    // 阻止事件传播
    context.stopPropagation()
}
```

## 🔗 概念关系

### 层次关系
```
GuiManager
    └── Session
        └── Page Stack
            ├── Page 1
            │   ├── Component A
            │   │   ├── Item
            │   │   └── Events
            │   └── Component B
            └── Page 2
```

### 生命周期绑定
- **GuiManager** 绑定到 **BasePlugin**
- **Session** 绑定到 **GuiManager**
- **Page** 绑定到 **Session**
- **Component** 绑定到 **Page**

### 数据流向
```
User Input → Event → Component → Page → Session → GuiManager
```

## 💡 设计原则

### 1. 单一职责
- Session负责导航管理
- Page负责界面布局
- Component负责区域渲染
- Item负责物品展示
- Event负责交互处理

### 2. 组合优于继承
- 通过组合不同Component构建复杂界面
- 避免深层继承关系

### 3. 声明式编程
- 使用DSL描述界面结构
- 关注"是什么"而非"怎么做"

### 4. 资源自动管理
- 自动绑定生命周期
- 防止内存泄漏

理解了这些核心概念，你就可以开始构建复杂而强大的GUI界面了！

## 🚀 下一步

- [快速入门](GETTING_STARTED.md) - 开始使用GUI模块
- [教程指南](tutorials/) - 系统学习GUI开发
- [API文档](api/) - 查阅详细API参考
