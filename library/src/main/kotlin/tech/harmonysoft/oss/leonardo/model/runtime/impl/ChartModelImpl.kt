package tech.harmonysoft.oss.leonardo.model.runtime.impl

import tech.harmonysoft.oss.leonardo.collection.DataTree
import tech.harmonysoft.oss.leonardo.collection.DataTreeImpl
import tech.harmonysoft.oss.leonardo.model.DataPoint
import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.data.ChartDataSource
import tech.harmonysoft.oss.leonardo.model.runtime.ChartModel
import tech.harmonysoft.oss.leonardo.model.runtime.ChartModelListener
import tech.harmonysoft.oss.leonardo.model.util.LeonardoUtil
import tech.harmonysoft.oss.leonardo.model.util.LeonardoUtil.LONG_COMPARATOR
import tech.harmonysoft.oss.leonardo.model.util.RangesList
import java.util.concurrent.Executor
import kotlin.math.max
import kotlin.math.min

/**
 * @param bufferPagesCount  number of chart data pages to keep in memory. E.g. if `1` is returned,
 *                          then the chart would keep one page before the current interval and one page
 *                          after the current interval
 * @param workersPool       thread pool to use for data loading
 */
class ChartModelImpl(private val bufferPagesCount: Int = 3, workersPool: Executor) : ChartModel {

    private val listeners = mutableListOf<ListenerRecord>()
    private val autoLoader = ChartDataAutoLoader(workersPool, this)

    private val points = mutableMapOf<ChartDataSource, DataTree<Long, DataPoint>>()
    private val loadedRanges = mutableMapOf<ChartDataSource, RangesList>()
    private val activeRanges = mutableMapOf<Any, Range>()
    private val disabledDataSources = mutableSetOf<ChartDataSource>()
    private val minimums = mutableMapOf<ChartDataSource, Long>()
    private val maximums = mutableMapOf<ChartDataSource, Long>()

    private var compoundActiveRange = Range.EMPTY_RANGE
    private var _bufferRange = Range.EMPTY_RANGE
    private var _hasSelection = false
    private var _selectedX = 0L

    init {
        addListener(autoLoader)
    }

    override val bufferRange get() = _bufferRange

    override val minX: Long?
        get() {
            var result: Long? = null
            for (dataSource in points.keys) {
                minimums[dataSource]?.let {
                    val r = result
                    if (r == null || r > it) {
                        result = it
                    }
                } ?: return null
            }
            return result
        }

    override val maxX: Long?
        get() {
            var result: Long? = null
            for (dataSource in points.keys) {
                maximums[dataSource]?.let {
                    val r = result
                    if (r == null || r < it) {
                        result = it
                    }
                } ?: return null
            }
            return result
        }

    override val hasSelection get() = _hasSelection
    override var selectedX: Long
        get() {
            check(_hasSelection) { "Detected a call for a selection when there is no selection" }
            return _selectedX
        }
        set(value) {
            if (_hasSelection && _selectedX == value) {
                return
            }
            _hasSelection = true
            _selectedX = value
            notifyListeners(ChartModelListener::onSelectionChange)
        }

    override fun resetSelection() {
        val changed = _hasSelection
        _hasSelection = false
        if (changed) {
            notifyListeners(ChartModelListener::onSelectionChange)
        }
    }

    override fun getActiveRange(anchor: Any): Range {
        return activeRanges[anchor] ?: Range.EMPTY_RANGE
    }

    override fun setActiveRange(range: Range, anchor: Any) {
        val rangeToUse = range.mayBeCut(minX, maxX)
        val previousRange = activeRanges.put(anchor, rangeToUse)
        if (previousRange == rangeToUse) {
            return
        }

        val newCompoundRange = if (compoundActiveRange.empty) {
            rangeToUse
        } else {
            var min = Long.MAX_VALUE
            var max = Long.MIN_VALUE
            for (value in activeRanges.values) {
                min = min(min, value.start)
                max = max(max, value.end)
            }
            Range(min, max)
        }

        if (compoundActiveRange != newCompoundRange) {
            compoundActiveRange = newCompoundRange.mayBeCut(minX, maxX)
            refreshBufferRange()

            for (rangesList in loadedRanges.values) {
                rangesList.keepOnly(bufferRange)
            }
            for (points in points.values) {
                points.removeLowerThen(bufferRange.start)
                points.removeGreaterThen(bufferRange.end)
            }
        }

        notifyListeners {
            it.onRangeChanged(anchor)
        }
    }

    private fun refreshBufferRange() {
        _bufferRange = Range(
            compoundActiveRange.start - bufferPagesCount.toLong() * compoundActiveRange.size,
            compoundActiveRange.end + bufferPagesCount.toLong() * compoundActiveRange.size
        )
    }

    override fun isActive(dataSource: ChartDataSource): Boolean {
        return points.containsKey(dataSource) && !disabledDataSources.contains(dataSource)
    }

    override val registeredDataSources get() = points.keys

    override fun addDataSource(dataSource: ChartDataSource) {
        require(!points.containsKey(dataSource)) {
            "Data source '$dataSource' is already registered (all registered data " +
            "sources: ${points.keys})"
        }
        points[dataSource] = DataTreeImpl(LONG_COMPARATOR)
        loadedRanges[dataSource] = RangesList()
        minimums.remove(dataSource)
        maximums.remove(dataSource)
        notifyListeners {
            it.onDataSourceAdded(dataSource)
        }
    }

    override fun removeDataSource(dataSource: ChartDataSource) {
        require(points.containsKey(dataSource)) {
            "Data source '$dataSource' is not registered. Registered: ${points.keys}"
        }
        points.remove(dataSource)
        loadedRanges.remove(dataSource)
        activeRanges.remove(dataSource)
        disabledDataSources.remove(dataSource)
        notifyListeners {
            it.onDataSourceRemoved(dataSource)
        }
    }

    override fun disableDataSource(dataSource: ChartDataSource) {
        require(points.containsKey(dataSource)) {
            "Data source '$dataSource' is not registered. Registered: ${points.keys}"
        }
        val changed = disabledDataSources.add(dataSource)
        if (changed) {
            notifyListeners {
                it.onDataSourceDisabled(dataSource)
            }
        }
    }

    override fun enableDataSource(dataSource: ChartDataSource) {
        require(points.containsKey(dataSource)) {
            "Data source '$dataSource' is not registered. Registered: ${points.keys}"
        }
        val changed = disabledDataSources.remove(dataSource)
        if (changed) {
            notifyListeners {
                it.onDataSourceEnabled(dataSource)
            }
        }
    }

    override fun getThisOrPrevious(dataSource: ChartDataSource, x: Long): DataPoint? {
        val dataSourcePoints = points[dataSource] ?: return null
        return dataSourcePoints.get(x) ?: dataSourcePoints.getPreviousValue(x)
    }

    override fun getThisOrNext(dataSource: ChartDataSource, x: Long): DataPoint? {
        val dataSourcePoints = points[dataSource] ?: return null
        return dataSourcePoints.get(x) ?: dataSourcePoints.getNextValue(x)
    }

    override fun getNext(dataSource: ChartDataSource, x: Long): DataPoint? {
        val dataSourcePoints = points[dataSource] ?: return null
        return dataSourcePoints.getNextValue(x)
    }

    override fun isLoadingInProgress(dataSource: ChartDataSource): Boolean {
        for (range in activeRanges.values) {
            if (autoLoader.isLoadingInProgress(dataSource, range.start, range.end)) {
                return true
            }
        }
        return false
    }

    override fun forRangePoints(dataSource: ChartDataSource,
                                start: Long,
                                end: Long,
                                includePrevious: Boolean,
                                includeNext: Boolean,
                                action: (DataPoint) -> Boolean) {
        val dataSourcePoints = points[dataSource] ?: return
        LeonardoUtil.forPoints(points = dataSourcePoints,
                               start = start,
                               end = end,
                               includePrevious = includePrevious,
                               includeNext = includeNext,
                               action = action)
    }

    override fun getLoadedRanges(dataSource: ChartDataSource): RangesList {
        return loadedRanges[dataSource]
               ?: throw IllegalArgumentException("No range info is found for data source $dataSource")
    }

    override fun onPointsLoaded(dataSource: ChartDataSource, range: Range, points: Iterable<DataPoint>) {
        val dataSourcePoints = this.points[dataSource] ?: return
        val loadedRanges = loadedRanges[dataSource] ?: return
        loadedRanges.add(range)
        val anchorsWithChangedActiveRange = mutableSetOf<Any>()
        for (point in points) {
            if (_bufferRange.contains(point.x)) {
                dataSourcePoints.put(point.x, point)
                for ((anchor, activeRange) in activeRanges) {
                    if (activeRange.contains(point.x)) {
                        anchorsWithChangedActiveRange.add(anchor)
                    }
                }
            }
        }

        if (anchorsWithChangedActiveRange.isNotEmpty()) {
            for (anchor in anchorsWithChangedActiveRange) {
                notifyListeners {
                    it.onActiveDataPointsLoaded(anchor)
                }
            }
        }
    }

    fun onPointsLoadingIterationEnd(dataSource: ChartDataSource) {
        notifyListeners {
            it.onPointsLoadingIterationEnd(dataSource)
        }
    }

    override fun addListener(listener: ChartModelListener) {
        listeners.add(ListenerRecord(listener))
    }

    override fun removeListener(listener: ChartModelListener) {
        listeners.removeAll {
            it.listener == listener
        }
    }

    private fun notifyListeners(action: (ChartModelListener) -> Unit) {
        for (listener in listeners) {
            if (!listener.notificationInProgress) {
                listener.notificationInProgress = true
                try {
                    action(listener.listener)
                } finally {
                    listener.notificationInProgress = false
                }
            }
        }
    }

    override fun getMin(dataSource: ChartDataSource): Long? {
        return minimums[dataSource]
    }

    fun setMin(dataSource: ChartDataSource, min: Long) {
        val previousMinimum = minX
        minimums[dataSource] = min
        val currentMinimum = minX
        if (currentMinimum != null && currentMinimum != previousMinimum) {
            notifyListeners {
                it.onMinimum(currentMinimum)
            }
            mayBeCutActiveRangesOnMinMaxChange()
        }
    }

    override fun getMax(dataSource: ChartDataSource): Long? {
        return maximums[dataSource]
    }

    fun setMax(dataSource: ChartDataSource, max: Long) {
        val previousMaximum = maxX
        maximums[dataSource] = max
        val currentMaximum = maxX
        if (currentMaximum != null && currentMaximum != previousMaximum) {
            notifyListeners {
                it.onMaximum(currentMaximum)
            }
            mayBeCutActiveRangesOnMinMaxChange()
        }
    }

    private fun mayBeCutActiveRangesOnMinMaxChange() {
        for ((anchor, range) in activeRanges) {
            val newRange = range.mayBeCut(minX, maxX)
            if (newRange != range) {
                setActiveRange(newRange, anchor)
                mayBeCutActiveRangesOnMinMaxChange()
                return
            }
        }
    }

    private data class ListenerRecord(val listener: ChartModelListener, var notificationInProgress: Boolean = false)
}