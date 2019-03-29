package tech.harmonysoft.oss.leonardo.model.config.navigator

import android.content.Context

/**
 * @author Denis Zhdanov
 * @since 26/3/19
 */
interface NavigatorConfigBuilder {

    fun withInactiveBackgroundColor(color: Int): NavigatorConfigBuilder

    fun withActiveBorderColor(color: Int): NavigatorConfigBuilder

    fun withActiveBorderHorizontalWidthInPixels(widthInPixels: Int): NavigatorConfigBuilder

    fun withActiveBorderVerticalHeightInPixels(heightInPixels: Int): NavigatorConfigBuilder

    fun withContext(context: Context): NavigatorConfigBuilder

    fun build(): NavigatorConfig
}