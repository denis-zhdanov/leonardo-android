package tech.harmonysoft.oss.leonardo.model.config.chart.impl

import android.content.Context
import tech.harmonysoft.oss.leonardo.R
import tech.harmonysoft.oss.leonardo.model.config.LeonardoConfigFactory
import tech.harmonysoft.oss.leonardo.model.config.axis.AxisConfig
import tech.harmonysoft.oss.leonardo.model.config.axis.AxisConfigBuilder
import tech.harmonysoft.oss.leonardo.model.config.chart.ChartConfig
import tech.harmonysoft.oss.leonardo.model.config.chart.ChartConfigBuilder
import tech.harmonysoft.oss.leonardo.model.util.LeonardoUtil.getColor
import tech.harmonysoft.oss.leonardo.model.util.LeonardoUtil.getDimensionSizeInPixels
import tech.harmonysoft.oss.leonardo.model.util.LeonardoUtil.getInt

class ChartConfigBuilderImpl : ChartConfigBuilder {

    private var style = R.style.Leonardo_Light
    private var drawSelection = true
    private var allowSelection = true
    private var drawBackground = true
    private var enableAnimations = true

    private var xAxisConfig: AxisConfig? = null
    private var xAxisConfigBuilder: AxisConfigBuilder? = null
    private var yAxisConfig: AxisConfig? = null
    private var yAxisConfigBuilder: AxisConfigBuilder? = null
    private var backgroundColor: Int? = null
    private var gridLineWidthInPixels: Int? = null
    private var gridColor: Int? = null
    private var plotLineWidthInPixels: Int? = null
    private var selectionSignRadiusInPixels: Int? = null
    private var legendTextColor: Int? = null
    private var legendBackgroundColor: Int? = null
    private var animationDurationMillis: Int? = null
    private var context: Context? = null

    override fun withXAxisConfig(config: AxisConfig) = apply {
        xAxisConfig = config
    }

    override fun withXAxisConfigBuilder(builder: AxisConfigBuilder) = apply {
        xAxisConfigBuilder = builder
    }

    override fun withYAxisConfig(config: AxisConfig) = apply {
        yAxisConfig = config
    }

    override fun withYAxisConfigBuilder(builder: AxisConfigBuilder) = apply {
        yAxisConfigBuilder = builder
    }

    override fun disableBackground() = apply {
        drawBackground = false
    }

    override fun withBackgroundColor(color: Int) = apply {
        backgroundColor = color
    }

    override fun withGridLineWidthInPixels(widthInPixels: Int) = apply {
        gridLineWidthInPixels = widthInPixels
    }

    override fun withGridColor(color: Int) = apply {
        gridColor = color
    }

    override fun withPlotLineWidthInPixels(widthInPixels: Int) = apply {
        plotLineWidthInPixels = widthInPixels
    }

    override fun disableSelection() = apply {
        allowSelection = false
        drawSelection = false
    }

    override fun withSelectionSignRadiusInPixels(radiusInPixels: Int) = apply {
        selectionSignRadiusInPixels = radiusInPixels
    }

    override fun withLegendTextTitleColor(color: Int) = apply {
        legendTextColor = color
    }

    override fun withLegendBackgroundColor(color: Int) = apply {
        legendBackgroundColor = color
    }

    override fun disableAnimations() = apply {
        enableAnimations = false
    }

    override fun withAnimationDurationMillis(duration: Int)  = apply {
        animationDurationMillis = duration
    }

    override fun withStyle(style: Int) = apply {
        this.style = style
    }

    override fun withContext(context: Context) = apply {
        this.context = context
    }

    override fun build(): ChartConfig {
        val xAxisConfig = getAxisConfig("X", xAxisConfig, xAxisConfigBuilder)
        val yAxisConfig = getAxisConfig("Y", yAxisConfig, yAxisConfigBuilder)
        val gridLineWidth = getDimensionSizeInPixels(
            "Grid line width",
            R.attr.leonardo_chart_grid_width,
            gridLineWidthInPixels,
            style,
            context
        )
        val backgroundColor = getColor(
            "Chart background color",
            R.attr.leonardo_chart_background_color,
            backgroundColor,
            style,
            context
        )
        val gridColor = getColor("Chart grid color", R.attr.leonardo_chart_grid_color, gridColor, style, context)
        val plotLineWidth = getDimensionSizeInPixels(
            "Plot line width",
            R.attr.leonardo_chart_plot_width,
            plotLineWidthInPixels,
            style,
            context
        )
        val selectionSignRadiusInPixels = getDimensionSizeInPixels(
            "Selection sign radius",
            R.attr.leonardo_chart_selection_sign_radius,
            selectionSignRadiusInPixels,
            style,
            context
        )
        val legendTitleColor = getColor(
            "Legend text title color",
            R.attr.leonardo_chart_legend_text_title_color,
            legendTextColor,
            style,
            context
        )

        val legendBackgroundColor = getColor(
            "Legend background color",
            R.attr.leonardo_chart_legend_background_color,
            legendBackgroundColor,
            style,
            context
        )

        val animationDurationMillis = getInt(
                "animation duration millis",
                R.attr.leonardo_animation_duration_millis,
                animationDurationMillis,
                style,
                context
        )

        return ChartConfig(
            xAxisConfig = xAxisConfig,
            yAxisConfig = yAxisConfig,
            drawBackground = drawBackground,
            gridLineWidthInPixels = gridLineWidth,
            backgroundColor = backgroundColor,
            gridColor = gridColor,
            plotLineWidthInPixels = plotLineWidth,
            selectionSignRadiusInPixels = selectionSignRadiusInPixels,
            legendTextTitleColor = legendTitleColor,
            legendBackgroundColor = legendBackgroundColor,
            drawSelection = drawSelection,
            selectionAllowed = allowSelection,
            animationEnabled = enableAnimations,
            animationDurationMillis = animationDurationMillis
        )
    }

    private fun getAxisConfig(axis: String, config: AxisConfig?, builder: AxisConfigBuilder?): AxisConfig {
        if (config != null && builder != null) {
            throw IllegalAccessException("Both axis config and axis config builder are specified for axis $axis")
        }
        if (config != null) {
            return config
        }

        return (builder ?: LeonardoConfigFactory.newAxisConfigBuilder()).apply {
            context?.let {
                withStyle(style)
                withContext(it)
            }
        }.build()
    }

    override fun withConfig(config: ChartConfig): ChartConfigBuilder {
        return withXAxisConfig(config.xAxisConfig)
            .withYAxisConfig(config.yAxisConfig)
            .withBackgroundColor(config.backgroundColor)
            .withGridLineWidthInPixels(config.gridLineWidthInPixels)
            .withGridColor(config.gridColor)
            .withPlotLineWidthInPixels(config.plotLineWidthInPixels)
            .withSelectionSignRadiusInPixels(config.selectionSignRadiusInPixels)
            .withLegendTextTitleColor(config.legendTextTitleColor)
            .withLegendBackgroundColor(config.legendBackgroundColor).apply {
                if (!config.drawSelection) {
                    disableSelection()
                }
            }
    }
}