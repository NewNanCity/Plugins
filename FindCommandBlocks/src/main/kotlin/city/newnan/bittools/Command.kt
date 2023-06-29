package city.newnan.bittools

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender

@CommandAlias("fcm")
object BetterBookCommand : BaseCommand() {
    @Default
    fun publishCommand(sender: CommandSender, worldName: String) {
        if (sender is ConsoleCommandSender) return;
        val world = Bukkit.getWorld(worldName)
    }
}