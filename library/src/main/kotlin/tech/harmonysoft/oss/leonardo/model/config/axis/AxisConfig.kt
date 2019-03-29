package tech.harmonysoft.oss.leonardo.model.config.axis

import tech.harmonysoft.oss.leonardo.model.text.ValueRepresentationStrategy

/**
 * @author Denis Zhdanov
 * @since 26/3/19
 */
data class AxisConfig(
    val drawAxis: Boolean,
    val drawLabels: Boolean,
    val labelFontSizeInPixels: Int,
    val labelColor: Int,
    val labelTextStrategy: ValueRepresentationStrategy
)