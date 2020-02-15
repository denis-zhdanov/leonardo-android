package tech.harmonysoft.oss.leonardo.view.util

import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.text.ValueRepresentationStrategy

class AxisStepChooser {

    fun choose(textStrategy: ValueRepresentationStrategy,
               minGapStrategy: GapStrategy,
               currentRange: Range,
               availableVisualSpace: Int,
               measurer: TextSpaceMeasurer
    ): Long {
        var low = 1L
        var high = if (currentRange.size >= 4) {
            currentRange.size / 4
        } else {
            10
        }
        while (true) {
            val checkResult = currentRange.findFirstStepValue(high)?.let {
                checkStep(textStrategy,
                          measurer,
                          minGapStrategy,
                          currentRange,
                          availableVisualSpace,
                          high,
                          it)
            } ?: CheckResult.TOO_BIG
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
            val checkResult = currentRange.findFirstStepValue(high)?.let {
                checkStep(textStrategy,
                          measurer,
                          minGapStrategy,
                          currentRange,
                          availableVisualSpace,
                          candidate,
                          it)
            } ?: CheckResult.TOO_BIG
            when (checkResult) {
                CheckResult.OK        -> return candidate
                CheckResult.TOO_BIG   -> high = candidate
                CheckResult.TOO_SMALL -> low = candidate
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