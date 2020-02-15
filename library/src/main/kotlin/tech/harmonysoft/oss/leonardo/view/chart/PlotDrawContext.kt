package tech.harmonysoft.oss.leonardo.view.chart

import android.graphics.Path

internal data class PlotDrawContext(
    var minVisualX: Float = 0f,
    var maxVisualX: Float = 0f,
    var previousVisualX: Float? = null,
    var previousVisualY: Float? = null,
    var drawn: Boolean = false
) {
    lateinit var path: Path
}