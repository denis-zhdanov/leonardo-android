package tech.harmonysoft.oss.leonardo.view.util

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import tech.harmonysoft.oss.leonardo.view.chart.ChartView

/**
 * We might want to apply particular value in animated fractions, e.g. suppose we want to animate value '5'
 * in 5 seconds. That way it might be processed like '1 per second' or '2 after the first 2 seconds', then
 * '1 after a second', '1 after a second', '1 after a second' etc.
 *
 * Standard [ValueAnimator] doesn't allow that, so, we achieve that by the current class.
 */
class IterationAwareValueAnimator(
    private val durationMillis: Int,
    interpolator: TimeInterpolator,
    private val listener: (Float) -> Unit
) {

    private val animator = ValueAnimator().apply {
        setInterpolator(interpolator)
        duration = ChartView.FLING_DURATION_MILLIS.toLong()
        addUpdateListener {
            tickAnimation(it.animatedValue as Int)
        }
    }

    private var animatedSoFar = 0.0f
    private var totalValueToAnimate = 0.0f

    fun start(valueToAnimate: Float) {
        animatedSoFar = 0.0f
        totalValueToAnimate = valueToAnimate
        animator.setIntValues(0, durationMillis)
        animator.cancel()
        animator.start()
    }

    fun cancel() {
        animator.cancel()
    }

    private fun tickAnimation(elapsedTime: Int) {
        val valueToCurrentTime = elapsedTime * totalValueToAnimate / durationMillis
        val fl = valueToCurrentTime - animatedSoFar
        listener(fl)
        animatedSoFar = valueToCurrentTime
    }
}