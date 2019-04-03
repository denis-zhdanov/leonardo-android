package tech.harmonysoft.oss.leonardo.model.runtime.impl

import tech.harmonysoft.oss.leonardo.model.*
import tech.harmonysoft.oss.leonardo.model.data.ChartDataSource
import tech.harmonysoft.oss.leonardo.model.runtime.ChartModel
import tech.harmonysoft.oss.leonardo.model.runtime.ChartModelListener
import tech.harmonysoft.oss.leonardo.model.util.RangesList
import java.util.*

/**
 * @param bufferPagesCount  number of chart data pages to keep in memory. E.g. if `1` is returned,
 *                          then the chart would keep one page before the current interval and one page
 *                          after the current interval
 */
class ChartModelImpl(private val bufferPagesCount: Int) : ChartModel {

    private val listeners = mutableListOf<ChartModelListener>()

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
            listeners.forEach(ChartModelListener::onSelectionChange)
        }

    override fun resetSelection() {
        val changed = _hasSelection
        _hasSelection = false
        if (changed) {
            listeners.forEach(ChartModelListener::onSelectionChange)
        }
    }

    override fun getActiveRange(anchor: Any): Range {
        return activeRanges[anchor] ?: Range.EMPTY_RANGE
    }

    override val bufferRange get() = _bufferRange

    override fun setActiveRange(range: Range, anchor: Any) {
        activeRanges[anchor] = range
        val newCompoundRange: Range
        if (compoundActiveRange.empty) {
            newCompoundRange = range
        } else {
            var min = Long.MAX_VALUE
            var max = Long.MIN_VALUE
            for (value in activeRanges.values) {
                min = Math.min(min, value.start)
                max = Math.max(max, value.end)
            }
            newCompoundRange = Range(min, max)
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

        listeners.forEach {
            it.onRangeChanged(anchor)
        }
    }

    private fun refreshBufferRange() {
        _bufferRange = Range(
            compoundActiveRange.start - bufferPagesCount * (compoundActiveRange.size + 1),
            compoundActiveRange.end + bufferPagesCount * (compoundActiveRange.size + 1)
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
        listeners.forEach {
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
        listeners.forEach {
            it.onDataSourceRemoved(dataSource)
        }
    }

    override fun disableDataSource(dataSource: ChartDataSource) {
        if (!points.containsKey(dataSource)) {
            throw IllegalArgumentException("Data source '$dataSource' is not registered. Registered: ${points.keys}")
        }
        val changed = disabledDataSources.add(dataSource)
        if (changed) {
            listeners.forEach {
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
            listeners.forEach {
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
    override fun getCurrentRangePoints(dataSource: ChartDataSource, anchor: Any): NavigableSet<DataPoint> {
        val range = getActiveRange(anchor)
        if (range.empty) {
            return EMPTY_DATA_POINTS
        }
        val points = points[dataSource] ?: return EMPTY_DATA_POINTS

        topDataAnchor.value = range.end
        val filteredFromTop = points.headSet(topDataAnchor, true)

        bottomDataAnchor.value = range.start
        return filteredFromTop.tailSet(bottomDataAnchor, true) as NavigableSet<DataPoint>
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
            listeners.forEach {
                it.onActiveDataPointsLoaded(anchor)
            }
        }
    }

    override fun addListener(listener: ChartModelListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: ChartModelListener) {
        listeners.remove(listener)
    }

    companion object {
        private val EMPTY_DATA_POINTS = TreeSet<DataPoint>()
    }
}