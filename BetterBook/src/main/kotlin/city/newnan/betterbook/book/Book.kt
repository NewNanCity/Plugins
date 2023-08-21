package city.newnan.betterbook.book

import city.newnan.betterbook.PluginMain
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.DateDeserializers
import com.fasterxml.jackson.databind.ser.std.DateSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Book(
    val title: String,
    val creator: UUID,
    val modifier: UUID = creator,
    @JsonSerialize(using = DateSerializer::class)
    @JsonDeserialize(using = DateDeserializers.TimestampDeserializer::class)
    val created: Date,
    @JsonSerialize(using = DateSerializer::class)
    @JsonDeserialize(using = DateDeserializers.TimestampDeserializer::class)
    val modified: Date = created,
    val pages: List<String> = emptyList()
) {
    fun readBook(player: Player) {
        val bookItem = ItemStack(Material.WRITTEN_BOOK)
        bookItem.itemMeta = (bookItem.itemMeta as BookMeta?)?.also {
            it.title = title
            it.author = Bukkit.getOfflinePlayer(creator).name ?: "未知"
            it.generation = BookMeta.Generation.COPY_OF_COPY
            it.pages = pages
        }
        player.openBook(bookItem)
    }
}

private val dateFormatter = SimpleDateFormat("yyyy年M月d日")
internal val bookIdNbtKey = NamespacedKey(PluginMain.INSTANCE, "book-uuid")

class UUIDDataType : PersistentDataType<ByteArray, UUID> {
    override fun toPrimitive(complex: UUID, context: PersistentDataAdapterContext): ByteArray {
        val bb: ByteBuffer = ByteBuffer.wrap(ByteArray(16))
        bb.putLong(complex.mostSignificantBits)
        bb.putLong(complex.leastSignificantBits)
        return bb.array()
    }

    override fun fromPrimitive(primitive: ByteArray, context: PersistentDataAdapterContext): UUID {
        val bb: ByteBuffer = ByteBuffer.wrap(primitive)
        val firstLong: Long = bb.long
        val secondLong: Long = bb.long
        return UUID(firstLong, secondLong)
    }

    override fun getPrimitiveType(): Class<ByteArray> = ByteArray::class.java

    override fun getComplexType(): Class<UUID> = UUID::class.java
}

internal fun ItemStack.findBookUUID(written: Boolean = true, writable: Boolean = true): UUID? {
    if (!(written && type == Material.WRITTEN_BOOK) && !(writable && type == Material.WRITABLE_BOOK)) return null
    return itemMeta?.persistentDataContainer?.get(bookIdNbtKey, UUIDDataType())
}

internal fun BookMeta.applyBook(book: Book, bookId: UUID, toWrittenBook: Boolean = true, addModifyInfo: Boolean = true): BookMeta {
    persistentDataContainer.set(bookIdNbtKey, UUIDDataType(), bookId)
    title = book.title
    author = Bukkit.getOfflinePlayer(book.creator).name ?: "秩名"
    generation = BookMeta.Generation.COPY_OF_COPY
    setDisplayName(book.title)
    val info = mutableListOf<String>()
    if (toWrittenBook) {
        info.add("§r§7${dateFormatter.format(book.created)} 初版§r")
    } else {
        info.add("§r§6作者    §r§7${Bukkit.getOfflinePlayer(book.creator).name ?: "秩名"}§r")
        info.add("§r§6创建时间 §r§7${dateFormatter.format(book.created)}§r")
        pages = book.pages.toList()
    }
    if (addModifyInfo) {
        info.add("§r§6最后修改 §r§7${Bukkit.getOfflinePlayer(book.modifier).name ?: "秩名"}§r")
        info.add("§r§6修改时间 §r§7${dateFormatter.format(book.modified)}§r")
    }
    info.add("§r§8§l[牛腩书局出版社]§r")
    lore = info
    return this
}