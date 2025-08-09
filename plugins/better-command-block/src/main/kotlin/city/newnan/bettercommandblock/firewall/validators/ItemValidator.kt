package city.newnan.bettercommandblock.firewall.validators

import city.newnan.bettercommandblock.firewall.scanner.CommandScanner

/**
 * 物品验证器
 *
 * 验证命令中使用的物品是否在安全列表中。
 * 主要用于验证give、summon等命令中的物品参数，防止生成危险物品。
 *
 * 支持的功能：
 * - 白名单物品检查
 * - 命名空间支持（minecraft:、自定义命名空间）
 * - NBT数据过滤
 * - 数量限制
 *
 * @param safeItems 安全物品ID集合
 * @param maxQuantity 最大允许数量
 * @param allowCustomNamespaces 是否允许自定义命名空间
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class ItemValidator(
    private val safeItems: Set<String> = DEFAULT_SAFE_ITEMS,
    private val maxQuantity: Int = DEFAULT_MAX_QUANTITY,
    private val allowCustomNamespaces: Boolean = false
) : AbstractValidator("ItemValidator", "Validates item IDs and quantities") {
    
    companion object {
        /**
         * 默认最大数量
         */
        const val DEFAULT_MAX_QUANTITY = 64
        
        /**
         * 默认安全物品列表
         * 包含常见的安全物品，不包括危险物品如命令方块、结构方块等
         */
        val DEFAULT_SAFE_ITEMS = setOf(
            // 基础方块
            "minecraft:dirt",
            "minecraft:grass_block",
            "minecraft:stone",
            "minecraft:cobblestone",
            "minecraft:oak_log",
            "minecraft:oak_planks",
            "minecraft:sand",
            "minecraft:gravel",
            "minecraft:clay",
            
            // 建筑材料
            "minecraft:bricks",
            "minecraft:stone_bricks",
            "minecraft:oak_stairs",
            "minecraft:oak_slab",
            "minecraft:glass",
            "minecraft:wool",
            "minecraft:concrete",
            
            // 工具和武器（基础）
            "minecraft:wooden_sword",
            "minecraft:wooden_pickaxe",
            "minecraft:wooden_axe",
            "minecraft:wooden_shovel",
            "minecraft:wooden_hoe",
            "minecraft:stone_sword",
            "minecraft:stone_pickaxe",
            "minecraft:stone_axe",
            "minecraft:stone_shovel",
            "minecraft:stone_hoe",
            
            // 食物
            "minecraft:bread",
            "minecraft:apple",
            "minecraft:cooked_beef",
            "minecraft:cooked_pork",
            "minecraft:cooked_chicken",
            "minecraft:carrot",
            "minecraft:potato",
            "minecraft:wheat",
            
            // 装饰物品
            "minecraft:flower_pot",
            "minecraft:painting",
            "minecraft:item_frame",
            "minecraft:torch",
            "minecraft:lantern",
            
            // 简化版本（不带命名空间）
            "dirt", "stone", "cobblestone", "oak_log", "oak_planks",
            "sand", "gravel", "clay", "bricks", "glass", "wool",
            "bread", "apple", "torch"
        )
        
        /**
         * 危险物品列表
         * 这些物品永远不应该被允许
         */
        val DANGEROUS_ITEMS = setOf(
            "minecraft:command_block",
            "minecraft:chain_command_block",
            "minecraft:repeating_command_block",
            "minecraft:structure_block",
            "minecraft:jigsaw",
            "minecraft:barrier",
            "minecraft:bedrock",
            "minecraft:end_portal_frame",
            "minecraft:spawner",
            "minecraft:debug_stick",
            "minecraft:knowledge_book",
            
            // 简化版本
            "command_block", "chain_command_block", "repeating_command_block",
            "structure_block", "jigsaw", "barrier", "bedrock", "spawner"
        )
    }
    
    override fun doValidate(scanner: CommandScanner): Boolean {
        // 获取下一个token作为物品ID
        val itemToken = scanner.nextToken() ?: return false
        
        // 检查是否为危险物品
        if (isDangerousItem(itemToken)) {
            return false
        }
        
        // 解析物品ID和NBT数据
        val (itemId, nbtData) = parseItemToken(itemToken)
        
        // 验证物品ID
        if (!isItemSafe(itemId)) {
            return false
        }
        
        // 验证NBT数据（如果存在）
        if (nbtData != null && !isNbtSafe(nbtData)) {
            return false
        }
        
        // 检查数量参数（如果存在）
        val quantityToken = scanner.nextToken() ?: return true
        return isQuantitySafe(quantityToken)
    }
    
    /**
     * 检查物品是否为危险物品
     *
     * @param itemToken 物品token
     * @return 如果是危险物品则返回true
     */
    private fun isDangerousItem(itemToken: String): Boolean {
        val (itemId, _) = parseItemToken(itemToken)
        return DANGEROUS_ITEMS.contains(itemId.lowercase())
    }
    
    /**
     * 检查物品是否安全
     *
     * @param itemId 物品ID
     * @return 如果物品安全则返回true
     */
    private fun isItemSafe(itemId: String): Boolean {
        val normalizedId = itemId.lowercase()
        
        // 检查是否在安全列表中
        if (safeItems.contains(normalizedId)) {
            return true
        }
        
        // 检查命名空间
        if (normalizedId.contains(":")) {
            val namespace = normalizedId.substringBefore(":")
            val item = normalizedId.substringAfter(":")
            
            // minecraft命名空间总是被检查
            if (namespace == "minecraft") {
                return safeItems.contains(item) || safeItems.contains(normalizedId)
            }
            
            // 自定义命名空间的处理
            if (!allowCustomNamespaces) {
                return false
            }
            
            // 如果允许自定义命名空间，检查物品名是否在安全列表中
            return safeItems.contains(item)
        }
        
        return false
    }
    
    /**
     * 检查NBT数据是否安全
     *
     * @param nbtData NBT数据字符串
     * @return 如果NBT数据安全则返回true
     */
    private fun isNbtSafe(nbtData: String): Boolean {
        // 基础的NBT安全检查
        val dangerousNbtKeys = setOf(
            "command", "Command",
            "CustomName", "display",
            "ench", "Enchantments",
            "AttributeModifiers",
            "CanDestroy", "CanPlaceOn"
        )
        
        // 简单的字符串检查，防止包含危险的NBT键
        for (dangerousKey in dangerousNbtKeys) {
            if (nbtData.contains(dangerousKey, ignoreCase = true)) {
                return false
            }
        }
        
        // 检查是否包含命令相关的内容
        val dangerousCommands = setOf("give", "summon", "setblock", "fill", "execute")
        for (command in dangerousCommands) {
            if (nbtData.contains(command, ignoreCase = true)) {
                return false
            }
        }
        
        return true
    }
    
    /**
     * 检查数量是否安全
     *
     * @param quantityToken 数量token
     * @return 如果数量安全则返回true
     */
    private fun isQuantitySafe(quantityToken: String): Boolean {
        return isSafeNumber(quantityToken, 1, maxQuantity)
    }
    
    /**
     * 解析物品token，分离物品ID和NBT数据
     *
     * @param itemToken 物品token
     * @return Pair<物品ID, NBT数据>，NBT数据可能为null
     */
    private fun parseItemToken(itemToken: String): Pair<String, String?> {
        // 查找NBT数据的开始位置
        val nbtStart = itemToken.indexOf('{')
        
        return if (nbtStart != -1) {
            val itemId = itemToken.substring(0, nbtStart)
            val nbtData = itemToken.substring(nbtStart)
            Pair(itemId, nbtData)
        } else {
            Pair(itemToken, null)
        }
    }
    
    /**
     * 获取安全物品列表
     *
     * @return 安全物品ID集合
     */
    fun getSafeItems(): Set<String> = safeItems.toSet()
    
    /**
     * 获取最大允许数量
     *
     * @return 最大数量
     */
    fun getMaxQuantity(): Int = maxQuantity
    
    /**
     * 检查是否允许自定义命名空间
     *
     * @return 如果允许则返回true
     */
    fun isCustomNamespacesAllowed(): Boolean = allowCustomNamespaces
    
    override fun getDescription(): String {
        return "Item validator with ${safeItems.size} safe items, max quantity: $maxQuantity, custom namespaces: $allowCustomNamespaces"
    }
    
    override fun toString(): String {
        return "ItemValidator(safeItems=${safeItems.size}, maxQuantity=$maxQuantity, customNamespaces=$allowCustomNamespaces)"
    }
}
