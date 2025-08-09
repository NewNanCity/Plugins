@file:Suppress("unused")

package city.newnan.config.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World

/**
 * 不可变的可序列化区块位置对象
 *
 * 表示一个区块的位置坐标
 *
 * @property x 区块X坐标
 * @property z 区块Z坐标
 * @property world 世界名称
 * @author Gk0Wk
 * @since 1.0.0
 */
@JsonSerialize(using = ChunkPositionSerializer::class)
@JsonDeserialize(using = ChunkPositionDeserializer::class)
data class ChunkPosition(
    val x: Int,
    val z: Int,
    val world: String
) {

    /**
     * 获取区块对象
     */
    fun toChunk(): Chunk {
        val bukkitWorld = Bukkit.getWorld(world)
            ?: throw IllegalStateException("世界 '$world' 不存在")
        return bukkitWorld.getChunkAt(x, z)
    }

    /**
     * 转换为区块中心的方块位置
     */
    fun toBlockPosition(): BlockPosition {
        return BlockPosition((x shl 4) + 8, 64, (z shl 4) + 8, world)
    }

    /**
     * 转换为区块中心的位置
     */
    fun toPosition(): Position {
        return Position((x shl 4) + 8.0, 64.0, (z shl 4) + 8.0, world)
    }

    /**
     * 添加坐标偏移
     */
    fun add(dx: Int, dz: Int): ChunkPosition {
        return ChunkPosition(x + dx, z + dz, world)
    }

    /**
     * 减去坐标偏移
     */
    fun subtract(dx: Int, dz: Int): ChunkPosition {
        return add(-dx, -dz)
    }

    /**
     * 与另一个区块位置创建区块区域
     */
    fun regionWith(other: ChunkPosition): ChunkRegion {
        return ChunkRegion.of(this, other)
    }

    /**
     * 编码为长整型（用于高效存储）
     */
    fun asEncodedLong(): Long {
        return x.toLong() and 0xffffffffL or ((z.toLong() and 0xffffffffL) shl 32)
    }

    companion object {
        /**
         * 从Bukkit Chunk创建ChunkPosition
         */
        fun of(chunk: Chunk): ChunkPosition {
            return ChunkPosition(chunk.x, chunk.z, chunk.world.name)
        }

        /**
         * 从方块坐标创建ChunkPosition
         */
        fun fromBlockCoords(blockX: Int, blockZ: Int, world: String): ChunkPosition {
            return ChunkPosition(blockX shr 4, blockZ shr 4, world)
        }

        /**
         * 从方块坐标创建ChunkPosition
         */
        fun fromBlockCoords(blockX: Int, blockZ: Int, world: World): ChunkPosition {
            return ChunkPosition(blockX shr 4, blockZ shr 4, world.name)
        }

        /**
         * 创建ChunkPosition
         */
        fun of(x: Int, z: Int, world: String): ChunkPosition {
            return ChunkPosition(x, z, world)
        }

        /**
         * 创建ChunkPosition
         */
        fun of(x: Int, z: Int, world: World): ChunkPosition {
            return ChunkPosition(x, z, world.name)
        }

        /**
         * 从编码的长整型解码ChunkPosition
         */
        fun fromEncodedLong(encoded: Long, world: String): ChunkPosition {
            val x = (encoded and 0xffffffffL).toInt()
            val z = (encoded shr 32).toInt()
            return ChunkPosition(x, z, world)
        }
    }
}

/**
 * ChunkPosition序列化器
 */
class ChunkPositionSerializer : JsonSerializer<ChunkPosition>() {
    override fun serialize(
        value: ChunkPosition,
        gen: JsonGenerator,
        serializers: SerializerProvider
    ) {
        gen.writeStartObject()
        gen.writeNumberField("x", value.x)
        gen.writeNumberField("z", value.z)
        gen.writeStringField("world", value.world)
        gen.writeEndObject()
    }
}

/**
 * ChunkPosition反序列化器
 */
class ChunkPositionDeserializer : JsonDeserializer<ChunkPosition>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext
    ): ChunkPosition {
        val node = p.readValueAsTree<JsonNode>()

        val x = node.get("x")?.asInt()
            ?: throw JsonMappingException.from(p, "缺少必需字段: x")
        val z = node.get("z")?.asInt()
            ?: throw JsonMappingException.from(p, "缺少必需字段: z")
        val world = node.get("world")?.asText()
            ?: throw JsonMappingException.from(p, "缺少必需字段: world")

        return ChunkPosition(x, z, world)
    }
}

/**
 * 不可变的可序列化方向对象
 *
 * 表示一个三维空间中的方向（偏航角和俯仰角）
 *
 * @property yaw 偏航角（水平旋转）
 * @property pitch 俯仰角（垂直旋转）
 * @author Gk0Wk
 * @since 1.0.0
 */
@JsonSerialize(using = DirectionSerializer::class)
@JsonDeserialize(using = DirectionDeserializer::class)
data class Direction(
    val yaw: Float,
    val pitch: Float
) {

    /**
     * 应用到Location对象
     */
    fun applyTo(location: Location): Location {
        val newLocation = location.clone()
        newLocation.yaw = yaw
        newLocation.pitch = pitch
        return newLocation
    }

    companion object {
        /**
         * 零方向常量
         */
        val ZERO = Direction(0.0f, 0.0f)

        /**
         * 从Bukkit Location创建Direction
         */
        fun from(location: Location): Direction {
            return Direction(location.yaw, location.pitch)
        }

        /**
         * 创建Direction
         */
        fun of(yaw: Float, pitch: Float): Direction {
            return Direction(yaw, pitch)
        }
    }
}

/**
 * Direction序列化器
 */
class DirectionSerializer : JsonSerializer<Direction>() {
    override fun serialize(
        value: Direction,
        gen: JsonGenerator,
        serializers: SerializerProvider
    ) {
        gen.writeStartObject()
        gen.writeNumberField("yaw", value.yaw)
        gen.writeNumberField("pitch", value.pitch)
        gen.writeEndObject()
    }
}

/**
 * Direction反序列化器
 */
class DirectionDeserializer : JsonDeserializer<Direction>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext
    ): Direction {
        val node = p.readValueAsTree<JsonNode>()

        val yaw = node.get("yaw")?.asDouble()?.toFloat()
            ?: throw JsonMappingException.from(p, "缺少必需字段: yaw")
        val pitch = node.get("pitch")?.asDouble()?.toFloat()
            ?: throw JsonMappingException.from(p, "缺少必需字段: pitch")

        return Direction(yaw, pitch)
    }
}

/**
 * 不可变的可序列化点对象
 *
 * 表示一个位置和方向的组合
 *
 * @property position 位置
 * @property direction 方向
 * @author Gk0Wk
 * @since 1.0.0
 */
@JsonSerialize(using = PointSerializer::class)
@JsonDeserialize(using = PointDeserializer::class)
data class Point(
    val position: Position,
    val direction: Direction
) {
    @Transient
    private var bukkitLocation: Location? = null

    /**
     * 转换为Bukkit Location对象
     */
    fun toLocation(): Location {
        if (bukkitLocation == null) {
            val bukkitWorld = Bukkit.getWorld(position.world)
                ?: throw IllegalStateException("世界 '${position.world}' 不存在")
            bukkitLocation = Location(
                bukkitWorld,
                position.x,
                position.y,
                position.z,
                direction.yaw,
                direction.pitch
            )
        }
        return bukkitLocation!!.clone()
    }

    /**
     * 转换为VectorPoint对象
     */
    fun toVectorPoint(): VectorPoint {
        return VectorPoint.of(this)
    }

    companion object {
        /**
         * 从Bukkit Location创建Point
         */
        fun of(location: Location): Point {
            return Point(Position.of(location), Direction.from(location))
        }

        /**
         * 创建Point
         */
        fun of(position: Position, direction: Direction): Point {
            return Point(position, direction)
        }
    }
}

/**
 * Point序列化器
 */
class PointSerializer : JsonSerializer<Point>() {
    override fun serialize(
        value: Point,
        gen: JsonGenerator,
        serializers: SerializerProvider
    ) {
        gen.writeStartObject()
        gen.writeObjectField("position", value.position)
        gen.writeObjectField("direction", value.direction)
        gen.writeEndObject()
    }
}

/**
 * Point反序列化器
 */
class PointDeserializer : JsonDeserializer<Point>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext
    ): Point {
        val node = p.readValueAsTree<JsonNode>()

        val positionNode = node.get("position")
            ?: throw JsonMappingException.from(p, "缺少必需字段: position")
        val directionNode = node.get("direction")
            ?: throw JsonMappingException.from(p, "缺少必需字段: direction")

        val position = p.codec.treeToValue(positionNode, Position::class.java)
        val direction = p.codec.treeToValue(directionNode, Direction::class.java)

        return Point(position, direction)
    }
}
