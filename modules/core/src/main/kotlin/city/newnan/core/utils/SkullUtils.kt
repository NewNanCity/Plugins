package city.newnan.core.utils

import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.net.URI
import java.net.URL
import java.util.*

/**
 * 头颅工具类
 *
 * 提供创建各种类型头颅的工具方法，包括：
 * - 玩家头颅
 * - 自定义材质头颅
 * - URL 材质头颅
 */
object SkullUtils {

    /**
     * 创建玩家头颅
     *
     * @param player 玩家对象
     * @param amount 数量，默认为 1
     * @return 玩家头颅物品
     */
    fun createPlayerSkull(player: OfflinePlayer, amount: Int = 1): ItemStack {
        return ItemStack(Material.PLAYER_HEAD, amount).apply {
            itemMeta = (itemMeta as SkullMeta).apply {
                owningPlayer = player
            }
        }
    }

    /**
     * 通过 UUID 创建玩家头颅
     *
     * @param uuid 玩家 UUID
     * @param amount 数量，默认为 1
     * @return 玩家头颅物品
     */
    fun createPlayerSkull(uuid: UUID, amount: Int = 1): ItemStack {
        return ItemStack(Material.PLAYER_HEAD, amount).apply {
            itemMeta = (itemMeta as SkullMeta).apply {
                val profile = GameProfile(uuid, null)
                ReflectionUtils.getDeclaredField(this.javaClass, "profile")?.set(this, profile)
            }
        }
    }

    /**
     * 通过玩家名创建玩家头颅
     *
     * @param playerName 玩家名
     * @param amount 数量，默认为 1
     * @return 玩家头颅物品
     */
    fun createPlayerSkull(playerName: String, amount: Int = 1): ItemStack {
        return ItemStack(Material.PLAYER_HEAD, amount).apply {
            itemMeta = (itemMeta as SkullMeta).apply {
                val profile = GameProfile(UUID.randomUUID(), playerName)
                ReflectionUtils.getDeclaredField(this.javaClass, "profile")?.set(this, profile)
            }
        }
    }

    /**
     * 通过材质 URL 创建头颅
     *
     * @param textureUrl 材质 URL
     * @param amount 数量，默认为 1
     * @return 自定义材质头颅物品
     */
    fun createTextureSkull(textureUrl: URL, amount: Int = 1): ItemStack {
        return ItemStack(Material.PLAYER_HEAD, amount).apply {
            itemMeta = (itemMeta as SkullMeta).apply {
                val profile = GameProfile(UUID.randomUUID(), null)
                val textureData = createTextureData(textureUrl.toString())
                profile.properties.put("textures", Property("textures", textureData))
                ReflectionUtils.getDeclaredField(this.javaClass, "profile")?.set(this, profile)
            }
        }
    }

    /**
     * 通过材质 URL 字符串创建头颅
     *
     * @param textureUrl 材质 URL 字符串
     * @param amount 数量，默认为 1
     * @return 自定义材质头颅物品
     */
    fun createTextureSkull(textureUrl: String, amount: Int = 1): ItemStack {
        val url = if (!textureUrl.startsWith("http://") && !textureUrl.startsWith("https://")) {
            "http://textures.minecraft.net/texture/$textureUrl"
        } else {
            textureUrl
        }
        return createTextureSkull(URI(url).toURL(), amount)
    }

    /**
     * 通过 Base64 编码的材质数据创建头颅
     *
     * @param textureData Base64 编码的材质数据
     * @param amount 数量，默认为 1
     * @return 自定义材质头颅物品
     */
    fun createSkullFromBase64(textureData: String, amount: Int = 1): ItemStack {
        return ItemStack(Material.PLAYER_HEAD, amount).apply {
            itemMeta = (itemMeta as SkullMeta).apply {
                val profile = GameProfile(UUID.randomUUID(), null)
                profile.properties.put("textures", Property("textures", textureData))
                ReflectionUtils.getDeclaredField(this.javaClass, "profile")?.set(this, profile)
            }
        }
    }

    /**
     * 创建材质数据的 Base64 编码
     *
     * @param textureUrl 材质 URL
     * @return Base64 编码的材质数据
     */
    fun createTextureData(textureUrl: String): String {
        val textureJson = """{"textures":{"SKIN":{"url":"$textureUrl"}}}"""
        return Base64.getEncoder().encodeToString(textureJson.toByteArray())
    }

    /**
     * 从材质数据解析 URL
     *
     * @param textureData Base64 编码的材质数据
     * @return 材质 URL，如果解析失败返回 null
     */
    fun parseTextureUrl(textureData: String): String? {
        return try {
            val decodedData = String(Base64.getDecoder().decode(textureData))
            // 简单的 JSON 解析，提取 URL
            val urlStart = decodedData.indexOf("\"url\":\"") + 7
            val urlEnd = decodedData.indexOf("\"", urlStart)
            if (urlStart > 6 && urlEnd > urlStart) {
                decodedData.substring(urlStart, urlEnd)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取头颅的材质 URL
     *
     * @param skull 头颅物品
     * @return 材质 URL，如果不是头颅或没有材质返回 null
     */
    fun getSkullTextureUrl(skull: ItemStack): String? {
        if (skull.type != Material.PLAYER_HEAD) return null

        val meta = skull.itemMeta as? SkullMeta ?: return null
        val profileField = ReflectionUtils.getDeclaredField(meta.javaClass, "profile") ?: return null
        val profile = ReflectionUtils.safeGet<GameProfile>(profileField, meta) ?: return null

        val texturesProperty = profile.properties.get("textures").firstOrNull() ?: return null
        return parseTextureUrl(texturesProperty.value)
    }

    /**
     * 检查物品是否为头颅
     *
     * @param item 物品
     * @return 是否为头颅
     */
    fun isSkull(item: ItemStack?): Boolean {
        return item?.type == Material.PLAYER_HEAD
    }

    /**
     * 检查头颅是否有自定义材质
     *
     * @param skull 头颅物品
     * @return 是否有自定义材质
     */
    fun hasCustomTexture(skull: ItemStack): Boolean {
        return getSkullTextureUrl(skull) != null
    }

    /**
     * 复制头颅的材质到另一个头颅
     *
     * @param source 源头颅
     * @param target 目标头颅
     * @return 是否复制成功
     */
    fun copyTexture(source: ItemStack, target: ItemStack): Boolean {
        if (!isSkull(source) || !isSkull(target)) return false

        val sourceMeta = source.itemMeta as SkullMeta
        val targetMeta = target.itemMeta as SkullMeta

        val profileField = ReflectionUtils.getDeclaredField(sourceMeta.javaClass, "profile") ?: return false
        val sourceProfile = ReflectionUtils.safeGet<GameProfile>(profileField, sourceMeta) ?: return false

        return ReflectionUtils.safeSet(profileField, targetMeta, sourceProfile).also {
            if (it) target.itemMeta = targetMeta
        }
    }
}

/**
 * 扩展函数：为 OfflinePlayer 添加获取头颅的方法
 */
fun OfflinePlayer.getSkull(amount: Int = 1): ItemStack =
    SkullUtils.createPlayerSkull(this, amount)

/**
 * 扩展函数：为 URL 添加转换为头颅的方法
 */
fun URL.toSkull(amount: Int = 1): ItemStack =
    SkullUtils.createTextureSkull(this, amount)

/**
 * 扩展函数：为 String 添加转换为头颅的方法
 */
fun String.toSkull(amount: Int = 1): ItemStack =
    SkullUtils.createTextureSkull(this, amount)

/**
 * 扩展函数：为 UUID 添加获取头颅的方法
 */
fun UUID.toSkull(amount: Int = 1): ItemStack =
    SkullUtils.createPlayerSkull(this, amount)
