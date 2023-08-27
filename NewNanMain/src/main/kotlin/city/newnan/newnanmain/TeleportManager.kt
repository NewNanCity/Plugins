package city.newnan.newnanmain

import com.fasterxml.jackson.annotation.JsonInclude
import me.lucko.helper.terminable.Terminable
import org.bukkit.entity.Player

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TeleportPoint(
    val name: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val world: String,
    val icon: String,
    val permission: String? = null,
)

class TeleportManager(val plugin: PluginMain) : Terminable {
    var points = mutableListOf<TeleportPoint>()

    init { bindWith(plugin) }

    fun enable() {

    }

    fun reload() {
        points.clear()
        plugin.configManager.touch("teleport.yml", { emptyList<TeleportPoint>() })
        points = plugin.configManager.parse<MutableList<TeleportPoint>>("teleport.yml")
    }

    fun save() {
        plugin.configManager.save(points, "teleport.yml")
    }

    fun getTeleportPoints(player: Player): List<TeleportPoint> =
        points.filter { if (it.permission == null) true else player.hasPermission(it.permission) }

    override fun close() {

    }
}