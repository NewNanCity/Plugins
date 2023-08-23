package city.newnan.foundation.gui

import city.newnan.foundation.PluginMain
import city.newnan.violet.gui.PlayerGuiSession
import city.newnan.violet.gui.UpdateType
import city.newnan.violet.item.getSkull
import city.newnan.violet.item.toSkull
import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material

fun openFoundationTopGui(session: PlayerGuiSession) {
    val list = PluginMain.INSTANCE.getTop {
        PluginMain.INSTANCE.messageManager.printf(session.player, "§f基金会数据统计中, 请稍候...")
    }
    session.open(Gui.paginated().rows(6).title(Component.text("§7[§3§l牛腩基金会§r§7]§r 慈善榜  §7(十分钟刷新一次)")).create(), { type, gui, _ ->
        // Init
        if (type == UpdateType.Init) {
            gui.setItem(6, 3, ItemBuilder.from("37aee9a75bf0df7897183015cca0b2a7d755c63388ff01752d5f4419fc645".toSkull())
                .name(Component.text("上一页")).asGuiItem { gui.previous() })
            gui.setItem(6, 7, ItemBuilder.from("682ad1b9cb4dd21259c0d75aa315ff389c3cef752be3949338164bac84a96e".toSkull())
                .name(Component.text("下一页")).asGuiItem { gui.next() })
            listOf(1,2,4,5,6,8).forEach { gui.setItem(6, it, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE)
                .name(Component.text("")).asGuiItem()) }
            gui.setItem(6, 9, ItemBuilder.from(Material.BARRIER).name(Component.text("关闭")).asGuiItem {
                session.back()
            })
            gui.setDefaultClickAction { it.isCancelled = true }
        }
        gui.clearPageItems()

        // Update
        list.forEachIndexed { index, record ->
            gui.addItem(ItemBuilder.from(record.player.getSkull())
                .name(Component.text("§6No.${index+1} §f§l${record.player.name ?: "§8未知"}"))
                .lore(
                    Component.text("§3主动: §6${record.active.toInt()}§r §7₦"),
                    Component.text("§3被动: §6${record.passive.toInt()}§r §7₦"),
                    Component.text("§3总计: §6${(record.active+record.passive).toInt()}§r §7₦"),
                    Component.text(""),
                    Component.text("感谢玩家对基金会的支持! 每一分牛币都会惠及您与其他人!"),
                    Component.text("§7您可以使用使用 §f/donate §a<金额> §7指令进行捐赠")
                ).asGuiItem {})
        }
        true
    })
}