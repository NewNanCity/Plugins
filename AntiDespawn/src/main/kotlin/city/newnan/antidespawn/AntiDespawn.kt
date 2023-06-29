package city.newnan.antidespawn

import me.lucko.helper.Events
import me.lucko.helper.plugin.ExtendedJavaPlugin
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority


class AntiDespawn : ExtendedJavaPlugin() {
    override fun enable() {
        // Plugin startup logic
        Events.subscribe(
            com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent::class.java,
            EventPriority.MONITOR
        )
            .filter { it.entity is Player }
            .filter { !it.entity.isDead }
            .handler {
                it.entity.teleport(it.entity.location.world!!.spawnLocation)
            }
            .bindWith(this)
    }
}