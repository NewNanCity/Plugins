package city.newnan.createarea.config

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class ConfigFile(
    val world: String,
    @JsonProperty("default-group")
    val defaultGroup: String,
    val builderGroup: String,
)

data class CreateArea(
    val mame: String,
    val x1: Int,
    val z1: Int,
    val x2: Int,
    val z2: Int,
)

typealias CreateAreas = Map<UUID, CreateArea>
