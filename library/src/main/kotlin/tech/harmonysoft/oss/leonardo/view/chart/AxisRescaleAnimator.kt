package tech.harmonysoft.oss.leonardo.view.chart

import android.animation.ValueAnimator
import android.view.View
import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.util.LeonardoUtil

/**
 * @author Denis Zhdanov
 * @since 30/3/19
 */
class AxisRescaleAnimator(private val view: View) {

    private var unitAnimator: ValueAnimator? = null
    private var fadeAnimator: ValueAnimator? = null

    var axisUnitSize = 0f
    var initialAxisStep = 0L
    var initialAxisRange = Range.EMPTY_RANGE
    var initialStepAlpha = 255
    var finalStepAlpha = 0

    val inProgress: Boolean
        get() {
            return unitAnimator?.isRunning ?: false
        }

    fun animate(from: Range, to: Range, step: Long, availableSize: Int) {
        if (!inProgress) {
            axisUnitSize = availableSize.toFloat() / from.size.toFloat()
            initialAxisStep = step
            initialAxisRange = from
        }

        unitAnimator?.apply { cancel() }
        val finalUnitSize = availableSize.toFloat() / to.size.toFloat()
        unitAnimator = ValueAnimator.ofFloat(axisUnitSize, finalUnitSize).apply {
            duration = LeonardoUtil.ANIMATION_DURATION_MILLIS
            addUpdateListener {
                axisUnitSize = it.animatedValue as Float
                view.invalidate()
            }
            start()
        }

        fadeAnimator?.apply { cancel() }
        initialStepAlpha = 255
        finalStepAlpha = 0
        fadeAnimator = ValueAnimator.ofInt(255, 0).apply {
            duration = LeonardoUtil.ANIMATION_DURATION_MILLIS
            startDelay = duration / 3
            addUpdateListener {
                initialStepAlpha = it.animatedValue as Int
                finalStepAlpha = 255 - initialStepAlpha
            }
            start()
        }
    }
}