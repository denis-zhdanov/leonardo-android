package tech.harmonysoft.oss.leonardo.model.util

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import tech.harmonysoft.oss.leonardo.collection.DataTree
import tech.harmonysoft.oss.leonardo.model.DataPoint
import tech.harmonysoft.oss.leonardo.view.chart.ChangeAnchor
import tech.harmonysoft.oss.leonardo.view.chart.ChartView
import tech.harmonysoft.oss.leonardo.view.navigator.NavigatorShowcase

object LeonardoUtil {

    const val DEFAULT_CORNER_RADIUS = 20f

    val LONG_COMPARATOR = Comparator<Long> { l1, l2 ->
        when {
            l1 < l2 -> -1
            l1 > l2 -> 1
            else -> 0
        }
    }

    fun getColor(resourceDescription: String,
                 attributeId: Int,
                 value: Int?,
                 defaultStyle: Int,
                 context: Context?): Int {
        if (value != null) {
            return value
        }
        if (context != null) {
            return getColor(context, defaultStyle, attributeId)
        }
        throw IllegalStateException("$resourceDescription is undefined")
    }

    fun getColor(context: Context, defaultStyle: Int, attributeId: Int): Int {
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
        val typedArray = context.obtainStyledAttributes(defaultStyle, intArrayOf(attributeId))
        try {
            return typedArray.getColor(0, Color.WHITE)
        } finally {
            typedArray.recycle()
        }
    }

    fun getDimensionSizeInPixels(resourceDescription: String,
                                 attributeId: Int,
                                 value: Int?,
                                 defaultStyle: Int,
                                 context: Context?): Int {
        if (value != null) {
            return value
        }
        if (context != null) {
            return getDimensionSizeInPixels(context, defaultStyle, attributeId)
        }
        throw IllegalStateException("$resourceDescription is undefined")
    }

    fun getDimensionSizeInPixels(context: Context, defaultStyle: Int, attributeId: Int): Int {
        // Look up a custom value in app theme first
        val appTypedValue = TypedValue()
        val resolved = context.theme.resolveAttribute(attributeId, appTypedValue, true)
        if (resolved) {
            return appTypedValue.getDimension(context.resources.displayMetrics).toInt()
        }

        // Fallback to library defaults
        val typedArray = context.obtainStyledAttributes(defaultStyle, intArrayOf(attributeId))
        try {
            return typedArray.getDimensionPixelSize(0, -1)
        } finally {
            typedArray.recycle()
        }
    }

    fun getInt(resourceDescription: String,
               attributeId: Int,
               value: Int?,
               defaultStyle: Int,
               context: Context?): Int {
        if (value != null) {
            return value
        }
        if (context != null) {
            return getInt(context, defaultStyle, attributeId)
        }
        throw IllegalStateException("$resourceDescription is undefined")
    }

    fun getInt(context: Context, defaultStyle: Int, attributeId: Int): Int {
        // Look up a custom value in app theme first
        val appTypedValue = TypedValue()
        val resolved = context.theme.resolveAttribute(attributeId, appTypedValue, true)
        if (resolved) {
            return appTypedValue.data
        }

        // Fallback to library defaults
        val typedArray = context.obtainStyledAttributes(defaultStyle, intArrayOf(attributeId))
        try {
            return typedArray.getInt(0, 300)
        } finally {
            typedArray.recycle()
        }
    }

    fun asNavigatorShowCase(view: ChartView): NavigatorShowcase {
        return object : NavigatorShowcase {
            override val dataAnchor get() = view.dataAnchor

            override val visualXShift: Float
                get() = view.xVisualShift

            override fun applyVisualXChange(deltaVisualX: Float, anchor: ChangeAnchor) {
                view.applyVisualXChange(deltaVisualX, anchor)
            }
        }
    }

    fun forPoints(points: DataTree<Long, DataPoint>,
                  start: Long,
                  end: Long,
                  includePrevious: Boolean = false,
                  includeNext: Boolean = false,
                  action: (DataPoint) -> Boolean) {
        var point: DataPoint? = if (includePrevious) {
            points.getPreviousValue(start) ?: points.get(start)
        } else {
            points.get(start)
        } ?: points.getNextValue(start) ?: return

        while (point != null) {
            if (point.x > end) {
                if (includeNext) {
                    action(point)
                }
                return
            }
            val shouldContinue = action(point)
            if (!shouldContinue) {
                return
            }
            point = points.getNextValue(point.x)
        }
    }

}