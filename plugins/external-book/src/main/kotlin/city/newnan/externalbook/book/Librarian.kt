package city.newnan.externalbook.book

import city.newnan.core.terminable.Terminable
import city.newnan.externalbook.config.StorageMode
import com.github.f4b6a3.ulid.Ulid
import com.github.f4b6a3.ulid.UlidCreator
import java.util.*

/**
 * 图书管理员接口 - 负责管理所有书籍的存储和检索
 *
 * 提供书籍的增删改查功能，支持分页查询和统计信息。
 * 实现类需要处理具体的存储逻辑（如文件系统、数据库等）。
 *
 * @author NewNanCity
 * @since 1.0.0
 */
interface Librarian : Terminable {
    /**
     * 实现名称
     */
    val mode: StorageMode

    /**
     * 重新加载所有书籍数据
     */
    fun reload()

    /**
     * 获取UUID对应的ULID
     */
    fun getUlid(uuid: UUID): Ulid?

    /**
     * 获取指定ID的书籍
     *
     * @param bookId 书籍ID
     * @param cache 是否使用缓存
     * @return 书籍对象，如果不存在则返回null
     */
    operator fun get(bookId: Ulid, cache: Boolean = true): Book?

    /**
     * 添加书籍
     *
     * @param idAndBook 书籍ID和书籍对象的配对
     */
    operator fun plusAssign(book: Book)

    /**
     * 删除书籍
     *
     * @param bookId 要删除的书籍ID
     */
    operator fun minusAssign(bookId: Ulid)

    /**
     * 检查是否包含指定书籍
     *
     * @param bookId 书籍ID
     * @return 是否包含该书籍
     */
    operator fun contains(bookId: Ulid): Boolean

    /**
     * 分页获取指定玩家的书籍ID列表
     *
     * @param playerId 玩家ID
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 书籍ID列表
     */
    fun getPlayerBooks(playerId: UUID, offset: Int, limit: Int): List<BookAbstract>

    /**
     * 获取指定玩家的书籍总数
     *
     * @param playerId 玩家ID
     * @return 书籍总数
     */
    fun getPlayerBookCount(playerId: UUID): Int

    /**
     * 分页获取所有已知的书籍ID
     *
     * @param playerId 玩家ID（用于权限检查）
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 书籍ID列表
     */
    fun getAllBookIds(playerId: UUID, offset: Int, limit: Int): List<Ulid>

    /**
     * 获取所有书籍总数
     *
     * @return 书籍总数
     */
    fun getBookCount(): Int

    /**
     * 获取所有有作者的ID
     *
     * @return 玩家ID集合
     */
    fun getAuthors(): Set<UUID>

    /**
     * 获取书籍作者
     */
    fun getBookCreator(bookId: Ulid): UUID?

    /**
     * 获取下一个可用的书籍ID
     */
    fun getNextAvailableUUID(): Ulid {
        return UlidCreator.getMonotonicUlid()
    }
}