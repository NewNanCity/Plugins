package city.newnan.logintitle.modules

import city.newnan.core.base.BaseModule
import city.newnan.core.event.subscribeEvent
import city.newnan.logintitle.LoginTitlePlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import java.time.Duration

/**
 * 登录标题模块
 *
 * 负责处理玩家登录时显示标题的逻辑
 *
 * @author AI
 * @since 1.0.0
 */
class LoginTitleModule(
    moduleName: String,
    val plugin: LoginTitlePlugin
) : BaseModule(moduleName, plugin) {

    // 标题内容 - 用户可以在这里修改标题内容
    private val titleText = "<red>这里是牛腩小镇 不是曼巴小镇</red>"
    private val subtitleText = "<yellow>不要再炸服了 违者banbanban!</yellow>"

    init { init() }

    override fun onInit() {
        // 注册玩家登录事件监听器（自动绑定到模块）
        subscribeEvent<PlayerJoinEvent> {
            priority(EventPriority.MONITOR)
            handler { event ->
                val player = event.player
                showLoginTitle(player)
            }
        }
    }

    override fun onReload() {
        // 这个简单插件不需要重载逻辑
    }

    /**
     * 显示登录标题
     */
    private fun showLoginTitle(player: org.bukkit.entity.Player) {
        try {
            // 创建标题组件
            val title = plugin.messager.sprintf(titleText)
            val subtitle = plugin.messager.sprintf(subtitleText)

            // 创建标题对象，设置持续时间为10秒
            val titleObject = Title.title(
                title,
                subtitle,
                Title.Times.times(
                    Duration.ofMillis(500),  // 淡入时间 0.5秒
                    Duration.ofSeconds(10),  // 显示时间 10秒
                    Duration.ofMillis(500)   // 淡出时间 0.5秒
                )
            )

            // 显示标题
            player.showTitle(titleObject)
        } catch (e: Exception) {
            logger.error("Failed to show login title to player ${player.name}", e)
        }
    }
}
