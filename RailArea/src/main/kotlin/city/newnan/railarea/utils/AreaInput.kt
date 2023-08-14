package city.newnan.railarea.utils

import city.newnan.railarea.PluginMain
import city.newnan.railarea.config.Direction
import city.newnan.railarea.config.RailArea
import city.newnan.railarea.config.RailLine
import city.newnan.railarea.config.Station
import city.newnan.railarea.octree.Point3D
import city.newnan.railarea.octree.Range3D
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

fun handleAreaInput (sender: Player, oldArea: RailArea?, done: (area: RailArea?) -> Unit): (String) -> Boolean {
    var station: Station? = oldArea?.station
    var railLine: RailLine? = oldArea?.line
    var area: Range3D? = oldArea?.range3D
    var world: World? = oldArea?.world
    var stopPoint: Point3D? = oldArea?.stopPoint
    var direction: Direction? = oldArea?.direction
    var reverse: Boolean? = oldArea?.reverse
    if (station == PluginMain.INSTANCE.unknownStation) station = null
    if (railLine == PluginMain.INSTANCE.unknownLine) railLine = null
    val preview = fun (p: String?) {
        if (station != null && reverse != null && railLine != null) {
            sender.sendTitle(station!!, railLine!!, reverse!!, RailTitleMode.UNDER_BOARD, 1, 70, 2)
            sender.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent("列车已到站停靠，您可下车，或留在车上待自动发车"))
        }
        area?.also {
            // spawn paticales to visualize area
            for (x in it.minX..(it.maxX+1)) {
                val xv = x.toDouble()
                sender.world.spawnParticle(Particle.FLAME, xv, it.minY.toDouble(), it.minZ.toDouble(), 1)
                sender.world.spawnParticle(Particle.FLAME, xv, it.minY.toDouble(), it.maxZ.toDouble(), 1)
                sender.world.spawnParticle(Particle.FLAME, xv, it.maxY.toDouble(), it.minZ.toDouble(), 1)
                sender.world.spawnParticle(Particle.FLAME, xv, it.maxY.toDouble(), it.maxZ.toDouble(), 1)
            }
            for (y in it.minY..(it.maxY+1)) {
                val yv = y.toDouble()
                sender.world.spawnParticle(Particle.FLAME, it.minX.toDouble(), yv, it.minZ.toDouble(), 1)
                sender.world.spawnParticle(Particle.FLAME, it.minX.toDouble(), yv, it.maxZ.toDouble(), 1)
                sender.world.spawnParticle(Particle.FLAME, it.maxX.toDouble(), yv, it.minZ.toDouble(), 1)
                sender.world.spawnParticle(Particle.FLAME, it.maxX.toDouble(), yv, it.maxZ.toDouble(), 1)
            }
            for (z in it.minZ..(it.maxZ+1)) {
                val zv = z.toDouble()
                sender.world.spawnParticle(Particle.FLAME, it.minX.toDouble(), it.minY.toDouble(), zv, 1)
                sender.world.spawnParticle(Particle.FLAME, it.minX.toDouble(), it.maxY.toDouble(), zv, 1)
                sender.world.spawnParticle(Particle.FLAME, it.maxX.toDouble(), it.minY.toDouble(), zv, 1)
                sender.world.spawnParticle(Particle.FLAME, it.maxX.toDouble(), it.maxY.toDouble(), zv, 1)
            }
        }
        stopPoint?.also {
            sender.world.spawnParticle(Particle.PORTAL, it.x.toDouble(), it.y.toDouble(), it.z.toDouble(), 10)
        }
        if (p == null) {
            PluginMain.INSTANCE.messageManager.printf(sender, "效果预览已显示")
        } else {
            PluginMain.INSTANCE.messageManager.printf(sender, "&2$p&r 已设定, 效果预览已显示，请继续设置其他属性!")
        }
    }
    return fun (input: String): Boolean {
        when {
            input.startsWith("station:") -> {
                getLineStation(sender, false) { l, s, _ ->
                    println("[4]")
                    val gui = Gui.gui()
                        .title(Component.text("选择方向"))
                        .rows(6)
                        .create()
                    gui.setItem(1, 1, ItemBuilder.from(Material.NETHER_STAR).name(Component.text("开往 ${l.stations.last().name} 方向")).asGuiItem {
                        reverse = false
                        railLine = l
                        station = s
                        preview("站点")
                        gui.close(sender)
                    })
                    gui.setItem(1, 2, ItemBuilder.from(Material.NETHER_STAR).name(Component.text("开往 ${l.stations.first().name} 方向")).asGuiItem {
                        reverse = true
                        railLine = l
                        station = s
                        preview("站点")
                        gui.close(sender)
                    })
                    gui.setDefaultClickAction { it.isCancelled = true }
                    Schedulers.sync().runLater({ gui.open(sender) }, 1)
                }
            }
            input.startsWith("area:") -> {
                val areaStr = input.substring(5).trim()
                if (areaStr.isEmpty()) {
                    val areaw = sender.getSelection()
                    if (areaw == null) {
                        PluginMain.INSTANCE.messageManager.printf(sender, "&c请先用小木斧选择一个区域!")
                    } else {
                        if (areaw.world != sender.world) {
                            PluginMain.INSTANCE.messageManager.printf(sender, "&c请先切换到选择区域所在的世界!")
                            return false
                        }
                        if (world != areaw.world) {
                            stopPoint = null
                            direction = null
                        }
                        world = areaw.world
                        area = areaw.range
                        area?.also { a ->
                            PluginMain.INSTANCE.messageManager.printf(sender,
                                "根据你小木斧选择的区域，已设定区域范围为: ${world!!.name} &2(${
                                    a.minX}, ${a.minY}, ${a.minZ}) - (${a.maxX}, ${a.maxY}, ${a.maxZ})")
                        }
                    }
                } else {
                    val range = areaStr.split(" ")
                    if (range.size != 7) {
                        PluginMain.INSTANCE.messageManager.printf(sender, "&c区域范围格式错误! 请使用 world x1 y1 z1 x2 y2 z2 的格式!")
                    } else {
                        try {
                            val worldt = Bukkit.getWorld(range[0])
                            if (worldt == null) {
                                PluginMain.INSTANCE.messageManager.printf(sender, "&c世界 ${range[0]} 不存在!")
                            } else {
                                if (world != worldt) {
                                    stopPoint = null
                                    direction = null
                                }
                                val minX = range[1].toInt()
                                val minY = range[2].toInt()
                                val minZ = range[3].toInt()
                                val maxX = range[4].toInt()
                                val maxY = range[5].toInt()
                                val maxZ = range[6].toInt()
                                area = Range3D(minX, minY, minZ, maxX, maxY, maxZ)
                                area?.also { a ->
                                    world = worldt
                                    PluginMain.INSTANCE.messageManager.printf(sender,
                                    "已设定区域范围为: ${world!!.name} &2(${
                                        a.minX}, ${a.minY}, ${a.minZ}) - (${a.maxX}, ${a.maxY}, ${a.maxZ})")
                                }
                            }
                        } catch (e: NumberFormatException) {
                            PluginMain.INSTANCE.messageManager.printf(sender, "&c区域范围格式错误! 请使用 world x1 y1 z1 x2 y2 z2 的格式!")
                        }
                    }
                }
            }
            input.startsWith("direction:") -> {
                if (world != null && world != sender.world) {
                    PluginMain.INSTANCE.messageManager.printf(sender, "&c你必须在范围所在世界!")
                    return false
                }
                try {
                    direction = Direction.valueOf(sender.facing)
                    PluginMain.INSTANCE.messageManager.printf(sender, "已设定方向为: &2${direction!!.name}")
                } catch (e: Exception) {
                    PluginMain.INSTANCE.messageManager.printf(sender, "&c请面向正东西南北!")
                    return false
                }
            }
            input.startsWith("stop:") -> {
                val stopStr = input.substring(5).trim()
                if (stopStr.isEmpty()) {
                    val pointw = sender.getPoint()
                    if (pointw == null) {
                        PluginMain.INSTANCE.messageManager.printf(sender, "&c请先用小木斧选择一个铁轨, 注意是边长为1的范围!")
                    } else {
                        if (pointw.world != sender.world) {
                            PluginMain.INSTANCE.messageManager.printf(sender, "&c请先切换到选择铁轨所在的世界!")
                            return false
                        }
                        if (world != null && world != pointw.world) {
                            PluginMain.INSTANCE.messageManager.printf(sender, "&c停靠点必须在区域内!")
                            return false
                        }
                        if (pointw.world.getBlockAt(pointw.point.x, pointw.point.y, pointw.point.z).type != Material.RAIL) {
                            PluginMain.INSTANCE.messageManager.printf(sender, "&c请先用小木斧选择一个铁轨, 注意是边长为1的范围!")
                            return false
                        }
                        world = pointw.world
                        stopPoint = pointw.point
                        PluginMain.INSTANCE.messageManager.printf(sender, "根据你小木斧选择的铁轨，已设定停靠点(铁轨)为: &2(${
                            stopPoint!!.x}, ${stopPoint!!.y}, ${stopPoint!!.z})")
                    }
                } else {
                    val stop = stopStr.split(" ")
                    if (stop.size != 3) {
                        PluginMain.INSTANCE.messageManager.printf(sender, "&c方块格式错误! 请使用 x y z 的格式!")
                    } else {
                        try {
                            val x = stop[1].toInt()
                            val y = stop[2].toInt()
                            val z = stop[3].toInt()
                            stopPoint = Point3D(x, y, z)
                            PluginMain.INSTANCE.messageManager.printf(sender, "已设定停靠点(铁轨)为: &2($x, $y, $z)")
                        } catch (e: NumberFormatException) {
                            PluginMain.INSTANCE.messageManager.printf(sender, "&c方块格式错误! 请使用 x y z 的格式!")
                        }
                    }
                }
            }
            input == "preview" -> {
                preview(null)
            }
            input == "cancel" -> {
                PluginMain.INSTANCE.messageManager.printf(sender, "已取消!")
                done(null)
                return true
            }
            input == "ok" -> {
                if (station == null || railLine == null || reverse == null) {
                    PluginMain.INSTANCE.messageManager.printf(sender, "&c请先设定站点!")
                } else if (area == null || world == null) {
                    PluginMain.INSTANCE.messageManager.printf(sender, "&c请先设定区域范围!")
                } else if (stopPoint == null) {
                    PluginMain.INSTANCE.messageManager.printf(sender, "&c请先设定停靠点!")
                } else if (direction == null) {
                    PluginMain.INSTANCE.messageManager.printf(sender, "&c请先设定方向!")
                } else {
                    if (!area!!.contains(stopPoint!!)) {
                        PluginMain.INSTANCE.messageManager.printf(sender, "&c停靠点必须在区域内!")
                        return false
                    }
                    done(RailArea(world!!, area!!, direction!!, stopPoint!!, station!!, railLine!!, reverse!!))
                    return true
                }
            }
        }
        return false
    }
}