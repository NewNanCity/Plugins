package city.newnan.externalbook.gui.authorbooks

import city.newnan.externalbook.ExternalBookPlugin
import city.newnan.externalbook.book.BookAbstract
import city.newnan.gui.dataprovider.AsyncDataProvider
import city.newnan.gui.dataprovider.CacheStrategy
import java.util.UUID

class AuthorBookDataProvider(
    private val plugin: ExternalBookPlugin,
    private val playerId: UUID,
) : AsyncDataProvider<BookAbstract>(
    sizeProvider = { callback ->
        try {
            val count = plugin.librarian?.getPlayerBookCount(playerId) ?: 0
            callback(Result.success(count))
        } catch (e: Exception) {
            callback(Result.success(0))
        }
    },
    itemProvider = { offset, limit, callback ->
        try {
            val items = plugin.librarian?.getPlayerBooks(playerId, offset, limit) ?: emptyList()
            callback(Result.success(items))
        } catch (e: Exception) {
            callback(Result.success(emptyList()))
        }
    }
)