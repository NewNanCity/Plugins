package city.newnan.feefly

import city.newnan.violet.config.ConfigManager
import city.newnan.violet.message.MessageManager
import co.aikar.commands.PaperCommandManager
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.plugin.ExtendedJavaPlugin
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.GameMode
import org.bukkit.OfflinePlayer
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerGameModeChangeEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.Vector

class PluginMain : ExtendedJavaPlugin() {
    companion object {
        lateinit var INSTANCE: PluginMain
            private set
    }
    init { INSTANCE = this }

    private var flySpeed = 0f
    private var costPerCount = 0.0
    private var tickPerCount: Long = 0
    private var costPerSecond = 0.0
    private lateinit var economy: Economy
    private var targetAccount: OfflinePlayer? = null
    private val configManager: ConfigManager by lazy { ConfigManager(this) }
    internal val messageManager: MessageManager by lazy { MessageManager(this) }
    private val commandManager: PaperCommandManager by lazy { PaperCommandManager(this) }
    internal val flyingPlayers: HashMap<Player, FlyingPlayer> = HashMap()

    override fun enable() {
        if (server.pluginManager.getPlugin("Vault") == null) {
            throw Exception("Vault not found!")
        }
        economy = server.servicesManager.getRegistration(Economy::class.java)?.provider
            ?: throw Exception("Vault economy service not found!")

        reload()
        messageManager setPlayerPrefix "[牛腩小镇]"
        commandManager.registerCommand(Commands)

        // 玩家死亡则取消飞行模式
        Events.subscribe(PlayerDeathEvent::class.java)
            .handler { cancelFly(it.entity, false) }
            .bindWith(this)
        // 如果玩家切换成创造或者旁观者模式，就取消玩家的飞行
        Events.subscribe(PlayerGameModeChangeEvent::class.java)
            .filter { !listOf(GameMode.CREATIVE, GameMode.SPECTATOR).contains(it.newGameMode) }
            .handler { cancelFly(it.player, false) }
            .bindWith(this)
        // 玩家切换世界，如果新的世界没有该权限，就取消玩家的飞行
        Events.subscribe(PlayerChangedWorldEvent::class.java)
            .filter { !it.player.hasPermission("fly.self") }
            .handler { cancelFly(it.player, false) }
            .bindWith(this)
        // 玩家退出，则取消飞行
        Events.subscribe(PlayerQuitEvent::class.java)
            .handler { cancelFly(it.player, false) }
            .bindWith(this)

        Schedulers.async().runRepeating( Runnable {
            if (flyingPlayers.size > 0) {
                // 不能在遍历的时候删除元组，所以需要暂时记录
                val toDeleteFlyingPlayer = Vector<Player>()

                // 遍历飞行玩家
                flyingPlayers.forEach { (player) ->
                    if (player.hasPermission("feefly.free")) {
                        player.spigot().sendMessage(
                            ChatMessageType.ACTION_BAR, TextComponent("&6&l#飞行中# &7祝 ${player.name} 白嫖快乐"))
                        return@forEach
                    }
                    // 获取玩家现金金额
                    val balance: Double = economy.getBalance(player)
                    // 如果玩家还有现金
                    if (balance > 0.0) {
                        val remainSecond = (balance / costPerSecond).toInt()
                        val cost = balance.coerceAtMost(costPerCount)
                        economy.withdrawPlayer(player, cost)
                        targetAccount?.let { economy.depositPlayer(it, cost) }
                        player.spigot().sendMessage(
                            ChatMessageType.ACTION_BAR, TextComponent(
                                messageManager.sprintf("&6&l#飞行中# &7剩余飞行时长: {0} (余额 {1,number,#.##} ₦)",
                                    formatSecond(remainSecond), balance)
                            )
                        )

                        // 如果只能飞一分钟以内，就警告
                        if (remainSecond <= 60.0) {
                            player.sendTitle(
                                ChatColor.translateAlternateColorCodes(
                                    '&', "&c余额即将耗尽, 请尽快结束飞行!"),
                                null, 1, 7, 2)
                        }
                        player.flySpeed = flySpeed
                    } else {
                        // 如果玩家没钱了，就踢出去
                        // 不能直接从里面删除，不太好，会让迭代器受损，所以先登记，for完了再删
                        toDeleteFlyingPlayer.add(player)
                    }
                }

                // 删掉刚才需要踢除的
                toDeleteFlyingPlayer.forEach { player -> cancelFly(player, true) }
                toDeleteFlyingPlayer.clear()
            }
        }, 0, tickPerCount)
    }

    override fun disable() {
        commandManager.unregisterCommands()
    }

    internal fun reload() {
        configManager touch "config.yml"
        configManager["config.yml"]!!.also {
            // 加载配置内容
            flySpeed = it.getNode("fly-speed").float
            costPerCount = it.getNode("cost-per-count").double
            tickPerCount = it.getNode("tick-per-count").long
            costPerSecond = (20.0 / tickPerCount) * costPerCount
            targetAccount = null
            it.getNode("target-account").string?.let {
                Bukkit.getOfflinePlayers().forEach { player ->
                    if (player.name == it) {
                        targetAccount = player
                        return@let
                    }
                }
            }
        }
    }

    /**
     * 格式化时间
     * @param second 待格式化的秒数
     * @return 格式化的字符串
     */
    private fun formatSecond(second: Int): String {
        return if (second < 60) {
            "${second}秒"
        } else if (second < 3600) {
            val m = second / 60
            val s = second % 60
            "${m}分${s}秒"
        } else if (second < 86400) {
            val h = second / 3600
            val m = second % 3600 / 60
            val s = second % 3600 % 60
            "${h}小时${m}分${s}秒"
        } else {
            val d = second / 3600 / 24
            val h = second / 3600 % 24
            val m = second % 3600 / 60
            val s = second % 3600 % 60
            "${d}天${h}小时${m}分${s}秒"
        }
    }

    /**
     * 让一个玩家进入/退出付费飞行模式
     * @param player 玩家实例
     */
    internal fun toggleFly(player: Player) {
        // 检查这个玩家是否在飞行
        if (!cancelFly(player, true)) {
            // 不在飞行，就开启飞行
            // 原本在创造或者观察者模式不能进入付费飞行
            if (player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR) {
                messageManager.printf(player, "在创造或者观察者模式下, 不能进入付费飞行.")
                return
            }
            // 原本就能飞的不能进入付费飞行
            if (player.allowFlight) {
                messageManager.printf(player, "你本来就能飞, 不用付费飞行")
                return
            }
            if (player.isFlying) {
                messageManager.printf(player, "你正在飞行, 不用付费飞行")
                return
            }
            // 现金大于零才能飞
            if (economy.getBalance(player) > 0.0 || player.hasPermission("feefly.free")) {
                // 添加玩家
                flyingPlayers[player] = FlyingPlayer(System.currentTimeMillis(), player.flySpeed)
                // 如果玩家在疾跑，应当取消它，否则飞起来之后会快
                player.isSprinting = false
                // 设置飞行和速度
                player.flySpeed = flySpeed
                player.allowFlight = true
                // 发送消息并播放声音
                messageManager.printf(player, "已允许飞行, 双击空格键开启飞行模式!")
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1.0f, 0.0f)
            } else {
                // 不大于零就提示不能飞
                messageManager.printf(player, "&c余额不足, 无法飞行!")
            }
        }
    }

    /**
     * 去掉某个玩家的飞行
     * @param player 要取消的玩家
     * @param sound 是否同时播放声音
     * @return 如果玩家之前在飞行名单里，就返回true，反之
     */
    private fun cancelFly(player: Player, sound: Boolean): Boolean {
        // 不存在于列表就不取消
        if (player !in flyingPlayers) {
            return false
        }
        // 恢复玩家的状态
        player.allowFlight = false
        player.flySpeed = flyingPlayers[player]!!.previousFlyingSpeed
        if (sound) {
            player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1.0f, 0.0f)
        }
        // 删除玩家
        flyingPlayers.remove(player)
        // 发送飞行结束通知
        messageManager.printf(player, "飞行结束!")
        player.spigot().sendMessage(
            ChatMessageType.ACTION_BAR, TextComponent(
                messageManager.sprintf("飞行结束!")
            )
        )
        return true
    }
}