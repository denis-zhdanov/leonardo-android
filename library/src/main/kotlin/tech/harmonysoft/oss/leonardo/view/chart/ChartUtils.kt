package tech.harmonysoft.oss.leonardo.view.chart

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
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
internal class ChartDrawContext(private val config: ChartConfig) {

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
}

internal class ChartDimensions(
    drawContext: ChartDrawContext,

    private val model: ChartModel,
    private val dataAnchor: Any,
    private val axisStepChooser: AxisStepChooser = AxisStepChooser.INSTANCE,
    private val xLabelStrategy: ValueRepresentationStrategy,
    private val animationEnabled: Boolean,
    private val xRescaleAnimator: AxisRescaleAnimator
) {

    private val xWidthMeasurer = TextWidthMeasurer(drawContext.xLabelPaint)
    private var chartWidth = 0

    val xAxisLabelHeight = run {
        val bounds = Rect()
        drawContext.xLabelPaint.getTextBounds("J", 0, 1, bounds)
        bounds.height()
    }
    val xAxisLabelVerticalPadding = xAxisLabelHeight / 2
    var xRange: Range = Range.EMPTY_RANGE
    var xAxisStep = 1L
    var xUnitWidth: Float = 0f
    var xVisualShift = 0f

    fun refresh(currentWidth: Int, currentHeight: Int) {
        refreshX(currentWidth)
    }

    private fun refreshX(currentWidth: Int) {
        val activeRange = model.getActiveRange(dataAnchor)
        val rescale = (activeRange.size != xRange.size) || (chartWidth != currentWidth)
        val initialRange = xRange
        val initialStep = xAxisStep
        val initialWidth = chartWidth
        xRange = activeRange
        if (!rescale) {
            return
        }

        chartWidth = currentWidth
        xVisualShift = 0f

        xAxisStep = axisStepChooser.choose(xLabelStrategy,
                                           X_AXIS_LABEL_GAP_STRATEGY,
                                            activeRange,
                                           chartWidth,
                                           xWidthMeasurer)
        xUnitWidth = chartWidth.toFloat() / activeRange.size.toFloat()

        if (animationEnabled && initialWidth > 0) {
            xRescaleAnimator.animate(initialRange, xRange, initialStep, currentWidth)
        }
    }
}