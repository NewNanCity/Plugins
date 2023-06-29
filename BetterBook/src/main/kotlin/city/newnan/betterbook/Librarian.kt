package city.newnan.betterbook

import me.lucko.helper.terminable.Terminable
import java.io.File
import java.util.*
import java.util.regex.Pattern

object Librarian : Terminable {
    private val bookUUIDs: HashSet<UUID> = hashSetOf()
    private val uuidFile: Pattern =
        Pattern.compile("([0-9a-zA-Z]{8}-[0-9a-zA-Z]{4}-[0-9a-zA-Z]{4}-[0-9a-zA-Z]{4}-[0-9a-zA-Z]{12}).yml")

    fun enable(): Librarian {
        File(BetterBook.INSTANCE.dataFolder, "books").run {
            if (!exists() || !isDirectory) return@run
            list()?.forEach {
                uuidFile.matcher(it).runCatching { if (groupCount() > 1) bookUUIDs.add(UUID.fromString(group(1))) }
            }
        }
        bindWith(BetterBook.INSTANCE)
        return this
    }

    internal val nextAvailableUUID: UUID
        get() {
            synchronized(bookUUIDs) {
                var uuid: UUID
                do {
                    uuid = UUID.randomUUID()
                } while (bookUUIDs.contains(uuid))
                return uuid
            }
        }

    operator fun get(uuid: UUID): Book? {
        val path = "books/$uuid.yml"
        return if (BetterBook.INSTANCE.configManager touch path)
            BetterBook.INSTANCE.configManager[path]?.toBook(uuid) else null
    }

    fun set(book: Book) {
        bookUUIDs.add(book.uuid)
        book.saveToFile()
    }

    override fun close() {
        bookUUIDs.clear()
    }
}