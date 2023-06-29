package city.newnan.betterbook

import city.newnan.violet.config.setListIfNull
import java.util.UUID
import java.util.Date
import me.lucko.helper.config.ConfigurationNode
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta
import java.text.SimpleDateFormat

private val dateFormatter: SimpleDateFormat = SimpleDateFormat("yyyy年M月d日")

internal fun ItemStack.findBookUUID(written: Boolean = true, writable: Boolean = true): UUID? {
    if (!(written && type == Material.WRITTEN_BOOK) && !(writable && type == Material.WRITABLE_BOOK)) return null
    return itemMeta?.persistentDataContainer?.get(NamespacedKey(BetterBook.INSTANCE, "book-uuid"), UUIDDataType())
}
internal fun BookMeta.applyBook(book: Book, toWrittenBook: Boolean = true, addModifyInfo: Boolean = true): BookMeta {
    persistentDataContainer.set(
        NamespacedKey(BetterBook.INSTANCE, "book-uuid"), UUIDDataType(), book.uuid
    )
    title = book.title
    author = Bukkit.getOfflinePlayer(book.creator).name ?: "秩名"
    generation = BookMeta.Generation.COPY_OF_COPY
    setDisplayName(book.title)
    val info = ArrayList<String>()
    if (toWrittenBook) {
        info.add("§r§7${dateFormatter.format(book.created)} 初版§r")
    } else {
        info.add("§r§6作者     §r§7${Bukkit.getOfflinePlayer(book.creator).name ?: "秩名"}§r")
        info.add("§r§6创建时间 §r§7${dateFormatter.format(book.created)}§r")
        pages = ArrayList<String>(book.pages)
    }
    if (addModifyInfo) {
        info.add("§r§6最后修改 §r§7${Bukkit.getOfflinePlayer(book.modifier).name ?: "秩名"}§r")
        info.add("§r§6修改时间 §r§7${dateFormatter.format(book.modified)}§r",)
    }
    info.add("§r§8§l[牛腩书局出版社]§r")
    lore = info
    return this
}
internal fun Long.toDate(): Date = Date(this)
internal fun String.toUUID(): UUID = UUID.fromString(this)

internal fun ConfigurationNode.toBook(uuid: UUID): Book? {
    val title = getNode("title").string ?: return null
    val creator = getNode("creator").string?.toUUID() ?: return null
    val modifier = getNode("modifier").string?.toUUID() ?: creator
    val created = getNode("created").run { if (isEmpty) Date() else long.toDate() }
    val modified = getNode("modified").run { if (isEmpty) Date() else long.toDate() }
    val pages = arrayListOf<String>().also {
        getNode("pages").setListIfNull().childrenList.forEach { child ->
            if (!child.isEmpty && child.string != null) it.add(child.string!!)
        }
    }
    return Book(title, created, modified, creator, modifier, uuid, pages)
}

class Book(var title: String,
           var created: Date,
           var modified: Date,
           var creator: UUID,
           var modifier: UUID,
           val uuid: UUID = Librarian.nextAvailableUUID,
           val pages: ArrayList<String> = arrayListOf()) {

    internal fun saveToFile() {
        val filePath = "books/$uuid.yml"
        BetterBook.INSTANCE.configManager.run {
            getOrCopyTemplate(filePath, "book-template.yml")?.run {
                getNode("title").value = title
                getNode("creator").value = creator.toString()
                getNode("modifier").value = modifier.toString()
                getNode("created").value = created.time
                getNode("modified").value = modified.time
                getNode("pages").value = pages
            }
            save(filePath)
        }
    }

    internal fun readBook(player: Player): Book {
        val bookItem = ItemStack(Material.WRITTEN_BOOK)
        bookItem.itemMeta = (bookItem.itemMeta as BookMeta?)?.also {
            it.title = title
            it.author = Bukkit.getOfflinePlayer(creator).name ?: "未知"
            it.generation = BookMeta.Generation.COPY_OF_COPY
            it.pages = pages
        }
        player.openBook(bookItem)
        return this
    }

    fun clone(): Book {
        return Book(title, created, modified, creator, modifier, uuid, ArrayList(pages))
    }
}