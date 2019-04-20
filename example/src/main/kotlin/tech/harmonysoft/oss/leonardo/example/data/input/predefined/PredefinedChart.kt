package tech.harmonysoft.oss.leonardo.example.data.input.predefined

import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.data.ChartDataSource

data class PredefinedChart(val name: String, val xRange: Range, val dataSources: Collection<ChartDataSource>)