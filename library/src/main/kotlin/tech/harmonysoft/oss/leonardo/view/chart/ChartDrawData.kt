package tech.harmonysoft.oss.leonardo.view.chart

import tech.harmonysoft.oss.leonardo.model.DataPoint
import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.config.chart.ChartConfig
import tech.harmonysoft.oss.leonardo.model.data.ChartDataSource
import tech.harmonysoft.oss.leonardo.model.runtime.ChartModel
import tech.harmonysoft.oss.leonardo.model.runtime.ChartModelListener
import tech.harmonysoft.oss.leonardo.model.runtime.DataMapper
import tech.harmonysoft.oss.leonardo.model.text.TextWrapper
import tech.harmonysoft.oss.leonardo.view.util.*
import kotlin.math.max

internal class ChartDrawData(
    palette: ChartPalette,
    config: ChartConfig,

    private val axisStepChooser: AxisStepChooser = AxisStepChooser.INSTANCE,

    private val view: ChartView,
    private val model: ChartModel
) : DataMapper {

    val xAxis = ChartAxisData(palette.xLabelPaint,
                              config.xAxisConfig,
                              AxisAnimator(view, config.animationDurationMillis))
    val yAxis = ChartAxisData(palette.yLabelPaint,
                              config.yAxisConfig,
                              AxisAnimator(view, config.animationDurationMillis))
    val legendLabelHeight = TextHeightMeasurer(palette.legendValuePaint).measureVisualSpace("W")

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
    private val animationEnabled = config.animationEnabled
    private val xWidthMeasurer = TextWidthMeasurer { palette.xLabelPaint }
    private val yWidthMeasurer = TextWidthMeasurer { palette.yLabelPaint }
    private val legendWidthMeasurer = TextWidthMeasurer { palette.legendValuePaint }
    private val plotAnimator = PlotAnimator(view, config.animationDurationMillis)
    private val yRangeUpdater: (DataPoint) -> Boolean = this::updateYDataRange

    private var maxYLabelWidth = 0

    private var forceRefresh = false
    private var lastWidth = 0
    private var lastHeight = 0

    private var minY = Long.MAX_VALUE
    private var maxY = Long.MIN_VALUE

    init {
        model.addListener(object : ChartModelListener {
            override fun onRangeChanged(anchor: Any) {
            }

            override fun onDataSourceEnabled(dataSource: ChartDataSource) {
                forceRefresh = true
            }

            override fun onDataSourceDisabled(dataSource: ChartDataSource) {
                forceRefresh = true
            }

            override fun onDataSourceAdded(dataSource: ChartDataSource) {
                forceRefresh = true
            }

            override fun onDataSourceRemoved(dataSource: ChartDataSource) {
                forceRefresh = true
            }

            override fun onActiveDataPointsLoaded(anchor: Any) {
                if (anchor == view.dataAnchor) {
                    forceRefresh = true
                }
            }

            override fun onPointsLoadingIterationEnd(dataSource: ChartDataSource) {
            }

            override fun onSelectionChange() {
            }

            override fun onMinimum(minX: Long) {
            }

            override fun onMaximum(maxX: Long) {
            }
        })
    }

    fun refresh() {
        val currentWidth = view.width
        val currentHeight = view.height

        val activeXDataRange = model.getActiveRange(view.dataAnchor)
        if (!forceRefresh && lastWidth == currentWidth && lastHeight == currentHeight
            && activeXDataRange == xAxis.range
        ) {
            return
        }

        lastWidth = currentWidth
        lastHeight = currentHeight

        val activeYDataRange = mayBeExpandYRange(getYDataRange())
        refreshAxis(activeYDataRange, chartBottom, yAxis, true)

        refreshAxis(activeXDataRange, currentWidth - chartLeft, xAxis, false)

        forceRefresh = false
    }

    private fun refreshAxis(currentValuesRange: Range, availableSize: Int, data: ChartAxisData, yAxis: Boolean) {
        val rescale = (currentValuesRange.size != data.range.size) || (availableSize != data.availableSize)
        val initialRange = data.range
        data.range = currentValuesRange
        if (!rescale) {
            return
        }

        val initialStep = data.axisStep
        val initialSize = data.availableSize

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

    private fun updateYDataRange(point: DataPoint): Boolean {
        if (point.y > maxY) {
            maxY = point.y
        }
        if (point.y < minY) {
            minY = point.y
        }
        return true
    }

    private fun getYDataRange(): Range {
        minY = Long.MAX_VALUE
        maxY = Long.MIN_VALUE
        val (start, end) = model.getActiveRange(view.dataAnchor)
        model.registeredDataSources.forEach { dataSource ->
            if (model.isActive(dataSource)) {
                model.forRangePoints(dataSource = dataSource, start = start, end = end, action = yRangeUpdater)
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
            chartLeft + xAxis.visualShift + xAxis.animator.getVisualValue(dataX)
        } else {
            chartLeft + xAxis.dataValueToVisualValue(dataX)
        }
    }

    override fun visualXToDataX(visualX: Float): Long {
        refresh()
        return if (xAxis.animator.inProgress) {
            xAxis.animator.getDataValue(visualX - chartLeft)
        } else {
            xAxis.visualValueToDataValue(visualX - chartLeft)
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
}

