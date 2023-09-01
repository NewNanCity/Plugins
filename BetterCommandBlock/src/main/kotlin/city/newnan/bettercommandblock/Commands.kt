package city.newnan.bettercommandblock

import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import org.bukkit.Bukkit
import org.bukkit.block.BlockFace
import org.bukkit.command.BlockCommandSender
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import java.io.File


@CommandAlias("commandblock|cb")
object Commands : BaseCommand() {
    @Subcommand("reload")
    @CommandPermission("better-command-block.reload")
    @Description("重载插件")
    fun onReload(sender: CommandSender) {
        PluginMain.INSTANCE.reload()
        PluginMain.INSTANCE.messageManager.printf(sender, "插件已重载!")
    }

    @Default
    @HelpCommand
    @Subcommand("help")
    fun onHelp(sender: CommandSender, help: CommandHelp) {
        help.showHelp()
    }

    @Subcommand("pick")
    @CommandCompletion("random-slot|random-item up|down")
    fun onRandomPick(sender: CommandSender, type: String, @Single target: String) {
        if (sender !is BlockCommandSender) {
            PluginMain.INSTANCE.messageManager.printf(sender, "只能在命令方块中使用!")
            return
        }
        val block = sender.block
        val blockDown = block.getRelative(BlockFace.DOWN).state
        if (blockDown !is InventoryHolder) {
            PluginMain.INSTANCE.messageManager.printf(sender, "命令方块下方没有容器!")
            return
        }
        val blockUp = block.getRelative(BlockFace.UP)
        if (blockUp !is InventoryHolder) {
            PluginMain.INSTANCE.messageManager.printf(sender, "命令方块上方没有容器!")
            return
        }
        val sourceInventory: Inventory = when (target) {
            "up" -> blockUp.inventory
            "down" -> blockDown.inventory
            else -> {
                PluginMain.INSTANCE.messageManager.printf(sender, "§c用法: /cb pick <random-slot|random-item> <up|down>")
                return
            }
        }
        if (sourceInventory.isEmpty) {
            PluginMain.INSTANCE.messageManager.printf(sender, "§c源容器为空!")
            return
        }
        val targetInventory: Inventory = when (target) {
            "up" -> blockDown.inventory
            "down" -> blockUp.inventory
            else -> {
                PluginMain.INSTANCE.messageManager.printf(sender, "§c用法: /cb pick <random-slot|random-item> <up|down>")
                return
            }
        }
        var randomSlot = -1
        when (type) {
            "random-slot" -> {
                // find all non-empty slots
                val nonEmptySlots = mutableListOf<Int>()
                for (i in 0 until sourceInventory.size) {
                    if (sourceInventory.getItem(i) != null) {
                        nonEmptySlots.add(i)
                    }
                }
                randomSlot = nonEmptySlots.random()
            }
            "random-item" -> {
                var totalItemCount = 0
                for (i in 0 until sourceInventory.size) {
                    totalItemCount += sourceInventory.getItem(i)?.amount ?: 0
                }
                var randomItem = (0 until totalItemCount).random()
                for (i in 0 until sourceInventory.size) {
                    randomItem -= sourceInventory.getItem(i)?.amount ?: 0
                    if (randomItem < 0) {
                        randomSlot = i
                        break
                    }
                }
            }
            else -> {
                PluginMain.INSTANCE.messageManager.printf(sender, "§c用法: /cb pick <random-slot|random-item> <up|down>")
                return
            }
        }
        if (randomSlot >= 0) {
            val original = sourceInventory.getItem(randomSlot)
            val picked = original!!.clone()
            picked.amount = 1
            original.amount--
            if (targetInventory.addItem(picked).size > 0) {
                sourceInventory.addItem(picked)
                PluginMain.INSTANCE.messageManager.printf(sender, "§c目标容器已满!")
            }
        }
    }

    @Subcommand("scoreboard players random")
    @CommandCompletion("set|add|sub")
    fun onRandomScoreBoard(sender: CommandSender, mode: String, target: String, objectiveName: String, minValue: Int, maxValue: Int) {
        if (sender !is BlockCommandSender) {
            PluginMain.INSTANCE.messageManager.printf(sender, "只能在命令方块中使用!")
            return
        }
        if (mode != "set" && mode != "add" && mode != "sub") {
            PluginMain.INSTANCE.messageManager.printf(sender, "§c用法: /cb scoreboard players random <set|add|sub> <目标> <目标计分板> <最小值> <最大值>")
            return
        }
        Bukkit.getScoreboardManager()?.mainScoreboard?.getObjective(objectiveName)?.also {
            val targets = if (target[0] != '@') listOf(target)
                else Bukkit.selectEntities(sender, target).filterIsInstance<Player>().map { it.name }
            if (targets.isEmpty()) {
                PluginMain.INSTANCE.messageManager.printf(sender, "§c找不到目标: $target")
                return
            }
            targets.forEach { target ->
                val score = it.getScore(target)
                val random = (minValue..maxValue).random()
                when (mode) {
                    "set" -> score.score = random
                    "add" -> score.score = minOf(Int.MAX_VALUE, random + score.score)
                    "sub" -> score.score = maxOf(Int.MIN_VALUE, score.score - random)
                    else -> {}
                }
            }
            PluginMain.INSTANCE.messageManager.printf(sender, "§a已随机设置计分板 $objectiveName 的值为 $minValue ~ $maxValue")
        } ?: run {
            PluginMain.INSTANCE.messageManager.printf(sender, "§c找不到计分板目标: $objectiveName")
            return
        }
    }

    @Subcommand("execute")
    @CommandPermission("better-command-block.execute")
    fun onExecute(sender: CommandSender, argv: Array<String>) {
        try {
            var executers = mutableMapOf(sender to mutableListOf<String>())
            var runMode = false
            var i = 0
            var xyzMode = 0
            if (argv.isEmpty()) {
                PluginMain.INSTANCE.messageManager.printf(sender, "§c用法: /cb execute (as <选择器>) <命令>")
                return
            }
            while (i < argv.size) {
                // run 之后的内容
                if (runMode) {
                    val arg = argv[i]
                    if (arg[0] == '@') {
                        if (arg.length > 1 && arg[1] == '@') {
                            // @ 转义字符
                            val s = arg.substring(1)
                            executers.forEach { (_, u) -> u.add(s) }
                        } else {
                            val executerToDelete = mutableListOf<CommandSender>()
                            executers.forEach { (executer, commands) ->
                                val selected = Bukkit.selectEntities(executer, arg).filterIsInstance<Player>()
                                if (selected.isEmpty()) {
                                    PluginMain.INSTANCE.messageManager.printf(sender, "§c${executer.name} 找不到实体 $arg")
                                    executerToDelete.add(executer)
                                    return@forEach
                                }
                                if (selected.size > 1) {
                                    PluginMain.INSTANCE.messageManager.printf(sender, "§c${executer.name} 选择器 $arg 匹配到多个实体")
                                    executerToDelete.add(executer)
                                    return@forEach
                                }
                                commands.add(selected.first().name)
                            }
                            executerToDelete.forEach { executers.remove(it) }
                        }
                    } else if (arg[0] == '~') {
                        val splited = arg.split('~')
                        val delta = if (splited.size > 1) splited[1].toDouble().toInt() else 0
                        val executerToDelete = mutableListOf<CommandSender>()
                        executers.forEach { (executer, commands) ->
                            val location = when {
                                executer is BlockCommandSender -> {
                                    executer.block.location
                                }
                                executer is Player -> {
                                    executer.location
                                }
                                else -> {
                                    PluginMain.INSTANCE.messageManager.printf(sender, "§c${executer.name} 不是一个有效的实体")
                                    executerToDelete.add(executer)
                                    return@forEach
                                }
                            }
                            val base = when(xyzMode) {
                                0 -> location.x
                                1 -> location.y
                                2 -> location.z
                                else -> 0
                            }.toInt()
                            commands.add((base + delta).toString())
                        }
                        xyzMode = (xyzMode + 1) % 3
                        executerToDelete.forEach { executers.remove(it) }
                    } else {
                        // 一般的参数
                        executers.forEach { (_, u) -> u.add(arg) }
                    }
                }
                // run 之前的内容
                if (argv[i] == "as") {
                    if (argv.size - i <= 1) {
                        PluginMain.INSTANCE.messageManager.printf(sender, "§c用法: /cb execute (as <选择器>) run <命令>")
                        return
                    }
                    val tmp = mutableMapOf<CommandSender, MutableList<String>>()
                    Bukkit.selectEntities(sender, argv[i + 1]).filterIsInstance<Player>().forEach {
                        tmp[it] = mutableListOf()
                    }
                    if (tmp.isEmpty()) {
                        PluginMain.INSTANCE.messageManager.printf(sender, "§c找不到实体: ${argv[i + 1]}")
                        return
                    }
                    executers = tmp
                    i++
                } else if (argv[i] == "run") {
                    runMode = true
                    if (argv.size - i <= 1) {
                        PluginMain.INSTANCE.messageManager.printf(sender, "§c用法: /cb execute (as <选择器>) run <命令>")
                        return
                    }
                    if (argv[i + 1] == "execute" || (argv[i + 1] == "cb" && argv.size - i > 2 && argv[i + 2] == "execute")) {
                        PluginMain.INSTANCE.messageManager.printf(sender, "§c用法: /cb execute (as <选择器>) run <命令>")
                        return
                    }
                    if (PluginMain.INSTANCE.blockedCommands.contains(argv[i + 1])) {
                        // remove the command block
                        PluginMain.INSTANCE.blockCommand((sender as BlockCommandSender).block, "cb execute ${argv.joinToString(" ")}")
                        return
                    }
                }
                i++
            }
            executers.forEach { (executer, commands) ->
                sender.server.dispatchCommand(executer, commands.joinToString(" "))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}