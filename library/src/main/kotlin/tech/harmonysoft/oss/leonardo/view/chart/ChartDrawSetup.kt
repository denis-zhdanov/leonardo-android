package tech.harmonysoft.oss.leonardo.view.chart

import android.graphics.Paint
import android.graphics.Typeface
import tech.harmonysoft.oss.leonardo.model.config.chart.ChartConfig

internal class ChartDrawSetup(private val config: ChartConfig) {

    private val paint = Paint()

    val backgroundPaint: Paint
        get() {
            return paint.apply {
                color = config.backgroundColor
                style = Paint.Style.FILL
                alpha = 255
            }
        }

    val gridPaint: Paint
        get() {
            return paint.apply {
                color = config.gridColor
                style = Paint.Style.STROKE
                strokeWidth = config.gridLineWidthInPixels.toFloat()
                alpha = 255
            }
        }

    val xLabelPaint: Paint
        get() {
            return paint.apply {
                color = config.xAxisConfig.labelColor
                style = Paint.Style.FILL
                strokeWidth = 0f
                textSize = config.xAxisConfig.labelFontSizeInPixels.toFloat()
                typeface = Typeface.DEFAULT
                alpha = 255
            }
        }

    val yLabelPaint: Paint
        get() {
            return paint.apply {
                color = config.yAxisConfig.labelColor
                style = Paint.Style.FILL
                strokeWidth = 0f
                textSize = config.yAxisConfig.labelFontSizeInPixels.toFloat()
                typeface = Typeface.DEFAULT
                alpha = 255
            }
        }

    val plotPaint: Paint
        get() {
            return paint.apply {
                style = Paint.Style.STROKE
                strokeWidth = config.plotLineWidthInPixels.toFloat()
                alpha = 255
            }
        }

    val legendBackgroundPaint: Paint
        get() {
            return paint.apply {
                color = config.legendBackgroundColor
                style = Paint.Style.FILL
                alpha = 255
            }
        }
}