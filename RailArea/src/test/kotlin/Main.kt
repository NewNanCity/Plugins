import city.newnan.railarea.config.Direction
import city.newnan.railarea.config.toMaterial
import city.newnan.railarea.octree.Octree
import city.newnan.railarea.octree.Point3D
import city.newnan.railarea.octree.Range3D
import city.newnan.violet.config.ConfigManager2
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.BlockFace
import kotlin.math.pow

fun benchmark() {
    val octree = Octree(Range3D(-10000, 0, -10000, 10000, 255, 10000))
    octree.insert(Range3D(-2144, 70, 897, -2141, 72, 907))
    octree.insert(Range3D(1254, 4, 1522, 1280, 13, 1540))
    octree.insert(Range3D(778, 23, 1522, 785, 40, 1540))
    octree.insert(Range3D(-2144+33, 70, 897, -2141+33, 72, 907))
    octree.insert(Range3D(1254+33, 4, 1522, 1280+33, 13, 1540))
    octree.insert(Range3D(778+33, 23, 1522, 785+33, 40, 1540))
    octree.insert(Range3D(-2144+180, 70, 897, -2141+180, 72, 907))
    octree.insert(Range3D(1254+180, 4, 1522, 1280+180, 13, 1540))
    octree.insert(Range3D(778+180, 23, 1522, 785+180, 40, 1540))
    octree.insert(Range3D(-2144-123, 70, 897, -2141-123, 72, 907))
    octree.insert(Range3D(1254-123, 4, 1522, 1280-123, 13, 1540))
    octree.insert(Range3D(778-123, 23, 1522, 785-123, 40, 1540))


    octree.insert(Range3D(-2144, 70, 897+106, -2141, 72, 907+106))
    octree.insert(Range3D(1254, 4, 1522+106, 1280, 13, 1540+106))
    octree.insert(Range3D(778, 23, 1522+106, 785, 40, 1540+106))
    octree.insert(Range3D(-2144+33, 70, 897+106, -2141+33, 72, 907+106))
    octree.insert(Range3D(1254+33, 4, 1522+106, 1280+33, 13, 1540+106))
    octree.insert(Range3D(778+33, 23, 1522+106, 785+33, 40, 1540+106))
    octree.insert(Range3D(-2144+180, 70, 897+106, -2141+180, 72, 907+106))
    octree.insert(Range3D(1254+180, 4, 1522+106, 1280+180, 13, 1540+106))
    octree.insert(Range3D(778+180, 23, 1522+106, 785+180, 40, 1540+106))
    octree.insert(Range3D(-2144-123, 70, 897+106, -2141-123, 72, 907+106))
    octree.insert(Range3D(1254-123, 4, 1522+106, 1280-123, 13, 1540+106))
    octree.insert(Range3D(778-123, 23, 1522+106, 785-123, 40, 1540+106))


    println()
    println()
    println("=================================================================")
    println()
    println()
    Point3D(-2142, 71, 905).also {
        // println()
        val startTime = System.currentTimeMillis()
        for (i in 0..10000000) {
            octree.firstRange(it)
        }
        val endTime = System.currentTimeMillis()
        println("Octree average query time: ${(endTime - startTime) / 10000001.0 * 1e6}ns")
    }
}

data class A(
    val direction: Direction = Direction.NORTH,
    val sound: Sound = Sound.BLOCK_NOTE_BLOCK_BELL,
    val material: Material = Material.PINK_CONCRETE,
)

fun main() {
    val a = Color.fromRGB(0x0ED145)
    // a.toMaterial().also(::println)
    println(a.toString())

    print("ban abc".split(" ", limit = 2))

    // benchmark()
//    val a = ConfigManager2.stringify(A(), ConfigManager2.ConfigFileType.Json).also(::println)
//    println(ConfigManager2.parse<A>(a, ConfigManager2.ConfigFileType.Json))
//
//    val numberPitch = (0..24).map { 2.0.pow((it - 12) / 12.0).toFloat() }.toTypedArray()
//
//    println(Sound.valueOf("BLOCK_NOTE_BLOCK_PLING"))

//    val octree = Octree(Range3D(-10000, 0, -10000, 10000, 255, 10000))
//    octree.insert(RailAreaConfig("(-2144,70,897)+(3,2,10)", "111", null, null).range3D)
//    Point3D(-2143, 71, 903).also {
//        println(octree.ranges(it))
//    }
}