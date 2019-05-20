package tech.harmonysoft.oss.leonardo.model.data

interface ChartDataLoader {

    /**
     * Loads target data. Is assumed to be called from a non-main thread.
     *
     * The implementation is free to choose whether to perform the actual loading in the calling background
     * thread or to do it in another thread (e.g. using an existing library). The only requirement is to
     * call [LoadHandle.onLoadingEnd] when the loading is done.
     *
     * @param from      target X start (inclusive)
     * @param to        target X end (inclusive)
     * @param handle    load handle to use
     */
    fun load(from: Long, to: Long, handle: LoadHandle)
}