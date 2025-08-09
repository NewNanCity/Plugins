package city.newnan.mcron.util

import java.util.*

/**
 * Cron表达式预处理器
 *
 * 支持将多语言的星期和月份名称转换为数字，使其兼容cron-utils库。
 * 支持的语言：英文、中文、日文、韩文、法文、德文、西班牙文、俄文等。
 * 支持大小写不敏感的匹配。
 *
 * @author NewNanCity
 * @since 2.0.0
 */
object CronExpressionPreprocessor {

    // 星期名称映射 (Monday = 1, Tuesday = 2, ..., Sunday = 7)
    // 注意：cron-utils库使用1-7表示星期，其中1=Monday, 7=Sunday
    private val dayOfWeekMappings = mapOf(
        // 英文
        "sunday" to "7", "sun" to "7",
        "monday" to "1", "mon" to "1",
        "tuesday" to "2", "tue" to "2", "tues" to "2",
        "wednesday" to "3", "wed" to "3",
        "thursday" to "4", "thu" to "4", "thur" to "4", "thurs" to "4",
        "friday" to "5", "fri" to "5",
        "saturday" to "6", "sat" to "6",

        // 中文
        "星期日" to "7", "星期天" to "7", "周日" to "7", "周天" to "7", "日" to "7",
        "星期一" to "1", "周一" to "1", "一" to "1",
        "星期二" to "2", "周二" to "2", "二" to "2",
        "星期三" to "3", "周三" to "3", "三" to "3",
        "星期四" to "4", "周四" to "4", "四" to "4",
        "星期五" to "5", "周五" to "5", "五" to "5",
        "星期六" to "6", "周六" to "6", "六" to "6",

        // 日文
        "日曜日" to "7", "日曜" to "7",
        "月曜日" to "1", "月曜" to "1",
        "火曜日" to "2", "火曜" to "2",
        "水曜日" to "3", "水曜" to "3",
        "木曜日" to "4", "木曜" to "4",
        "金曜日" to "5", "金曜" to "5",
        "土曜日" to "6", "土曜" to "6",

        // 韩文
        "일요일" to "7", "일" to "7",
        "월요일" to "1", "월" to "1",
        "화요일" to "2", "화" to "2",
        "수요일" to "3", "수" to "3",
        "목요일" to "4", "목" to "4",
        "금요일" to "5", "금" to "5",
        "토요일" to "6", "토" to "6",

        // 法文
        "dimanche" to "7", "dim" to "7",
        "lundi" to "1", "lun" to "1",
        "mardi" to "2", "mar" to "2",
        "mercredi" to "3", "mer" to "3",
        "jeudi" to "4", "jeu" to "4",
        "vendredi" to "5", "ven" to "5",
        "samedi" to "6", "sam" to "6",

        // 德文
        "sonntag" to "7", "so" to "7",
        "montag" to "1", "mo" to "1",
        "dienstag" to "2", "di" to "2",
        "mittwoch" to "3", "mi" to "3",
        "donnerstag" to "4", "do" to "4",
        "freitag" to "5", "fr" to "5",
        "samstag" to "6", "sa" to "6",

        // 西班牙文
        "domingo" to "7", "dom" to "7",
        "lunes" to "1", "lun" to "1",
        "martes" to "2", "mar" to "2",
        "miércoles" to "3", "mié" to "3", "miercoles" to "3", "mie" to "3",
        "jueves" to "4", "jue" to "4",
        "viernes" to "5", "vie" to "5",
        "sábado" to "6", "sáb" to "6", "sabado" to "6", "sab" to "6",

        // 俄文
        "воскресенье" to "7", "вс" to "7",
        "понедельник" to "1", "пн" to "1",
        "вторник" to "2", "вт" to "2",
        "среда" to "3", "ср" to "3",
        "четверг" to "4", "чт" to "4",
        "пятница" to "5", "пт" to "5",
        "суббота" to "6", "сб" to "6"
    )

    // 月份名称映射 (January = 1, February = 2, ..., December = 12)
    private val monthMappings = mapOf(
        // 英文
        "january" to "1", "jan" to "1",
        "february" to "2", "feb" to "2",
        "march" to "3", "mar" to "3",
        "april" to "4", "apr" to "4",
        "may" to "5",
        "june" to "6", "jun" to "6",
        "july" to "7", "jul" to "7",
        "august" to "8", "aug" to "8",
        "september" to "9", "sep" to "9", "sept" to "9",
        "october" to "10", "oct" to "10",
        "november" to "11", "nov" to "11",
        "december" to "12", "dec" to "12",

        // 中文
        "一月" to "1", "1月" to "1",
        "二月" to "2", "2月" to "2",
        "三月" to "3", "3月" to "3",
        "四月" to "4", "4月" to "4",
        "五月" to "5", "5月" to "5",
        "六月" to "6", "6月" to "6",
        "七月" to "7", "7月" to "7",
        "八月" to "8", "8月" to "8",
        "九月" to "9", "9月" to "9",
        "十月" to "10", "10月" to "10",
        "十一月" to "11", "11月" to "11",
        "十二月" to "12", "12月" to "12",

        // 日文
        "一月" to "1", "1月" to "1",
        "二月" to "2", "2月" to "2",
        "三月" to "3", "3月" to "3",
        "四月" to "4", "4月" to "4",
        "五月" to "5", "5月" to "5",
        "六月" to "6", "6月" to "6",
        "七月" to "7", "7月" to "7",
        "八月" to "8", "8月" to "8",
        "九月" to "9", "9月" to "9",
        "十月" to "10", "10月" to "10",
        "十一月" to "11", "11月" to "11",
        "十二月" to "12", "12月" to "12",

        // 法文
        "janvier" to "1", "janv" to "1",
        "février" to "2", "févr" to "2", "fevrier" to "2", "fevr" to "2",
        "mars" to "3",
        "avril" to "4", "avr" to "4",
        "mai" to "5",
        "juin" to "6",
        "juillet" to "7", "juil" to "7",
        "août" to "8", "aout" to "8",
        "septembre" to "9", "sept" to "9",
        "octobre" to "10", "oct" to "10",
        "novembre" to "11", "nov" to "11",
        "décembre" to "12", "déc" to "12", "decembre" to "12", "dec" to "12",

        // 德文
        "januar" to "1", "jan" to "1",
        "februar" to "2", "feb" to "2",
        "märz" to "3", "mär" to "3", "maerz" to "3", "maer" to "3",
        "april" to "4", "apr" to "4",
        "mai" to "5",
        "juni" to "6", "jun" to "6",
        "juli" to "7", "jul" to "7",
        "august" to "8", "aug" to "8",
        "september" to "9", "sep" to "9",
        "oktober" to "10", "okt" to "10",
        "november" to "11", "nov" to "11",
        "dezember" to "12", "dez" to "12",

        // 西班牙文
        "enero" to "1", "ene" to "1",
        "febrero" to "2", "feb" to "2",
        "marzo" to "3", "mar" to "3",
        "abril" to "4", "abr" to "4",
        "mayo" to "5", "may" to "5",
        "junio" to "6", "jun" to "6",
        "julio" to "7", "jul" to "7",
        "agosto" to "8", "ago" to "8",
        "septiembre" to "9", "sep" to "9",
        "octubre" to "10", "oct" to "10",
        "noviembre" to "11", "nov" to "11",
        "diciembre" to "12", "dic" to "12",

        // 俄文
        "январь" to "1", "янв" to "1",
        "февраль" to "2", "фев" to "2",
        "март" to "3", "мар" to "3",
        "апрель" to "4", "апр" to "4",
        "май" to "5",
        "июнь" to "6", "июн" to "6",
        "июль" to "7", "июл" to "7",
        "август" to "8", "авг" to "8",
        "сентябрь" to "9", "сен" to "9",
        "октябрь" to "10", "окт" to "10",
        "ноябрь" to "11", "ноя" to "11",
        "декабрь" to "12", "дек" to "12"
    )

    // 特殊范围映射
    private val specialRangeMappings = mapOf(
        // 英文工作日/周末
        "weekdays" to "1-5",
        "weekday" to "1-5",
        "workdays" to "1-5",
        "workday" to "1-5",
        "weekends" to "6,7",
        "weekend" to "6,7",

        // 中文工作日/周末
        "工作日" to "1-5",
        "平日" to "1-5",
        "周末" to "6,7",
        "双休日" to "6,7",

        // 英文范围
        "mon-fri" to "1-5",
        "monday-friday" to "1-5",
        "sat-sun" to "6,7",
        "saturday-sunday" to "6,7",

        // 中文范围
        "周一到周五" to "1-5",
        "周一至周五" to "1-5",
        "星期一到星期五" to "1-5",
        "星期一至星期五" to "1-5",
        "周六到周日" to "6,7",
        "周六至周日" to "6,7",
        "星期六到星期日" to "6,7",
        "星期六至星期日" to "6,7"
    )

    /**
     * 预处理cron表达式
     *
     * @param expression 原始cron表达式
     * @return 处理后的cron表达式
     */
    fun preprocess(expression: String): String {
        var parts1 = expression.trim().split(Regex("\\s+")).toList()

        // 如果只有五个部分说明为unix，需要补足秒部分(0)
        // 最终对齐到QUARTZ格式：秒、分、时、日、月、周、年（年字段可选）
        if (parts1.size == 5) {
            parts1 = listOf("0") + parts1
        }

        // 变 Array
        val parts = parts1.toTypedArray()

        // 处理特殊范围
        var anyDayOfWeek = false
        var anyDayOfMonth = false
        var notCareDayOfWeek = false
        var notCareDayOfMonth = false
        for (idx in parts.indices) {
            when (idx) {
                3 -> { // 日字段
                    anyDayOfMonth = parts[idx] == "*"
                    notCareDayOfMonth = parts[idx] == "?"
                }
                4 -> { // 月份字段
                    var p = parts[idx]
                    monthMappings.forEach { (pattern, replacement) ->
                        p = replaceWholeWord(p, pattern, replacement)
                    }
                    parts[idx] = p
                }
                5 -> { // 星期字段
                    var p = parts[idx]
                    specialRangeMappings.forEach { (pattern, replacement) ->
                        p = p.replace(pattern, replacement, ignoreCase = true)
                    }
                    dayOfWeekMappings.forEach { (pattern, replacement) ->
                        p = replaceWholeWord(p, pattern, replacement)
                    }
                    notCareDayOfWeek = p == "?"
                    anyDayOfWeek = p == "*"
                    parts[idx] = p
                }
            }
        }

        when {
            // 情况1：两个都是'?' -> 非法，修复为日='*'，周='?'（每天触发）
            notCareDayOfMonth && notCareDayOfWeek -> {
                parts[3] = "*"  // 日字段设为*
                parts[5] = "?"  // 周字段保持?
            }

            // 情况2：两个都不是'?'
            !notCareDayOfMonth && !notCareDayOfWeek -> {
                when {
                    // 2a: 两个都是'*' -> 修复为日='*'，周='?'（每天触发）
                    anyDayOfMonth && anyDayOfWeek -> {
                        parts[5] = "?"  // 周字段改为?
                    }
                    // 2b: 日='*'，周是具体值 -> 修复为日='?'，周不变（按周触发）
                    anyDayOfMonth && !anyDayOfWeek -> {
                        parts[3] = "?"  // 日字段改为?
                    }
                    // 2c: 日是具体值，周='*' -> 修复为周='?'，日不变（按日触发）
                    !anyDayOfMonth && anyDayOfWeek -> {
                        parts[5] = "?"  // 周字段改为?
                    }
                    // 2d: 两个都是具体值 -> 保持原样（合法但语义为"或"）
                }
            }
        }

        return parts.joinToString(" ")
    }

    /**
     * 替换完整单词（避免部分匹配）
     * 支持中文和其他Unicode字符
     */
    private fun replaceWholeWord(text: String, pattern: String, replacement: String): String {
        // 对于中文和其他Unicode字符，使用更宽泛的边界检查
        // 检查前后是否为空白字符、标点符号或字符串边界
        val escapedPattern = Regex.escape(pattern)
        val regex = "(?<=^|\\s|[,\\-/])$escapedPattern(?=\\s|[,\\-/]|$)".toRegex(RegexOption.IGNORE_CASE)
        return regex.replace(text, replacement)
    }
}
