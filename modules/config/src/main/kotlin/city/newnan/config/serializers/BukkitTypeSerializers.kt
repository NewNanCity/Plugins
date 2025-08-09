@file:Suppress("unused")

package city.newnan.config.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import org.bukkit.configuration.serialization.ConfigurationSerializable
import org.bukkit.configuration.serialization.ConfigurationSerialization
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Inventory
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import kotlin.math.floor

/**
 * Bukkit 类型序列化器模块
 *
 * 提供对 Bukkit 原生类型（如 Location、ItemStack、Material 等）的 Jackson 序列化支持
 * 以及扩展的位置、区域、向量等类型的序列化支持
 *
 * @author Gk0Wk
 * @since 1.0.0
 */
class BukkitTypeSerializersModule : SimpleModule("BukkitTypeSerializers") {

    init {
        // 注册 ConfigurationSerializable 序列化器
        addSerializer(ConfigurationSerializable::class.java, ConfigurationSerializableSerializer())
        addDeserializer(ConfigurationSerializable::class.java, ConfigurationSerializableDeserializer())

        // 注册位置相关序列化器
        addSerializer(Position::class.java, PositionSerializer())
        addDeserializer(Position::class.java, PositionDeserializer())
        addSerializer(BlockPosition::class.java, BlockPositionSerializer())
        addDeserializer(BlockPosition::class.java, BlockPositionDeserializer())
        addSerializer(ChunkPosition::class.java, ChunkPositionSerializer())
        addDeserializer(ChunkPosition::class.java, ChunkPositionDeserializer())

        // 注册方向和点序列化器
        addSerializer(Direction::class.java, DirectionSerializer())
        addDeserializer(Direction::class.java, DirectionDeserializer())
        addSerializer(Point::class.java, PointSerializer())
        addDeserializer(Point::class.java, PointDeserializer())

        // 注册区域序列化器
        addSerializer(Region::class.java, RegionSerializer())
        addDeserializer(Region::class.java, RegionDeserializer())
        addSerializer(BlockRegion::class.java, BlockRegionSerializer())
        addDeserializer(BlockRegion::class.java, BlockRegionDeserializer())
        addSerializer(ChunkRegion::class.java, ChunkRegionSerializer())
        addDeserializer(ChunkRegion::class.java, ChunkRegionDeserializer())
        addSerializer(CircularRegion::class.java, CircularRegionSerializer())
        addDeserializer(CircularRegion::class.java, CircularRegionDeserializer())

        // 注册物品栏序列化器
        addSerializer(ItemStack::class.java, ItemStackSerializer())
        addDeserializer(ItemStack::class.java, ItemStackDeserializer())
        addSerializer(Array<ItemStack?>::class.java, ItemStackArraySerializer())
        addDeserializer(Array<ItemStack?>::class.java, ItemStackArrayDeserializer())
        addSerializer(SerializableInventory::class.java, SerializableInventorySerializer())
        addDeserializer(SerializableInventory::class.java, SerializableInventoryDeserializer())

        // 注册向量序列化器
        addSerializer(Vector2d::class.java, Vector2dSerializer())
        addDeserializer(Vector2d::class.java, Vector2dDeserializer())
        addSerializer(Vector2f::class.java, Vector2fSerializer())
        addDeserializer(Vector2f::class.java, Vector2fDeserializer())
        addSerializer(Vector3d::class.java, Vector3dSerializer())
        addDeserializer(Vector3d::class.java, Vector3dDeserializer())
        addSerializer(Vector3f::class.java, Vector3fSerializer())
        addDeserializer(Vector3f::class.java, Vector3fDeserializer())
        addSerializer(VectorPoint::class.java, VectorPointSerializer())
        addDeserializer(VectorPoint::class.java, VectorPointDeserializer())
    }
}

/**
 * ConfigurationSerializable 序列化器
 *
 * 将实现了 ConfigurationSerializable 接口的 Bukkit 对象序列化为 JSON
 */
class ConfigurationSerializableSerializer : JsonSerializer<ConfigurationSerializable>() {

    override fun serialize(
        value: ConfigurationSerializable,
        gen: JsonGenerator,
        serializers: SerializerProvider
    ) {
        val serialized = value.serialize()
        val map = mutableMapOf<String, Any?>()

        // 添加类型标识
        map[ConfigurationSerialization.SERIALIZED_TYPE_KEY] =
            ConfigurationSerialization.getAlias(value.javaClass)

        // 添加序列化数据
        map.putAll(serialized)

        // 序列化为 JSON
        gen.writeObject(map)
    }
}

/**
 * ConfigurationSerializable 反序列化器
 *
 * 从 JSON 反序列化为实现了 ConfigurationSerializable 接口的 Bukkit 对象
 */
class ConfigurationSerializableDeserializer : JsonDeserializer<ConfigurationSerializable>() {

    private val mapTypeRef = object : TypeReference<MutableMap<String, Any?>>() {}

    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext
    ): ConfigurationSerializable {
        val map: MutableMap<String, Any?> = p.readValueAs(mapTypeRef)

        // 递归处理嵌套的 ConfigurationSerializable 对象
        deserializeChildren(map)

        return ConfigurationSerialization.deserializeObject(map)
            ?: throw JsonMappingException.from(p, "无法反序列化 ConfigurationSerializable 对象")
    }

    /**
     * 递归处理嵌套的可序列化对象
     *
     * @param map 要处理的映射
     */
    private fun deserializeChildren(map: MutableMap<String, Any?>) {
        for ((key, value) in map) {
            when (value) {
                is Map<*, *> -> {
                    try {
                        @Suppress("UNCHECKED_CAST")
                        val valueMap = value as MutableMap<String, Any?>

                        // 递归处理子映射
                        deserializeChildren(valueMap)

                        // 如果包含序列化标识，则反序列化
                        if (valueMap.containsKey(ConfigurationSerialization.SERIALIZED_TYPE_KEY)) {
                            val deserialized = ConfigurationSerialization.deserializeObject(valueMap)
                            if (deserialized != null) {
                                map[key] = deserialized
                            } else {
                                // 记录反序列化失败的情况
                                val typeName = valueMap[ConfigurationSerialization.SERIALIZED_TYPE_KEY]
                                System.err.println("Warning: Failed to deserialize object for key '$key': $typeName")
                            }
                        }
                    } catch (e: Exception) {
                        // 记录详细的异常信息，而不是静默忽略
                        System.err.println("Warning: Failed to process nested object for key '$key': ${e.message}")
                        // 在开发环境下可以打印堆栈跟踪
                        if (System.getProperty("config.debug") == "true") {
                            e.printStackTrace()
                        }
                    }
                }
                is Number -> {
                    try {
                        // 处理数字类型，确保正确的类型转换
                        val doubleVal = value.toDouble()
                        
                        // 使用更精确的整数检查
                        val intVal = doubleVal.toInt()
                        val longVal = doubleVal.toLong()

                        map[key] = when {
                            // 检查是否为整数且在int范围内
                            doubleVal.isFinite() && doubleVal == intVal.toDouble() && 
                            intVal.toLong() == longVal -> intVal
                            // 检查是否为长整数
                            doubleVal.isFinite() && doubleVal == longVal.toDouble() -> longVal
                            // 保持为双精度浮点数
                            else -> doubleVal
                        }
                    } catch (e: NumberFormatException) {
                        System.err.println("Warning: Failed to convert number for key '$key': ${e.message}")
                        // 保持原值
                    }
                }
            }
        }
    }
}

/**
 * 扩展函数：为 ObjectMapper 注册 Bukkit 类型序列化器
 *
 * @return 配置后的 ObjectMapper
 */
fun ObjectMapper.registerBukkitSerializers(): ObjectMapper {
    return this.registerModule(BukkitTypeSerializersModule())
}
