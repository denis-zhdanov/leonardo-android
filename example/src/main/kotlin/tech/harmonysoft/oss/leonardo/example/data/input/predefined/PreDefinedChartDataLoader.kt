package tech.harmonysoft.oss.leonardo.example.data.input.predefined

import tech.harmonysoft.oss.leonardo.model.DataPoint
import tech.harmonysoft.oss.leonardo.model.DataPointAnchor
import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.WithComparableLongProperty
import tech.harmonysoft.oss.leonardo.model.data.ChartDataLoader
import java.util.*

class PreDefinedChartDataLoader(points: Collection<DataPoint>) : ChartDataLoader {

    private val points = TreeSet<WithComparableLongProperty>(WithComparableLongProperty.COMPARATOR).apply {
        addAll(points)
    }

    private val bottomDataAnchor = DataPointAnchor()
    private val topDataAnchor = DataPointAnchor()

    @Suppress("UNCHECKED_CAST")
    override fun load(range: Range): Collection<DataPoint>? {
        if (points.isEmpty()) {
            return null
        }

        val last = points.last() as DataPoint
        if (range.start > last.x) {
            return null
        }

        val first = points.first() as DataPoint
        if (range.end < first.x) {
            return null
        }

        topDataAnchor.value = range.end
        val filteredFromTop = points.headSet(topDataAnchor, true)

        bottomDataAnchor.value = range.start
        return filteredFromTop.tailSet(bottomDataAnchor, true) as NavigableSet<DataPoint>
    }
}