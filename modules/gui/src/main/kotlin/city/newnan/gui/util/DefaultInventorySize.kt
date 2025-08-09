package city.newnan.gui.util

import org.bukkit.event.inventory.InventoryType

/**
 * 获取指定 InventoryType 的默认 slot 数
 *
 * 基于 Bukkit 官方文档和 Minecraft Wiki 整理
 * 文档来源：https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/event/inventory/InventoryType.html
 *
 * 注意：
 * - CHEST 的大小取决于具体的箱子类型（小箱子27个槽，大箱子54个槽）
 * - PLAYER 包含多个区域的总计（9个快捷栏 + 27个背包 + 4个护甲 + 1个副手）
 * - 某些类型如 COMPOSTER, JUKEBOX 等是伪装库存，实际使用中可能有差异
 *
 * @param inventoryType InventoryType 枚举值
 * @return 默认 slot 数，如果类型未知则返回 0
 */
fun getDefaultInventorySize(inventoryType: InventoryType): Int {
    return when (inventoryType) {
        // === 存储容器类型 ===
        InventoryType.CHEST -> 27           // 普通箱子（大箱子为54）
        InventoryType.DISPENSER -> 9        // 发射器
        InventoryType.DROPPER -> 9          // 投掷器
        InventoryType.HOPPER -> 5           // 漏斗
        InventoryType.SHULKER_BOX -> 27     // 潜影盒
        InventoryType.BARREL -> 27          // 木桶
        InventoryType.ENDER_CHEST -> 27     // 末影箱

        // === 工作台类型 ===
        InventoryType.WORKBENCH -> 10       // 工作台（9个合成槽 + 1个结果槽）
        InventoryType.CRAFTING -> 5         // 玩家合成（4个合成槽 + 1个结果槽）
        // InventoryType.CRAFTER -> 9          // 合成器（1.21+实验性功能）

        // === 熔炉类型 ===
        InventoryType.FURNACE -> 3          // 熔炉（1个输入 + 1个燃料 + 1个输出）
        InventoryType.BLAST_FURNACE -> 3    // 高炉
        InventoryType.SMOKER -> 3           // 烟熏炉

        // === 酿造与附魔类型 ===
        InventoryType.BREWING -> 5          // 酿造台（1个燃料 + 3个药水 + 1个材料）
        InventoryType.ENCHANTING -> 2       // 附魔台（1个物品 + 1个青金石）

        // === 铁砧与升级类型 ===
        InventoryType.ANVIL -> 3            // 铁砧（2个输入 + 1个输出）
        InventoryType.SMITHING -> 4         // 锻造台（3个输入 + 1个输出）
        InventoryType.GRINDSTONE -> 3       // 砂轮（2个输入 + 1个输出）

        // === 工具台类型 ===
        InventoryType.CARTOGRAPHY -> 3      // 制图台（2个输入 + 1个输出）
        InventoryType.LOOM -> 4             // 织布机（3个输入 + 1个输出）
        InventoryType.STONECUTTER -> 2      // 切石机（1个输入 + 1个输出）

        // === 特殊功能类型 ===
        InventoryType.BEACON -> 1           // 信标（1个支付物品槽）
        InventoryType.LECTERN -> 1          // 讲台（1个书槽）

        // === 玩家相关类型 ===
        InventoryType.PLAYER -> 41          // 玩家背包（9快捷栏 + 27背包 + 4护甲 + 1副手）
        InventoryType.CREATIVE -> 9         // 创造模式（仅9个快捷栏槽）

        // === 交易类型 ===
        InventoryType.MERCHANT -> 3         // 村民交易（2个输入 + 1个输出）

        // === 伪装库存类型（实际使用中可能有差异）===
        InventoryType.COMPOSTER -> 1        // 堆肥桶（0或1个槽，动态变化）
        InventoryType.CHISELED_BOOKSHELF -> 6 // 雕纹书架（6个书槽）
        InventoryType.JUKEBOX -> 1          // 唱片机（1个唱片槽）
        // InventoryType.DECORATED_POT -> 1    // 装饰陶罐（1个物品槽）

        // === 未知类型 ===
        else -> 0                           // 未知类型返回0
    }
}