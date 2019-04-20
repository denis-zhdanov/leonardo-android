package tech.harmonysoft.oss.leonardo.example.settings

import android.content.Context
import android.content.SharedPreferences
import tech.harmonysoft.oss.leonardo.example.R

class SettingsManager(private val context: Context) {

    private val preferences: SharedPreferences
        get() {
            return context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        }

    var theme: ActiveTheme
        get() {
            return ActiveTheme.valueOf(preferences.getString(Key.ACTIVE_THEME, ActiveTheme.LIGHT.name)!!)
        }
        set(value) {
            store {
                putString(Key.ACTIVE_THEME, value.name)
            }
        }

    val chartStyle: Int
        get() {
            return when (theme) {
                ActiveTheme.LIGHT -> R.style.Charts_Light
                ActiveTheme.DARK -> R.style.Charts_Dark
            }
        }

    var chartType: ChartType
        get() {
            return ChartType.valueOf(preferences.getString(Key.CHART_TYPE, ChartType.STATIC.name)!!)
        }
        set(value) {
            store {
                putString(Key.CHART_TYPE, value.name)
            }
        }

    private fun store(action: SharedPreferences.Editor.() -> Unit) {
        val editor = preferences.edit()
        try {
            action(editor)
        } finally {
            editor.apply()
        }
    }

    companion object {
        const val PREFERENCE_NAME = "Leonardo"
    }

    private object Key {
        const val ACTIVE_THEME = "ACTIVE_THEME"
        const val CHART_TYPE = "CHART_TYPE"
    }
}