@file:Suppress("unused")

package city.newnan.config.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.BlockFace
import kotlin.math.floor

/**
 * 不可变的可序列化位置对象
 *
 * 表示一个三维空间中的精确位置，包含世界信息
 *
 * @property x X坐标
 * @property y Y坐标  
 * @property z Z坐标
 * @property world 世界名称
 * @author Gk0Wk
 * @since 1.0.0
 */
@JsonSerialize(using = PositionSerializer::class)
@JsonDeserialize(using = PositionDeserializer::class)
data class Position(
    val x: Double,
    val y: Double,
    val z: Double,
    val world: String
) {
    @Transient
    private var bukkitLocation: Location? = null

    /**
     * 转换为Bukkit Location对象
     */
    fun toLocation(): Location {
        if (bukkitLocation == null) {
            val bukkitWorld = Bukkit.getWorld(world)
                ?: throw IllegalStateException("世界 '$world' 不存在")
            bukkitLocation = Location(bukkitWorld, x, y, z)
        }
        return bukkitLocation!!.clone()
    }

    /**
     * 获取方块位置（向下取整）
     */
    fun floor(): BlockPosition {
        return BlockPosition(
            floor(x).toInt(),
            floor(y).toInt(), 
            floor(z).toInt(),
            world
        )
    }

    /**
     * 获取相对位置
     */
    fun getRelative(face: BlockFace): Position {
        return Position(
            x + face.modX,
            y + face.modY,
            z + face.modZ,
            world
        )
    }

    /**
     * 添加坐标偏移
     */
    fun add(dx: Double, dy: Double, dz: Double): Position {
        return Position(x + dx, y + dy, z + dz, world)
    }

    /**
     * 减去坐标偏移
     */
    fun subtract(dx: Double, dy: Double, dz: Double): Position {
        return add(-dx, -dy, -dz)
    }

    /**
     * 与另一个位置创建区域
     */
    fun regionWith(other: Position): Region {
        return Region.of(this, other)
    }

    /**
     * 与方向组合创建点
     */
    fun withDirection(direction: Direction): Point {
        return Point(this, direction)
    }

    companion object {
        /**
         * 从Bukkit Location创建Position
         */
        fun of(location: Location): Position {
            return Position(
                location.x,
                location.y,
                location.z,
                location.world?.name ?: throw IllegalArgumentException("Location的世界不能为null")
            )
        }

        /**
         * 创建Position
         */
        fun of(x: Double, y: Double, z: Double, world: String): Position {
            return Position(x, y, z, world)
        }

        /**
         * 创建Position
         */
        fun of(x: Double, y: Double, z: Double, world: World): Position {
            return Position(x, y, z, world.name)
        }
    }
}

/**
 * Position序列化器
 */
class PositionSerializer : JsonSerializer<Position>() {
    override fun serialize(
        value: Position,
        gen: JsonGenerator,
        serializers: SerializerProvider
    ) {
        gen.writeStartObject()
        gen.writeNumberField("x", value.x)
        gen.writeNumberField("y", value.y)
        gen.writeNumberField("z", value.z)
        gen.writeStringField("world", value.world)
        gen.writeEndObject()
    }
}

/**
 * Position反序列化器
 */
class PositionDeserializer : JsonDeserializer<Position>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext
    ): Position {
        val node = p.readValueAsTree<JsonNode>()
        
        val x = node.get("x")?.asDouble() 
            ?: throw JsonMappingException.from(p, "缺少必需字段: x")
        val y = node.get("y")?.asDouble()
            ?: throw JsonMappingException.from(p, "缺少必需字段: y")
        val z = node.get("z")?.asDouble()
            ?: throw JsonMappingException.from(p, "缺少必需字段: z")
        val world = node.get("world")?.asText()
            ?: throw JsonMappingException.from(p, "缺少必需字段: world")
            
        return Position(x, y, z, world)
    }
}

/**
 * 不可变的可序列化方块位置对象
 *
 * 表示一个三维空间中的整数方块位置
 *
 * @property x X坐标（整数）
 * @property y Y坐标（整数）
 * @property z Z坐标（整数）
 * @property world 世界名称
 * @author Gk0Wk
 * @since 1.0.0
 */
@JsonSerialize(using = BlockPositionSerializer::class)
@JsonDeserialize(using = BlockPositionDeserializer::class)
data class BlockPosition(
    val x: Int,
    val y: Int,
    val z: Int,
    val world: String
) {
    @Transient
    private var bukkitLocation: Location? = null

    /**
     * 转换为Bukkit Location对象
     */
    fun toLocation(): Location {
        if (bukkitLocation == null) {
            val bukkitWorld = Bukkit.getWorld(world)
                ?: throw IllegalStateException("世界 '$world' 不存在")
            bukkitLocation = Location(bukkitWorld, x.toDouble(), y.toDouble(), z.toDouble())
        }
        return bukkitLocation!!.clone()
    }

    /**
     * 转换为精确位置
     */
    fun toPosition(): Position {
        return Position(x.toDouble(), y.toDouble(), z.toDouble(), world)
    }

    /**
     * 转换为方块中心位置
     */
    fun toPositionCenter(): Position {
        return Position(x + 0.5, y + 0.5, z + 0.5, world)
    }

    /**
     * 获取所在区块位置
     */
    fun toChunk(): ChunkPosition {
        return ChunkPosition(x shr 4, z shr 4, world)
    }

    /**
     * 添加坐标偏移
     */
    fun add(dx: Int, dy: Int, dz: Int): BlockPosition {
        return BlockPosition(x + dx, y + dy, z + dz, world)
    }

    /**
     * 减去坐标偏移
     */
    fun subtract(dx: Int, dy: Int, dz: Int): BlockPosition {
        return add(-dx, -dy, -dz)
    }

    /**
     * 与另一个方块位置创建方块区域
     */
    fun regionWith(other: BlockPosition): BlockRegion {
        return BlockRegion.of(this, other)
    }

    /**
     * 获取相对位置
     */
    fun getRelative(face: BlockFace): BlockPosition {
        return BlockPosition(x + face.modX, y + face.modY, z + face.modZ, world)
    }

    companion object {
        /**
         * 从Bukkit Location创建BlockPosition
         */
        fun of(location: Location): BlockPosition {
            return BlockPosition(
                floor(location.x).toInt(),
                floor(location.y).toInt(),
                floor(location.z).toInt(),
                location.world?.name ?: throw IllegalArgumentException("Location的世界不能为null")
            )
        }

        /**
         * 创建BlockPosition
         */
        fun of(x: Int, y: Int, z: Int, world: String): BlockPosition {
            return BlockPosition(x, y, z, world)
        }

        /**
         * 创建BlockPosition
         */
        fun of(x: Int, y: Int, z: Int, world: World): BlockPosition {
            return BlockPosition(x, y, z, world.name)
        }
    }
}

/**
 * BlockPosition序列化器
 */
class BlockPositionSerializer : JsonSerializer<BlockPosition>() {
    override fun serialize(
        value: BlockPosition,
        gen: JsonGenerator,
        serializers: SerializerProvider
    ) {
        gen.writeStartObject()
        gen.writeNumberField("x", value.x)
        gen.writeNumberField("y", value.y)
        gen.writeNumberField("z", value.z)
        gen.writeStringField("world", value.world)
        gen.writeEndObject()
    }
}

/**
 * BlockPosition反序列化器
 */
class BlockPositionDeserializer : JsonDeserializer<BlockPosition>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext
    ): BlockPosition {
        val node = p.readValueAsTree<JsonNode>()
        
        val x = node.get("x")?.asInt()
            ?: throw JsonMappingException.from(p, "缺少必需字段: x")
        val y = node.get("y")?.asInt()
            ?: throw JsonMappingException.from(p, "缺少必需字段: y")
        val z = node.get("z")?.asInt()
            ?: throw JsonMappingException.from(p, "缺少必需字段: z")
        val world = node.get("world")?.asText()
            ?: throw JsonMappingException.from(p, "缺少必需字段: world")
            
        return BlockPosition(x, y, z, world)
    }
}
