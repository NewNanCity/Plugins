@file:Suppress("unused")

package city.newnan.config.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.bukkit.Bukkit
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*

/**
 * 物品栏序列化工具类
 *
 * 提供物品堆栈和物品栏的序列化和反序列化功能
 *
 * @author Gk0Wk
 * @since 1.0.0
 */
object InventorySerializationUtils {

    /**
     * 将物品堆栈编码为字节数组
     */
    fun encodeItemStack(item: ItemStack): ByteArray {
        return try {
            ByteArrayOutputStream().use { outputStream ->
                BukkitObjectOutputStream(outputStream).use { dataOutput ->
                    dataOutput.writeObject(item)
                    outputStream.toByteArray()
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("编码物品堆栈失败", e)
        }
    }

    /**
     * 将物品堆栈编码为Base64字符串
     */
    fun encodeItemStackToString(item: ItemStack): String {
        return Base64.getEncoder().encodeToString(encodeItemStack(item))
    }

    /**
     * 从字节数组解码物品堆栈
     */
    fun decodeItemStack(data: ByteArray): ItemStack {
        return try {
            ByteArrayInputStream(data).use { inputStream ->
                BukkitObjectInputStream(inputStream).use { dataInput ->
                    dataInput.readObject() as ItemStack
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("解码物品堆栈失败", e)
        } catch (e: ClassNotFoundException) {
            throw RuntimeException("解码物品堆栈失败：找不到类", e)
        }
    }

    /**
     * 从Base64字符串解码物品堆栈
     */
    fun decodeItemStack(data: String): ItemStack {
        return decodeItemStack(Base64.getDecoder().decode(data))
    }

    /**
     * 将物品堆栈数组编码为字节数组
     */
    fun encodeItemStacks(items: Array<ItemStack?>): ByteArray {
        return try {
            ByteArrayOutputStream().use { outputStream ->
                BukkitObjectOutputStream(outputStream).use { dataOutput ->
                    dataOutput.writeInt(items.size)
                    for (item in items) {
                        dataOutput.writeObject(item)
                    }
                    outputStream.toByteArray()
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("编码物品堆栈数组失败", e)
        }
    }

    /**
     * 将物品堆栈数组编码为Base64字符串
     */
    fun encodeItemStacksToString(items: Array<ItemStack?>): String {
        return Base64.getEncoder().encodeToString(encodeItemStacks(items))
    }

    /**
     * 从字节数组解码物品堆栈数组
     */
    fun decodeItemStacks(data: ByteArray): Array<ItemStack?> {
        return try {
            ByteArrayInputStream(data).use { inputStream ->
                BukkitObjectInputStream(inputStream).use { dataInput ->
                    val length = dataInput.readInt()
                    Array(length) { dataInput.readObject() as ItemStack? }
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("解码物品堆栈数组失败", e)
        } catch (e: ClassNotFoundException) {
            throw RuntimeException("解码物品堆栈数组失败：找不到类", e)
        }
    }

    /**
     * 从Base64字符串解码物品堆栈数组
     */
    fun decodeItemStacks(data: String): Array<ItemStack?> {
        return decodeItemStacks(Base64.getDecoder().decode(data))
    }

    /**
     * 将物品栏编码为字节数组
     */
    fun encodeInventory(inventory: Inventory): ByteArray {
        return try {
            ByteArrayOutputStream().use { outputStream ->
                BukkitObjectOutputStream(outputStream).use { dataOutput ->
                    dataOutput.writeInt(inventory.size)
                    for (i in 0 until inventory.size) {
                        dataOutput.writeObject(inventory.getItem(i))
                    }
                    outputStream.toByteArray()
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("编码物品栏失败", e)
        }
    }

    /**
     * 将物品栏编码为Base64字符串
     */
    fun encodeInventoryToString(inventory: Inventory): String {
        return Base64.getEncoder().encodeToString(encodeInventory(inventory))
    }

    /**
     * 从字节数组解码物品栏
     */
    fun decodeInventory(data: ByteArray, title: String): Inventory {
        return try {
            ByteArrayInputStream(data).use { inputStream ->
                BukkitObjectInputStream(inputStream).use { dataInput ->
                    val size = dataInput.readInt()
                    val inventory = Bukkit.getServer().createInventory(null, size, net.kyori.adventure.text.Component.text(title))
                    for (i in 0 until size) {
                        inventory.setItem(i, dataInput.readObject() as ItemStack?)
                    }
                    inventory
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("解码物品栏失败", e)
        } catch (e: ClassNotFoundException) {
            throw RuntimeException("解码物品栏失败：找不到类", e)
        }
    }

    /**
     * 从Base64字符串解码物品栏
     */
    fun decodeInventory(data: String, title: String): Inventory {
        return decodeInventory(Base64.getDecoder().decode(data), title)
    }
}

/**
 * 物品堆栈序列化器
 *
 * 将ItemStack序列化为Base64字符串
 */
class ItemStackSerializer : JsonSerializer<ItemStack>() {
    override fun serialize(
        value: ItemStack,
        gen: JsonGenerator,
        serializers: SerializerProvider
    ) {
        val encoded = InventorySerializationUtils.encodeItemStackToString(value)
        gen.writeString(encoded)
    }
}

/**
 * 物品堆栈反序列化器
 *
 * 从Base64字符串反序列化为ItemStack
 */
class ItemStackDeserializer : JsonDeserializer<ItemStack>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext
    ): ItemStack {
        val data = p.valueAsString
            ?: throw JsonMappingException.from(p, "ItemStack数据不能为null")

        return try {
            InventorySerializationUtils.decodeItemStack(data)
        } catch (e: Exception) {
            throw JsonMappingException.from(p, "反序列化ItemStack失败", e)
        }
    }
}

/**
 * 物品堆栈数组序列化器
 *
 * 将ItemStack数组序列化为Base64字符串
 */
class ItemStackArraySerializer : JsonSerializer<Array<ItemStack?>>() {
    override fun serialize(
        value: Array<ItemStack?>,
        gen: JsonGenerator,
        serializers: SerializerProvider
    ) {
        val encoded = InventorySerializationUtils.encodeItemStacksToString(value)
        gen.writeString(encoded)
    }
}

/**
 * 物品堆栈数组反序列化器
 *
 * 从Base64字符串反序列化为ItemStack数组
 */
class ItemStackArrayDeserializer : JsonDeserializer<Array<ItemStack?>>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext
    ): Array<ItemStack?> {
        val data = p.valueAsString
            ?: throw JsonMappingException.from(p, "ItemStack数组数据不能为null")

        return try {
            InventorySerializationUtils.decodeItemStacks(data)
        } catch (e: Exception) {
            throw JsonMappingException.from(p, "反序列化ItemStack数组失败", e)
        }
    }
}

/**
 * 可序列化的物品栏包装器
 *
 * 包装Bukkit Inventory以支持序列化
 *
 * @property title 物品栏标题
 * @property data 序列化的物品栏数据
 * @author Gk0Wk
 * @since 1.0.0
 */
@JsonSerialize(using = SerializableInventorySerializer::class)
@JsonDeserialize(using = SerializableInventoryDeserializer::class)
data class SerializableInventory(
    val title: String,
    val data: String
) {
    /**
     * 转换为Bukkit Inventory对象
     */
    fun toInventory(): Inventory {
        return InventorySerializationUtils.decodeInventory(data, title)
    }

    companion object {
        /**
         * 从Bukkit Inventory创建可序列化包装器
         */
        fun of(inventory: Inventory): SerializableInventory {
            val title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(inventory.type.defaultTitle())
            val data = InventorySerializationUtils.encodeInventoryToString(inventory)
            return SerializableInventory(title, data)
        }

        /**
         * 从Bukkit Inventory创建可序列化包装器（自定义标题）
         */
        fun of(inventory: Inventory, title: String): SerializableInventory {
            val data = InventorySerializationUtils.encodeInventoryToString(inventory)
            return SerializableInventory(title, data)
        }
    }
}

/**
 * 可序列化物品栏序列化器
 */
class SerializableInventorySerializer : JsonSerializer<SerializableInventory>() {
    override fun serialize(
        value: SerializableInventory,
        gen: JsonGenerator,
        serializers: SerializerProvider
    ) {
        gen.writeStartObject()
        gen.writeStringField("title", value.title)
        gen.writeStringField("data", value.data)
        gen.writeEndObject()
    }
}

/**
 * 可序列化物品栏反序列化器
 */
class SerializableInventoryDeserializer : JsonDeserializer<SerializableInventory>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext
    ): SerializableInventory {
        val node = p.readValueAsTree<JsonNode>()

        val title = node.get("title")?.asText()
            ?: throw JsonMappingException.from(p, "缺少必需字段: title")
        val data = node.get("data")?.asText()
            ?: throw JsonMappingException.from(p, "缺少必需字段: data")

        return SerializableInventory(title, data)
    }
}
