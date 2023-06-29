package city.newnan.mcron

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import java.util.*
import kotlin.collections.HashMap


/**
 * 定时表达式类，比Linux的cron表达式多了一个"秒描述"
 * @param expressionString cron表达式，但是比Linux的cron表达式多了一个"秒描述"
 */
class CronExpression(expressionString: String) {
    private val secondList: IntArray
    private val minuteList: IntArray
    private val hourList: IntArray
    private val monthList: IntArray
    private val availableRegularDayOfWeek: List<Boolean>?

    // 虚拟转轮算法
    // 很简单，秒→分→时→天→月→年
    private var secondListPointer = 0
    private var minuteListPointer = 0
    private var hourListPointer = 0
    private var dayPointer = 0
    private var monthListPointer = 0
    private var yearPointer = 0

    /**
     * Cron表达式
     */
    internal val expressionString: String

    /**
     * 毫秒时间戳，本地时间，也就是说，不是从标准格林尼治时间1970年开始，而是基于现在的时区
     * 值为0，代表着已经没有下一次，该表达式已失效
     */
    private var nextTime: Long = 1

    init {
        // 表达式分割
        val expressionSplits = expressionString.split(" ")

        // 秒解析
        secondList = parseToArray(expressionSplits[0], 60, LEGAL_SECOND_INTERVAL_LIST, null, false)

        // 分解析
        minuteList = parseToArray(expressionSplits[1], 60, LEGAL_MINUTE_INTERVAL_LIST, null, false)

        // 时解析
        hourList = parseToArray(expressionSplits[2], 24, LEGAL_HOUR_INTERVAL_LIST, null, false)

        // 月解析
        monthList = parseToArray(expressionSplits[4], 13, LEGAL_MONTH_INTERVAL_LIST, MONTH_NAME_MAP, true)

        // 常规周解析
        availableRegularDayOfWeek = parseRegularDayOfWeekList(expressionSplits[5])

        // 存储表达式
        this.expressionString = expressionString

        // 初始化工作
        tickNext(true)
    }

    /**
     * 获取该表达式在未来的最近一次执行时间
     * @return 未来最近一次执行的毫秒时间戳(本地时间，考虑时区)；如果表达式已失效，即不存在未来满足条件的执行时刻，则返回0
     */
    fun getNextTime(): Long {
        // 失效的表达式，没有下一次了
        if (nextTime == 0L) return 0
        // 获得当前时间
        val curTime = System.currentTimeMillis()
        // 如果当前时间已晚于上次预计执行的时间，那么就计算下一次执行时间
        // 或者，表达式失效
        while (curTime >= nextTime || nextTime == 0L) {
            tickNext(false)
        }
        return nextTime
    }

    /**
     * 获取下一个可用时刻，或者初始化获得最近的下一个可用时刻
     * @param initFlag 初始化标志
     */
    private fun tickNext(initFlag: Boolean) {
        // 如果是初始化模式
        if (initFlag) {
            // 重置转轮
            secondListPointer = -1
            minuteListPointer = -1
            hourListPointer = -1
            dayPointer = -1
            monthListPointer = -1
            yearPointer = -1
            val curTime = LocalDateTime.now()

            // 先定位下一个可用天(含今天)
            tickNextDay(curTime)

            // 如果是今天，那么时分秒也要定位到下一个可用时刻；
            // 如果不是今天，时分秒只要指向第一个可用时刻就可以
            if (curTime.dayOfMonth == dayPointer && curTime.monthValue == monthList[monthListPointer] && curTime.year == yearPointer) {
                secondListPointer = 0
                minuteListPointer = 0
                hourListPointer = 0
            } else {
                var carrier: Int
                var tmp = 0
                while (tmp < secondList.size) {
                    if (curTime.second <= secondList[tmp]) {
                        break
                    }
                    tmp++
                }
                // 如果都没找到，就说明需要到下一个可用分钟去找，故进位并指向第一个可用秒
                if (tmp == secondList.size) {
                    // 进位1
                    carrier = 1
                    secondListPointer = 0
                } else {
                    carrier = 0
                    secondListPointer = tmp
                }
                tmp = 0
                while (tmp < minuteList.size) {
                    if (curTime.minute + carrier <= minuteList[tmp]) {
                        break
                    }
                    tmp++
                }
                // 如果都没找到，就说明需要到下一个可用小时去找，故进位并指向第一个可用时
                if (tmp == minuteList.size) {
                    // 进位1
                    carrier = 1
                    minuteListPointer = 0
                } else {
                    carrier = 0
                    minuteListPointer = tmp
                }
                tmp = 0
                while (tmp < hourList.size) {
                    if (curTime.hour + carrier <= hourList[tmp]) {
                        break
                    }
                    tmp++
                }
                // 如果没有找到，说明今天没有可用的时间，需要到下一个可用天去找
                if (tmp == hourList.size) {
                    secondListPointer = 0
                    minuteListPointer = 0
                    hourListPointer = 0
                    tickNextDay(curTime)
                } else {
                    hourListPointer = tmp
                }
            }
        } else {
            // 如果不是初始化模式
            secondListPointer = (secondListPointer + 1) % secondList.size
            if (secondListPointer == 0) {
                minuteListPointer = (minuteListPointer + 1) % minuteList.size
                if (minuteListPointer == 0) {
                    hourListPointer = (hourListPointer + 1) % hourList.size
                    if (hourListPointer == 0) {
                        tickNextDay(LocalDateTime.now())
                    }
                }
            }
        }

        // 说明该表达式已失效，没有下一次可执行的时间了
        if (nextTime == 0L) return

        // 获取时间戳，考虑时区
        nextTime = LocalDateTime.of(
            yearPointer,
            monthList[monthListPointer],
            dayPointer,
            hourList[hourListPointer],
            minuteList[minuteListPointer],
            secondList[secondListPointer]
        ).toInstant(localTimezoneOffset).toEpochMilli()
    }

    /**
     * 进到下一天
     * 由于年月日+周都是相互关联的，不能拆开，所以要一起考虑
     * @param curTime LocalDateTime实例，当前时间
     */
    private fun tickNextDay(curTime: LocalDateTime) {
        // 表达式分割
        val expressionSplits = expressionString.split(" ").toTypedArray()

        // 循环查找
        // 首先看看能不能直接找到本月的下一个可用日
        // 如果找不到(Day of Month、Day of Week和月天数三个条件)，就需要找下一个可用月
        // 找下一个可用月，如果找不到就需要找下一个可用年；
        // 此时如果存在下一个可用年就一定存在下一个可用月(可能不存在可用日)，否则所有年份都不存在可用月，这个可以用反证法证明
        // 所以寻找可用月不需要while循环
        while (!findNextDay(curTime, expressionSplits)) {
            // 如果有下一个可用年份，则至少存在一个可用月
            // 所以这个循环最多执行2次
            while (!findNextMonth(curTime)) {
                if (!findNextYear(curTime, expressionSplits)) {
                    // 没有下一个可用年份，该表达式已失效
                    nextTime = 0
                    return
                }
            }
        }
    }

    /**
     * 在当前monthListPointer内寻找下一个比当前dayPointer大的合法day
     * 如果dayPointer为-1，即未初始化，那么如果月份为-1则直接返回去初始化年份，否则指向当前年份月份大于等于当前日期的第一个日
     * @param expressionSplits Cron表达式分割
     * @return true为找到，false为未找到
     */
    private fun findNextDay(curTime: LocalDateTime, expressionSplits: Array<String>): Boolean {
        // -1代表未初始化
        if (dayPointer == -1) {
            dayPointer = if (monthListPointer == -1) {
                // 如果月份也未初始化，那么就先返回false去找year
                return false
            } else {
                // 如果月份初始化了，就找当前月份大于等于当前日的第一个日
                // 这里先将其置为今天的前一天或者当月0天(日从1开始)，这样自然就接上了
                if (yearPointer == curTime.year && monthList[monthListPointer] == curTime.monthValue) {
                    curTime.dayOfMonth - 1
                } else {
                    0
                }
            }
        }

        // 如果已经是最后一天，就本月就没有下一个可用日了
        // 初始化并返回false，首先去寻找一个新可用月份
        if (dayPointer == LAST_DAY_OF_MONTH[monthList[monthListPointer]]) {
            dayPointer = -1
            return false
        }

        // 接下来的任务就是去找比this.dayPointer大的符合条件的日子
        var nextDay = 32
        // 要同时考虑 Day of Month 和 Day of Week
        // 如果二者都是*，则为每个月的每一天
        // 如果只有一个为*，就只考虑一个
        // 如果都不是*，就是二者之一
        if (expressionSplits[3] == "*" && expressionSplits[5] == "*") {
            nextDay = dayPointer + 1
        } else {
            val theDate = LocalDateTime.of(
                yearPointer, monthList[monthListPointer],
                1, 0, 0
            )
            // Day of Month 部分
            if (expressionSplits[3] != "*") {
                for (splits in expressionSplits[3].split(",")) {
                    // 识别 'L' 每个月的最后一天
                    if (splits == "L") {
                        val lastDayOfMonth = theDate.with(TemporalAdjusters.lastDayOfMonth()).dayOfMonth
                        if (lastDayOfMonth < nextDay) {
                            nextDay = lastDayOfMonth
                        }
                        continue
                    }

                    // 识别 'W' 每个月第一个工作日
                    if (splits == "W") {
                        var firstWeekday = 1
                        val dayOfWeekInTheMonth = theDate.dayOfWeek
                        if (dayOfWeekInTheMonth == DayOfWeek.SATURDAY) {
                            firstWeekday += 2
                        } else if (dayOfWeekInTheMonth == DayOfWeek.SUNDAY) {
                            firstWeekday += 1
                        }
                        if (firstWeekday in (dayPointer + 1) until nextDay) {
                            nextDay = firstWeekday
                        }
                        continue
                    }

                    // 按 '-' 分割
                    val subSplits = splits.split("-")
                    if (subSplits.size == 2) {
                        val from = subSplits[0].toInt()
                        val to = subSplits[1].toInt()

                        // 如果这个区间在下一天已过去，就忽略
                        if (to <= dayPointer) {
                            continue
                        }

                        // 如果下一天处于区间内，就取第二天
                        if (from <= dayPointer + 1) {
                            nextDay = yearPointer + 1
                            break
                        }

                        // 如果下一天的时候该区间还未开始，就看看这个区间的开始
                        if (from < nextDay) nextDay = from
                    } else {
                        // 日点
                        val number = subSplits[0].toInt()

                        // 如果这个年份在之前年份之后，就可以考虑
                        if (number in (dayPointer + 1) until nextDay) nextDay = number
                    }
                }
            }
            // Day of Week 部分
            if (expressionSplits[5] != "*") {
                for (splits in expressionSplits[5].split(",")) {
                    // 先从表里面找最近的日期
                    if (availableRegularDayOfWeek != null) {
                        // 定义第二天而不是第一天，因为第一天可能是非法的(0)
                        val secondDate = LocalDateTime.of(
                            yearPointer,
                            monthList[monthListPointer],
                            dayPointer + 1, 0, 0
                        )
                        val curDayOfWeek = secondDate.dayOfWeek.value
                        var tmpDay = dayPointer + 1
                        var i = curDayOfWeek
                        for (j in 0..6) {
                            if (availableRegularDayOfWeek[i]) break
                            tmpDay++
                            i = if (i == 7) 1 else i + 1
                        }
                        if (tmpDay < nextDay) nextDay = tmpDay
                    }

                    // \d+#\d+部分
                    if (!splits.contains("-") && splits.contains("#")) {
                        val subSplits = splits.split("#").toTypedArray()
                        val dayOfWeek = DAYOFWEEK_NAME_MAP[subSplits[0].lowercase(Locale.getDefault())]!!
                        val numOfWeek = subSplits[1].toInt()
                        val day = theDate.with(
                            TemporalAdjusters.dayOfWeekInMonth(
                                numOfWeek,
                                DayOfWeek.of(dayOfWeek)
                            )
                        ).dayOfMonth
                        if (day in (dayPointer + 1) until nextDay) nextDay = day
                    }
                }
            }
        }

        // 非法日期视为无可用
        if (nextDay > LAST_DAY_OF_MONTH[monthList[monthListPointer]]) {
            dayPointer = -1
            return false
        }
        dayPointer = nextDay
        return true
    }

    /**
     * 在当前yearPointer内寻找下一个比当前monthListPointer大的合法month
     * 如果monthListPointer为-1，即未初始化，那么如果年份为-1则直接返回去初始化年份，否则指向当前年份大于等于当前月份的第一个月
     * @return true为找到，false为未找到
     */
    private fun findNextMonth(curTime: LocalDateTime): Boolean {
        // -1代表未初始化
        if (monthListPointer == -1) {
            if (yearPointer == -1) {
                // 如果年份也未初始化，那么就先返回false去找year
                return false
            } else {
                // 如果年份初始化了，就找当前年份大于等于当前月份的第一个月
                if (yearPointer == curTime.year) {
                    var i = 0
                    while (i < monthList.size) {
                        if (curTime.monthValue <= monthList[i]) break
                        i++
                    }
                    if (i == monthList.size) {
                        return false
                    }
                    monthListPointer = i
                    return true
                } else {
                    monthListPointer = 0
                }
            }
        } else {
            // 如果已经初始化了，那么就自然进一
            monthListPointer = (monthListPointer + 1) % monthList.size
            // 到达进位处，月复位，返回false以更新year，并再次初始化month
            if (monthListPointer == 0) {
                monthListPointer = -1
                return false
            }
        }
        // 否则，返回true
        return true
    }

    /**
     * 在当前寻找下一个可用的年份
     * 如果yearPointer为-1，即未初始化，会定位到离现在最近的未来可用年份(包括今年)
     * @param curTime LocalDateTime实例，现在的时间日期
     * @param expressionSplits Cron表达式分割
     * @return true为找到，false为未找到
     */
    private fun findNextYear(curTime: LocalDateTime, expressionSplits: Array<String>): Boolean {
        // -1，代表未初始化，那么就定位到最近的可用年份
        // 先把yearPointer设为去年，然后后面一样处理就可以了
        if (yearPointer == -1) {
            yearPointer = curTime.year - 1
        }

        // 如果没有年份描述段，或者识别到 '*' 通配符
        if (expressionSplits.size == 6 || expressionSplits[6] == "*") {
            // 那么年份简单地增加一年就好
            yearPointer++
        } else {
            // 否则就要找比其大的最小年份
            var nextYear = Int.MAX_VALUE
            for (splits in expressionSplits[6].split(",")) {
                // 按 '-' 分割
                val subSplits = splits.split("-")
                if (subSplits.size == 2) {
                    // 年份区间
                    val from = subSplits[0].toInt()
                    val to = subSplits[1].toInt()

                    // 如果这个区间在下一年已过去，就忽略
                    if (to <= yearPointer) {
                        continue
                    }

                    // 如果下一年处于区间内，就取明年
                    if (from <= yearPointer + 1) {
                        nextYear = yearPointer + 1
                        break
                    }

                    // 如果下一年的时候该区间还未开始，就看看这个区间的开始
                    if (from < nextYear) nextYear = from
                } else {
                    // 年份点
                    val number = subSplits[0].toInt()

                    // 如果这个年份在之前年份之后，就可以考虑
                    if (number in (yearPointer + 1) until nextYear) nextYear = number
                }
            }
            // 如果没有找到
            if (nextYear == Int.MAX_VALUE) {
                nextTime = 0
                return false
            }
            yearPointer = nextYear
        }
        return true
    }

    /**
     * 解析子表达式为int[]类型的位表
     * @param subExpression 子表达式，应满足正则表达式 (\d+(-\d+)?)(,\d+(-\d+)?)*
     * @param valueLimit 数值上线，排除非法数值，如秒是60，时是24
     * @return int[]实例
     */
    private fun parseToArray(
        subExpression: String,
        valueLimit: Int,
        legalIntervalList: List<Int>,
        optionalMap: Map<String, Int>?,
        startFromOne: Boolean
    ): IntArray {
        val tmpList = ArrayList<Int>()
        // 按 ',' 分割
        for (splits in subExpression.split(",")) {
            // 识别 '*' 通配符
            if (splits == "*") {
                for (i in (if (startFromOne) 1 else 0) until valueLimit) {
                    atomicListAdd(tmpList, i)
                }
                break
            }

            // 识别 '*/d+' 间隔通配
            if (splits.matches(Regex("\\*/\\d+"))) {
                val interval = parseInt(splits.split("/")[1], optionalMap)
                if (legalIntervalList.contains(interval)) {
                    var i = if (startFromOne) 1 else 0
                    while (i < valueLimit) {
                        atomicListAdd(tmpList, i)
                        i += interval
                    }
                }
                break
            }

            // 按 '-' 分割
            val subSplits = splits.split("-")
            if (subSplits.size == 2) {
                var from = parseInt(subSplits[0], optionalMap)
                var to = parseInt(subSplits[1], optionalMap)
                if (to > valueLimit) to = valueLimit
                while (from < to) {
                    atomicListAdd(tmpList, from)
                    from++
                }
            } else {
                val number = parseInt(subSplits[0], optionalMap)
                if (number <= valueLimit) atomicListAdd(tmpList, number)
            }
        }

        // List -> int[]
        tmpList.sort()
        val ans = IntArray(tmpList.size)
        for (i in tmpList.indices) {
            ans[i] = tmpList[i]
        }
        return ans
    }

    /**
     * 解析周表达式中的常规部分(不含'#'的部分)
     * @param weekExpression 周表达式
     * @return 周可用情况List
     */
    private fun parseRegularDayOfWeekList(weekExpression: String): List<Boolean>? {
        val list: MutableList<Boolean> = ArrayList(8)
        list.add(false)
        if (weekExpression == "*") {
            for (i in 1..7) list.add(true)
            return list
        }
        var ifEmpty = true
        for (i in 1..7) list.add(false)
        // 按 ',' 分割
        for (splits in weekExpression.split(",")) {
            // 按 '-' 分割
            val subSplits = splits.split("-")
            if (subSplits.size == 2) {
                val from = DAYOFWEEK_NAME_MAP[subSplits[0].lowercase(Locale.getDefault())]!!
                val to = DAYOFWEEK_NAME_MAP[subSplits[1].lowercase(Locale.getDefault())]!!
                var i = from
                while (i <= to) {
                    list[i] = true
                    ifEmpty = false
                    i = if (i == 7) 1 else i + 1
                }
            } else {
                // 忽略 \d+#\d+
                if (!splits.contains("#")) {
                    val num = DAYOFWEEK_NAME_MAP[subSplits[0].lowercase(Locale.getDefault())]!!
                    list[num] = true
                    ifEmpty = false
                }
            }
        }
        return if (ifEmpty) null else list
    }

    /**
     * 使用指定的映射Map来解析获得整数
     * @param s 待解析字符串
     * @param optionalMap 映射表
     * @return 解析得到的整数
     */
    private fun parseInt(s: String, optionalMap: Map<String, Int>?): Int {
        return if (optionalMap == null) {
            s.toInt()
        } else {
            optionalMap[s.lowercase(Locale.getDefault())]!!
        }
    }

    /**
     * 不重复地添加一个元素到List里
     * @param list List实例
     * @param object 元素实例
     * @param <T> List模板类型
    </T> */
    private fun <T> atomicListAdd(list: MutableList<T>, `object`: T) {
        if (list.contains(`object`)) return
        list.add(`object`)
    }

    @Deprecated("")
    internal fun printPointer() {
        System.out.printf("second: %d\n", secondList[secondListPointer])
        System.out.printf("minute: %d\n", monthList[minuteListPointer])
        System.out.printf("hour: %d\n", hourList[hourListPointer])
        System.out.printf("day: %d\n", dayPointer)
        System.out.printf("month: %d\n", monthList[monthListPointer])
        System.out.printf("year: %d\n", yearPointer)
    }

    companion object {
        private val LEGAL_SECOND_INTERVAL_LIST = listOf(2, 3, 4, 5, 6, 10, 12, 15, 20, 30)
        private val LEGAL_MINUTE_INTERVAL_LIST = LEGAL_SECOND_INTERVAL_LIST
        private val LEGAL_HOUR_INTERVAL_LIST = listOf(2, 3, 4, 6, 8, 12)
        private val LEGAL_MONTH_INTERVAL_LIST = listOf(2, 3, 4, 6)
        private val LAST_DAY_OF_MONTH = listOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        private val MONTH_NAME_MAP: HashMap<String, Int> = hashMapOf(
            "jan" to 1,
            "january" to 1,
            "1" to 1,
            "feb" to 2,
            "february" to 2,
            "2" to 2,
            "mar" to 3,
            "march" to 3,
            "3" to 3,
            "apr" to 4,
            "april" to 4,
            "4" to 4,
            "may" to 5,
            "5" to 5,
            "jun" to 6,
            "june" to 6,
            "6" to 6,
            "jul" to 7,
            "july" to 7,
            "7" to 7,
            "aug" to 8,
            "august" to 8,
            "8" to 8,
            "sept" to 9,
            "september" to 9,
            "9" to 9,
            "oct" to 10,
            "october" to 10,
            "10" to 10,
            "nov" to 11,
            "november" to 11,
            "11" to 11,
            "dec" to 12,
            "december" to 12,
            "12" to 12,
        )
        private val DAYOFWEEK_NAME_MAP: HashMap<String, Int> = hashMapOf(
            "mon" to 1,
            "monday" to 1,
            "1" to 1,
            "tue" to 2,
            "tuesday" to 2,
            "2" to 2,
            "wed" to 3,
            "wednesday" to 3,
            "3" to 3,
            "thu" to 4,
            "thursday" to 4,
            "4" to 4,
            "fri" to 5,
            "friday" to 5,
            "5" to 5,
            "sat" to 6,
            "saturday" to 6,
            "6" to 6,
            "sun" to 7,
            "sunday" to 7,
            "0" to 7,
            "7" to 7,
        )
        private var localTimezoneOffset = ZoneOffset.of("Z")

        /**
         * 重新设置时区
         * @param offsetId 时区ID
         */
        internal fun setTimeZoneOffset(offsetId: String?) {
            localTimezoneOffset = ZoneOffset.of(offsetId ?: "Z")
        }
    }
}