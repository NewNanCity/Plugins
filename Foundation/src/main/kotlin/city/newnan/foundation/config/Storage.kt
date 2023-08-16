package city.newnan.foundation.config

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import java.util.*

@JsonPropertyOrder("id", "active", "passive")
data class RecordStr(val id: String, val active: String, val passive: String)

@JsonPropertyOrder("id", "active", "passive")
data class RecordDouble(val id: UUID, val active: Double, val passive: Double)

@JsonPropertyOrder("id", "active", "passive")
data class RecordDisplay(val name: String, val active: Double, val passive: Double)

@JsonPropertyOrder("date", "who", "target", "amount", "reason")
data class AllocateStr(val date: String, val who: String, val target: String, val amount: Double, val reason: String)