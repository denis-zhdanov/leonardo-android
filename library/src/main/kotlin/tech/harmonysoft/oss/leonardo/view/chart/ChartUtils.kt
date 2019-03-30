package tech.harmonysoft.oss.leonardo.view.chart

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.view.View
import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.config.chart.ChartConfig
import tech.harmonysoft.oss.leonardo.model.runtime.ChartModel
import tech.harmonysoft.oss.leonardo.model.text.ValueRepresentationStrategy
import tech.harmonysoft.oss.leonardo.view.util.AxisStepChooser
import tech.harmonysoft.oss.leonardo.view.util.TextWidthMeasurer
import tech.harmonysoft.oss.leonardo.view.util.X_AXIS_LABEL_GAP_STRATEGY

/**
 * @author Denis Zhdanov
 * @since 29/3/19
 */
internal class ChartDrawSetup(private val config: ChartConfig) {

    private val paint = Paint()

    val backgroundPaint: Paint
        get() {
            return paint.apply {
                color = config.backgroundColor
                alpha = 255
            }
        }

    val gridPaint: Paint
        get() {
            return paint.apply {
                color = config.gridColor
                strokeWidth = config.gridLineWidthInPixels.toFloat()
                alpha = 255
            }
        }

    val xLabelPaint: Paint
        get() {
            return paint.apply {
                color = config.xAxisConfig.labelColor
                paint.textSize = config.xAxisConfig.labelFontSizeInPixels.toFloat()
                paint.typeface = Typeface.DEFAULT
                alpha = 255
            }
        }

    val yLabelPaint: Paint
        get() {
            return paint.apply {
                color = config.yAxisConfig.labelColor
                paint.textSize = config.yAxisConfig.labelFontSizeInPixels.toFloat()
                paint.typeface = Typeface.DEFAULT
                alpha = 255
            }
        }
}

internal class ChartDrawData(
    drawSetup: ChartDrawSetup,
    view: View,
    xLabelStrategy: ValueRepresentationStrategy,
    yLabelStrategy: ValueRepresentationStrategy,

    private val model: ChartModel,
    private val dataAnchor: Any,

    private val axisStepChooser: AxisStepChooser = AxisStepChooser.INSTANCE,
    private val animationEnabled: Boolean
) {

    val xAxis = AxisData(drawSetup.xLabelPaint, xLabelStrategy, AxisRescaleAnimator(view))
    val yAxis = AxisData(drawSetup.yLabelPaint, yLabelStrategy, AxisRescaleAnimator(view))

    private val xWidthMeasurer = TextWidthMeasurer(drawSetup.xLabelPaint)

    fun refresh(currentWidth: Int, currentHeight: Int) {
        val activeRange = model.getActiveRange(dataAnchor)
        refreshAxis(activeRange, currentWidth, xAxis)
    }

    private fun refreshAxis(currentValuesRange: Range, availableSize: Int, data: AxisData) {
        val rescale = (currentValuesRange.size != data.range.size) || (availableSize != data.availableSize)
        val initialRange = data.range
        val initialStep = data.axisStep
        val initialSize = data.availableSize
        data.range = currentValuesRange
        if (!rescale) {
            return
        }

        data.availableSize = availableSize
        data.visualShift = 0f

        data.axisStep = axisStepChooser.choose(data.valueStrategy,
                                               X_AXIS_LABEL_GAP_STRATEGY,
                                               currentValuesRange,
                                               availableSize,
                                               xWidthMeasurer)
        data.unitSize = availableSize.toFloat() / currentValuesRange.size.toFloat()

        if (animationEnabled && initialSize > 0) {
            data.animator.animate(initialRange, data.range, initialStep, availableSize)
        }
    }
}

internal class AxisData(
    paint: Paint,
    val valueStrategy: ValueRepresentationStrategy,
    val animator: AxisRescaleAnimator
) {
    var range: Range = Range.EMPTY_RANGE
    var axisStep = 1L
    var unitSize: Float = 0f
    var visualShift = 0f

    val labelWidth: Int
    val labelHeight: Int
    val verticalLabelPadding: Int

    var availableSize = 0

    init {
        val bounds = Rect()
        paint.getTextBounds("W", 0, 1, bounds)
        labelWidth = bounds.width()
        labelHeight = bounds.height()
        verticalLabelPadding = labelHeight / 2
    }
}