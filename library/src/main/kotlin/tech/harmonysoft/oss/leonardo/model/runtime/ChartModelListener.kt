package tech.harmonysoft.oss.leonardo.model.runtime

import tech.harmonysoft.oss.leonardo.model.data.ChartDataSource

interface ChartModelListener {

    fun onRangeChanged(anchor: Any)

    fun onDataSourceEnabled(dataSource: ChartDataSource)

    fun onDataSourceDisabled(dataSource: ChartDataSource)

    fun onDataSourceAdded(dataSource: ChartDataSource)

    fun onDataSourceRemoved(dataSource: ChartDataSource)

    fun onActiveDataPointsLoaded(anchor: Any)

    fun onSelectionChange()
}