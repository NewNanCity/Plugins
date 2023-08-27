package city.newnan.newnanmain.gui

import city.newnan.newnanmain.PluginMain
import city.newnan.newnanmain.input.handlePrefixInput
import city.newnan.newnanmain.input.handleYesInput
import city.newnan.violet.gui.PlayerGuiSession
import city.newnan.violet.gui.UpdateType
import city.newnan.violet.item.toSkull
import dev.triumphteam.gui.builder.item.ItemBuilder
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

fun openGlobalPrefixsGui(session: PlayerGuiSession, namespace: String) {
    session.open(pageGui(session, Component.text("§7[§3§l牛腩小镇§r§7]§r 类别: $namespace")), { type, gui, _ ->
        if (type == UpdateType.Init) {
            gui.setItem(6, 1, ItemBuilder.from("9a2d891c6ae9f6baa040d736ab84d48344bb6b70d7f1a280dd12cbac4d777".toSkull())
                .name(Component.text("添加")).asGuiItem {
                    handlePrefixInput(session, null) { prefix ->
                        if (prefix != null) {
                            PluginMain.INSTANCE.prefixManager.setGlobalPrefix(namespace, prefix.first, prefix.second)
                        }
                        session.show()
                    }
                })
        }
        gui.clearPageItems()
        PluginMain.INSTANCE.prefixManager.globalPrefix[namespace]?.forEach { (key, value) ->
            val item = ItemStack(Material.NAME_TAG)
            item.itemMeta = item.itemMeta?.also {
                it.setDisplayName("§f名称: §r$key")
                it.lore = listOf(
                    "§7内容: §r$value",
                    "",
                    "§6左键: 修改",
                    "§6右键: 删除",
                )
            }
            gui.addItem(
                ItemBuilder.from(item)
                    .asGuiItem {
                        if (it.isShiftClick) return@asGuiItem
                        if (it.isLeftClick) {
                            handlePrefixInput(session, key to value) { prefix ->
                                if (prefix != null) {
                                    PluginMain.INSTANCE.prefixManager.removeGlobalPrefix(namespace, key, false)
                                    PluginMain.INSTANCE.prefixManager.setGlobalPrefix(namespace, prefix.first, prefix.second)
                                }
                                session.show()
                            }
                        } else {
                            handleYesInput(session, "§c确认删除称号? 所有拥有此称号的玩家将失去此称号! 输入Y确认, 输入其他则取消") { yes ->
                                if (yes) PluginMain.INSTANCE.prefixManager.removeGlobalPrefix(namespace, key)
                                session.show()
                            }
                        }
                    }
            )
        }
        true
    })
}