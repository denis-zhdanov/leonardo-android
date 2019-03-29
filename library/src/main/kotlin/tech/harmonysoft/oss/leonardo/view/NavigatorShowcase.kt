package tech.harmonysoft.oss.leonardo.view

import tech.harmonysoft.oss.leonardo.model.runtime.ChartModel

/**
 * @author Denis Zhdanov
 * @since 28/3/19
 */
interface NavigatorShowcase {

    /**
     * Anchor to use for [getting showcase&#39; data][ChartModel]
     */
    val dataAnchor: Any

    val visualXShift: Float

    fun scrollHorizontally(deltaVisualX: Float)
}