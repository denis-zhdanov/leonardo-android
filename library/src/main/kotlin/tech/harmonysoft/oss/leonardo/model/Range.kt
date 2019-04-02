package tech.harmonysoft.oss.leonardo.model

/**
 * Target data range with inclusive boundaries.
 *
 * @author Denis Zhdanov
 * @since 26/3/19
 */
data class Range(val start: Long, val end: Long) {

    val empty: Boolean get() = start > end
    val size: Int get() = (end - start).toInt()

    fun shift(delta: Long): Range {
        return Range(start + delta, end + delta)
    }

    fun findFirstStepValue(step: Long): Long {
        if (step <= 0) {
            throw IllegalArgumentException("Given step value ($step) is negative. Range: $this")
        }

        if (step > end && step > -start) {
            return Long.MIN_VALUE
        }

        val value = step * (start / step)
        return when {
            value >= start -> value
            value + step <= end -> value + step
            else -> Long.MIN_VALUE
        }
    }

    operator fun contains(value: Long): Boolean {
        return value in start..end
    }

    fun padBy(padSize: Long): Range {
        val startToPad = start % padSize
        val startToUse = when {
            startToPad > 0 -> start - startToPad
            startToPad < 0 -> start - (padSize + startToPad)
            else -> start - padSize
        }

        val endToPad = end % padSize
        val endToUse = when {
            endToPad > 0 -> end + (padSize - endToPad)
            endToPad < 0 -> end - endToPad
            else -> end + padSize
        }

        return if (startToUse == start && endToUse == end) {
            this
        } else {
            Range(startToUse, endToUse)
        }
    }

    companion object {
        val EMPTY_RANGE = Range(-1, 0)
    }
}