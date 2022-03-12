package city.newnan.betterbook

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Subcommand
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta
import java.util.*

@CommandAlias("book|betterbook|bb")
object BetterBookCommand : BaseCommand() {
    @Subcommand("publish|static")
    fun publishCommand(player: Player) {
        player.inventory.itemInMainHand.findBookUUID(written = false)?.also {
            val book = Librarian[it] ?: return@also
            if (player.inventory.addItem(ItemStack(Material.WRITTEN_BOOK).apply {
                    itemMeta = (itemMeta as BookMeta?)?.applyBook(book, toWrittenBook = true, addModifyInfo = false)
                }).size > 0) {
                BetterBook.INSTANCE.messageManager.printf(player, "背包已满，无法出版图书！")
            } else {
                BetterBook.INSTANCE.messageManager.printf(player, "书目出版成功，修改内容将会同步到发布的书中！")
            }
            return@publishCommand
        }
        BetterBook.INSTANCE.messageManager.printf(player, "请用主手拿书与笔/书目未注册&导入！")
    }

    @Subcommand("import|register")
    fun importCommand(player: Player) {
        var book: Book? = null
        var newBook = true
        if (player.inventory.itemInMainHand.type == Material.WRITABLE_BOOK) {
            val now = Date()
            val bookId: UUID? = player.inventory.itemInMainHand.findBookUUID(written = false)?.apply {
                book = Librarian[this]
                book?.run {
                    if (creator != player.uniqueId && !player.hasPermission("betterbook.bypass")) {
                        BetterBook.INSTANCE.messageManager.printf(player, "只有原作者/OP/特别授权者能够导入已注册的原书！")
                        return@importCommand
                    }
                    newBook = false
                }
            }
            if (book == null)
                book = Book(
                    title = "《无题》",
                    created = now,
                    modified = now,
                    creator = player.uniqueId,
                    modifier = player.uniqueId,
                    uuid = bookId ?: UUID.randomUUID()
                )
            val bookMeta = player.inventory.itemInMainHand.itemMeta as BookMeta
            val me = bookMeta.author?.let { Bukkit.getPlayer(it)?.uniqueId } ?: player.uniqueId
            book!!.apply {
                title = bookMeta.title ?: bookMeta.displayName
                modifier = me
                modified = now
                pages.clear()
                pages.addAll(bookMeta.pages)
                Librarian.set(this)
                player.inventory.itemInMainHand.itemMeta = bookMeta.applyBook(this, toWrittenBook = false, addModifyInfo = true)
            }
            BetterBook.INSTANCE.messageManager.printf(player,
                if (newBook) "书目注册并导入成功，现在可以出版了！" else "书目内容更新完毕，所有出版成书内容随之更新！")
        } else {
            BetterBook.INSTANCE.messageManager.printf(player, "请用主手拿书与笔！")
        }
    }

    @Subcommand("export|origin|edit")
    fun exportCommand(player: Player) {
        player.inventory.itemInMainHand.findBookUUID()?.also {
            val book = Librarian[it] ?: return@also
            if (book.creator != player.uniqueId && !player.hasPermission("betterbook.bypass")) {
                BetterBook.INSTANCE.messageManager.printf(player, "只有原作者/OP/特别授权者能够导出原书！")
                return@exportCommand
            }
            if (player.inventory.addItem(ItemStack(Material.WRITABLE_BOOK).apply {
                    itemMeta = (itemMeta as BookMeta?)?.applyBook(book, toWrittenBook = false, addModifyInfo = true)
                }).size > 0) {
                BetterBook.INSTANCE.messageManager.printf(player, "背包已满，无法导出原书！")
            } else {
                BetterBook.INSTANCE.messageManager.printf(player, "原书导出成功，可以使用该书编辑内容！")
            }
            return@exportCommand
        }
        BetterBook.INSTANCE.messageManager.printf(player, "请用主手拿书与笔或者成书！")
    }

    @Subcommand("strip|unbind")
    fun stripCommand(player: Player) {
        player.inventory.itemInMainHand.findBookUUID()?.also {
            val book = Librarian[it] ?: return@also
            if (book.creator != player.uniqueId && !player.hasPermission("betterbook.bypass")) {
                BetterBook.INSTANCE.messageManager.printf(player, "只有原作者/OP/特别授权者能够解绑该书！")
                return@stripCommand
            }
            player.inventory.itemInMainHand.itemMeta!!
                .persistentDataContainer.remove(NamespacedKey(BetterBook.INSTANCE, "book-uuid"))
            BetterBook.INSTANCE.messageManager
                .printf(player, "已解绑该书，但已发布内容与成书并未受影响，可以继续使用成书导出原书！")
            return@stripCommand
        }
        BetterBook.INSTANCE.messageManager.printf(player, "请用主手拿书与笔或者成书！")
    }

    // @Subcommand("test")
    fun testCommand(player: Player) {
        player.inventory.addItem(ItemStack(Material.WRITABLE_BOOK).apply {
            itemMeta = (itemMeta as BookMeta?)?.apply {
                persistentDataContainer.set(
                    NamespacedKey(BetterBook.INSTANCE, "book-uuid"), UUIDDataType(), UUID.randomUUID()
                )
                setDisplayName("Test")
            }
        })
    }
}