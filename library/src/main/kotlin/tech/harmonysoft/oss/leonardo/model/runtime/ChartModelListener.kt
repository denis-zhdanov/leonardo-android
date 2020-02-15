package tech.harmonysoft.oss.leonardo.model.runtime

import tech.harmonysoft.oss.leonardo.model.data.ChartDataSource

interface ChartModelListener {

    fun onRangeChanged(anchor: Any)

    fun onDataSourceEnabled(dataSource: ChartDataSource)

    fun onDataSourceDisabled(dataSource: ChartDataSource)

    fun onDataSourceAdded(dataSource: ChartDataSource)

    fun onDataSourceRemoved(dataSource: ChartDataSource)

    fun onActiveDataPointsLoaded(anchor: Any)

    fun onPointsLoadingIterationEnd(dataSource: ChartDataSource)

    fun onSelectionChange()

    fun onMinimum(minX: Long)

    fun onMaximum(maxX: Long)
}