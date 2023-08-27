package city.newnan.newnanmain.gui

import city.newnan.newnanmain.PluginMain
import city.newnan.newnanmain.input.handleTeleportInput
import city.newnan.newnanmain.input.handleYesInput
import city.newnan.violet.gui.PlayerGuiSession
import city.newnan.violet.item.toSkull
import dev.triumphteam.gui.builder.item.ItemBuilder
import me.lucko.helper.Schedulers
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material

fun openTeleportGui(session: PlayerGuiSession, editable: Boolean = false) {
    session.open(pageGui(session, Component.text("§7[§3§l牛腩小镇§r§7]§r 传送列表")), { type, gui, _ ->
        if (editable) {
            // add teleport point
            gui.setItem(6, 1, ItemBuilder.from("9a2d891c6ae9f6baa040d736ab84d48344bb6b70d7f1a280dd12cbac4d777".toSkull())
                .name(Component.text("添加")).asGuiItem {
                    handleTeleportInput(session, null) { point ->
                        if (point != null) {
                            PluginMain.INSTANCE.teleportManager.points.add(point)
                            PluginMain.INSTANCE.teleportManager.save()
                        }
                        session.show()
                    }
                })
        }
        gui.clearPageItems()
        if (!editable) {
            gui.addItem(ItemBuilder.from(Material.RED_BED).name(Component.text("§r床")).lore(Component.text("§6点击传送到此地点")).asGuiItem {
                session.player.performCommand("bed")
                session.clear()
            })
            gui.addItem(ItemBuilder.from("37410c07bfbb4145004bf918c8d6301bd97ce13270ce1f221d9aabee1afd52a3".toSkull()).name(Component.text("§r家")).lore(Component.text("§6点击传送到此地点")).asGuiItem {
                session.player.performCommand("home")
                session.clear()
            })
            gui.addItem(ItemBuilder.from(Material.GRASS_BLOCK).name(Component.text("§r资源世界")).lore(Component.text("§6点击传送到此地点")).asGuiItem {
                session.player.performCommand("resource tp")
                session.clear()
            })
            gui.addItem(ItemBuilder.from(Material.NETHERRACK).name(Component.text("§r资源下界")).lore(Component.text("§6点击传送到此地点")).asGuiItem {
                session.player.performCommand("resource tp nether")
                session.clear()
            })
        }
        PluginMain.INSTANCE.teleportManager.getTeleportPoints(session.player).forEach { point ->
            val icon = try {
                ItemBuilder.from(Material.valueOf(point.icon))
            } catch (e: Exception) {
                ItemBuilder.from(point.icon.toSkull())
            }
            icon.name(Component.text("§r${point.name}"))
            if (editable) {
                gui.addItem(icon.lore(
                    Component.text("坐标: §6${point.x}, ${point.y}, ${point.z} §r世界: §6${point.world}"),
                    Component.text("权限: §6${point.permission ?: "无"}"),
                    Component.text(""),
                    Component.text("§6左键: 传送"),
                    Component.text("§6右键: 编辑"),
                    Component.text("§6shift+右键: 删除"),
                ).asGuiItem {
                    if (it.isShiftClick) {
                        if (it.isShiftClick) {
                            handleYesInput(session, "§c你确定要删除此传送点吗? 输入 Y 确认, 输入其他任意内容取消") { yes ->
                                if (yes) {
                                    PluginMain.INSTANCE.teleportManager.points.remove(point)
                                    PluginMain.INSTANCE.teleportManager.save()
                                }
                                session.show()
                            }
                        }
                    } else {
                        if (it.isLeftClick) {
                            val world = Bukkit.getWorld(point.world) ?: run {
                                PluginMain.INSTANCE.messageManager.printf(session.player, "§c世界 §f${point.world} §c不存在!")
                                return@asGuiItem
                            }
                            Schedulers.sync().runLater({
                                session.player.teleport(Location(world, point.x.toDouble() + 0.5, point.y.toDouble() + 0.3, point.z.toDouble() + 0.5))
                                PluginMain.INSTANCE.messageManager.printf(session.player, "§a传送成功!")
                            }, 1L)
                            session.clear()
                        } else if (it.isRightClick) {
                            handleTeleportInput(session, point) { newPoint ->
                                if (newPoint != null) {
                                    val index = PluginMain.INSTANCE.teleportManager.points.indexOf(point)
                                    if (index >= 0) {
                                        PluginMain.INSTANCE.teleportManager.points[index] = newPoint
                                    } else {
                                        PluginMain.INSTANCE.teleportManager.points.add(newPoint)
                                    }
                                    PluginMain.INSTANCE.teleportManager.save()
                                }
                                session.show()
                            }
                        }
                    }
                })
            } else {
                gui.addItem(icon.lore(Component.text("§6点击传送到此地点")).asGuiItem {
                    val world = Bukkit.getWorld(point.world) ?: run {
                        PluginMain.INSTANCE.messageManager.printf(session.player, "§c世界 §f${point.world} §c不存在!")
                        return@asGuiItem
                    }
                    Schedulers.sync().runLater({
                        session.player.teleport(Location(world, point.x.toDouble() + 0.5, point.y.toDouble() + 0.3, point.z.toDouble() + 0.5))
                        PluginMain.INSTANCE.messageManager.printf(session.player, "§a传送成功!")
                    }, 1L)
                    session.clear()
                })
            }
        }
        true
    })
}