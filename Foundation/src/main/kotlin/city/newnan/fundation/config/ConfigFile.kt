package city.newnan.fundation.config

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ConfigFile(
    val target: String? = null,
)