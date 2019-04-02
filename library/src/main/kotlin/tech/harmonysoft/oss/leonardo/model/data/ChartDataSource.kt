package tech.harmonysoft.oss.leonardo.model.data

data class ChartDataSource(
    val legend: String,
    val color: Int,
    val loader: ChartDataLoader
)