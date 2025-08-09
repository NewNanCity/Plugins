package city.newnan.mcron.config

import com.cronutils.model.time.ExecutionTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.jvm.optionals.getOrNull

class CronTask {
    val expression: String?
    val executionTime: ExecutionTime?
    val nextTime: Long
    val commands: List<String>

    constructor(expression: String, executionTime: ExecutionTime, commands: List<String>) {
        this.expression = expression
        this.executionTime = executionTime
        this.nextTime = 0L
        this.commands = commands.toList()
    }

    constructor(nextTime: Long, commands: Array<String>) {
        this.nextTime = nextTime
        this.expression = null
        this.executionTime = null
        this.commands = commands.toList()
    }

    fun getNextTime(now: ZonedDateTime) =
        executionTime?.nextExecution(now)?.getOrNull()?.toInstant()?.toEpochMilli() ?: nextTime
}