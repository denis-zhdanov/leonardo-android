package tech.harmonysoft.oss.leonardo.model.config.chart

import tech.harmonysoft.oss.leonardo.model.config.axis.AxisConfig

data class ChartConfig(
    val xAxisConfig: AxisConfig,
    val yAxisConfig: AxisConfig,
    val backgroundColor: Int,
    val gridColor: Int,
    val gridLineWidthInPixels: Int,
    val plotLineWidthInPixels: Int,
    val selectionSignRadiusInPixels: Int,
    val legendTextTitleColor: Int,
    val legendBackgroundColor: Int,
    val selectionAllowed: Boolean,
    val drawSelection: Boolean,
    val drawBackground: Boolean,
    val animationEnabled: Boolean,
    val animationDurationMillis: Int,
    val noChartsDrawableId: Int?
)