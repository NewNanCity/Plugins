package city.newnan.railarea.config

import org.bukkit.Color
import org.bukkit.Material

/**
 * 配置类扩展方法
 *
 * 提供配置类的便利方法
 *
 * @author NewNanCity
 * @since 2.0.0
 */

/**
 * 预定义的颜色到混凝土材料的映射表
 * 使用旧版插件的16种标准颜色
 */
val colorMaterials = arrayOf(
    Color.fromRGB(0xcdd3d4) to Material.WHITE_CONCRETE,
    Color.fromRGB(0xdd5700) to Material.ORANGE_CONCRETE,
    Color.fromRGB(0xa32699) to Material.MAGENTA_CONCRETE,
    Color.fromRGB(0x1983c4) to Material.LIGHT_BLUE_CONCRETE,
    Color.fromRGB(0xf2ac0b) to Material.YELLOW_CONCRETE,
    Color.fromRGB(0x54a20c) to Material.LIME_CONCRETE,
    Color.fromRGB(0xd35d89) to Material.PINK_CONCRETE,
    Color.fromRGB(0x2c3034) to Material.GRAY_CONCRETE,
    Color.fromRGB(0x75756b) to Material.LIGHT_GRAY_CONCRETE,
    Color.fromRGB(0x0b7082) to Material.CYAN_CONCRETE,
    Color.fromRGB(0x5b1495) to Material.PURPLE_CONCRETE,
    Color.fromRGB(0x212388) to Material.BLUE_CONCRETE,
    Color.fromRGB(0x583215) to Material.BROWN_CONCRETE,
    Color.fromRGB(0x3f5219) to Material.GREEN_CONCRETE,
    Color.fromRGB(0x871515) to Material.RED_CONCRETE,
    Color.fromRGB(0x010103) to Material.BLACK_CONCRETE,
)

/**
 * 颜色转换为材料
 * 使用Lab颜色距离算法匹配最接近的混凝土材料
 */
fun Color.toMaterial(): Material {
    var minDistance = 0xFFFFFF
    var minMaterial = Material.WHITE_CONCRETE
    val r = red
    val g = green
    val b = blue

    for ((color, material) in colorMaterials) {
        val dr = color.red - r
        val dg = color.green - g
        val db = color.blue - b

        // Lab distance algorithm - 考虑人眼对不同颜色的敏感度
        val rmean = (color.red + r) shr 1
        val distance = (512 + rmean) * dr * dr + (dg * dg shl 10) + (767 - rmean) * db * db

        if (distance == 0) {
            return material
        }
        if (distance < minDistance) {
            minDistance = distance
            minMaterial = material
        }
    }
    return minMaterial
}

/**
 * 颜色转换为十六进制字符串
 */
fun Color.toHexString(): String {
    return String.format("#%02X%02X%02X", red, green, blue)
}

/**
 * 十六进制字符串转换为颜色
 */
fun String.parseHexColor(): Color {
    if (!startsWith("#") || length != 7) {
        throw IllegalArgumentException("Invalid color format: $this")
    }

    val hex = substring(1)
    val r = hex.substring(0, 2).toInt(16)
    val g = hex.substring(2, 4).toInt(16)
    val b = hex.substring(4, 6).toInt(16)

    return Color.fromRGB(r, g, b)
}

/**
 * 音符转换为音高
 */
fun String.toPitch(): Float {
    return when (this.uppercase()) {
        // 第2八度
        "C2" -> 0.5f
        "C♯2", "D♭2" -> 0.53f
        "D2" -> 0.56f
        "D♯2", "E♭2" -> 0.59f
        "E2" -> 0.63f
        "F2" -> 0.67f
        "F♯2", "G♭2" -> 0.71f
        "G2" -> 0.75f
        "G♯2", "A♭2" -> 0.79f
        "A2" -> 0.84f
        "A♯2", "B♭2" -> 0.89f
        "B2" -> 0.94f

        // 第3八度
        "C3" -> 1.0f
        "C♯3", "D♭3" -> 1.06f
        "D3" -> 1.12f
        "D♯3", "E♭3" -> 1.19f
        "E3" -> 1.26f
        "F3" -> 1.33f
        "F♯3", "G♭3" -> 1.41f
        "G3" -> 1.5f
        "G♯3", "A♭3" -> 1.59f
        "A3" -> 1.68f
        "A♯3", "B♭3" -> 1.78f
        "B3" -> 1.89f

        // 第4八度
        "C4" -> 2.0f
        "C♯4", "D♭4" -> 2.12f
        "D4" -> 2.24f
        "D♯4", "E♭4" -> 2.38f
        "E4" -> 2.52f
        "F4" -> 2.67f
        "F♯4", "G♭4" -> 2.83f
        "G4" -> 3.0f
        "G♯4", "A♭4" -> 3.17f
        "A4" -> 3.36f
        "A♯4", "B♭4" -> 3.56f
        "B4" -> 3.77f

        else -> 1.0f // 默认C3
    }
}
