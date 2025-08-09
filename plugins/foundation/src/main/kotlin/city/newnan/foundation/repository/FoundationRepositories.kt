package city.newnan.foundation.repository

import city.newnan.foundation.config.TransferRecord
import city.newnan.foundation.config.AllocationLogRecord
import java.math.BigDecimal
import java.util.*

/**
 * 分页结果数据类
 */
data class PageResult<T>(
    val items: List<T>,
    val totalCount: Long,
    val offset: Int,
    val limit: Int
) {
    val hasNext: Boolean get() = offset + limit < totalCount
    val hasPrevious: Boolean get() = offset > 0
}

/**
 * 排行榜条目数据类
 */
data class TransferRankEntry(
    val uuid: UUID,
    val active: BigDecimal,
    val passive: BigDecimal,
    val total: BigDecimal = active + passive
)

/**
 * 转账记录存储库接口
 *
 * 负责转账记录的持久化操作，支持分页查询和数据库扩展
 */
interface TransferRecordRepository {
    /**
     * 获取特定账户的转账记录
     *
     * @param uuid 账户UUID
     * @return 转账记录，如果不存在则返回null
     */
    fun getRecord(uuid: UUID): TransferRecord?

    /**
     * 获取所有转账记录（分页）
     *
     * @param offset 偏移量（从0开始）
     * @param limit 限制数量
     * @return 分页结果
     */
    fun getAllRecords(offset: Int = 0, limit: Int = 50): PageResult<TransferRecord>

    /**
     * 获取转账记录之和
     *
     * @return 主动转账总额和被动转账总额
     */
    fun getTotalDonations(): Pair<BigDecimal, BigDecimal>

    /**
     * 获取排行榜（按总捐款金额降序）
     *
     * @param offset 偏移量（从0开始）
     * @param limit 限制数量
     * @return 分页的排行榜结果
     */
    fun getTopDonors(offset: Int = 0, limit: Int = 50): PageResult<TransferRankEntry>

    /**
     * 获取转账记录总数
     *
     * @return 记录总数
     */
    fun getRecordCount(): Int

    /**
     * 更新主动转账记录
     *
     * @param uuid 账户UUID
     * @param delta 增加的金额
     */
    fun updateActiveRecord(uuid: UUID, delta: BigDecimal)

    /**
     * 更新被动转账记录
     *
     * @param uuid 账户UUID
     * @param delta 增加的金额
     */
    fun updatePassiveRecord(uuid: UUID, delta: BigDecimal)

    /**
     * 检查账户是否存在记录
     *
     * @param uuid 账户UUID
     * @return 是否存在记录
     */
    fun hasRecord(uuid: UUID): Boolean

    /**
     * 重新加载数据
     * 从存储介质重新加载所有数据到内存缓存
     */
    fun reload()
}

/**
 * 拨款日志存储库接口
 *
 * 负责拨款日志的追加写入操作
 */
interface AllocationLogRepository {
    /**
     * 追加拨款记录到日志
     *
     * @param record 拨款记录
     */
    fun appendAllocation(record: AllocationLogRecord)

    /**
     * 获取总拨款金额
     *
     * @return 总拨款金额
     */
    fun getTotalAllocation(): BigDecimal
}
