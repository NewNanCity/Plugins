# GUI1 事件处理示例

本文档展示了如何使用GUI1模块的新事件处理DSL功能。

## 📋 目录

- [BaseComponent事件处理](#basecomponent事件处理)
- [ChestPage事件处理](#chestpage事件处理)
- [BookPage事件处理](#bookpage事件处理)
- [StorageComponent物品交互](#storagecomponent物品交互)

## 🎯 BaseComponent事件处理

### 基本点击事件

```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        openPage(InventoryType.CHEST, 27, player, "事件示例") {
            slotComponent(x = 4, y = 1) { component ->
                render { context ->
                    ItemUtil.create(Material.DIAMOND) {
                        name("点击我！")
                        lore("左键：普通点击", "右键：特殊操作", "Shift+左键：批量操作")
                    }
                }

                // 使用新的DSL语法
                onClick { context ->
                    context.player.sendMessage("你点击了钻石！")
                }

                onLeftClick { context ->
                    context.player.sendMessage("左键点击 - 普通操作")
                }

                onRightClick { context ->
                    context.player.sendMessage("右键点击 - 特殊操作")
                    context.stopPropagation() // 阻止事件继续传播
                }

                onShiftLeftClick { context ->
                    context.player.sendMessage("Shift+左键 - 批量操作")
                }
            }
        }
    }
}
```

### 拖拽事件处理

```kotlin
slotComponent(x = 2, y = 2) { component ->
    render { context ->
        ItemUtil.create(Material.CHEST) {
            name("拖拽目标")
        }
    }

    onDrag { context ->
        context.player.sendMessage("检测到拖拽操作")
        context.player.sendMessage("涉及槽位: ${context.slots}")
    }

    onDragSingle { context ->
        context.player.sendMessage("单个物品拖拽")
    }

    onDragEven { context ->
        context.player.sendMessage("平均分配拖拽")
    }
}
```

## 🏗️ ChestPage事件处理

### 页面级别事件

```kotlin
openPage(InventoryType.CHEST, 54, player, "页面事件示例") { page ->
    // 页面级别的点击事件
    page.onClick { context ->
        context.player.sendMessage("页面被点击了！槽位: ${context.slot}")
    }

    // 页面关闭事件
    page.onClose { context ->
        context.player.sendMessage("页面关闭了，原因: ${context.reason}")
    }

    // 页面打开事件
    page.onOpen { context ->
        context.player.sendMessage("页面打开了！")
    }

    // 添加组件...
    slotComponent(x = 0, y = 0) {
        render { context ->
            itemUtil.createItemStack(Material.EMERALD) {
                name("组件物品")
            }
        }

        onClick { context ->
            context.player.sendMessage("组件被点击")
            // 如果不调用stopPropagation()，事件会继续传播到页面
        }
    }
}
```

## 📖 BookPage事件处理

### 可编辑书籍

```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 创建可编辑的书籍页面
        val bookPage = createBookPage(
            player = player,
            title = "我的日记",
            bookTitle = "玩家日记",
            author = player.name,
            editable = true
        ) {
            addPage("这是第一页内容")
            addPage("这是第二页内容")
        }

        // 书籍翻页事件
        bookPage.onBookPageTurn { context ->
            val direction = if (context.isNextPage) "下一页" else "上一页"
            context.player.sendMessage("翻到了${direction}，当前页码: ${context.currentPage}")
        }

        // 书籍编辑事件
        bookPage.onBookEdit { context ->
            context.player.sendMessage("书籍内容被编辑了")
            if (context.newBookMeta != null) {
                val pageCount = context.newBookMeta.pages().size
                context.player.sendMessage("新的页数: $pageCount")
            }
        }

        // 书籍签名事件
        bookPage.onBookSign { context ->
            context.player.sendMessage("书籍已签名完成！")
            context.player.sendMessage("标题: ${context.title}")
            context.player.sendMessage("作者: ${context.author}")
            context.player.sendMessage("页数: ${context.pages.size}")
        }

        // 打开书籍页面
        player.openPage(bookPage)
    }
}
```

### 只读书籍

```kotlin
val readOnlyBook = createBookPage(
    player = player,
    title = "服务器规则",
    bookTitle = "服务器规则手册",
    author = "管理员",
    editable = false
) {
    addPage("欢迎来到我们的服务器！")
    addPage("请遵守以下规则：\n1. 不要破坏他人建筑\n2. 不要使用作弊工具")
    addPage("违反规则将被处罚")
}

// 只监听翻页事件
readOnlyBook.onBookPageTurn { context ->
    // 记录玩家阅读进度
    val progress = (context.currentPage + 1).toDouble() / readOnlyBook.getTotalPages()
    context.player.sendMessage("阅读进度: ${(progress * 100).toInt()}%")
}
```

## 🗃️ StorageComponent物品交互

### 存储槽组件

```kotlin
openPage(InventoryType.CHEST, 27, player, "存储示例") {
    // 创建存储区域
    storageSlotComponent(x = 1, y = 1) { component ->
        render { context ->
            // 存储槽可以为空，显示背景物品
            context.oldItem ?: itemUtil.createItemStack(Material.GRAY_STAINED_GLASS_PANE) {
                name("存储槽")
                lore("可以放入物品")
            }
        }

        // 物品变化监听
        onItemChange { oldItem, newItem ->
            when {
                oldItem == null && newItem != null -> {
                    player.sendMessage("放入了物品: ${newItem.type}")
                }
                oldItem != null && newItem == null -> {
                    player.sendMessage("取出了物品: ${oldItem.type}")
                }
                oldItem != null && newItem != null -> {
                    player.sendMessage("替换了物品: ${oldItem.type} -> ${newItem.type}")
                }
            }
        }
    }

    // 普通组件（不允许物品交互）
    slotComponent(x = 7, y = 1) {
        render { context ->
            itemUtil.createItemStack(Material.BARRIER) {
                name("禁止交互")
                lore("这个槽位不允许放入物品")
            }
        }

        onClick { context ->
            context.player.sendMessage("这个槽位不允许物品交互！")
        }
    }
}
```

## 🔧 高级用法

### 事件传播控制

```kotlin
slotComponent(x = 4, y = 2) {
    onClick { context ->
        context.player.sendMessage("组件处理了点击事件")

        if (context.isShiftClick) {
            // 阻止事件传播到页面
            context.stopPropagation()
            context.player.sendMessage("事件传播已停止")
        }
        // 如果不是Shift点击，事件会继续传播到页面
    }
}
```

### 错误处理

所有事件处理都有自动的错误处理和日志记录：

```kotlin
onClick { context ->
    // 如果这里抛出异常，会被自动捕获并记录
    throw RuntimeException("测试异常")
}
```

异常会被记录到GuiLogger中，包含详细的上下文信息，不会影响其他组件的正常运行。
