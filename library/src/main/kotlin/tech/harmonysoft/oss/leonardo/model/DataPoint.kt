package tech.harmonysoft.oss.leonardo.model

data class DataPoint(val x: Long, val y: Long) : WithComparableLongProperty {

    override val property get() = x
}