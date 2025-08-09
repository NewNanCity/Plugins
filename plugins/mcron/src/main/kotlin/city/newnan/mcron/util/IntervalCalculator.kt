package city.newnan.mcron.util

import java.util.concurrent.TimeUnit

object IntervalCalculator {
    /**
     * 一段时间所对应的秒数
     */
    private val SECONDS_OF_12HOUR = TimeUnit.HOURS.toSeconds(12)
    private val SECONDS_OF_6HOUR = TimeUnit.HOURS.toSeconds(6)
    private val SECONDS_OF_3HOUR = TimeUnit.HOURS.toSeconds(3)
    private val SECONDS_OF_1HOUR = TimeUnit.HOURS.toSeconds(1)
    private val SECONDS_OF_30MIN = TimeUnit.MINUTES.toSeconds(30)
    private val SECONDS_OF_15MIN = TimeUnit.MINUTES.toSeconds(15)
    private val SECONDS_OF_8MIN = TimeUnit.MINUTES.toSeconds(8)
    private val SECONDS_OF_4MIN = TimeUnit.MINUTES.toSeconds(4)
    private val SECONDS_OF_MINUTE = TimeUnit.MINUTES.toSeconds(1)
    private val SECONDS_OF_15SEC = TimeUnit.SECONDS.toSeconds(15)
    private val SECONDS_OF_5SEC = TimeUnit.SECONDS.toSeconds(5)
    private val SECONDS_OF_SECOND = TimeUnit.SECONDS.toSeconds(1)

    /**
     * 一段时间所对应的毫秒数
     */
    private val MILLISECOND_OF_2DAY: Long = TimeUnit.DAYS.toMillis(2)
    private val MILLISECOND_OF_1DAY: Long = TimeUnit.DAYS.toMillis(1)
    private val MILLISECOND_OF_12HOUR: Long = TimeUnit.HOURS.toMillis(12)
    private val MILLISECOND_OF_4HOUR: Long = TimeUnit.HOURS.toMillis(4)
    private val MILLISECOND_OF_2HOUR: Long = TimeUnit.HOURS.toMillis(2)
    private val MILLISECOND_OF_HOUR: Long = TimeUnit.HOURS.toMillis(1)
    private val MILLISECOND_OF_30MIN: Long = TimeUnit.MINUTES.toMillis(30)
    private val MILLISECOND_OF_15MIN: Long = TimeUnit.MINUTES.toMillis(15)
    private val MILLISECOND_OF_5MIN: Long = TimeUnit.MINUTES.toMillis(5)
    private val MILLISECOND_OF_MINUTE: Long = TimeUnit.MINUTES.toMillis(1)
    private val MILLISECOND_OF_30SEC: Long = TimeUnit.SECONDS.toMillis(30)

    /**
     * 根据毫秒差获得合适的休眠间隔(秒为单位)
     * @param delta 毫秒差
     * @return 休眠间隔(秒为单位)
     */
    fun getIntervalSeconds(delta: Long): Long {
        // 小于30秒   - 每秒
        if (delta < MILLISECOND_OF_30SEC) return SECONDS_OF_SECOND

        // 小于1分钟  - 每5秒
        if (delta < MILLISECOND_OF_MINUTE) return SECONDS_OF_5SEC

        // 小于5分钟  - 每15秒
        if (delta < MILLISECOND_OF_5MIN) return SECONDS_OF_15SEC

        // 小于15分钟 - 每1分钟
        if (delta < MILLISECOND_OF_15MIN) return SECONDS_OF_MINUTE

        // 小于30分钟 - 每4分钟
        if (delta < MILLISECOND_OF_30MIN) return SECONDS_OF_4MIN

        // 小于1小时 - 每8分钟
        if (delta < MILLISECOND_OF_HOUR) return SECONDS_OF_8MIN

        // 小于2小时 - 每15分钟
        if (delta < MILLISECOND_OF_2HOUR) return SECONDS_OF_15MIN

        // 小于4小时 - 每半小时
        if (delta < MILLISECOND_OF_4HOUR) return SECONDS_OF_30MIN

        // 小于12小时 - 每1小时
        if (delta < MILLISECOND_OF_12HOUR) return SECONDS_OF_1HOUR

        // 小于1天 - 每3小时
        if (delta < MILLISECOND_OF_1DAY) return SECONDS_OF_3HOUR

        // 小于2天 - 每6小时 / 其他 - 每12小时
        return if (delta < MILLISECOND_OF_2DAY) SECONDS_OF_6HOUR else SECONDS_OF_12HOUR
    }
}