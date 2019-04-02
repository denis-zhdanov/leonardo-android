package tech.harmonysoft.oss.leonardo.view.util

typealias GapStrategy = (Int) -> Int

val X_AXIS_LABEL_GAP_STRATEGY: GapStrategy = { it * 5 / 3 }
val Y_AXIS_LABEL_GAP_STRATEGY: GapStrategy = { it * 5 }