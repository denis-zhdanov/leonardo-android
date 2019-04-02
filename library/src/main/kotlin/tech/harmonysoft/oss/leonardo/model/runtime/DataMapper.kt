package tech.harmonysoft.oss.leonardo.model.runtime

import tech.harmonysoft.oss.leonardo.model.DataPoint
import tech.harmonysoft.oss.leonardo.model.VisualPoint

interface DataMapper {

    fun dataXToVisualX(dataX: Long): Float

    fun visualXToDataX(visualX: Float): Long

    fun dataPointToVisualPoint(dataPoint: DataPoint): VisualPoint
}