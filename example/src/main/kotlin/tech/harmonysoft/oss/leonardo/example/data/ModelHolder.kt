package tech.harmonysoft.oss.leonardo.example.data

import tech.harmonysoft.oss.leonardo.model.runtime.ChartModel
import kotlin.properties.Delegates.observable

/**
 * @author Denis Zhdanov
 * @since 31/3/19
 */
object ModelHolder {

    var model: ChartModel? by observable(null) { _, _: ChartModel?, newValue: ChartModel? ->
        callback?.invoke(newValue)
    }

    var callback: ((ChartModel?) -> Unit)? = null
}