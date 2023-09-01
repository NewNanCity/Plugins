import city.newnan.foundation.config.AllocateStr
import city.newnan.violet.config.ConfigManager2
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import java.io.File
import java.math.BigDecimal
import java.util.*

@JsonPropertyOrder("id", "active", "passive")
data class A(val id: UUID, val active: Double, val passive: Double)

fun main() {
//    val a = ConfigManager2.stringify(A(BigDecimal(0)), ConfigManager2.ConfigFileType.Yaml).also(::println)
//    ConfigManager2.parse<A>(a, ConfigManager2.ConfigFileType.Yaml).also(::println)
//    val mapper = ConfigManager2.mapper[ConfigManager2.ConfigFileType.Csv] as CsvMapper
//    val schema = mapper.schemaFor(A::class.java).withHeader()
//
//    val it = mapper.readerFor(A::class.java).with(schema).readValues<A>("""
//        id,active,passive
//        5b1ae398-ea12-481e-9013-9086b263b0d2,3,3
//        532c9cbf-4c56-44b4-a5cb-b8192282f68d,2,2
//        5b1ae398-ea12-481e-9013-9086b263b0d8,8,8
//        5b1ae398-ea12-481e-9013-9086b263b0d4,4,4
//    """.trimIndent())
//
//    val a = it.readAll()
//    println(a)
//    a.sortByDescending { record -> record.passive + record.active }
//    println(a)

//    mapper.writer(schema).writeValue(File("a.csv"), a)

    "啊啊啊啊啊啊啊".length.also(::println)
}

//data class TransferOther(val account: String, val amount: Double, var e: Int)
//data class TransferSelf(val amount: Double, var e: Int)
//
//fun main() {
//    val otherTransfers = ArrayDeque<TransferOther>()
//    val selfTransfers = ArrayDeque<TransferSelf>()
//    val bypassTransfer1 = mutableSetOf<Pair<String, Double>>()
//    val bypassTransfer2 = mutableSetOf<Double>()
//
//    fun check(account: String, old: Double, new: Double) {
//        while (!otherTransfers.isEmpty() && otherTransfers.first().e == 0)
//            otherTransfers.removeFirst()
//        while (!selfTransfers.isEmpty() && selfTransfers.first().e == 0)
//            selfTransfers.removeFirst()
//        if (account == "基金会") {
//            // 流入是别人的流出
//            val amount = new - old
//            println("delta = $amount")
//            if (bypassTransfer2.remove(amount)) return
//            // 查看转入交易中有没有金额一致的
//            val transfer = otherTransfers.find { t -> t.amount == amount && t.e > 0 }
//            if (transfer == null) {
//                selfTransfers.add(TransferSelf(amount, 1))
//            } else {
//                transfer.e = 0
//                println("${transfer.account}, $amount")
//            }
//        } else {
//            // 流出是别人的流入
//            val amount = old - new
//            println("delta = $amount")
//            if (bypassTransfer1.remove(account to amount)) return
//            // 查看转出交易中有没有金额一致的
//            val transfer = selfTransfers.find { t -> t.amount == amount && t.e > 0 }
//            if (transfer == null) {
//                otherTransfers.add(TransferOther(account, amount, 1))
//            } else {
//                transfer.e = 0
//                println("$account, $amount")
//            }
//        }
//        println()
//    }
//
//    check("基金会", 100.0, 200.0)
//    check("A", 400.0, 300.0)
//
//    check("B", 400.0, 300.0)
//    check("基金会", 100.0, 200.0)
//
//    check("C", 100.0, 200.0)
//    check("基金会", 400.0, 300.0)
//
//    check("基金会", 400.0, 300.0)
//    check("D", 100.0, 200.0)
//}