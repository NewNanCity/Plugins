@file:Suppress("unused")

package city.newnan.config.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.bukkit.util.Vector

/**
 * 二维向量（Double）
 *
 * @property x X坐标
 * @property y Y坐标
 * @author Gk0Wk
 * @since 1.0.0
 */
@JsonSerialize(using = Vector2dSerializer::class)
@JsonDeserialize(using = Vector2dDeserializer::class)
data class Vector2d(
    val x: Double,
    val y: Double
) {
    /**
     * 向量长度
     */
    val length: Double = kotlin.math.sqrt(x * x + y * y)

    /**
     * 向量长度的平方
     */
    val lengthSquared: Double = x * x + y * y

    /**
     * 单位向量
     */
    fun normalize(): Vector2d {
        val len = length
        return if (len == 0.0) Vector2d(0.0, 0.0) else Vector2d(x / len, y / len)
    }

    /**
     * 向量加法
     */
    operator fun plus(other: Vector2d): Vector2d = Vector2d(x + other.x, y + other.y)

    /**
     * 向量减法
     */
    operator fun minus(other: Vector2d): Vector2d = Vector2d(x - other.x, y - other.y)

    /**
     * 标量乘法
     */
    operator fun times(scalar: Double): Vector2d = Vector2d(x * scalar, y * scalar)

    /**
     * 标量除法
     */
    operator fun div(scalar: Double): Vector2d = Vector2d(x / scalar, y / scalar)

    /**
     * 点积
     */
    fun dot(other: Vector2d): Double = x * other.x + y * other.y

    companion object {
        val ZERO = Vector2d(0.0, 0.0)
        val ONE = Vector2d(1.0, 1.0)
        val UNIT_X = Vector2d(1.0, 0.0)
        val UNIT_Y = Vector2d(0.0, 1.0)
    }
}

/**
 * 二维向量（Float）
 *
 * @property x X坐标
 * @property y Y坐标
 * @author Gk0Wk
 * @since 1.0.0
 */
@JsonSerialize(using = Vector2fSerializer::class)
@JsonDeserialize(using = Vector2fDeserializer::class)
data class Vector2f(
    val x: Float,
    val y: Float
) {
    /**
     * 向量长度
     */
    val length: Float = kotlin.math.sqrt(x * x + y * y)

    /**
     * 向量长度的平方
     */
    val lengthSquared: Float = x * x + y * y

    /**
     * 单位向量
     */
    fun normalize(): Vector2f {
        val len = length
        return if (len == 0.0f) Vector2f(0.0f, 0.0f) else Vector2f(x / len, y / len)
    }

    /**
     * 向量加法
     */
    operator fun plus(other: Vector2f): Vector2f = Vector2f(x + other.x, y + other.y)

    /**
     * 向量减法
     */
    operator fun minus(other: Vector2f): Vector2f = Vector2f(x - other.x, y - other.y)

    /**
     * 标量乘法
     */
    operator fun times(scalar: Float): Vector2f = Vector2f(x * scalar, y * scalar)

    /**
     * 标量除法
     */
    operator fun div(scalar: Float): Vector2f = Vector2f(x / scalar, y / scalar)

    /**
     * 点积
     */
    fun dot(other: Vector2f): Float = x * other.x + y * other.y

    /**
     * 转换为Double向量
     */
    fun toVector2d(): Vector2d = Vector2d(x.toDouble(), y.toDouble())

    companion object {
        val ZERO = Vector2f(0.0f, 0.0f)
        val ONE = Vector2f(1.0f, 1.0f)
        val UNIT_X = Vector2f(1.0f, 0.0f)
        val UNIT_Y = Vector2f(0.0f, 1.0f)
    }
}

/**
 * 三维向量（Double）
 *
 * @property x X坐标
 * @property y Y坐标
 * @property z Z坐标
 * @author Gk0Wk
 * @since 1.0.0
 */
@JsonSerialize(using = Vector3dSerializer::class)
@JsonDeserialize(using = Vector3dDeserializer::class)
data class Vector3d(
    val x: Double,
    val y: Double,
    val z: Double
) {
    /**
     * 向量长度
     */
    val length: Double = kotlin.math.sqrt(x * x + y * y + z * z)

    /**
     * 向量长度的平方
     */
    val lengthSquared: Double = x * x + y * y + z * z

    /**
     * 单位向量
     */
    fun normalize(): Vector3d {
        val len = length
        return if (len == 0.0) Vector3d(0.0, 0.0, 0.0) else Vector3d(x / len, y / len, z / len)
    }

    /**
     * 向量加法
     */
    operator fun plus(other: Vector3d): Vector3d = Vector3d(x + other.x, y + other.y, z + other.z)

    /**
     * 向量减法
     */
    operator fun minus(other: Vector3d): Vector3d = Vector3d(x - other.x, y - other.y, z - other.z)

    /**
     * 标量乘法
     */
    operator fun times(scalar: Double): Vector3d = Vector3d(x * scalar, y * scalar, z * scalar)

    /**
     * 标量除法
     */
    operator fun div(scalar: Double): Vector3d = Vector3d(x / scalar, y / scalar, z / scalar)

    /**
     * 点积
     */
    fun dot(other: Vector3d): Double = x * other.x + y * other.y + z * other.z

    /**
     * 叉积
     */
    fun cross(other: Vector3d): Vector3d = Vector3d(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    /**
     * 转换为Bukkit Vector
     */
    fun toBukkitVector(): Vector = Vector(x, y, z)

    companion object {
        val ZERO = Vector3d(0.0, 0.0, 0.0)
        val ONE = Vector3d(1.0, 1.0, 1.0)
        val UNIT_X = Vector3d(1.0, 0.0, 0.0)
        val UNIT_Y = Vector3d(0.0, 1.0, 0.0)
        val UNIT_Z = Vector3d(0.0, 0.0, 1.0)

        /**
         * 从Bukkit Vector创建
         */
        fun from(vector: Vector): Vector3d = Vector3d(vector.x, vector.y, vector.z)
    }
}

/**
 * 三维向量（Float）
 *
 * @property x X坐标
 * @property y Y坐标
 * @property z Z坐标
 * @author Gk0Wk
 * @since 1.0.0
 */
@JsonSerialize(using = Vector3fSerializer::class)
@JsonDeserialize(using = Vector3fDeserializer::class)
data class Vector3f(
    val x: Float,
    val y: Float,
    val z: Float
) {
    /**
     * 向量长度
     */
    val length: Float = kotlin.math.sqrt(x * x + y * y + z * z)

    /**
     * 向量长度的平方
     */
    val lengthSquared: Float = x * x + y * y + z * z

    /**
     * 单位向量
     */
    fun normalize(): Vector3f {
        val len = length
        return if (len == 0.0f) Vector3f(0.0f, 0.0f, 0.0f) else Vector3f(x / len, y / len, z / len)
    }

    /**
     * 向量加法
     */
    operator fun plus(other: Vector3f): Vector3f = Vector3f(x + other.x, y + other.y, z + other.z)

    /**
     * 向量减法
     */
    operator fun minus(other: Vector3f): Vector3f = Vector3f(x - other.x, y - other.y, z - other.z)

    /**
     * 标量乘法
     */
    operator fun times(scalar: Float): Vector3f = Vector3f(x * scalar, y * scalar, z * scalar)

    /**
     * 标量除法
     */
    operator fun div(scalar: Float): Vector3f = Vector3f(x / scalar, y / scalar, z / scalar)

    /**
     * 点积
     */
    fun dot(other: Vector3f): Float = x * other.x + y * other.y + z * other.z

    /**
     * 叉积
     */
    fun cross(other: Vector3f): Vector3f = Vector3f(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    /**
     * 转换为Double向量
     */
    fun toVector3d(): Vector3d = Vector3d(x.toDouble(), y.toDouble(), z.toDouble())

    /**
     * 转换为Bukkit Vector
     */
    fun toBukkitVector(): Vector = Vector(x.toDouble(), y.toDouble(), z.toDouble())

    companion object {
        val ZERO = Vector3f(0.0f, 0.0f, 0.0f)
        val ONE = Vector3f(1.0f, 1.0f, 1.0f)
        val UNIT_X = Vector3f(1.0f, 0.0f, 0.0f)
        val UNIT_Y = Vector3f(0.0f, 1.0f, 0.0f)
        val UNIT_Z = Vector3f(0.0f, 0.0f, 1.0f)

        /**
         * 从Bukkit Vector创建
         */
        fun from(vector: Vector): Vector3f = Vector3f(vector.x.toFloat(), vector.y.toFloat(), vector.z.toFloat())
    }
}

// 序列化器实现

class Vector2dSerializer : JsonSerializer<Vector2d>() {
    override fun serialize(value: Vector2d, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeNumberField("x", value.x)
        gen.writeNumberField("y", value.y)
        gen.writeEndObject()
    }
}

class Vector2dDeserializer : JsonDeserializer<Vector2d>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Vector2d {
        val node = p.readValueAsTree<JsonNode>()
        val x = node.get("x")?.asDouble() ?: throw JsonMappingException.from(p, "缺少必需字段: x")
        val y = node.get("y")?.asDouble() ?: throw JsonMappingException.from(p, "缺少必需字段: y")
        return Vector2d(x, y)
    }
}

class Vector2fSerializer : JsonSerializer<Vector2f>() {
    override fun serialize(value: Vector2f, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeNumberField("x", value.x)
        gen.writeNumberField("y", value.y)
        gen.writeEndObject()
    }
}

class Vector2fDeserializer : JsonDeserializer<Vector2f>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Vector2f {
        val node = p.readValueAsTree<JsonNode>()
        val x = node.get("x")?.asDouble()?.toFloat() ?: throw JsonMappingException.from(p, "缺少必需字段: x")
        val y = node.get("y")?.asDouble()?.toFloat() ?: throw JsonMappingException.from(p, "缺少必需字段: y")
        return Vector2f(x, y)
    }
}

class Vector3dSerializer : JsonSerializer<Vector3d>() {
    override fun serialize(value: Vector3d, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeNumberField("x", value.x)
        gen.writeNumberField("y", value.y)
        gen.writeNumberField("z", value.z)
        gen.writeEndObject()
    }
}

class Vector3dDeserializer : JsonDeserializer<Vector3d>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Vector3d {
        val node = p.readValueAsTree<JsonNode>()
        val x = node.get("x")?.asDouble() ?: throw JsonMappingException.from(p, "缺少必需字段: x")
        val y = node.get("y")?.asDouble() ?: throw JsonMappingException.from(p, "缺少必需字段: y")
        val z = node.get("z")?.asDouble() ?: throw JsonMappingException.from(p, "缺少必需字段: z")
        return Vector3d(x, y, z)
    }
}

class Vector3fSerializer : JsonSerializer<Vector3f>() {
    override fun serialize(value: Vector3f, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeNumberField("x", value.x)
        gen.writeNumberField("y", value.y)
        gen.writeNumberField("z", value.z)
        gen.writeEndObject()
    }
}

class Vector3fDeserializer : JsonDeserializer<Vector3f>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Vector3f {
        val node = p.readValueAsTree<JsonNode>()
        val x = node.get("x")?.asDouble()?.toFloat() ?: throw JsonMappingException.from(p, "缺少必需字段: x")
        val y = node.get("y")?.asDouble()?.toFloat() ?: throw JsonMappingException.from(p, "缺少必需字段: y")
        val z = node.get("z")?.asDouble()?.toFloat() ?: throw JsonMappingException.from(p, "缺少必需字段: z")
        return Vector3f(x, y, z)
    }
}

/**
 * 向量点（位置+方向的向量表示）
 *
 * @property position 三维位置向量
 * @property direction 方向向量（yaw, pitch）
 * @author Gk0Wk
 * @since 1.0.0
 */
@JsonSerialize(using = VectorPointSerializer::class)
@JsonDeserialize(using = VectorPointDeserializer::class)
data class VectorPoint(
    val position: Vector3d,
    val direction: Vector2f
) {
    /**
     * 转换为Point对象
     */
    fun toPoint(world: String): Point {
        val pos = Position(position.x, position.y, position.z, world)
        val dir = Direction(direction.x, direction.y)
        return Point(pos, dir)
    }

    /**
     * 转换为Position对象
     */
    fun toPosition(world: String): Position {
        return Position(position.x, position.y, position.z, world)
    }

    companion object {
        /**
         * 从Point创建VectorPoint
         */
        fun of(point: Point): VectorPoint {
            val position = Vector3d(point.position.x, point.position.y, point.position.z)
            val direction = Vector2f(point.direction.yaw, point.direction.pitch)
            return VectorPoint(position, direction)
        }

        /**
         * 从位置和方向创建VectorPoint
         */
        fun of(position: Vector3d, direction: Vector2f): VectorPoint {
            return VectorPoint(position, direction)
        }
    }
}

class VectorPointSerializer : JsonSerializer<VectorPoint>() {
    override fun serialize(value: VectorPoint, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeObjectField("position", value.position)
        gen.writeObjectField("direction", value.direction)
        gen.writeEndObject()
    }
}

class VectorPointDeserializer : JsonDeserializer<VectorPoint>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): VectorPoint {
        val node = p.readValueAsTree<JsonNode>()

        val positionNode = node.get("position")
            ?: throw JsonMappingException.from(p, "缺少必需字段: position")
        val directionNode = node.get("direction")
            ?: throw JsonMappingException.from(p, "缺少必需字段: direction")

        val position = p.codec.treeToValue(positionNode, Vector3d::class.java)
        val direction = p.codec.treeToValue(directionNode, Vector2f::class.java)

        return VectorPoint(position, direction)
    }
}
