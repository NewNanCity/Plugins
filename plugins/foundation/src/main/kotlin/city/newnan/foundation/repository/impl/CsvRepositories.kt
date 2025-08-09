package city.newnan.foundation.repository.impl

import city.newnan.core.base.BaseModule
import city.newnan.core.scheduler.ITaskHandler
import city.newnan.core.scheduler.runAsync
import city.newnan.core.scheduler.runAsyncRepeating
import city.newnan.foundation.FoundationPlugin
import city.newnan.foundation.config.*
import city.newnan.foundation.manager.TransferManager
import city.newnan.foundation.repository.TransferRecordRepository
import city.newnan.foundation.repository.AllocationLogRepository
import city.newnan.foundation.repository.PageResult
import city.newnan.foundation.repository.TransferRankEntry
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import java.io.File
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * CSV格式的转账记录存储库实现
 */
class CsvTransferRecordRepository(
    private val transferManager: TransferManager,
    private val plugin: FoundationPlugin
) : TransferRecordRepository, BaseModule("CsvTransferRecordRepository", transferManager) {

    private var saveFile = File(plugin.dataFolder, "data.csv")
    private var autoSaveIntervalTicks = 1200L
    private var rankingCacheExpireMilliseconds = 1800000L

    // 批处理保存任务处理器
    private var batchSaveTask: ITaskHandler<Unit>? = null

    // CSV记录的字符串读写器
    private val recordsStrReader: ObjectReader
    private val recordsStrWriter: ObjectWriter

    // 内存缓存，在初始化时加载所有数据
    private val records = ConcurrentHashMap<UUID, TransferRecord>()
    // 脏标记
    private var dirty = false

    // 排行榜缓存相关字段
    @Volatile
    private var cachedRankings: List<TransferRankEntry>? = null

    @Volatile
    private var rankingCacheTimestamp: Long = 0

    init {
        val mapper = CsvMapper()
        val transferSchema = mapper.schemaFor(TransferRecordStr::class.java).withHeader()
        recordsStrReader = mapper.readerFor(TransferRecordStr::class.java).with(transferSchema)
        recordsStrWriter = mapper.writer(transferSchema)

        init()
    }

    override fun onReload() {
        // 取消定时任务
        batchSaveTask?.cancel(true)

        val config = plugin.getPluginConfig()
        saveFile = File(plugin.dataFolder, config.dataStorage.fileStorage.dataFile)
        autoSaveIntervalTicks = config.dataStorage.fileStorage.autoSaveIntervalTicks
        rankingCacheExpireMilliseconds = config.dataStorage.fileStorage.rankingCacheExpireMilliseconds

        // 清除排行榜缓存
        invalidateRankingCache()

        // 重新加载数据
        loadDataToMemory()

        // 启动批量保存定时任务，每20tick（1秒）执行一次
        batchSaveTask = runAsyncRepeating(autoSaveIntervalTicks, autoSaveIntervalTicks) {
            saveBatch()
            true // 持续运行
        }
    }

    override fun onClose() {
        // 取消定时任务
        batchSaveTask?.cancel()

        // 强制同步保存所有待写入的变更，确保数据不丢失
        saveBatch()
    }

    // ===== 接口方法实现（基于内存缓存） =====

    override fun getRecord(uuid: UUID): TransferRecord? {
        plugin.logger.debug("Getting record for UUID: $uuid")
        return records[uuid]
    }

    override fun getAllRecords(offset: Int, limit: Int): PageResult<TransferRecord> {
        plugin.logger.debug("Getting all records - offset: $offset, limit: $limit")
        val recordsList = records.values.toList()
        return paginateResults(recordsList, offset, limit)
    }

    override fun getTotalDonations(): Pair<BigDecimal, BigDecimal> {
        val totalActive = records.values.sumOf { it.active }
        val totalPassive = records.values.sumOf { it.passive }
        return totalActive to totalPassive
    }

    override fun getTopDonors(offset: Int, limit: Int): PageResult<TransferRankEntry> {
        plugin.logger.debug("Getting top donors - offset: $offset, limit: $limit")
        val rankEntries = getCachedRankings()
        return paginateResults(rankEntries, offset, limit)
    }

    override fun getRecordCount(): Int {
        plugin.logger.debug("Getting record count")
        return records.size
    }

    override fun updateActiveRecord(uuid: UUID, delta: BigDecimal) {
        plugin.logger.debug("Updating active record for UUID: $uuid, delta: $delta")

        // 直接更新内存数据
        val existing = records[uuid] ?: TransferRecord(uuid, BigDecimal.ZERO, BigDecimal.ZERO)
        records[uuid] = existing.copy(active = existing.active + delta)
        dirty = true

        // 主动捐款低频且额度大，直接更新
        saveBatch()
    }

    override fun updatePassiveRecord(uuid: UUID, delta: BigDecimal) {
        plugin.logger.debug("Updating passive record for UUID: $uuid, delta: $delta")

        // 直接更新内存数据
        val existing = records[uuid] ?: TransferRecord(uuid, BigDecimal.ZERO, BigDecimal.ZERO)
        records[uuid] = existing.copy(passive = existing.passive + delta)
        dirty = true
    }

    override fun hasRecord(uuid: UUID): Boolean {
        plugin.logger.debug("Checking if record exists for UUID: $uuid")
        return records.containsKey(uuid)
    }

    // ===== 批处理实现 =====

    /**
     * 执行批量保存操作
     */
    private fun saveBatch() {
        if (!dirty) return

        plugin.logger.debug("Saving records from memory cache to file")

        if (!plugin.dataFolder.exists()) {
            plugin.dataFolder.mkdirs()
        }

        try {
            if (records.isEmpty()) {
                // 如果没有记录，创建空文件或写入CSV头部
                saveFile.writeText("id,active,passive\n")
                return
            }

            val recordsStrList = records.values.map { record ->
                TransferRecordStr(
                    id = record.id.toString(),
                    active = record.active.toString(),
                    passive = record.passive.toString()
                )
            }

            // 使用Jackson CSV writer保存
            recordsStrWriter.writeValue(saveFile, recordsStrList)
            plugin.logger.debug("Successfully saved ${records.size} records to file")
            dirty = false

            // 清除排行榜缓存，因为数据已更新
            invalidateRankingCache()
        } catch (e: Exception) {
            plugin.logger.error("Failed to save records to file", e)
            throw e
        }
    }

    // ===== 私有辅助方法 =====

    /**
     * 从文件加载数据到内存缓存
     */
    private fun loadDataToMemory() {
        if (!saveFile.exists()) {
            plugin.logger.info("Data file does not exist, starting with empty records")
            return
        }

        plugin.logger.info("Loading data from: ${saveFile.absolutePath}")

        try {
            val loadedRecords = recordsStrReader.readValues<TransferRecordStr>(saveFile).readAll()
            records.clear()
            dirty = false

            loadedRecords.forEach { recordStr ->
                // 将字符串记录转换为内存中的记录
                val record = TransferRecord(
                    id = UUID.fromString(recordStr.id),
                    active = BigDecimal(recordStr.active),
                    passive = BigDecimal(recordStr.passive)
                )
                records[record.id] = record
            }

            plugin.logger.info("Data loaded successfully to memory cache: ${records.size} records")

            // 详细记录加载的数据
            records.forEach { (uuid, record) ->
                plugin.logger.info("Loaded record - UUID: $uuid, Active: ${record.active}, Passive: ${record.passive}")
            }
        } catch (e: Exception) {
            plugin.logger.error("Failed to load transfer data to memory", e)
            records.clear()
        }
    }

    /**
     * 清除排行榜缓存
     */
    private fun invalidateRankingCache() {
        plugin.logger.debug("Invalidating rankings cache")
        cachedRankings = null
        rankingCacheTimestamp = 0
    }

    /**
     * 通用分页方法
     */
    private fun <T> paginateResults(items: List<T>, offset: Int, limit: Int): PageResult<T> {
        val totalCount = items.size.toLong()
        val validOffset = offset.coerceAtLeast(0)
        val validLimit = limit.coerceAtLeast(1)

        val startIndex = validOffset
        val endIndex = (startIndex + validLimit).coerceAtMost(items.size)

        val pageItems = if (startIndex < items.size) {
            items.subList(startIndex, endIndex)
        } else {
            emptyList()
        }

        return PageResult(
            items = pageItems,
            totalCount = totalCount,
            offset = validOffset,
            limit = validLimit
        )
    }

    /**
     * 获取缓存的排行榜数据
     */
    private fun getCachedRankings(): List<TransferRankEntry> {
        val currentTime = System.currentTimeMillis()

        // 检查缓存是否有效
        if (cachedRankings != null &&
            currentTime - rankingCacheTimestamp < rankingCacheExpireMilliseconds) {
            plugin.logger.debug("Using cached rankings (age: ${currentTime - rankingCacheTimestamp}ms)")
            return cachedRankings!!
        }

        // 缓存已过期或不存在，重新生成
        plugin.logger.debug("Regenerating rankings cache...")
        val rankEntries = records.values.map { record ->
            TransferRankEntry(
                uuid = record.id,
                active = record.active,
                passive = record.passive,
                total = record.active + record.passive
            )
        }.sortedByDescending { it.total }

        // 更新缓存
        cachedRankings = rankEntries
        rankingCacheTimestamp = currentTime

        plugin.logger.debug("Rankings cache updated with ${rankEntries.size} entries")
        return rankEntries
    }
}

/**
 * CSV格式的拨款日志存储库实现
 */
class CsvAllocationLogRepository(
    private val transferManager: TransferManager,
    private val plugin: FoundationPlugin
) : AllocationLogRepository, BaseModule("CsvAllocationLogRepository", transferManager) {

    // 日期格式化器 - 使用ISO 8601标准格式
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    // 文件写入锁，确保并发安全
    private val fileLock = Any()

    private var saveFile: File = File(plugin.dataFolder, "allocate.csv")

    override fun onReload() {
        val config = plugin.getPluginConfig()
        saveFile = File(plugin.dataFolder, config.dataStorage.fileStorage.allocateLogFile)
    }

    override fun appendAllocation(record: AllocationLogRecord) {
        try {
            // 异步执行文件写入，但使用同步锁确保线程安全
            runAsync {
                synchronized(fileLock) {
                    // 确保目录存在
                    if (!plugin.dataFolder.exists()) {
                        plugin.dataFolder.mkdirs()
                    }

                    // 检查文件是否存在，如果不存在则创建并写入头部
                    val isNewFile = !saveFile.exists()
                    if (isNewFile) {
                        // 创建新文件并写入CSV头部
                        saveFile.bufferedWriter().use { writer ->
                            writer.write("date,who,target,amount,reason\n")
                            writer.write(buildCsvLine(record))
                        }
                        plugin.logger.info("Created new allocation log file: ${saveFile.absolutePath}")
                    } else {
                        // 使用追加模式写入，避免读取整个文件
                        saveFile.appendText(buildCsvLine(record))
                    }

                    plugin.logger.info("ALLOCATION LOGGED TO CSV: $record")
                }
            }

        } catch (e: Exception) {
            plugin.logger.error("Failed to write allocation log to CSV: ${e.message}", e)

            // 降级到文本日志
            try {
                val logEntry = "[${LocalDateTime.now().format(dateFormatter)}] ${record.who} -> ${record.target}: ${record.amount} (Reason: ${record.reason})"
                val fallbackFile = File(plugin.dataFolder, "allocations.log")

                // 降级日志也需要同步
                synchronized(fileLock) {
                    if (!fallbackFile.exists()) {
                        fallbackFile.parentFile?.mkdirs()
                        fallbackFile.createNewFile()
                    }
                    fallbackFile.appendText("$logEntry\n")
                }
                plugin.logger.info("ALLOCATION LOGGED TO FALLBACK: $logEntry")
            } catch (fallbackException: Exception) {
                plugin.logger.error("Failed to write allocation log to fallback file: ${fallbackException.message}", fallbackException)
            }
        }
    }

    /**
     * 构建CSV行
     */
    private fun buildCsvLine(record: AllocationLogRecord): String {
        return buildString {
            append("\"${record.date}\",")
            append("\"${record.who}\",")
            append("\"${record.target}\",")
            append("${record.amount},")
            append("\"${record.reason.replace("\"", "\"\"")}\"\n") // 转义双引号
        }
    }

    /**
     * 获取总拨款金额
     *
     * @return 总拨款金额
     */
    override fun getTotalAllocation(): BigDecimal {
        // 流式读取文件，避免加载整个文件到内存
        // 先打开文件
        return saveFile.bufferedReader().use { reader ->
            // 跳过头部
            reader.readLine()
            // 读取每一行并累加
            reader.lineSequence().fold(BigDecimal.ZERO) { acc, line ->
                acc + line.split(",")[3].toBigDecimal()
            }
        }
    }
}