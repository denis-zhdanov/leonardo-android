package tech.harmonysoft.oss.leonardo.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.Range.Companion.EMPTY_RANGE
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

    private lateinit var mConfig: NavigatorConfig
    private lateinit var mModel: ChartModel
    private val mView = ChartView(getContext())
    private lateinit var mShowCase: NavigatorShowcase

    private lateinit var mInactiveBackgroundPaint: Paint
    private lateinit var mActiveBackgroundPaint: Paint
    private lateinit var mActiveBorderPaint: Paint

    private var mCurrentAction: ActionType? = null
    private var mPreviousActionVisualX: Float? = null

    fun apply(navigatorConfig: NavigatorConfig, chartConfig: ChartConfig) {
        mConfig = navigatorConfig
        mActiveBackgroundPaint = createPaint(chartConfig.backgroundColor)
        mInactiveBackgroundPaint = createPaint(mConfig.inactiveChartBackgroundColor)
        mActiveBorderPaint = createPaint(mConfig.activeBorderColor)
        val plotLineWidth = Math.max(1, chartConfig.plotLineWidthInPixels / 2)
        val axisConfig = LeonardoConfigFactory.newAxisConfigBuilder()
            .disableLabels()
            .disableAxis()
            .build()
        mView.apply(LeonardoConfigFactory.newChartConfigBuilder()
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
        mModel = model
        mView.apply(model)
        setupListener(model)
        refreshMyRange()
        invalidate()
    }

    fun apply(showCase: NavigatorShowcase) {
        mShowCase = showCase
    }

    private fun setupListener(model: ChartModel) {
        model.addListener(object : ChartModelListener {
            override fun onRangeChanged(anchor: Any) {
                if (anchor === getModelAnchor()) {
                    refreshMyRange()
                } else if (anchor === mShowCase.dataAnchor) {
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
        val navigatorRange = mModel.getActiveRange(getModelAnchor())
        mModel.setActiveRange(navigatorRange, mView)
        val dependencyPointsNumber = Math.max(1, (navigatorRange.size + 1) / 4)
        val dependencyRangeShift = (navigatorRange.size + 1 - dependencyPointsNumber) / 2
        val dependencyRangeStart = navigatorRange.start + 1 + dependencyRangeShift
        val dependencyRange = Range(dependencyRangeStart,
                                    dependencyRangeStart + dependencyPointsNumber)
        mModel.setActiveRange(dependencyRange, mShowCase.dataAnchor)
        invalidate()
    }

    private fun getModelAnchor(): Any {
        return this
    }

    private fun mayBeInitialize() {
        mayBeSetInnerChartDimensions()
    }

    private fun mayBeSetInnerChartDimensions() {
        if (mView.width + 2 != width) {
            mView.left = 0
            mView.top = 0
            mView.right = width
            mView.bottom = height
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
        if (!::mConfig.isInitialized) {
            return
        }

        mayBeInitialize()

        val dx = getNavigatorVisualShift()
        drawInactiveBackground(canvas, dx)
        drawActiveBackground(canvas, dx)
        drawActiveBorder(canvas, dx)
        mView.draw(canvas)
    }

    private fun drawInactiveBackground(canvas: Canvas, dx: Float) {
        val activeRange = mModel.getActiveRange(mShowCase.dataAnchor)
        if (activeRange === EMPTY_RANGE) {
            return
        }

        val wholeRange = mModel.getActiveRange(mView)

        if (wholeRange.start < activeRange.start) {
            val activeXStart = mView.dataMapper.dataXToVisualX(activeRange.start)
            canvas.drawRect(0f, 0f, activeXStart + dx, mView.height.toFloat(), mInactiveBackgroundPaint)
        }

        if (wholeRange.end > activeRange.end) {
            val activeXEnd = mView.dataMapper.dataXToVisualX(activeRange.end)
            canvas.drawRect(activeXEnd + dx,
                            0f,
                            mView.width.toFloat(),
                            mView.height.toFloat(),
                            mInactiveBackgroundPaint)
        }
    }

    private fun drawActiveBackground(canvas: Canvas, dx: Float) {
        val activeRange = mModel.getActiveRange(mShowCase.dataAnchor)
        if (activeRange === EMPTY_RANGE) {
            return
        }

        val activeXStart = mView.dataMapper.dataXToVisualX(activeRange.start)
        val activeXEnd = mView.dataMapper.dataXToVisualX(activeRange.end)
        canvas.drawRect(activeXStart + dx,
                        0f,
                        activeXEnd + dx,
                        mView.height.toFloat(),
                        mActiveBackgroundPaint)
    }

    private fun drawActiveBorder(canvas: Canvas, dx: Float) {
        val activeRange = mModel.getActiveRange(mShowCase.dataAnchor)
        if (activeRange === EMPTY_RANGE) {
            return
        }

        // Left edge
        val activeXStart = mView.dataMapper.dataXToVisualX(activeRange.start)
        canvas.drawRect(activeXStart + dx,
                        0f,
                        activeXStart + mConfig.activeBorderHorizontalWidthInPixels,
                        mView.height.toFloat(),
                        mActiveBorderPaint)

        // Right edge
        val activeXEnd = mView.dataMapper.dataXToVisualX(activeRange.end)
        canvas.drawRect(activeXEnd + -mConfig.activeBorderHorizontalWidthInPixels + dx,
                        0f,
                        activeXEnd + dx,
                        mView.height.toFloat(),
                        mActiveBorderPaint)

        // Top edge
        canvas.drawRect(activeXStart + dx,
                        0f,
                        activeXEnd + dx,
                        mConfig.activeBorderVerticalHeightInPixels.toFloat(),
                        mActiveBorderPaint)

        // Bottom edge
        canvas.drawRect(activeXStart + dx,
                        mView.height.toFloat(),
                        activeXEnd + dx,
                        mView.height - mConfig.activeBorderVerticalHeightInPixels.toFloat(),
                        mActiveBorderPaint)
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
        mCurrentAction = getActionType(visualX)
        if (mCurrentAction != null) {
            mPreviousActionVisualX = visualX
            invalidate()
        }
    }

    private fun move(visualX: Float) {
        val previousVisualX = mPreviousActionVisualX
        mPreviousActionVisualX = visualX

        if (mCurrentAction == null || previousVisualX == null) {
            return
        }

        val navigatorVisualDeltaX = previousVisualX - visualX
        if (navigatorVisualDeltaX == 0f) {
            return
        }

        val ratio = getShowCaseVisualRatio()

        if (mCurrentAction == MOVE_COMPLETE_ACTIVE_INTERVAL) {
            mShowCase.scrollHorizontally(navigatorVisualDeltaX * ratio)
            invalidate()
        } else if (mCurrentAction == MOVE_ACTIVE_INTERVAL_START) {
            val showCaseDataRange = mModel.getActiveRange(mShowCase.dataAnchor)
            val endRangeVisualX = mView.dataMapper.dataXToVisualX(showCaseDataRange.end)
            if (endRangeVisualX - visualX < MIN_WIDTH_IN_PIXELS) {
                // Don't allow selector to become too narrow
                return
            }
            val newStartDataX = mView.dataMapper.visualXToDataX(Math.max(visualX, 0f))
            if (newStartDataX != showCaseDataRange.start) {
                mModel.setActiveRange(Range(newStartDataX, showCaseDataRange.end), mShowCase.dataAnchor)
            }
        } else {
            val showCaseDataRange = mModel.getActiveRange(mShowCase.dataAnchor)
            val myDataRange = mModel.getActiveRange(getModelAnchor())
            val startRangeVisualX = mView.dataMapper.dataXToVisualX(showCaseDataRange.start)
            if (visualX - startRangeVisualX < MIN_WIDTH_IN_PIXELS) {
                // Don't allow selector to become too narrow
                return
            }
            val newEndDataX = mView.dataMapper.visualXToDataX(Math.max(visualX, 0f))
            if (newEndDataX <= myDataRange.end && newEndDataX != showCaseDataRange.end) {
                mModel.setActiveRange(Range(showCaseDataRange.start, newEndDataX), mShowCase.dataAnchor)
            }
        }
    }

    private fun release(visualX: Float) {
        move(visualX)
        mCurrentAction = null
        mPreviousActionVisualX = null
        invalidate()
    }

    private fun getActionType(x: Float): ActionType? {
        val activeRange = mModel.getActiveRange(mShowCase.dataAnchor)
        if (activeRange === EMPTY_RANGE) {
            return null
        }

        val dx = getNavigatorVisualShift()
        val startX = mView.dataMapper.dataXToVisualX(activeRange.start) + dx
        val endX = mView.dataMapper.dataXToVisualX(activeRange.end) + dx

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
        val navigatorDataRange = mModel.getActiveRange(getModelAnchor())
        val showCaseDataRange = mModel.getActiveRange(mShowCase.dataAnchor)
        return navigatorDataRange.size.toFloat() / showCaseDataRange.size.toFloat()
    }

    private fun getNavigatorVisualShift(): Float {
        var dx = 0f
        val showCaseVisualXShift = mShowCase.visualXShift
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