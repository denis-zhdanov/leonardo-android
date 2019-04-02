package tech.harmonysoft.oss.leonardo.view.chart

import android.os.Handler
import android.os.Looper
import android.view.View
import tech.harmonysoft.oss.leonardo.model.DataPoint
import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.VisualPoint
import tech.harmonysoft.oss.leonardo.model.config.chart.ChartConfig
import tech.harmonysoft.oss.leonardo.model.data.ChartDataSource
import tech.harmonysoft.oss.leonardo.model.runtime.ChartModel
import tech.harmonysoft.oss.leonardo.model.runtime.DataMapper
import tech.harmonysoft.oss.leonardo.model.util.LeonardoUtil
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
    var maxYLabelWidth = 0

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

    private val drawXLabels = config.xAxisConfig.drawAxis || config.xAxisConfig.drawLabels
    private val animationDataSourceInfo = mutableMapOf<ChartDataSource, DataSourceAnimationContext>()
    private val handler = Handler(Looper.getMainLooper())
    private val redrawTask = {
        view.invalidate()
    }
    private val animationEnabled = config.animationEnabled
    private val xWidthMeasurer = TextWidthMeasurer(drawSetup.xLabelPaint)

    private var forceRefresh = false

    fun refresh() {
        if (tickDataSourceFadeAnimation()) {
            handler.postDelayed(redrawTask, LeonardoUtil.ANIMATION_TICK_FREQUENCY_MILLIS)
        }
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

    private fun tickDataSourceFadeAnimation(): Boolean {
        val toRemove = mutableListOf<ChartDataSource>()
        val now = System.currentTimeMillis()
        for ((key, context) in animationDataSourceInfo) {
            if (now >= context.startTimeMs + LeonardoUtil.ANIMATION_DURATION_MILLIS) {
                toRemove.add(key)
                break
            }

            val elapsedTimeMs = now - context.startTimeMs
            val totalAlphaDelta = context.finalAlpha - context.initialAlpha
            val currentAlphaDelta = elapsedTimeMs * totalAlphaDelta / LeonardoUtil.ANIMATION_DURATION_MILLIS
            context.currentAlpha = (context.initialAlpha + currentAlphaDelta).toInt()
            if ((totalAlphaDelta > 0 && context.currentAlpha >= context.finalAlpha)
                || (totalAlphaDelta < 0 && context.currentAlpha <= context.finalAlpha)
            ) {
                toRemove.add(key)
            }
        }

        if (toRemove.isNotEmpty()) {
            forceRefresh = true
            toRemove.forEach {
                animationDataSourceInfo.remove(it)
            }
        }

        return !animationDataSourceInfo.isEmpty()
    }


    fun fadeIn(dataSource: ChartDataSource) {
        animationDataSourceInfo[dataSource] = DataSourceAnimationContext(0, 255)
        forceRefresh = true
    }

    fun fadeOut(dataSource: ChartDataSource) {
        animationDataSourceInfo[dataSource] = DataSourceAnimationContext(255, 0)
        forceRefresh = true
    }

    fun isAnimationInProgress(dataSource: ChartDataSource): Boolean {
        return animationDataSourceInfo.containsKey(dataSource)
    }

    fun getCurrentAlpha(dataSource: ChartDataSource): Int {
        return animationDataSourceInfo[dataSource]?.currentAlpha ?: 255
    }

    private data class DataSourceAnimationContext(
        val initialAlpha: Int,
        val finalAlpha: Int,
        val startTimeMs: Long = System.currentTimeMillis()
    ) {
        var currentAlpha = initialAlpha
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
        return yAxis.availableSize -  if (yAxis.animator.inProgress) {
            yAxis.animator.getVisualValue(dataY)
        } else {
            yAxis.dataValueToVisualValue(dataY)
        }
    }

    override fun dataPointToVisualPoint(dataPoint: DataPoint): VisualPoint {
        return VisualPoint(dataXToVisualX(dataPoint.x), dataYToVisualY(dataPoint.y))
    }
}

