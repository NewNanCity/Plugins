package city.newnan.tpa.modules

import city.newnan.config.extensions.configManager
import city.newnan.core.base.BaseModule
import city.newnan.tpa.TPAPlugin
import city.newnan.tpa.i18n.LanguageKeys
import org.bukkit.OfflinePlayer
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * TPA玩家屏蔽管理器
 * 
 * 负责管理玩家的屏蔽列表，包括：
 * - 添加和移除屏蔽关系
 * - 检查屏蔽状态
 * - 持久化屏蔽数据
 * - 提供屏蔽列表查询
 * 
 * @author AI Assistant
 * @since 2.0.0
 */
class TPABlockManager(
    moduleName: String,
    val plugin: TPAPlugin
) : BaseModule(moduleName, plugin) {
    // ===== 屏蔽数据存储 =====
    // Key: 屏蔽者的UUID, Value: 被屏蔽玩家的UUID集合
    private val blockData = ConcurrentHashMap<UUID, MutableSet<UUID>>()
    
    // ===== 配置文件名 =====
    private val blockDataFile = "blocked.yml"

    // ===== 必须：手动调用init()来触发初始化 =====
    init { init() }
    
    override fun onReload() {
        // 重新加载屏蔽数据
        try {
            blockData.clear()

            // 创建文件（如果不存在）
            plugin.configManager.touch(
                configPath = blockDataFile,
                defaultData = { emptyMap<UUID, Set<UUID>>() }
            )

            // 加载数据
            val loadedData = plugin.configManager.parse<Map<UUID, Set<UUID>>>(blockDataFile)

            // 转换为可变集合
            loadedData.forEach { (blocker, blocked) ->
                blockData[blocker] = Collections.synchronizedSet(blocked.toMutableSet())
            }

        } catch (e: Exception) {
            logger.error(LanguageKeys.Log.Error.CONFIG_ERROR, e, "Failed to load block data")
        }
    }
    
    // ===== 屏蔽管理 =====
    
    /**
     * 屏蔽指定玩家
     * 
     * @param blocker 执行屏蔽的玩家
     * @param target 被屏蔽的玩家
     * @return true如果成功添加屏蔽，false如果已经屏蔽过
     */
    fun blockPlayer(blocker: OfflinePlayer, target: OfflinePlayer): Boolean {
        // 不能屏蔽自己
        if (blocker.uniqueId == target.uniqueId) {
            return false
        }
        
        // 获取或创建屏蔽集合
        val blockedSet = blockData.computeIfAbsent(blocker.uniqueId) { 
            Collections.synchronizedSet(mutableSetOf<UUID>()) 
        }
        
        // 添加屏蔽关系
        val added = blockedSet.add(target.uniqueId)
        
        if (added) {
            // 保存到文件
            saveBlockData()
            
            // 发送消息
            if (blocker.isOnline) {
                plugin.messager.printf(blocker.player!!, LanguageKeys.Commands.Block.SUCCESS, target.name ?: "Unknown")
                plugin.messager.printf(blocker.player!!, LanguageKeys.Commands.Block.USAGE_HINT, target.name ?: "Unknown")
            }
            
            logger.debug("Player ${blocker.name} blocked ${target.name}")
        } else {
            // 已经屏蔽过
            if (blocker.isOnline) {
                plugin.messager.printf(blocker.player!!, LanguageKeys.Commands.Block.ALREADY_BLOCKED)
            }
        }
        
        return added
    }
    
    /**
     * 取消屏蔽指定玩家
     * 
     * @param blocker 执行取消屏蔽的玩家
     * @param target 被取消屏蔽的玩家
     * @return true如果成功移除屏蔽，false如果本来就没有屏蔽
     */
    fun unblockPlayer(blocker: OfflinePlayer, target: OfflinePlayer): Boolean {
        val blockedSet = blockData[blocker.uniqueId] ?: return false
        
        val removed = blockedSet.remove(target.uniqueId)
        
        if (removed) {
            // 如果集合为空，移除整个条目
            if (blockedSet.isEmpty()) {
                blockData.remove(blocker.uniqueId)
            }
            
            // 保存到文件
            saveBlockData()
            
            // 发送消息
            if (blocker.isOnline) {
                plugin.messager.printf(blocker.player!!, LanguageKeys.Commands.Unblock.SUCCESS, target.name ?: "?")
            }
        } else {
            // 本来就没有屏蔽
            if (blocker.isOnline) {
                plugin.messager.printf(blocker.player!!, LanguageKeys.Commands.Unblock.NOT_BLOCKED)
            }
        }
        
        return removed
    }
    
    /**
     * 检查是否被屏蔽
     * 
     * @param blocker 可能执行屏蔽的玩家
     * @param target 可能被屏蔽的玩家
     * @return true如果target被blocker屏蔽了
     */
    fun isBlocked(blocker: OfflinePlayer, target: OfflinePlayer): Boolean {
        val blockedSet = blockData[blocker.uniqueId] ?: return false
        return blockedSet.contains(target.uniqueId)
    }
    
    /**
     * 获取指定玩家的屏蔽列表
     * 
     * @param player 玩家
     * @return 被该玩家屏蔽的玩家UUID集合（只读）
     */
    fun getBlockedPlayers(player: OfflinePlayer): Set<UUID> {
        return blockData[player.uniqueId]?.toSet() ?: emptySet()
    }
    
    // ===== 数据持久化 =====
    
    /**
     * 保存屏蔽数据到配置文件
     */
    private fun saveBlockData() {
        try {
            // 转换为不可变集合用于序列化
            val saveData = blockData.mapValues { it.value.toSet() }
            
            // 保存到文件
            plugin.configManager.save(saveData, blockDataFile)
            
        } catch (e: Exception) {
            logger.error(LanguageKeys.Log.Error.CONFIG_ERROR, e, "Failed to save block data")
        }
    }
    
    // ===== 统计信息 =====
    
    /**
     * 获取总的屏蔽关系数量
     */
    fun getTotalBlockCount(): Int {
        return blockData.values.sumOf { it.size }
    }
    
    /**
     * 获取有屏蔽关系的玩家数量
     */
    fun getBlockingPlayerCount(): Int {
        return blockData.size
    }
    
    /**
     * 获取所有屏蔽数据的调试信息
     */
    fun getDebugInfo(): String {
        return buildString {
            appendLine("=== TPA Block Manager Debug Info ===")
            appendLine("Blocking players: ${getBlockingPlayerCount()}")
            appendLine("Total blocks: ${getTotalBlockCount()}")
            appendLine("Block data:")
            blockData.forEach { (blocker, blocked) ->
                appendLine("  $blocker -> ${blocked.size} players")
            }
        }
    }
}
