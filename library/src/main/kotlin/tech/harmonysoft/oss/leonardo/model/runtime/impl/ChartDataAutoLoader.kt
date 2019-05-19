package tech.harmonysoft.oss.leonardo.model.runtime.impl

import android.annotation.SuppressLint
import android.os.AsyncTask
import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.data.ChartDataSource
import tech.harmonysoft.oss.leonardo.model.runtime.ChartModelListener

class ChartDataAutoLoader(private val model: ChartModelImpl) : ChartModelListener {

    private val tasks = mutableSetOf<ChartDataLoadTask>()

    private var lastKnownBufferRange = Range.EMPTY_RANGE

    init {
        model.addListener(this)
    }

    override fun onRangeChanged(anchor: Any) {
        mayBeLoadRanges()
    }

    override fun onDataSourceEnabled(dataSource: ChartDataSource) {
    }

    override fun onDataSourceDisabled(dataSource: ChartDataSource) {
    }

    override fun onDataSourceAdded(dataSource: ChartDataSource) {
        mayBeLoadRanges()
    }

    override fun onDataSourceRemoved(dataSource: ChartDataSource) {
    }

    override fun onActiveDataPointsLoaded(anchor: Any) {
    }

    override fun onSelectionChange() {
    }

    private fun mayBeLoadRanges() {
        val bufferRange = model.bufferRange

        if (bufferRange.empty
            || (bufferRange.start >= lastKnownBufferRange.start && bufferRange.end <= lastKnownBufferRange.end)
        ) {
            return
        }

        lastKnownBufferRange = bufferRange

        for (dataSource in model.registeredDataSources) {
            val loadedRanges = model.getLoadedRanges(dataSource)
            val rangesToLoad = loadedRanges.getMissing(bufferRange)
            val min = model.getMin(dataSource)
            val max = model.getMax(dataSource)

            for (rangeToLoad in rangesToLoad) {
                if ((min != null && rangeToLoad.end < min) || (max != null && max < rangeToLoad.start)) {
                    continue
                }

                val startToUse = if (min == null || rangeToLoad.start >= min) {
                    rangeToLoad.start
                } else {
                    min
                }

                val endToUse = if (max == null || rangeToLoad.end <= max) {
                    rangeToLoad.end
                } else {
                    max
                }

                val rangeToUse = if (startToUse == rangeToLoad.start && endToUse == rangeToLoad.end) {
                    rangeToLoad
                } else {
                    Range(startToUse, endToUse)
                }

                val task = ChartDataLoadTask(dataSource, rangeToUse)
                tasks += task
                task.execute(null)
            }
        }
    }

    fun isLoadingInProgress(dataSource: ChartDataSource, start: Long, end: Long): Boolean {
        return tasks.any { task ->
            task.dataSource == dataSource && (task.range.contains(start) || task.range.contains(end))
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class ChartDataLoadTask(
        val dataSource: ChartDataSource,
        val range: Range
    ) : AsyncTask<Void?, Void?, Void?>() {

        private val handle = LoadHandleImpl()

        override fun doInBackground(vararg dummy: Void?): Void? {
            dataSource.loader.load(range.start, range.end, handle)
            return null
        }

        override fun onPostExecute(dummy: Void?) {
            tasks.remove(this)

            val min = handle.minimum.get()
            if (min != null) {
                model.setMin(dataSource, min)
            }

            val max = handle.maximum.get()
            if (max != null) {
                model.setMax(dataSource, max)
            }

            val points = handle.points
            if (points.isNotEmpty()) {
                model.onPointsLoaded(dataSource, range, points)
            }
        }
    }

}