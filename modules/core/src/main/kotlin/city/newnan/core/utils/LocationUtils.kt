package city.newnan.core.utils

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import kotlin.math.*

/**
 * 位置工具类
 *
 * 提供位置相关的实用方法，包括距离计算、位置转换、区域检测等。
 *
 * @author NewNanCity
 * @since 1.0.0
 */
object LocationUtils {

    /**
     * 计算两个位置之间的距离
     */
    fun distance(loc1: Location, loc2: Location): Double {
        return if (loc1.world?.name == loc2.world?.name) {
            loc1.distance(loc2)
        } else {
            Double.MAX_VALUE
        }
    }

    /**
     * 计算两个位置之间的距离（忽略Y轴）
     */
    fun distance2D(loc1: Location, loc2: Location): Double {
        return if (loc1.world?.name == loc2.world?.name) {
            val dx = loc1.x - loc2.x
            val dz = loc1.z - loc2.z
            sqrt(dx * dx + dz * dz)
        } else {
            Double.MAX_VALUE
        }
    }

    /**
     * 计算两个位置之间的距离平方（性能优化）
     */
    fun distanceSquared(loc1: Location, loc2: Location): Double {
        return if (loc1.world?.name == loc2.world?.name) {
            loc1.distanceSquared(loc2)
        } else {
            Double.MAX_VALUE
        }
    }

    /**
     * 检查位置是否在指定范围内
     */
    fun isWithinRange(center: Location, target: Location, range: Double): Boolean {
        return distance(center, target) <= range
    }

    /**
     * 检查位置是否在指定范围内（忽略Y轴）
     */
    fun isWithinRange2D(center: Location, target: Location, range: Double): Boolean {
        return distance2D(center, target) <= range
    }

    /**
     * 获取位置周围的方块
     */
    fun getBlocksAround(center: Location, radius: Int): List<Block> {
        val blocks = mutableListOf<Block>()
        val world = center.world ?: return blocks

        for (x in -radius..radius) {
            for (y in -radius..radius) {
                for (z in -radius..radius) {
                    val block = world.getBlockAt(
                        center.blockX + x,
                        center.blockY + y,
                        center.blockZ + z
                    )
                    blocks.add(block)
                }
            }
        }

        return blocks
    }

    /**
     * 获取位置周围的方块（球形）
     */
    fun getBlocksInSphere(center: Location, radius: Double): List<Block> {
        val blocks = mutableListOf<Block>()
        val world = center.world ?: return blocks
        val radiusSquared = radius * radius

        val minX = (center.x - radius).toInt()
        val maxX = (center.x + radius).toInt()
        val minY = (center.y - radius).toInt()
        val maxY = (center.y + radius).toInt()
        val minZ = (center.z - radius).toInt()
        val maxZ = (center.z + radius).toInt()

        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    val blockLoc = Location(world, x.toDouble(), y.toDouble(), z.toDouble())
                    if (distanceSquared(center, blockLoc) <= radiusSquared) {
                        blocks.add(world.getBlockAt(x, y, z))
                    }
                }
            }
        }

        return blocks
    }

    /**
     * 获取位置周围的实体
     */
    fun getEntitiesAround(center: Location, radius: Double): List<Entity> {
        val world = center.world ?: return emptyList()
        return world.entities.filter { distance(center, it.location) <= radius }
    }

    /**
     * 获取位置周围的玩家
     */
    fun getPlayersAround(center: Location, radius: Double): List<Player> {
        return getEntitiesAround(center, radius).filterIsInstance<Player>()
    }

    /**
     * 获取最近的玩家
     */
    fun getNearestPlayer(location: Location): Player? {
        val world = location.world ?: return null
        return world.players.minByOrNull { distance(location, it.location) }
    }

    /**
     * 获取最近的实体
     */
    fun getNearestEntity(location: Location, type: Class<out Entity>? = null): Entity? {
        val world = location.world ?: return null
        val entities = if (type != null) {
            world.entities.filter { type.isInstance(it) }
        } else {
            world.entities
        }
        return entities.minByOrNull { distance(location, it.location) }
    }

    /**
     * 检查位置是否安全（可以站立）
     */
    fun isSafeLocation(location: Location): Boolean {
        val world = location.world ?: return false
        val block = world.getBlockAt(location)
        val above = world.getBlockAt(location.clone().add(0.0, 1.0, 0.0))
        val below = world.getBlockAt(location.clone().subtract(0.0, 1.0, 0.0))

        return !block.type.isSolid && !above.type.isSolid && below.type.isSolid
    }

    /**
     * 寻找安全的位置
     */
    fun findSafeLocation(location: Location, maxRadius: Int = 10): Location? {
        val world = location.world ?: return null

        // 首先检查当前位置
        if (isSafeLocation(location)) {
            return location
        }

        // 在周围寻找安全位置
        for (radius in 1..maxRadius) {
            for (x in -radius..radius) {
                for (z in -radius..radius) {
                    if (abs(x) != radius && abs(z) != radius) continue

                    for (y in -radius..radius) {
                        val testLoc = location.clone().add(x.toDouble(), y.toDouble(), z.toDouble())
                        if (isSafeLocation(testLoc)) {
                            return testLoc
                        }
                    }
                }
            }
        }

        return null
    }

    /**
     * 获取位置的方向向量
     */
    fun getDirection(from: Location, to: Location): Vector {
        return to.toVector().subtract(from.toVector()).normalize()
    }

    /**
     * 根据方向和距离获取新位置
     */
    fun getLocationInDirection(location: Location, direction: Vector, distance: Double): Location {
        return location.clone().add(direction.clone().multiply(distance))
    }

    /**
     * 检查位置是否在矩形区域内
     */
    fun isInRegion(location: Location, corner1: Location, corner2: Location): Boolean {
        if (location.world?.name != corner1.world?.name || location.world?.name != corner2.world?.name) {
            return false
        }

        val minX = minOf(corner1.x, corner2.x)
        val maxX = maxOf(corner1.x, corner2.x)
        val minY = minOf(corner1.y, corner2.y)
        val maxY = maxOf(corner1.y, corner2.y)
        val minZ = minOf(corner1.z, corner2.z)
        val maxZ = maxOf(corner1.z, corner2.z)

        return location.x in minX..maxX &&
                location.y in minY..maxY &&
                location.z in minZ..maxZ
    }

    /**
     * 获取区域中心点
     */
    fun getRegionCenter(corner1: Location, corner2: Location): Location? {
        if (corner1.world?.name != corner2.world?.name) return null

        val centerX = (corner1.x + corner2.x) / 2
        val centerY = (corner1.y + corner2.y) / 2
        val centerZ = (corner1.z + corner2.z) / 2

        return Location(corner1.world, centerX, centerY, centerZ)
    }

    /**
     * 序列化位置为字符串
     */
    fun serialize(location: Location): String {
        return "${location.world?.name}:${location.x}:${location.y}:${location.z}:${location.yaw}:${location.pitch}"
    }

    /**
     * 从字符串反序列化位置
     */
    fun deserialize(serialized: String): Location? {
        val parts = serialized.split(":")
        if (parts.size != 6) return null

        val world = Bukkit.getWorld(parts[0]) ?: return null
        val x = parts[1].toDoubleOrNull() ?: return null
        val y = parts[2].toDoubleOrNull() ?: return null
        val z = parts[3].toDoubleOrNull() ?: return null
        val yaw = parts[4].toFloatOrNull() ?: return null
        val pitch = parts[5].toFloatOrNull() ?: return null

        return Location(world, x, y, z, yaw, pitch)
    }

    /**
     * 格式化位置为可读字符串
     */
    fun format(location: Location, decimals: Int = 2): String {
        val format = "%.${decimals}f"
        return "${location.world?.name} (${format.format(location.x)}, ${format.format(location.y)}, ${format.format(location.z)})"
    }
}

/**
 * Location 扩展函数
 */
fun Location.distanceTo(other: Location): Double = LocationUtils.distance(this, other)
fun Location.distance2DTo(other: Location): Double = LocationUtils.distance2D(this, other)
fun Location.isWithinRange(other: Location, range: Double): Boolean = LocationUtils.isWithinRange(this, other, range)
fun Location.isSafe(): Boolean = LocationUtils.isSafeLocation(this)
fun Location.findSafe(maxRadius: Int = 10): Location? = LocationUtils.findSafeLocation(this, maxRadius)
fun Location.serializeToString(): String = LocationUtils.serialize(this)
fun Location.format(decimals: Int = 2): String = LocationUtils.format(this, decimals)
