package city.newnan.foundation.repository.impl

import city.newnan.core.base.BaseModule
import city.newnan.core.scheduler.ITaskHandler
import city.newnan.core.scheduler.runAsync
import city.newnan.core.scheduler.runAsyncRepeating
import city.newnan.foundation.FoundationPlugin
import city.newnan.foundation.config.*
import city.newnan.foundation.manager.TransferManager
import city.newnan.foundation.repository.*
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 转账记录数据表结构
 *
 * 使用UUID作为主键，存储玩家的主动、被动和总转账金额
 * 总捐赠额字段用于排序优化，避免每次查询时计算
 */
class TransferRecordsTable(tableName: String) : UUIDTable(tableName) {
    val active = decimal("active", 19, 2).default(BigDecimal.ZERO)
    val passive = decimal("passive", 19, 2).default(BigDecimal.ZERO)
    /* 总捐赠额字段，用于排序优化 */
    val total = decimal("total", 19, 2).default(BigDecimal.ZERO).index(isUnique = false, customIndexName = "idx_transfer_total")
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
}

/**
 * 拨款日志数据表结构
 *
 * 存储所有拨款操作的日志记录，支持审计追踪
 */
class AllocationLogsTable(tableName: String) : IntIdTable(tableName) {
    val who = varchar("who", 64).index(isUnique = false, customIndexName = "idx_allocation_who")
    val target = uuid("target").index(isUnique = false, customIndexName = "idx_allocation_target")
    val amount = decimal("amount", 19, 2)
    val reason = text("reason")
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime).index(isUnique = false, customIndexName = "idx_allocation_created")
}

/**
 * 数据库存储库实现
 *
 * 合并TransferRecordRepository和AllocationLogRepository接口的数据库实现
 * 使用同一个连接池管理数据库连接，支持批量保存和异步操作
 */
class DatabaseRepositories(
    private val transferManager: TransferManager,
    private val plugin: FoundationPlugin
) : TransferRecordRepository, AllocationLogRepository, BaseModule("DatabaseRepositories", transferManager) {

    private var dataSource: HikariDataSource? = null
    private lateinit var database: Database
    private lateinit var transferTable: TransferRecordsTable
    private lateinit var allocationTable: AllocationLogsTable

    // 批量保存相关字段
    private var batchSaveIntervalTicks = 200L
    private val activeDeltas = ConcurrentHashMap<UUID, BigDecimal>()
    private val passiveDeltas = ConcurrentHashMap<UUID, BigDecimal>()
    private var batchSaveTask: ITaskHandler<Unit>? = null

    // 日期格式化器
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    init {
        init()
    }

    override fun onInit() {
        reload()
    }

    override fun onReload() {
        val config = plugin.getPluginConfig()
        val databaseConfig = config.dataStorage.databaseStorage

        // 更新配置
        batchSaveIntervalTicks = databaseConfig.batchSaveIntervalTicks

        // 初始化连接池
        dataSource?.close()
        dataSource = HikariDataSource(databaseConfig.toHikariConfig())

        // 创建数据库连接
        database = Database.connect(dataSource!!)

        // 初始化表实例
        val transferTableName = databaseConfig.tablePrefix + databaseConfig.transferTableName
        val allocationTableName = databaseConfig.tablePrefix + databaseConfig.allocationTableName
        transferTable = TransferRecordsTable(transferTableName)
        allocationTable = AllocationLogsTable(allocationTableName)

        plugin.logger.info("Using transfer table: $transferTableName")
        plugin.logger.info("Using allocation table: $allocationTableName")

        // 创建表结构
        transaction(database) {
            SchemaUtils.create(transferTable, allocationTable)
            plugin.logger.info("Database tables initialized")
        }

        // 启动批量保存定时任务
        batchSaveTask?.cancel(true)
        batchSaveTask = runAsyncRepeating(batchSaveIntervalTicks, batchSaveIntervalTicks) {
            flushBatch()
            true // 持续运行
        }
    }

    override fun onClose() {
        // 取消定时任务
        batchSaveTask?.cancel()

        // 强制同步保存所有待写入的变更
        flushBatch()

        // 关闭数据源
        dataSource?.close()
        dataSource = null
    }

    // ===== TransferRecordRepository 接口实现 =====

    override fun getRecord(uuid: UUID): TransferRecord? = transaction(database) {
        transferTable.selectAll()
            .where { transferTable.id eq uuid }
            .withDistinct()
            .firstOrNull()
            ?.run {
                val id = this[transferTable.id].value
                TransferRecord(
                    id = id,
                    active = this[transferTable.active] + (activeDeltas[id] ?: BigDecimal.ZERO),
                    passive = this[transferTable.passive] + (passiveDeltas[id] ?: BigDecimal.ZERO)
                )
            }
    }

    override fun getAllRecords(offset: Int, limit: Int): PageResult<TransferRecord> = transaction(database) {
        val totalCount = transferTable.selectAll().count()
        val records = transferTable.selectAll()
            .limit(limit)
            .offset(offset.toLong())
            .map { row ->
                val id = row[transferTable.id].value
                TransferRecord(
                    id = id,
                    active = row[transferTable.active] + (activeDeltas[id] ?: BigDecimal.ZERO),
                    passive = row[transferTable.passive] + (passiveDeltas[id] ?: BigDecimal.ZERO)
                )
            }

        PageResult(
            items = records,
            totalCount = totalCount,
            offset = offset,
            limit = limit
        )
    }

    override fun getTotalDonations(): Pair<BigDecimal, BigDecimal> {
        return transaction(database) {
            var totalActive = transferTable
                .select(transferTable.active.sum())
                .first()[transferTable.active.sum()] ?: BigDecimal.ZERO

            var totalPassive = transferTable
                .select(transferTable.passive.sum())
                .first()[transferTable.passive.sum()] ?: BigDecimal.ZERO

            activeDeltas.values.forEach { totalActive += it }
            passiveDeltas.values.forEach { totalPassive += it }

            totalActive to totalPassive
        }
    }

    override fun getTopDonors(offset: Int, limit: Int): PageResult<TransferRankEntry> {
        return transaction(database) {
            val totalCount = transferTable.selectAll().count()
            val rankEntries = transferTable.selectAll()
                .orderBy(transferTable.total to SortOrder.DESC)
                .limit(limit)
                .offset(offset.toLong())
                .map { row ->
                    val id = row[transferTable.id].value
                    TransferRankEntry(
                        uuid = id,
                        active = row[transferTable.active] + (activeDeltas[id] ?: BigDecimal.ZERO),
                        passive = row[transferTable.passive] + (passiveDeltas[id] ?: BigDecimal.ZERO),
                        total = row[transferTable.total]
                    )
                }

            PageResult(
                items = rankEntries,
                totalCount = totalCount,
                offset = offset,
                limit = limit
            )
        }
    }

    override fun getRecordCount(): Int {
        return transaction(database) {
            transferTable.selectAll().count().toInt()
        }
    }

    override fun updateActiveRecord(uuid: UUID, delta: BigDecimal) {
        plugin.logger.debug("Queuing active record update for $uuid: +$delta")
        // 合并增量变更到批处理缓存
        activeDeltas.merge(uuid, delta, BigDecimal::add)
        // 主动捐款低频且额度大，直接更新
        flushBatch()
    }

    override fun updatePassiveRecord(uuid: UUID, delta: BigDecimal) {
        plugin.logger.debug("Queuing passive record update for $uuid: +$delta")
        // 合并增量变更到批处理缓存
        passiveDeltas.merge(uuid, delta, BigDecimal::add)
    }

    override fun hasRecord(uuid: UUID): Boolean {
        return transaction(database) {
            transferTable.selectAll()
                .where { transferTable.id eq uuid }
                .count() > 0
        }
    }

    // ===== AllocationLogRepository 接口实现 =====

    override fun appendAllocation(record: AllocationLogRecord) {
        runAsync {
            try {
                transaction(database) {
                    allocationTable.insert {
                        it[who] = record.who
                        it[target] = record.target
                        it[amount] = BigDecimal(record.amount)
                        it[reason] = record.reason
                    }
                }

                plugin.logger.info("ALLOCATION LOGGED TO DATABASE: $record")
            } catch (e: Exception) {
                plugin.logger.error("Failed to log allocation to database: ${e.message}", e)

                // 降级到内存日志
                val logEntry = "[${LocalDateTime.now().format(dateFormatter)}] ${record.who} -> ${record.target}: ${record.amount} (Reason: ${record.reason})"
                plugin.logger.warn("ALLOCATION LOGGED TO FALLBACK: $logEntry")
            }
        }
    }

    /**
     * 获取总拨款金额
     *
     * @return 总拨款金额
     */
    override fun getTotalAllocation(): BigDecimal {
        return transaction(database) {
            allocationTable.select(allocationTable.amount.sum())
                .first()[allocationTable.amount.sum()] ?: BigDecimal.ZERO
        }
    }

    // ===== 批量保存实现 =====

    /**
     * 执行批量保存操作
     * 使用REPEATABLE_READ事务隔离级别防止脏读，确保数据一致性
     */
    private fun flushBatch() {
        if (activeDeltas.isEmpty() && passiveDeltas.isEmpty()) {
            return
        }

        plugin.logger.debug("Flushing batch save - Active deltas: ${activeDeltas.size}, Passive deltas: ${passiveDeltas.size}")

        // 原子性地获取当前的变更并清空缓存
        val activeDeltasCopy = activeDeltas.toMap()
        val passiveDeltasCopy = passiveDeltas.toMap()
        activeDeltas.clear()
        passiveDeltas.clear()

        try {
            transaction(database) {
                // 处理所有受影响的UUID - 使用一个事务确保数据一致性
                (activeDeltasCopy.keys + passiveDeltasCopy.keys).forEach { uuid ->
                    val activeDelta = activeDeltasCopy[uuid] ?: BigDecimal.ZERO
                    val passiveDelta = passiveDeltasCopy[uuid] ?: BigDecimal.ZERO

                    // 使用 SELECT FOR UPDATE 锁定行，防止并发修改
                    val existing = transferTable.selectAll()
                        .where { transferTable.id eq uuid }
                        .forUpdate()
                        .firstOrNull()

                    if (existing != null) {
                        // 更新现有记录 - 在原始值基础上增加增量
                        val currentActive = existing[transferTable.active]
                        val currentPassive = existing[transferTable.passive]
                        val newActive = currentActive + activeDelta
                        val newPassive = currentPassive + passiveDelta
                        val newTotal = newActive + newPassive

                        transferTable.update({ transferTable.id eq uuid }) {
                            it[active] = newActive
                            it[passive] = newPassive
                            it[total] = newTotal
                            it[updatedAt] = javaLocalDateTime2Kotlin(LocalDateTime.now())
                        }

                        plugin.logger.debug("Updated record for $uuid: active($currentActive + $activeDelta = $newActive), passive($currentPassive + $passiveDelta = $newPassive)")
                    } else {
                        // 创建新记录
                        val newTotal = activeDelta + passiveDelta
                        transferTable.insert {
                            it[id] = uuid
                            it[active] = activeDelta
                            it[passive] = passiveDelta
                            it[total] = newTotal
                        }

                        plugin.logger.debug("Created new record for $uuid: active($activeDelta), passive($passiveDelta), total($newTotal)")
                    }
                }
            }

            plugin.logger.debug("Batch save transaction completed successfully")
        } catch (e: Exception) {
            plugin.logger.error("Failed to perform batch save transaction", e)
            
            // 将失败的变更重新放回缓存，避免数据丢失
            activeDeltasCopy.forEach { (uuid, delta) ->
                activeDeltas.merge(uuid, delta, BigDecimal::add)
            }
            passiveDeltasCopy.forEach { (uuid, delta) ->
                passiveDeltas.merge(uuid, delta, BigDecimal::add)
            }
        }
    }
}

fun javaLocalDateTime2Kotlin(dateTime: LocalDateTime): kotlinx.datetime.LocalDateTime {
    return kotlinx.datetime.LocalDateTime(
        dateTime.year,
        dateTime.monthValue,
        dateTime.dayOfMonth,
        dateTime.hour,
        dateTime.minute,
        dateTime.second,
        dateTime.nano
    )
}