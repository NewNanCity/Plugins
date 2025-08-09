package city.newnan.externalbook.book

import city.newnan.core.utils.text.ComponentParseMode
import city.newnan.core.utils.text.toComponent
import city.newnan.core.utils.text.toPlain
import city.newnan.externalbook.ExternalBookPlugin
import city.newnan.externalbook.i18n.LanguageKeys
import com.github.f4b6a3.ulid.Ulid
import net.kyori.adventure.text.Component
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

class BookAbstract(
    val id: Ulid,
    val title: String,
    val creator: UUID,
    val modifier: UUID = creator,
    val created: Date,
    val modified: Date = created,
    val preview: String = ""
)

data class Book(
    val id: Ulid,
    val title: String,
    val creator: UUID,
    val modifier: UUID = creator,
    val created: Date,
    val modified: Date = created,
    val pages: List<String> = emptyList()
)

/**
 * 为玩家打开书籍阅读界面
 */
fun Book.readBook(player: Player) {
    val bookItem = ItemStack(Material.WRITTEN_BOOK)
    bookItem.itemMeta = (bookItem.itemMeta as BookMeta?)?.also {
        it.title(Component.text(title))
        it.generation = BookMeta.Generation.COPY_OF_COPY
        // 转换字符串页面为Component
        it.pages(pages.map { page -> Component.text(page) })
    }
    player.openBook(bookItem)
}

/**
 * UUID持久化数据类型
 */
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

/**
 * ULID持久化数据类型
 */
class ULIDDataType : PersistentDataType<ByteArray, Ulid> {
    override fun toPrimitive(complex: Ulid, context: PersistentDataAdapterContext): ByteArray {
        return complex.toBytes()
    }

    override fun fromPrimitive(primitive: ByteArray, context: PersistentDataAdapterContext): Ulid {
        return Ulid.from(primitive)
    }

    override fun getPrimitiveType(): Class<ByteArray> = ByteArray::class.java
    override fun getComplexType(): Class<Ulid> = Ulid::class.java
}

/**
 * 默认日期格式化器
 */
val dateFormatter = SimpleDateFormat("yyyy/MM/dd")

/**
 * 书籍ID的NBT键
 */
lateinit var bookUuidNbtKey: NamespacedKey
    private set
lateinit var bookUlidNbtKey: NamespacedKey
    private set

/**
 * 初始化NBT键
 */
fun initializeBookNbtKey(plugin: org.bukkit.plugin.Plugin) {
    bookUuidNbtKey = NamespacedKey(plugin, "book-uuid")
    bookUlidNbtKey = NamespacedKey(plugin, "book-ulid")
}

/**
 * 从物品中查找书籍ID
 */
fun ItemStack.findBookID(written: Boolean = true, writable: Boolean = true): Pair<UUID?, Ulid?> {
    if (!(written && type == Material.WRITTEN_BOOK) && !(writable && type == Material.WRITABLE_BOOK)) return null to null
    return itemMeta?.persistentDataContainer?.get(bookUuidNbtKey, UUIDDataType()) to
            itemMeta?.persistentDataContainer?.get(bookUlidNbtKey, ULIDDataType())
}

/**
 * 从物品中移除旧的UUID并添加新的ULID
 */
fun ItemStack.updateBookID(oldUuid: UUID, newUlid: Ulid) {
    if (type != Material.WRITTEN_BOOK && type != Material.WRITABLE_BOOK) return
    if (itemMeta == null) return
    itemMeta.persistentDataContainer.remove(bookUuidNbtKey)
    itemMeta.persistentDataContainer.set(bookUlidNbtKey, ULIDDataType(), newUlid)
    itemMeta = itemMeta
}

/**
 * 将书籍信息应用到BookMeta
 */
fun BookMeta.applyBook(
    book: Book,
    toWrittenBook: Boolean = true,
    addModifyInfo: Boolean = true
): BookMeta {
    val creatorPlayer = Bukkit.getOfflinePlayer(book.creator)

    persistentDataContainer.set(bookUlidNbtKey, ULIDDataType(), book.id)
    title(book.title.toComponent(ComponentParseMode.Plain))
    val authorName = creatorPlayer.name?.toComponent(ComponentParseMode.Plain) ?: ExternalBookPlugin.instance.messager.sprintf(
        provider = true,
        parseMode = ComponentParseMode.Plain,
        formatText = LanguageKeys.Book.UNKNOWN_AUTHOR)
    author(authorName)
    generation = BookMeta.Generation.COPY_OF_COPY
    displayName(book.title.toComponent(ComponentParseMode.Plain))

    when {
        toWrittenBook && !addModifyInfo -> {
            lore(ExternalBookPlugin.instance.messager.sprintfPlain(
                LanguageKeys.Book.WRITTEN_BOOK_LORE,
                authorName.toPlain(),
                dateFormatter.format(book.created)
            ).split('\n').map { it.toComponent() })
        }
        toWrittenBook && addModifyInfo -> {
            lore(ExternalBookPlugin.instance.messager.sprintfPlain(
                LanguageKeys.Book.WRITTEN_BOOK_WITH_MODIFY_INFO,
                authorName.toPlain(),
                dateFormatter.format(book.created),
                Bukkit.getOfflinePlayer(book.modifier).name ?: ExternalBookPlugin.instance.messager.sprintf(
                    provider = true,
                    parseMode = ComponentParseMode.Plain,
                    formatText = LanguageKeys.Book.UNKNOWN_AUTHOR),
                dateFormatter.format(book.modified)
            ).split('\n').map { it.toComponent() })
        }
        !toWrittenBook && !addModifyInfo -> {
            lore(ExternalBookPlugin.instance.messager.sprintfPlain(
                LanguageKeys.Book.WRITABLE_BOOK_LORE,
                dateFormatter.format(book.created)
            ).split('\n').map { it.toComponent() })
        }
        else -> {
            lore(ExternalBookPlugin.instance.messager.sprintfPlain(
                LanguageKeys.Book.WRITABLE_BOOK_WITH_MODIFY_INFO,
                dateFormatter.format(book.created),
                Bukkit.getOfflinePlayer(book.modifier).name ?: ExternalBookPlugin.instance.messager.sprintf(
                    provider = true,
                    parseMode = ComponentParseMode.Plain,
                    formatText = LanguageKeys.Book.UNKNOWN_AUTHOR),
                dateFormatter.format(book.modified)
            ).split('\n').map { it.toComponent() })
        }
    }
    return this
}

fun BookMeta.updateBook(book: Book, addModifyInfo: Boolean = false): BookMeta {
    persistentDataContainer.set(bookUlidNbtKey, ULIDDataType(), book.id)
    persistentDataContainer.remove(bookUuidNbtKey)
    title(book.title.toComponent(ComponentParseMode.Plain))
    val creatorPlayer = Bukkit.getOfflinePlayer(book.creator)
    val authorName = creatorPlayer.name?.toComponent(ComponentParseMode.Plain) ?: ExternalBookPlugin.instance.messager.sprintf(
        provider = true,
        parseMode = ComponentParseMode.Plain,
        formatText = LanguageKeys.Book.UNKNOWN_AUTHOR)
    author(authorName)
    generation = BookMeta.Generation.COPY_OF_COPY
    displayName(book.title.toComponent(ComponentParseMode.Plain))
    when (addModifyInfo) {
        true -> {
            lore(ExternalBookPlugin.instance.messager.sprintfPlain(
                LanguageKeys.Book.WRITTEN_BOOK_WITH_MODIFY_INFO,
                authorName.toPlain(),
                dateFormatter.format(book.created),
                Bukkit.getOfflinePlayer(book.modifier).name ?: ExternalBookPlugin.instance.messager.sprintf(
                    provider = true,
                    parseMode = ComponentParseMode.Plain,
                    formatText = LanguageKeys.Book.UNKNOWN_AUTHOR),
                dateFormatter.format(book.modified)
            ).split('\n').map { it.toComponent() })
        }
        false -> {
            lore(ExternalBookPlugin.instance.messager.sprintfPlain(
                LanguageKeys.Book.WRITTEN_BOOK_LORE,
                authorName.toPlain(),
                dateFormatter.format(book.created)
            ).split('\n').map { it.toComponent() })
        }
    }
    return this
}

/**
 * 转换为可编辑的原始书籍
 */
internal fun Book.toOriginalWritableBook(): ItemStack {
    val item = ItemStack(Material.WRITABLE_BOOK)
    item.itemMeta = (item.itemMeta as BookMeta?)?.also {
        it.title(title.toComponent(ComponentParseMode.Plain))
        val creatorPlayer = Bukkit.getOfflinePlayer(creator)
        val authorName = creatorPlayer.name?.toComponent(ComponentParseMode.Plain) ?: ExternalBookPlugin.instance.messager.sprintf(
            provider = true,
            parseMode = ComponentParseMode.Plain,
            formatText = LanguageKeys.Book.UNKNOWN_AUTHOR)
        it.author(authorName)
        it.generation = BookMeta.Generation.ORIGINAL
        it.pages(pages.map { page -> page.toComponent(ComponentParseMode.Plain) })
        it.displayName(title.toComponent(ComponentParseMode.Plain))
    }
    return item
}