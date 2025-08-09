package city.newnan.railarea.spatial

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import kotlin.math.max
import kotlin.math.min

/**
 * 3D范围
 *
 * 表示三维空间中的一个立方体区域，用于空间索引和碰撞检测
 *
 * @param minX 最小X坐标
 * @param minY 最小Y坐标
 * @param minZ 最小Z坐标
 * @param maxX 最大X坐标
 * @param maxY 最大Y坐标
 * @param maxZ 最大Z坐标
 * @author NewNanCity
 * @since 2.0.0
 */
data class Range3D @JsonCreator constructor(
    @JsonProperty("minX") val minX: Int,
    @JsonProperty("minY") val minY: Int,
    @JsonProperty("minZ") val minZ: Int,
    @JsonProperty("maxX") val maxX: Int,
    @JsonProperty("maxY") val maxY: Int,
    @JsonProperty("maxZ") val maxZ: Int
) {

    init {
        require(minX <= maxX) { "minX ($minX) must be <= maxX ($maxX)" }
        require(minY <= maxY) { "minY ($minY) must be <= maxY ($maxY)" }
        require(minZ <= maxZ) { "minZ ($minZ) must be <= maxZ ($maxZ)" }
    }

    /**
     * 从两个点创建Range3D
     */
    constructor(point1: Point3D, point2: Point3D) : this(
        min(point1.x, point2.x), min(point1.y, point2.y), min(point1.z, point2.z),
        max(point1.x, point2.x), max(point1.y, point2.y), max(point1.z, point2.z)
    )

    /**
     * 从两个Location创建Range3D
     */
    constructor(loc1: Location, loc2: Location) : this(
        Point3D(loc1), Point3D(loc2)
    )

    fun toBoundingBox(): BoundingBox {
        return BoundingBox.of(
            Vector(minX.toDouble(), minY.toDouble(), minZ.toDouble()),
            Vector(maxX.toDouble(), maxY.toDouble(), maxZ.toDouble())
        )
    }

    /**
     * 获取范围的中心点
     */
    val center: Point3D
        get() = Point3D(
            (minX + maxX) / 2,
            (minY + maxY) / 2,
            (minZ + maxZ) / 2
        )

    /**
     * 获取范围的体积
     */
    val volume: Long
        get() = (maxX - minX + 1L) * (maxY - minY + 1L) * (maxZ - minZ + 1L)

    /**
     * 检查点是否在范围内
     */
    fun contains(point: Point3D): Boolean {
        return point.x >= minX && point.x <= maxX &&
               point.y >= minY && point.y <= maxY &&
               point.z >= minZ && point.z <= maxZ
    }

    /**
     * 检查Location是否在范围内
     */
    fun contains(location: Location): Boolean {
        return contains(Point3D(location))
    }

    /**
     * 检查两个范围是否相交
     */
    fun intersects(other: Range3D): Boolean {
        return !(maxX < other.minX || minX > other.maxX ||
                maxY < other.minY || minY > other.maxY ||
                maxZ < other.minZ || minZ > other.maxZ)
    }

    /**
     * 检查当前范围是否完全包含另一个范围
     */
    fun contains(other: Range3D): Boolean {
        return minX <= other.minX && maxX >= other.maxX &&
               minY <= other.minY && maxY >= other.maxY &&
               minZ <= other.minZ && maxZ >= other.maxZ
    }

    /**
     * 扩展范围
     */
    fun expand(amount: Int): Range3D {
        return Range3D(
            minX - amount, minY - amount, minZ - amount,
            maxX + amount, maxY + amount, maxZ + amount
        )
    }

    /**
     * 获取范围的8个角点
     */
    fun getCorners(): List<Point3D> {
        return listOf(
            Point3D(minX, minY, minZ), Point3D(maxX, minY, minZ),
            Point3D(minX, maxY, minZ), Point3D(maxX, maxY, minZ),
            Point3D(minX, minY, maxZ), Point3D(maxX, minY, maxZ),
            Point3D(minX, maxY, maxZ), Point3D(maxX, maxY, maxZ)
        )
    }

    /**
     * 分割为8个子范围（用于八叉树）
     */
    fun subdivide(): List<Range3D> {
        val centerX = (minX + maxX) / 2
        val centerY = (minY + maxY) / 2
        val centerZ = (minZ + maxZ) / 2

        return listOf(
            Range3D(minX, minY, minZ, centerX, centerY, centerZ),
            Range3D(centerX + 1, minY, minZ, maxX, centerY, centerZ),
            Range3D(minX, centerY + 1, minZ, centerX, maxY, centerZ),
            Range3D(centerX + 1, centerY + 1, minZ, maxX, maxY, centerZ),
            Range3D(minX, minY, centerZ + 1, centerX, centerY, maxZ),
            Range3D(centerX + 1, minY, centerZ + 1, maxX, centerY, maxZ),
            Range3D(minX, centerY + 1, centerZ + 1, centerX, maxY, maxZ),
            Range3D(centerX + 1, centerY + 1, centerZ + 1, maxX, maxY, maxZ)
        )
    }

    override fun toString(): String {
        return "Range3D(($minX,$minY,$minZ) to ($maxX,$maxY,$maxZ))"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Range3D

        if (minX != other.minX) return false
        if (minY != other.minY) return false
        if (minZ != other.minZ) return false
        if (maxX != other.maxX) return false
        if (maxY != other.maxY) return false
        if (maxZ != other.maxZ) return false

        return true
    }

    companion object {
        /**
         * 从两个点创建Range3D
         */
        fun fromPoints(point1: Point3D, point2: Point3D): Range3D {
            val minX = minOf(point1.x, point2.x)
            val maxX = maxOf(point1.x, point2.x)
            val minY = minOf(point1.y, point2.y)
            val maxY = maxOf(point1.y, point2.y)
            val minZ = minOf(point1.z, point2.z)
            val maxZ = maxOf(point1.z, point2.z)
            return Range3D(minX, minY, minZ, maxX, maxY, maxZ)
        }
    }
}