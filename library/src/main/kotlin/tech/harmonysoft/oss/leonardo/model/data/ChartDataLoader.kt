package tech.harmonysoft.oss.leonardo.model.data

import tech.harmonysoft.oss.leonardo.model.DataPoint
import tech.harmonysoft.oss.leonardo.model.Range

interface ChartDataLoader {

    /**
     * Loads target data. Is assumed to be called from a non-main thread.
     *
     * @param range     target X range
     * @return          an interval for the target X range if it's within the current dataset's range;
     *                  `null` otherwise
     */
    fun load(range: Range): Collection<DataPoint>?
}