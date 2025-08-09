# GUI1 头颅物品使用示例

本文档展示如何在GUI1中使用各种类型的头颅物品，包括玩家头颅、URL材质头颅和Base64头颅。

## 🎭 玩家头颅

### 基本玩家头颅

```kotlin
// 使用玩家对象创建头颅
slotComponent(x = 4, y = 1) {
    render {
        ItemUtil.skull(player, "玩家信息") {
            addLore("等级: ${player.level}")
            addLore("血量: ${player.health}/${player.maxHealth}")
            addLore("点击查看详情")
        }
    }
    onLeftClick { showPlayerDetails(player) }
}

// 使用玩家名称创建头颅
slotComponent(x = 5, y = 1) {
    render {
        ItemUtil.skull("Notch", "创始人") {
            addLore("Minecraft 创始人")
            addLore("传奇人物")
            enchant(Enchantment.LURE, 1)
            flag(ItemFlag.HIDE_ENCHANTS)
        }
    }
}

// 使用UUID创建头颅
slotComponent(x = 6, y = 1) {
    render {
        ItemUtil.skull(UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"), "特定玩家") {
            addLore("UUID: 069a79f4-44e9-4726-a5be-fca90e38aaf5")
        }
    }
}
```

### 使用扩展函数

```kotlin
// 玩家扩展函数
slotComponent(x = 4, y = 2) {
    render {
        player.getSkull(1) {
            name(Component.text("玩家: ${player.name}").color(NamedTextColor.GOLD))
            addLore("在线时间: ${getOnlineTime(player)}")
            addLore("最后登录: ${getLastLogin(player)}")
        }
    }
}
```

## 🎨 自定义材质头颅

### URL材质头颅

```kotlin
// 使用完整URL
slotComponent(x = 2, y = 2) {
    render {
        ItemUtil.urlSkull("http://textures.minecraft.net/texture/abc123def456", "自定义头颅") {
            addLore("这是一个自定义材质的头颅")
            addLore("材质来源: Minecraft官方")
            enchant(Enchantment.LURE, 1)
            flag(ItemFlag.HIDE_ENCHANTS)
        }
    }
}

// 使用材质ID（自动补全URL）
slotComponent(x = 3, y = 2) {
    render {
        ItemUtil.urlSkull("abc123def456", "简化URL头颅") {
            addLore("材质ID: abc123def456")
            addLore("自动补全为完整URL")
        }
    }
}

// 使用String扩展函数
slotComponent(x = 4, y = 2) {
    render {
        "abc123def456".toSkull(1) {
            name(Component.text("材质头颅").color(NamedTextColor.AQUA))
            addLore("使用String扩展函数创建")
            addLore("材质ID: abc123def456")
        }
    }
}

// 使用URL扩展函数
slotComponent(x = 5, y = 2) {
    render {
        URL("http://textures.minecraft.net/texture/def456abc123").toSkull(1) {
            name(Component.text("URL头颅").color(NamedTextColor.GREEN))
            addLore("使用URL扩展函数创建")
        }
    }
}
```

### Base64材质头颅

```kotlin
// 使用Base64编码的材质数据
slotComponent(x = 6, y = 2) {
    render {
        val base64Texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWJjMTIzZGVmNDU2In19fQ=="
        ItemUtil.customSkull(base64Texture, "Base64头颅") {
            addLore("Base64编码的头颅材质")
            addLore("适用于复杂的材质数据")
            amount(1)
        }
    }
}
```

## 🎯 实际应用示例

### 玩家列表GUI

```kotlin
fun createPlayerListGUI(players: List<Player>) {
    openPage(InventoryType.CHEST, 54, viewer) {
        title("在线玩家列表")
        
        players.forEachIndexed { index, player ->
            val x = index % 9
            val y = index / 9
            
            slotComponent(x, y) {
                render {
                    player.getSkull(1) {
                        name(Component.text(player.name).color(NamedTextColor.YELLOW))
                        addLore("等级: ${player.level}")
                        addLore("血量: ${player.health.toInt()}/${player.maxHealth.toInt()}")
                        addLore("世界: ${player.world.name}")
                        addLore("")
                        addLore("点击传送到该玩家")
                        
                        // 根据玩家状态添加特效
                        if (player.isOp) {
                            enchant(Enchantment.LURE, 1)
                            flag(ItemFlag.HIDE_ENCHANTS)
                        }
                    }
                }
                
                onLeftClick {
                    viewer.teleport(player.location)
                    viewer.sendMessage("已传送到 ${player.name}")
                }
            }
        }
    }
}
```

### 装饰性头颅菜单

```kotlin
fun createDecorationMenu() {
    openPage(InventoryType.CHEST, 27, viewer) {
        title("装饰头颅商店")
        
        // 动物头颅
        slotComponent(x = 1, y = 1) {
            render {
                ItemUtil.urlSkull("creeper_texture_id", "苦力怕头颅") {
                    addLore("价格: 100金币")
                    addLore("稀有度: 普通")
                    addLore("点击购买")
                }
            }
        }
        
        slotComponent(x = 2, y = 1) {
            render {
                ItemUtil.urlSkull("enderman_texture_id", "末影人头颅") {
                    addLore("价格: 500金币")
                    addLore("稀有度: 稀有")
                    addLore("点击购买")
                    enchant(Enchantment.LURE, 1)
                    flag(ItemFlag.HIDE_ENCHANTS)
                }
            }
        }
        
        slotComponent(x = 3, y = 1) {
            render {
                ItemUtil.urlSkull("dragon_texture_id", "末影龙头颅") {
                    addLore("价格: 2000金币")
                    addLore("稀有度: 传说")
                    addLore("点击购买")
                    enchant(Enchantment.LURE, 2)
                    flag(ItemFlag.HIDE_ENCHANTS)
                }
            }
        }
    }
}
```

### 成就系统头颅

```kotlin
fun createAchievementGUI(player: Player) {
    openPage(InventoryType.CHEST, 45, player) {
        title("成就系统")
        
        val achievements = getPlayerAchievements(player)
        
        achievements.forEachIndexed { index, achievement ->
            val x = index % 9
            val y = index / 9
            
            slotComponent(x, y) {
                render {
                    ItemUtil.urlSkull(achievement.iconTextureId, achievement.name) {
                        addLore("描述: ${achievement.description}")
                        addLore("进度: ${achievement.progress}/${achievement.maxProgress}")
                        addLore("奖励: ${achievement.reward}")
                        addLore("")
                        
                        if (achievement.isCompleted) {
                            addLore("§a✓ 已完成")
                            enchant(Enchantment.LURE, 1)
                            flag(ItemFlag.HIDE_ENCHANTS)
                        } else {
                            addLore("§7○ 未完成")
                        }
                        
                        // 根据稀有度设置数量
                        amount(achievement.rarity.level)
                    }
                }
                
                onLeftClick {
                    if (achievement.isCompleted && !achievement.isRewarded) {
                        giveAchievementReward(player, achievement)
                    }
                }
            }
        }
    }
}
```

## 💡 最佳实践

1. **性能优化**: 缓存常用的头颅物品，避免重复创建
2. **材质来源**: 使用可靠的材质来源，确保材质URL有效
3. **错误处理**: 为头颅创建添加异常处理，防止无效材质导致错误
4. **用户体验**: 为头颅添加有意义的描述和交互提示
5. **视觉效果**: 合理使用附魔光效和物品标志增强视觉效果

通过这些示例，你可以在GUI中创建丰富多样的头颅物品，提升用户界面的视觉效果和交互体验。
