package city.newnan.externalbook.book.impl

import city.newnan.config.extensions.configManager
import city.newnan.externalbook.ExternalBookPlugin
import city.newnan.externalbook.book.*
import city.newnan.externalbook.config.StorageMode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.f4b6a3.ulid.Ulid
import com.zaxxer.hikari.HikariDataSource
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentDate
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.json.json
import java.time.ZoneId
import java.util.*

/**
 * 书籍数据表结构
 *
 * 使用单表设计，存储所有书籍相关信息：
 * - id: ULID主键，提供时间排序和分片友好的特性
 * - uuid: 可空的UUID字段，用于兼容旧数据系统
 * - title: 书籍标题
 * - creator/modifier: 创建者和修改者的UUID
 * - created/modified: 时间戳，支持自动更新
 * - pages: JSON格式存储页面内容
 * - preview: VARCHAR字段存储书籍预览文本（35字符，140字节），优化BookAbstract查询性能
 *
 * 索引策略：
 * - 主键索引：id (ULID)
 * - 查询索引：creator (按作者查询)、uuid (兼容查询)
 * - 时间索引：created、modified (按时间排序)
 */
class Books(tableName: String, private val jsonObjectMapper: ObjectMapper) : Table(tableName) {
    val id = char(name = "id", length = 26)
    val uuid = uuid("uuid").nullable().index(isUnique = true, customIndexName = "book_uuid_idx")
    val title = varchar("title", 64)
    val creator = uuid("creator").index(isUnique = false, customIndexName = "book_author_idx")
    val modifier = uuid("modifier")
    val created = date("created").defaultExpression(CurrentDate)
        .index(isUnique = false, customIndexName = "book_created_idx")
    val modified = date("modified").defaultExpression(CurrentDate)
        .index(isUnique = false, customIndexName = "book_modified_idx")
    val pages = json(
        "pages",
        { jsonObjectMapper.writeValueAsString(it) },
        { jsonObjectMapper.readValue<List<String>>(it) }
    )
    val preview = varchar("preview", 36) // 存储书籍预览文本

    override val primaryKey = PrimaryKey(id, name = "book_ulid_idx")
}

fun LocalDate.toJavaDate(zone: ZoneId = ZoneId.systemDefault()): Date =
    Date.from(toJavaLocalDate().atStartOfDay(zone).toInstant())

fun Date.toKotlinLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate {
    return toInstant().atZone(zone).toLocalDate().toKotlinLocalDate()
}

/**
 * 数据库图书管理员实现
 *
 * 基于数据库存储的图书管理员实现，使用Ktorm ORM框架。
 * 采用单表设计，相比JsonLibrarian的三表设计更加简洁高效。
 *
 * 设计特点：
 * - 使用ULID作为主键，提供时间排序和分布式友好特性
 * - 保留UUID字段用于兼容旧系统数据
 * - JSON格式存储页面内容（使用TEXT字段），避免1:N关联表的复杂性
 * - 合理的索引设计，优化常见查询性能
 *
 * 性能优化：
 * - 使用COUNT()查询替代totalRecords提高性能
 * - 分页查询支持，避免大结果集内存问题
 * - 缓存参数支持（虽然Ktorm本身不提供查询缓存）
 *
 * @author NewNanCity
 * @since 1.0.0
 */
class MySqlLibrarian(
    private val plugin: ExternalBookPlugin
) : Librarian {

    override val mode: StorageMode = StorageMode.DATABASE

    private var dataSource: HikariDataSource? = null
    private lateinit var database: Database
    private lateinit var bookTable: Books // 配置的表实例
    private val jsonObjectMapper: ObjectMapper by lazy {
        plugin.configManager.getMapper("json")
    }

    /**
     * 重新加载所有书籍数据
     */
    override fun reload() {
        // 创建数据库管理器
        val config = plugin.getPluginConfig()
        val databaseConfig = config.storage.databaseStorage

        // 初始化连接池
        dataSource?.close()
        dataSource = HikariDataSource(config.storage.databaseStorage.toHikariConfig())

        // 创建数据库连接
        database = Database.connect(dataSource!!)

        // 初始化表实例
        val baseTableName = databaseConfig.tableName
        val fullTableName = databaseConfig.tablePrefix + baseTableName
        bookTable = Books(fullTableName, jsonObjectMapper)
        plugin.logger.info("Using table name: $fullTableName (prefix: '${databaseConfig.tablePrefix}', base: '$baseTableName')")

        // 创建表结构
        transaction(database) {
            SchemaUtils.create(bookTable)
        }
    }

    /**
     * 获取UUID对应的ULID
     */
    override fun getUlid(uuid: UUID): Ulid? {
        return transaction(database) {
            bookTable.select(bookTable.id)
                .where { bookTable.uuid eq uuid }
                .withDistinct()
                .firstOrNull()
                ?.let { Ulid.from(it[bookTable.id]) }
        }
    }

    /**
     * 获取书籍
     */
    override operator fun get(bookId: Ulid, cache: Boolean): Book? {
        return try {
            transaction(database) {
                bookTable.selectAll()
                    .where { bookTable.id eq bookId.toString() }
                    .withDistinct()
                    .firstOrNull()
                    ?.let { row ->
                        Book(
                            id = bookId,
                            title = row[bookTable.title],
                            creator = row[bookTable.creator],
                            modifier = row[bookTable.modifier],
                            created = row[bookTable.created].toJavaDate(),
                            modified = row[bookTable.modified].toJavaDate(),
                            pages = row[bookTable.pages]
                        )
                    }
            }
        } catch (e: Exception) {
            plugin.logger.error("Failed to get book: $bookId", e)
            null
        }
    }

    /**
     * 添加或更新书籍
     * 如果书籍已存在，则更新记录并保留原有的uuid字段
     */
    override operator fun plusAssign(book: Book) {
        try {
            val bookIdStr = book.id.toString()

            // 检查书籍是否已存在（使用主键id判断）
            val bookExists = contains(book.id)

            val bookPreview = generatePreview(book.pages)

            if (bookExists) {
                // 更新现有记录，保留原有的uuid
                val updatedCount = transaction(database) {
                    bookTable.update({ bookTable.id eq bookIdStr }, limit = 1) {
                        it[bookTable.title] = book.title
                        it[bookTable.modifier] = book.modifier
                        it[bookTable.modified] = book.modified.toKotlinLocalDate()
                        it[bookTable.pages] = book.pages
                        it[bookTable.preview] = bookPreview
                    }
                }

                if (updatedCount > 0) {
                    plugin.logger.debug("Updated book: ${book.id} - ${book.title}")
                } else {
                    plugin.logger.warn("No rows updated for book: ${book.id}")
                }
            } else {
                // 插入新记录
                transaction(database) {
                    bookTable.insert {
                        it[id] = bookIdStr
                        it[uuid] = null
                        it[title] = book.title
                        it[creator] = book.creator
                        it[modifier] = book.modifier
                        it[created] = book.created.toKotlinLocalDate()
                        it[modified] = book.modified.toKotlinLocalDate()
                        it[pages] = book.pages
                        it[preview] = bookPreview
                    }
                }

                plugin.logger.debug("Added new book: ${book.id} - ${book.title}")
            }

        } catch (e: Exception) {
            plugin.logger.error("Failed to save book: ${book.id} - ${book.title}", e)
            throw e
        }
    }

    /**
     * 删除书籍
     */
    override operator fun minusAssign(bookId: Ulid) {
        try {
            val deletedCount = transaction(database) {
                bookTable.deleteWhere { bookTable.id eq bookId.toString() }
            }

            if (deletedCount > 0) {
                plugin.logger.debug("Removed book: $bookId")
            } else {
                plugin.logger.warn("Book not found for deletion: $bookId")
            }
        } catch (e: Exception) {
            plugin.logger.error("Failed to delete book: $bookId", e)
            throw e
        }
    }

    /**
     * 获取书籍作者
     */
    override fun getBookCreator(bookId: Ulid): UUID? {
        return transaction(database) {
            bookTable.select(bookTable.creator)
                .where { bookTable.id eq bookId.toString() }
                .withDistinct()
                .firstOrNull()
                ?.let { it[bookTable.creator] }
        }
    }

    /**
     * 检查是否包含指定书籍
     */
    override operator fun contains(bookId: Ulid): Boolean {
        return transaction(database) {
            bookTable.select(bookTable.id)
                .where { bookTable.id eq bookId.toString() }
                .withDistinct()
                .firstOrNull()?.let { true } ?: false
        }
    }

    /**
     * 获取玩家的书籍数量
     */
    override fun getPlayerBookCount(playerId: UUID): Int {
        return transaction(database) {
            bookTable.select(bookTable.id)
                .where { bookTable.creator eq playerId }
                .count()
                .toInt()
        }
    }

    /**
     * 分页获取玩家的所有书籍
     */
    override fun getPlayerBooks(playerId: UUID, offset: Int, limit: Int): List<BookAbstract> {
        return try {
            transaction(database) {
                bookTable.select(bookTable.id, bookTable.title, bookTable.creator, bookTable.modifier, bookTable.created, bookTable.modified, bookTable.preview)
                    .where { bookTable.creator eq playerId }
                    .orderBy(bookTable.modified to SortOrder.DESC)
                    .limit(limit)
                    .offset(offset.toLong())
                    .map { row ->
                        BookAbstract(
                            id = Ulid.from(row[bookTable.id]),
                            title = row[bookTable.title],
                            creator = row[bookTable.creator],
                            modifier = row[bookTable.modifier],
                            created = row[bookTable.created].toJavaDate(),
                            modified = row[bookTable.modified].toJavaDate(),
                            preview = row[bookTable.preview]
                        )
                    }
            }
        } catch (e: Exception) {
            plugin.logger.error("Failed to get player books for: $playerId", e)
            emptyList()
        }
    }

    /**
     * 分页获取所有已知的书籍ID
     */
    override fun getAllBookIds(playerId: UUID, offset: Int, limit: Int): List<Ulid> {
        return transaction {
            bookTable.select(bookTable.id)
                .orderBy(bookTable.id to SortOrder.ASC)
                .limit(limit)
                .offset(offset.toLong())
                .map { Ulid.from(it[bookTable.id]) }
        }
    }

    override fun getBookCount(): Int {
        return transaction(database) {
            bookTable.select(bookTable.id)
                .count()
                .toInt()
        }
    }

    /**
     * 获取所有有书籍的玩家ID
     */
    override fun getAuthors(): Set<UUID> {
        return transaction(database) {
            bookTable.select(bookTable.creator)
                .groupBy(bookTable.creator)
                .map { it[bookTable.creator] }
                .toSet()
        }
    }

    override fun close() {
        plugin.logger.info("Shutting down MySqlLibrarian...")
        dataSource?.close()
        dataSource = null
    }

    /**
     * 生成书籍预览文本
     */
    private fun generatePreview(pages: List<String>): String {
        return pages.firstOrNull()?.let { page ->
            page.substring(0, minOf(36, page.length)).replace('\n', ' ')
        } ?: ""
    }
}
