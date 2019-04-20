package tech.harmonysoft.oss.leonardo.model.config.chart

import android.content.Context
import tech.harmonysoft.oss.leonardo.model.config.axis.AxisConfig
import tech.harmonysoft.oss.leonardo.model.config.axis.AxisConfigBuilder
import tech.harmonysoft.oss.leonardo.R

interface ChartConfigBuilder {

    fun withXAxisConfig(config: AxisConfig): ChartConfigBuilder

    fun withXAxisConfigBuilder(builder: AxisConfigBuilder): ChartConfigBuilder

    fun withYAxisConfig(config: AxisConfig): ChartConfigBuilder

    fun withYAxisConfigBuilder(builder: AxisConfigBuilder): ChartConfigBuilder

    fun disableBackground(): ChartConfigBuilder

    /**
     *
     * Specifies background color to use.
     *
     *
     * If it's not specified explicitly and [a context is provided][withContext],
     * then [R.attr.leonardo_chart_background_color] from its theme is used if defined.
     *
     *
     * @param color     background color to use
     * @return          current builder
     */
    fun withBackgroundColor(color: Int): ChartConfigBuilder

    /**
     *
     * Custom grid lines width in pixels to use.
     *
     *
     * If it's not specified explicitly and [a context is provided][.withContext],
     * the [R.attr.leonardo_chart_grid_width] is used if defined.
     *
     *
     * @param widthInPixels     grid line width in pixels to use
     * @return                  current builder
     */
    fun withGridLineWidthInPixels(widthInPixels: Int): ChartConfigBuilder

    /**
     *
     * Specifies color to use for drawing grid and axis.
     *
     *
     * If it's not specified explicitly and [a context is provided][.withContext],
     * then [R.attr.leonardo_chart_grid_color] from its theme is used.
     *
     *
     * @param color     background color to use
     * @return          current builder
     */
    fun withGridColor(color: Int): ChartConfigBuilder

    /**
     *
     * Custom plot lines width in pixels to use.
     *
     *
     * If it's not specified explicitly and [a context is provided][.withContext],
     * the [R.attr.leonardo_chart_plot_width] is used if defined.
     *
     *
     * @param widthInPixels     plot line width in pixels to use
     * @return                  current builder
     */
    fun withPlotLineWidthInPixels(widthInPixels: Int): ChartConfigBuilder

    fun disableSelection(): ChartConfigBuilder

    /**
     *
     * Custom selection sign radius in pixels to use.
     *
     *
     * If it's not specified explicitly and [a context is provided][.withContext],
     * the [R.attr.leonardo_chart_selection_sign_radius] is used if defined.
     *
     *
     * @param radiusInPixels    selection sign radius in pixels to use
     * @return                  current builder
     */
    fun withSelectionSignRadiusInPixels(radiusInPixels: Int): ChartConfigBuilder

    /**
     *
     * Specifies color to use for drawing legend title.
     *
     *
     * If it's not specified explicitly and [a context is provided][.withContext],
     * then [R.attr.leonardo_chart_legend_text_title_color] from its theme is used.
     *
     *
     * @param color     legend text title color to use
     * @return          current builder
     */
    fun withLegendTextTitleColor(color: Int): ChartConfigBuilder

    /**
     *
     * Specifies color to use for drawing legend background.
     *
     *
     * If it's not specified explicitly and [a context is provided][.withContext],
     * then [R.attr.leonardo_chart_legend_background_color] from its theme is used.
     *
     *
     * @param color     legend background color to use
     * @return          current builder
     */
    fun withLegendBackgroundColor(color: Int): ChartConfigBuilder

    fun disableAnimations(): ChartConfigBuilder

    fun withAnimationDurationMillis(duration: Int): ChartConfigBuilder

    fun withStyle(style: Int): ChartConfigBuilder

    /**
     * Applies default values to various graphic elements obtained from the given context's
     * [theme][Context.getTheme].
     *
     * @param context   theme holder which defaults should be applied unless explicitly specified
     * @return          current builder
     */
    fun withContext(context: Context): ChartConfigBuilder

    fun withConfig(config: ChartConfig): ChartConfigBuilder

    fun build(): ChartConfig
}