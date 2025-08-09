# GUI Builder Pattern Enhancement

## 概述

GUI模块现在支持Builder模式的延迟执行机制，确保页面配置代码在页面完全初始化后才执行，避免了竞态条件和初始化顺序问题。

## 🚀 主要改进

### 延迟执行机制
- **问题**：之前的builder在页面创建时立即执行，可能导致页面未完全初始化就开始配置
- **解决方案**：builder现在在`initInternal()`方法中执行，确保页面完全准备好

### 类型安全
- `BasePage.() -> Unit` - 通用页面builder
- `ChestPage.() -> Unit` - 箱子页面专用builder  
- `BookPage.() -> Unit` - 书本页面专用builder

### 向后兼容
- 所有现有代码无需修改
- Builder参数都有默认值`{}`
- API保持不变

## 📝 使用示例

### 基础用法

```kotlin
// ChestPage Builder
val page = plugin.createChestPage(player, "示例", 27) {
    // 这里的代码在页面初始化时执行，而不是创建时
    addComponent(ButtonComponent(0, item) { /* handler */ })
}

// BookPage Builder  
val bookPage = plugin.createBookPage(player, "书本", "作者") {
    addPage("第一页内容")
    addPage("第二页内容")
}

// 通用Page Builder
val genericPage = plugin.createPage(player, "页面", InventoryType.HOPPER) {
    addComponent(LabelComponent(0, item, "标签"))
}
```

### 直接打开页面

```kotlin
plugin.openPage(InventoryType.CHEST, 54, player, "菜单") {
    // 页面配置
    for (i in 0..8) {
        addComponent(ButtonComponent(i, item) { /* handler */ })
    }
}
```

### 复杂配置

```kotlin
plugin.createChestPage(player, "动态菜单", 45) {
    val playerLevel = player.level
    
    // 根据玩家等级动态配置
    when {
        playerLevel >= 50 -> {
            addComponent(LabelComponent(4, diamondItem, "高级玩家"))
        }
        playerLevel >= 20 -> {
            addComponent(LabelComponent(4, goldItem, "中级玩家"))
        }
        else -> {
            addComponent(LabelComponent(4, ironItem, "新手玩家"))
        }
    }
}
```

## 🔧 实现细节

### BasePage
```kotlin
abstract class BasePage(
    // ... 其他参数
    private val builder: BasePage.() -> Unit = {}
) : Page {
    
    override fun initInternal() {
        if (initialized) return
        
        try {
            onInit()
            builder() // 在这里调用builder
            initialized = true
            eventHandlers.handleEvent(InitEventContext(player))
        } catch (e: Exception) {
            // 错误处理
        }
    }
}
```

### ChestPage
```kotlin
class ChestPage(
    // ... 其他参数
    private val chestBuilder: ChestPage.() -> Unit = {}
) : BasePage(/* ... */) {
    
    override fun initInternal() {
        super.initInternal() // 先调用父类初始化
        
        try {
            chestBuilder() // 调用ChestPage特定的builder
        } catch (e: Exception) {
            // 错误处理
        }
    }
}
```

### BookPage
```kotlin
class BookPage(
    // ... 其他参数
    private val bookBuilder: BookPage.() -> Unit = {}
) : Page {
    
    override fun initInternal() {
        if (initialized) return
        
        try {
            onInit()
            bookBuilder() // 调用BookPage特定的builder
            initialized = true
            eventHandlers.handleEvent(InitEventContext(player))
        } catch (e: Exception) {
            // 错误处理
        }
    }
}
```

## 🎯 优势

### 1. 避免竞态条件
- Builder在页面完全初始化后执行
- 确保所有页面属性都已准备好
- 避免访问未初始化的资源

### 2. 更好的错误处理
- Builder执行错误会被正确捕获和记录
- 提供详细的错误上下文信息
- 不会影响页面的基础初始化

### 3. 类型安全
- 每个页面类型都有专用的builder类型
- 编译时检查builder参数匹配
- IDE提供更好的代码补全和类型提示

### 4. 灵活性
- 支持复杂的动态配置逻辑
- 可以在builder中访问页面的所有属性和方法
- 支持条件配置和循环配置

## 📚 相关文件

- `BasePage.kt` - 基础页面实现
- `ChestPage.kt` - 箱子页面实现  
- `BookPage.kt` - 书本页面实现
- `GuiManager.kt` - GUI管理器
- `BasePluginExtensions.kt` - DSL扩展函数
- `BuilderPatternExample.kt` - 使用示例

## 🔄 迁移指南

### 现有代码
现有代码无需修改，因为：
- 所有builder参数都有默认值`{}`
- API保持完全兼容
- 行为保持一致（除了执行时机）

### 新代码建议
- 使用新的builder模式进行页面配置
- 利用类型安全的builder参数
- 在builder中进行复杂的动态配置

## ⚠️ 注意事项

1. **Builder执行时机**：Builder在`initInternal()`中执行，不是页面创建时
2. **异常处理**：Builder中的异常会被捕获并记录，但会中断页面初始化
3. **资源访问**：在builder中可以安全访问页面的所有属性和方法
4. **性能考虑**：Builder应该避免执行耗时操作，因为它在主线程中执行

这个改进使GUI模块更加健壮和易用，同时保持了完全的向后兼容性。
