package tech.harmonysoft.oss.leonardo.example.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import tech.harmonysoft.oss.leonardo.example.R
import tech.harmonysoft.oss.leonardo.example.event.ThemeChangedEvent
import tech.harmonysoft.oss.leonardo.example.settings.SettingsManager
import tech.harmonysoft.oss.leonardo.model.util.LeonardoUtil

class SeparatorViewFactory(
    private val settingsManager: SettingsManager,
    eventBus: EventBus
) {

    private val separators = mutableListOf<Pair<View, Context>>()

    init {
        eventBus.register(this)
    }

    fun createSeparator(context: Context): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 3)
            setBackgroundColor(LeonardoUtil.getColor(context,
                                                     settingsManager.chartStyle,
                                                     R.attr.leonardo_chart_grid_color))
            separators += this to context
        }
    }

    @Subscribe
    fun onThemeChanged(event: ThemeChangedEvent) {
        for ((view, context) in separators) {
            val color = LeonardoUtil.getColor(context,
                                              event.currentStyle,
                                              R.attr.leonardo_chart_grid_color)
            view.setBackgroundColor(color)
            view.invalidate()
        }
    }
}