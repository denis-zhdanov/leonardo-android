package tech.harmonysoft.oss.leonardo.model.runtime

import tech.harmonysoft.oss.leonardo.model.DataPoint
import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.data.ChartDataSource
import tech.harmonysoft.oss.leonardo.model.util.RangesList
import java.util.*

/**
 * All methods are assumed to be called from UI thread.
 */
interface ChartModel {

    val hasSelection: Boolean
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

    fun arePointsForActiveRangeLoaded(dataSource: ChartDataSource, anchor: Any): Boolean

    /**
     * @param dataSource    target data source
     * @param anchor        target anchor
     * @return              sorted collection of given source's data points
     */
    fun getCurrentRangePoints(dataSource: ChartDataSource, anchor: Any): NavigableSet<DataPoint>
    fun getPreviousPointForActiveRange(dataSource: ChartDataSource, anchor: Any): DataPoint?
    fun getNextPointForActiveRange(dataSource: ChartDataSource, anchor: Any): DataPoint?

    fun getLoadedRanges(dataSource: ChartDataSource): RangesList

    fun onPointsLoaded(dataSource: ChartDataSource, range: Range, points: Collection<DataPoint>)

    fun addListener(listener: ChartModelListener)
    fun removeListener(listener: ChartModelListener)
}