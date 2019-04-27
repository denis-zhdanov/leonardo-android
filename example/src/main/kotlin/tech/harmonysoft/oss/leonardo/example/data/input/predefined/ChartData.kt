package tech.harmonysoft.oss.leonardo.example.data.input.predefined

import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.data.ChartDataSource
import tech.harmonysoft.oss.leonardo.model.text.ValueRepresentationStrategy

data class ChartData(
    val name: String,
    val xRange: Range,
    val dataSources: Collection<ChartDataSource>,
    val xLabelStrategy: ValueRepresentationStrategy
)