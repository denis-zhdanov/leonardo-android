package tech.harmonysoft.oss.leonardo.example.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.google.common.eventbus.EventBus
import kotlinx.android.synthetic.main.activity_example.*
import tech.harmonysoft.oss.leonardo.example.LeonardoApplication
import tech.harmonysoft.oss.leonardo.example.R
import tech.harmonysoft.oss.leonardo.example.data.InfiniteChartDataLoader
import tech.harmonysoft.oss.leonardo.example.data.ChartData
import tech.harmonysoft.oss.leonardo.example.event.ThemeChangedEvent
import tech.harmonysoft.oss.leonardo.example.settings.ActiveTheme
import tech.harmonysoft.oss.leonardo.example.settings.SettingsManager
import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.config.axis.impl.DefaultValueRepresentationStrategy
import tech.harmonysoft.oss.leonardo.model.data.ChartDataSource
import tech.harmonysoft.oss.leonardo.model.util.LeonardoUtil
import javax.inject.Inject

@SuppressLint("InflateParams")
class ExampleActivity : AppCompatActivity() {

    @Inject lateinit var eventBus: EventBus
    @Inject lateinit var settingsManager: SettingsManager
    @Inject lateinit var chartInitializer: ChartInitializer

    private lateinit var rootContent: View

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
        rootContent = findViewById(R.id.root_content)
        setupActionBar()
        setupChart()
        initThemeSwitcher()

        setTheme(when (settingsManager.theme) {
                     ActiveTheme.LIGHT -> R.style.AppTheme_Light
                     ActiveTheme.DARK -> R.style.AppTheme_Dark
                 })
        refreshTheme()
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar)
    }

    private fun setupChart() {
        val chartData = ChartData(
                resources.getString(R.string.chart_title),
                Range(-100L, 100L),
                createDataSources(),
                DefaultValueRepresentationStrategy.INSTANCE
        )
        chartInitializer.prepareChart(chartData, findViewById(R.id.chart_content), this, layoutInflater)
    }

    private fun createDataSources(): Collection<ChartDataSource> {
        return listOf(
                ChartDataSource("first",
                                ResourcesCompat.getColor(resources, R.color.plot1, null),
//                                InfiniteChartDataLoader(2000)),
                                InfiniteChartDataLoader()),
                ChartDataSource("second",
                                ResourcesCompat.getColor(resources, R.color.plot2, null),
                                InfiniteChartDataLoader())
        )
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
        toolbar.setTitleTextColor(LeonardoUtil.getColor(this, currentChartStyle, R.attr.action_bar_text_color))
        toolbar.setBackgroundColor(LeonardoUtil.getColor(this, currentChartStyle, R.attr.action_bar_background_color))
        rootContent.setBackgroundColor(LeonardoUtil.getColor(this, currentChartStyle, R.attr.leonardo_chart_background_color))
    }
}