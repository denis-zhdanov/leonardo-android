package tech.harmonysoft.oss.leonardo.example.data

import tech.harmonysoft.oss.leonardo.model.DataPoint
import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.data.ChartDataLoader
import java.util.*

class InfiniteChartDataLoader : ChartDataLoader {

    override fun load(range: Range): Collection<DataPoint>? {
        val points = mutableListOf<DataPoint>()
        val random = Random()
        var prevY = random.nextInt(Y_RANGE_LENGTH) + Y_MIN
        for (x in range.start..range.end) {
            val sign = if (random.nextBoolean()) 1 else -1
            val y = prevY + sign * random.nextInt(Y_RANGE_LENGTH / 10) + Y_MIN
            prevY = y
            points.add(DataPoint(x, y.toLong()))
        }
        return points
    }

    companion object {
        private const val Y_MIN = 0
        private const val Y_MAX = 120
        private const val Y_RANGE_LENGTH = Y_MAX - Y_MIN + 1
    }
}