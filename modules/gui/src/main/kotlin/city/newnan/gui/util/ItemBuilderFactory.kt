package city.newnan.gui.util

import city.newnan.gui.manager.GuiManager
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.ItemStack
import java.util.UUID

class ItemBuilderFactory(val guiManager: GuiManager) {
    /**
     * 创建物品构建器
     */
    fun of(material: Material): ItemBuilder = ItemBuilder(material, guiManager)

    /**
     * 创建物品构建器
     */
    fun of(material: Material, amount: Int): ItemBuilder = ItemBuilder(material, amount, guiManager)

    /**
     * 创建物品构建器
     */
    fun of(itemStack: ItemStack): ItemBuilder = ItemBuilder(itemStack.clone(), guiManager)

    /**
     * 快速创建简单物品
     */
    fun create(material: Material, name: String? = null, lore: List<String>? = null): ItemStack {
        return ItemBuilder(material, guiManager)
            .name(name)
            .lore(lore)
            .build()
    }

    /**
     * 快速创建简单物品（可变参数描述）
     */
    fun create(material: Material, name: String? = null, vararg lore: String): ItemStack {
        return create(material, name, lore.toList())
    }

    /**
     * 创建玩家头颅（使用OfflinePlayer）
     */
    fun createSkull(player: OfflinePlayer, name: String? = null, lore: List<String>? = null): ItemStack {
        return ItemBuilder(Material.PLAYER_HEAD, guiManager)
            .skullOwner(player)
            .name(name ?: "")
            .lore(lore ?: emptyList())
            .build()
    }

    /**
     * 创建玩家头颅（使用UUID）
     */
    fun createSkull(uuid: UUID, name: String? = null, lore: List<String>? = null): ItemStack {
        return createSkull(Bukkit.getOfflinePlayer(uuid), name, lore)
    }

    /**
     * 创建玩家头颅（使用玩家名称）
     */
    fun createSkull(playerName: String, name: String? = null, lore: List<String>? = null): ItemStack {
        return createSkull(Bukkit.getOfflinePlayer(playerName), name, lore)
    }

    /**
     * 创建自定义材质头颅（使用Base64数据）
     */
    fun createCustomSkull(base64Texture: String, name: String? = null, lore: List<String>? = null): ItemStack {
        return ItemBuilder(Material.PLAYER_HEAD, guiManager)
            .skullTexture(base64Texture)
            .name(name ?: "")
            .lore(lore ?: emptyList())
            .build()
    }

    /**
     * 创建URL材质头颅
     */
    fun createUrlSkull(textureUrlOrHash: String, name: String? = null, lore: List<String>? = null): ItemStack {
        return ItemBuilder(Material.PLAYER_HEAD, guiManager)
            .skullTextureUrl(textureUrlOrHash)
            .name(name ?: "")
            .lore(lore ?: emptyList())
            .build()
    }
}