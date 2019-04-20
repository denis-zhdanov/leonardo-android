package tech.harmonysoft.oss.leonardo.model.config.navigator.impl

import android.content.Context
import tech.harmonysoft.oss.leonardo.R
import tech.harmonysoft.oss.leonardo.model.config.navigator.NavigatorConfig
import tech.harmonysoft.oss.leonardo.model.config.navigator.NavigatorConfigBuilder
import tech.harmonysoft.oss.leonardo.model.util.LeonardoUtil.getColor
import tech.harmonysoft.oss.leonardo.model.util.LeonardoUtil.getDimensionSizeInPixels

class NavigatorConfigBuilderImpl : NavigatorConfigBuilder {

    private var style = R.style.Leonardo_Light
    private var inactiveBackgroundColor: Int? = null
    private var activeBorderColor: Int? = null
    private var activeBackgroundWidthInPixels: Int? = null
    private var activeBackgroundHeightInPixels: Int? = null
    private var context: Context? = null

    override fun withInactiveBackgroundColor(color: Int) = apply {
        inactiveBackgroundColor = color
    }

    override fun withActiveBorderColor(color: Int) = apply {
        activeBorderColor = color
    }

    override fun withActiveBorderHorizontalWidthInPixels(widthInPixels: Int) = apply {
        activeBackgroundWidthInPixels = widthInPixels
    }

    override fun withActiveBorderVerticalHeightInPixels(heightInPixels: Int) = apply {
        activeBackgroundHeightInPixels = heightInPixels
    }

    override fun withStyle(style: Int) = apply {
        this.style = style
    }

    override fun withContext(context: Context) = apply {
        this.context = context
    }

    override fun build(): NavigatorConfig {
        val inactiveBackgroundColor = getColor(
            "Navigator chart background",
            R.attr.leonardo_navigator_chart_background_inactive,
            inactiveBackgroundColor,
            style,
            context
        )

        val activeBorderColor = getColor(
            "Navigator chart active border color",
            R.attr.leonardo_navigator_chart_active_border_color,
            activeBorderColor,
            style,
            context
        )

        val activeBorderHorizontalWidth = getDimensionSizeInPixels(
            "Navigator chart active border width",
            R.attr.leonardo_navigator_chart_active_border_horizontal_size,
            activeBackgroundWidthInPixels,
            style,
            context
        )

        val activeBorderVerticalHeight = getDimensionSizeInPixels(
            "Navigator chart active border height",
            R.attr.leonardo_navigator_chart_active_border_vertical_size,
            activeBackgroundHeightInPixels,
            style,
            context
        )

        return NavigatorConfig(
            inactiveBackgroundColor,
            activeBorderColor,
            activeBorderHorizontalWidth,
            activeBorderVerticalHeight
        )
    }
}