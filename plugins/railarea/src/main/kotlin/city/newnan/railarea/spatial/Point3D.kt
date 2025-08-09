package city.newnan.railarea.spatial

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.bukkit.Location
import org.bukkit.World

/**
 * 3D点坐标
 * 
 * 表示三维空间中的一个点，用于空间索引和碰撞检测
 * 
 * @param x X坐标
 * @param y Y坐标  
 * @param z Z坐标
 * @author NewNanCity
 * @since 2.0.0
 */
data class Point3D @JsonCreator constructor(
    @JsonProperty("x") val x: Int,
    @JsonProperty("y") val y: Int,
    @JsonProperty("z") val z: Int
) {
    
    /**
     * 从Bukkit Location创建Point3D
     */
    constructor(location: Location) : this(
        location.blockX,
        location.blockY,
        location.blockZ
    )
    
    /**
     * 转换为Bukkit Location
     */
    fun toLocation(world: World): Location {
        return Location(world, x.toDouble(), y.toDouble(), z.toDouble())
    }
    
    /**
     * 计算到另一个点的距离平方
     */
    fun distanceSquared(other: Point3D): Double {
        val dx = (x - other.x).toDouble()
        val dy = (y - other.y).toDouble()
        val dz = (z - other.z).toDouble()
        return dx * dx + dy * dy + dz * dz
    }
    
    /**
     * 计算到另一个点的距离
     */
    fun distance(other: Point3D): Double {
        return kotlin.math.sqrt(distanceSquared(other))
    }
    
    /**
     * 检查点是否在指定范围内
     */
    fun isInRange(range: Range3D): Boolean {
        return x >= range.minX && x <= range.maxX &&
               y >= range.minY && y <= range.maxY &&
               z >= range.minZ && z <= range.maxZ
    }
    
    /**
     * 偏移点坐标
     */
    fun offset(dx: Int, dy: Int, dz: Int): Point3D {
        return Point3D(x + dx, y + dy, z + dz)
    }
    
    /**
     * 获取相邻的8个点（用于八叉树分割）
     */
    fun getAdjacentPoints(): List<Point3D> {
        return listOf(
            Point3D(x - 1, y, z), Point3D(x + 1, y, z),
            Point3D(x, y - 1, z), Point3D(x, y + 1, z),
            Point3D(x, y, z - 1), Point3D(x, y, z + 1),
            Point3D(x - 1, y - 1, z), Point3D(x + 1, y + 1, z)
        )
    }
    
    override fun toString(): String {
        return "Point3D($x, $y, $z)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Point3D
        return x == other.x && y == other.y && z == other.z
    }
}