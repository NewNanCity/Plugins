@file:Suppress("unused")

package city.newnan.config.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import kotlin.math.*

/**
 * 不可变的可序列化区块区域对象
 *
 * 表示一个二维空间中的区块区域
 *
 * @property min 最小区块坐标点
 * @property max 最大区块坐标点
 * @author Gk0Wk
 * @since 1.0.0
 */
@JsonSerialize(using = ChunkRegionSerializer::class)
@JsonDeserialize(using = ChunkRegionDeserializer::class)
data class ChunkRegion(
    val min: ChunkPosition,
    val max: ChunkPosition
) {
    init {
        require(min.world == max.world) { "区域的两个位置必须在同一个世界中" }
    }

    /**
     * 区域宽度（X轴区块数）
     */
    val width: Int = max.x - min.x + 1

    /**
     * 区域深度（Z轴区块数）
     */
    val depth: Int = max.z - min.z + 1

    /**
     * 区域区块总数
     */
    val chunkCount: Int = width * depth

    /**
     * 区域中心区块位置
     */
    val center: ChunkPosition = ChunkPosition(
        (min.x + max.x) / 2,
        (min.z + max.z) / 2,
        min.world
    )

    /**
     * 检查区块位置是否在区域内
     */
    fun contains(position: ChunkPosition): Boolean {
        return position.world == min.world &&
                position.x >= min.x && position.x <= max.x &&
                position.z >= min.z && position.z <= max.z
    }

    /**
     * 检查坐标是否在区域内
     */
    fun contains(x: Int, z: Int): Boolean {
        return x >= min.x && x <= max.x &&
                z >= min.z && z <= max.z
    }

    /**
     * 扩展区域
     */
    fun expand(amount: Int): ChunkRegion {
        return ChunkRegion(
            ChunkPosition(min.x - amount, min.z - amount, min.world),
            ChunkPosition(max.x + amount, max.z + amount, max.world)
        )
    }

    /**
     * 收缩区域
     */
    fun contract(amount: Int): ChunkRegion {
        return expand(-amount)
    }

    /**
     * 获取区域的所有区块位置
     */
    fun getChunkPositions(): List<ChunkPosition> {
        val positions = mutableListOf<ChunkPosition>()
        
        for (x in min.x..max.x) {
            for (z in min.z..max.z) {
                positions.add(ChunkPosition(x, z, min.world))
            }
        }
        return positions
    }

    /**
     * 转换为方块区域（覆盖所有区块的方块）
     */
    fun toBlockRegion(): BlockRegion {
        return BlockRegion(
            BlockPosition(min.x shl 4, 0, min.z shl 4, min.world),
            BlockPosition((max.x shl 4) + 15, 255, (max.z shl 4) + 15, max.world)
        )
    }

    companion object {
        /**
         * 从两个区块位置创建区域
         */
        fun of(a: ChunkPosition, b: ChunkPosition): ChunkRegion {
            require(a.world == b.world) { "两个位置必须在同一个世界中" }
            
            val minPos = ChunkPosition(
                min(a.x, b.x),
                min(a.z, b.z),
                a.world
            )
            val maxPos = ChunkPosition(
                max(a.x, b.x),
                max(a.z, b.z),
                a.world
            )
            
            return ChunkRegion(minPos, maxPos)
        }
    }
}

/**
 * ChunkRegion序列化器
 */
class ChunkRegionSerializer : JsonSerializer<ChunkRegion>() {
    override fun serialize(
        value: ChunkRegion,
        gen: JsonGenerator,
        serializers: SerializerProvider
    ) {
        gen.writeStartObject()
        gen.writeObjectField("min", value.min)
        gen.writeObjectField("max", value.max)
        gen.writeEndObject()
    }
}

/**
 * ChunkRegion反序列化器
 */
class ChunkRegionDeserializer : JsonDeserializer<ChunkRegion>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext
    ): ChunkRegion {
        val node = p.readValueAsTree<JsonNode>()
        
        val minNode = node.get("min")
            ?: throw JsonMappingException.from(p, "缺少必需字段: min")
        val maxNode = node.get("max")
            ?: throw JsonMappingException.from(p, "缺少必需字段: max")
            
        val min = p.codec.treeToValue(minNode, ChunkPosition::class.java)
        val max = p.codec.treeToValue(maxNode, ChunkPosition::class.java)
            
        return ChunkRegion(min, max)
    }
}

/**
 * 不可变的可序列化圆形区域对象
 *
 * 表示一个二维空间中的圆形区域（忽略Y轴）
 *
 * @property center 圆心位置
 * @property radius 半径
 * @author Gk0Wk
 * @since 1.0.0
 */
@JsonSerialize(using = CircularRegionSerializer::class)
@JsonDeserialize(using = CircularRegionDeserializer::class)
data class CircularRegion(
    val center: Position,
    val radius: Double
) {
    init {
        require(radius >= 0) { "半径不能为负数" }
    }

    /**
     * 圆形区域面积
     */
    val area: Double = PI * radius * radius

    /**
     * 圆形区域周长
     */
    val circumference: Double = 2 * PI * radius

    /**
     * 检查位置是否在圆形区域内（忽略Y轴）
     */
    fun contains(position: Position): Boolean {
        if (position.world != center.world) return false
        
        val dx = position.x - center.x
        val dz = position.z - center.z
        val distanceSquared = dx * dx + dz * dz
        
        return distanceSquared <= radius * radius
    }

    /**
     * 检查坐标是否在圆形区域内（忽略Y轴）
     */
    fun contains(x: Double, z: Double): Boolean {
        val dx = x - center.x
        val dz = z - center.z
        val distanceSquared = dx * dx + dz * dz
        
        return distanceSquared <= radius * radius
    }

    /**
     * 计算到位置的距离（忽略Y轴）
     */
    fun distanceTo(position: Position): Double {
        require(position.world == center.world) { "位置必须在同一个世界中" }
        
        val dx = position.x - center.x
        val dz = position.z - center.z
        
        return sqrt(dx * dx + dz * dz)
    }

    /**
     * 扩展圆形区域
     */
    fun expand(amount: Double): CircularRegion {
        return CircularRegion(center, radius + amount)
    }

    /**
     * 收缩圆形区域
     */
    fun contract(amount: Double): CircularRegion {
        return expand(-amount)
    }

    /**
     * 获取边界矩形区域
     */
    fun getBoundingRegion(): Region {
        return Region(
            Position(center.x - radius, center.y - radius, center.z - radius, center.world),
            Position(center.x + radius, center.y + radius, center.z + radius, center.world)
        )
    }

    /**
     * 获取圆形区域内的方块位置（近似）
     */
    fun getBlockPositions(minY: Int = 0, maxY: Int = 255): List<BlockPosition> {
        val positions = mutableListOf<BlockPosition>()
        val radiusInt = ceil(radius).toInt()
        
        for (x in -radiusInt..radiusInt) {
            for (z in -radiusInt..radiusInt) {
                val blockX = center.x.toInt() + x
                val blockZ = center.z.toInt() + z
                
                if (contains(blockX.toDouble(), blockZ.toDouble())) {
                    for (y in minY..maxY) {
                        positions.add(BlockPosition(blockX, y, blockZ, center.world))
                    }
                }
            }
        }
        
        return positions
    }

    companion object {
        /**
         * 创建圆形区域
         */
        fun of(center: Position, radius: Double): CircularRegion {
            return CircularRegion(center, radius)
        }

        /**
         * 从两个位置创建圆形区域（以第一个位置为圆心，到第二个位置的距离为半径）
         */
        fun of(center: Position, edge: Position): CircularRegion {
            require(center.world == edge.world) { "两个位置必须在同一个世界中" }
            
            val dx = edge.x - center.x
            val dz = edge.z - center.z
            val radius = sqrt(dx * dx + dz * dz)
            
            return CircularRegion(center, radius)
        }
    }
}

/**
 * CircularRegion序列化器
 */
class CircularRegionSerializer : JsonSerializer<CircularRegion>() {
    override fun serialize(
        value: CircularRegion,
        gen: JsonGenerator,
        serializers: SerializerProvider
    ) {
        gen.writeStartObject()
        gen.writeObjectField("center", value.center)
        gen.writeNumberField("radius", value.radius)
        gen.writeEndObject()
    }
}

/**
 * CircularRegion反序列化器
 */
class CircularRegionDeserializer : JsonDeserializer<CircularRegion>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext
    ): CircularRegion {
        val node = p.readValueAsTree<JsonNode>()
        
        val centerNode = node.get("center")
            ?: throw JsonMappingException.from(p, "缺少必需字段: center")
        val radius = node.get("radius")?.asDouble()
            ?: throw JsonMappingException.from(p, "缺少必需字段: radius")
            
        val center = p.codec.treeToValue(centerNode, Position::class.java)
            
        return CircularRegion(center, radius)
    }
}
