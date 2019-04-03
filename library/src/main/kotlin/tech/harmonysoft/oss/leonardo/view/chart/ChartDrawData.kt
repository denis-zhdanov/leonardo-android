package tech.harmonysoft.oss.leonardo.view.chart

import android.view.View
import tech.harmonysoft.oss.leonardo.model.DataPoint
import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.VisualPoint
import tech.harmonysoft.oss.leonardo.model.config.chart.ChartConfig
import tech.harmonysoft.oss.leonardo.model.data.ChartDataSource
import tech.harmonysoft.oss.leonardo.model.runtime.ChartModel
import tech.harmonysoft.oss.leonardo.model.runtime.DataMapper
import tech.harmonysoft.oss.leonardo.model.text.TextWrapper
import tech.harmonysoft.oss.leonardo.view.util.*
import kotlin.math.max
import kotlin.math.min

internal class ChartDrawData(
    drawSetup: ChartDrawSetup,
    config: ChartConfig,

    private val axisStepChooser: AxisStepChooser = AxisStepChooser.INSTANCE,

    private val view: View,
    private val model: ChartModel,
    private val dataAnchor: Any
) : DataMapper {

    val xAxis = ChartAxisData(drawSetup.xLabelPaint, config.xAxisConfig, AxisAnimator(view))
    val yAxis = ChartAxisData(drawSetup.yLabelPaint, config.yAxisConfig, AxisAnimator(view))
    val legendLabelHeight = TextHeightMeasurer(drawSetup.legendValuePaint).measureVisualSpace("W")

    val chartBottom: Int
        get() {
            return if (drawXLabels) {
                view.height - xAxis.labelHeight - xAxis.labelPadding
            } else {
                view.height
            }
        }

    val chartLeft: Int
        get() {
            return maxYLabelWidth + yAxis.labelPadding
        }

    var maxYLabelWidth = 0

    private val drawXLabels = config.xAxisConfig.drawAxis || config.xAxisConfig.drawLabels
    private val animationEnabled = config.animationEnabled
    private val xWidthMeasurer = TextWidthMeasurer { drawSetup.xLabelPaint }
    private val yWidthMeasurer = TextWidthMeasurer { drawSetup.yLabelPaint }
    private val legendWidthMeasurer = TextWidthMeasurer { drawSetup.legendValuePaint }
    private val plotAnimator = PlotAnimator(view)

    private var forceRefresh = false

    fun refresh() {
        maxYLabelWidth = 0

        val activeXDataRange = model.getActiveRange(dataAnchor)
        if (!forceRefresh && activeXDataRange == xAxis.range) {
            return
        }

        val activeYDataRange = mayBeExpandYRange(getYDataRange())
        refreshAxis(activeYDataRange, chartBottom, yAxis, true)

        refreshAxis(activeXDataRange, view.width - maxYLabelWidth - yAxis.labelPadding, xAxis, false)

        forceRefresh = false
    }

    private fun refreshAxis(currentValuesRange: Range, availableSize: Int, data: ChartAxisData, yAxis: Boolean) {
        val rescale = (currentValuesRange.size != data.range.size) || (availableSize != data.availableSize)
        val initialRange = data.range
        val initialStep = data.axisStep
        val initialSize = data.availableSize
        data.range = currentValuesRange
        if (!rescale) {
            return
        }

        if (yAxis) {
            maxYLabelWidth = 0
        }

        data.availableSize = availableSize
        data.visualShift = 0f

        val rangeForAxisStepCalculation = if (yAxis) {
            getYDataRange()
        } else {
            currentValuesRange
        }

        val gapStrategy = if (yAxis) {
            Y_AXIS_LABEL_GAP_STRATEGY
        } else {
            X_AXIS_LABEL_GAP_STRATEGY
        }
        data.axisStep = axisStepChooser.choose(data.valueStrategy,
                                               gapStrategy,
                                               rangeForAxisStepCalculation,
                                               availableSize,
                                               xWidthMeasurer)
        if (yAxis) {
            data.range = mayBeExpandYRange(rangeForAxisStepCalculation)
        }
        data.unitSize = availableSize.toFloat() / data.range.size.toFloat()

        if (animationEnabled && initialSize > 0) {
            data.animator.animate(initialRange, data.range, initialStep, availableSize)
        }
    }

    private fun getYDataRange(): Range {
        var minY = Long.MAX_VALUE
        var maxY = Long.MIN_VALUE
        model.registeredDataSources.forEach { dataSource ->
            if (model.isActive(dataSource)) {
                model.getCurrentRangePoints(dataSource, dataAnchor).forEach { dataPoint ->
                    minY = min(minY, dataPoint.y)
                    maxY = max(maxY, dataPoint.y)
                }
            }
        }
        return Range(minY, maxY)
    }

    /**
     * There is a possible case that current Y range is not divisible by step size, e.g. current range
     * is `[4; 24]` and current step is `10`. We want to expand the range to `[0; 30]` then
     * in order to draw Y axis labels through the same intervals.
     *
     * @param range Y range to process
     * @return Y range to use
     */
    private fun mayBeExpandYRange(range: Range): Range {
        return if (yAxis.axisStep <= 0 || !yAxis.drawAxis) {
            range
        } else {
            range.padBy(yAxis.axisStep)
        }
    }


    fun onYLabel(label: TextWrapper) {
        maxYLabelWidth = max(maxYLabelWidth, yWidthMeasurer.measureVisualSpace(label))
    }

    fun getYLabelWidth(label: String): Int {
        return yWidthMeasurer.measureVisualSpace(label)
    }

    fun getLegendValueWidth(value: TextWrapper): Int {
        return legendWidthMeasurer.measureVisualSpace(value)
    }

    fun fadeIn(dataSource: ChartDataSource) {
        plotAnimator.fadeIn(dataSource)
        forceRefresh = true
    }

    fun fadeOut(dataSource: ChartDataSource) {
        plotAnimator.fadeOut(dataSource)
        forceRefresh = true
    }

    fun isAnimationInProgress(dataSource: ChartDataSource): Boolean {
        return plotAnimator.isAnimationInProgress(dataSource)
    }

    fun getCurrentAlpha(dataSource: ChartDataSource): Int {
        return plotAnimator.getAlpha(dataSource)
    }

    override fun dataXToVisualX(dataX: Long): Float {
        refresh()
        return if (xAxis.animator.inProgress) {
            xAxis.visualShift + xAxis.animator.getVisualValue(dataX)
        } else {
            xAxis.dataValueToVisualValue(dataX)
        }
    }

    override fun visualXToDataX(visualX: Float): Long {
        refresh()
        return if (xAxis.animator.inProgress) {
            xAxis.animator.getDataValue(visualX - xAxis.visualShift)
        } else {
            xAxis.visualValueToDataValue(visualX)
        }
    }

    fun dataYToVisualY(dataY: Long): Float {
        refresh()
        return yAxis.availableSize - if (yAxis.animator.inProgress) {
            yAxis.animator.getVisualValue(dataY)
        } else {
            yAxis.dataValueToVisualValue(dataY)
        }
    }

    override fun dataPointToVisualPoint(dataPoint: DataPoint): VisualPoint {
        return VisualPoint(dataXToVisualX(dataPoint.x), dataYToVisualY(dataPoint.y))
    }
}

