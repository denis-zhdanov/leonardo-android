package tech.harmonysoft.oss.leonardo.example.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import tech.harmonysoft.oss.leonardo.example.R
import tech.harmonysoft.oss.leonardo.view.util.RoundedRectangleDrawer

class CheckBoxView @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null,
    defaultStyle: Int = 0
) : View(context, attributes, defaultStyle) {

    var color = 0
    var callback: ((Boolean) -> Unit)? = null
    var checked = true
        set(value) {
            field = value
            invalidate()
        }

    private val paint = Paint()
    private val roundedRectangleDrawer = RoundedRectangleDrawer.INSTANCE
    private var cachedBounds: RectF? = null

    init {
        setOnClickListener {
            checked = !checked
            callback?.invoke(checked)
            invalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val edge = resources.getDimension(R.dimen.checkbox_edge)
        setMeasuredDimension(edge.toInt(), edge.toInt())
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        val bounds = cachedBounds?.takeIf {
            it.width() == width.toFloat() && it.height() == height.toFloat()
        } ?: RectF(0f, 0f, width.toFloat(), height.toFloat())
        cachedBounds = bounds

        val edge = resources.getDimension(R.dimen.checkbox_edge)
        val radius = edge / 6

        paint.color = color
        paint.style = Paint.Style.FILL
        roundedRectangleDrawer.draw(bounds, { paint }, { paint }, radius, canvas)

        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = edge / 10
        val path = Path()
        if (checked) {
            path.moveTo(edge * 12f / 54f, top + edge * 31f / 54f)
            path.lineTo(edge * 20f / 54f, top + edge * 39f / 54f)
            path.lineTo(edge * 43f / 54f, top + edge * 17f / 54f)
            canvas.drawPath(path, paint)
        } else {
            val signPadding = edge / 4f
            canvas.drawLine(signPadding,
                            signPadding,
                            edge - signPadding,
                            edge - signPadding,
                            paint)
            canvas.drawLine(signPadding,
                            edge - signPadding,
                            edge - signPadding,
                            signPadding,
                            paint)
        }
    }
}