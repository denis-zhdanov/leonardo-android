package tech.harmonysoft.oss.leonardo.view.navigator

import tech.harmonysoft.oss.leonardo.model.runtime.ChartModel
import tech.harmonysoft.oss.leonardo.view.chart.ChangeAnchor

interface NavigatorShowcase {

    /**
     * Anchor to use for [getting showcase&#39; data][ChartModel]
     */
    val dataAnchor: Any

    val visualXShift: Float

    fun applyVisualXChange(deltaVisualX: Float, anchor: ChangeAnchor)
}