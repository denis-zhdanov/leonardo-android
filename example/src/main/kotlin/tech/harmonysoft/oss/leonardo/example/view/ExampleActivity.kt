package tech.harmonysoft.oss.leonardo.example.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.google.common.eventbus.EventBus
import kotlinx.android.synthetic.main.activity_example.*
import tech.harmonysoft.oss.leonardo.example.LeonardoApplication
import tech.harmonysoft.oss.leonardo.example.R
import tech.harmonysoft.oss.leonardo.example.event.ThemeChangedEvent
import tech.harmonysoft.oss.leonardo.example.settings.*
import tech.harmonysoft.oss.leonardo.model.util.LeonardoUtil
import javax.inject.Inject

@SuppressLint("InflateParams")
class ExampleActivity : AppCompatActivity() {

    @Inject lateinit var eventBus: EventBus
    @Inject lateinit var settingsManager: SettingsManager

    private val staticChartView: View by lazy {
        layoutInflater.inflate(R.layout.layout_static_chart, null)
    }

    private val dynamicChartView: View by lazy {
        layoutInflater.inflate(R.layout.layout_dynamic_chart, null)
    }

    private val darkThemeActive: Boolean
        get() {
            return TypedValue().let {
                theme.resolveAttribute(R.attr.theme_name, it, true)
                "dark" == it.string?.toString()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LeonardoApplication.graph.inject(this)
        setContentView(R.layout.activity_example)
        restoreState()
        setupActionBar()
        initDrawer()
        initThemeSwitcher()

        setTheme(when (settingsManager.theme) {
                     ActiveTheme.LIGHT -> R.style.AppTheme_Light
                     ActiveTheme.DARK -> R.style.AppTheme_Dark
                 })
        refreshTheme()
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu)
        }
    }

    private fun initDrawer() {
        navigation.setNavigationItemSelectedListener { menuItem ->
            menuItem.isChecked = true
            drawer_layout.closeDrawers()
            val chartType = when (menuItem.itemId) {
                R.id.static_chart -> ChartType.STATIC
                R.id.dynamic_chart -> ChartType.DYNAMIC
                else -> null
            }
            if (chartType != null) {
                mayBeChangeChartContent(chartType)
            }
            true
        }
    }

    private fun mayBeChangeChartContent(chartType: ChartType) {
        if (chart_content.childCount > 0 && chartType == settingsManager.activeChartType) {
            return
        }

        val (viewToShow, menuItemId) = when (chartType) {
            ChartType.DYNAMIC -> dynamicChartView to R.id.static_chart
            ChartType.STATIC -> staticChartView to R.id.dynamic_chart
        }
        if (chart_content.childCount == 0) {
            chart_content.addView(viewToShow)
        } else if (chart_content.getChildAt(0) != viewToShow) {
            chart_content.removeAllViews()
            chart_content.addView(viewToShow)
        }

        navigation.menu.findItem(menuItemId).isChecked = true
        settingsManager.activeChartType = chartType
    }

    private fun initThemeSwitcher() {
        theme_switcher.setOnClickListener {
            val currentTheme = if (darkThemeActive) {
                setTheme(R.style.AppTheme_Light)
                ActiveTheme.LIGHT
            } else {
                setTheme(R.style.AppTheme_Dark)
                ActiveTheme.DARK
            }

            settingsManager.theme = currentTheme

            refreshTheme()

            eventBus.post(ThemeChangedEvent(currentTheme, settingsManager.chartStyle))
        }
    }

    private fun refreshTheme() {
        val image = when (settingsManager.theme) {
            ActiveTheme.LIGHT -> R.drawable.ic_moon
            ActiveTheme.DARK -> R.drawable.ic_sun
        }
        theme_switcher.setImageResource(image)
        val currentChartStyle = settingsManager.chartStyle
        drawer_layout.setBackgroundColor(LeonardoUtil.getColor(this,
                                                               currentChartStyle,
                                                               android.R.attr.windowBackground))
        toolbar.setTitleTextColor(LeonardoUtil.getColor(this, currentChartStyle, R.attr.action_bar_text_color))
        toolbar.setBackgroundColor(LeonardoUtil.getColor(this, currentChartStyle, R.attr.action_bar_background_color))

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                navigation.setCheckedItem(when (settingsManager.activeChartType) {
                                              ChartType.DYNAMIC -> R.id.dynamic_chart
                                              ChartType.STATIC -> R.id.static_chart
                                          })
                drawer_layout.openDrawer(GravityCompat.START)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        restoreState()
    }

    private fun restoreState() {
        mayBeChangeChartContent(settingsManager.activeChartType)
    }
}