package tech.harmonysoft.oss.leonardo.example.data.input.predefined

import tech.harmonysoft.oss.leonardo.collection.DataTreeImpl
import tech.harmonysoft.oss.leonardo.model.DataPoint
import tech.harmonysoft.oss.leonardo.model.data.ChartDataLoader
import tech.harmonysoft.oss.leonardo.model.data.LoadHandle
import tech.harmonysoft.oss.leonardo.model.util.LeonardoUtil
import tech.harmonysoft.oss.leonardo.model.util.LeonardoUtil.LONG_COMPARATOR

class PreDefinedChartDataLoader(points: Collection<DataPoint>) : ChartDataLoader {

    private val points = DataTreeImpl<Long, DataPoint>(LONG_COMPARATOR).apply {
        points.forEach { put(it.x, it) }
    }

    override fun load(from: Long, to: Long, handle: LoadHandle) {
        LeonardoUtil.forPoints(points, from, to) {
            handle.onPointLoaded(it.x, it.y)
            true
        }

        val first = points.first
        if (first != null && first.x >= from) {
            handle.onMinimumValue(first.x)
        }

        val last = points.last
        if (last != null && last.x <= to) {
            handle.onMaximumValue(last.x)
        }
        handle.onLoadingEnd()
    }
}