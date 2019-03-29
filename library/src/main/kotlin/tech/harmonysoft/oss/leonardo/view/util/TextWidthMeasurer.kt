package tech.harmonysoft.oss.leonardo.view.util

import android.graphics.Paint
import android.graphics.Rect
import tech.harmonysoft.oss.leonardo.model.text.TextWrapper

/**
 * @author Denis Zhdanov
 * @since 27/3/19
 */
class TextWidthMeasurer(private val paint: Paint) : TextSpaceMeasurer {

    private val bounds = Rect()

    override fun measureVisualSpace(text: String): Int {
        paint.getTextBounds(text, 0, text.length, bounds)
        return bounds.width()
    }

    override fun measureVisualSpace(text: TextWrapper): Int {
        paint.getTextBounds(text.data, 0, text.length, bounds)
        return bounds.width()
    }
}