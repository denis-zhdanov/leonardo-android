package tech.harmonysoft.oss.leonardo.model.runtime

import tech.harmonysoft.oss.leonardo.model.data.ChartDataSource

abstract class ChartModelListenerAdapter : ChartModelListener {
    override fun onRangeChanged(anchor: Any) {
    }

    override fun onDataSourceEnabled(dataSource: ChartDataSource) {
    }

    override fun onDataSourceDisabled(dataSource: ChartDataSource) {
    }

    override fun onDataSourceAdded(dataSource: ChartDataSource) {
    }

    override fun onDataSourceRemoved(dataSource: ChartDataSource) {
    }

    override fun onActiveDataPointsLoaded(anchor: Any) {
    }

    override fun onPointsLoadingIterationEnd(dataSource: ChartDataSource) {
    }

    override fun onSelectionChange() {
    }
}