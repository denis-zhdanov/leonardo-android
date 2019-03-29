package tech.harmonysoft.oss.leonardo.model.text

/**
 * Defines contract for a custom strategy to map chart data to human-readable values.
 *
 * @author Denis Zhdanov
 * @since 26/3/19
 */
interface ValueRepresentationStrategy {

    /**
     * Allows getting a custom value to use for the axis label. E.g. suppose our data points use time as X.
     * Then we can map the value to a human-readable text like `'16:42'`.
     *
     * @param value     target point's value
     * @param step      a step used by the current axis. E.g. when we draw time stored in seconds, we'd like
     *                  to use seconds label if possible, like `'12:23:43'`. 'Step' value would be `'1'`
     *                  then. However, it might be that we have so many data points shown at the moment,
     *                  that it would be reasonable to fallback to seconds, like `'12:23'`. 'Step' would
     *                  be `'60'` in this case. Further on, we can fallback to hours (step `3600`) etc.
     *
     * @return          human-readable text representation of the target value with the given step
     */
    fun getLabel(value: Long, step: Long): TextWrapper

    /**
     * There is a possible case that particular value is rather big and occupies a lot of visual space,
     * e.g. *123432543*. We might want to show minified value instead, e.g. *~123M*.
     *
     * This method is used for getting such minified labels.
     */
    fun getMinifiedLabel(value: Long, step: Long): TextWrapper
}