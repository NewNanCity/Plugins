package city.newnan.betterbook.book

import city.newnan.betterbook.PluginMain
import me.lucko.helper.terminable.Terminable
import java.io.File
import java.util.*
import java.util.regex.Pattern

private val uuidFile: Pattern =
    Pattern.compile("^([0-9a-zA-Z]{8}-[0-9a-zA-Z]{4}-[0-9a-zA-Z]{4}-[0-9a-zA-Z]{4}-[0-9a-zA-Z]{12}).yml$")

object Librarian : Terminable {
    private val knownBookIds: MutableSet<UUID> = mutableSetOf()

    fun enable(): Librarian {
        File(PluginMain.INSTANCE.dataFolder, "books").run {
            if (!exists() || !isDirectory) return@run
            list()?.forEach {
                uuidFile.matcher(it).runCatching { if (groupCount() > 1) knownBookIds.add(UUID.fromString(group(1))) }
            }
        }
        bindWith(PluginMain.INSTANCE)
        return this
    }

    internal val nextAvailableUUID: UUID
        get() {
            var uuid: UUID
            do { uuid = UUID.randomUUID() } while (knownBookIds.contains(uuid))
            return uuid
        }

    operator fun get(bookId: UUID): Book? {
        if (!knownBookIds.contains(bookId)) return null
        try {
            return PluginMain.INSTANCE.configManager.parse<Book>("books/$bookId.yml")
        } catch (e: Exception) { e.printStackTrace() }
        return null
    }

    operator fun plusAssign(idAndBook: Pair<UUID, Book>) {
        knownBookIds.add(idAndBook.first)
        PluginMain.INSTANCE.configManager.save(idAndBook.second, "books/${idAndBook.first}.yml")
    }

    override fun close() {
        knownBookIds.clear()
    }
}