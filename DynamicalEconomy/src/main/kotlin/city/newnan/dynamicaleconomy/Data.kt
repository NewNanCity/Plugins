package city.newnan.dynamicaleconomy

import org.bukkit.Material

object Data {
    /**
     * 价值资源矿与价值资源的对应
     */
    val valueResourceBlockItemMap = mapOf(
        Material.COAL_ORE to Material.COAL,             // 煤矿 -> 煤炭
        Material.IRON_ORE to Material.IRON_INGOT,       // 铁矿 -> 铁锭
        Material.GOLD_ORE to Material.GOLD_INGOT,       // 金矿 -> 金锭
        Material.REDSTONE_ORE to Material.REDSTONE,     // 红石矿 -> 红石
        Material.LAPIS_ORE to Material.LAPIS_LAZULI,    // 青金石矿 -> 青金石
        Material.DIAMOND_ORE to Material.DIAMOND,       // 钻石矿 -> 钻石
        Material.EMERALD_ORE to Material.EMERALD,       // 绿宝石矿 -> 绿宝石
        Material.NETHER_QUARTZ_ORE to Material.QUARTZ,  // 下界石英矿 -> 下界石英
    )

    val valueResourceItemBlockMap = valueResourceBlockItemMap.entries.associate { (k, v) -> v to k }

    /**
     * 价值资源的价值量
     */
    val valueResourceValueMap = mapOf(
        Material.COAL_ORE to 1.7,          // 煤矿石
        Material.IRON_ORE to 3.2,          // 铁矿石
        Material.GOLD_ORE to 12.5,         // 金矿石
        Material.REDSTONE_ORE to 35.0,     // 红石矿石
        Material.LAPIS_ORE to 90.0,        // 青金石矿石
        Material.DIAMOND_ORE to 100.0,     // 钻石矿石
        Material.EMERALD_ORE to 620.0,     // 绿宝石矿石
        Material.NETHER_QUARTZ_ORE to 5.2, // 下界石英矿石
    )
}