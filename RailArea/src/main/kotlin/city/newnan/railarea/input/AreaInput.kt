package city.newnan.railarea.input

import city.newnan.railarea.PluginMain
import city.newnan.railarea.config.Direction
import city.newnan.railarea.config.RailArea
import city.newnan.railarea.config.RailLine
import city.newnan.railarea.config.Station
import city.newnan.railarea.gui.showLineStationGui
import city.newnan.railarea.gui.showReverseGui
import city.newnan.railarea.octree.Point3D
import city.newnan.railarea.octree.Range3D
import city.newnan.railarea.utils.*
import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import me.lucko.helper.Schedulers
import net.kyori.adventure.text.Component
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.entity.Player

val emptyRange3D = Range3D(0, 0, 0, 0, 0, 0)
val emptyPoint3D = Point3D(0, 0, 0)

fun handleAreaInput (player: Player, oldArea: RailArea?, iStation: Station? = null, iLine: RailLine? = null, iReverse: Boolean? = null,
                     done: (area: RailArea?) -> Unit) {
    if (inputLocks.contains(player.uniqueId)) {
        PluginMain.INSTANCE.messageManager.printf(player, "§c你正在进行其他输入, 请先取消之!")
        done(null)
        return
    }
    var station: Station? = oldArea?.station ?: iStation
    var railLine: RailLine? = oldArea?.line ?: iLine
    var area: Range3D? = oldArea?.range3D
    var world: World = oldArea?.world ?: player.world
    var stopPoint: Point3D? = oldArea?.stopPoint
    var direction: Direction? = oldArea?.direction
    var reverse: Boolean? = oldArea?.reverse ?: iReverse
    if (station == PluginMain.INSTANCE.unknownStation) station = null
    if (railLine == PluginMain.INSTANCE.unknownLine) railLine = null
    fun preview(p: String?) {
        if (station != null && reverse != null && railLine != null) {
            player.sendTitle(RailArea(world, emptyRange3D, Direction.NORTH, emptyPoint3D, station!!, railLine!!, reverse!!),
                RailTitleMode.UNDER_BOARD, 1, 70, 2)
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent("列车已到站停靠，您可下车，或留在车上待自动发车"))
        }
        area?.visualize(world, Particle.FLAME, 10)
        stopPoint?.visualize(world, Particle.BARRIER, 10)
        if (p == null) {
            PluginMain.INSTANCE.messageManager.printf(player, "效果预览已显示")
        } else {
            PluginMain.INSTANCE.messageManager.printf(player, "§2$p&r 已设定, 效果预览已显示，请继续设置其他属性!")
        }
    }
    inputLocks.add(player.uniqueId)
    PluginMain.INSTANCE.messageManager.gets(player) { input ->
        val argv = input.split(" ").filter { it.isNotEmpty() }
        when (argv[0].lowercase()) {
            "station" -> {
                showLineStationGui(player, false) { l, s, back ->
                    showReverseGui(player, l) {
                        if (it == null) {
                            back()
                            return@showReverseGui
                        }
                        reverse = it
                        railLine = l
                        station = s
                        preview("站点")
                    }
                }
            }
            "area" -> {
                if (argv.size < 2) {
                    val areaw = player.getSelection()
                    if (areaw == null) {
                        PluginMain.INSTANCE.messageManager.printf(player, "§c请先用小木斧选择一个区域!")
                    } else {
                        if (areaw.world != player.world) {
                            PluginMain.INSTANCE.messageManager.printf(player, "§c请先切换到选择区域所在的世界!")
                            return@gets false
                        }
                        if (areaw.range.maxX == areaw.range.minX || areaw.range.maxY == areaw.range.minY || areaw.range.maxZ == areaw.range.minZ) {
                            PluginMain.INSTANCE.messageManager.printf(player, "§c是不是不小心把area打成stop了? 选择区域大一点吧!")
                            return@gets false
                        }
                        if (world != areaw.world) {
                            stopPoint = null
                            direction = null
                        }
                        world = areaw.world
                        area = areaw.range
                        area?.also { a ->
                            PluginMain.INSTANCE.messageManager.printf(player,
                                "根据你小木斧选择的区域，已设定区域范围为: ${world.name} §2(${
                                    a.minX}, ${a.minY}, ${a.minZ}) - (${a.maxX}, ${a.maxY}, ${a.maxZ})")
                            preview(null)
                        }
                    }
                } else {
                    if (argv.size != 8) {
                        PluginMain.INSTANCE.messageManager.printf(player, "§c区域范围格式错误! 请使用 world x1 y1 z1 x2 y2 z2 的格式!")
                    } else {
                        try {
                            val worldt = Bukkit.getWorld(argv[1])
                            if (worldt == null) {
                                PluginMain.INSTANCE.messageManager.printf(player, "§c世界 ${argv[1]} 不存在!")
                            } else {
                                if (world != worldt) {
                                    stopPoint = null
                                    direction = null
                                }
                                val minX = argv[2].toInt()
                                val minY = argv[3].toInt()
                                val minZ = argv[4].toInt()
                                val maxX = argv[5].toInt()
                                val maxY = argv[6].toInt()
                                val maxZ = argv[7].toInt()
                                area = Range3D(minX, minY, minZ, maxX, maxY, maxZ)
                                area?.also { a ->
                                    world = worldt
                                    PluginMain.INSTANCE.messageManager.printf(player,
                                    "已设定区域范围为: ${world.name} §2(${
                                        a.minX}, ${a.minY}, ${a.minZ}) - (${a.maxX}, ${a.maxY}, ${a.maxZ})")
                                    preview(null)
                                }
                            }
                        } catch (e: NumberFormatException) {
                            PluginMain.INSTANCE.messageManager.printf(player, "§c区域范围格式错误! 请使用 world x1 y1 z1 x2 y2 z2 的格式!")
                        }
                    }
                }
            }
            "dir", "direction" -> {
                if (world != player.world) {
                    PluginMain.INSTANCE.messageManager.printf(player, "§c你必须在范围所在世界!")
                    return@gets false
                }
                try {
                    direction = Direction.valueOf(player.facing)
                    PluginMain.INSTANCE.messageManager.printf(player, "已设定方向为: §2${direction!!.name}")
                } catch (e: Exception) {
                    PluginMain.INSTANCE.messageManager.printf(player, "§c请面向正东西南北!")
                    return@gets false
                }
            }
            "stop" -> {
                if (argv.size < 2) {
                    val pointw = player.getPoint()
                    if (pointw == null) {
                        PluginMain.INSTANCE.messageManager.printf(player, "§c请先用小木斧选择一个铁轨, 注意是边长为1的范围!")
                    } else {
                        if (pointw.world != player.world) {
                            PluginMain.INSTANCE.messageManager.printf(player, "§c请先切换到选择铁轨所在的世界!")
                            return@gets false
                        }
                        if (world != pointw.world) {
                            PluginMain.INSTANCE.messageManager.printf(player, "§c停靠点必须在区域内!")
                            return@gets false
                        }
                        if (pointw.world.getBlockAt(pointw.point.x, pointw.point.y, pointw.point.z).type != Material.RAIL) {
                            PluginMain.INSTANCE.messageManager.printf(player, "§c请先用小木斧选择一个铁轨, 注意是边长为1的范围!")
                            return@gets false
                        }
                        world = pointw.world
                        stopPoint = pointw.point
                        PluginMain.INSTANCE.messageManager.printf(player, "根据你小木斧选择的铁轨，已设定停靠点(铁轨)为: §2(${
                            stopPoint!!.x}, ${stopPoint!!.y}, ${stopPoint!!.z})")
                        preview(null)
                    }
                } else {
                    if (argv.size != 4) {
                        PluginMain.INSTANCE.messageManager.printf(player, "§c方块格式错误! 请使用 x y z 的格式!")
                    } else {
                        try {
                            val x = argv[1].toInt()
                            val y = argv[2].toInt()
                            val z = argv[3].toInt()
                            stopPoint = Point3D(x, y, z)
                            PluginMain.INSTANCE.messageManager.printf(player, "已设定停靠点(铁轨)为: §2($x, $y, $z)")
                            preview(null)
                        } catch (e: NumberFormatException) {
                            PluginMain.INSTANCE.messageManager.printf(player, "§c方块格式错误! 请使用 x y z 的格式!")
                        }
                    }
                }
            }
            "preview" -> {
                preview(null)
            }
            "cancel" -> {
                PluginMain.INSTANCE.messageManager.printf(player, "已取消!")
                inputLocks.remove(player.uniqueId)
                done(null)
                return@gets true
            }
            "ok" -> {
                if (station == null || railLine == null || reverse == null) {
                    PluginMain.INSTANCE.messageManager.printf(player, "§c请先设定站点!")
                } else if (area == null) {
                    PluginMain.INSTANCE.messageManager.printf(player, "§c请先设定区域范围!")
                } else if (stopPoint == null) {
                    PluginMain.INSTANCE.messageManager.printf(player, "§c请先设定停靠点!")
                } else if (direction == null) {
                    PluginMain.INSTANCE.messageManager.printf(player, "§c请先设定方向!")
                } else {
                    if (!area!!.contains(stopPoint!!)) {
                        PluginMain.INSTANCE.messageManager.printf(player, "§c停靠点必须在区域内!")
                        return@gets false
                    }
                    inputLocks.remove(player.uniqueId)
                    done(RailArea(world, area!!, direction!!, stopPoint!!, station!!, railLine!!, reverse!!))
                    return@gets true
                }
            }
            else -> {
                PluginMain.INSTANCE.messageManager.printf(player, "§c未知指令! 你现在正处于区域设置模式，可用指令有: station, area, dir, stop, preview, cancel, ok")
            }
        }
        return@gets false
    }
}