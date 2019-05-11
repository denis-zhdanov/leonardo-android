package tech.harmonysoft.oss.leonardo.example.data.input.predefined

import tech.harmonysoft.oss.leonardo.collection.DataTreeImpl
import tech.harmonysoft.oss.leonardo.model.DataPoint
import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.data.ChartDataLoader
import tech.harmonysoft.oss.leonardo.model.util.LeonardoUtil
import tech.harmonysoft.oss.leonardo.model.util.LeonardoUtil.LONG_COMPARATOR

class PreDefinedChartDataLoader(points: Collection<DataPoint>) : ChartDataLoader {

    private val points = DataTreeImpl<Long, DataPoint>(LONG_COMPARATOR).apply {
        points.forEach { put(it.x, it) }
    }

    private var pointsView = mutableListOf<DataPoint>()
    private var pointsPopulater: (DataPoint) -> Boolean = this::addPoint

    @Suppress("UNCHECKED_CAST")
    override fun load(range: Range): Collection<DataPoint>? {
        if (points.empty) {
            return null
        }

        val last = points.last ?: return null
        if (range.start > last.x) {
            return null
        }

        val first = points.first ?: return null
        if (range.end < first.x) {
            return null
        }

        pointsView.clear()
        LeonardoUtil.forPoints(points = points, start = range.start, end = range.end, action = pointsPopulater)
        return pointsView
    }

    private fun addPoint(point: DataPoint): Boolean {
        pointsView.add(point)
        return true
    }
}