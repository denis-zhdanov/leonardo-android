package tech.harmonysoft.oss.leonardo.view.chart

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.view.GestureDetectorCompat
import tech.harmonysoft.oss.leonardo.model.DataPoint
import tech.harmonysoft.oss.leonardo.model.LineFormula
import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.VisualPoint
import tech.harmonysoft.oss.leonardo.model.config.chart.ChartConfig
import tech.harmonysoft.oss.leonardo.model.data.ChartDataSource
import tech.harmonysoft.oss.leonardo.model.runtime.ChartModel
import tech.harmonysoft.oss.leonardo.model.runtime.ChartModelListener
import tech.harmonysoft.oss.leonardo.model.util.LeonardoUtil
import tech.harmonysoft.oss.leonardo.view.util.IterationAwareValueAnimator
import tech.harmonysoft.oss.leonardo.view.util.RoundedRectangleDrawer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

class ChartView @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null,
    defaultStyle: Int = 0
) : View(context, attributes, defaultStyle) {

    val dataAnchor: Any get() = _dataAnchor

    /**
     * Keep own data sources list in order to work with them in lexicographically, e.g. when showing selection legend
     */
    private val dataSources = mutableListOf<ChartDataSource>()

    private val gestureDetector = GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            onScroll(distanceX, false)
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (e.pointerCount == 1) {
                onTap()
            }
            return true
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            flingAnimator.start(-velocityX / 5000 * FLING_DURATION_MILLIS)
            pendingFlingVisualDeltaX = 0.0f
            return true
        }

        override fun onDown(e: MotionEvent?): Boolean {
            return true
        }
    })

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            onRescale(detector.scaleFactor)
            return true
        }
    })

    private val drawContext = PlotDrawContext()
    private val drawerCallback: (DataPoint) -> Boolean = this::doDraw

    private val flingAnimator = IterationAwareValueAnimator(FLING_DURATION_MILLIS, DecelerateInterpolator()) {
        onScroll(it, true)
    }
    private var pendingFlingVisualDeltaX = 0.0f

    private lateinit var _dataAnchor: Any

    private lateinit var config: ChartConfig
    private lateinit var palette: ChartPalette
    private lateinit var model: ChartModel
    private lateinit var drawData: ChartDrawData

    private lateinit var noChartsBitmap: Bitmap

    private val roundedRectangleDrawer = RoundedRectangleDrawer.INSTANCE

    private val modelListener = object : ChartModelListener {
        override fun onRangeChanged(anchor: Any) {
            if (anchor === dataAnchor) {
                invalidate()
            }
        }

        override fun onDataSourceEnabled(dataSource: ChartDataSource) {
            if (::config.isInitialized && config.animationEnabled) {
                drawData.fadeIn(dataSource)
                invalidate()
            }
        }

        override fun onDataSourceDisabled(dataSource: ChartDataSource) {
            if (::config.isInitialized && config.animationEnabled) {
                if (areThereOtherActiveDataSources(dataSource)) {
                    drawData.fadeOut(dataSource)
                }
                invalidate()
            }
        }

        override fun onDataSourceAdded(dataSource: ChartDataSource) {
            refreshDataSources()
            if (::config.isInitialized && config.animationEnabled) {
                if (areThereOtherActiveDataSources(dataSource)) {
                    drawData.fadeIn(dataSource)
                }
            }
            invalidate()
        }

        override fun onDataSourceRemoved(dataSource: ChartDataSource) {
            refreshDataSources()
            if (::config.isInitialized && config.animationEnabled) {
                drawData.fadeOut(dataSource)
            }
            invalidate()
        }

        override fun onActiveDataPointsLoaded(anchor: Any) {
            if (anchor === dataAnchor) {
                invalidate()
            }
        }

        override fun onPointsLoadingIterationEnd(dataSource: ChartDataSource) {
        }

        override fun onSelectionChange() {
            invalidate()
        }

        override fun onMinimum(minX: Long) {
            invalidate()
        }

        override fun onMaximum(maxX: Long) {
            invalidate()
        }
    }

    private var lastClickVisualX = 0f
    private var lastClickVisualY = 0f

    private var legendRect: RectF? = null
    private var rescaled = false

    init {
        setOnTouchListener { _, event ->
            lastClickVisualX = event.x
            lastClickVisualY = event.y
            false
        }
    }

    private fun areThereOtherActiveDataSources(dataSource: ChartDataSource): Boolean {
        return model.registeredDataSources.any {
            it != dataSource && model.isActive(it)
        }
    }

    fun apply(config: ChartConfig) {
        this.config = config
        palette = ChartPalette(config)
        mayBeInitNoChartsBitmap(config)
        if (::model.isInitialized) {
            drawData = ChartDrawData(palette = palette,
                                     view = this,
                                     model = model,
                                     config = config)
            invalidate()
        }
    }

    private fun mayBeInitNoChartsBitmap(config: ChartConfig) {
        val drawableId = config.noChartsDrawableId
        if (drawableId != null) {
            noChartsBitmap = BitmapFactory.decodeResource(resources, drawableId)
        }
    }

    fun apply(chartModel: ChartModel, dataAnchor: Any? = null) {
        _dataAnchor = dataAnchor ?: this
        if (::model.isInitialized && model !== chartModel) {
            model.removeListener(modelListener)
        }
        chartModel.addListener(modelListener)
        model = chartModel
        drawData = ChartDrawData(palette = palette,
                                 view = this,
                                 model = model,
                                 config = config)
        refreshDataSources()
        invalidate()
    }

    private fun refreshDataSources() {
        dataSources.clear()
        dataSources.addAll(model.registeredDataSources)
        dataSources.sortBy { it.legend }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setMeasuredDimension(width, width)
        } else {
            setMeasuredDimension(width, width / 4)
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        if (!::palette.isInitialized || !::drawData.isInitialized) {
            return
        }

        drawData.refresh()

        drawBackground(canvas)
        drawGrid(canvas)
        drawPlots(canvas)
        drawSelection(canvas)
    }

    private fun drawBackground(canvas: Canvas) {
        if (config.drawBackground) {
            canvas.drawPaint(palette.backgroundPaint)
        }
    }

    private fun drawGrid(canvas: Canvas) {
        drawXAxis(canvas)
        drawYAxis(canvas)
    }

    private fun drawXAxis(canvas: Canvas) {
        drawXAxisLine(canvas)
        drawXAxisLabels(canvas)
    }

    private fun drawXAxisLine(canvas: Canvas) {
        if (!config.xAxisConfig.drawAxis) {
            return
        }
        val y = drawData.chartBottom.toFloat()
        val clipBounds = canvas.clipBounds
        if (y <= clipBounds.bottom) {
            canvas.drawLine(0f, y, width.toFloat(), y, palette.gridPaint)
        }
    }

    private fun drawXAxisLabels(canvas: Canvas) {
        if (!config.xAxisConfig.drawLabels) {
            return
        }
        val clipBounds = canvas.clipBounds
        val height = height
        val minY = height - drawData.xAxis.labelHeight
        if (clipBounds.bottom <= minY) {
            return
        }

        if (drawData.xAxis.animator.inProgress) {
            with(drawData.xAxis.animator) {
                drawXAxisLabels(canvas, rangeFrom, initialStep, initialStepAlpha)
            }
            with(drawData.xAxis) {
                drawXAxisLabels(canvas, range, axisStep, drawData.xAxis.animator.finalStepAlpha)
            }
        } else {
            with(drawData.xAxis) {
                drawXAxisLabels(canvas, range, axisStep, 255)
            }
        }
    }

    private fun drawXAxisLabels(canvas: Canvas, range: Range, step: Long, alpha: Int) {
        val unitWidth = width.toFloat() / range.size
        val paint = palette.xLabelPaint.apply { this.alpha = alpha }
        var value = range.findFirstStepValue(step) ?: return
        var x = drawData.xAxis.visualShift + unitWidth * (value - range.start)
        while (x >= 0 && x < width) {
            val label = config.xAxisConfig.labelTextStrategy.getLabel(value, step)
            canvas.drawText(label.data, 0, label.length, x, height.toFloat(), paint)
            value += step
            x += step * unitWidth
        }
    }

    private fun drawYAxis(canvas: Canvas) {
        if (!config.yAxisConfig.drawAxis || drawData.yAxis.range.empty) {
            return
        }

        if (drawData.yAxis.animator.inProgress) {
            with(drawData.yAxis.animator) {
                drawYGrid(canvas, rangeFrom, initialStep, initialStepAlpha)
            }
            with(drawData.yAxis) {
                drawYGrid(canvas, range, axisStep, animator.finalStepAlpha)
            }
        } else {
            with(drawData.yAxis) {
                drawYGrid(canvas, range, axisStep, 255)

            }
        }
    }

    private fun drawYGrid(canvas: Canvas, range: Range, dataStep: Long, alpha: Int) {
        val drawGrid = alpha > 128
        var value = range.findFirstStepValue(dataStep) ?: return
        while (true) {
            var y = drawData.dataYToVisualY(value)
            if (y <= 0) {
                break
            } else if (y <= drawData.chartBottom) {
                if (drawGrid) {
                    canvas.drawLine(0f, y, width.toFloat(), y, palette.gridPaint)
                }
                val label = drawData.yAxis.valueStrategy.getLabel(value, dataStep)
                y -= drawData.yAxis.labelPadding
                if (y - drawData.yAxis.labelHeight <= 0) {
                    break
                }
                if (config.yAxisConfig.drawLabels) {
                    canvas.drawText(label.data, 0, label.length, 0f, y, palette.yLabelPaint)
                    drawData.onYLabel(label)
                }
            }

            value += dataStep
            if (value > range.end) {
                break
            }
        }
    }

    private fun drawPlots(canvas: Canvas) {
        var plotDrawn = false
        drawContext.minVisualX = drawData.chartLeft.toFloat()
        drawContext.maxVisualX = width.toFloat()
        for (dataSource in dataSources) {
            plotDrawn = plotDrawn or drawPlot(dataSource, canvas)
        }

        if (!plotDrawn && ::noChartsBitmap.isInitialized) {
            val left = drawData.chartLeft + (width - drawData.chartLeft - noChartsBitmap.width) / 2
            val top = (drawData.chartBottom - noChartsBitmap.height) / 2
            canvas.drawBitmap(noChartsBitmap, left.toFloat(), top.toFloat(), null)
        }
    }

    private fun drawPlot(dataSource: ChartDataSource, canvas: Canvas): Boolean {
        val paint = palette.plotPaint.apply {
            color = dataSource.color
        }
        if (drawData.isAnimationInProgress(dataSource)) {
            paint.alpha = drawData.getCurrentAlpha(dataSource)

        } else if (!model.isActive(dataSource)) {
            return false
        }

        val dataXStart: Long
        val dataXEnd: Long
        with(drawData.xAxis.animator) {
            if (inProgress) {
                dataXStart = min(rangeFrom.start, rangeTo.start)
                dataXEnd = max(rangeFrom.end, rangeTo.end)
            } else {
                dataXStart = drawData.xAxis.range.start
                dataXEnd = drawData.xAxis.range.end
            }
        }

        drawContext.previousVisualPoint = null
        drawContext.drawn = false
        drawContext.path = Path()
        model.forRangePoints(dataSource,
                             dataXStart,
                             dataXEnd,
                             includePrevious = true,
                             includeNext = true,
                             action = drawerCallback)
        canvas.drawPath(drawContext.path, paint)
        return drawContext.drawn
    }

    private fun doDraw(dataPoint: DataPoint): Boolean {
        val visualPoint = drawData.dataPointToVisualPoint(dataPoint)
        val previousVisualPoint = drawContext.previousVisualPoint
        if (previousVisualPoint == null || visualPoint.x < drawContext.minVisualX) {
            drawContext.previousVisualPoint = visualPoint
            return true
        }

        var x: Float
        var y: Float
        if (visualPoint.x > drawContext.maxVisualX) {
            val formula = calculateLineFormula(previousVisualPoint, visualPoint)
            x = drawContext.maxVisualX
            y = formula.getY(x)
            if (y > drawData.chartBottom) {
                y = drawData.chartBottom.toFloat()
                x = formula.getX(y)
            }
            if (!drawContext.drawn) {
                drawContext.path.moveTo(previousVisualPoint.x, previousVisualPoint.y)
            }
            drawContext.path.lineTo(x, y)
            return false
        }

        if (previousVisualPoint.x < drawContext.minVisualX) {
            val formula = calculateLineFormula(previousVisualPoint, visualPoint)
            x = drawContext.minVisualX
            y = formula.getY(x)
            if (y > drawData.chartBottom) {
                y = drawData.chartBottom.toFloat()
                x = formula.getX(y)
            }
            drawContext.path.moveTo(x, y)
            if (visualPoint.x != x || visualPoint.y != y) {
                drawContext.path.lineTo(visualPoint.x, visualPoint.y)
            }
            drawContext.drawn = true
            drawContext.previousVisualPoint = visualPoint
            return true
        }

        if (!drawContext.drawn) {
            drawContext.drawn = true
            drawContext.path.moveTo(previousVisualPoint.x, previousVisualPoint.y)
        }

        if (visualPoint.x - previousVisualPoint.x >= MIN_PLOT_UNIT_PX) {
            drawContext.path.lineTo(visualPoint.x, visualPoint.y)
            drawContext.previousVisualPoint = visualPoint
        }
        return true
    }

    private fun calculateLineFormula(p1: VisualPoint, p2: VisualPoint): LineFormula {
        val a = (p1.y - p2.y) / (p1.x - p2.x)
        val b = p1.y - a * p1.x
        return LineFormula(a, b)
    }

    private fun drawSelection(canvas: Canvas) {
        legendRect = null

        if (!config.drawSelection || !model.hasSelection) {
            return
        }

        val dataX = model.selectedX
        val visualX = drawData.dataXToVisualX(dataX)
        if (visualX < drawData.chartLeft || visualX > width) {
            return
        }

        canvas.drawLine(visualX, drawData.chartBottom.toFloat(), visualX, 0f, palette.gridPaint)

        val dataSource2yInfo = mutableMapOf<ChartDataSource, ValueInfo>()
        for (dataSource in dataSources) {
            if (!model.isActive(dataSource)) {
                continue
            }

            val previous = model.getThisOrPrevious(dataSource, model.selectedX)

            val dataY = if (previous == null) {
                continue
            } else if (previous.x == model.selectedX) {
                previous.y
            } else {
                val next = getNext(dataSource, model.selectedX)
                if (next == null) {
                    continue
                } else {
                    val formula = calculateLineFormula(VisualPoint(previous.x.toFloat(),
                                                                   previous.y.toFloat()),
                                                       VisualPoint(next.x.toFloat(),
                                                                   next.y.toFloat()))
                    formula.getY(dataX.toFloat()).toDouble().roundToLong()
                }
            }

            val visualPoint = drawData.dataPointToVisualPoint(DataPoint(dataX, dataY))
            dataSource2yInfo[dataSource] = ValueInfo(dataY, visualPoint.y)
            val yShift = config.plotLineWidthInPixels / 2
            drawSelectionPlotSign(canvas,
                                  VisualPoint(visualPoint.x, visualPoint.y - yShift),
                                  dataSource.color)
        }

        drawSelectionLegend(canvas, visualX, dataSource2yInfo)
    }

    private fun getNext(dataSource: ChartDataSource, baseX: Long): DataPoint? {
        var x = baseX
        while (true) {
            val point = model.getNext(dataSource, x) ?: return null
            x = point.x
            val dx = (point.x - baseX) * drawData.xAxis.unitSize
            if (dx.roundToLong() < MIN_PLOT_UNIT_PX) {
                continue
            }
            return point
        }
    }

    private fun drawSelectionPlotSign(canvas: Canvas, point: VisualPoint, color: Int) {
        val paint = palette.plotPaint.apply {
            this.color = config.backgroundColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(point.x, point.y, config.selectionSignRadiusInPixels.toFloat(), paint)

        paint.color = color
        paint.style = Paint.Style.STROKE
        canvas.drawCircle(point.x, point.y, config.selectionSignRadiusInPixels.toFloat(), paint)
    }

    private fun drawSelectionLegend(canvas: Canvas, x: Float, dataSource2yInfo: Map<ChartDataSource, ValueInfo>) {
        val context = LegendDrawContext(dataSource2yInfo)
        val shouldDraw = (fillLegendInternalHorizontalData(context)
                          && fillLegendInternalVerticalData(context)
                          && locateLegend(context, x, dataSource2yInfo))
        if (shouldDraw) {
            doDrawLegend(canvas, context)
        }
    }

    private fun fillLegendInternalHorizontalData(context: LegendDrawContext): Boolean {
        if (context.dataSource2yInfo.isEmpty()) {
            return false
        }
        context.horizontalPadding = drawData.yAxis.labelHeight * 7 / 5

        val legendTitle = config.xAxisConfig.labelTextStrategy.getLabel(model.selectedX,
                                                                        drawData.xAxis.axisStep)
        val legendTitleWidth = drawData.getLegendValueWidth(legendTitle)
        if (legendTitleWidth + context.horizontalPadding * 2 > width) {
            context.tooNarrow = true
            return true
        }

        var legendTextWidth = measureLegendTextWidth(context, false)
        if (legendTextWidth < 0) {
            legendTextWidth = measureLegendTextWidth(context, true)
            if (legendTextWidth > 0) {
                context.minifiedValues = true
            }
        }

        if (legendTextWidth < 0) {
            context.tooNarrow = true
        }

        context.legendWidth = max(legendTitleWidth, legendTextWidth) + context.horizontalPadding * 2
        return true
    }

    private fun measureLegendTextWidth(context: LegendDrawContext, minifyValues: Boolean): Int {
        var first = true
        var legendTextWidth = 0
        for (dataSource in dataSources) {
            if (!model.isActive(dataSource)) {
                continue
            }
            val valueInfo = context.dataSource2yInfo[dataSource] ?: continue
            val dataY = valueInfo.dataValue
            val value = if (minifyValues) {
                config.yAxisConfig.labelTextStrategy.getMinifiedLabel(dataY, drawData.yAxis.axisStep)
            } else {
                config.yAxisConfig.labelTextStrategy.getLabel(dataY, drawData.yAxis.axisStep)
            }
            val valueWidth = drawData.getLegendValueWidth(value)
            val legendWidth = drawData.getYLabelWidth(dataSource.legend)
            legendTextWidth += max(valueWidth, legendWidth)
            if (first) {
                first = false
            } else {
                legendTextWidth += context.horizontalPadding
            }

            if (legendTextWidth + context.horizontalPadding * 2 > width) {
                return -1
            }
        }

        return legendTextWidth
    }

    private fun fillLegendInternalVerticalData(context: LegendDrawContext): Boolean {
        context.verticalPadding = drawData.yAxis.labelHeight
        context.titleYShift = context.verticalPadding + drawData.yAxis.labelHeight
        context.valueYShift = context.titleYShift + drawData.legendLabelHeight * 2
        context.legendYShift = context.valueYShift + drawData.yAxis.labelHeight * 5 / 2
        context.legendHeight = context.legendYShift + context.verticalPadding
        return context.legendHeight <= height
    }

    private fun locateLegend(context: LegendDrawContext,
                             selectionVisualX: Float,
                             dataSource2yInfo: Map<ChartDataSource, ValueInfo>): Boolean {
        // Ideally we'd like to show legend above selection
        val selectionYs = mutableListOf<Float>()
        for (valueInfo in dataSource2yInfo.values) {
            selectionYs.add(valueInfo.visualValue)
        }
        selectionYs.sort()

        var topLimit = 0f
        var legendTop = -1f
        for (selectionY in selectionYs) {
            if (context.legendHeight + context.verticalPadding * 2 <= selectionY - topLimit) {
                legendTop = selectionY - context.verticalPadding.toFloat() - context.legendHeight.toFloat()
                break
            } else {
                topLimit = selectionY
            }
        }

        if (context.legendHeight + context.verticalPadding * 2 <= drawData.chartBottom - topLimit) {
            legendTop = topLimit + context.verticalPadding
        }

        if (legendTop >= 0) {
            // Show legend above the selected X in a way to not hiding selection points.
            context.topOnChart = legendTop
            if (context.tooNarrow) {
                context.leftOnChart = context.horizontalPadding.toFloat()
                context.legendWidth = width
            } else {
                val desiredLeft = selectionVisualX - context.legendWidth * 5f / 28f
                context.leftOnChart = normalizeLeft(max(0f, desiredLeft), context.legendWidth.toFloat())
            }
            return true
        }

        // We can't show legend above the selected X without hiding one or more selection points.
        if (context.tooNarrow) {
            context.leftOnChart = context.horizontalPadding.toFloat()
            context.legendWidth = width
            context.topOnChart = context.verticalPadding.toFloat()
            return true
        }

        if (selectionVisualX + context.horizontalPadding.toFloat() + context.legendWidth.toFloat() <= width) {
            // We can show legend to the right of selected x
            context.leftOnChart = selectionVisualX + context.horizontalPadding
            context.topOnChart = max(0f, selectionYs[0] - context.legendHeight * 5f / 28f)
            return true
        }

        if (selectionVisualX - context.horizontalPadding.toFloat() - context.legendWidth.toFloat() >= 0) {
            // We can show legend to the left of selected x
            context.leftOnChart = selectionVisualX - context.horizontalPadding.toFloat() - context.legendWidth.toFloat()
            context.topOnChart = max(0f, selectionYs[0] - context.legendHeight * 5f / 28f)
            return true
        }

        // We failed finding a location where legend doesn't hid selection points, let's show it above them.
        context.leftOnChart =
            normalizeLeft(max(0f, selectionVisualX - context.legendWidth * 5f / 28f),
                          context.legendWidth.toFloat())
        context.topOnChart = context.verticalPadding.toFloat()
        return true
    }

    private fun normalizeLeft(left: Float, width: Float): Float {
        val xToShiftLeft = left + width - this.width
        return if (xToShiftLeft <= 0) {
            left
        } else max(drawData.chartLeft.toFloat(), left - xToShiftLeft)
    }

    private fun doDrawLegend(canvas: Canvas, context: LegendDrawContext) {
        val rect = RectF(context.leftOnChart,
                         context.topOnChart,
                         context.leftOnChart + context.legendWidth,
                         context.topOnChart + context.legendHeight)
        legendRect = rect

        roundedRectangleDrawer.draw(rect,
                                    { palette.gridPaint },
                                    { palette.legendBackgroundPaint },
                                    LeonardoUtil.DEFAULT_CORNER_RADIUS,
                                    canvas)
        drawLegendTitle(canvas, context)
        drawLegendValues(canvas, context)
    }

    private fun drawLegendTitle(canvas: Canvas, context: LegendDrawContext) {
        val xText = config.xAxisConfig.labelTextStrategy.getLabel(model.selectedX, drawData.xAxis.axisStep)
        canvas.drawText(xText.data,
                        0,
                        xText.length,
                        context.leftOnChart + context.horizontalPadding,
                        context.topOnChart + context.titleYShift,
                        palette.legendTitlePaint)
    }

    private fun drawLegendValues(canvas: Canvas, context: LegendDrawContext) {
        var x = context.leftOnChart + context.horizontalPadding
        val valueY = context.topOnChart + context.valueYShift
        val legendY = context.topOnChart + context.legendYShift
        for (dataSource in dataSources) {
            if (!model.isActive(dataSource)) {
                continue
            }

            val valueInfo = context.dataSource2yInfo[dataSource] ?: continue
            val value = if (context.minifiedValues) {
                drawData.yAxis.valueStrategy.getMinifiedLabel(valueInfo.dataValue, drawData.yAxis.axisStep)
            } else {
                drawData.yAxis.valueStrategy.getLabel(valueInfo.dataValue, drawData.yAxis.axisStep)
            }
            canvas.drawText(value.data, 0, value.length, x, valueY, palette.legendValuePaint.apply {
                color = dataSource.color
            })

            canvas.drawText(dataSource.legend, x, legendY, palette.yLabelPaint.apply {
                color = dataSource.color
            })

            x += max(drawData.getLegendValueWidth(value), drawData.getYLabelWidth(dataSource.legend))
            x += context.horizontalPadding.toFloat()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        rescaled = false
        scaleDetector.onTouchEvent(event)
        if (!rescaled && event.pointerCount == 1) {
            gestureDetector.onTouchEvent(event)
        }
        return true
    }

    private fun onRescale(scaleFactor: Float) {
        rescaled = true
        if (scaleFactor == 1.0f) {
            return
        }
        val currentRange = model.getActiveRange(dataAnchor)
        if (currentRange.size <= 3 && scaleFactor > 1) {
            return
        }
        val currentPointsNumber = currentRange.size + 1
        var newRangePointsNumber = (currentPointsNumber / scaleFactor).toLong()
        if (newRangePointsNumber == currentPointsNumber && scaleFactor < 1f) {
            newRangePointsNumber += 2
        }
        if (newRangePointsNumber <= 3 || newRangePointsNumber == currentPointsNumber) {
            return
        }

        val totalDelta = newRangePointsNumber - currentPointsNumber
        val rightDelta = totalDelta / 2
        val leftDelta = rightDelta - totalDelta
        val newRange = Range(currentRange.start + leftDelta, currentRange.end + rightDelta)
            .mayBeCut(model.minX, model.maxX)
        if (newRange == currentRange) {
            return
        }

        model.setActiveRange(newRange, dataAnchor)
        drawData.refresh()
    }

    private fun onScroll(deltaVisualX: Float, fling: Boolean) {
        val deltaVisualXToUse = if (fling) {
            deltaVisualX + pendingFlingVisualDeltaX
        } else {
            flingAnimator.cancel()
            deltaVisualX
        }
        val activeRange = model.getActiveRange(dataAnchor)
        val minX = model.minX
        val maxX = model.maxX
        if (deltaVisualXToUse == 0.0f
            || (deltaVisualXToUse < 0 && minX != null && minX >= activeRange.start)
            || (deltaVisualXToUse > 0 && maxX != null && maxX <= activeRange.end)
        ) {
            pendingFlingVisualDeltaX = deltaVisualXToUse
            return
        }

        var deltaDataX = (deltaVisualXToUse / drawData.xAxis.unitSize).toLong()
        if (deltaDataX == 0L) {
            if (fling) {
                pendingFlingVisualDeltaX = deltaVisualXToUse
                return
            } else {
                deltaDataX = if (deltaVisualXToUse < 0) {
                    -2L
                } else {
                    2L
                }
            }
        }
        pendingFlingVisualDeltaX = 0.0f
        model.setActiveRange(activeRange.shift(deltaDataX), dataAnchor)
    }

    private fun onTap() {
        flingAnimator.cancel()
        val legendRect = legendRect
        if (legendRect != null
            && ::model.isInitialized
            && legendRect.contains(lastClickVisualX, lastClickVisualY)
        ) {
            model.resetSelection()
            return
        }

        if (!::config.isInitialized || !::model.isInitialized || !config.selectionAllowed) {
            return
        }

        model.selectedX = drawData.visualXToDataX(lastClickVisualX)
    }

    companion object {
        /**
         * There is a possible case that the scale is so small that thousands/millions of data points are covered
         * by the active range. There is no point for us in trying to draw edges between every to points then.
         * We choose only subset of points which are distant enough from each other then.
         *
         * Current constant defines minimum interested visual length to use during that.
         */
        const val MIN_PLOT_UNIT_PX = 2

        const val FLING_DURATION_MILLIS = 500
    }

    private data class ValueInfo(val dataValue: Long, val visualValue: Float)

    private class LegendDrawContext internal constructor(internal val dataSource2yInfo: Map<ChartDataSource, ValueInfo>) {

        var tooNarrow: Boolean = false
        var minifiedValues: Boolean = false

        var horizontalPadding: Int = 0
        var legendWidth: Int = 0

        var verticalPadding: Int = 0
        var titleYShift: Int = 0
        var valueYShift: Int = 0
        var legendYShift: Int = 0
        var legendHeight: Int = 0

        var leftOnChart: Float = 0f
        var topOnChart: Float = 0f
    }
}