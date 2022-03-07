package city.newnan.deathtrigger

import me.lucko.helper.Events
import me.lucko.helper.plugin.ExtendedJavaPlugin
import org.bukkit.Bukkit
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageEvent


class DeathTrigger : ExtendedJavaPlugin() {
    public override fun enable() {
        val world = Bukkit.getWorld("world")
        // Plugin startup logic
        Events.subscribe(EntityDamageEvent::class.java, EventPriority.LOWEST)
            .filter { event: EntityDamageEvent -> !event.isCancelled }
            .filter { event: EntityDamageEvent -> event.entityType == EntityType.PLAYER }
            .filter { event: EntityDamageEvent ->
                event.entity.location.world == world
            }
            .handler { event: EntityDamageEvent ->
                // 区域筛选
                val location = event.entity.location
                if (location.blockX > -2734 || location.blockX < -3109 || location.blockZ > 440 || location.blockZ < -82
                ) return@handler
                // 致命伤害筛选
                val player = event.entity as Player
                if (player.health > event.finalDamage) return@handler
                player.health = player.maxHealth
                player.teleport(player.bedSpawnLocation!!)
                event.isCancelled = true
            }
    }

    public override fun disable() {
        // Plugin shutdown logic
    }
}