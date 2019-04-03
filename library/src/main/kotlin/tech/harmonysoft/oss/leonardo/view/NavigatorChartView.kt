package tech.harmonysoft.oss.leonardo.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.config.LeonardoConfigFactory
import tech.harmonysoft.oss.leonardo.model.config.chart.ChartConfig
import tech.harmonysoft.oss.leonardo.model.config.navigator.NavigatorConfig
import tech.harmonysoft.oss.leonardo.model.data.ChartDataSource
import tech.harmonysoft.oss.leonardo.model.runtime.ChartModel
import tech.harmonysoft.oss.leonardo.model.runtime.ChartModelListener
import tech.harmonysoft.oss.leonardo.view.NavigatorChartView.ActionType.*
import tech.harmonysoft.oss.leonardo.view.chart.ChartView

class NavigatorChartView @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null,
    defaultStyle: Int = 0
) : View(context, attributes, defaultStyle) {

    private lateinit var config: NavigatorConfig
    private lateinit var model: ChartModel
    private val view = ChartView(getContext())
    private lateinit var showCase: NavigatorShowcase

    private lateinit var inactiveBackgroundPaint: Paint
    private lateinit var activeBackgroundPaint: Paint
    private lateinit var activeBorderPaint: Paint

    private var currentAction: ActionType? = null
    private var previousActionVisualX = Float.NaN

    fun apply(navigatorConfig: NavigatorConfig, chartConfig: ChartConfig) {
        config = navigatorConfig
        activeBackgroundPaint = createPaint(chartConfig.backgroundColor)
        inactiveBackgroundPaint = createPaint(config.inactiveChartBackgroundColor)
        activeBorderPaint = createPaint(config.activeBorderColor)
        val plotLineWidth = Math.max(1, chartConfig.plotLineWidthInPixels / 2)
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
                        .withContext(context)
                        .build())
        invalidate()
    }

    fun apply(model: ChartModel) {
        this.model = model
        view.apply(model)
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
                if (anchor === getModelAnchor()) {
                    refreshMyRange()
                } else if (anchor === showCase.dataAnchor) {
                    invalidate()
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

            override fun onSelectionChange() {}
        })
    }

    private fun refreshMyRange() {
        val navigatorRange = model.getActiveRange(getModelAnchor())
        model.setActiveRange(navigatorRange, view)
        val dependencyPointsNumber = Math.max(1, (navigatorRange.size + 1) / 4)
        val dependencyRangeShift = (navigatorRange.size + 1 - dependencyPointsNumber) / 2
        val dependencyRangeStart = navigatorRange.start + 1 + dependencyRangeShift
        val dependencyRange = Range(dependencyRangeStart,
                                    dependencyRangeStart + dependencyPointsNumber)
        model.setActiveRange(dependencyRange, showCase.dataAnchor)
        invalidate()
    }

    private fun getModelAnchor(): Any {
        return this
    }

    private fun mayBeInitialize() {
        mayBeSetInnerChartDimensions()
    }

    private fun mayBeSetInnerChartDimensions() {
        if (view.width + 2 != width) {
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
        val width = View.MeasureSpec.getSize(widthMeasureSpec)
        val chartHeight = getChartHeight(width)

        setMeasuredDimension(width, chartHeight)
    }

    private fun getChartHeight(width: Int): Int {
        return width / 9
    }

    @SuppressLint("WrongCall")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!::config.isInitialized) {
            return
        }

        mayBeInitialize()

        val dx = getNavigatorVisualShift()
        drawInactiveBackground(canvas, dx)
        drawActiveBackground(canvas, dx)
        drawActiveBorder(canvas, dx)
        view.draw(canvas)
    }

    private fun drawInactiveBackground(canvas: Canvas, dx: Float) {
        val activeRange = model.getActiveRange(showCase.dataAnchor)
        if (activeRange.empty) {
            return
        }

        val wholeRange = model.getActiveRange(view)

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

        // Left edge
        val activeXStart = view.dataMapper.dataXToVisualX(activeRange.start)
        canvas.drawRect(activeXStart + dx,
                        0f,
                        activeXStart + config.activeBorderHorizontalWidthInPixels,
                        view.height.toFloat(),
                        activeBorderPaint)

        // Right edge
        val activeXEnd = view.dataMapper.dataXToVisualX(activeRange.end)
        canvas.drawRect(activeXEnd + -config.activeBorderHorizontalWidthInPixels + dx,
                        0f,
                        activeXEnd + dx,
                        view.height.toFloat(),
                        activeBorderPaint)

        // Top edge
        canvas.drawRect(activeXStart + dx,
                        0f,
                        activeXEnd + dx,
                        config.activeBorderVerticalHeightInPixels.toFloat(),
                        activeBorderPaint)

        // Bottom edge
        canvas.drawRect(activeXStart + dx,
                        view.height.toFloat(),
                        activeXEnd + dx,
                        view.height - config.activeBorderVerticalHeightInPixels.toFloat(),
                        activeBorderPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        when (action) {
            MotionEvent.ACTION_DOWN -> startAction(event.x)
            MotionEvent.ACTION_MOVE -> move(event.x)
            MotionEvent.ACTION_UP   -> release(event.x)
        }
        return true
    }

    private fun startAction(visualX: Float) {
        currentAction = getActionType(visualX)
        if (currentAction != null) {
            previousActionVisualX = visualX
            invalidate()
        }
    }

    private fun move(visualX: Float) {
        val previousVisualX = previousActionVisualX
        previousActionVisualX = visualX

        if (currentAction == null || previousVisualX.isNaN()) {
            return
        }

        val navigatorVisualDeltaX = previousVisualX - visualX
        if (navigatorVisualDeltaX == 0f) {
            return
        }

        val ratio = getShowCaseVisualRatio()

        if (currentAction == MOVE_COMPLETE_ACTIVE_INTERVAL) {
            showCase.scrollHorizontally(navigatorVisualDeltaX * ratio)
            invalidate()
        } else if (currentAction == MOVE_ACTIVE_INTERVAL_START) {
            val showCaseDataRange = model.getActiveRange(showCase.dataAnchor)
            val endRangeVisualX = view.dataMapper.dataXToVisualX(showCaseDataRange.end)
            if (endRangeVisualX - visualX < MIN_WIDTH_IN_PIXELS) {
                // Don't allow selector to become too narrow
                return
            }
            val newStartDataX = view.dataMapper.visualXToDataX(Math.max(visualX, 0f))
            if (newStartDataX != showCaseDataRange.start) {
                model.setActiveRange(Range(newStartDataX, showCaseDataRange.end), showCase.dataAnchor)
            }
        } else {
            val showCaseDataRange = model.getActiveRange(showCase.dataAnchor)
            val myDataRange = model.getActiveRange(getModelAnchor())
            val startRangeVisualX = view.dataMapper.dataXToVisualX(showCaseDataRange.start)
            if (visualX - startRangeVisualX < MIN_WIDTH_IN_PIXELS) {
                // Don't allow selector to become too narrow
                return
            }
            val newEndDataX = view.dataMapper.visualXToDataX(Math.max(visualX, 0f))
            if (newEndDataX <= myDataRange.end && newEndDataX != showCaseDataRange.end) {
                model.setActiveRange(Range(showCaseDataRange.start, newEndDataX), showCase.dataAnchor)
            }
        }
    }

    private fun release(visualX: Float) {
        move(visualX)
        currentAction = null
        previousActionVisualX = Float.NaN
        invalidate()
    }

    private fun getActionType(x: Float): ActionType? {
        val activeRange = model.getActiveRange(showCase.dataAnchor)
        if (activeRange.empty) {
            return null
        }

        val dx = getNavigatorVisualShift()
        val startX = view.dataMapper.dataXToVisualX(activeRange.start) + dx
        val endX = view.dataMapper.dataXToVisualX(activeRange.end) + dx

        if (x + CLICK_RECOGNITION_ERROR_IN_PIXELS < startX || x - CLICK_RECOGNITION_ERROR_IN_PIXELS > endX) {
            return null
        }

        return when {
            Math.abs(startX - x) < CLICK_RECOGNITION_ERROR_IN_PIXELS -> MOVE_ACTIVE_INTERVAL_START
            Math.abs(endX - x) < CLICK_RECOGNITION_ERROR_IN_PIXELS   -> MOVE_ACTIVE_INTERVAL_END
            else                                                     -> MOVE_COMPLETE_ACTIVE_INTERVAL
        }
    }

    private fun getShowCaseVisualRatio(): Float {
        val navigatorDataRange = model.getActiveRange(getModelAnchor())
        val showCaseDataRange = model.getActiveRange(showCase.dataAnchor)
        return navigatorDataRange.size.toFloat() / showCaseDataRange.size.toFloat()
    }

    private fun getNavigatorVisualShift(): Float {
        var dx = 0f
        val showCaseVisualXShift = showCase.visualXShift
        if (showCaseVisualXShift != 0f) {
            dx = showCaseVisualXShift / getShowCaseVisualRatio()
        }
        return -dx
    }

    companion object {
        private const val MIN_WIDTH_IN_PIXELS: Long = 40
        private const val CLICK_RECOGNITION_ERROR_IN_PIXELS: Long = 30
    }

    private enum class ActionType {
        MOVE_COMPLETE_ACTIVE_INTERVAL, MOVE_ACTIVE_INTERVAL_START, MOVE_ACTIVE_INTERVAL_END
    }
}