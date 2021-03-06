package tech.harmonysoft.oss.leonardo.view.util

import android.graphics.Paint
import android.graphics.Rect
import tech.harmonysoft.oss.leonardo.model.text.TextWrapper

class TextWidthMeasurer(private val paintSupplier: () -> Paint) : TextSpaceMeasurer {

    private val bounds = Rect()

    override fun measureVisualSpace(text: String): Int {
        paintSupplier().getTextBounds(text, 0, text.length, bounds)
        return bounds.width()
    }

    override fun measureVisualSpace(text: TextWrapper): Int {
        paintSupplier().getTextBounds(text.data, 0, text.length, bounds)
        return bounds.width()
    }
}