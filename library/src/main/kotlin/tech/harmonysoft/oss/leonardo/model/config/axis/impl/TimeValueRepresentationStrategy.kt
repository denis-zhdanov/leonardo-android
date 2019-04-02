package tech.harmonysoft.oss.leonardo.model.config.axis.impl

import tech.harmonysoft.oss.leonardo.model.text.TextWrapper
import tech.harmonysoft.oss.leonardo.model.text.ValueRepresentationStrategy
import java.util.*
import java.util.concurrent.TimeUnit

class TimeValueRepresentationStrategy(private val offsetMs: Long) : ValueRepresentationStrategy {

    constructor(timeZone: TimeZone) : this(timeZone.rawOffset.toLong())

    constructor() : this(TimeZone.getDefault())

    private val text = TextWrapper()

    override fun getLabel(value: Long, step: Long): TextWrapper {
        text.reset()
        val formatter = when (step) {
            in MILLISECOND..(SECOND - 1) -> FORMATTER_MILLISECOND
            in SECOND..(MINUTE - 1)      -> FORMATTER_SECOND
            in MINUTE..(DAY - 1)         -> FORMATTER_MINUTE
            in DAY..(MONTH - 1)          -> FORMATTER_DAY
            in MONTH..(YEAR - 1)         -> FORMATTER_MONTH
            else                         -> FORMATTER_YEAR
        }
        formatter(value + offsetMs, text)
        return text
    }

    override fun getMinifiedLabel(value: Long, step: Long): TextWrapper {
        // No minification
        return getLabel(value, step)
    }

    @Suppress("ArrayInDataClass")
    private data class DayInfo(val month: CharArray, val day: Int)

    companion object {

        val INSTANCE = TimeValueRepresentationStrategy()

        const val MILLISECOND: Long = 1
        val SECOND = TimeUnit.SECONDS.toMillis(1)
        val MINUTE = TimeUnit.MINUTES.toMillis(1)
        val HOUR = TimeUnit.HOURS.toMillis(1)
        val DAY = TimeUnit.DAYS.toMillis(1)
        val MONTH = DAY * 30
        val YEAR = 365 * DAY
        val LEAP_YEAR = YEAR + DAY + 30 * MINUTE
        val TWO_YEARS = 2 * YEAR
        val YEAR_CYCLE = 4 * YEAR + DAY

        private val FEBRUARY_29 = (31 + 28 - 1).toLong()

        private val MONTHS = arrayOfNulls<DayInfo>(365)

        init {
            var offset = 0
            offset += addMonths("Jan", offset, 31)
            offset += addMonths("Feb", offset, 28)
            offset += addMonths("Mar", offset, 31)
            offset += addMonths("Apr", offset, 30)
            offset += addMonths("May", offset, 31)
            offset += addMonths("Jun", offset, 30)
            offset += addMonths("Jul", offset, 31)
            offset += addMonths("Aug", offset, 31)
            offset += addMonths("Sep", offset, 30)
            offset += addMonths("Oct", offset, 31)
            offset += addMonths("Nov", offset, 30)
            addMonths("Dec", offset, 31)
        }

        private fun addMonths(monthName: String, offset: Int, length: Int): Int {
            for (i in 0 until length) {
                MONTHS[offset + i] = DayInfo(monthName.toCharArray(), i + 1)
            }
            return length
        }

        private val FORMATTER_MILLISECOND = { value: Long, text: TextWrapper ->
            val trimmed = value % MINUTE
            appendAndPadIfNecessary(trimmed / SECOND, 10, text)
            text.append('.')
            appendAndPadIfNecessary(trimmed % SECOND, 100, text)
        }

        private val FORMATTER_SECOND = { value: Long, text: TextWrapper ->
            val trimmed = value % HOUR
            appendAndPadIfNecessary(trimmed / MINUTE, 10, text)
            text.append(':')
            appendAndPadIfNecessary(trimmed % MINUTE / SECOND, 10, text)
        }

        private val FORMATTER_MINUTE = { value: Long, text: TextWrapper ->
            val trimmed = value % DAY
            appendAndPadIfNecessary(trimmed / HOUR, 10, text)
            text.append(':')
            appendAndPadIfNecessary(trimmed % HOUR / MINUTE, 10, text)
        }

        private val FORMATTER_DAY =
            { value: Long, text: TextWrapper -> processLeapAwareTime(value, text, false, true, true) }

        private val FORMATTER_MONTH =
            { value: Long, text: TextWrapper -> processLeapAwareTime(value, text, false, true, false) }

        private val FORMATTER_YEAR =
            { value: Long, text: TextWrapper -> processLeapAwareTime(value, text, true, false, false) }

        private fun appendAndPadIfNecessary(value: Long, base: Long, text: TextWrapper) {
            var v = value
            var b = base
            while (b > 0) {
                text.append(v / b)
                v %= b
                b /= 10
            }
        }

        private fun processLeapAwareTime(
            value: Long,
            text: TextWrapper,
            printYear: Boolean,
            printMonth: Boolean,
            printDay: Boolean
        ) {
            var year = 1968
            var trimmed = (value + TWO_YEARS) % YEAR_CYCLE
            year += (4 * ((value + TWO_YEARS) / YEAR_CYCLE)).toInt()
            val leap = trimmed <= LEAP_YEAR
            while (trimmed >= YEAR) {
                trimmed -= YEAR
                year++
            }

            val days = trimmed / DAY

            if (printYear) {
                text.append(year)
                if (!printMonth) {
                    return
                }
            }

            if (leap && days == FEBRUARY_29) {
                text.append(MONTHS[32]!!.month)
                if (printDay) {
                    text.append(" 29")
                }

            } else {
                val dayInfo = MONTHS[days.toInt()]
                text.append(dayInfo!!.month)
                if (printDay) {
                    text.append(' ')
                    text.append(dayInfo.day)
                }
            }
        }
    }
}