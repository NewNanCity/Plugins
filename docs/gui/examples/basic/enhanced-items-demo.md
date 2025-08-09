# GUI1 增强物品功能演示

本文档展示了GUI1模块新增的ItemBuilder function参数和头颅功能的实际应用示例。

## 🎮 玩家信息面板

创建一个展示玩家详细信息的GUI，使用增强的物品功能。

```kotlin
fun createPlayerInfoGUI(player: Player) {
    openPage(InventoryType.CHEST, 45, player) {
        title("玩家信息 - ${player.name}")
        
        // 玩家头颅（中央位置）
        slotComponent(x = 4, y = 1) {
            render {
                player.getSkull(1) {
                    name(Component.text("${player.name}").color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
                    addLore("§7等级: §e${player.level}")
                    addLore("§7血量: §c${player.health.toInt()}§7/§c${player.maxHealth.toInt()}")
                    addLore("§7饥饿值: §6${player.foodLevel}§7/§620")
                    addLore("§7经验: §b${player.exp}")
                    addLore("§7世界: §a${player.world.name}")
                    addLore("")
                    addLore("§e点击查看详细统计")
                    
                    // 根据玩家状态添加特效
                    if (player.isOp) {
                        enchant(Enchantment.LURE, 1)
                        flag(ItemFlag.HIDE_ENCHANTS)
                    }
                }
            }
            onLeftClick { showDetailedStats(player) }
        }
        
        // 装备展示
        slotComponent(x = 2, y = 2) {
            render {
                val helmet = player.inventory.helmet ?: ItemStack(Material.LEATHER_HELMET)
                ItemUtil.create(helmet.type, "头盔") {
                    if (helmet.hasItemMeta()) {
                        // 复制原物品的meta
                        meta { meta -> 
                            helmet.itemMeta?.let { originalMeta ->
                                meta.displayName(originalMeta.displayName())
                                meta.lore(originalMeta.lore())
                            }
                        }
                    }
                    addLore("")
                    addLore("§7当前装备的头盔")
                }
            }
        }
        
        // 统计信息按钮
        slotComponent(x = 6, y = 2) {
            render {
                ItemUtil.info(Material.BOOK, "游戏统计", listOf(
                    "游戏时间: ${getPlayTime(player)}",
                    "击杀数: ${getKills(player)}",
                    "死亡数: ${getDeaths(player)}",
                    "点击查看更多"
                )) {
                    enchant(Enchantment.LURE, 1)
                    flag(ItemFlag.HIDE_ENCHANTS)
                }
            }
            onLeftClick { showGameStats(player) }
        }
        
        // 权限信息
        slotComponent(x = 1, y = 3) {
            render {
                if (player.isOp) {
                    ItemUtil.success(Material.DIAMOND, "管理员", listOf(
                        "拥有所有权限",
                        "可以执行管理命令"
                    )) {
                        enchant(Enchantment.LURE, 2)
                        flag(ItemFlag.HIDE_ENCHANTS)
                    }
                } else {
                    ItemUtil.info(Material.IRON_INGOT, "普通玩家", listOf(
                        "基础权限",
                        "点击查看权限列表"
                    ))
                }
            }
        }
        
        // 在线状态
        slotComponent(x = 7, y = 3) {
            render {
                ItemUtil.statusIndicator(true, "在线", "离线") {
                    addLore("连接时间: ${getConnectionTime(player)}")
                    addLore("IP地址: ${player.address?.address?.hostAddress ?: "未知"}")
                    enchant(Enchantment.LURE, 1)
                    flag(ItemFlag.HIDE_ENCHANTS)
                }
            }
        }
        
        // 返回按钮
        slotComponent(x = 4, y = 4) {
            render {
                ItemUtil.backButton("返回主菜单") {
                    enchant(Enchantment.LURE, 1)
                    flag(ItemFlag.HIDE_ENCHANTS)
                }
            }
            onLeftClick { session.pop() }
        }
    }
}
```

## 🏪 装饰头颅商店

展示如何使用自定义材质头颅创建商店界面。

```kotlin
fun createSkullShopGUI(player: Player) {
    openPage(InventoryType.CHEST, 54, player) {
        title("装饰头颅商店")
        
        // 商店头颅物品
        val skullItems = listOf(
            SkullItem("creeper", "苦力怕头颅", 100, "普通"),
            SkullItem("enderman", "末影人头颅", 500, "稀有"),
            SkullItem("dragon", "末影龙头颅", 2000, "传说"),
            SkullItem("wither", "凋零头颅", 1500, "史诗"),
            SkullItem("zombie", "僵尸头颅", 50, "普通"),
            SkullItem("skeleton", "骷髅头颅", 75, "普通")
        )
        
        skullItems.forEachIndexed { index, skullItem ->
            val x = index % 9
            val y = index / 9 + 1
            
            slotComponent(x, y) {
                render {
                    ItemUtil.urlSkull(skullItem.textureId, skullItem.name) {
                        addLore("§7价格: §6${skullItem.price} 金币")
                        addLore("§7稀有度: ${getRarityColor(skullItem.rarity)}${skullItem.rarity}")
                        addLore("")
                        addLore("§e左键购买")
                        addLore("§e右键预览")
                        
                        // 根据稀有度添加特效
                        when (skullItem.rarity) {
                            "传说" -> {
                                enchant(Enchantment.LURE, 3)
                                flag(ItemFlag.HIDE_ENCHANTS)
                            }
                            "史诗" -> {
                                enchant(Enchantment.LURE, 2)
                                flag(ItemFlag.HIDE_ENCHANTS)
                            }
                            "稀有" -> {
                                enchant(Enchantment.LURE, 1)
                                flag(ItemFlag.HIDE_ENCHANTS)
                            }
                        }
                        
                        // 根据稀有度设置数量显示
                        amount(when (skullItem.rarity) {
                            "传说" -> 5
                            "史诗" -> 4
                            "稀有" -> 3
                            else -> 1
                        })
                    }
                }
                
                onLeftClick { 
                    purchaseSkull(player, skullItem)
                }
                
                onRightClick {
                    previewSkull(player, skullItem)
                }
            }
        }
        
        // 分类按钮
        slotComponent(x = 0, y = 0) {
            render {
                ItemUtil.navigation(Material.ZOMBIE_HEAD, "怪物头颅", "查看所有怪物头颅") {
                    enchant(Enchantment.LURE, 1)
                    flag(ItemFlag.HIDE_ENCHANTS)
                }
            }
            onLeftClick { showMonsterSkulls(player) }
        }
        
        slotComponent(x = 1, y = 0) {
            render {
                ItemUtil.navigation(Material.PLAYER_HEAD, "玩家头颅", "查看知名玩家头颅") {
                    enchant(Enchantment.LURE, 1)
                    flag(ItemFlag.HIDE_ENCHANTS)
                }
            }
            onLeftClick { showPlayerSkulls(player) }
        }
        
        slotComponent(x = 2, y = 0) {
            render {
                ItemUtil.navigation(Material.CARVED_PUMPKIN, "装饰头颅", "查看装饰性头颅") {
                    enchant(Enchantment.LURE, 1)
                    flag(ItemFlag.HIDE_ENCHANTS)
                }
            }
            onLeftClick { showDecorationSkulls(player) }
        }
        
        // 玩家金币显示
        slotComponent(x = 8, y = 0) {
            render {
                ItemUtil.info(Material.GOLD_INGOT, "我的金币", listOf(
                    "当前金币: ${getPlayerCoins(player)}",
                    "点击查看交易记录"
                )) {
                    amount(minOf(64, maxOf(1, getPlayerCoins(player) / 100)))
                    enchant(Enchantment.LURE, 1)
                    flag(ItemFlag.HIDE_ENCHANTS)
                }
            }
            onLeftClick { showTransactionHistory(player) }
        }
        
        // 关闭按钮
        slotComponent(x = 4, y = 5) {
            render {
                ItemUtil.closeButton("关闭商店") {
                    enchant(Enchantment.LURE, 1)
                    flag(ItemFlag.HIDE_ENCHANTS)
                }
            }
            onLeftClick { session.close() }
        }
    }
}

data class SkullItem(
    val textureId: String,
    val name: String,
    val price: Int,
    val rarity: String
)

fun getRarityColor(rarity: String): String = when (rarity) {
    "传说" -> "§6"
    "史诗" -> "§5"
    "稀有" -> "§9"
    "普通" -> "§f"
    else -> "§7"
}
```

## 🎯 成就系统界面

使用自定义头颅展示成就系统。

```kotlin
fun createAchievementGUI(player: Player) {
    openPage(InventoryType.CHEST, 45, player) {
        title("成就系统")
        
        val achievements = getPlayerAchievements(player)
        
        achievements.forEachIndexed { index, achievement ->
            val x = index % 9
            val y = index / 9 + 1
            
            slotComponent(x, y) {
                render {
                    ItemUtil.urlSkull(achievement.iconTextureId, achievement.name) {
                        addLore("§7${achievement.description}")
                        addLore("")
                        addLore("§7进度: §e${achievement.progress}§7/§e${achievement.maxProgress}")
                        
                        val percentage = (achievement.progress * 100) / achievement.maxProgress
                        addLore("§7完成度: §e$percentage%")
                        addLore("")
                        addLore("§7奖励: §6${achievement.reward}")
                        addLore("")
                        
                        if (achievement.isCompleted) {
                            addLore("§a✓ 已完成")
                            if (!achievement.isRewarded) {
                                addLore("§e点击领取奖励")
                            } else {
                                addLore("§7已领取奖励")
                            }
                            
                            // 完成的成就添加发光效果
                            enchant(Enchantment.LURE, 1)
                            flag(ItemFlag.HIDE_ENCHANTS)
                        } else {
                            addLore("§7○ 未完成")
                            addLore("§7继续努力!")
                        }
                        
                        // 根据稀有度设置数量
                        amount(achievement.rarity.level)
                    }
                }
                
                onLeftClick {
                    if (achievement.isCompleted && !achievement.isRewarded) {
                        giveAchievementReward(player, achievement)
                        refresh() // 刷新界面
                    } else {
                        showAchievementDetails(player, achievement)
                    }
                }
            }
        }
        
        // 统计信息
        slotComponent(x = 4, y = 0) {
            render {
                val completedCount = achievements.count { it.isCompleted }
                val totalCount = achievements.size
                val percentage = if (totalCount > 0) (completedCount * 100) / totalCount else 0
                
                ItemUtil.progressIndicator(completedCount, totalCount, "成就进度") {
                    addLore("已完成: $completedCount/$totalCount")
                    addLore("完成率: $percentage%")
                    addLore("")
                    addLore("§e继续探索获得更多成就!")
                    
                    if (percentage >= 100) {
                        enchant(Enchantment.LURE, 2)
                        flag(ItemFlag.HIDE_ENCHANTS)
                    }
                }
            }
        }
        
        // 返回按钮
        slotComponent(x = 0, y = 4) {
            render {
                ItemUtil.backButton("返回主菜单") {
                    enchant(Enchantment.LURE, 1)
                    flag(ItemFlag.HIDE_ENCHANTS)
                }
            }
            onLeftClick { session.pop() }
        }
    }
}
```

## 💡 实用工具函数

```kotlin
// 获取玩家在线时间
fun getOnlineTime(player: Player): String {
    val ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE)
    val minutes = ticks / 20 / 60
    val hours = minutes / 60
    return "${hours}小时${minutes % 60}分钟"
}

// 获取连接时间
fun getConnectionTime(player: Player): String {
    // 这里需要根据实际插件实现
    return "30分钟"
}

// 获取玩家金币
fun getPlayerCoins(player: Player): Int {
    // 这里需要根据经济插件实现
    return 1000
}

// 购买头颅
fun purchaseSkull(player: Player, skullItem: SkullItem) {
    val coins = getPlayerCoins(player)
    if (coins >= skullItem.price) {
        // 扣除金币并给予物品
        player.sendMessage("§a成功购买 ${skullItem.name}!")
    } else {
        player.sendMessage("§c金币不足!")
    }
}
```

这些示例展示了如何充分利用GUI1模块的新功能来创建丰富、交互性强的用户界面。通过ItemBuilder function参数和头颅功能，开发者可以创建更加精美和功能强大的GUI。
