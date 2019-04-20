package tech.harmonysoft.oss.leonardo.view.navigator

import tech.harmonysoft.oss.leonardo.model.runtime.ChartModel

interface NavigatorShowcase {

    /**
     * Anchor to use for [getting showcase&#39; data][ChartModel]
     */
    val dataAnchor: Any

    val visualXShift: Float

    fun scrollHorizontally(deltaVisualX: Float)
}