package tech.harmonysoft.oss.leonardo.view.chart

import android.graphics.Path
import tech.harmonysoft.oss.leonardo.model.VisualPoint

internal data class PlotDrawContext(
    var minVisualX: Float = 0f,
    var maxVisualX: Float = 0f,
    var previousVisualPoint: VisualPoint? = null,
    var drawn: Boolean = false
) {
    lateinit var path: Path
}