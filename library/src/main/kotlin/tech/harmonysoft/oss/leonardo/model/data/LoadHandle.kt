package tech.harmonysoft.oss.leonardo.model.data

import tech.harmonysoft.oss.leonardo.model.DataPoint

interface LoadHandle {

    val cancelled: Boolean

    fun onMinimumValue(value: Long)

    fun onMaximumValue(value: Long)

    fun onPointLoaded(x: Long, y: Long)

    fun onPointsLoaded(points: Iterable<DataPoint>)
}