package city.newnan.railarea.input

import city.newnan.railarea.PluginMain
import city.newnan.railarea.config.Direction
import city.newnan.railarea.config.RailArea
import city.newnan.railarea.config.RailLine
import city.newnan.railarea.config.Station
import city.newnan.railarea.gui.showLineStationGui
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

fun handleAreaInput (sender: Player, oldArea: RailArea?, iStation: Station? = null, iLine: RailLine? = null,
                     done: (area: RailArea?) -> Unit) {
    var station: Station? = oldArea?.station ?: iStation
    var railLine: RailLine? = oldArea?.line ?: iLine
    var area: Range3D? = oldArea?.range3D
    var world: World? = oldArea?.world
    var stopPoint: Point3D? = oldArea?.stopPoint
    var direction: Direction? = oldArea?.direction
    var reverse: Boolean? = oldArea?.reverse
    if (station == PluginMain.INSTANCE.unknownStation) station = null
    if (railLine == PluginMain.INSTANCE.unknownLine) railLine = null
    fun preview(p: String?) {
        if (station != null && reverse != null && railLine != null) {
            sender.sendTitle(station!!, railLine!!, reverse!!, RailTitleMode.UNDER_BOARD, 1, 70, 2)
            sender.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent("列车已到站停靠，您可下车，或留在车上待自动发车"))
        }
        area?.also {
            var counter = 20
            Schedulers.sync().runRepeating({ task ->
                // spawn paticales to visualize area
                for (x in it.minX..(it.maxX+1)) {
                    val xv = x.toDouble()
                    world!!.spawnParticle(Particle.FLAME, xv, it.minY.toDouble(), it.minZ.toDouble(), 5, 0.0, 0.0, 0.0, 0.0)
                    world!!.spawnParticle(Particle.FLAME, xv, it.minY.toDouble(), it.maxZ.toDouble(), 5, 0.0, 0.0, 0.0, 0.0)
                    world!!.spawnParticle(Particle.FLAME, xv, it.maxY.toDouble(), it.minZ.toDouble(), 5, 0.0, 0.0, 0.0, 0.0)
                    world!!.spawnParticle(Particle.FLAME, xv, it.maxY.toDouble(), it.maxZ.toDouble(), 5, 0.0, 0.0, 0.0, 0.0)
                }
                for (y in it.minY..(it.maxY+1)) {
                    val yv = y.toDouble()
                    world!!.spawnParticle(Particle.FLAME, it.minX.toDouble(), yv, it.minZ.toDouble(), 5, 0.0, 0.0, 0.0, 0.0)
                    world!!.spawnParticle(Particle.FLAME, it.minX.toDouble(), yv, it.maxZ.toDouble(), 5, 0.0, 0.0, 0.0, 0.0)
                    world!!.spawnParticle(Particle.FLAME, it.maxX.toDouble(), yv, it.minZ.toDouble(), 5, 0.0, 0.0, 0.0, 0.0)
                    world!!.spawnParticle(Particle.FLAME, it.maxX.toDouble(), yv, it.maxZ.toDouble(), 5, 0.0, 0.0, 0.0, 0.0)
                }
                for (z in it.minZ..(it.maxZ+1)) {
                    val zv = z.toDouble()
                    world!!.spawnParticle(Particle.FLAME, it.minX.toDouble(), it.minY.toDouble(), zv, 5, 0.0, 0.0, 0.0, 0.0)
                    world!!.spawnParticle(Particle.FLAME, it.minX.toDouble(), it.maxY.toDouble(), zv, 5, 0.0, 0.0, 0.0, 0.0)
                    world!!.spawnParticle(Particle.FLAME, it.maxX.toDouble(), it.minY.toDouble(), zv, 5, 0.0, 0.0, 0.0, 0.0)
                    world!!.spawnParticle(Particle.FLAME, it.maxX.toDouble(), it.maxY.toDouble(), zv, 5, 0.0, 0.0, 0.0, 0.0)
                }
                if (--counter < 0) task.close()
            }, 0, 10)
        }
        stopPoint?.also {
            var counter = 20
            val xO = it.x.toDouble()
            val yO = it.y.toDouble()
            val zO = it.z.toDouble()
            val xs = listOf(xO, xO+0.5, xO+1.0)
            val ys = listOf(yO, yO+0.5, yO+1.0)
            val zs = listOf(zO, zO+0.5, zO+1.0)
            Schedulers.sync().runRepeating({ task ->
                // Draw a cube fire framework
                for (x in xs) {
                    for (y in ys) {
                        for (z in zs) {
                            world!!.spawnParticle(Particle.PORTAL, x, y, z, 3, 0.0, 0.0, 0.0, 0.0)
                        }
                    }
                }
                if (--counter < 0) task.close()
            }, 0, 10)
        }
        if (p == null) {
            PluginMain.INSTANCE.messageManager.printf(sender, "效果预览已显示")
        } else {
            PluginMain.INSTANCE.messageManager.printf(sender, "&2$p&r 已设定, 效果预览已显示，请继续设置其他属性!")
        }
    }
    PluginMain.INSTANCE.messageManager.gets(sender) { input ->
        val argv = input.split(" ").filter { it.isNotEmpty() }
        when (argv[0].lowercase()) {
            "station" -> {
                showLineStationGui(sender, false) { l, s, _ ->
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
            "area" -> {
                if (argv.size < 2) {
                    val areaw = sender.getSelection()
                    if (areaw == null) {
                        PluginMain.INSTANCE.messageManager.printf(sender, "&c请先用小木斧选择一个区域!")
                    } else {
                        if (areaw.world != sender.world) {
                            PluginMain.INSTANCE.messageManager.printf(sender, "&c请先切换到选择区域所在的世界!")
                            return@gets false
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
                    if (argv.size != 8) {
                        PluginMain.INSTANCE.messageManager.printf(sender, "&c区域范围格式错误! 请使用 world x1 y1 z1 x2 y2 z2 的格式!")
                    } else {
                        try {
                            val worldt = Bukkit.getWorld(argv[1])
                            if (worldt == null) {
                                PluginMain.INSTANCE.messageManager.printf(sender, "&c世界 ${argv[1]} 不存在!")
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
            "dir" -> {
                if (world != null && world != sender.world) {
                    PluginMain.INSTANCE.messageManager.printf(sender, "&c你必须在范围所在世界!")
                    return@gets false
                }
                try {
                    direction = Direction.valueOf(sender.facing)
                    PluginMain.INSTANCE.messageManager.printf(sender, "已设定方向为: &2${direction!!.name}")
                } catch (e: Exception) {
                    PluginMain.INSTANCE.messageManager.printf(sender, "&c请面向正东西南北!")
                    return@gets false
                }
            }
            "stop" -> {
                if (argv.size < 2) {
                    val pointw = sender.getPoint()
                    if (pointw == null) {
                        PluginMain.INSTANCE.messageManager.printf(sender, "&c请先用小木斧选择一个铁轨, 注意是边长为1的范围!")
                    } else {
                        if (pointw.world != sender.world) {
                            PluginMain.INSTANCE.messageManager.printf(sender, "&c请先切换到选择铁轨所在的世界!")
                            return@gets false
                        }
                        if (world != null && world != pointw.world) {
                            PluginMain.INSTANCE.messageManager.printf(sender, "&c停靠点必须在区域内!")
                            return@gets false
                        }
                        if (pointw.world.getBlockAt(pointw.point.x, pointw.point.y, pointw.point.z).type != Material.RAIL) {
                            PluginMain.INSTANCE.messageManager.printf(sender, "&c请先用小木斧选择一个铁轨, 注意是边长为1的范围!")
                            return@gets false
                        }
                        world = pointw.world
                        stopPoint = pointw.point
                        PluginMain.INSTANCE.messageManager.printf(sender, "根据你小木斧选择的铁轨，已设定停靠点(铁轨)为: &2(${
                            stopPoint!!.x}, ${stopPoint!!.y}, ${stopPoint!!.z})")
                    }
                } else {
                    if (argv.size != 4) {
                        PluginMain.INSTANCE.messageManager.printf(sender, "&c方块格式错误! 请使用 x y z 的格式!")
                    } else {
                        try {
                            val x = argv[1].toInt()
                            val y = argv[2].toInt()
                            val z = argv[3].toInt()
                            stopPoint = Point3D(x, y, z)
                            PluginMain.INSTANCE.messageManager.printf(sender, "已设定停靠点(铁轨)为: &2($x, $y, $z)")
                        } catch (e: NumberFormatException) {
                            PluginMain.INSTANCE.messageManager.printf(sender, "&c方块格式错误! 请使用 x y z 的格式!")
                        }
                    }
                }
            }
            "preview" -> {
                preview(null)
            }
            "cancel" -> {
                PluginMain.INSTANCE.messageManager.printf(sender, "已取消!")
                done(null)
                return@gets true
            }
            "ok" -> {
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
                        return@gets false
                    }
                    done(RailArea(world!!, area!!, direction!!, stopPoint!!, station!!, railLine!!, reverse!!))
                    return@gets true
                }
            }
            else -> {
                PluginMain.INSTANCE.messageManager.printf(sender, "&c未知指令! 你现在正处于区域设置模式，可用指令有: station, area, dir, stop, preview, cancel, ok")
            }
        }
        return@gets false
    }
}