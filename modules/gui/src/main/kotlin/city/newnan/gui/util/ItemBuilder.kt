package city.newnan.gui.util

import city.newnan.core.utils.ReflectionUtils
import city.newnan.core.utils.SkullUtils
import city.newnan.core.utils.text.ComponentParseMode
import city.newnan.core.utils.text.ComponentProcessor
import city.newnan.gui.manager.GuiManager
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
class ItemBuilder(private var itemStack: ItemStack, private val guiManager: GuiManager) {

    constructor(material: Material, guiManager: GuiManager) : this(ItemStack(material), guiManager)
    constructor(material: Material, amount: Int, guiManager: GuiManager) : this(ItemStack(material, amount), guiManager)

    /**
     * 格式化文本，包括语言映射、参数替换和自动格式解析
     *
     * @param text 待格式化的文本
     * @param parseMode 解析模式
     * @param args 格式化参数
     * @return 格式化后的组件
     */
    fun format(text: String, vararg args: Any, parseMode: ComponentParseMode = ComponentParseMode.Auto): Component =
        guiManager.format(text, *args, parseMode = parseMode)

    /**
     * 格式化文本，包括语言映射、参数替换和自动格式解析
     *
     * @param text 待格式化的文本
     * @param parseMode 解析模式
     * @param args 格式化参数
     * @return 格式化后的Legacy字符串
     */
    fun formatLegacy(text: String, vararg args: Any, parseMode: ComponentParseMode = ComponentParseMode.Auto): String =
        guiManager.formatLegacy(text, *args, parseMode = parseMode)

    /**
     * 格式化文本，包括语言映射和参数替换，不包含自动格式解析
     *
     * @param text 待格式化的文本
     * @param args 格式化参数
     * @return 格式化后的字符串
     */
    fun formatPlain(text: String, vararg args: Any): String =
        guiManager.formatPlain(text, *args)


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
            return name(name?.run { format(this, parseMode = parseMode) })
        }
        itemStack.itemMeta = itemStack.itemMeta?.apply {
            @Suppress("DEPRECATION")
            this.setDisplayName(name?.run { formatLegacy(this, parseMode = parseMode) })
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
            return lore(lore?.map { format(it, parseMode = parseMode) })
        }
        itemStack.itemMeta = itemStack.itemMeta?.apply {
            @Suppress("DEPRECATION")
            this.lore = lore?.map { formatLegacy(it, parseMode = parseMode) }
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
    fun lore(lore: String, parseMode: ComponentParseMode = ComponentParseMode.Auto): ItemBuilder =
        lore(formatPlain(lore).split('\n'), parseMode)

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
     * 应用自定义元数据修改
     */
    @JvmName("metaWithCast")
    fun <T : ItemMeta> meta(modifier: (T) -> Unit): ItemBuilder {
        @Suppress("UNCHECKED_CAST")
        val meta = itemStack.itemMeta as? T
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
        return ItemBuilder(itemStack.clone(), guiManager)
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
    fun skullTextureUrl(textureUrlOrHash: String): ItemBuilder {
        val url = if (!textureUrlOrHash.startsWith("http://") && !textureUrlOrHash.startsWith("https://")) {
            "http://textures.minecraft.net/texture/$textureUrlOrHash"
        } else {
            textureUrlOrHash
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
}