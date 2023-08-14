package city.newnan.railarea.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.bukkit.Sound
import kotlin.math.pow


data class WorldSize(val x1: Int, val z1: Int, val x2: Int, val z2: Int)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ConfigFile(
    @JsonProperty("world-size")
    val worldSize: Map<String, WorldSize>,
    @JsonProperty("waiting-seconds")
    val waitingSeconds: Int = 5,
    @JsonProperty("start-warning-sound")
    val startWarningSound: Sound = Sound.BLOCK_NOTE_BLOCK_BIT,
    @JsonProperty("start-warning-second")
    val startWarningSecond: Int = 3,
    @JsonProperty("start-speed")
    val startSpeed: Double = 1.0,
    @JsonProperty("arrive-music")
    val arriveMusic: List<String> = emptyList(),
    @JsonProperty("start-music")
    val startMusic: List<String> = emptyList(),
)

val numberPitch = (0..24).map { 2.0.pow((it - 12) / 12.0).toFloat() }.toTypedArray()

fun String.toPitch(): Float =
    when (this) {
        "0", "F#2", "Gb2", "F♯2", "G♭2" -> numberPitch[0]
        "1", "G2" -> numberPitch[1]
        "2", "G#2", "Ab2", "G♯2", "A♭2" -> numberPitch[2]
        "3", "A2" -> numberPitch[3]
        "4", "A#2", "Bb2", "A♯2", "B♭2" -> numberPitch[4]
        "5", "B2" -> numberPitch[5]
        "6", "C3" -> numberPitch[6]
        "7", "C#3", "Db3", "C♯3", "D♭3" -> numberPitch[7]
        "8", "D3" -> numberPitch[8]
        "9", "D#3", "Eb3", "D♯3", "E♭3" -> numberPitch[9]
        "10", "E3" -> numberPitch[10]
        "11", "F3" -> numberPitch[11]
        "12", "F#3", "Gb3", "F♯3", "G♭3" -> numberPitch[12]
        "13", "G3" -> numberPitch[13]
        "14", "G#3", "Ab3", "G♯3", "A♭3" -> numberPitch[14]
        "15", "A3" -> numberPitch[15]
        "16", "A#3", "Bb3", "A♯3", "B♭3" -> numberPitch[16]
        "17", "B3" -> numberPitch[17]
        "18", "C4" -> numberPitch[18]
        "19", "C#4", "Db4", "C♯4", "D♭4" -> numberPitch[19]
        "20", "D4" -> numberPitch[20]
        "21", "D#4", "Eb4", "D♯4", "E♭4" -> numberPitch[21]
        "22", "E4" -> numberPitch[22]
        "23", "F4" -> numberPitch[23]
        "24", "F#4", "Gb4", "F♯4", "G♭4" -> numberPitch[24]
        else -> numberPitch[12]
    }