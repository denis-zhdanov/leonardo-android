package tech.harmonysoft.oss.leonardo.model.runtime.impl

import tech.harmonysoft.oss.leonardo.model.*
import tech.harmonysoft.oss.leonardo.model.data.ChartDataSource
import tech.harmonysoft.oss.leonardo.model.runtime.ChartModel
import tech.harmonysoft.oss.leonardo.model.runtime.ChartModelListener
import tech.harmonysoft.oss.leonardo.model.util.RangesList
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * @param bufferPagesCount  number of chart data pages to keep in memory. E.g. if `1` is returned,
 *                          then the chart would keep one page before the current interval and one page
 *                          after the current interval
 */
class ChartModelImpl(private val bufferPagesCount: Int = 3) : ChartModel {

    private val listeners = mutableListOf<ListenerRecord>()
    private val autoLoader = ChartDataAutoLoader(this)

    private val points = mutableMapOf<ChartDataSource, NavigableSet<WithComparableLongProperty>>()
    private val loadedRanges = mutableMapOf<ChartDataSource, RangesList>()
    private val activeRanges = mutableMapOf<Any, Range>()
    private val disabledDataSources = mutableSetOf<ChartDataSource>()
    private val bottomDataAnchor = DataPointAnchor()
    private val topDataAnchor = DataPointAnchor()

    private var compoundActiveRange = Range.EMPTY_RANGE
    private var _bufferRange = Range.EMPTY_RANGE
    private var _hasSelection = false
    private var _selectedX = 0L

    init {
        addListener(autoLoader)
    }

    override val hasSelection get() = _hasSelection
    override var selectedX: Long
        get() {
            if (!_hasSelection) {
                throw IllegalStateException("Detected a call for a selection when there is no selection")
            }
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

    override val bufferRange get() = _bufferRange

    override fun setActiveRange(range: Range, anchor: Any) {
        activeRanges[anchor] = range
        val newCompoundRange = if (compoundActiveRange.empty) {
            range
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
            compoundActiveRange = newCompoundRange
            refreshBufferRange()

            for (rangesList in loadedRanges.values) {
                rangesList.keepOnly(bufferRange)
            }
            for (points in points.values) {
                points.tailSet(DataPoint(bufferRange.end, 0), false).clear()
                points.headSet(DataPoint(bufferRange.start, 0), false).clear()
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
        if (points.containsKey(dataSource)) {
            throw IllegalArgumentException(
                "Data source '$dataSource' is already registered (all registered "
                + "data sources: ${points.keys})"
            )
        }
        points[dataSource] = TreeSet(WithComparableLongProperty.COMPARATOR)
        loadedRanges[dataSource] = RangesList()
        notifyListeners {
            it.onDataSourceAdded(dataSource)
        }
    }

    override fun removeDataSource(dataSource: ChartDataSource) {
        if (!points.containsKey(dataSource)) {
            throw IllegalArgumentException("Data source '$dataSource' is not registered. Registered: ${points.keys}")
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
        if (!points.containsKey(dataSource)) {
            throw IllegalArgumentException("Data source '$dataSource' is not registered. Registered: ${points.keys}")
        }
        val changed = disabledDataSources.add(dataSource)
        if (changed) {
            notifyListeners {
                it.onDataSourceDisabled(dataSource)
            }
        }
    }

    override fun enableDataSource(dataSource: ChartDataSource) {
        if (!points.containsKey(dataSource)) {
            throw IllegalArgumentException("Data source '$dataSource' is not registered. Registered: ${points.keys}")
        }
        val changed = disabledDataSources.remove(dataSource)
        if (changed) {
            notifyListeners {
                it.onDataSourceEnabled(dataSource)
            }
        }
    }

    override fun arePointsForActiveRangeLoaded(dataSource: ChartDataSource, anchor: Any): Boolean {
        val range = getActiveRange(anchor)
        if (range.empty) {
            return true
        }
        val rangesList = loadedRanges[dataSource]
        return rangesList != null && rangesList.contains(range)
    }

    @Suppress("UNCHECKED_CAST")
    override fun getAllPoints(dataSource: ChartDataSource): NavigableSet<DataPoint> {
        return points[dataSource] as? NavigableSet<DataPoint> ?: EMPTY_DATA_POINTS
    }

    @Suppress("UNCHECKED_CAST")
    override fun getPoints(dataSource: ChartDataSource, start: Long, end: Long): NavigableSet<DataPoint> {
        if (start > end) {
            return EMPTY_DATA_POINTS
        }

        val points = points[dataSource] ?: return EMPTY_DATA_POINTS
        topDataAnchor.value = end
        val filteredFromTop = points.headSet(topDataAnchor, true)

        bottomDataAnchor.value = start
        return filteredFromTop.tailSet(bottomDataAnchor, true) as NavigableSet<DataPoint>
    }

    override fun getCurrentRangePoints(dataSource: ChartDataSource, anchor: Any): NavigableSet<DataPoint> {
        val range = getActiveRange(anchor)
        return getPoints(dataSource, range.start, range.end)
    }

    override fun getPreviousPointForActiveRange(dataSource: ChartDataSource, anchor: Any): DataPoint? {
        val range = getActiveRange(anchor)
        if (range.empty) {
            return null
        }
        val points = points[dataSource] ?: return null
        return previous(range.start, points) as? DataPoint
    }

    override fun getNextPointForActiveRange(dataSource: ChartDataSource, anchor: Any): DataPoint? {
        val range = getActiveRange(anchor)
        if (range.empty) {
            return null
        }
        val points = points[dataSource] ?: return null
        return next(range.end, points) as? DataPoint
    }

    override fun getLoadedRanges(dataSource: ChartDataSource): RangesList {
        return loadedRanges[dataSource]
               ?: throw IllegalArgumentException("No range info is found for data source $dataSource")
    }

    override fun onPointsLoaded(dataSource: ChartDataSource, range: Range, points: Collection<DataPoint>) {
        val dataSourcePoints = this.points[dataSource]
        val rangesList = loadedRanges[dataSource]
        if (dataSourcePoints == null || rangesList == null) {
            return
        }

        val anchorWithChangedActiveRange = mutableSetOf<Any>()
        for (point in points) {
            if (!bufferRange.contains(point.x)) {
                continue
            }

            dataSourcePoints += point
            for ((key, value) in activeRanges) {
                if (value.contains(point.x)) {
                    anchorWithChangedActiveRange.add(key)
                }
            }
        }

        rangesList.add(range)
        rangesList.keepOnly(bufferRange)

        for (anchor in anchorWithChangedActiveRange) {
            notifyListeners {
                it.onActiveDataPointsLoaded(anchor)
            }
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
        listeners.forEach {
            if (!it.notificationInProgress) {
                it.notificationInProgress = true
                try {
                    action(it.listener)
                } finally {
                    it.notificationInProgress = false
                }
            }
        }
    }

    companion object {
        private val EMPTY_DATA_POINTS = TreeSet<DataPoint>()
    }

    private data class ListenerRecord(val listener: ChartModelListener, var notificationInProgress: Boolean = false)
}