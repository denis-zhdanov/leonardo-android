package tech.harmonysoft.oss.leonardo.model.runtime

interface DataMapper {

    fun dataXToVisualX(dataX: Long): Float

    fun visualXToDataX(visualX: Float): Long
}