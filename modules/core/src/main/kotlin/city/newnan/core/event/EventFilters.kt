package city.newnan.core.event

import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.block.BlockEvent
import org.bukkit.event.entity.EntityEvent
import org.bukkit.event.player.PlayerEvent
import org.bukkit.event.player.PlayerMoveEvent
import java.util.function.Predicate

/**
 * 常用事件过滤器
 * 
 * 提供一系列预定义的事件过滤器，简化常见的事件过滤需求。
 */
object EventFilters {
    
    /**
     * 忽略已取消的事件
     */
    fun <T> ignoreCancelled(): Predicate<T> where T : Cancellable {
        return Predicate { !it.isCancelled }
    }
    
    /**
     * 只处理已取消的事件
     */
    fun <T> onlyCancelled(): Predicate<T> where T : Cancellable {
        return Predicate { it.isCancelled }
    }
    
    /**
     * 忽略相同方块的移动事件（只在玩家移动到新方块时触发）
     */
    fun ignoreSameBlock(): Predicate<PlayerMoveEvent> {
        return Predicate { event ->
            val from = event.from
            val to = event.to ?: return@Predicate false
            
            from.blockX != to.blockX || from.blockY != to.blockY || from.blockZ != to.blockZ
        }
    }
    
    /**
     * 忽略相同位置的移动事件（只在玩家实际移动时触发）
     */
    fun ignoreSameLocation(): Predicate<PlayerMoveEvent> {
        return Predicate { event ->
            val from = event.from
            val to = event.to ?: return@Predicate false
            
            from.x != to.x || from.y != to.y || from.z != to.z
        }
    }
    
    /**
     * 只处理有权限的玩家
     */
    fun playerHasPermission(permission: String): Predicate<PlayerEvent> {
        return Predicate { event ->
            event.player.hasPermission(permission)
        }
    }
    
    /**
     * 只处理没有权限的玩家
     */
    fun playerLacksPermission(permission: String): Predicate<PlayerEvent> {
        return Predicate { event ->
            !event.player.hasPermission(permission)
        }
    }
    
    /**
     * 只处理OP玩家
     */
    fun playerIsOp(): Predicate<PlayerEvent> {
        return Predicate { event ->
            event.player.isOp
        }
    }
    
    /**
     * 只处理非OP玩家
     */
    fun playerIsNotOp(): Predicate<PlayerEvent> {
        return Predicate { event ->
            !event.player.isOp
        }
    }
    
    /**
     * 只处理指定玩家
     */
    fun playerIs(player: Player): Predicate<PlayerEvent> {
        return Predicate { event ->
            event.player == player
        }
    }
    
    /**
     * 只处理指定玩家（通过名称）
     */
    fun playerIs(playerName: String): Predicate<PlayerEvent> {
        return Predicate { event ->
            event.player.name.equals(playerName, ignoreCase = true)
        }
    }
    
    /**
     * 排除指定玩家
     */
    fun playerIsNot(player: Player): Predicate<PlayerEvent> {
        return Predicate { event ->
            event.player != player
        }
    }
    
    /**
     * 排除指定玩家（通过名称）
     */
    fun playerIsNot(playerName: String): Predicate<PlayerEvent> {
        return Predicate { event ->
            !event.player.name.equals(playerName, ignoreCase = true)
        }
    }
    
    /**
     * 只处理指定世界的事件
     */
    fun inWorld(worldName: String): Predicate<PlayerEvent> {
        return Predicate { event ->
            event.player.world.name.equals(worldName, ignoreCase = true)
        }
    }
    
    /**
     * 排除指定世界的事件
     */
    fun notInWorld(worldName: String): Predicate<PlayerEvent> {
        return Predicate { event ->
            !event.player.world.name.equals(worldName, ignoreCase = true)
        }
    }
    
    /**
     * 只处理指定世界的方块事件
     */
    fun blockInWorld(worldName: String): Predicate<BlockEvent> {
        return Predicate { event ->
            event.block.world.name.equals(worldName, ignoreCase = true)
        }
    }
    
    /**
     * 只处理指定世界的实体事件
     */
    fun entityInWorld(worldName: String): Predicate<EntityEvent> {
        return Predicate { event ->
            event.entity.world.name.equals(worldName, ignoreCase = true)
        }
    }
    
    /**
     * 组合多个过滤器（AND逻辑）
     */
    fun <T> and(vararg filters: Predicate<T>): Predicate<T> {
        return Predicate { event ->
            filters.all { it.test(event) }
        }
    }
    
    /**
     * 组合多个过滤器（OR逻辑）
     */
    fun <T> or(vararg filters: Predicate<T>): Predicate<T> {
        return Predicate { event ->
            filters.any { it.test(event) }
        }
    }
    
    /**
     * 反转过滤器
     */
    fun <T> not(filter: Predicate<T>): Predicate<T> {
        return Predicate { event ->
            !filter.test(event)
        }
    }
}
