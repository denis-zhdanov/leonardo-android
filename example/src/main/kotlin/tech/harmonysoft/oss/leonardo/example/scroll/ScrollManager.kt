package tech.harmonysoft.oss.leonardo.example.scroll

import tech.harmonysoft.oss.leonardo.view.navigator.NavigatorChartView

class ScrollManager {

    var scrollOwner: NavigatorChartView? = null
        set(value) {
            if (!skipStopScroll && field != null && field != value) {
                skipStopScroll = true
                field?.stopAction()
                skipStopScroll = false
            }
            field = value
        }

    private var skipStopScroll = false
}