package tech.harmonysoft.oss.leonardo.view.chart

import android.graphics.Paint
import android.graphics.Rect
import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.config.axis.AxisConfig
import kotlin.math.round

internal class ChartAxisData(
    paint: Paint,
    config: AxisConfig,

    val animator: AxisAnimator
) {
    val labelWidth: Int
    val labelHeight: Int
    val labelPadding: Int
    val valueStrategy = config.labelTextStrategy
    val drawAxis = config.drawAxis

    var range: Range = Range.EMPTY_RANGE
    var axisStep = 1L
    var unitSize: Float = 0f
    var visualShift = 0f

    var availableSize = 0

    init {
        val bounds = Rect()
        paint.getTextBounds("W", 0, 1, bounds)
        labelWidth = bounds.width()
        labelHeight = bounds.height()
        labelPadding = labelHeight / 2
    }

    fun dataValueToVisualValue(dataValue: Long): Float {
        return visualShift + (dataValue - range.start).toFloat() * availableSize.toFloat() / range.size.toFloat()
    }

    fun visualValueToDataValue(visualValue: Float): Long {
        return range.start + round((visualValue - visualShift) / (availableSize.toFloat() * range.size)).toLong()
    }
}