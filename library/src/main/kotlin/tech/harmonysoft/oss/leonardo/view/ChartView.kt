package tech.harmonysoft.oss.leonardo.view

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import tech.harmonysoft.oss.leonardo.model.DataPoint
import tech.harmonysoft.oss.leonardo.model.LineFormula
import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.Range.Companion.EMPTY_RANGE
import tech.harmonysoft.oss.leonardo.model.VisualPoint
import tech.harmonysoft.oss.leonardo.model.config.axis.AxisConfig
import tech.harmonysoft.oss.leonardo.model.config.chart.ChartConfig
import tech.harmonysoft.oss.leonardo.model.data.ChartDataSource
import tech.harmonysoft.oss.leonardo.model.runtime.ChartModel
import tech.harmonysoft.oss.leonardo.model.runtime.ChartModelListener
import tech.harmonysoft.oss.leonardo.model.text.TextWrapper
import tech.harmonysoft.oss.leonardo.model.util.LeonardoUtil
import tech.harmonysoft.oss.leonardo.model.util.LeonardoUtil.ANIMATION_DURATION_MILLIS
import tech.harmonysoft.oss.leonardo.view.util.*
import java.util.*
import kotlin.collections.ArrayList

/**
 * @author Denis Zhdanov
 * @since 27/3/19
 */
class ChartView @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null,
    defaultStyle: Int = 0
) : View(context, attributes, defaultStyle) {

    private val mAxisStepChooser = AxisStepChooser()

    private val mHandler = Handler(Looper.getMainLooper())
    private val mRedrawTask = {
        invalidate()
    }

    private val mAnimationDataSourceInfo = mutableMapOf<ChartDataSource, DataSourceAnimationContext>()

    /**
     * Keep own data sources list in order to work with them in lexicographically, e.g. when showing selection legend
     */
    private val mDataSources = mutableListOf<ChartDataSource>()
    private val mRoundedRectangleDrawer = RoundedRectangleDrawer()
    private val mYAxisGapStrategy = getYLabelGapStrategy {
        mYAxisLabelVerticalPadding
    }

    private val mModelListener = object : ChartModelListener {
        override fun onRangeChanged(anchor: Any) {
            if (anchor === getDataAnchor()) {
                refreshXAxisSetupIfNecessary()
                invalidate()
            }
        }

        override fun onDataSourceEnabled(dataSource: ChartDataSource) {
            startDataSourceFadeInAnimation(dataSource)
        }

        override fun onDataSourceDisabled(dataSource: ChartDataSource) {
            startDataSourceFadeOutAnimation(dataSource)
        }

        override fun onDataSourceAdded(dataSource: ChartDataSource) {
            refreshDataSources()
            startDataSourceFadeInAnimation(dataSource)
            invalidate()
        }

        override fun onDataSourceRemoved(dataSource: ChartDataSource) {
            refreshDataSources()
            stopDataSourceFadeAnimation(dataSource)
            invalidate()
        }

        override fun onActiveDataPointsLoaded(anchor: Any) {
            if (anchor === getDataAnchor()) {
                invalidate()
            }
        }

        override fun onSelectionChange() {
            invalidate()
        }
    }

    private lateinit var mChartConfig: ChartConfig
    private var mConfigApplied = false

    private lateinit var mChartModel: ChartModel

    private lateinit var mBackgroundPaint: Paint
    private lateinit var mGridPaint: Paint
    private lateinit var mPlotPaint: Paint

    private lateinit var mLegendValuePaint: Paint
    private lateinit var mLegendValueWidthMeasurer: TextSpaceMeasurer
    private var mLegendValueHeight: Int = 0

    private lateinit var mXAxisConfig: AxisConfig
    private lateinit var mXLabelPaint: Paint
    private lateinit var mXAxisLabelWidthMeasurer: TextSpaceMeasurer
    private lateinit var mCurrentXRange: Range
    private var mXAxisLabelPadding: Int = 0
    private var mXAxisLabelHeight: Int = 0
    private var mXAxisStep: Long = 0
    private var mXUnitVisualWidth: Float = 0.toFloat()
    private var mXVisualShift: Float = 0.toFloat()
    private var mPendingXRescale: Boolean = false

    private lateinit var mYAxisConfig: AxisConfig
    private lateinit var mYLabelPaint: Paint
    private lateinit var mCurrentYRange: Range
    private lateinit var mYAxisLabelWidthMeasurer: TextSpaceMeasurer
    private lateinit var mYAxisLabelHeightMeasurer: TextSpaceMeasurer
    private var mYAxisLabelHeight: Int = 0
    private var mYAxisLabelHorizontalPadding: Int = 0
    private var mYAxisLabelVerticalPadding: Int = 0
    private var mYAxisStep: Long = 0
    private var mMaxYLabelWidth: Int = 0

    private var mLastClickVisualX: Float = 0.toFloat()
    private var mLastClickVisualY: Float = 0.toFloat()

    private var mLegendRect: RectF? = null

    private var mYAnimationFirstDataValue: Long = 0
    private var mYAnimationAxisStep: Long = 0
    private var mYAnimationInitialUnitHeight: Float = 0.toFloat()
    private var mYAnimationCurrentUnitHeight: Float = 0.toFloat()
    private var mYAnimationFinalUnitHeight: Float = 0.toFloat()
    private var mYAnimationStartTimeMs: Long = 0
    private var mYAnimationOngoingLabelAlpha: Int = 0
    private var mYAnimationOngoingGridAlpha: Int = 0
    private var mYAnimationFinalLabelAlpha: Int = 0
    private var mYAnimationFinalGridAlpha: Int = 0

    init {
        setOnTouchListener { _, event ->
            mLastClickVisualX = event.x
            mLastClickVisualY = event.y
            false
        }
        setOnClickListener { _ ->
            val legendRect = mLegendRect
            if (legendRect != null && ::mChartModel.isInitialized && legendRect.contains(mLastClickVisualX, mLastClickVisualY)) {
                mChartModel.resetSelection()
                return@setOnClickListener
            }

            if (!::mChartConfig.isInitialized || !::mChartModel.isInitialized || !mChartConfig.selectionAllowed) {
                return@setOnClickListener
            }

            mChartModel.selectedX = visualXToDataX(mLastClickVisualX)
        }
    }

    fun getDataAnchor(): Any {
        return this
    }

    fun apply(config: ChartConfig) {
        mChartConfig = config
        mConfigApplied = false
        applyConfig()
    }

    private fun applyConfig() {
        if (mConfigApplied) {
            return
        }

        initBackground()
        initLegend()
        initGrid()
        initXAxis()
        initYAxis()
        initPlot()

        mConfigApplied = true
        invalidate()
    }

    private fun initBackground() {
        val paint = Paint()
        paint.color = mChartConfig.backgroundColor
        paint.style = Paint.Style.FILL
        mBackgroundPaint = paint
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

    private fun initGrid() {
        val paint = Paint()
        paint.color = mChartConfig.gridColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = mChartConfig.gridLineWidthInPixels.toFloat()
        mGridPaint = paint
    }

    private fun initXAxis() {
        mXAxisConfig = mChartConfig.xAxisConfig
        mXLabelPaint = getAxisLabelPaint(mChartConfig.xAxisConfig)
        mXAxisLabelWidthMeasurer = TextWidthMeasurer(mXLabelPaint)
        val fontMetrics = mXLabelPaint.getFontMetrics()
        mXAxisLabelHeight = (fontMetrics.descent - fontMetrics.ascent).toInt()
        mXAxisLabelPadding = mXAxisLabelHeight / 2
    }

    private fun initYAxis() {
        mYAxisConfig = mChartConfig.yAxisConfig
        mYLabelPaint = getAxisLabelPaint(mChartConfig.yAxisConfig)
        mYAxisLabelWidthMeasurer = TextWidthMeasurer(mYLabelPaint)
        mYAxisLabelHeightMeasurer = TextHeightMeasurer(mYLabelPaint)
        val fontMetrics = mYLabelPaint.getFontMetrics()
        mYAxisLabelHeight = (fontMetrics.descent - fontMetrics.ascent).toInt()
        mYAxisLabelVerticalPadding = mYAxisLabelHeight * 3 / 5
        mYAxisLabelHorizontalPadding = mYAxisLabelVerticalPadding
    }

    private fun getAxisLabelPaint(config: AxisConfig): Paint {
        val paint = Paint()
        paint.color = config.labelColor
        paint.typeface = Typeface.DEFAULT
        paint.textSize = config.labelFontSizeInPixels.toFloat()
        return paint
    }

    private fun initPlot() {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.strokeWidth = mChartConfig.plotLineWidthInPixels.toFloat()
        mPlotPaint = paint
    }

    fun apply(chartModel: ChartModel) {
        if (::mChartModel.isInitialized && chartModel !== mChartModel) {
            mChartModel.removeListener(mModelListener)
        }
        chartModel.addListener(mModelListener)
        mChartModel = chartModel
        refreshDataSources()
        invalidate()
    }

    private fun refreshDataSources() {
        mDataSources.clear()
        mDataSources.addAll(mChartModel.registeredDataSources)
        Collections.sort(mDataSources, COMPARATOR)
    }

    fun getChartBottom(): Int {
        var result = height
        if (mXAxisConfig.drawAxis || mXAxisConfig.drawLabels) {
            result -= mXAxisLabelHeight + mXAxisLabelPadding
        }
        return result
    }

    fun getChartLeft(): Int {
        return 0
    }

    fun getChartRight(): Int {
        return width
    }

    fun getChartTop(): Int {
        return 0
    }

    fun getChartHeight(): Int {
        return height
    }

    fun getChartWidth(): Int {
        return getChartRight() - getChartLeft()
    }

    fun getXVisualShift(): Float {
        return mXVisualShift
    }

    fun getPlotLeft(): Int {
        return getChartLeft() + mMaxYLabelWidth + mYAxisLabelHorizontalPadding
    }

    fun getPlotRight(): Int {
        return getChartRight()
    }

    fun dataPointToVisualPoint(point: DataPoint): VisualPoint {
        var yRange: Range? = mCurrentYRange
        if (yRange == null) {
            yRange = getCurrentYRange()
        }

        val yUnitHeight: Float
        if (isYRescaleAnimationInProgress()) {
            yUnitHeight = mYAnimationCurrentUnitHeight
        } else {
            yUnitHeight = getChartHeight() / (yRange.size - 1f)
        }

        val y = getChartBottom() - (point.y - yRange.start) * yUnitHeight

        return VisualPoint(dataXToVisualX(point.x), y)
    }

    fun dataXToVisualX(dataX: Long): Float {
        refreshXAxisSetupIfNecessary()
        val xRange = mCurrentXRange
        return getChartLeft() + mXVisualShift + (dataX - xRange.start) * mXUnitVisualWidth
    }

    fun visualXToDataX(visualX: Float): Long {
        refreshXAxisSetupIfNecessary()
        val xRange = mCurrentXRange
        return ((visualX - getChartLeft().toFloat() - mXVisualShift) / mXUnitVisualWidth + xRange.start).toLong()
    }

    fun scrollHorizontally(deltaVisualX: Float) {
        val effectiveDeltaVisualX = deltaVisualX + mXVisualShift
        refreshXAxisSetupIfNecessary()
        val currentXRange = mCurrentXRange
        val deltaDataX = (-effectiveDeltaVisualX / mXUnitVisualWidth).toLong()

//        var minDataX = java.lang.Long.MAX_VALUE
//        var maxDataX = java.lang.Long.MIN_VALUE
//        for (activeDataSource in mDataSources) {
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

        mXVisualShift = effectiveDeltaVisualX % mXUnitVisualWidth
        if (deltaDataX != 0L) {
            mChartModel.setActiveRange(currentXRange.shift(deltaDataX), getDataAnchor())
        }
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val edge = View.MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(edge, edge)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!::mChartConfig.isInitialized) {
            return
        }
        applyConfig()
        mayBeTickAnimations()
        drawBackground(canvas)
        drawGrid(canvas)
        drawPlots(canvas)
        drawSelection(canvas)
    }

    private fun drawBackground(canvas: Canvas) {
        if (mChartConfig.drawBackground) {
            mBackgroundPaint.color = mChartConfig.backgroundColor
            canvas.drawPaint(mBackgroundPaint)
        }
    }

    private fun drawGrid(canvas: Canvas) {
        drawXAxis(canvas)
        drawYAxis(canvas)
    }

    private fun drawXAxis(canvas: Canvas) {
        refreshXAxisSetupIfNecessary()
        drawXAxisLine(canvas)
        drawXAxisLabels(canvas)
    }

    /**
     * Is assumed to be called when [.mXAxisLabelWidthMeasurer] is initialized and [.getWidth] is known.
     */
    private fun refreshXAxisSetupIfNecessary() {
        var rescale = false
        val activeRange = mChartModel.getActiveRange(getDataAnchor())
        if (mPendingXRescale
            || !::mCurrentXRange.isInitialized
            || activeRange.size != mCurrentXRange.size
        ) {
            rescale = true
        }

        mCurrentXRange = activeRange

        if (rescale) {
            if (!::mXAxisConfig.isInitialized || getChartWidth() <= 0) {
                mPendingXRescale = true
            } else {
                mPendingXRescale = false
                val labelTextStrategy = mXAxisConfig.labelTextStrategy
                mXAxisStep = mAxisStepChooser.choose(labelTextStrategy,
                                                     X_AXIS_LABEL_GAP_STRATEGY,
                                                     mCurrentXRange,
                                                     width,
                                                     mXAxisLabelWidthMeasurer)
                mXUnitVisualWidth = getChartWidth() / (mCurrentXRange.size - 1f)
                mXVisualShift = 0f
            }
        }
    }

    private fun drawXAxisLine(canvas: Canvas) {
        if (!mXAxisConfig.drawAxis) {
            return
        }
        val y = getChartBottom()
        val clipBounds = canvas.clipBounds
        if (y <= clipBounds.bottom) {
            canvas.drawLine(getChartLeft().toFloat(),
                            y.toFloat(),
                            getChartRight().toFloat(),
                            y.toFloat(),
                            mGridPaint)
        }
    }

    private fun drawXAxisLabels(canvas: Canvas) {
        if (!mXAxisConfig.drawLabels) {
            return
        }
        val clipBounds = canvas.clipBounds
        val height = height
        val minY = height - mXAxisLabelHeight
        if (clipBounds.bottom <= minY) {
            return
        }

        val labelTextStrategy = mXAxisConfig.labelTextStrategy
        refreshXAxisSetupIfNecessary()
        val range = mCurrentXRange
        val y = height
        var value = range.findFirstStepValue(mXAxisStep)
        while (range.contains(value)) {
            val xAnchor = getChartLeft() + mXVisualShift + mXUnitVisualWidth * (value - range.start)
            val label = labelTextStrategy.getLabel(value, mXAxisStep)
            canvas.drawText(label.data, 0, label.length, xAnchor, y.toFloat(), mXLabelPaint)
            value += mXAxisStep
        }
    }

    private fun drawYAxis(canvas: Canvas) {
        refreshYAxisSetupIfNecessary()
        drawYGrid(canvas)
    }

    private fun refreshYAxisSetupIfNecessary() {
        val previousRange = if (::mCurrentYRange.isInitialized) mCurrentYRange else null
        val currentYRange = getCurrentYRange()
        if (currentYRange === EMPTY_RANGE) {
            mCurrentYRange = currentYRange
            return
        }

        var rescale = false
        if (!::mCurrentYRange.isInitialized || mCurrentYRange.size != currentYRange.size) {
            rescale = true
        }

        mCurrentYRange = currentYRange

        if (rescale) {
            val labelTextStrategy = mYAxisConfig.labelTextStrategy
            val nonPaddedRange = getCurrentYRange(false)
            val newYAxisStep = mAxisStepChooser.choose(labelTextStrategy,
                                                       mYAxisGapStrategy,
                                                       nonPaddedRange,
                                                       getChartHeight(),
                                                       mYAxisLabelHeightMeasurer)

            val currentRange = mayBeExpandYRange(nonPaddedRange)
            val unitHeight = getChartHeight() / (currentRange.size - 1f)
            val previousStep = mYAxisStep

            mYAxisStep = newYAxisStep
            mCurrentYRange = mayBeExpandYRange(nonPaddedRange)

            if (isYRescaleAnimationInProgress()) {
                if (unitHeight != mYAnimationFinalUnitHeight) {
                    startYRescaleAnimation(previousRange, previousStep, mCurrentYRange)
                }
            } else if (previousRange != null && previousStep > 0 && !previousRange.equals(currentRange)) {
                startYRescaleAnimation(previousRange, previousStep, mCurrentYRange)
            }
        }
    }

    private fun getCurrentYRange(): Range {
        return getCurrentYRange(true)
    }

    private fun getCurrentYRange(padByStepSize: Boolean): Range {
        var min = Long.MAX_VALUE
        var max = Long.MIN_VALUE
        for (dataSource in mDataSources) {
            if (!mChartModel.isActive(dataSource)) {
                continue
            }

            if (mChartModel.arePointsForActiveRangeLoaded(dataSource, getDataAnchor())) {
                val interval = mChartModel.getCurrentRangePoints(dataSource, getDataAnchor())
                for (point in interval) {
                    if (min > point.y) {
                        min = point.y
                    }
                    if (max < point.y) {
                        max = point.y
                    }
                }
            }
        }

        if (min > max) {
            return EMPTY_RANGE
        }

        val range = Range(min, max)
        return if (padByStepSize) {
            mayBeExpandYRange(range)
        } else {
            range
        }
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
        return if (mYAxisStep <= 0 || !mYAxisConfig.drawAxis) {
            range
        } else range.padBy(mYAxisStep)
    }

    private fun drawYGrid(canvas: Canvas) {
        if (!mYAxisConfig.drawAxis || mCurrentYRange === EMPTY_RANGE) {
            mMaxYLabelWidth = 0
            return
        }

        mYLabelPaint.setColor(mYAxisConfig.labelColor)
        mYLabelPaint.setTypeface(Typeface.DEFAULT)

        if (isYRescaleAnimationInProgress()) {
            drawYGrid(canvas,
                      mYAnimationFirstDataValue,
                      mYAnimationAxisStep,
                      mYAnimationCurrentUnitHeight,
                      mYAnimationOngoingGridAlpha,
                      mYAnimationOngoingLabelAlpha)
            drawYGrid(canvas,
                      mCurrentYRange.findFirstStepValue(mYAxisStep),
                      mYAxisStep,
                      getChartHeight() / (mCurrentYRange.size - 1f),
                      mYAnimationFinalGridAlpha,
                      mYAnimationFinalLabelAlpha)
        } else {
            drawYGrid(canvas,
                      mCurrentYRange.findFirstStepValue(mYAxisStep),
                      mYAxisStep,
                      getChartHeight() / (mCurrentYRange.size - 1f),
                      255,
                      255)
        }
    }

    private fun drawYGrid(canvas: Canvas,
                          firstValue: Long,
                          dataStep: Long,
                          unitHeight: Float,
                          gridAlpha: Int,
                          labelAlpha: Int) {
        mGridPaint.setAlpha(gridAlpha)
        mYLabelPaint.setAlpha(labelAlpha)
        var value = firstValue
        while (true) {
            var y = (getChartBottom() - unitHeight * (value - mCurrentYRange.start)).toInt()
            if (y <= getChartTop()) {
                break
            }
            canvas.drawLine(getChartLeft().toFloat(), y.toFloat(), getChartRight().toFloat(), y.toFloat(), mGridPaint)
            val label = mYAxisConfig.labelTextStrategy.getLabel(value, mYAxisStep)
            y -= mYAxisLabelVerticalPadding
            if (y - mYAxisLabelHeight <= getChartTop()) {
                break
            }
            if (mYAxisConfig.drawLabels) {
                canvas.drawText(label.data, 0, label.length, getChartLeft().toFloat(), y.toFloat(), mYLabelPaint)
            }
            mMaxYLabelWidth = Math.max(mMaxYLabelWidth, mYAxisLabelWidthMeasurer.measureVisualSpace(label))
            value += dataStep
        }
    }

    private fun drawPlots(canvas: Canvas) {
        for (dataSource in mDataSources) {
            drawPlot(dataSource, canvas)
        }
    }

    private fun drawPlot(dataSource: ChartDataSource, canvas: Canvas) {
        mPlotPaint.setColor(dataSource.color)
        val animationContext = mAnimationDataSourceInfo[dataSource]
        if (animationContext == null) {
            if (!mChartModel.isActive(dataSource)) {
                return
            } else {
                mPlotPaint.setAlpha(255)
            }
        } else {
            mPlotPaint.setAlpha(animationContext.currentAlpha)
        }
        mPlotPaint.setStyle(Paint.Style.STROKE)
        val interval = mChartModel.getCurrentRangePoints(dataSource, getDataAnchor())

        val minX = getPlotLeft().toFloat()
        val maxX = getPlotRight().toFloat()

        val points = ArrayList<DataPoint>(interval.size + 2)
        val previous = mChartModel.getPreviousPointForActiveRange(dataSource, getDataAnchor())
        if (previous != null) {
            points.add(previous)
        }
        points.addAll(interval)
        val next = mChartModel.getNextPointForActiveRange(dataSource, getDataAnchor())
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
            val visualPoint = dataPointToVisualPoint(point)
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
                if (y > getChartBottom()) {
                    y = getChartBottom().toFloat()
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
                if (y > getChartBottom()) {
                    y = getChartBottom().toFloat()
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
                    if (endY > getChartBottom()) {
                        endY = getChartBottom().toFloat()
                        endX = formula.getX(endY)
                    }
                }
                path.lineTo(endX, endY)
            }

            previousVisualPoint = visualPoint
            i++
        }

        canvas.drawPath(path, mPlotPaint)
    }

    private fun calculateLineFormula(p1: VisualPoint, p2: VisualPoint): LineFormula {
        val a = (p1.y - p2.y) / (p1.x - p2.x)
        val b = p1.y - a * p1.x
        return LineFormula(a, b)
    }

    private fun drawSelection(canvas: Canvas) {
        if (!mChartConfig.drawSelection || !mChartModel.hasSelection) {
            return
        }

        mLegendRect = null

        val dataX = mChartModel.selectedX
        val visualX = dataXToVisualX(dataX)
        if (visualX < getChartLeft() || visualX > getChartRight()) {
            return
        }

        canvas.drawLine(visualX, getChartBottom().toFloat(), visualX, getChartTop().toFloat(), mGridPaint)

        val dataSource2yInfo = HashMap<ChartDataSource, ValueInfo>()
        for (dataSource in mDataSources) {
            if (!mChartModel.isActive(dataSource)) {
                continue
            }
            val points = mChartModel.getCurrentRangePoints(dataSource, getDataAnchor())
            if (points.isEmpty()) {
                continue
            }

            val dataY: Long

            val firstDataPoint = points.get(0)
            val lastDataPoint = points.get(points.size - 1)
            if (dataX < firstDataPoint.x) {
                val previousDataPoint =
                    mChartModel.getPreviousPointForActiveRange(dataSource, getDataAnchor()) ?: continue
                val formula = calculateLineFormula(VisualPoint(previousDataPoint.x.toFloat(),
                                                               previousDataPoint.y.toFloat()),
                                                   VisualPoint(firstDataPoint.x.toFloat(),
                                                               firstDataPoint.y.toFloat()))
                dataY = Math.round(formula.getY(dataX.toFloat()).toDouble())
            } else if (dataX > lastDataPoint.x) {
                val nextDataPoint = mChartModel.getNextPointForActiveRange(dataSource, getDataAnchor()) ?: continue
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

            val visualPoint = dataPointToVisualPoint(DataPoint(dataX, dataY))
            dataSource2yInfo[dataSource] = ValueInfo(dataY, visualPoint.y)
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
        context.horizontalPadding = mYAxisLabelHeight * 7 / 5

        val legendTitle = mXAxisConfig.labelTextStrategy.getLabel(mChartModel.selectedX, mXAxisStep)
        val legendTitleWidth = mYAxisLabelWidthMeasurer.measureVisualSpace(legendTitle)
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
        for (dataSource in mDataSources) {
            if (!mChartModel.isActive(dataSource)) {
                continue
            }
            val valueInfo = context.dataSource2yInfo[dataSource] ?: continue
            val dataY = valueInfo.dataValue
            val value = if (minifyValues) {
                mYAxisConfig.labelTextStrategy.getMinifiedLabel(dataY, mYAxisStep)
            } else {
                mYAxisConfig.labelTextStrategy.getLabel(dataY, mYAxisStep)
            }
            val valueWidth = mLegendValueWidthMeasurer.measureVisualSpace(value)
            val legendWidth = mYAxisLabelWidthMeasurer.measureVisualSpace(dataSource.legend)
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
        context.verticalPadding = mYAxisLabelHeight
        context.titleYShift = context.verticalPadding + mYAxisLabelHeight
        context.valueYShift = context.titleYShift + mLegendValueHeight * 2
        context.legendYShift = context.valueYShift + mYAxisLabelHeight * 2 / 3 + mYAxisLabelHeight
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

        if (context.legendHeight + context.verticalPadding * 2 <= getChartBottom() - topLimit) {
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
        val xToShiftLeft = left + width - getChartRight()
        return if (xToShiftLeft <= 0) {
            left
        } else Math.max(getChartLeft().toFloat(), left - xToShiftLeft)
    }

    private fun doDrawLegend(canvas: Canvas, context: LegendDrawContext) {
        val rect = RectF(context.leftOnChart,
                         context.topOnChart,
                         context.leftOnChart + context.legendWidth,
                         context.topOnChart + context.legendHeight)
        mLegendRect = rect

        mBackgroundPaint.color = mChartConfig.legendBackgroundColor
        mRoundedRectangleDrawer.draw(rect,
                                     mGridPaint,
                                     mBackgroundPaint,
                                     LeonardoUtil.DEFAULT_CORNER_RADIUS,
                                     canvas)
        drawLegendTitle(canvas, context)
        drawLegendValues(canvas, context)
    }

    private fun drawLegendTitle(canvas: Canvas, context: LegendDrawContext) {
        mYLabelPaint.typeface = TYPEFACE_BOLD
        mYLabelPaint.color = mChartConfig.legendTextTitleColor
        val xText = mXAxisConfig.labelTextStrategy.getLabel(mChartModel.selectedX, mXAxisStep)
        canvas.drawText(xText.data,
                        0,
                        xText.length,
                        context.leftOnChart + context.horizontalPadding,
                        context.topOnChart + context.titleYShift,
                        mYLabelPaint)
    }

    private fun drawLegendValues(canvas: Canvas, context: LegendDrawContext) {
        var x = context.leftOnChart + context.horizontalPadding
        val valueY = context.topOnChart + context.valueYShift
        val legendY = context.topOnChart + context.legendYShift
        mYLabelPaint.typeface = Typeface.DEFAULT
        for (dataSource in mDataSources) {
            if (!mChartModel.isActive(dataSource)) {
                continue
            }
            val valueInfo = context.dataSource2yInfo[dataSource] ?: continue
            val value: TextWrapper
            if (context.minifiedValues) {
                value = mYAxisConfig.labelTextStrategy.getMinifiedLabel(valueInfo.dataValue, mYAxisStep)
            } else {
                value = mYAxisConfig.labelTextStrategy.getLabel(valueInfo.dataValue, mYAxisStep)
            }
            mLegendValuePaint.color = dataSource.color
            canvas.drawText(value.data, 0, value.length, x, valueY, mLegendValuePaint)

            mYLabelPaint.color = dataSource.color
            canvas.drawText(dataSource.legend, x, legendY, mYLabelPaint)

            x += Math.max(mLegendValueWidthMeasurer.measureVisualSpace(value),
                          mYAxisLabelWidthMeasurer.measureVisualSpace(dataSource.legend))
            x += context.horizontalPadding.toFloat()
        }
    }

    private fun mayBeTickAnimations() {
        val hasActiveAnimation = tickYRescaleAnimation() or tickDataSourceFadeAnimation()
        if (hasActiveAnimation) {
            mHandler.postDelayed(mRedrawTask, LeonardoUtil.ANIMATION_TICK_FREQUENCY_MILLIS)
        }
    }

    private fun isYRescaleAnimationInProgress(): Boolean {
        return mYAnimationStartTimeMs > 0 && mYAnimationFirstDataValue != java.lang.Long.MIN_VALUE
    }

    private fun startYRescaleAnimation(rangeFrom: Range?, stepFrom: Long, rangeTo: Range) {
        if (!mChartConfig.animationEnabled) {
            return
        }

        mYAnimationStartTimeMs = System.currentTimeMillis()
        mYAnimationFirstDataValue = rangeFrom!!.findFirstStepValue(stepFrom)
        mYAnimationAxisStep = stepFrom
        mYAnimationInitialUnitHeight = getChartHeight() / (rangeFrom.size - 1f)
        mYAnimationCurrentUnitHeight = mYAnimationInitialUnitHeight
        mYAnimationFinalUnitHeight = getChartHeight() / (rangeTo.size - 1f)
        mYAnimationOngoingGridAlpha = 255
        mYAnimationOngoingLabelAlpha = 255
        mYAnimationFinalLabelAlpha = 0
        mYAnimationFinalGridAlpha = 0

        invalidate()
    }

    private fun tickYRescaleAnimation(): Boolean {
        if (!isYRescaleAnimationInProgress()) {
            return false
        }

        val animationEndTime = mYAnimationStartTimeMs + ANIMATION_DURATION_MILLIS
        val now = System.currentTimeMillis()
        if (now >= animationEndTime) {
            stopYRescaleAnimation()
            return false
        }

        val unitHeightDelta = mYAnimationFinalUnitHeight - mYAnimationInitialUnitHeight
        val tickUnitHeightChange = (now - mYAnimationStartTimeMs) * unitHeightDelta / ANIMATION_DURATION_MILLIS
        mYAnimationCurrentUnitHeight = mYAnimationInitialUnitHeight + tickUnitHeightChange

        updateAnimationLabelAlpha()
        updateAnimationGridAlpha()

        return true
    }

    private fun updateAnimationLabelAlpha() {
        val ongoingLabelFadeDuration = ANIMATION_DURATION_MILLIS * 2 / 3
        val ongoingLabelFadeStartTime = mYAnimationStartTimeMs + ANIMATION_DURATION_MILLIS - ongoingLabelFadeDuration
        val now = System.currentTimeMillis()
        if (now <= ongoingLabelFadeStartTime) {
            mYAnimationOngoingLabelAlpha = 255
            mYAnimationFinalLabelAlpha = 0
            return
        }
        mYAnimationFinalLabelAlpha = ((now - ongoingLabelFadeStartTime) * 255 / ongoingLabelFadeDuration).toInt()
        mYAnimationOngoingLabelAlpha = 255 - mYAnimationFinalLabelAlpha
    }

    private fun updateAnimationGridAlpha() {
        val switchTime = mYAnimationStartTimeMs + ANIMATION_DURATION_MILLIS * 2 / 3
        if (System.currentTimeMillis() >= switchTime) {
            mYAnimationOngoingGridAlpha = 255
            mYAnimationFinalGridAlpha = 0
        } else {
            mYAnimationOngoingGridAlpha = 0
            mYAnimationFinalGridAlpha = 255
        }
    }

    private fun stopYRescaleAnimation() {
        mYAnimationStartTimeMs = -1
        mYAnimationFirstDataValue = -1
        mYAnimationAxisStep = -1
        mYAnimationInitialUnitHeight = -1f
        mYAnimationCurrentUnitHeight = -1f
        mYAnimationFinalUnitHeight = -1f
        invalidate()
    }

    private fun startDataSourceFadeInAnimation(dataSource: ChartDataSource) {
        if (!mChartConfig.animationEnabled) {
            return
        }
        mAnimationDataSourceInfo[dataSource] = DataSourceAnimationContext(0, 255)
        invalidate()
    }

    private fun startDataSourceFadeOutAnimation(dataSource: ChartDataSource) {
        if (!mChartConfig.animationEnabled) {
            return
        }
        mAnimationDataSourceInfo[dataSource] = DataSourceAnimationContext(255, 0)
        invalidate()
    }

    private fun tickDataSourceFadeAnimation(): Boolean {
        val toRemove = HashSet<ChartDataSource>()
        val now = System.currentTimeMillis()
        for ((key, context) in mAnimationDataSourceInfo) {
            if (now >= context.startTimeMs + ANIMATION_DURATION_MILLIS) {
                toRemove.add(key)
                break
            }

            val elapsedTimeMs = now - context.startTimeMs
            val totalAlphaDelta = context.finalAlpha - context.initialAlpha
            val currentAlphaDelta = elapsedTimeMs * totalAlphaDelta / ANIMATION_DURATION_MILLIS
            context.currentAlpha = (context.initialAlpha + currentAlphaDelta).toInt()
            if (totalAlphaDelta > 0 && context.currentAlpha >= context.finalAlpha || totalAlphaDelta < 0 && context.currentAlpha <= context.finalAlpha) {
                toRemove.add(key)
            }
        }
        for (dataSource in toRemove) {
            stopDataSourceFadeAnimation(dataSource)
        }
        return !mAnimationDataSourceInfo.isEmpty()
    }

    private fun stopDataSourceFadeAnimation(dataSource: ChartDataSource) {
        mAnimationDataSourceInfo.remove(dataSource)
    }

    companion object {

        private val TYPEFACE_BOLD = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

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

    private class DataSourceAnimationContext(val initialAlpha: Int, val finalAlpha: Int) {

        val startTimeMs = System.currentTimeMillis()

        var currentAlpha: Int = 0

        init {
            currentAlpha = initialAlpha
        }
    }
}