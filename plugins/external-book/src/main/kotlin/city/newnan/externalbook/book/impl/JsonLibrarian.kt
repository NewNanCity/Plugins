package city.newnan.externalbook.book.impl

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.DateDeserializers
import com.fasterxml.jackson.databind.ser.std.DateSerializer
import city.newnan.config.extensions.configManager
import city.newnan.externalbook.ExternalBookPlugin
import city.newnan.externalbook.book.*
import city.newnan.externalbook.config.StorageMode
import com.github.f4b6a3.ulid.Ulid
import com.github.f4b6a3.ulid.UlidCreator
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import java.util.regex.Pattern

// NOTE: 系统曾经使用UUID作为书籍的唯一标识，但现在改为了ULID，但是为了兼容旧数据，需要平滑迁移

/**
 * 书籍数据类
 *
 * @property title 书籍标题
 * @property creator 创建者UUID
 * @property modifier 最后修改者UUID
 * @property created 创建时间
 * @property modified 最后修改时间
 * @property pages 书籍页面内容
 *
 * @author NewNanCity
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class JsonBook(
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
)

typealias Uuid2UlidMap = MutableMap<UUID, Ulid>

private val uuidFile: Pattern =
    Pattern.compile("^([0-9a-zA-Z]{8}-[0-9a-zA-Z]{4}-[0-9a-zA-Z]{4}-[0-9a-zA-Z]{4}-[0-9a-zA-Z]{12})[.]yml$")
private val ulidFile: Pattern = Pattern.compile("^([0-9A-Z]{26})[.]yml$")

/**
 * JSON文件系统图书管理员实现
 *
 * 基于YAML文件存储的图书管理员实现，将书籍数据存储在文件系统中。
 * 支持缓存、分页查询和自动备份功能。
 *
 * @author NewNanCity
 * @since 1.0.0
 */
class JsonLibrarian(private val plugin: ExternalBookPlugin) : Librarian {
    private val knownBookIds: MutableSet<Ulid> = mutableSetOf()
    private val playerBooks: MutableMap<UUID, MutableSet<Ulid>> = mutableMapOf()
    private val bookUuid2Ulid: Uuid2UlidMap = mutableMapOf()
    private var bookUuid2UlidDirty: Boolean = false

    override val mode: StorageMode = StorageMode.FILE

    /**
     * 重新加载所有书籍数据
     */
    override fun reload() {
        plugin.logger.info("Reloading book data...")

        // 清理现有数据
        knownBookIds.clear()

        // 加载书籍UUID到ULID的映射
        reloadBookUuid2UlidMapping()

        // 加载玩家与书籍的映射关系
        reloadPlayerBooksMapping()

        // 扫描并加载所有书籍文件
        scanAndLoadKnownBookIds()

        // 保存映射关系
        saveBookUuid2UlidMapping()

        plugin.logger.info("Book data reloaded successfully")
    }

    override fun getUlid(uuid: UUID): Ulid? {
        return bookUuid2Ulid[uuid]
    }

    /**
     * 加载书籍UUID到ULID的映射
     */
    private fun reloadBookUuid2UlidMapping() {
        try {
            plugin.configManager.touch("book-uuid-ulid.yml", { bookUuid2Ulid })
            val tmpData = plugin.configManager.parse<Map<UUID, String>>("book-uuid-ulid.yml", useCache = false)
            bookUuid2Ulid.clear()
            tmpData.forEach { (uuid, ulidStr) ->
                try {
                    bookUuid2Ulid[uuid] = Ulid.from(ulidStr)
                } catch (e: Exception) {
                    plugin.logger.warn("Failed to parse ULID from book UUID mapping: ${e.message}")
                }
            }
        } catch (e: Exception) {
            plugin.logger.warn("Failed to load book UUID to ULID mapping: ${e.message}")
        }
    }

    /**
     * 加载玩家与书籍的映射关系
     * 将UUID转换为ULID并添加映射关系
     */
    private fun reloadPlayerBooksMapping() {
        try {
            plugin.configManager.touch("players.yml", { playerBooks })
            val tmpData = plugin.configManager.parse<MutableMap<UUID, MutableSet<String>>>("players.yml", useCache = false)
                .mapValues {
                    it.value.mapNotNull { idStr ->
                        when (idStr.length) {
                            36 -> null
                            26 -> Ulid.from(idStr)
                            else -> null
                        }
                    }.toMutableSet()
                }
            playerBooks.clear()
            playerBooks.putAll(tmpData)
        } catch (e: Exception) {
            plugin.logger.warn("Failed to load player books mapping: ${e.message}")
        }
    }

    /**
     * 扫描并加载 knownBookIds 也会修复不存在的数据映射
     */
    private fun scanAndLoadKnownBookIds() {
        val booksDir = File(plugin.dataFolder, "books")
        if (!booksDir.exists() || !booksDir.isDirectory) {
            plugin.logger.info("Books directory does not exist, creating...")
            booksDir.mkdirs()
            return
        }

        var shouldSavePlayerBookMapping = false
        val tmpBookOwnerMap = mutableMapOf<Ulid, UUID>()
        playerBooks.forEach { (playerId, bookIds) ->
            bookIds.forEach { tmpBookOwnerMap[it] = playerId }
        }

        booksDir.list()?.forEach { fileName ->
            val bookId = when {
                ulidFile.matcher(fileName).find() -> Ulid.from(fileName.substring(0, 26))
                uuidFile.matcher(fileName).find() -> {
                    val ulid = bookUuid2Ulid.getOrPut(UUID.fromString(fileName.substring(0, 36))) {
                        val book = plugin.configManager.parse<JsonBook>("books/$fileName", useCache = false)
                        val createdTime = book.created
                        bookUuid2UlidDirty = true
                        val ulid = UlidCreator.getMonotonicUlid(createdTime.time)
                        playerBooks.getOrPut(book.creator) { mutableSetOf() }.add(ulid)
                        shouldSavePlayerBookMapping = true
                        ulid
                    }
                    // 重命名文件
                    val bookFile = File(booksDir, fileName)
                    val newBookFile = File(booksDir, "$ulid.yml")
                    if (bookFile.renameTo(newBookFile)) {
                        plugin.logger.info("Renamed book file: $fileName -> $ulid.yml")
                    } else {
                        plugin.logger.warn("Failed to rename book file: $fileName -> $ulid.yml")
                    }
                    ulid
                }
                else -> return@forEach
            }
            knownBookIds.add(bookId)
            // 如果映射关系中没有这本书，尝试从书籍文件中获取作者信息
            if (tmpBookOwnerMap.remove(bookId) == null) {
                val book = get(bookId, cache = false)
                if (book == null) {
                    plugin.logger.warn("Failed to load book $bookId, skipped.")
                    return@forEach
                }
                playerBooks.getOrPut(book.creator) { mutableSetOf() }.add(bookId)
                shouldSavePlayerBookMapping = true
            }
        }

        // 清理找不到的书籍
        if (tmpBookOwnerMap.isNotEmpty()) {
            tmpBookOwnerMap.forEach { (bookId, playerId) ->
                plugin.logger.warn("Book $bookId not found, removing from player $playerId")
                playerBooks[playerId]?.remove(bookId)
            }
            shouldSavePlayerBookMapping = true
        }

        if (shouldSavePlayerBookMapping) {
            savePlayerBooksMapping()
        }
    }

    /**
     * 保存玩家与书籍的映射关系
     */
    private fun savePlayerBooksMapping() {
        try {
            plugin.configManager.save(playerBooks.mapValues {
                it.value.map { ulid -> ulid.toString() }.toSet()
            }, "players.yml", updateCache = false)
        } catch (e: Exception) {
            plugin.logger.error("Failed to save player books mapping: ${e.message}", e)
        }
    }

    /**
     * 保存书籍UUID到ULID的映射
     */
    private fun saveBookUuid2UlidMapping() {
        if (!bookUuid2UlidDirty) return
        try {
            plugin.configManager.save(bookUuid2Ulid.mapValues {
                it.value.toString()
            }, "book-uuid-ulid.yml", updateCache = false)
            bookUuid2UlidDirty = false
        } catch (e: Exception) {
            plugin.logger.error("Failed to save book UUID to ULID mapping: ${e.message}", e)
        }
    }

    /**
     * 获取书籍
     */
    override operator fun get(bookId: Ulid, cache: Boolean): Book? {
        if (!knownBookIds.contains(bookId)) return null
        return try {
            plugin.configManager.parse<JsonBook>("books/$bookId.yml", useCache = cache).let {
                Book(
                    id = bookId,
                    title = it.title,
                    creator = it.creator,
                    modifier = it.modifier,
                    created = it.created,
                    modified = it.modified,
                    pages = it.pages
                )
            }
        } catch (e: Exception) {
            plugin.logger.error("Failed to load book $bookId: ${e.message}", e)
            null
        }
    }

    /**
     * 添加书籍
     */
    override operator fun plusAssign(book: Book) {
        val bookId = book.id
        val book = JsonBook(
            title = book.title,
            creator = book.creator,
            modifier = book.modifier,
            created = book.created,
            modified = book.modified,
            pages = book.pages
        )

        knownBookIds.add(bookId)
        playerBooks.getOrPut(book.creator) { mutableSetOf() }.add(bookId)
        plugin.configManager.save(book, "books/$bookId.yml")

        // 保存映射关系
        savePlayerBooksMapping()

        plugin.logger.debug("Added book: $bookId - ${book.title}")
    }

    /**
     * 删除书籍
     */
    override operator fun minusAssign(bookId: Ulid) {
        val bookPlayerId = getBookCreator(bookId)

        knownBookIds.remove(bookId)

        // 移动文件到removed目录而不是直接删除
        val removedDir = File(plugin.dataFolder, "removed")
        if (!removedDir.exists()) {
            removedDir.mkdirs()
        }

        val originalFile = File(plugin.dataFolder, "books/$bookId.yml")
        val backupFile = File(removedDir, "$bookId.yml")

        if (originalFile.exists()) {
            if (originalFile.renameTo(backupFile)) {
                plugin.logger.info("Book backup created: ${backupFile.name}")
            } else {
                plugin.logger.warn("Failed to create backup for book: $bookId")
            }
        }

        // 更新映射关系
        bookPlayerId?.let { creator ->
            playerBooks[creator]?.remove(bookId)
            if (playerBooks[creator]?.isEmpty() == true) {
                playerBooks.remove(creator)
            }
            savePlayerBooksMapping()
        }

        // 更新ULID到UUID的映射
        val bookUuids = bookUuid2Ulid.filter { it.value == bookId }.keys
        if (bookUuids.isNotEmpty()) {
            bookUuid2UlidDirty = true
            bookUuid2Ulid -= bookUuids
            saveBookUuid2UlidMapping()
        }

        plugin.logger.debug("Removed book: $bookId")
    }

    /**
     * 获取书籍作者
     */
    override fun getBookCreator(bookId: Ulid): UUID? {
        return get(bookId)?.creator
    }

    /**
     * 检查是否包含指定书籍
     */
    override operator fun contains(bookId: Ulid): Boolean {
        return knownBookIds.contains(bookId)
    }

    /**
     * 分页获取玩家的所有书籍ID
     */
    private fun getPlayerBookIds(playerId: UUID, offset: Int, limit: Int): List<Ulid> {
        val books = playerBooks[playerId]?.toList() ?: emptyList()
        val endIndex = minOf(offset + limit, books.size)
        return if (offset < books.size) books.subList(offset, endIndex) else emptyList()
    }

    /**
     * 获取玩家的书籍数量
     */
    override fun getPlayerBookCount(playerId: UUID): Int {
        return playerBooks[playerId]?.size ?: 0
    }

    /**
     * 分页获取玩家的所有书籍
     */
    override fun getPlayerBooks(playerId: UUID, offset: Int, limit: Int): List<BookAbstract> {
        return getPlayerBookIds(playerId, offset, limit).mapNotNull { bookId ->
            get(bookId)?.let {
                BookAbstract(
                    id = bookId,
                    title = it.title,
                    creator = it.creator,
                    modifier = it.modifier,
                    created = it.created,
                    modified = it.modified,
                    preview = it.pages.firstOrNull()?.let { page ->
                        page.substring(0, minOf(35, page.length)).replace('\n', ' ')
                    } ?: ""
                )
            }
        }
    }

    /**
     * 获取书籍数量
     */
    override fun getBookCount(): Int = knownBookIds.size

    /**
     * 分页获取所有已知的书籍ID
     */
    override fun getAllBookIds(playerId: UUID, offset: Int, limit: Int): List<Ulid> {
        val books = knownBookIds.toList()
        val endIndex = minOf(offset + limit, books.size)
        return if (offset < books.size) books.subList(offset, endIndex) else emptyList()
    }

    /**
     * 获取所有有书籍的玩家ID
     */
    override fun getAuthors(): Set<UUID> = playerBooks.keys.toSet()

    override fun close() {
        plugin.logger.info("Shutting down JsonLibrarian...")
        knownBookIds.clear()
        playerBooks.clear()
    }
}
