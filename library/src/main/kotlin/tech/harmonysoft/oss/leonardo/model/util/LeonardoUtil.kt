package tech.harmonysoft.oss.leonardo.model.util

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import tech.harmonysoft.oss.leonardo.R
import tech.harmonysoft.oss.leonardo.view.ChartView
import tech.harmonysoft.oss.leonardo.view.NavigatorShowcase

/**
 * @author Denis Zhdanov
 * @since 26/3/19
 */
object LeonardoUtil {

    const val DEFAULT_CORNER_RADIUS = 20f
    const val ACTION_START_AUTO_EXPAND_AREA_IN_PIXELS = 15
    const val ANIMATION_DURATION_MILLIS: Long = 300
    const val ANIMATION_TICK_FREQUENCY_MILLIS: Long = 20

    fun getColor(resourceDescription: String, attributeId: Int, value: Int?, context: Context?): Int {
        if (value != null) {
            return value
        }
        if (context != null) {
            return getColor(context, attributeId)
        }
        throw IllegalStateException("$resourceDescription is undefined")
    }

    fun getColor(context: Context, attributeId: Int): Int {
        // Look up a custom value in app theme first
        val appTypedValue = TypedValue()
        val resolved = context.theme.resolveAttribute(attributeId, appTypedValue, true)
        if (resolved
            && appTypedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT
            && appTypedValue.type <= TypedValue.TYPE_LAST_COLOR_INT
        ) {
            return appTypedValue.data
        }

        // Fallback to library defaults
        val typedArray = context.obtainStyledAttributes(R.style.Leonardo_Light, intArrayOf(attributeId))
        try {
            return typedArray.getColor(0, Color.WHITE)
        } finally {
            typedArray.recycle()
        }
    }

    fun getDimensionSizeInPixels(resourceDescription: String, attributeId: Int, value: Int?, context: Context?): Int {
        if (value != null) {
            return value
        }
        if (context != null) {
            return getDimensionSizeInPixels(context, attributeId)
        }
        throw IllegalStateException("$resourceDescription is undefined")
    }

    fun getDimensionSizeInPixels(context: Context, attributeId: Int): Int {
        // Look up a custom value in app theme first
        val appTypedValue = TypedValue()
        val resolved = context.theme.resolveAttribute(attributeId, appTypedValue, true)
        if (resolved) {
            return appTypedValue.getDimension(context.resources.displayMetrics).toInt()
        }

        // Fallback to library defaults
        val typedArray = context.obtainStyledAttributes(R.style.Leonardo_Light, intArrayOf(attributeId))
        try {
            return typedArray.getDimensionPixelSize(0, -1)
        } finally {
            typedArray.recycle()
        }
    }

    fun asNavigatorShowCase(view: ChartView): NavigatorShowcase {
        return object : NavigatorShowcase {
            override val dataAnchor get() = view.getDataAnchor()

            override val visualXShift: Float
                get() = view.getXVisualShift()

            override fun scrollHorizontally(deltaVisualX: Float) {
                view.scrollHorizontally(deltaVisualX)
            }
        }
    }
}