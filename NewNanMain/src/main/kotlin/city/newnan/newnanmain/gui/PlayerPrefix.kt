package city.newnan.newnanmain.gui

import city.newnan.newnanmain.PlayerPrefixConfig
import city.newnan.newnanmain.PluginMain
import city.newnan.violet.gui.PlayerGuiSession
import dev.triumphteam.gui.builder.item.ItemBuilder
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.OfflinePlayer

fun openPlayerPrefixGui(session: PlayerGuiSession, target: OfflinePlayer) {
    val path = "player-prefix/${target.uniqueId}.yml"
    PluginMain.INSTANCE.configManager.touch(path, { PlayerPrefixConfig() })
    val config = PluginMain.INSTANCE.configManager.parse<PlayerPrefixConfig>(path)
    session.open(pageGui(session, Component.text("§7[§3§l牛腩小镇§r§7]§r 选择称号")), { _, gui, _ ->
        gui.clearPageItems()
        val namespaceToDelete = mutableListOf<String>()
        config.available.forEach { (namespace, key) ->
            val inUse = namespace == config.current
            val text = PluginMain.INSTANCE.prefixManager.getGlobalPrefix(namespace, key)
            if (text == null) {
                namespaceToDelete.add(namespace)
                return@forEach
            }
            gui.addItem(ItemBuilder.from(if (inUse) Material.LIME_WOOL else Material.RED_WOOL)
                .name(Component.text("§r$text"))
                .lore(
                    Component.text("启用: ${if (inUse) "§a是" else "§c否"}"),
                    Component.text(""),
                    Component.text("§6点击${if (inUse) "§c禁用" else "§a启用"}§6此称号"),
                ).asGuiItem {
                    config.current = if (inUse) null else namespace
                    PluginMain.INSTANCE.configManager.save(config, path)
                    if (target.player != null) PluginMain.INSTANCE.prefixManager.checkPlayer(target.player!!)
                    session.refresh()
                }
            )
        }
        if (namespaceToDelete.isNotEmpty()) {
            namespaceToDelete.forEach { config.available.remove(it) }
            PluginMain.INSTANCE.configManager.save(config, path)
        }
        true
    })
}