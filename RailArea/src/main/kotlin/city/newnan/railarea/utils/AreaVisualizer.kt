package city.newnan.railarea.utils

import city.newnan.railarea.PluginMain
import city.newnan.railarea.octree.Point3D
import city.newnan.railarea.octree.Range3D
import me.lucko.helper.Schedulers
import org.bukkit.Particle
import org.bukkit.World

fun Range3DWorld.visualize(particle: Particle, second: Int) =
    this.range.visualize(this.world, particle, second)

fun Range3D.visualize(world: World, particle: Particle, second: Int) {
    if (second <= 0) return
    var counter = second shl 1
    Schedulers.sync().runRepeating({ task ->
        // spawn particles to visualize area
        val minXv = minX.toDouble()
        val maxXv = maxX.toDouble()+1
        val minZv = minZ.toDouble()
        val maxZv = maxZ.toDouble()+1
        val minYv = minY.toDouble()
        val maxYv = maxY.toDouble()+1
        for (x in minX..(maxX+1)) {
            val xv = x.toDouble()
            world.spawnParticle(particle, xv, minYv, minZv, 1, 0.0, 0.0, 0.0, 0.0)
            world.spawnParticle(particle, xv, minYv, maxZv, 1, 0.0, 0.0, 0.0, 0.0)
            world.spawnParticle(particle, xv, maxYv, minZv, 1, 0.0, 0.0, 0.0, 0.0)
            world.spawnParticle(particle, xv, maxYv, maxZv, 1, 0.0, 0.0, 0.0, 0.0)
        }
        for (y in minY..(maxY+1)) {
            val yv = y.toDouble()
            world.spawnParticle(particle, minXv, yv, minZv, 1, 0.0, 0.0, 0.0, 0.0)
            world.spawnParticle(particle, minXv, yv, maxZv, 1, 0.0, 0.0, 0.0, 0.0)
            world.spawnParticle(particle, maxXv, yv, minZv, 1, 0.0, 0.0, 0.0, 0.0)
            world.spawnParticle(particle, maxXv, yv, maxZv, 1, 0.0, 0.0, 0.0, 0.0)
        }
        for (z in minZ..(maxZ+1)) {
            val zv = z.toDouble()
            world.spawnParticle(particle, minXv, minYv, zv, 1, 0.0, 0.0, 0.0, 0.0)
            world.spawnParticle(particle, minXv, maxYv, zv, 1, 0.0, 0.0, 0.0, 0.0)
            world.spawnParticle(particle, maxXv, minYv, zv, 1, 0.0, 0.0, 0.0, 0.0)
            world.spawnParticle(particle, maxXv, maxYv, zv, 1, 0.0, 0.0, 0.0, 0.0)
        }
        if (--counter < 0) task.close()
    }, 0, 10).bindWith(PluginMain.INSTANCE)
}

fun Point3DWorld.visualize(particle: Particle, second: Int) =
    this.point.visualize(this.world, particle, second)

fun Point3D.visualize(world: World, particle: Particle, second: Int) {
    if (second <= 0) return
    var counter = second shl 1
    val xO = x.toDouble()
    val yO = y.toDouble()
    val zO = z.toDouble()
    val xs = listOf(xO, xO+0.5, xO+1.0)
    val ys = listOf(yO, yO+0.5, yO+1.0)
    val zs = listOf(zO, zO+0.5, zO+1.0)
    Schedulers.sync().runRepeating({ task ->
        if (particle == Particle.BARRIER) {
            world.spawnParticle(particle, xs[1], ys[1], zs[1], 1, 0.0, 0.0, 0.0, 0.0)
        } else {
            for (x in xs) for (y in ys) for (z in zs)
                world.spawnParticle(particle, x, y, z, 1, 0.0, 0.0, 0.0, 0.0)
        }
        if (--counter < 0) task.close()
    }, 0, 10).bindWith(PluginMain.INSTANCE)
}