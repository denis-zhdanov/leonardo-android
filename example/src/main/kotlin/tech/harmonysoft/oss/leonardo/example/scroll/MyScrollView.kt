package tech.harmonysoft.oss.leonardo.example.scroll

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.ScrollView
import tech.harmonysoft.oss.leonardo.example.LeonardoApplication
import javax.inject.Inject

class MyScrollView @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null,
    defaultStyle: Int = 0
) : ScrollView(context, attributes, defaultStyle) {

    @Inject lateinit var scrollManager: ScrollManager

    private val locationCache = mutableMapOf<View, Point>()

    init {
        LeonardoApplication.graph.inject(this)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val scrollOwner = scrollManager.scrollOwner ?: return super.onTouchEvent(e)

        val location = getRelativeY(scrollOwner)
        val action = e.actionMasked
        if (action == MotionEvent.ACTION_MOVE) {
            scrollOwner.move(e.x - location.x)
        } else {
            scrollManager.scrollOwner = null
        }
        return true
    }

    private fun getRelativeY(view: View): Point {
        val cached = locationCache.get(view)
        if (cached != null) {
            return cached
        }

        var x = 0
        var y = 0
        var v = view
        while (v !== this) {
            x += v.left
            y += v.top
            val parent = v.parent
            if (parent is View) {
                v = parent
            } else {
                break
            }
        }

        val result = Point(x, y)
        locationCache.put(view, result)
        return result
    }
}