package city.newnan.bittools

import me.lucko.helper.Events
import me.lucko.helper.plugin.ExtendedJavaPlugin
import org.bukkit.Bukkit
import org.bukkit.event.EventPriority
import org.bukkit.event.server.ServerLoadEvent
import java.io.FileWriter

class BitTools : ExtendedJavaPlugin() {
    companion object {
        lateinit var INSTANCE: BitTools
            private set
    }
    init { INSTANCE = this }

    override fun enable() {
//        Events.subscribe(ServerLoadEvent::class.java, EventPriority.MONITOR)
//            .handler {
//                val file = FileWriter("${this.dataFolder}/result.txt")
//                Bukkit.getServer().worlds.forEach {
//                    file.write("${it.name} (71, 7, -34) ${it.getBlockAt(71, 7, -34).type}\n")
//                    file.write("${it.name} (70, 7, -8) ${it.getBlockAt(70, 7, -8).type}\n")
//                }
//                file.close()
//            }
//            .bindWith(this)
        val file = FileWriter("${this.dataFolder}/result.txt")
        Bukkit.getServer().worlds.forEach {
            file.write("${it.name} (71, 7, -34) ${it.getBlockAt(71, 7, -34).type}\n")
            file.write("${it.name} (70, 7, -8) ${it.getBlockAt(70, 7, -8).type}\n")
            file.write("${it.name} (258, 45, 171) ${it.getBlockAt(258, 45, 171).type}\n")
            file.write("${it.name} (236, 45, 163) ${it.getBlockAt(236, 45, 163).type}\n")
        }
        file.close()
    }
}