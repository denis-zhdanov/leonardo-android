package tech.harmonysoft.oss.leonardo.model

/**
 * @author Denis Zhdanov
 * @since 26/3/19
 */
data class DataPoint(val x: Long, val y: Long) {

    companion object {
        val COMPARATOR_BY_X = Comparator<DataPoint> { p1, p2 ->
            p1.x.compareTo(p2.x)
        }
    }
}