# 会话管理详解

Session是GUI1中的核心概念之一，负责管理页面栈和导航逻辑。本文详细介绍Session的工作原理和使用方法。

## 🏗️ Session架构

### 基本概念

Session类似于浏览器的标签页，每个玩家可以有多个Session，每个Session维护一个Page栈：

```
Session
├── Page Stack (页面栈)
│   ├── Page 3 (栈顶 - 当前显示)
│   ├── Page 2
│   └── Page 1 (栈底)
├── Visibility State (可见状态)
└── Player Reference (玩家引用)
```

### 栈操作原理

Session使用栈（Stack）数据结构管理页面：

```kotlin
// 栈的基本操作
val session = getDefaultSession(player)

// 1. push - 压入新页面到栈顶
session.push(newPage)  // [Page1, Page2, newPage] ← 栈顶

// 2. pop - 弹出栈顶页面
val poppedPage = session.pop()  // 返回 newPage，栈变为 [Page1, Page2]

// 3. current - 获取栈顶页面（不移除）
val currentPage = session.current()  // 返回 Page2

// 4. size - 获取栈大小
val stackSize = session.size()  // 返回 2
```

## 🔄 页面生命周期管理

### 自动生命周期

Session会自动管理页面的生命周期：

```kotlin
// 当页面被压入栈时
session.push(page) // 自动调用 page.show()

// 当页面被弹出栈时
session.pop() // 自动销毁被弹出的页面，然后显示新的栈顶页面

// 当页面被替换时
session.replace(newPage) // 销毁旧页面，显示新页面

// 当Session关闭时
session.close() // 自动销毁所有页面并关闭Session

// 当玩家关闭容器时
// 页面自动监听InventoryCloseEvent并调用page.close()
```

### 页面状态转换

```
创建 → 压入栈 → 显示 → 隐藏 → 弹出栈/关闭 → 销毁
  ↓      ↓      ↓      ↓       ↓           ↓
Page() → push() → show() → hide() → pop()/close() → destroy
```

### 页面销毁触发条件

页面会在以下情况下被销毁：
1. **session.pop()** - 弹出栈顶页面
2. **session.replace()** - 替换页面
3. **session.goto()** - 跳转时移除后续页面
4. **session.clear()** - 清空所有页面
5. **session.close()** - 关闭Session
6. **page.close()** - 直接关闭页面
7. **容器关闭事件** - 玩家关闭容器时自动触发

## 📱 导航操作详解

### 推荐的页面创建方法

GUI1提供了多种创建页面的方法，推荐使用以下方式：

```kotlin
// 🌟 最佳实践：使用openPage函数（推荐）
fun openNewPage(player: Player) {
    openPage(InventoryType.CHEST, 27, player) {
        title("新页面")
        // 配置页面内容...
    }
    // openPage会自动创建页面、压入Session栈并显示
}

// 🌟 最佳实践：使用Session的openPage方法
fun openNewPageInSession(player: Player) {
    val session = getDefaultSession(player)
    session.openPage(InventoryType.CHEST, 27) {
        title("新页面")
        // 配置页面内容...
    }
}

// ⚠️ 高级用法：手动创建和管理页面（不推荐新手使用）
fun openNewPageAdvanced(player: Player) {
    val page = createPage(player, "新页面", InventoryType.CHEST, 27) {
        // 配置页面内容...
    }

    val session = getDefaultSession(player)
    session.push(page)  // 需要手动压入栈
    page.show()         // 需要手动显示
}
```

### 1. 基本导航

// 返回上一页
fun goBack(player: Player) {
    val session = getDefaultSession(player)
    val poppedPage = session.pop()  // 弹出并销毁当前页面，显示上一页

    if (poppedPage != null) {
        player.sendMessage("&7已返回上一页")
    } else {
        player.sendMessage("&7已经是第一页了")
    }
}
```

### 2. 页面替换

```kotlin
// 替换当前页面（不增加栈深度）
fun replaceCurrentPage(player: Player) {
    val newPage = createPage(player, "替换页面", InventoryType.CHEST, 27)

    val session = getDefaultSession(player)
    session.replace(newPage)  // 销毁旧页面，替换为新页面
}
```

### 3. 跳转导航

```kotlin
// 跳转到指定位置
fun jumpToPage(player: Player, pageIndex: Int) {
    val session = getDefaultSession(player)

    try {
        session.goto(pageIndex)  // 跳转到指定页面，销毁其后的所有页面
        player.sendMessage("&a已跳转到页面 $pageIndex")
    } catch (e: IndexOutOfBoundsException) {
        player.sendMessage("&c无效的页面索引")
    }
}

// 跳转到第一页
fun goToFirstPage(player: Player) {
    getDefaultSession(player).goto(0)
}

// 跳转到上一页（相对于当前位置）
fun goToPreviousPage(player: Player) {
    val session = getDefaultSession(player)
    val currentIndex = session.size() - 1
    if (currentIndex > 0) {
        session.goto(currentIndex - 1)
    }
}
```

## 🎯 实际应用场景

### 1. 多级菜单导航

```kotlin
// 主菜单
fun showMainMenu(player: Player) {
    openPage(InventoryType.CHEST, 54, player) {
        title("&6主菜单")

        // 玩家管理按钮
        slotComponent(x = 2, y = 2) {
            render { itemUtil.createItemStack(Material.PLAYER_HEAD) { name("&e玩家管理") } }
            onLeftClick { showPlayerManagementMenu(player) }
        }

        // 服务器设置按钮
        slotComponent(x = 6, y = 2) {
            render { itemUtil.createItemStack(Material.REDSTONE) { name("&c服务器设置") } }
            onLeftClick { showServerSettingsMenu(player) }
        }
    }
}

// 玩家管理菜单（二级菜单）
fun showPlayerManagementMenu(player: Player) {
    // 使用推荐的openPage方法
    openPage(InventoryType.CHEST, 27, player) {
        title("&e玩家管理")

        // 在线玩家列表
        slotComponent(x = 2, y = 1) {
            render { itemUtil.createItemStack(Material.EMERALD) { name("&a在线玩家") } }
            onLeftClick { showOnlinePlayersMenu(player) }
        }

        // 返回按钮
        addBackButton()
    }
}

// 在线玩家菜单（三级菜单）
fun showOnlinePlayersMenu(player: Player) {
    // 使用推荐的openPage方法
    openPage(InventoryType.CHEST, 54, player) {
        title("&a在线玩家")

        // 玩家列表...

        // 返回按钮
        addBackButton()
    }
}

// 通用返回按钮
fun Page.addBackButton() {
    slotComponent(x = 0, y = 5) {
        render {
            itemUtil.createItemStack(Material.ARROW) {
                name("&7返回")
                lore("&7返回上一页")
            }
        }
        onLeftClick {
            val poppedPage = session.pop()
            if (poppedPage == null) {
                player.sendMessage("&7已经是第一页了")
            }
        }
    }
}
```

### 2. 向导式流程

```kotlin
// 创建公会向导 - 第一步：输入公会名称
fun startCreateGuildWizard(player: Player) {
    // 使用推荐的openPage方法
    openPage(InventoryType.HOPPER, player = player) {
        title("&6创建公会 - 第1步")

        slotComponent(x = 2, y = 0) {
            render {
                itemUtil.createItemStack(Material.WRITABLE_BOOK) {
                    name("&e输入公会名称")
                    lore("&7点击在聊天框中输入公会名称")
                }
            }
            onLeftClick {
                page.chatInput { guildName ->
                    if (isValidGuildName(guildName)) {
                        showCreateGuildStep2(player, guildName)
                        true
                    } else {
                        player.sendMessage("&c公会名称无效，请重新输入")
                        false
                    }
                }
            }
        }
    }
}

// 第二步：选择公会图标
fun showCreateGuildStep2(player: Player, guildName: String) {
    // 使用Session的replace方法替换当前页面
    val session = getDefaultSession(player)
    val page = createPage(player, "&6创建公会 - 第2步", InventoryType.CHEST, 27) {
        title("&6选择公会图标")

        // 图标选择...

        // 下一步按钮
        slotComponent(x = 8, y = 2) {
            render { itemUtil.createItemStack(Material.ARROW) { name("&a下一步") } }
            onLeftClick { showCreateGuildStep3(player, guildName, selectedIcon) }
        }

        // 返回按钮
        addBackButton()
    }

    session.replace(page) // 替换当前页面
}

// 第三步：确认创建
fun showCreateGuildStep3(player: Player, guildName: String, icon: Material) {
    // 使用推荐的openPage方法
    openPage(InventoryType.HOPPER, player = player) {
        title("&6创建公会 - 确认")

        // 确认信息显示...

        // 确认按钮
        slotComponent(x = 1, y = 0) {
            render { itemUtil.createItemStack(Material.GREEN_WOOL) { name("&a确认创建") } }
            onLeftClick {
                createGuild(player, guildName, icon)
                session.clear() // 清空整个栈，回到主界面
                showMainMenu(player)
            }
        }

        // 取消按钮
        slotComponent(x = 3, y = 0) {
            render { itemUtil.createItemStack(Material.RED_WOOL) { name("&c取消") } }
            onLeftClick {
                session.goto(0) // 跳转到第一页
            }
        }
    }
}
```

## 🔧 高级功能

### 1. Session清理操作

```kotlin
// 清空Session中的所有页面（但保持Session开启）
fun clearAllPages(player: Player) {
    val session = getDefaultSession(player)
    session.clear() // 销毁所有页面，栈变为空
    player.sendMessage("&7所有页面已清空")
}

// 关闭整个Session（销毁所有页面并关闭Session）
fun closeSession(player: Player) {
    val session = getDefaultSession(player)
    session.close() // 销毁所有页面并关闭Session
    player.sendMessage("&7Session已关闭")
}

// 清空并重新开始
fun restartSession(player: Player) {
    val session = getDefaultSession(player)
    session.clear() // 清空所有页面

    // 打开新的主页面
    showMainMenu(player)
}
```

### 2. 多Session管理

```kotlin
// 为不同功能使用不同的Session
fun openAdminPanel(player: Player) {
    val adminSession = getSession(player, "admin")
    // 使用Session的openPage方法（推荐）
    adminSession.openPage(InventoryType.CHEST, 54) {
        title("&c管理面板")
        // 配置管理面板内容...
    }
}

fun openShop(player: Player) {
    val shopSession = getSession(player, "shop")
    // 使用Session的openPage方法（推荐）
    shopSession.openPage(InventoryType.CHEST, 54) {
        title("&e商店")
        // 配置商店内容...
    }
}

// 关闭特定Session
fun closeAdminPanel(player: Player) {
    val adminSession = getSession(player, "admin")
    adminSession.close() // 关闭管理面板Session
}
```

### 3. Session状态检查

```kotlin
fun checkSessionState(player: Player) {
    val session = getDefaultSession(player)

    logger.info("玩家 ${player.name} 的Session状态:")
    logger.info("- 栈大小: ${session.size()}")
    logger.info("- 当前页面: ${session.current()?.title ?: "无"}")
    logger.info("- 是否可见: ${session.current()?.isVisible ?: false}")

    // 获取所有页面
    val allPages = session.getAllPages()
    allPages.forEachIndexed { index, page ->
        logger.info("- 页面 $index: ${page.title}")
    }
}
```

### 4. 安全的导航操作

```kotlin
fun safeGoBack(player: Player): Boolean {
    val session = getDefaultSession(player)

    return if (session.size() > 1) {
        session.pop()
        true
    } else {
        player.sendMessage("&7已经是第一页了")
        false
    }
}

fun safeJumpTo(player: Player, index: Int): Boolean {
    val session = getDefaultSession(player)

    return try {
        if (index >= 0 && index < session.size()) {
            session.goto(index)
            true
        } else {
            player.sendMessage("&c无效的页面索引: $index")
            false
        }
    } catch (e: Exception) {
        player.sendMessage("&c跳转失败: ${e.message}")
        false
    }
}
```

## 💡 最佳实践

### 1. 合理的栈深度
- 避免过深的页面栈（建议不超过5层）
- 提供快速返回主页的方式
- 在关键节点提供"面包屑"导航

### 2. 用户体验
- 总是提供返回按钮
- 在重要操作前提供确认
- 使用一致的导航模式

### 3. 资源管理
- Session会自动管理页面生命周期
- 避免手动调用`page.close()`
- 使用`session.clear()`清空整个栈

### 4. 错误处理
- 检查栈是否为空
- 处理索引越界异常
- 提供友好的错误提示

通过合理使用Session系统，你可以创建流畅、直观的GUI导航体验！
