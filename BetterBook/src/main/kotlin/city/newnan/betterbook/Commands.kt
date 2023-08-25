package city.newnan.betterbook

import city.newnan.betterbook.book.*
import city.newnan.betterbook.gui.openOnlinePlayersGui
import city.newnan.betterbook.gui.openPlayerBooksGui
import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta
import java.util.*

@CommandAlias("book|betterbook")
object Commands : BaseCommand() {
    @HelpCommand
    @Subcommand("help")
    fun help(sender: CommandSender, help: CommandHelp) {
        help.showHelp()
    }

    @Subcommand("reload")
    @CommandPermission("betterbook.reload")
    fun reloadCommand(sender: CommandSender) {
        Librarian.reload()
    }

    @Subcommand("publish|static")
    @Description("出版书目，如果书目已注册则会覆盖原书！")
    fun publishCommand(player: Player) {
        player.inventory.itemInMainHand.findBookUUID(written = false)?.also {
            val book = Librarian[it] ?: return@also
            if (book.creator != player.uniqueId && !player.hasPermission("betterbook.bypass")) {
                PluginMain.INSTANCE.message.printf(player, "只有原作者/OP/特别授权者能够出版原书！")
                return@publishCommand
            }
            if (player.inventory.addItem(ItemStack(Material.WRITTEN_BOOK).apply {
                    itemMeta = (itemMeta as BookMeta?)?.applyBook(book, it, toWrittenBook = true, addModifyInfo = false)
                }).size > 0) {
                PluginMain.INSTANCE.message.printf(player, "背包已满，无法出版图书！")
            } else {
                PluginMain.INSTANCE.message.printf(player, "书目出版成功，修改内容将会同步到发布的书中！")
            }
            return@publishCommand
        }
        PluginMain.INSTANCE.message.printf(player, "请用主手拿书与笔/书目未注册&导入！")
    }

    @Subcommand("import|register")
    @Description("导入书目，如果书目已注册则会覆盖原书！")
    fun importCommand(player: Player) {
        var newBook = true
        val item = player.inventory.itemInMainHand
        if ((item.type == Material.WRITABLE_BOOK || item.type == Material.WRITABLE_BOOK) && item.itemMeta != null) {
            // 检查手上的书是不是已经注册过
            var book: Book? = null
            val bookId: UUID = item.findBookUUID(written = true)?.let {
                book = Librarian[it]
                book?.run {
                    if (creator != player.uniqueId && !player.hasPermission("betterbook.bypass")) {
                        PluginMain.INSTANCE.message.printf(player, "只有原作者/OP/特别授权者能够导入已注册的原书！")
                        return@importCommand
                    }
                    newBook = false
                }
                if (book == null) null else it
            } ?: Librarian.nextAvailableUUID
            // 创建新的书
            val bookMeta = item.itemMeta as BookMeta
            val now = Date()
            val book2 = Book(
                title = if (bookMeta.hasDisplayName()) bookMeta.displayName else bookMeta.title ?: book?.title ?: "《无题》",
                creator = book?.creator ?: player.uniqueId,
                modifier = bookMeta.author?.let { Bukkit.getPlayer(it)?.uniqueId } ?: player.uniqueId,
                created = book?.created ?: now,
                modified = now,
                pages = bookMeta.pages.toList(),
            )
            // 添加到图书馆
            Librarian += bookId to book2
            // 信息更新到手上的书
            item.itemMeta = bookMeta.applyBook(book2, bookId, toWrittenBook = false, addModifyInfo = true)
            PluginMain.INSTANCE.message.printf(player,
                if (newBook) "书目注册并导入成功，现在可以出版了！" else "书目内容更新完毕，所有出版成书内容随之更新！")
        } else {
            PluginMain.INSTANCE.message.printf(player, "请用主手拿[书与笔]或者普通的[成书]！")
        }
    }

    @Subcommand("export|origin|edit")
    @Description("导出原书")
    fun exportCommand(player: Player) {
        player.inventory.itemInMainHand.findBookUUID()?.also {
            val book = Librarian[it] ?: return@also
            if (book.creator != player.uniqueId && !player.hasPermission("betterbook.bypass")) {
                PluginMain.INSTANCE.message.printf(player, "只有原作者/OP/特别授权者能够导出原书！")
                return@exportCommand
            }
            if (player.inventory.addItem(ItemStack(Material.WRITABLE_BOOK).apply {
                    itemMeta = (itemMeta as BookMeta?)?.applyBook(book, it, toWrittenBook = false, addModifyInfo = true)
                }).size > 0) {
                PluginMain.INSTANCE.message.printf(player, "背包已满，无法导出原书！")
            } else {
                PluginMain.INSTANCE.message.printf(player, "原书导出成功，可以使用该书编辑内容！")
            }
            return@exportCommand
        }
        PluginMain.INSTANCE.message.printf(player, "请用主手拿书与笔或者成书！")
    }

    @Subcommand("strip|unbind")
    @Description("解绑书目，解绑后这本书就变成一本普通的书，之后可以继续使用成书导出原书！")
    fun stripCommand(player: Player) {
        player.inventory.itemInMainHand.findBookUUID()?.also {
            val book = Librarian[it] ?: return@also
            if (book.creator != player.uniqueId && !player.hasPermission("betterbook.bypass")) {
                PluginMain.INSTANCE.message.printf(player, "只有原作者/OP/特别授权者能够解绑该书！")
                return@stripCommand
            }
            player.inventory.itemInMainHand.itemMeta!!.persistentDataContainer.remove(bookIdNbtKey)
            PluginMain.INSTANCE.message
                .printf(player, "已解绑该书，这本书变成一本普通的书，不再受书局管理，可以继续使用成书导出原书！")
            return@stripCommand
        }
        PluginMain.INSTANCE.message.printf(player, "请用主手拿书与笔或者成书！")
    }

    @Private
    @Subcommand("id")
    @CommandPermission("betterbook.bypass")
    @Description("获得指定 id 的书")
    fun uuidCommand(player: Player, uuidString: String) {
        kotlin.runCatching { UUID.fromString(uuidString) }
            .onFailure{ PluginMain.INSTANCE.message.printf(player, "UUID无效！") }
            .onSuccess {
                val book = Librarian[it]
                if (book == null) {
                    PluginMain.INSTANCE.message.printf(player, "书目不存在！")
                } else {
                    if (player.inventory.addItem(ItemStack(Material.WRITTEN_BOOK).apply {
                            itemMeta = (itemMeta as BookMeta?)?.applyBook(book, it, toWrittenBook = true, addModifyInfo = false)
                        }).size > 0) {
                        PluginMain.INSTANCE.message.printf(player, "背包已满，无法获得成书！")
                    } else {
                        PluginMain.INSTANCE.message.printf(player, "成书已放至背包！")
                    }
                }
            }
    }

    @Private
    @Subcommand("writable")
    @Description("将书变成可编辑状态")
    fun editCommand(player: Player) {
        if (player.inventory.itemInMainHand.type != Material.WRITTEN_BOOK) {
            PluginMain.INSTANCE.message.printf(player, "请拿成书！")
            return
        }
        if (player.inventory.itemInMainHand.findBookUUID() != null) {
            player.performCommand("book edit")
            return
        }
        (player.inventory.itemInMainHand.itemMeta as BookMeta).also {
            if (it.author?.equals(player.displayName) != true && !player.hasPermission("betterbook.bypass")) {
                PluginMain.INSTANCE.message.printf(player, "只有原作者/Op/有权限者能够将原书变成成书！")
                return@editCommand
            }
            if (player.inventory.addItem(ItemStack(Material.WRITABLE_BOOK).apply {
                    itemMeta = (itemMeta as BookMeta?)?.also { newMeta ->
                        newMeta.author = it.author
                        newMeta.title = it.title
                        newMeta.setDisplayName(it.title)
                        newMeta.generation = BookMeta.Generation.ORIGINAL
                        newMeta.lore = it.lore
                        newMeta.pages = it.pages.toList()
                    }
                }).size > 0) {
                PluginMain.INSTANCE.message.printf(player, "背包已满，无法获得书！")
            } else {
                PluginMain.INSTANCE.message.printf(player, "书已放至背包！")
            }
        }
    }

    @Default
    @Subcommand("gui")
    @Description("打开牛腩书局界面")
    fun guiCommand(player: Player) {
        val session = PluginMain.INSTANCE.gui[player]
        session.clear()
        openPlayerBooksGui(session)
    }

    @Subcommand("admin")
    @Description("打开牛腩书局界面")
    @CommandPermission("betterbook.bypass")
    fun guiAdminCommand(player: Player) {
        val session = PluginMain.INSTANCE.gui[player]
        session.clear()
        openOnlinePlayersGui(session) { target ->
            openPlayerBooksGui(session, target)
        }
    }
}