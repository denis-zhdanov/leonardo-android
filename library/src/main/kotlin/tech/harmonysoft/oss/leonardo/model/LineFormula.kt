package tech.harmonysoft.oss.leonardo.model

/**
 * @author Denis Zhdanov
 * @since 26/3/19
 */
data class LineFormula(val a: Float, val b: Float) {

    fun getY(x: Float): Float {
        return a * x + b
    }

    fun getX(y: Float): Float {
        return (y - b) / a
    }
}