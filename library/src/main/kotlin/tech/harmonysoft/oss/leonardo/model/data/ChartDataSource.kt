package tech.harmonysoft.oss.leonardo.model.data

/**
 * @author Denis Zhdanov
 * @since 26/3/19
 */
data class ChartDataSource(
    val legend: String,
    val color: Int,
    val loader: ChartDataLoader
)