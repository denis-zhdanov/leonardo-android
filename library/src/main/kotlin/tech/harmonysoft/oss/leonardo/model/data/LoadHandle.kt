package tech.harmonysoft.oss.leonardo.model.data

import tech.harmonysoft.oss.leonardo.model.DataPoint

interface LoadHandle {

    val cancelled: Boolean

    fun onMinimumValue(value: Long)

    fun onMaximumValue(value: Long)

    fun onPointLoaded(x: Long, y: Long)

    fun onPointsLoaded(points: Iterable<DataPoint>)

    /**
     * The API is async, hence, there is a possible case that actual loading is performed
     * in a background thread (e.g. using a library solution). Also it's possible that the data is loaded
     * in batches, hence, we need explicitly signal about data loading iteration end.
     *
     * This method serves that purpose
     */
    fun onLoadingEnd()
}