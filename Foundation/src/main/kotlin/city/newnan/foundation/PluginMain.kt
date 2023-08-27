package city.newnan.foundation

import city.newnan.foundation.config.*
import city.newnan.violet.config.ConfigManager2
import city.newnan.violet.gui.GuiManager
import city.newnan.violet.message.MessageManager
import co.aikar.commands.PaperCommandManager
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.plugin.ExtendedJavaPlugin
import net.ess3.api.events.UserBalanceUpdateEvent
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.event.EventPriority
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayDeque

data class TransferOther(val account: OfflinePlayer, val amount: BigDecimal, var time: Long, val rounded: BigDecimal)
data class TransferSelf(val amount: BigDecimal, var time: Long, val rounded: BigDecimal)

class PluginMain : ExtendedJavaPlugin() {
    companion object {
        lateinit var INSTANCE: PluginMain
            private set
    }

    private val configManager: ConfigManager2 by lazy {
        ConfigManager2(this).apply {
            setCache(ConfigManager2.CacheType.None)
        }
    }
    val guiManager: GuiManager by lazy { GuiManager(this) }
    val messageManager: MessageManager by lazy { MessageManager(this) }
    private val commandManager: PaperCommandManager by lazy { PaperCommandManager(this) }
    lateinit var economy: Economy
    var targetAccount: OfflinePlayer? = null
    private val otherTransfers = ArrayDeque<TransferOther>()
    private val selfTransfers = ArrayDeque<TransferSelf>()
    private val bypassTransfer1 = mutableSetOf<Pair<UUID, BigDecimal>>()
    private val bypassTransfer2 = mutableSetOf<BigDecimal>()
    private val expireMilliseconds = TimeUnit.SECONDS.toMillis(5)
    private var patchActiveTransferMap = mutableMapOf<UUID, BigDecimal>()
    private var patchPassiveTransferMap = mutableMapOf<UUID, BigDecimal>()
    private var topCache: List<RecordDisplay>? = null
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    private val recordsStrReader: ObjectReader
    private val recordsStrWriter: ObjectWriter
    private val recordsReader: ObjectReader
    private val allocationStrWriter: ObjectWriter

    init {
        INSTANCE = this
        val mapper = ConfigManager2.mapper[ConfigManager2.ConfigFileType.Csv] as CsvMapper
        val schema = mapper.schemaFor(RecordStr::class.java).withHeader()
        recordsStrReader = mapper.readerFor(RecordStr::class.java).with(schema)
        recordsStrWriter = mapper.writer(schema)
        recordsReader = mapper.readerFor(RecordDouble::class.java).with(mapper.schemaFor(RecordDouble::class.java).withHeader())
        allocationStrWriter = mapper.writer(mapper.schemaFor(AllocateStr::class.java))
        dateFormatter.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    }

    override fun enable() {
        if (server.pluginManager.getPlugin("Vault") == null) {
            throw Exception("Vault not found!")
        }
        economy = server.servicesManager.getRegistration(Economy::class.java)?.provider
            ?: throw Exception("Vault economy service not found!")

        reload()
        messageManager setPlayerPrefix "§7[§6牛腩基金§7] §f"

        // 初始化CommandManager
        commandManager.enableUnstableAPI("help")
        commandManager.registerCommand(Commands)
        commandManager.locales.setDefaultLocale(Locale.SIMPLIFIED_CHINESE)

        Events.subscribe(UserBalanceUpdateEvent::class.java, EventPriority.MONITOR)
            .filter { targetAccount != null }
            .handler { event ->
                // 清理过期内容
                val now = System.currentTimeMillis()
                val expired = now - expireMilliseconds
                while (!otherTransfers.isEmpty() && otherTransfers.first().time < expired)
                    otherTransfers.removeFirst()
                while (!selfTransfers.isEmpty() && selfTransfers.first().time < expired)
                    selfTransfers.removeFirst()
                if (event.player.uniqueId == targetAccount!!.uniqueId) {
                    // 流入是别人的流出
                    val amount = event.newBalance - event.oldBalance
                    val rounded = amount.setScale(2, RoundingMode.HALF_UP)
                    if (bypassTransfer2.remove(rounded)) return@handler
                    // 查看转入交易中有没有金额一致的
                    val transfer = otherTransfers.find { t -> t.rounded == rounded && t.time >= expired }
                    if (transfer == null) {
                        selfTransfers.add(TransferSelf(amount, now, rounded))
                    } else {
                        transfer.time = 0
                        passiveTransfer(transfer.account, amount)
                    }
                } else {
                    // 流出是别人的流入
                    val amount = event.oldBalance - event.newBalance
                    val rounded = amount.setScale(2, RoundingMode.HALF_UP)
                    if (bypassTransfer1.remove(event.player.uniqueId to rounded)) return@handler
                    // 查看转出交易中有没有金额一致的
                    val transfer = selfTransfers.find { t -> t.rounded == rounded && t.time >= expired }
                    if (transfer == null) {
                        otherTransfers.add(TransferOther(event.player, amount, now, rounded))
                    } else {
                        transfer.time = 0
                        passiveTransfer(event.player, amount)
                    }
                }
            }.bindWith(this)

        Schedulers.async().runRepeating({ _ -> save() }, 1200L, 1200L).bindWith(this)
    }

    private fun save() {
        if (patchActiveTransferMap.isEmpty() && patchPassiveTransferMap.isEmpty()) return
        val activeT = patchActiveTransferMap
        val passiveT = patchPassiveTransferMap
        patchActiveTransferMap = mutableMapOf()
        patchPassiveTransferMap = mutableMapOf()
        val active = activeT.mapKeysTo(mutableMapOf()) { it.key.toString() }
        val passive = passiveT.mapKeysTo(mutableMapOf())  { it.key.toString() }
        // Fast Patch Update without UUID/BigDecimal Parse
        val it = recordsStrReader.readValues<RecordStr>(File(dataFolder, "data.csv"))
        val records = mutableListOf<RecordStr>()
        it.forEach { record ->
            if (active.containsKey(record.id)) {
                if (passive.containsKey(record.id)) {
                    records.add(RecordStr(record.id, (active[record.id]!! + BigDecimal(record.active)).toString(),
                        (passive[record.id]!! + BigDecimal(record.passive)).toString()))
                    passive.remove(record.id)
                } else {
                    records.add(RecordStr(record.id, (active[record.id]!! + BigDecimal(record.active)).toString(), record.passive))
                }
                active.remove(record.id)
            } else {
                if (passive.containsKey(record.id)) {
                    records.add(RecordStr(record.id, record.active, (passive[record.id]!! + BigDecimal(record.passive)).toString()))
                    passive.remove(record.id)
                } else {
                    records.add(record)
                }
            }
        }
        active.forEach { (id, amount) ->
            if (passive.containsKey(id)) {
                records.add(RecordStr(id, amount.toString(), passive[id]!!.toString()))
                passive.remove(id)
            } else {
                records.add(RecordStr(id, amount.toString(), "0"))
            }
        }
        passive.forEach { (id, amount) ->
            records.add(RecordStr(id, "0", amount.toString()))
        }
        recordsStrWriter.writeValue(File(dataFolder, "data.csv"), records)
    }

    fun reload() {
        configManager touch "config.yml"
        configManager.parse<ConfigFile>("config.yml").also {
            targetAccount = it.target?.let { name ->
                if (name.isBlank()) return@let null
                Bukkit.getOfflinePlayers().find { p -> p.name == name }?.also { p ->
                    messageManager.info("设置基金账户为: ${p.name}")
                }
            }
            if (targetAccount == null) {
                messageManager.warn("基金账户未设置, 插件不会工作!")
            }
        }
        configManager touch "data.csv"
    }

    override fun disable() {
        commandManager.unregisterCommands()
        save()
    }

    // 基金会 -> 其他人: 负
    // 其他人 -> 基金会: 正
    fun activeTransfer(account: OfflinePlayer, amount: BigDecimal) {
        val rounded = amount.setScale(2, RoundingMode.HALF_UP)
        bypassTransfer2.add(rounded)
        bypassTransfer1.add(account.uniqueId to rounded)
        if (amount > BigDecimal.ZERO) {
            patchActiveTransferMap[account.uniqueId] = patchActiveTransferMap.getOrDefault(account.uniqueId, BigDecimal.ZERO) + amount
        }
    }

    // 基金会 -> 其他人: 负
    // 其他人 -> 基金会: 正
    private fun passiveTransfer(account: OfflinePlayer, amount: BigDecimal) {
        patchPassiveTransferMap[account.uniqueId] = patchPassiveTransferMap.getOrDefault(account.uniqueId, BigDecimal.ZERO) + amount
    }

    internal fun getTop(onUpdate: () -> Unit): List<RecordDisplay> {
        if (topCache != null) return topCache!!
        onUpdate()
        Schedulers.sync().runLater({ topCache = null }, 1200L).bindWith(this)
        val list = mutableListOf<RecordDisplay>()
        for (record in recordsReader.readValues<RecordDouble>(File(dataFolder, "data.csv"))) {
            val player = Bukkit.getOfflinePlayer(record.id)
            if (!player.hasPlayedBefore()) continue
            list.add(RecordDisplay(player, record.active, record.passive))
        }
        list.sortByDescending { record -> record.passive + record.active }
        topCache = list
        return topCache!!
    }

    fun appendAllocationLog(date: Date, who: OfflinePlayer?, target: OfflinePlayer, amount: Double, reason: String) {
        // append to csv log
        configManager touch "allocation.csv"
        val file = File(dataFolder, "allocation.csv")
        val s = allocationStrWriter.writeValueAsString(AllocateStr(dateFormatter.format(date), who?.name ?: "#控制台#", target.name!!, amount, reason))
        file.appendText(s)
    }
}
