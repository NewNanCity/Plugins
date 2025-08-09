package city.newnan.bettercommandblock.commands.impl

import city.newnan.bettercommandblock.BetterCommandBlockPlugin
import city.newnan.bettercommandblock.i18n.LanguageKeys
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.command.BlockCommandSender
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Default
import org.incendo.cloud.annotations.Permission

import kotlin.random.Random

/**
 * 扩展命令实现类
 *
 * 实现BetterCommandBlock的所有扩展命令功能：
 * - pick: 随机物品选择
 * - scoreboard players random: 随机计分板操作
 * - execute: 增强的execute命令（利用Cloud的选择器支持）
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class ExtendedCommands(val plugin: BetterCommandBlockPlugin) {

    // ==================== Pick 命令 ====================

    /**
     * 随机物品选择命令
     * 从命令方块上方或下方的容器中随机选择物品到另一个容器
     */
    @Command("cb pick <type> <direction>")
    @CommandDescription(LanguageKeys.Commands.Pick.DESCRIPTION)
    fun pickCommand(
        sender: CommandSender,
        @Argument("type") type: String,
        @Argument("direction") direction: String
    ) {
        // 只能在命令方块中使用
        if (sender !is BlockCommandSender) {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.COMMAND_BLOCK_ONLY)
            return
        }

        val block = sender.block
        val blockDown = block.getRelative(BlockFace.DOWN)
        val blockUp = block.getRelative(BlockFace.UP)

        // 检查容器存在性
        if (blockDown.state !is InventoryHolder) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Pick.NO_CONTAINER_DOWN)
            return
        }
        if (blockUp.state !is InventoryHolder) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Pick.NO_CONTAINER_UP)
            return
        }

        // 确定源容器和目标容器
        val (sourceInventory, targetInventory, sourceDirection) = when (direction.lowercase()) {
            "up" -> Triple(
                (blockUp.state as InventoryHolder).inventory,
                (blockDown.state as InventoryHolder).inventory,
                "上方"
            )
            "down" -> Triple(
                (blockDown.state as InventoryHolder).inventory,
                (blockUp.state as InventoryHolder).inventory,
                "下方"
            )
            else -> {
                plugin.messager.printf(sender, LanguageKeys.Commands.Pick.INVALID_DIRECTION)
                return
            }
        }

        // 检查源容器是否为空
        if (sourceInventory.isEmpty) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Pick.SOURCE_EMPTY)
            return
        }

        // 根据类型选择物品
        val selectedSlot = when (type.lowercase()) {
            "random-slot" -> selectRandomSlot(sourceInventory)
            "random-item" -> selectRandomItemByWeight(sourceInventory)
            else -> {
                plugin.messager.printf(sender, LanguageKeys.Commands.Pick.INVALID_TYPE)
                return
            }
        }

        if (selectedSlot >= 0) {
            moveItemFromSlot(sourceInventory, targetInventory, selectedSlot, sender, sourceDirection)
        }
    }

    /**
     * 随机选择一个非空格子
     */
    private fun selectRandomSlot(inventory: Inventory): Int {
        val nonEmptySlots = mutableListOf<Int>()
        for (i in 0 until inventory.size) {
            if (inventory.getItem(i) != null && inventory.getItem(i)!!.type != Material.AIR) {
                nonEmptySlots.add(i)
            }
        }
        return if (nonEmptySlots.isNotEmpty()) nonEmptySlots.random() else -1
    }

    /**
     * 按物品数量权重随机选择
     */
    private fun selectRandomItemByWeight(inventory: Inventory): Int {
        var totalItemCount = 0
        for (i in 0 until inventory.size) {
            val item = inventory.getItem(i)
            if (item != null && item.type != Material.AIR) {
                totalItemCount += item.amount
            }
        }

        if (totalItemCount == 0) return -1

        var randomItem = Random.nextInt(totalItemCount)
        for (i in 0 until inventory.size) {
            val item = inventory.getItem(i)
            if (item != null && item.type != Material.AIR) {
                randomItem -= item.amount
                if (randomItem < 0) {
                    return i
                }
            }
        }
        return -1
    }

    /**
     * 从指定格子移动物品
     */
    private fun moveItemFromSlot(
        sourceInventory: Inventory,
        targetInventory: Inventory,
        slot: Int,
        sender: CommandSender,
        sourceDirection: String
    ) {
        val originalItem = sourceInventory.getItem(slot) ?: return
        val pickedItem = originalItem.clone()
        pickedItem.amount = 1

        // 减少原物品数量
        if (originalItem.amount > 1) {
            originalItem.amount--
        } else {
            sourceInventory.setItem(slot, null)
        }

        // 尝试添加到目标容器
        val leftover = targetInventory.addItem(pickedItem)
        if (leftover.isNotEmpty()) {
            // 目标容器已满，恢复原物品
            sourceInventory.addItem(pickedItem)
            plugin.messager.printf(sender, LanguageKeys.Commands.Pick.TARGET_FULL)
        } else {
            plugin.messager.printf(sender, LanguageKeys.Commands.Pick.SUCCESS,
                sourceDirection, pickedItem.type.name)
        }
    }

    // ==================== Scoreboard 命令 ====================

    /**
     * 随机计分板命令
     */
    @Command("cb scoreboard players random <mode> <target> <objective> <min> <max>")
    @CommandDescription(LanguageKeys.Commands.Scoreboard.DESCRIPTION)
    fun scoreboardRandomCommand(
        sender: CommandSender,
        @Argument("mode") mode: String,
        @Argument("target") target: String,
        @Argument("objective") objectiveName: String,
        @Argument("min") minValue: Int,
        @Argument("max") maxValue: Int
    ) {
        // 只能在命令方块中使用
        if (sender !is BlockCommandSender) {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.COMMAND_BLOCK_ONLY)
            return
        }

        // 验证模式
        if (mode !in setOf("set", "add", "sub")) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Scoreboard.INVALID_MODE)
            return
        }

        // 获取计分板目标
        val scoreboard = Bukkit.getScoreboardManager()?.mainScoreboard
        val objective = scoreboard?.getObjective(objectiveName)

        if (objective == null) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Scoreboard.OBJECTIVE_NOT_FOUND, objectiveName)
            return
        }

        // 解析目标（支持选择器和玩家名）
        val targets = if (target.startsWith("@")) {
            try {
                Bukkit.selectEntities(sender, target).filterIsInstance<Player>().map { it.name }
            } catch (e: Exception) {
                plugin.messager.printf(sender, LanguageKeys.Commands.Scoreboard.TARGET_NOT_FOUND, target)
                return
            }
        } else {
            listOf(target)
        }

        if (targets.isEmpty()) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Scoreboard.TARGET_NOT_FOUND, target)
            return
        }

        // 对每个目标执行操作
        targets.forEach { targetName ->
            val score = objective.getScore(targetName)
            val randomValue = Random.nextInt(minValue, maxValue + 1)

            when (mode) {
                "set" -> score.score = randomValue
                "add" -> score.score = (score.score + randomValue).coerceAtMost(Int.MAX_VALUE)
                "sub" -> score.score = (score.score - randomValue).coerceAtLeast(Int.MIN_VALUE)
            }
        }

        plugin.messager.printf(sender, LanguageKeys.Commands.Scoreboard.SUCCESS,
            objectiveName, minValue, maxValue)
    }

    // ==================== Execute 命令 ====================

    /**
     * 增强的execute命令
     * 使用传统的选择器字符串解析
     */
    @Command("cb execute as <selector> run <command>")
    @CommandDescription(LanguageKeys.Commands.Execute.DESCRIPTION)
    @Permission("better-command-block.execute")
    fun executeCommand(
        sender: CommandSender,
        @Argument("selector") selector: String,
        @Argument("command") command: String
    ) {
        // 只能在命令方块中使用
        if (sender !is BlockCommandSender) {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.COMMAND_BLOCK_ONLY)
            return
        }

        // 检查是否是递归execute
        if (command.startsWith("cb execute") || command.startsWith("execute")) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Execute.RECURSIVE_EXECUTE)
            return
        }

        // 检查命令是否被安全策略阻止
        val commandParts = command.split(" ")
        if (commandParts.isNotEmpty()) {
            val baseCommand = commandParts[0].lowercase()
            if (plugin.securityModule.isBlocked(baseCommand)) {
                plugin.messager.printf(sender, LanguageKeys.Commands.Execute.BLOCKED_COMMAND, baseCommand)
                return
            }
        }

        try {
            // 使用Bukkit的选择器解析
            val selectedEntities = Bukkit.selectEntities(sender, selector)
            if (selectedEntities.isEmpty()) {
                plugin.messager.printf(sender, LanguageKeys.Commands.Execute.ENTITY_NOT_FOUND, selector)
                return
            }

            // 对每个实体执行命令
            selectedEntities.forEach { entity ->
                try {
                    if (entity is Player) {
                        Bukkit.dispatchCommand(entity, command)
                    } else {
                        // 对于非玩家实体，使用控制台执行
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
                    }
                } catch (e: Exception) {
                    plugin.logger.error("Failed to execute command for entity ${entity.name}", e)
                }
            }

            plugin.messager.printf(sender, LanguageKeys.Commands.Execute.SUCCESS)
        } catch (e: Exception) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Execute.ENTITY_NOT_FOUND, selector)
        }
    }
}
