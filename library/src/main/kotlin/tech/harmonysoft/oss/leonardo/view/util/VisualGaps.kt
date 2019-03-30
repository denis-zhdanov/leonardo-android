package tech.harmonysoft.oss.leonardo.view.util

/**
 * @author Denis Zhdanov
 * @since 27/3/19
 */

typealias GapStrategy = (Int) -> Int

val X_AXIS_LABEL_GAP_STRATEGY: GapStrategy = { it * 5 / 3 }

fun getYLabelGapStrategy(additionalPaddingSupplier: () -> Int): GapStrategy {
    return {
        it * 5 + additionalPaddingSupplier()
    }
}