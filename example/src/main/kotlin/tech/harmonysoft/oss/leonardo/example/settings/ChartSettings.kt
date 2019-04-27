package tech.harmonysoft.oss.leonardo.example.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.data.ChartDataSource

class ChartSettings(
    private val chartName: String,
    private val preferences: SharedPreferences
) {

    fun enableDataSource(dataSource: ChartDataSource) {
        preferences.edit {
            putBoolean(getDataSourceKey(dataSource), true)
        }
    }

    fun disableDataSource(dataSource: ChartDataSource) {
        preferences.edit {
            putBoolean(getDataSourceKey(dataSource), false)
        }
    }

    private fun getDataSourceKey(dataSource: ChartDataSource): String {
        return "$chartName.${dataSource.legend}.enabled"
    }

    fun isDataSourceEnabled(dataSource: ChartDataSource): Boolean {
        return preferences.getBoolean(getDataSourceKey(dataSource), true)
    }

    fun getRange(marker: String): Range? {
        val start = preferences.getLong("$chartName.$marker.start", 1L)
        val end = preferences.getLong("$chartName.$marker.end", 0L)
        return if (start <= end) {
            Range(start, end)
        } else {
            null
        }
    }

    fun storeRange(marker: String, range: Range) {
        preferences.edit {
            putLong("$chartName.$marker.start", range.start)
            putLong("$chartName.$marker.end", range.end)
        }
    }
}