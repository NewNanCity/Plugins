package city.newnan.powertools.commands.user

import city.newnan.core.utils.SkullUtils
import city.newnan.core.utils.toSkull
import city.newnan.core.utils.getSkull
import city.newnan.powertools.PowerToolsPlugin
import city.newnan.powertools.i18n.LanguageKeys
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.bukkit.parser.OfflinePlayerParser
import org.incendo.cloud.type.Either
import java.util.UUID

/**
 * 头颅命令
 *
 * 提供头颅获取功能，包括：
 * - 通过URL获取头颅
 * - 通过玩家名获取头颅
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class SkullCommand(private val plugin: PowerToolsPlugin) {
    /**
     * 通过URL获取头颅
     */
    @Command("skull|head <url-or-player>")
    @Command("powertools skull|head <url-or-player>")
    @CommandDescription(LanguageKeys.Commands.Skull.URL_DESCRIPTION)
    @Permission("powertools.skull")
    fun skullUrl(
        sender: CommandSender,
        @Argument(value = "url-or-player", description = LanguageKeys.Commands.Skull.URL_OR_PLAYER_ARG)
        urlOrPlayer: Either<OfflinePlayer, String>
    ) {
        // 检查是否为玩家
        if (sender !is Player) {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.PLAYER_ONLY)
            return
        }

        if (urlOrPlayer.primary().isPresent) {
            // 检查功能是否启用
            val config = plugin.getPluginConfig()
            if (!config.skullSettings.enablePlayerSkulls) {
                plugin.messager.printf(sender, LanguageKeys.Commands.Skull.FEATURE_DISABLED)
                return
            }
            // 创建头颅
            giveSkull(sender, urlOrPlayer.primary().get().getSkull(1))
            plugin.messager.printf(sender, LanguageKeys.Commands.Skull.SUCCESS)
            return
        }

        if (urlOrPlayer.fallback().isPresent) {
            // 检查功能是否启用
            val config = plugin.getPluginConfig()
            if (!config.skullSettings.enableUrlSkulls) {
                plugin.messager.printf(sender, LanguageKeys.Commands.Skull.FEATURE_DISABLED)
                return
            }
            val url = urlOrPlayer.fallback().get()
            when {
                // base64
                url.startsWith("data:") -> {
                    giveSkull(sender, SkullUtils.createSkullFromBase64(url, 1))
                }
                // url 或 hash
                url.startsWith("http://") || url.startsWith("https://") -> {
                    giveSkull(sender, url.toSkull(1))
                }
                url.matches(Regex("[0-9a-fA-F]+")) -> {
                    giveSkull(sender, url.toSkull(1))
                }
                // uuid
                url.matches(Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) -> {
                    UUID.fromString(url).toSkull(1)
                }
                else -> {
                    plugin.messager.printf(sender, LanguageKeys.Commands.Skull.INVALID_URL)
                    return
                }
            }
            plugin.messager.printf(sender, LanguageKeys.Commands.Skull.SUCCESS)
            return
        }
    }

    private fun giveSkull(target: Player, skull: ItemStack) {
        // 检查背包空间
        val items = target.inventory.addItem(skull)
        if (items.isNotEmpty()) {
            // 扔到地上
            items.forEach{ (_, item) -> target.world.dropItem(target.location, item) }
            plugin.messager.printf(target, LanguageKeys.Commands.Skull.INVENTORY_FULL)
            return
        }
    }
}
