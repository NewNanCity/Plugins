package city.newnan.core.utils

import city.newnan.core.utils.text.toComponent
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*

/**
 * 玩家工具类
 *
 * 提供玩家相关的实用方法，包括物品管理、状态检查、传送等功能。
 *
 * @author NewNanCity
 * @since 1.0.0
 */
object PlayerUtils {

    /**
     * 检查玩家是否在线
     */
    fun isOnline(uuid: UUID): Boolean {
        return Bukkit.getPlayer(uuid) != null
    }

    /**
     * 检查玩家是否在线
     */
    fun isOnline(name: String): Boolean {
        return Bukkit.getPlayer(name) != null
    }

    /**
     * 获取在线玩家
     */
    fun getOnlinePlayer(uuid: UUID): Player? {
        return Bukkit.getPlayer(uuid)
    }

    /**
     * 获取在线玩家
     */
    fun getOnlinePlayer(name: String): Player? {
        return Bukkit.getPlayer(name)
    }

    /**
     * 获取所有在线玩家
     */
    fun getOnlinePlayers(): Collection<Player> {
        return Bukkit.getOnlinePlayers()
    }

    /**
     * 检查玩家背包是否已满
     */
    fun isInventoryFull(player: Player): Boolean {
        return player.inventory.firstEmpty() == -1
    }

    /**
     * 检查玩家背包是否有空间
     */
    fun hasInventorySpace(player: Player, amount: Int = 1): Boolean {
        var emptySlots = 0
        for (item in player.inventory.contents) {
            if (item == null || item.type == Material.AIR) {
                emptySlots++
                if (emptySlots >= amount) return true
            }
        }
        return false
    }

    /**
     * 给玩家物品（如果背包满了则掉落）
     */
    fun giveItem(player: Player, item: ItemStack): Boolean {
        val remaining = player.inventory.addItem(item)
        if (remaining.isNotEmpty()) {
            // 背包满了，掉落物品
            for (drop in remaining.values) {
                player.world.dropItemNaturally(player.location, drop)
            }
            return false
        }
        return true
    }

    /**
     * 给玩家多个物品
     */
    fun giveItems(player: Player, vararg items: ItemStack): Boolean {
        var allGiven = true
        for (item in items) {
            if (!giveItem(player, item)) {
                allGiven = false
            }
        }
        return allGiven
    }

    /**
     * 移除玩家物品
     */
    fun removeItem(player: Player, item: ItemStack): Boolean {
        return player.inventory.removeItem(item).isEmpty()
    }

    /**
     * 移除玩家指定数量的物品
     */
    fun removeItem(player: Player, material: Material, amount: Int): Int {
        var remaining = amount
        val inventory = player.inventory

        for (i in 0 until inventory.size) {
            val item = inventory.getItem(i)
            if (item != null && item.type == material) {
                val itemAmount = item.amount
                if (itemAmount <= remaining) {
                    inventory.setItem(i, null)
                    remaining -= itemAmount
                } else {
                    item.amount = itemAmount - remaining
                    remaining = 0
                }

                if (remaining <= 0) break
            }
        }

        return amount - remaining
    }

    /**
     * 检查玩家是否有指定物品
     */
    fun hasItem(player: Player, material: Material, amount: Int = 1): Boolean {
        var count = 0
        for (item in player.inventory.contents) {
            if (item != null && item.type == material) {
                count += item.amount
                if (count >= amount) return true
            }
        }
        return false
    }

    /**
     * 计算玩家拥有的指定物品数量
     */
    fun countItem(player: Player, material: Material): Int {
        var count = 0
        for (item in player.inventory.contents) {
            if (item != null && item.type == material) {
                count += item.amount
            }
        }
        return count
    }

    /**
     * 清空玩家背包
     */
    fun clearInventory(player: Player) {
        player.inventory.clear()
    }

    /**
     * 清空玩家盔甲
     */
    fun clearArmor(player: Player) {
        player.inventory.armorContents = arrayOfNulls(4)
    }

    /**
     * 治愈玩家
     */
    fun heal(player: Player) {
        player.health = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0
        player.foodLevel = 20
        player.saturation = 20f
        player.exhaustion = 0f
        clearPotionEffects(player)
        player.fireTicks = 0
    }

    /**
     * 清除玩家所有药水效果
     */
    fun clearPotionEffects(player: Player) {
        for (effect in player.activePotionEffects) {
            player.removePotionEffect(effect.type)
        }
    }

    /**
     * 给玩家添加药水效果
     */
    fun addPotionEffect(player: Player, type: PotionEffectType, duration: Int, amplifier: Int = 0) {
        player.addPotionEffect(PotionEffect(type, duration, amplifier))
    }

    /**
     * 检查玩家是否有指定权限
     */
    fun hasPermission(player: Player, permission: String): Boolean {
        return player.hasPermission(permission)
    }

    /**
     * 检查玩家是否是OP
     */
    fun isOp(player: Player): Boolean {
        return player.isOp
    }

    /**
     * 安全传送玩家
     */
    fun teleportSafely(player: Player, location: Location): Boolean {
        val safeLocation = LocationUtils.findSafeLocation(location)
        return if (safeLocation != null) {
            player.teleport(safeLocation)
            true
        } else {
            false
        }
    }

    /**
     * 检查玩家是否在创造模式
     */
    fun isCreative(player: Player): Boolean {
        return player.gameMode == GameMode.CREATIVE
    }

    /**
     * 检查玩家是否在生存模式
     */
    fun isSurvival(player: Player): Boolean {
        return player.gameMode == GameMode.SURVIVAL
    }

    /**
     * 检查玩家是否在冒险模式
     */
    fun isAdventure(player: Player): Boolean {
        return player.gameMode == GameMode.ADVENTURE
    }

    /**
     * 检查玩家是否在观察者模式
     */
    fun isSpectator(player: Player): Boolean {
        return player.gameMode == GameMode.SPECTATOR
    }

    /**
     * 检查玩家是否在飞行
     */
    fun isFlying(player: Player): Boolean {
        return player.isFlying
    }

    /**
     * 检查玩家是否可以飞行
     */
    fun canFly(player: Player): Boolean {
        return player.allowFlight
    }

    /**
     * 设置玩家飞行状态
     */
    fun setFlying(player: Player, flying: Boolean, allowFlight: Boolean = flying) {
        player.allowFlight = allowFlight
        player.isFlying = flying
    }

    /**
     * 重置玩家状态
     */
    fun reset(player: Player) {
        heal(player)
        clearInventory(player)
        clearArmor(player)
        player.gameMode = GameMode.SURVIVAL
        player.allowFlight = false
        player.isFlying = false
        player.walkSpeed = 0.2f
        player.flySpeed = 0.1f
        player.exp = 0f
        player.level = 0
        player.totalExperience = 0
    }

    /**
     * 获取玩家显示名称
     */
    fun getDisplayName(player: Player): String {
        return player.displayName()?.let {
            net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(it)
        } ?: player.name
    }

    /**
     * 发送消息给玩家
     */
    fun sendMessage(player: Player, message: String) {
        player.sendMessage(message)
    }

    /**
     * 发送多行消息给玩家
     */
    fun sendMessages(player: Player, vararg messages: String) {
        for (message in messages) {
            player.sendMessage(message)
        }
    }

    /**
     * 发送标题给玩家（使用现代化API）
     */
    fun sendTitle(player: Player, title: String?, subtitle: String?, fadeIn: Int = 10, stay: Int = 70, fadeOut: Int = 20) {
        try {
            // 使用 Adventure API（Paper 1.16.5+）
            val titleComponent = title?.let { net.kyori.adventure.text.Component.text(it) }
            val subtitleComponent = subtitle?.let { net.kyori.adventure.text.Component.text(it) }

            val titleObj = net.kyori.adventure.title.Title.title(
                titleComponent ?: net.kyori.adventure.text.Component.empty(),
                subtitleComponent ?: net.kyori.adventure.text.Component.empty(),
                net.kyori.adventure.title.Title.Times.times(
                    java.time.Duration.ofMillis(fadeIn * 50L),
                    java.time.Duration.ofMillis(stay * 50L),
                    java.time.Duration.ofMillis(fadeOut * 50L)
                )
            )
            player.showTitle(titleObj)
        } catch (e: Exception) {
            // 降级到传统API
            @Suppress("DEPRECATION")
            player.sendTitle(title, subtitle, fadeIn, stay, fadeOut)
        }
    }

    /**
     * 发送动作栏消息给玩家（使用现代化API）
     */
    fun sendActionBar(player: Player, message: String) {
        try {
            // 使用 Adventure API（Paper 1.16.5+）
            player.sendActionBar(message.toComponent())
        } catch (e: Exception) {
            // 降级到传统API
            try {
                @Suppress("DEPRECATION")
                player.sendActionBar(message)
            } catch (ex: Exception) {
                // 最后降级：发送普通消息
                player.sendMessage(message)
            }
        }
    }

    /**
     * 播放声音给玩家
     */
    fun playSound(player: Player, sound: org.bukkit.Sound, volume: Float = 1.0f, pitch: Float = 1.0f) {
        player.playSound(player.location, sound, volume, pitch)
    }

    /**
     * 检查两个玩家是否在同一世界
     */
    fun inSameWorld(player1: Player, player2: Player): Boolean {
        return player1.world.name == player2.world.name
    }

    /**
     * 获取玩家之间的距离
     */
    fun getDistance(player1: Player, player2: Player): Double {
        return LocationUtils.distance(player1.location, player2.location)
    }

    /**
     * 检查玩家是否在指定范围内
     */
    fun isWithinRange(player1: Player, player2: Player, range: Double): Boolean {
        return getDistance(player1, player2) <= range
    }
}

/**
 * Player 扩展函数
 */
fun Player.giveItem(item: ItemStack): Boolean = PlayerUtils.giveItem(this, item)
fun Player.hasItem(material: Material, amount: Int = 1): Boolean = PlayerUtils.hasItem(this, material, amount)
fun Player.countItem(material: Material): Int = PlayerUtils.countItem(this, material)
fun Player.removeItem(material: Material, amount: Int): Int = PlayerUtils.removeItem(this, material, amount)
fun Player.heal(): Unit = PlayerUtils.heal(this)
fun Player.reset(): Unit = PlayerUtils.reset(this)
fun Player.teleportSafely(location: Location): Boolean = PlayerUtils.teleportSafely(this, location)
fun Player.isInventoryFull(): Boolean = PlayerUtils.isInventoryFull(this)
fun Player.hasInventorySpace(amount: Int = 1): Boolean = PlayerUtils.hasInventorySpace(this, amount)
