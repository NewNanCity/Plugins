package city.newnan.railexpress

import org.bukkit.Material
import java.util.*


class RailConfig(
    val powerRailOnly: Boolean,
    val allowNonPlayer: Boolean,
    blockType: Map<String, Double>
) {
    val blockSpeedMap =
        blockType.mapKeys { Material.valueOf(it.key.uppercase(Locale.getDefault())) }
}