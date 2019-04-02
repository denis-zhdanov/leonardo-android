package tech.harmonysoft.oss.leonardo.model.config.axis

import android.content.Context
import tech.harmonysoft.oss.leonardo.model.text.ValueRepresentationStrategy
import tech.harmonysoft.oss.leonardo.R

interface AxisConfigBuilder {

    fun disableLabels(): AxisConfigBuilder

    fun disableAxis(): AxisConfigBuilder

    fun withLabelTextStrategy(strategy: ValueRepresentationStrategy): AxisConfigBuilder

    /**
     * Specifies font size in pixels to use for axis labels.
     *
     * If it's not specified explicitly and [a context is provided][.withContext],
     * then [R.attr.leonardo_chart_axis_label_text_size] from its theme is used.
     *
     * If font size is still undefined, an attempt to [build] a config throws an exception.
     *
     * @param size      font size in pixels to use
     * @return          current builder
     */
    fun withFontSizeInPixels(size: Int): AxisConfigBuilder

    /**
     * Specifies color to use for drawing axis labels.
     *
     * If it's not specified explicitly and [a context is provided][withContext],
     * then [R.attr.leonardo_chart_axis_label_color] from its theme is used.
     *
     * If label color is still undefined, an attempt to [build] a config throws an exception.
     *
     * @param color     axis label color to use
     * @return          current builder
     */
    fun withLabelColor(color: Int): AxisConfigBuilder

    /**
     * Applies default values to various graphic elements obtained from the given context's
     * [theme][Context.getTheme].
     *
     * @param context   theme holder which defaults should be applied unless explicitly specified
     * @return          current builder
     */
    fun withContext(context: Context): AxisConfigBuilder

    fun build(): AxisConfig
}