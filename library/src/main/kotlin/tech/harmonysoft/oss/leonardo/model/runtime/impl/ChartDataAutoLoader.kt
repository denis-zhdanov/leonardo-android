package tech.harmonysoft.oss.leonardo.model.runtime.impl

import android.annotation.SuppressLint
import android.os.AsyncTask
import tech.harmonysoft.oss.leonardo.model.DataPoint
import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.data.ChartDataSource
import tech.harmonysoft.oss.leonardo.model.runtime.ChartModel
import tech.harmonysoft.oss.leonardo.model.runtime.ChartModelListener

class ChartDataAutoLoader(private val model: ChartModel) : ChartModelListener {

    private val tasks = mutableSetOf<ChartDataLoadTask>()

    init {
        model.addListener(this)
        mayBeLoadRanges()
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
        for (task in tasks) {
            task.cancel(true)
        }
        tasks.clear()
        val bufferRange = model.bufferRange

        if (bufferRange.empty) {
            return
        }
        for (dataSource in model.registeredDataSources) {
            val loadedRanges = model.getLoadedRanges(dataSource)
            val rangesToLoad = loadedRanges.getMissing(bufferRange)

            for (rangeToLoad in rangesToLoad) {
                val task = ChartDataLoadTask()
                tasks += task
                task.execute(LoadRequest(dataSource, rangeToLoad))
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class ChartDataLoadTask : AsyncTask<LoadRequest, Void, LoadResult>() {

        override fun doInBackground(vararg requests: LoadRequest): LoadResult {
            if (requests.size != 1) {
                throw IllegalArgumentException("Expected to get a single load request but got ${requests.size}")
            }
            val request = requests[0]
            val points = request.dataSource.loader.load(request.range)
            return LoadResult(points ?: emptyList(), request.range, request.dataSource)
        }

        override fun onPostExecute(loadResult: LoadResult) {
            tasks.remove(this)
            model.onPointsLoaded(loadResult.source, loadResult.range, loadResult.points)
        }
    }

    private data class LoadRequest(val dataSource: ChartDataSource, val range: Range)

    private data class LoadResult(val points: Collection<DataPoint>, val range: Range, val source: ChartDataSource)
}