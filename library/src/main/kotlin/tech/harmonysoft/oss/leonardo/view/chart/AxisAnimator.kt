package tech.harmonysoft.oss.leonardo.view.chart

import android.animation.ValueAnimator
import android.view.View
import android.view.animation.LinearInterpolator
import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.util.LeonardoUtil
import kotlin.math.round

class AxisAnimator(
    private val view: View,
    private val animationDurationMillis: Long = LeonardoUtil.ANIMATION_DURATION_MILLIS
) {

    val inProgress: Boolean
        get() {
            return animator.isRunning
        }

    var rangeFrom = Range.EMPTY_RANGE
    var rangeTo = Range.EMPTY_RANGE
    var initialStep = 0L
    var initialStepAlpha = 255
    var finalStepAlpha = 0

    private val animator = ValueAnimator().apply {
        interpolator = LinearInterpolator()
        duration = animationDurationMillis
        addUpdateListener {
            tickAnimation(round(it.animatedValue as Float).toLong())
        }
    }

    private val fadeDurationMs = animationDurationMillis * 2 / 3
    private val fadeDelay = animationDurationMillis - fadeDurationMs

    private var visualSize = 0
    private var elapsedTimeRatio = 0f

    fun animate(from: Range, to: Range, initialStep: Long, availableSize: Int) {
        rangeFrom = if (inProgress) {
            Range(rangeFrom.start + ((rangeTo.start - rangeFrom.start) * elapsedTimeRatio).toLong(),
                  rangeFrom.end + ((rangeTo.end - rangeFrom.end) * elapsedTimeRatio).toLong())
        } else {
            from
        }
        this.initialStep = initialStep
        visualSize = availableSize
        rangeTo = to

        initialStepAlpha = 255
        finalStepAlpha = 0

        if (!inProgress) {
            animator.setFloatValues(0f, animationDurationMillis.toFloat())
            animator.start()
        }
    }

    private fun tickAnimation(elapsedTimeMs: Long) {
        elapsedTimeRatio = elapsedTimeMs.toFloat() / animationDurationMillis.toFloat()

        if (elapsedTimeMs <= fadeDelay) {
            return
        }

        finalStepAlpha = ((elapsedTimeMs - fadeDelay) * 255 / fadeDurationMs).toInt()
        initialStepAlpha = 255 - finalStepAlpha
        view.invalidate()
    }

    fun getVisualValue(dataValue: Long): Float {
        val initialValue = (dataValue - rangeFrom.start) * visualSize.toFloat() / rangeFrom.size
        val finalValue = (dataValue - rangeTo.start) * visualSize.toFloat() / rangeTo.size
        return initialValue + (finalValue - initialValue) * elapsedTimeRatio
    }

    fun getDataValue(visualValue: Float): Long {
        val initialValue = rangeFrom.start + visualValue * rangeFrom.size.toFloat() / visualSize.toFloat()
        val finalValue = rangeTo.start + visualValue * rangeTo.size.toFloat() / visualSize.toFloat()
        return (initialValue + (finalValue - initialValue) * elapsedTimeRatio).toLong()
    }
}