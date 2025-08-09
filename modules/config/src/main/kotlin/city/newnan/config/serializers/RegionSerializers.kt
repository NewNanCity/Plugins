@file:Suppress("unused")

package city.newnan.config.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import kotlin.math.*

/**
 * 不可变的可序列化区域对象
 *
 * 表示一个三维空间中的矩形区域
 *
 * @property min 最小坐标点
 * @property max 最大坐标点
 * @author Gk0Wk
 * @since 1.0.0
 */
@JsonSerialize(using = RegionSerializer::class)
@JsonDeserialize(using = RegionDeserializer::class)
data class Region(
    val min: Position,
    val max: Position
) {
    init {
        require(min.world == max.world) { "区域的两个位置必须在同一个世界中" }
    }

    /**
     * 区域宽度（X轴）
     */
    val width: Double = max.x - min.x

    /**
     * 区域高度（Y轴）
     */
    val height: Double = max.y - min.y

    /**
     * 区域深度（Z轴）
     */
    val depth: Double = max.z - min.z

    /**
     * 区域体积
     */
    val volume: Double = width * height * depth

    /**
     * 区域中心点
     */
    val center: Position = Position(
        (min.x + max.x) / 2.0,
        (min.y + max.y) / 2.0,
        (min.z + max.z) / 2.0,
        min.world
    )

    /**
     * 检查位置是否在区域内
     */
    fun contains(position: Position): Boolean {
        return position.world == min.world &&
                position.x >= min.x && position.x <= max.x &&
                position.y >= min.y && position.y <= max.y &&
                position.z >= min.z && position.z <= max.z
    }

    /**
     * 检查坐标是否在区域内
     */
    fun contains(x: Double, y: Double, z: Double): Boolean {
        return x >= min.x && x <= max.x &&
                y >= min.y && y <= max.y &&
                z >= min.z && z <= max.z
    }

    /**
     * 扩展区域
     */
    fun expand(amount: Double): Region {
        return Region(
            Position(min.x - amount, min.y - amount, min.z - amount, min.world),
            Position(max.x + amount, max.y + amount, max.z + amount, max.world)
        )
    }

    /**
     * 收缩区域
     */
    fun contract(amount: Double): Region {
        return expand(-amount)
    }

    /**
     * 获取区域的所有方块位置
     */
    fun getBlockPositions(): List<BlockPosition> {
        val positions = mutableListOf<BlockPosition>()
        val minBlock = min.floor()
        val maxBlock = max.floor()
        
        for (x in minBlock.x..maxBlock.x) {
            for (y in minBlock.y..maxBlock.y) {
                for (z in minBlock.z..maxBlock.z) {
                    positions.add(BlockPosition(x, y, z, min.world))
                }
            }
        }
        return positions
    }

    companion object {
        /**
         * 从两个位置创建区域
         */
        fun of(a: Position, b: Position): Region {
            require(a.world == b.world) { "两个位置必须在同一个世界中" }
            
            val minPos = Position(
                min(a.x, b.x),
                min(a.y, b.y),
                min(a.z, b.z),
                a.world
            )
            val maxPos = Position(
                max(a.x, b.x),
                max(a.y, b.y),
                max(a.z, b.z),
                a.world
            )
            
            return Region(minPos, maxPos)
        }
    }
}

/**
 * Region序列化器
 */
class RegionSerializer : JsonSerializer<Region>() {
    override fun serialize(
        value: Region,
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
 * Region反序列化器
 */
class RegionDeserializer : JsonDeserializer<Region>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext
    ): Region {
        val node = p.readValueAsTree<JsonNode>()
        
        val minNode = node.get("min")
            ?: throw JsonMappingException.from(p, "缺少必需字段: min")
        val maxNode = node.get("max")
            ?: throw JsonMappingException.from(p, "缺少必需字段: max")
            
        val min = p.codec.treeToValue(minNode, Position::class.java)
        val max = p.codec.treeToValue(maxNode, Position::class.java)
            
        return Region(min, max)
    }
}

/**
 * 不可变的可序列化方块区域对象
 *
 * 表示一个三维空间中的整数方块区域
 *
 * @property min 最小方块坐标点
 * @property max 最大方块坐标点
 * @author Gk0Wk
 * @since 1.0.0
 */
@JsonSerialize(using = BlockRegionSerializer::class)
@JsonDeserialize(using = BlockRegionDeserializer::class)
data class BlockRegion(
    val min: BlockPosition,
    val max: BlockPosition
) {
    init {
        require(min.world == max.world) { "区域的两个位置必须在同一个世界中" }
    }

    /**
     * 区域宽度（X轴方块数）
     */
    val width: Int = max.x - min.x + 1

    /**
     * 区域高度（Y轴方块数）
     */
    val height: Int = max.y - min.y + 1

    /**
     * 区域深度（Z轴方块数）
     */
    val depth: Int = max.z - min.z + 1

    /**
     * 区域方块总数
     */
    val blockCount: Int = width * height * depth

    /**
     * 区域中心方块位置
     */
    val center: BlockPosition = BlockPosition(
        (min.x + max.x) / 2,
        (min.y + max.y) / 2,
        (min.z + max.z) / 2,
        min.world
    )

    /**
     * 检查方块位置是否在区域内
     */
    fun contains(position: BlockPosition): Boolean {
        return position.world == min.world &&
                position.x >= min.x && position.x <= max.x &&
                position.y >= min.y && position.y <= max.y &&
                position.z >= min.z && position.z <= max.z
    }

    /**
     * 检查坐标是否在区域内
     */
    fun contains(x: Int, y: Int, z: Int): Boolean {
        return x >= min.x && x <= max.x &&
                y >= min.y && y <= max.y &&
                z >= min.z && z <= max.z
    }

    /**
     * 扩展区域
     */
    fun expand(amount: Int): BlockRegion {
        return BlockRegion(
            BlockPosition(min.x - amount, min.y - amount, min.z - amount, min.world),
            BlockPosition(max.x + amount, max.y + amount, max.z + amount, max.world)
        )
    }

    /**
     * 收缩区域
     */
    fun contract(amount: Int): BlockRegion {
        return expand(-amount)
    }

    /**
     * 获取区域的所有方块位置
     */
    fun getBlockPositions(): List<BlockPosition> {
        val positions = mutableListOf<BlockPosition>()
        
        for (x in min.x..max.x) {
            for (y in min.y..max.y) {
                for (z in min.z..max.z) {
                    positions.add(BlockPosition(x, y, z, min.world))
                }
            }
        }
        return positions
    }

    /**
     * 转换为精确区域
     */
    fun toRegion(): Region {
        return Region(min.toPosition(), max.toPositionCenter())
    }

    companion object {
        /**
         * 从两个方块位置创建区域
         */
        fun of(a: BlockPosition, b: BlockPosition): BlockRegion {
            require(a.world == b.world) { "两个位置必须在同一个世界中" }
            
            val minPos = BlockPosition(
                min(a.x, b.x),
                min(a.y, b.y),
                min(a.z, b.z),
                a.world
            )
            val maxPos = BlockPosition(
                max(a.x, b.x),
                max(a.y, b.y),
                max(a.z, b.z),
                a.world
            )
            
            return BlockRegion(minPos, maxPos)
        }
    }
}

/**
 * BlockRegion序列化器
 */
class BlockRegionSerializer : JsonSerializer<BlockRegion>() {
    override fun serialize(
        value: BlockRegion,
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
 * BlockRegion反序列化器
 */
class BlockRegionDeserializer : JsonDeserializer<BlockRegion>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext
    ): BlockRegion {
        val node = p.readValueAsTree<JsonNode>()
        
        val minNode = node.get("min")
            ?: throw JsonMappingException.from(p, "缺少必需字段: min")
        val maxNode = node.get("max")
            ?: throw JsonMappingException.from(p, "缺少必需字段: max")
            
        val min = p.codec.treeToValue(minNode, BlockPosition::class.java)
        val max = p.codec.treeToValue(maxNode, BlockPosition::class.java)
            
        return BlockRegion(min, max)
    }
}
