import org.bukkit.Material

fun main() {
    val x = Material.CHEST
    println(x.toString())
    println(Material.valueOf(x.toString()))
}