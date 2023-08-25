package city.newnan.betterbook.book

import city.newnan.betterbook.PluginMain
import me.lucko.helper.terminable.Terminable
import java.io.File
import java.util.*
import java.util.regex.Pattern

private val uuidFile: Pattern =
    Pattern.compile("^([0-9a-zA-Z]{8}-[0-9a-zA-Z]{4}-[0-9a-zA-Z]{4}-[0-9a-zA-Z]{4}-[0-9a-zA-Z]{12})[.]yml$")

object Librarian : Terminable {
    private val knownBookIds: MutableSet<UUID> = mutableSetOf()
    var playerBooks: MutableMap<UUID, MutableSet<UUID>> = mutableMapOf()

    fun enable(): Librarian {
        bindWith(PluginMain.INSTANCE)
        reload()
        return this
    }

    fun reload() {
        // 作者与书籍的映射
        PluginMain.INSTANCE.configManager.touch("players.yml", { playerBooks })
        val tmpBookOwnerMap = mutableMapOf<UUID, UUID>()
        playerBooks = PluginMain.INSTANCE.configManager.parse<MutableMap<UUID, MutableSet<UUID>>>("players.yml", saveToCache = false)
        playerBooks.forEach { (playerId, bookIds) -> bookIds.forEach { tmpBookOwnerMap[it] = playerId } }

        // 遍历书籍
        val that = this
        File(PluginMain.INSTANCE.dataFolder, "books").run {
            if (!exists() || !isDirectory) {
                return@run
            }
            list()?.forEach {
                val m = uuidFile.matcher(it)
                if (m.find()) {
                    val bookId = UUID.fromString(m.group(1))
                    knownBookIds.add(bookId)
                    if (tmpBookOwnerMap.remove(bookId) == null) {
                        val creator = that[bookId, false]!!.creator
                        playerBooks.getOrPut(creator) { mutableSetOf() }.add(bookId)
                    }
                }
            }
        }

        // 同步
        tmpBookOwnerMap.forEach {
            playerBooks[it.value]?.remove(it.key)
            if (playerBooks[it.value]?.isEmpty() == true) playerBooks.remove(it.value)
        }

        // 保存
        PluginMain.INSTANCE.configManager.save(playerBooks, "players.yml")
    }

    internal val nextAvailableUUID: UUID
        get() {
            var uuid: UUID
            do { uuid = UUID.randomUUID() } while (knownBookIds.contains(uuid))
            return uuid
        }

    operator fun get(bookId: UUID, cache: Boolean = true): Book? {
        if (!knownBookIds.contains(bookId)) return null
        try {
            return PluginMain.INSTANCE.configManager.parse<Book>("books/$bookId.yml", saveToCache = cache)
        } catch (e: Exception) { e.printStackTrace() }
        return null
    }

    operator fun plusAssign(idAndBook: Pair<UUID, Book>) {
        knownBookIds.add(idAndBook.first)
        playerBooks.getOrPut(idAndBook.second.creator) { mutableSetOf() }.add(idAndBook.first)
        PluginMain.INSTANCE.configManager.save(idAndBook.second, "books/${idAndBook.first}.yml")
    }

    operator fun minusAssign(idAndBook: Pair<UUID, Book>) {
        knownBookIds.remove(idAndBook.first)
        playerBooks[idAndBook.second.creator]?.remove(idAndBook.first)
        if (playerBooks[idAndBook.second.creator]?.isEmpty() == true) playerBooks.remove(idAndBook.second.creator)
        File(PluginMain.INSTANCE.dataFolder, "removed").also {
            if (!it.exists()) it.mkdirs()
            File(PluginMain.INSTANCE.dataFolder, "books/${idAndBook.first}.yml").renameTo(
                File(it, "${System.currentTimeMillis()}.yml")
            )
        }
    }

    override fun close() {
        knownBookIds.clear()
    }
}