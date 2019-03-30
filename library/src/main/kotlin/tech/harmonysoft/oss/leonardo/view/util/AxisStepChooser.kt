package tech.harmonysoft.oss.leonardo.view.util

import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.text.ValueRepresentationStrategy

/**
 * @author Denis Zhdanov
 * @since 27/3/19
 */
class AxisStepChooser {

    fun choose(textStrategy: ValueRepresentationStrategy,
               minGapStrategy: GapStrategy,
               currentRange: Range,
               availableVisualSpace: Int,
               measurer: TextSpaceMeasurer
    ): Long {
        var low: Long = 1
        var high: Long = 10
        while (true) {
            val value = currentRange.findFirstStepValue(high)
            val checkResult = checkStep(textStrategy,
                                        measurer,
                                        minGapStrategy,
                                        currentRange,
                                        availableVisualSpace,
                                        high,
                                        value)
            if (checkResult == CheckResult.OK) {
                return high
            } else if (checkResult == CheckResult.TOO_BIG) {
                break
            } else {
                low = high
                high *= 10
            }
        }

        while (low <= high) {
            val candidate = low + (high - low) / 2
            if (candidate == low) {
                return low
            }
            val value = currentRange.findFirstStepValue(high)
            val checkResult = checkStep(textStrategy,
                                        measurer,
                                        minGapStrategy,
                                        currentRange,
                                        availableVisualSpace,
                                        candidate,
                                        value)
            when (checkResult) {
                AxisStepChooser.CheckResult.OK        -> return candidate
                AxisStepChooser.CheckResult.TOO_BIG   -> high = candidate
                AxisStepChooser.CheckResult.TOO_SMALL -> low = candidate
            }
        }
        return low
    }

    private fun checkStep(textStrategy: ValueRepresentationStrategy,
                          measurer: TextSpaceMeasurer,
                          minGapStrategy: GapStrategy,
                          range: Range,
                          availableVisualSpace: Int,
                          step: Long,
                          firstValue: Long): CheckResult {
        var remainingVisualSpace = availableVisualSpace
        if (firstValue == Long.MIN_VALUE) {
            return CheckResult.TOO_BIG
        }
        var labelsNumber = 0
        var value = firstValue
        while (range.contains(value) && remainingVisualSpace >= 0) {
            val label = textStrategy.getLabel(value, step)
            val labelSize = measurer.measureVisualSpace(label)
            remainingVisualSpace -= labelSize
            value += step
            labelsNumber++
            if (value > range.end) {
                break
            }
            if (value != firstValue) {
                remainingVisualSpace -= minGapStrategy(labelSize)
            }
        }
        return if (remainingVisualSpace >= 0) {
            if (labelsNumber > 2) {
                CheckResult.OK
            } else {
                CheckResult.TOO_BIG
            }
        } else {
            CheckResult.TOO_SMALL
        }
    }


    private enum class CheckResult {
        TOO_BIG, TOO_SMALL, OK
    }

    companion object {
        val INSTANCE = AxisStepChooser()
    }
}