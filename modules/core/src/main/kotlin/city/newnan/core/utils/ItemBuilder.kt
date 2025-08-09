package city.newnan.core.utils

import city.newnan.core.utils.text.ComponentParseMode
import city.newnan.core.utils.text.ComponentProcessor
import city.newnan.core.utils.text.toComponent
import city.newnan.core.utils.text.toLegacy
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import java.net.URL
import java.util.Base64
import java.util.UUID

/**
 * 物品构建器
 *
 * 提供链式调用的方式来构建 ItemStack，简化物品创建过程。
 * 支持设置名称、描述、附魔、标志等属性。
 *
 * @author NewNanCity
 * @since 1.0.0
 */
class ItemBuilder(private var itemStack: ItemStack) {

    constructor(material: Material) : this(ItemStack(material))
    constructor(material: Material, amount: Int) : this(ItemStack(material, amount))

    /**
     * 设置物品类型
     */
    fun type(material: Material): ItemBuilder {
        itemStack.type = material
        return this
    }

    /**
     * 设置物品数量
     */
    fun amount(amount: Int): ItemBuilder {
        itemStack.amount = amount
        return this
    }

    /**
     * 设置物品耐久度
     */
    fun durability(durability: Int): ItemBuilder {
        itemStack.itemMeta = itemStack.itemMeta?.apply {
            (this as Damageable).damage = durability
        }
        return this
    }

    /**
     * 设置物品名称
     */
    fun name(name: String?, parseMode: ComponentParseMode = ComponentParseMode.Auto): ItemBuilder {
        if (ComponentProcessor.isComponentSupported) {
            return name(name?.toComponent(parseMode))
        }
        itemStack.itemMeta = itemStack.itemMeta?.apply {
            @Suppress("DEPRECATION")
            this.setDisplayName(name?.toLegacy(parseMode))
        }
        return this
    }

    /**
     * 设置物品名称（Component版本）
     */
    fun name(name: Component?): ItemBuilder {
        itemStack.itemMeta = itemStack.itemMeta?.apply {
            displayName(name)
        }
        return this
    }

    /**
     * 设置物品描述
     */
    fun lore(lore: List<String>?, parseMode: ComponentParseMode = ComponentParseMode.Auto): ItemBuilder {
        if (ComponentProcessor.isComponentSupported) {
            return lore(lore?.map { it.toComponent(parseMode) })
        }
        itemStack.itemMeta = itemStack.itemMeta?.apply {
            @Suppress("DEPRECATION")
            this.lore = lore?.map { it.toLegacy(parseMode) }
        }
        return this
    }

    /**
     * 设置物品描述（Component版本）
     */
    fun lore(loreComponents: List<Component>?): ItemBuilder {
        itemStack.itemMeta = itemStack.itemMeta?.apply {
            this.lore(loreComponents)
        }
        return this
    }

    /**
     * 设置物品描述（可变参数）
     */
    fun lore(vararg lore: String, parseMode: ComponentParseMode = ComponentParseMode.Auto): ItemBuilder =
        lore(lore.toList(), parseMode)

    /**
     * 添加附魔
     */
    fun enchant(enchantment: Enchantment, level: Int): ItemBuilder {
        itemStack.addUnsafeEnchantment(enchantment, level)
        return this
    }

    /**
     * 移除附魔
     */
    fun removeEnchant(enchantment: Enchantment): ItemBuilder {
        itemStack.removeEnchantment(enchantment)
        return this
    }

    /**
     * 清除所有附魔
     */
    fun clearEnchants(): ItemBuilder {
        itemStack.enchantments.keys.forEach { itemStack.removeEnchantment(it) }
        return this
    }

    /**
     * 添加物品标志
     */
    fun flag(vararg flags: ItemFlag): ItemBuilder {
        val meta = itemStack.itemMeta
        if (meta != null) {
            meta.addItemFlags(*flags)
            itemStack.itemMeta = meta
        }
        return this
    }

    /**
     * 移除物品标志
     */
    fun removeFlag(vararg flags: ItemFlag): ItemBuilder {
        val meta = itemStack.itemMeta
        if (meta != null) {
            meta.removeItemFlags(*flags)
            itemStack.itemMeta = meta
        }
        return this
    }

    /**
     * 设置是否无法破坏
     */
    fun unbreakable(unbreakable: Boolean): ItemBuilder {
        val meta = itemStack.itemMeta
        if (meta != null) {
            meta.isUnbreakable = unbreakable
            itemStack.itemMeta = meta
        }
        return this
    }

    /**
     * 隐藏所有标志
     */
    fun hideAll(): ItemBuilder {
        return flag(*ItemFlag.values())
    }

    /**
     * 隐藏附魔
     */
    fun hideEnchants(): ItemBuilder {
        return flag(ItemFlag.HIDE_ENCHANTS)
    }

    /**
     * 隐藏属性
     */
    fun hideAttributes(): ItemBuilder {
        return flag(ItemFlag.HIDE_ATTRIBUTES)
    }

    /**
     * 隐藏无法破坏标志
     */
    fun hideUnbreakable(): ItemBuilder {
        return flag(ItemFlag.HIDE_UNBREAKABLE)
    }

    /**
     * 隐藏药水效果
     */
    fun hidePotionEffects(): ItemBuilder {
        return flag(ItemFlag.HIDE_POTION_EFFECTS)
    }

    /**
     * 应用自定义元数据修改
     */
    fun meta(modifier: (ItemMeta) -> Unit): ItemBuilder {
        val meta = itemStack.itemMeta
        if (meta != null) {
            modifier(meta)
            itemStack.itemMeta = meta
        }
        return this
    }

    /**
     * 应用自定义修改
     */
    fun apply(modifier: (ItemStack) -> Unit): ItemBuilder {
        modifier(itemStack)
        return this
    }

    /**
     * 克隆构建器
     */
    fun clone(): ItemBuilder {
        return ItemBuilder(itemStack.clone())
    }

    /**
     * 设置头颅拥有者（使用OfflinePlayer）
     */
    fun skullOwner(player: OfflinePlayer): ItemBuilder {
        if (itemStack.type == Material.PLAYER_HEAD) {
            val meta = itemStack.itemMeta
            if (meta is SkullMeta) {
                meta.owningPlayer = player
                itemStack.itemMeta = meta
            }
        }
        return this
    }

    /**
     * 设置头颅拥有者（使用UUID）
     */
    fun skullOwner(uuid: UUID): ItemBuilder {
        return skullOwner(Bukkit.getOfflinePlayer(uuid))
    }

    /**
     * 设置头颅拥有者（使用玩家名称）
     */
    fun skullOwner(playerName: String): ItemBuilder {
        return skullOwner(Bukkit.getOfflinePlayer(playerName))
    }

    /**
     * 设置头颅材质（使用Base64数据）
     */
    fun skullTexture(base64Texture: String): ItemBuilder {
        if (itemStack.type == Material.PLAYER_HEAD) {
            val meta = itemStack.itemMeta
            if (meta is SkullMeta) {
                try {
                    val skullItem = SkullUtils.createSkullFromBase64(base64Texture)
                    val skullMeta = skullItem.itemMeta as SkullMeta
                    val profileField = ReflectionUtils.getDeclaredField(skullMeta.javaClass, "profile")
                    val profile = ReflectionUtils.safeGet<Any>(profileField, skullMeta)
                    ReflectionUtils.safeSet(profileField, meta, profile)
                    itemStack.itemMeta = meta
                } catch (e: Exception) {
                    // 如果设置失败，保持默认头颅
                }
            }
        }
        return this
    }

    /**
     * 设置头颅材质（使用URL）
     */
    fun skullTextureUrl(textureUrl: String): ItemBuilder {
        val url = if (!textureUrl.startsWith("http://") && !textureUrl.startsWith("https://")) {
            "http://textures.minecraft.net/texture/$textureUrl"
        } else {
            textureUrl
        }

        val base64Texture = Base64.getEncoder().encodeToString(
            "{\"textures\":{\"SKIN\":{\"url\":\"$url\"}}}".toByteArray()
        )

        return skullTexture(base64Texture)
    }

    /**
     * 构建物品
     */
    fun build(): ItemStack {
        return itemStack.clone()
    }

    companion object {
        /**
         * 创建物品构建器
         */
        fun of(material: Material): ItemBuilder = ItemBuilder(material)

        /**
         * 创建物品构建器
         */
        fun of(material: Material, amount: Int): ItemBuilder = ItemBuilder(material, amount)

        /**
         * 创建物品构建器
         */
        fun of(itemStack: ItemStack): ItemBuilder = ItemBuilder(itemStack.clone())

        /**
         * 快速创建简单物品
         */
        fun create(material: Material, name: String? = null, lore: List<String>? = null): ItemStack {
            return ItemBuilder(material)
                .name(name ?: "")
                .lore(lore ?: emptyList())
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
            return ItemBuilder(Material.PLAYER_HEAD)
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
            return ItemBuilder(Material.PLAYER_HEAD)
                .skullTexture(base64Texture)
                .name(name ?: "")
                .lore(lore ?: emptyList())
                .build()
        }

        /**
         * 创建URL材质头颅
         */
        fun createUrlSkull(textureUrl: String, name: String? = null, lore: List<String>? = null): ItemStack {
            return ItemBuilder(Material.PLAYER_HEAD)
                .skullTextureUrl(textureUrl)
                .name(name ?: "")
                .lore(lore ?: emptyList())
                .build()
        }
    }
}