package tech.harmonysoft.oss.leonardo.model

import java.util.*

/**
 * We rather frequently want to filter data points sorted set by particular criteria. Memory profiling shows
 * that that creating new DataPoint object for that is rather expensive in terms of memory usage.
 *
 * That's why we provide a wrapper which allows to avoid memory allocations at all for such filtering operations.
 *
 * IMPORTANT: it's assumed that these functions are called from the UI thread only!
 */

private val dataPointAnchor = object : WithComparableLongProperty {
    override val property: Long
        get() = dataPointAnchorValue
}

private var dataPointAnchorValue = 0L

/**
 * @return the biggest element in the given set which is less than the given bound, if any
 */
@Suppress("UNCHECKED_CAST")
fun <T : WithComparableLongProperty> previous(bound: Long, points: NavigableSet<T>): T? {
    dataPointAnchorValue = bound
    val floor = (points as NavigableSet<WithComparableLongProperty>).floor(dataPointAnchor)
    return when {
        floor == null -> null
        floor.property < bound -> floor as T
        else -> points.lower(floor) as? T
    }
}

@Suppress("UNCHECKED_CAST")
fun <T : WithComparableLongProperty> equalOrLower(bound: Long, points: NavigableSet<T>): T? {
    dataPointAnchorValue = bound
    return (points as NavigableSet<WithComparableLongProperty>).floor(dataPointAnchor) as? T
}

/**
 * @return the smallest element in the given set which is greater than the given bound, if any
 */
@Suppress("UNCHECKED_CAST")
fun <T : WithComparableLongProperty> next(bound: Long, points: NavigableSet<T>): T? {
    dataPointAnchorValue = bound
    val ceiling = (points as NavigableSet<WithComparableLongProperty>).ceiling(dataPointAnchor)
    return when {
        ceiling == null -> null
        ceiling.property > bound -> ceiling as T
        else -> points.higher(ceiling) as? T
    }
}

interface WithComparableLongProperty {

    val property: Long

    companion object {
        val COMPARATOR = Comparator<WithComparableLongProperty> { o1, o2 ->
            when {
                o1.property < o2.property -> -1
                o1.property > o2.property -> 1
                else -> 0
            }
        }
    }
}

data class DataPointAnchor(var value: Long = 0L) : WithComparableLongProperty {

    override val property: Long get() = value
}

