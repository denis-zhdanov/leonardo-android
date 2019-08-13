package tech.harmonysoft.oss.leonardo.view.navigator

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.config.LeonardoConfigFactory
import tech.harmonysoft.oss.leonardo.model.config.chart.ChartConfig
import tech.harmonysoft.oss.leonardo.model.config.navigator.NavigatorConfig
import tech.harmonysoft.oss.leonardo.model.data.ChartDataSource
import tech.harmonysoft.oss.leonardo.model.runtime.ChartModel
import tech.harmonysoft.oss.leonardo.model.runtime.ChartModelListener
import tech.harmonysoft.oss.leonardo.view.navigator.NavigatorChartView.ActionType.*
import tech.harmonysoft.oss.leonardo.view.chart.ChartView
import kotlin.math.max

class NavigatorChartView @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null,
    defaultStyle: Int = 0
) : View(context, attributes, defaultStyle) {

    val dataAnchor: Any get() = _dataAnchor
    var scrollListener: ScrollListener? = null

    private val animator = ValueAnimator().apply {
        interpolator = LinearInterpolator()
        repeatCount = ValueAnimator.INFINITE
        setFloatValues(100f)
        addUpdateListener {
            tickAnimation(it.animatedValue as Float)
        }
    }

    private val view = ChartView(getContext())

    private val showCaseVisualRatio: Float
        get() {
            val navigatorDataRange = model.getActiveRange(_dataAnchor)
            val showCaseDataRange = model.getActiveRange(showCase.dataAnchor)
            return navigatorDataRange.size.toFloat() / showCaseDataRange.size.toFloat()
        }

    private val navigatorVisualShift: Float
        get() {
            var dx = 0f
            val showCaseVisualXShift = showCase.visualXShift
            if (showCaseVisualXShift != 0f) {
                dx = showCaseVisualXShift / showCaseVisualRatio
            }
            return -dx
        }

    private lateinit var config: NavigatorConfig
    private lateinit var model: ChartModel
    private lateinit var showCase: NavigatorShowcase
    private lateinit var _dataAnchor: Any

    private lateinit var inactiveBackgroundPaint: Paint
    private lateinit var activeBackgroundPaint: Paint
    private lateinit var activeBorderPaint: Paint

    private var currentAction: ActionType? = null
    private var previousActionVisualX = Float.NaN

    private var skipRangeEvent = false

    private var leftEdgeAutoExpand = false
    private var rightEdgeAutoExpand = false

    private val borderMarkerWidth: Float get() = config.activeBorderHorizontalWidthInPixels / 5f

    fun apply(navigatorConfig: NavigatorConfig, chartConfig: ChartConfig) {
        config = navigatorConfig
        activeBackgroundPaint = createPaint(chartConfig.backgroundColor)
        inactiveBackgroundPaint = createPaint(config.inactiveChartBackgroundColor)
        activeBorderPaint = createPaint(config.activeBorderColor)
        val plotLineWidth = max(1, chartConfig.plotLineWidthInPixels / 2)
        val axisConfig = LeonardoConfigFactory.newAxisConfigBuilder()
            .disableLabels()
            .disableAxis()
            .build()
        view.apply(LeonardoConfigFactory.newChartConfigBuilder()
                       .withConfig(chartConfig)
                       .withPlotLineWidthInPixels(plotLineWidth)
                       .disableSelection()
                       .disableBackground()
                       .disableAnimations()
                       .withXAxisConfig(axisConfig)
                       .withYAxisConfig(axisConfig)
                       .withNoChartsDrawableId(null)
                       .withContext(context)
                       .build())
        invalidate()
    }

    fun apply(model: ChartModel, dataAnchor: Any? = null) {
        this._dataAnchor = dataAnchor ?: this
        this.model = model
        view.apply(model, this._dataAnchor)
        setupListener(model)
        refreshMyRange()
        invalidate()
    }

    fun apply(showCase: NavigatorShowcase) {
        this.showCase = showCase
    }

    private fun setupListener(model: ChartModel) {
        model.addListener(object : ChartModelListener {
            override fun onRangeChanged(anchor: Any) {
                if (skipRangeEvent) {
                    return
                }
                if (anchor == _dataAnchor) {
                    refreshMyRange()
                } else if (anchor == showCase.dataAnchor) {
                    onShowCaseRangeChanged()
                }
            }

            override fun onDataSourceEnabled(dataSource: ChartDataSource) {
                invalidate()
            }

            override fun onDataSourceDisabled(dataSource: ChartDataSource) {
                invalidate()
            }

            override fun onDataSourceAdded(dataSource: ChartDataSource) {
                invalidate()
            }

            override fun onDataSourceRemoved(dataSource: ChartDataSource) {
                invalidate()
            }

            override fun onActiveDataPointsLoaded(anchor: Any) {
                invalidate()
            }

            override fun onPointsLoadingIterationEnd(dataSource: ChartDataSource) {
            }

            override fun onSelectionChange() {}
        })
    }

    private fun refreshMyRange() {
        val navigatorRange = model.getActiveRange(_dataAnchor)
        if (navigatorRange.empty) {
            return
        }
        model.setActiveRange(navigatorRange, view.dataAnchor)
        val dependencyPointsNumber = max(1, navigatorRange.size / 4)
        val dependencyRangeShift = (navigatorRange.size - dependencyPointsNumber) / 2
        val dependencyRangeStart = navigatorRange.start + dependencyRangeShift
        val dependencyRange = Range(dependencyRangeStart, dependencyRangeStart + dependencyPointsNumber)
        model.setActiveRange(dependencyRange, showCase.dataAnchor)
        invalidate()
    }

    private fun onShowCaseRangeChanged() {
        val showCaseRange = model.getActiveRange(showCase.dataAnchor)
        val navigatorRange = model.getActiveRange(view.dataAnchor)
        if (showCaseRange.start < navigatorRange.start) {
            setNewRange(navigatorRange.shift(showCaseRange.start - navigatorRange.start))
        } else if (showCaseRange.end > navigatorRange.end) {
            setNewRange(navigatorRange.shift(showCaseRange.end - navigatorRange.end))
        }
        invalidate()
    }

    private fun setNewRange(range: Range) {
        skipRangeEvent = true
        try {
            model.setActiveRange(range, dataAnchor)
            model.setActiveRange(range, view.dataAnchor)
        } finally {
            skipRangeEvent = false
        }
    }

    private fun mayBeInitialize() {
        mayBeSetInnerChartDimensions()
    }

    private fun mayBeSetInnerChartDimensions() {
        if (view.width != width) {
            view.left = 0
            view.top = 0
            view.right = width
            view.bottom = height
        }
    }

    private fun createPaint(color: Int): Paint {
        val paint = Paint()
        paint.color = color
        paint.style = Paint.Style.FILL
        return paint
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)

        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setMeasuredDimension(width, width / 9)
        } else {
            setMeasuredDimension(width, width / 15)
        }
    }

    @SuppressLint("WrongCall")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!::config.isInitialized) {
            return
        }

        mayBeInitialize()

        val dx = navigatorVisualShift
        drawInactiveBackground(canvas, dx)
        drawActiveBackground(canvas, dx)
        view.draw(canvas)
        drawActiveBorder(canvas, dx)
    }

    private fun drawInactiveBackground(canvas: Canvas, dx: Float) {
        val activeRange = model.getActiveRange(showCase.dataAnchor)
        if (activeRange.empty) {
            return
        }

        val wholeRange = model.getActiveRange(_dataAnchor)

        if (wholeRange.start < activeRange.start) {
            val activeXStart = view.dataMapper.dataXToVisualX(activeRange.start)
            canvas.drawRect(0f, 0f, activeXStart + dx, view.height.toFloat(), inactiveBackgroundPaint)
        }

        if (wholeRange.end > activeRange.end) {
            val activeXEnd = view.dataMapper.dataXToVisualX(activeRange.end)
            canvas.drawRect(activeXEnd + dx,
                            0f,
                            view.width.toFloat(),
                            view.height.toFloat(),
                            inactiveBackgroundPaint)
        }
    }

    private fun drawActiveBackground(canvas: Canvas, dx: Float) {
        val activeRange = model.getActiveRange(showCase.dataAnchor)
        if (activeRange.empty) {
            return
        }

        val activeXStart = view.dataMapper.dataXToVisualX(activeRange.start)
        val activeXEnd = view.dataMapper.dataXToVisualX(activeRange.end)
        canvas.drawRect(activeXStart + dx,
                        0f,
                        activeXEnd + dx,
                        view.height.toFloat(),
                        activeBackgroundPaint)
    }

    private fun drawActiveBorder(canvas: Canvas, dx: Float) {
        val activeRange = model.getActiveRange(showCase.dataAnchor)
        if (activeRange.empty) {
            return
        }

        val borderWidth = config.activeBorderHorizontalWidthInPixels

        // Left edge
        val activeXStart = view.dataMapper.dataXToVisualX(activeRange.start)
        canvas.drawRect(activeXStart + dx - borderWidth,
                        0f,
                        activeXStart,
                        view.height.toFloat(),
                        activeBorderPaint)
        drawBorderMarker(activeXStart - borderWidth + (borderWidth - borderMarkerWidth) / 2f, canvas)

        // Right edge
        val activeXEnd = view.dataMapper.dataXToVisualX(activeRange.end)
        canvas.drawRect(activeXEnd + dx,
                        0f,
                        activeXEnd + borderWidth + dx,
                        view.height.toFloat(),
                        activeBorderPaint)
        drawBorderMarker(activeXEnd + (borderWidth - borderMarkerWidth) / 2f, canvas)

        // Top edge
        canvas.drawRect(activeXStart + dx - borderWidth,
                        0f,
                        activeXEnd + dx + borderWidth,
                        config.activeBorderVerticalHeightInPixels.toFloat(),
                        activeBorderPaint)

        // Bottom edge
        canvas.drawRect(activeXStart + dx - borderWidth,
                        view.height.toFloat(),
                        activeXEnd + dx + borderWidth,
                        view.height - config.activeBorderVerticalHeightInPixels.toFloat(),
                        activeBorderPaint)
    }

    private fun drawBorderMarker(left: Float, canvas: Canvas) {
        val colorToRestore = activeBackgroundPaint.color
        activeBackgroundPaint.color = Color.WHITE

        val radius = borderMarkerWidth / 2f
        val height = height
        val markerHeight = height / 4f
        val markerTop = (height - markerHeight) / 2f
        val markerBottom = height - markerTop
        canvas.drawCircle(left + radius, markerTop + radius, radius, activeBackgroundPaint)
        canvas.drawCircle(left + radius, markerBottom - radius, radius, activeBackgroundPaint)
        canvas.drawRect(left, markerTop + radius, left + 2 * radius, markerBottom - radius, activeBackgroundPaint)

        activeBackgroundPaint.color = colorToRestore
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> startAction(event.x)
            MotionEvent.ACTION_MOVE -> move(event.x)
            MotionEvent.ACTION_UP -> release(event.x)
        }
        return true
    }

    private fun startAction(visualX: Float) {
        currentAction = getActionType(visualX)
        if (currentAction != null) {
            previousActionVisualX = visualX
            scrollListener?.onStarted()
            invalidate()
        }
    }

    fun move(visualX: Float) {
        val previousVisualX = previousActionVisualX
        previousActionVisualX = visualX

        if (currentAction == null || previousVisualX.isNaN()) {
            return
        }

        val navigatorVisualDeltaX = previousVisualX - visualX
        if (navigatorVisualDeltaX == 0f) {
            return
        }

        if (currentAction == MOVE_COMPLETE_ACTIVE_INTERVAL) {
            moveCompleteInterval(navigatorVisualDeltaX)
        } else if (currentAction == MOVE_ACTIVE_INTERVAL_START) {
            val showCaseDataRange = model.getActiveRange(showCase.dataAnchor)
            val endRangeVisualX = view.dataMapper.dataXToVisualX(showCaseDataRange.end)
            if (endRangeVisualX - visualX < MIN_WIDTH_IN_PIXELS) {
                // Don't allow selector to become too narrow
                return
            }
            val newStartDataX = view.dataMapper.visualXToDataX(max(visualX, 0f))
            if (newStartDataX != showCaseDataRange.start) {
                model.setActiveRange(Range(newStartDataX, showCaseDataRange.end), showCase.dataAnchor)
            }
        } else {
            val showCaseDataRange = model.getActiveRange(showCase.dataAnchor)
            val myDataRange = model.getActiveRange(_dataAnchor)
            val startRangeVisualX = view.dataMapper.dataXToVisualX(showCaseDataRange.start)
            if (visualX - startRangeVisualX < MIN_WIDTH_IN_PIXELS) {
                // Don't allow selector to become too narrow
                return
            }
            val newEndDataX = view.dataMapper.visualXToDataX(max(visualX, 0f))
            if (newEndDataX <= myDataRange.end && newEndDataX != showCaseDataRange.end) {
                model.setActiveRange(Range(showCaseDataRange.start, newEndDataX), showCase.dataAnchor)
            }
        }
    }

    private fun moveCompleteInterval(deltaVisualX: Float) {
        val showCaseDataRange = model.getActiveRange(showCase.dataAnchor)

        if (deltaVisualX > 0) {
            val showCaseStartVisualX = view.dataMapper.dataXToVisualX(showCaseDataRange.start)
            if (showCaseStartVisualX - deltaVisualX < navigatorVisualShift) {
                if (leftEdgeAutoExpand) {
                    return
                }
                mayBeStopAutoExpand()
                leftEdgeAutoExpand = true
                startAnimation()
                return
            } else {
                if (rightEdgeAutoExpand) {
                    mayBeStopAutoExpand()
                }
            }
        } else if (deltaVisualX < 0) {
            val showCaseEndVisualX = view.dataMapper.dataXToVisualX(showCaseDataRange.end)
            if (showCaseEndVisualX - deltaVisualX > view.width) {
                if (rightEdgeAutoExpand) {
                    return
                }
                mayBeStopAutoExpand()
                rightEdgeAutoExpand = true
                startAnimation()
                return
            } else if (leftEdgeAutoExpand) {
                mayBeStopAutoExpand()
            }
        }

        showCase.scrollHorizontally(-deltaVisualX * showCaseVisualRatio)
        invalidate()
    }

    private fun startAnimation() {
        if (animator.isRunning) {
            animator.resume()
        } else {
            animator.start()
        }
    }

    private fun mayBeStopAutoExpand() {
        leftEdgeAutoExpand = false
        rightEdgeAutoExpand = false
        if (animator.isRunning) {
            animator.pause()
        }
    }

    private fun tickAnimation(elapsed: Float) {
        if (elapsed <= 0) {
            return
        }
        val action = currentAction ?: return

        if (action == MOVE_COMPLETE_ACTIVE_INTERVAL) {
            val absToScroll = (width / 50) * elapsed / 100f
            val direction = if (leftEdgeAutoExpand) -1 else 1
            val toScroll = absToScroll * direction * showCaseVisualRatio
            showCase.scrollHorizontally(toScroll)
            return
        }
    }

    private fun release(visualX: Float) {
        move(visualX)
        currentAction = null
        previousActionVisualX = Float.NaN
        mayBeStopAutoExpand()
        invalidate()
    }

    private fun getActionType(x: Float): ActionType? {
        val activeRange = model.getActiveRange(showCase.dataAnchor)
        if (activeRange.empty) {
            return null
        }

        val dx = navigatorVisualShift
        val startX = view.dataMapper.dataXToVisualX(activeRange.start) + dx
        val endX = view.dataMapper.dataXToVisualX(activeRange.end) + dx
        val borderWidth = config.activeBorderHorizontalWidthInPixels


        if (x < startX - borderWidth || x > endX + borderWidth) {
            return null
        }

        return when {
            x <= startX -> MOVE_ACTIVE_INTERVAL_START
            x >= endX -> MOVE_ACTIVE_INTERVAL_END
            else -> MOVE_COMPLETE_ACTIVE_INTERVAL
        }
    }

    fun stopAction() {
        currentAction = null
        previousActionVisualX = java.lang.Float.NaN
        scrollListener?.onStopped()
        invalidate()
    }

    companion object {
        private const val MIN_WIDTH_IN_PIXELS: Long = 40
    }

    private enum class ActionType {
        MOVE_COMPLETE_ACTIVE_INTERVAL, MOVE_ACTIVE_INTERVAL_START, MOVE_ACTIVE_INTERVAL_END
    }
}