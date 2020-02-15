package tech.harmonysoft.oss.leonardo.example.data.input.infinite

import tech.harmonysoft.oss.leonardo.model.data.ChartDataLoader
import tech.harmonysoft.oss.leonardo.model.data.LoadHandle
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class InfiniteChartDataLoader(private val delayMs: Long? = null) : ChartDataLoader {

    private val cache = ConcurrentHashMap<Long, Long>()

    override fun load(from: Long, to: Long, handle: LoadHandle) {
        val action = { doLoad(from, to, handle) }
        if (delayMs == null) {
            action()
        } else {
            executor.schedule(action, delayMs, TimeUnit.MILLISECONDS)
        }
    }

    private fun doLoad(from: Long, to: Long, handle: LoadHandle) {
        var prevY: Long? = null
        val random = Random()
        for (x in (from..to)) {
            val cachedY = cache[x]
            if (cachedY == null) {
                val sign = if (random.nextBoolean()) 1 else -1
                val shift = sign * random.nextInt(Y_RANGE_LENGTH / 10) + Y_MIN.toLong()
                val y = shift + (prevY ?: 0)
                cache[x] = y
                handle.onPointLoaded(x, y)
                prevY = y
            } else {
                handle.onPointLoaded(x, cachedY)
                prevY = cachedY
            }
        }
        handle.onLoadingEnd()
    }

    companion object {
        private const val Y_MIN = 0
        private const val Y_MAX = 120
        private const val Y_RANGE_LENGTH = Y_MAX - Y_MIN + 1

        private val executor = Executors.newScheduledThreadPool(1)
    }
}