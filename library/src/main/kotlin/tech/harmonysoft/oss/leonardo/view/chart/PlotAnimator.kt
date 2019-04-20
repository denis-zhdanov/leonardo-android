package tech.harmonysoft.oss.leonardo.view.chart

import android.animation.ValueAnimator
import android.view.View
import android.view.animation.LinearInterpolator
import tech.harmonysoft.oss.leonardo.model.data.ChartDataSource
import tech.harmonysoft.oss.leonardo.model.util.LeonardoUtil

class PlotAnimator(
    private val view: View,
    private val animationDurationMillis: Long = LeonardoUtil.ANIMATION_DURATION_MILLIS
) {

    private val animator = ValueAnimator().apply {
        interpolator = LinearInterpolator()
        duration = animationDurationMillis
        addUpdateListener {
            tickAnimation()
        }
    }

    private val animationDataSourceInfo = mutableMapOf<ChartDataSource, DataSourceAnimationContext>()

    private fun tickAnimation() {
        val toRemove = mutableListOf<ChartDataSource>()
        val now = System.currentTimeMillis()
        for ((key, context) in animationDataSourceInfo) {
            if (now >= context.startTimeMs + animationDurationMillis) {
                toRemove.add(key)
                break
            }

            val elapsedTimeMs = now - context.startTimeMs
            val totalAlphaDelta = context.finalAlpha - context.initialAlpha
            val currentAlphaDelta = elapsedTimeMs * totalAlphaDelta / LeonardoUtil.ANIMATION_DURATION_MILLIS
            context.currentAlpha = (context.initialAlpha + currentAlphaDelta).toInt()
            if ((totalAlphaDelta > 0 && context.currentAlpha >= context.finalAlpha)
                || (totalAlphaDelta < 0 && context.currentAlpha <= context.finalAlpha)
            ) {
                toRemove.add(key)
            }
        }

        if (toRemove.isNotEmpty()) {
            toRemove.forEach {
                animationDataSourceInfo.remove(it)
            }
        }

        if (animationDataSourceInfo.isNotEmpty()) {
            view.invalidate()
        }
    }

    fun fadeIn(dataSource: ChartDataSource) {
        animationDataSourceInfo[dataSource] = DataSourceAnimationContext(0, 255)
        startAnimation()
    }

    fun fadeOut(dataSource: ChartDataSource) {
        animationDataSourceInfo[dataSource] = DataSourceAnimationContext(255, 0)
        startAnimation()
    }

    private fun startAnimation() {
        animator.cancel()
        animator.setIntValues(0, 255)
        animator.start()
    }

    fun isAnimationInProgress(dataSource: ChartDataSource): Boolean {
        return animationDataSourceInfo.containsKey(dataSource)
    }

    fun getAlpha(dataSource: ChartDataSource): Int {
        return animationDataSourceInfo[dataSource]?.currentAlpha ?: 255
    }

    private data class DataSourceAnimationContext(
        val initialAlpha: Int,
        val finalAlpha: Int,
        val startTimeMs: Long = System.currentTimeMillis()
    ) {
        var currentAlpha = initialAlpha
    }
}