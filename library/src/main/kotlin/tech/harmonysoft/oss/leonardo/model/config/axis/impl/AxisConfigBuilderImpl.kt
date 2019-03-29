package tech.harmonysoft.oss.leonardo.model.config.axis.impl

import android.content.Context
import android.graphics.Color
import tech.harmonysoft.oss.leonardo.R
import tech.harmonysoft.oss.leonardo.model.config.axis.AxisConfig
import tech.harmonysoft.oss.leonardo.model.config.axis.AxisConfigBuilder
import tech.harmonysoft.oss.leonardo.model.text.ValueRepresentationStrategy
import tech.harmonysoft.oss.leonardo.model.util.LeonardoUtil.getColor
import tech.harmonysoft.oss.leonardo.model.util.LeonardoUtil.getDimensionSizeInPixels

/**
 * @author Denis Zhdanov
 * @since 26/3/19
 */
class AxisConfigBuilderImpl : AxisConfigBuilder {

    private var labelTextStrategy: ValueRepresentationStrategy = DefaultValueRepresentationStrategy.INSTANCE
    private var labelsDisabled: Boolean = false
    private var axisDisabled: Boolean = false

    private var labelFontSizeInPixels: Int? = null
    private var labelColor: Int? = null
    private var context: Context? = null

    override fun disableLabels() = apply {
        labelsDisabled = true
    }

    override fun disableAxis() = apply {
        axisDisabled = true
    }

    override fun withLabelTextStrategy(strategy: ValueRepresentationStrategy) = apply {
        labelTextStrategy = strategy
    }

    override fun withFontSizeInPixels(size: Int) = apply {
        labelFontSizeInPixels = size
    }

    override fun withLabelColor(color: Int) = apply {
        labelColor = color
    }

    override fun withContext(context: Context) = apply {
        this.context = context
    }

    override fun build(): AxisConfig {
        val fontSize = labelFontSizeInPixels ?: run {
            if (labelsDisabled) {
                12 // Dummy value
            } else {
                context?.let {
                    getDimensionSizeInPixels(it, R.attr.leonardo_chart_axis_label_text_size)
                } ?: throw IllegalStateException("Label font size is undefined")
            }
        }

        val labelColor = this.labelColor ?: run {
            if (labelsDisabled) {
                Color.GRAY // Dummy value
            } else {
                context?.let {
                    getColor(it, R.attr.leonardo_chart_axis_label_color)
                } ?: throw IllegalStateException("Axis label color is undefined")
            }
        }

        return AxisConfig(
            labelTextStrategy = labelTextStrategy,
            labelFontSizeInPixels = fontSize,
            labelColor = labelColor,
            drawLabels = !labelsDisabled,
            drawAxis = !axisDisabled
        )
    }
}