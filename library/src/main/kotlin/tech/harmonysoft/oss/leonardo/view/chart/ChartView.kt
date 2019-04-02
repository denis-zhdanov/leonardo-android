package tech.harmonysoft.oss.leonardo.view.chart

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import tech.harmonysoft.oss.leonardo.model.DataPoint
import tech.harmonysoft.oss.leonardo.model.LineFormula
import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.VisualPoint
import tech.harmonysoft.oss.leonardo.model.config.chart.ChartConfig
import tech.harmonysoft.oss.leonardo.model.data.ChartDataSource
import tech.harmonysoft.oss.leonardo.model.runtime.ChartModel
import tech.harmonysoft.oss.leonardo.model.runtime.ChartModelListener
import tech.harmonysoft.oss.leonardo.model.runtime.DataMapper
import tech.harmonysoft.oss.leonardo.model.text.TextWrapper
import tech.harmonysoft.oss.leonardo.model.util.LeonardoUtil
import tech.harmonysoft.oss.leonardo.view.util.RoundedRectangleDrawer
import tech.harmonysoft.oss.leonardo.view.util.TextSpaceMeasurer
import tech.harmonysoft.oss.leonardo.view.util.TextWidthMeasurer
import java.util.*
import kotlin.collections.ArrayList

class ChartView @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null,
    defaultStyle: Int = 0
) : View(context, attributes, defaultStyle) {

    val dataAnchor: Any = this
    val dataMapper: DataMapper by lazy {
        drawData
    }
    val xVisualShift: Float
        get() {
            return drawData.xAxis.visualShift
        }

    /**
     * Keep own data sources list in order to work with them in lexicographically, e.g. when showing selection legend
     */
    private val dataSources = mutableListOf<ChartDataSource>()

    private lateinit var yLabelWidthMeasurer: TextWidthMeasurer
    private lateinit var config: ChartConfig
    private lateinit var drawSetup: ChartDrawSetup
    private lateinit var model: ChartModel
    private lateinit var drawData: ChartDrawData

    private val mRoundedRectangleDrawer = RoundedRectangleDrawer()

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
                drawData.fadeOut(dataSource)
                invalidate()
            }
        }

        override fun onDataSourceAdded(dataSource: ChartDataSource) {
            refreshDataSources()
            if (::config.isInitialized && config.animationEnabled) {
                drawData.fadeIn(dataSource)
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

        override fun onSelectionChange() {
            invalidate()
        }
    }

    private lateinit var mChartConfig: ChartConfig
    private var mConfigApplied = false

    private lateinit var mPlotPaint: Paint

    private lateinit var mLegendValuePaint: Paint
    private lateinit var mLegendValueWidthMeasurer: TextSpaceMeasurer
    private var mLegendValueHeight: Int = 0

    private var mLastClickVisualX: Float = 0.toFloat()
    private var mLastClickVisualY: Float = 0.toFloat()

    private var mLegendRect: RectF? = null

    init {
        setOnTouchListener { _, event ->
            mLastClickVisualX = event.x
            mLastClickVisualY = event.y
            false
        }
        setOnClickListener { _ ->
            val legendRect = mLegendRect
            if (legendRect != null && ::model.isInitialized && legendRect.contains(mLastClickVisualX,
                                                                                   mLastClickVisualY)
            ) {
                model.resetSelection()
                return@setOnClickListener
            }

            if (!::mChartConfig.isInitialized || !::model.isInitialized || !mChartConfig.selectionAllowed) {
                return@setOnClickListener
            }

            model.selectedX = drawData.visualXToDataX(mLastClickVisualX)
        }
    }

    fun apply(config: ChartConfig) {
        this.config = config
        drawSetup = ChartDrawSetup(config)
        if (::model.isInitialized) {
            drawData = ChartDrawData(drawSetup = drawSetup,
                                     view = this,
                                     model = model,
                                     dataAnchor = dataAnchor,
                                     config = config)
            invalidate()
        }
    }

    private fun applyConfig() {
        if (mConfigApplied) {
            return
        }

        initLegend()

        mConfigApplied = true
        invalidate()
    }

    private fun initLegend() {
        val paint = Paint()
        paint.typeface = Typeface.DEFAULT
        paint.textSize = (mChartConfig.yAxisConfig.labelFontSizeInPixels * 3 / 2).toFloat()
        mLegendValuePaint = paint
        val fontMetrics = mLegendValuePaint.fontMetrics
        mLegendValueHeight = (fontMetrics.descent - fontMetrics.ascent).toInt()
        mLegendValueWidthMeasurer = TextWidthMeasurer(mLegendValuePaint)
    }

    fun apply(chartModel: ChartModel) {
        if (::model.isInitialized && model !== chartModel) {
            model.removeListener(modelListener)
        }
        chartModel.addListener(modelListener)
        model = chartModel
        if (::config.isInitialized) {
            drawData = ChartDrawData(drawSetup = drawSetup,
                                     view = this,
                                     model = model,
                                     dataAnchor = dataAnchor,
                                     config = config)
        }
        refreshDataSources()
        invalidate()
    }

    private fun refreshDataSources() {
        dataSources.clear()
        dataSources.addAll(model.registeredDataSources)
        dataSources.sortWith(COMPARATOR)
    }

    fun scrollHorizontally(deltaVisualX: Float) {
        val effectiveDeltaVisualX = deltaVisualX + drawData.xAxis.visualShift
        drawData.refresh()
        val currentXRange = drawData.xAxis.range
        val deltaDataX = (-effectiveDeltaVisualX / drawData.xAxis.unitSize).toLong()

//        var minDataX = java.lang.Long.MAX_VALUE
//        var maxDataX = java.lang.Long.MIN_VALUE
//        for (activeDataSource in dataSources) {
//            val range = activeDataSource.getDataRange()
//            if (range.getStart() > java.lang.Long.MIN_VALUE && range.getStart() < minDataX) {
//                minDataX = range.getStart()
//            }
//            if (range.getEnd() < java.lang.Long.MAX_VALUE && range.getEnd() > maxDataX) {
//                maxDataX = range.getEnd()
//            }
//        }
//        if (deltaDataX > 0 && currentXRange.contains(maxDataX) || deltaDataX < 0 && currentXRange.contains(minDataX)) {
//            // We can't scroll more as we're already at the min/max possible edge
//            return
//        }

        drawData.xAxis.visualShift = effectiveDeltaVisualX % drawData.xAxis.unitSize
        if (deltaDataX != 0L) {
            model.setActiveRange(currentXRange.shift(deltaDataX), dataAnchor)
        }
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val edge = View.MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(edge, edge)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        if (!::drawSetup.isInitialized || !::drawData.isInitialized) {
            return
        }
        if (!::yLabelWidthMeasurer.isInitialized) {
            yLabelWidthMeasurer = TextWidthMeasurer(drawSetup.yLabelPaint)
        }

        drawData.refresh()

        drawBackground(canvas)
        drawGrid(canvas)
        drawPlots(canvas)
        // TODO den uncomment
//        drawSelection(canvas)
    }

    private fun drawBackground(canvas: Canvas) {
        if (config.drawBackground) {
            canvas.drawPaint(drawSetup.backgroundPaint)
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
            canvas.drawLine(0f, y, width.toFloat(), y, drawSetup.gridPaint)
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
        val labelTextStrategy = config.xAxisConfig.labelTextStrategy
        val paint = drawSetup.xLabelPaint.apply { this.alpha = alpha }
        var value = range.findFirstStepValue(step)
        var x = drawData.xAxis.visualShift + unitWidth * (value - range.start)
        while (x < width) {
            val label = labelTextStrategy.getLabel(value, step)
            canvas.drawText(label.data, 0, label.length, x, height.toFloat(), paint)
            value += step
            x += step * unitWidth
        }
    }

    private fun drawYAxis(canvas: Canvas) {
        drawYGrid(canvas)
    }

    private fun drawYGrid(canvas: Canvas) {
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
        val firstValue = range.findFirstStepValue(dataStep)
        var value = firstValue
        while (true) {
            var y = drawData.dataYToVisualY(value)
            if (y <= 0) {
                break
            } else if (y <= drawData.chartBottom) {
                if (drawGrid) {
                    canvas.drawLine(0f,
                                    y,
                                    width.toFloat(),
                                    y,
                                    drawSetup.gridPaint.apply {
                                        this.alpha = alpha
                                    })
                }
                val label = drawData.yAxis.valueStrategy.getLabel(value, dataStep)
                y -= drawData.yAxis.labelPadding
                if (y - drawData.yAxis.labelHeight <= 0) {
                    break
                }
                if (config.yAxisConfig.drawLabels) {
                    canvas.drawText(label.data,
                                    0,
                                    label.length,
                                    0f,
                                    y,
                                    drawSetup.yLabelPaint.apply {
                                        this.alpha = alpha
                                    })
                }
                drawData.maxYLabelWidth = Math.max(drawData.maxYLabelWidth, yLabelWidthMeasurer.measureVisualSpace(label))
            }

            value += dataStep
            if (value > range.end) {
                break
            }
        }
    }

    private fun drawPlots(canvas: Canvas) {
        for (dataSource in dataSources) {
            drawPlot(dataSource, canvas)
        }
    }

    private fun drawPlot(dataSource: ChartDataSource, canvas: Canvas) {
        val paint = drawSetup.plotPaint.apply {
            color = dataSource.color
        }
        if (!drawData.isAnimationInProgress(dataSource)) {
            if (!model.isActive(dataSource)) {
                return
            }
        } else {
            paint.alpha = drawData.getCurrentAlpha(dataSource)
        }
        val interval = model.getCurrentRangePoints(dataSource, dataAnchor)

        val minX = drawData.chartLeft.toFloat()
        val maxX = width.toFloat()

        val points = ArrayList<DataPoint>(interval.size + 2)
        val previous = model.getPreviousPointForActiveRange(dataSource, dataAnchor)
        if (previous != null) {
            points.add(previous)
        }
        points.addAll(interval)
        val next = model.getNextPointForActiveRange(dataSource, dataAnchor)
        if (next != null) {
            points.add(next)
        }

        val path = Path()
        var first = true
        var stop = false
        var previousVisualPoint: VisualPoint? = null
        var i = 0
        while (!stop && i < points.size) {
            val point = points.get(i)
            val visualPoint = drawData.dataPointToVisualPoint(point)
            if (i == 0) {
                previousVisualPoint = visualPoint
                i++
                continue
            }

            if (previousVisualPoint!!.x < minX && visualPoint.x <= minX) {
                previousVisualPoint = visualPoint
                i++
                continue
            }

            var x: Float
            var y: Float
            if (previousVisualPoint.x < minX) {
                val formula = calculateLineFormula(previousVisualPoint, visualPoint)
                x = minX
                y = formula.getY(x)
                if (y > drawData.chartBottom) {
                    y = drawData.chartBottom.toFloat()
                    x = formula.getX(y)
                }
            } else if (visualPoint.x > maxX) {
                if (first) {
                    first = false
                    path.moveTo(previousVisualPoint.x, previousVisualPoint.y)
                } else {
                    path.lineTo(previousVisualPoint.x, previousVisualPoint.y)
                }
                val formula = calculateLineFormula(previousVisualPoint, visualPoint)
                x = maxX
                y = formula.getY(x)
                if (y > drawData.chartBottom) {
                    y = drawData.chartBottom.toFloat()
                    x = formula.getX(y)
                }
                stop = true
            } else {
                x = previousVisualPoint.x
                y = previousVisualPoint.y
            }

            if (first) {
                first = false
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }

            if (!stop && i == points.size - 1) {
                var endX: Float
                var endY: Float
                if (visualPoint.x <= maxX) {
                    endX = visualPoint.x
                    endY = visualPoint.y
                } else {
                    val formula = calculateLineFormula(previousVisualPoint, visualPoint)
                    endX = maxX
                    endY = formula.getY(endX)
                    if (endY > drawData.chartBottom) {
                        endY = drawData.chartBottom.toFloat()
                        endX = formula.getX(endY)
                    }
                }
                path.lineTo(endX, endY)
            }

            previousVisualPoint = visualPoint
            i++
        }

        canvas.drawPath(path, paint)
    }

    private fun calculateLineFormula(p1: VisualPoint, p2: VisualPoint): LineFormula {
        val a = (p1.y - p2.y) / (p1.x - p2.x)
        val b = p1.y - a * p1.x
        return LineFormula(a, b)
    }

    private fun drawSelection(canvas: Canvas) {
        if (!mChartConfig.drawSelection || !model.hasSelection) {
            return
        }

        mLegendRect = null

        val dataX = model.selectedX
        val visualX = drawData.dataXToVisualX(dataX)
        if (visualX < 0f || visualX > width) {
            return
        }

        canvas.drawLine(visualX, drawData.chartBottom.toFloat(), visualX, 0f, drawSetup.gridPaint)

        val dataSource2yInfo = HashMap<ChartDataSource, ValueInfo>()
        for (dataSource in dataSources) {
            if (!model.isActive(dataSource)) {
                continue
            }
            val points = model.getCurrentRangePoints(dataSource, dataAnchor)
            if (points.isEmpty()) {
                continue
            }

            val dataY: Long

            val firstDataPoint = points.get(0)
            val lastDataPoint = points.get(points.size - 1)
            if (dataX < firstDataPoint.x) {
                val previousDataPoint =
                    model.getPreviousPointForActiveRange(dataSource, dataAnchor) ?: continue
                val formula = calculateLineFormula(VisualPoint(previousDataPoint.x.toFloat(),
                                                               previousDataPoint.y.toFloat()),
                                                   VisualPoint(firstDataPoint.x.toFloat(),
                                                               firstDataPoint.y.toFloat()))
                dataY = Math.round(formula.getY(dataX.toFloat()).toDouble())
            } else if (dataX > lastDataPoint.x) {
                val nextDataPoint = model.getNextPointForActiveRange(dataSource, dataAnchor) ?: continue
                val formula = calculateLineFormula(VisualPoint(lastDataPoint.x.toFloat(),
                                                               lastDataPoint.y.toFloat()),
                                                   VisualPoint(nextDataPoint.x.toFloat(),
                                                               nextDataPoint.y.toFloat()))
                dataY = Math.round(formula.getY(dataX.toFloat()).toDouble())
            } else {
                var i = Collections.binarySearch(points, DataPoint(dataX, 0), DataPoint.COMPARATOR_BY_X)
                if (i >= 0) {
                    dataY = points.get(i).y
                } else {
                    i = -(i + 1)
                    val prev = points.get(i - 1)
                    val next = points.get(i)
                    val formula = calculateLineFormula(VisualPoint(prev.x.toFloat(), prev.y.toFloat()),
                                                       VisualPoint(next.x.toFloat(), next.y.toFloat()))
                    dataY = Math.round(formula.getY(dataX.toFloat()).toDouble())
                }
            }

            val visualPoint = drawData.dataPointToVisualPoint(DataPoint(dataX, dataY))
            dataSource2yInfo[dataSource] =
                ValueInfo(dataY, visualPoint.y)
            val yShift = mChartConfig.plotLineWidthInPixels / 2
            drawSelectionPlotSign(canvas,
                                  VisualPoint(visualPoint.x, visualPoint.y - yShift),
                                  dataSource.color)
        }

        drawSelectionLegend(canvas, visualX, dataSource2yInfo)
    }

    private fun drawSelectionPlotSign(canvas: Canvas, point: VisualPoint, color: Int) {
        mPlotPaint.color = mChartConfig.backgroundColor
        mPlotPaint.style = Paint.Style.FILL
        canvas.drawCircle(point.x, point.y, mChartConfig.selectionSignRadiusInPixels.toFloat(), mPlotPaint)

        mPlotPaint.color = color
        mPlotPaint.style = Paint.Style.STROKE
        canvas.drawCircle(point.x, point.y, mChartConfig.selectionSignRadiusInPixels.toFloat(), mPlotPaint)
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
        val legendTitleWidth = yLabelWidthMeasurer.measureVisualSpace(legendTitle)
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

        context.legendWidth = Math.max(legendTitleWidth, legendTextWidth) + context.horizontalPadding * 2
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
            val valueWidth = mLegendValueWidthMeasurer.measureVisualSpace(value)
            val legendWidth = yLabelWidthMeasurer.measureVisualSpace(dataSource.legend)
            legendTextWidth += Math.max(valueWidth, legendWidth)
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
        context.valueYShift = context.titleYShift + mLegendValueHeight * 2
        context.legendYShift = context.valueYShift + drawData.yAxis.labelHeight * 2 / 3 + drawData.yAxis.labelHeight
        context.legendHeight = context.legendYShift + context.verticalPadding
        return context.legendHeight <= height
    }

    private fun locateLegend(context: LegendDrawContext,
                             selectionVisualX: Float,
                             dataSource2yInfo: Map<ChartDataSource, ValueInfo>): Boolean {
        // Ideally we'd like to show legend above selection
        val selectionYs = ArrayList<Float>()
        for (valueInfo in dataSource2yInfo.values) {
            selectionYs.add(valueInfo.visualValue)
        }
        Collections.sort(selectionYs)

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
                context.leftOnChart = normalizeLeft(Math.max(0f, desiredLeft), context.legendWidth.toFloat())
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
            context.topOnChart = Math.max(0f, selectionYs[0] - context.legendHeight * 5f / 28f)
            return true
        }

        if (selectionVisualX - context.horizontalPadding.toFloat() - context.legendWidth.toFloat() >= 0) {
            // We can show legend to the left of selected x
            context.leftOnChart = selectionVisualX - context.horizontalPadding.toFloat() - context.legendWidth.toFloat()
            context.topOnChart = Math.max(0f, selectionYs[0] - context.legendHeight * 5f / 28f)
            return true
        }

        // We failed finding a location where legend doesn't hid selection points, let's show it above them.
        context.leftOnChart =
            normalizeLeft(Math.max(0f, selectionVisualX - context.legendWidth * 5f / 28f),
                          context.legendWidth.toFloat())
        context.topOnChart = context.verticalPadding.toFloat()
        return true
    }

    private fun normalizeLeft(left: Float, width: Float): Float {
        val xToShiftLeft = left + width - this.width
        return if (xToShiftLeft <= 0) {
            left
        } else Math.max(0f, left - xToShiftLeft)
    }

    private fun doDrawLegend(canvas: Canvas, context: LegendDrawContext) {
        val rect = RectF(context.leftOnChart,
                         context.topOnChart,
                         context.leftOnChart + context.legendWidth,
                         context.topOnChart + context.legendHeight)
        mLegendRect = rect

        mRoundedRectangleDrawer.draw(rect,
                                     { drawSetup.gridPaint },
                                     { drawSetup.legendBackgroundPaint },
                                     LeonardoUtil.DEFAULT_CORNER_RADIUS,
                                     canvas)
        drawLegendTitle(canvas, context)
        drawLegendValues(canvas, context)
    }

    private fun drawLegendTitle(canvas: Canvas, context: LegendDrawContext) {
        val paint = drawSetup.yLabelPaint.apply {
            color = mChartConfig.legendTextTitleColor
        }
        val xText = config.xAxisConfig.labelTextStrategy.getLabel(model.selectedX,
                                                                  drawData.xAxis.axisStep)
        canvas.drawText(xText.data,
                        0,
                        xText.length,
                        context.leftOnChart + context.horizontalPadding,
                        context.topOnChart + context.titleYShift,
                        paint)
    }

    private fun drawLegendValues(canvas: Canvas, context: LegendDrawContext) {
        var x = context.leftOnChart + context.horizontalPadding
        val valueY = context.topOnChart + context.valueYShift
        val legendY = context.topOnChart + context.legendYShift
        for (dataSource in dataSources) {
            if (!model.isActive(dataSource)) {
                continue
            }
            val paint = drawSetup.yLabelPaint.apply {
                color = dataSource.color
            }
            val valueInfo = context.dataSource2yInfo[dataSource] ?: continue
            val value: TextWrapper
            if (context.minifiedValues) {
                value = drawData.yAxis.valueStrategy.getMinifiedLabel(valueInfo.dataValue, drawData.yAxis.axisStep)
            } else {
                value = drawData.yAxis.valueStrategy.getLabel(valueInfo.dataValue, drawData.yAxis.axisStep)
            }
            mLegendValuePaint.color = dataSource.color
            canvas.drawText(value.data, 0, value.length, x, valueY, mLegendValuePaint)

            canvas.drawText(dataSource.legend, x, legendY, paint)

            x += Math.max(mLegendValueWidthMeasurer.measureVisualSpace(value),
                          yLabelWidthMeasurer.measureVisualSpace(dataSource.legend))
            x += context.horizontalPadding.toFloat()
        }
    }

    companion object {

        private val COMPARATOR = Comparator<ChartDataSource> { s1, s2 -> s1.legend.compareTo(s2.legend) }
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

        var leftOnChart: Float = 0.toFloat()
        var topOnChart: Float = 0.toFloat()
    }
}