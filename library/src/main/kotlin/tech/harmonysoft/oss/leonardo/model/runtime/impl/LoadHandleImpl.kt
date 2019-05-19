package tech.harmonysoft.oss.leonardo.model.runtime.impl

import tech.harmonysoft.oss.leonardo.model.DataPoint
import tech.harmonysoft.oss.leonardo.model.data.LoadHandle
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class LoadHandleImpl : LoadHandle {

    private val pointsRef = AtomicReference<MutableCollection<DataPoint>>(mutableListOf())

    val points: Collection<DataPoint>
        get() {
            return pointsRef.get()
        }

    val minimum = AtomicReference<Long?>()
    val maximum = AtomicReference<Long?>()

    private val _cancelled = AtomicBoolean()

    override val cancelled get() = _cancelled.get()

    override fun onMinimumValue(value: Long) {
        minimum.set(value)
    }

    override fun onMaximumValue(value: Long) {
        maximum.set(value)
    }

    override fun onPointLoaded(x: Long, y: Long) {
        pointsRef.get().add(DataPoint(x, y))
    }

    override fun onPointsLoaded(points: Iterable<DataPoint>) {
        pointsRef.get().addAll(points)
    }
}