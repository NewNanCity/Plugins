import city.newnan.mcron.timeiterator.CronExpression
import java.text.SimpleDateFormat
import java.util.*

fun main() {
    CronExpression.setTimeZoneOffset("+8")
    val now = System.currentTimeMillis()
    val f = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    println(f.format(Date(CronExpression("0 0 */3 * * *").getNextTime(now))))
}