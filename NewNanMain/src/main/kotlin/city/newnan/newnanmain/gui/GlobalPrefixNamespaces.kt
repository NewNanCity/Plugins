package city.newnan.newnanmain.gui

import city.newnan.newnanmain.PluginMain
import city.newnan.newnanmain.input.handleNamespaceInput
import city.newnan.newnanmain.input.handleYesInput
import city.newnan.violet.gui.PlayerGuiSession
import city.newnan.violet.gui.UpdateType
import city.newnan.violet.item.toSkull
import dev.triumphteam.gui.builder.item.ItemBuilder
import net.kyori.adventure.text.Component
import org.bukkit.Material

fun openGlobalPrefixNamespacesGui(session: PlayerGuiSession) {
    session.open(pageGui(session, Component.text("§7[§3§l牛腩小镇§r§7]§r 所有头衔类别")), { type, gui, _ ->
        if (type == UpdateType.Init) {
            gui.setItem(6, 1, ItemBuilder.from("9a2d891c6ae9f6baa040d736ab84d48344bb6b70d7f1a280dd12cbac4d777".toSkull())
                .name(Component.text("添加")).asGuiItem {
                    handleNamespaceInput(session, null) { namespace ->
                        if (namespace != null) {
                            PluginMain.INSTANCE.prefixManager.globalPrefix.getOrPut(namespace) { mutableMapOf() }
                        }
                        session.show()
                    }
                })
        }
        gui.clearPageItems()
        PluginMain.INSTANCE.prefixManager.globalPrefix.forEach { (namespace, keys) ->
            gui.addItem(
                ItemBuilder.from(Material.CHEST)
                    .name(Component.text("§r$namespace"))
                    .lore(
                        Component.text("共计: §a${keys.size}§r 个头衔"),
                        Component.text(""),
                        Component.text("§6左键: 查看此类别的所有头衔"),
                        Component.text("§6shift+右键: 删除该类别"),
                    ).asGuiItem {
                        if (it.isLeftClick && !it.isShiftClick) {
                            openGlobalPrefixsGui(session, namespace)
                        }
                        if (it.isRightClick && it.isShiftClick) {
                            handleYesInput(session, "§c确定要删除此类别吗? 所有属于此类别的头衔都将被删除! 玩家与之相关的称号将消失!") {
                                PluginMain.INSTANCE.prefixManager.globalPrefix.remove(namespace)
                                session.show()
                            }
                        }
                    }
            )
        }
        true
    })
}