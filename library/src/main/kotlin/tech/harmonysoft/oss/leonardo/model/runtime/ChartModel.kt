package tech.harmonysoft.oss.leonardo.model.runtime

import tech.harmonysoft.oss.leonardo.model.DataPoint
import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.data.ChartDataSource
import tech.harmonysoft.oss.leonardo.model.util.RangesList

/**
 * All methods are assumed to be called from UI thread.
 *
 * @author Denis Zhdanov
 * @since 27/3/19
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
    fun getCurrentRangePoints(dataSource: ChartDataSource, anchor: Any): List<DataPoint>
    fun getPreviousPointForActiveRange(dataSource: ChartDataSource, anchor: Any): DataPoint?
    fun getNextPointForActiveRange(dataSource: ChartDataSource, anchor: Any): DataPoint?

    fun getLoadedRanges(dataSource: ChartDataSource): RangesList

    fun onPointsLoaded(dataSource: ChartDataSource, range: Range, points: Collection<DataPoint>)

    fun addListener(listener: ChartModelListener)
    fun removeListener(listener: ChartModelListener)
}