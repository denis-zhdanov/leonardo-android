package tech.harmonysoft.oss.leonardo.model.data

interface ChartDataLoader {

    /**
     * Loads target data. Is assumed to be called from a non-main thread.
     *
     * @param from      target X start (inclusive)
     * @param to        target X end (inclusive)
     * @param handle    load handle to use
     */
    fun load(from: Long, to: Long, handle: LoadHandle)
}