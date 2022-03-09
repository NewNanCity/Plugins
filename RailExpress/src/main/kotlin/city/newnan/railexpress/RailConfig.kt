package city.newnan.railexpress

import me.lucko.helper.config.ConfigurationNode
import org.bukkit.Material
import java.util.*


class RailConfig(
    val powerRailOnly: Boolean,
    val allowNonPlayer: Boolean,
    blockType: ConfigurationNode
) {
    val blockSpeedMap = HashMap<Material, Double>()

    init {
        blockType.childrenMap.forEach { (key: Any, node: ConfigurationNode) ->
            blockSpeedMap[Material.valueOf(
                (key as String).uppercase(Locale.getDefault())
            )] = node.getDouble(RailExpress.DEFAULT_SPEED)
        }
    }
}