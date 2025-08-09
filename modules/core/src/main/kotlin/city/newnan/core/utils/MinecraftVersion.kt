package city.newnan.core.utils

import org.bukkit.Bukkit
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

/**
 * Minecraft 版本封装类
 * 
 * 用于解析、比较和管理 Minecraft 版本信息。
 * 支持正式版本和快照版本的解析。
 * 
 * @author NewNanCity
 * @since 1.0.0
 */
data class MinecraftVersion(
    val major: Int,
    val minor: Int,
    val build: Int,
    val developmentStage: String? = null,
    val snapshot: SnapshotVersion? = null
) : Comparable<MinecraftVersion> {
    
    companion object {
        /**
         * 最新已知的 Minecraft 版本
         */
        private const val NEWEST_MINECRAFT_VERSION = "1.21.4"
        
        /**
         * 最新版本的发布日期
         */
        private const val MINECRAFT_LAST_RELEASE_DATE = "2024-12-03"
        
        /**
         * 版本解析正则表达式
         */
        private val VERSION_PATTERN = Pattern.compile(".*\\(.*MC.\\s*([a-zA-z0-9\\-\\.]+)\\s*\\)")
        
        /**
         * 运行时版本
         */
        val RUNTIME_VERSION: MinecraftVersion by lazy {
            parseServerVersion(Bukkit.getVersion())
        }
        
        /**
         * 常用版本常量
         */
        val v1_8 = parse("1.8")
        val v1_9 = parse("1.9")
        val v1_10 = parse("1.10")
        val v1_11 = parse("1.11")
        val v1_12 = parse("1.12")
        val v1_13 = parse("1.13")
        val v1_14 = parse("1.14")
        val v1_15 = parse("1.15")
        val v1_16 = parse("1.16")
        val v1_17 = parse("1.17")
        val v1_18 = parse("1.18")
        val v1_19 = parse("1.19")
        val v1_20 = parse("1.20")
        val v1_21 = parse("1.21")
        
        /**
         * 创建版本实例
         */
        fun of(major: Int, minor: Int, build: Int): MinecraftVersion {
            return MinecraftVersion(major, minor, build)
        }
        
        /**
         * 解析版本字符串
         */
        fun parse(version: String, parseSnapshot: Boolean = true): MinecraftVersion {
            val parts = version.split("-")
            var snapshot: SnapshotVersion? = null
            var versionComponents: IntArray
            
            try {
                versionComponents = parseVersion(parts[0])
            } catch (cause: NumberFormatException) {
                if (!parseSnapshot) throw cause
                
                try {
                    // 尝试解析快照版本
                    snapshot = SnapshotVersion.parse(parts[0])
                    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    
                    val latest = parse(NEWEST_MINECRAFT_VERSION, false)
                    val newer = snapshot.snapshotDate.compareTo(format.parse(MINECRAFT_LAST_RELEASE_DATE)) > 0
                    
                    versionComponents = intArrayOf(
                        latest.major,
                        latest.minor + if (newer) 1 else -1,
                        0
                    )
                } catch (e: Exception) {
                    throw IllegalArgumentException("Cannot parse $version", e)
                }
            }
            
            val major = versionComponents[0]
            val minor = versionComponents[1]
            val build = versionComponents[2]
            val development = if (parts.size > 1) parts[1] else if (snapshot != null) "snapshot" else null
            
            return MinecraftVersion(major, minor, build, development, snapshot)
        }
        
        /**
         * 解析服务器版本
         */
        private fun parseServerVersion(serverVersion: String): MinecraftVersion {
            val matcher = VERSION_PATTERN.matcher(serverVersion)
            
            return if (matcher.matches() && matcher.group(1) != null) {
                parse(matcher.group(1))
            } else {
                throw IllegalStateException("Cannot parse version String '$serverVersion'")
            }
        }
        
        /**
         * 解析版本组件
         */
        private fun parseVersion(version: String): IntArray {
            val elements = version.split(".")
            val numbers = IntArray(3)
            
            if (elements.isEmpty()) {
                throw IllegalStateException("Corrupt MC version: $version")
            }
            
            // 1 或 1.2 被解释为 1.0.0 和 1.2.0
            for (i in 0 until minOf(numbers.size, elements.size)) {
                numbers[i] = elements[i].trim().toInt()
            }
            
            return numbers
        }
    }
    
    /**
     * 是否为快照版本
     */
    val isSnapshot: Boolean
        get() = snapshot != null
    
    /**
     * 获取版本字符串
     */
    val version: String
        get() = if (developmentStage == null) {
            "$major.$minor.$build"
        } else {
            "$major.$minor.$build-$developmentStage${if (isSnapshot) snapshot else ""}"
        }
    
    /**
     * 比较版本
     */
    override fun compareTo(other: MinecraftVersion): Int {
        return compareValuesBy(this, other,
            { it.major },
            { it.minor },
            { it.build },
            { it.developmentStage },
            { it.snapshot }
        )
    }
    
    /**
     * 是否在指定版本之后
     */
    fun isAfter(other: MinecraftVersion): Boolean = compareTo(other) > 0
    
    /**
     * 是否在指定版本之后或相等
     */
    fun isAfterOrEq(other: MinecraftVersion): Boolean = compareTo(other) >= 0
    
    /**
     * 是否在指定版本之前
     */
    fun isBefore(other: MinecraftVersion): Boolean = compareTo(other) < 0
    
    /**
     * 是否在指定版本之前或相等
     */
    fun isBeforeOrEq(other: MinecraftVersion): Boolean = compareTo(other) <= 0
    
    /**
     * 是否在两个版本之间
     */
    fun isBetween(v1: MinecraftVersion, v2: MinecraftVersion): Boolean {
        return (isAfterOrEq(v1) && isBeforeOrEq(v2)) || (isBeforeOrEq(v1) && isAfterOrEq(v2))
    }
    
    override fun toString(): String = "(MC: $version)"
}

/**
 * 快照版本类
 */
data class SnapshotVersion(
    val snapshotDate: Date,
    val snapshotWeekVersion: Int,
    private val rawString: String? = null
) : Comparable<SnapshotVersion> {
    
    companion object {
        private val SNAPSHOT_PATTERN = Pattern.compile("(\\d{2}w\\d{2})([a-z])")
        
        /**
         * 解析快照版本
         */
        fun parse(version: String): SnapshotVersion {
            val matcher = SNAPSHOT_PATTERN.matcher(version.trim())
            
            if (matcher.matches()) {
                try {
                    val format = SimpleDateFormat("yy'w'ww", Locale.US).apply { isLenient = false }
                    val date = format.parse(matcher.group(1))
                    val weekVersion = matcher.group(2)[0] - 'a'
                    
                    return SnapshotVersion(date, weekVersion, version)
                } catch (e: Exception) {
                    throw IllegalArgumentException("Date implied by snapshot version is invalid.", e)
                }
            } else {
                throw IllegalArgumentException("Cannot parse $version as a snapshot version.")
            }
        }
    }
    
    /**
     * 获取快照字符串
     */
    val snapshotString: String
        get() = rawString ?: run {
            val calendar = Calendar.getInstance(Locale.US).apply { time = snapshotDate }
            String.format("%02dw%02d%s",
                calendar.get(Calendar.YEAR) % 100,
                calendar.get(Calendar.WEEK_OF_YEAR),
                ('a' + snapshotWeekVersion).toChar()
            )
        }
    
    override fun compareTo(other: SnapshotVersion): Int {
        return compareValuesBy(this, other,
            { it.snapshotDate },
            { it.snapshotWeekVersion }
        )
    }
    
    override fun toString(): String = snapshotString
}
