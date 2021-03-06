package tech.harmonysoft.oss.leonardo.model.runtime

import tech.harmonysoft.oss.leonardo.model.DataPoint
import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.data.ChartDataSource
import tech.harmonysoft.oss.leonardo.model.util.RangesList

/**
 * All methods are assumed to be called from UI thread.
 */
interface ChartModel {

    val hasSelection: Boolean
    val minX: Long?
    val maxX: Long?
    var selectedX: Long
    fun resetSelection()

    fun getActiveRange(anchor: Any): Range
    val bufferRange: Range

    fun setActiveRange(range: Range, anchor: Any)

    val registeredDataSources: Collection<ChartDataSource>
    fun isActive(dataSource: ChartDataSource): Boolean
    fun addDataSource(dataSource: ChartDataSource)
    fun disableDataSource(dataSource: ChartDataSource)
    fun enableDataSource(dataSource: ChartDataSource)
    fun removeDataSource(dataSource: ChartDataSource)

    fun getThisOrPrevious(dataSource: ChartDataSource, x: Long): DataPoint?
    fun getThisOrNext(dataSource: ChartDataSource, x: Long): DataPoint?
    fun getNext(dataSource: ChartDataSource, x: Long): DataPoint?

    fun isLoadingInProgress(dataSource: ChartDataSource): Boolean
    fun forRangePoints(dataSource: ChartDataSource,
                       start: Long,
                       end: Long,
                       includePrevious: Boolean = false,
                       includeNext: Boolean = false,
                       action: (DataPoint) -> Boolean)

    fun getLoadedRanges(dataSource: ChartDataSource): RangesList
    fun onPointsLoaded(dataSource: ChartDataSource, range: Range, points: Iterable<DataPoint>)

    fun getMin(dataSource: ChartDataSource): Long?
    fun getMax(dataSource: ChartDataSource): Long?

    fun addListener(listener: ChartModelListener)
    fun removeListener(listener: ChartModelListener)
}