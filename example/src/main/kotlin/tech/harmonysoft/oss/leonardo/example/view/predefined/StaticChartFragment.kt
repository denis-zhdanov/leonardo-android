package tech.harmonysoft.oss.leonardo.example.view.predefined

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import tech.harmonysoft.oss.leonardo.example.LeonardoApplication
import tech.harmonysoft.oss.leonardo.example.R
import tech.harmonysoft.oss.leonardo.example.data.input.predefined.JsonDataSourceParser
import tech.harmonysoft.oss.leonardo.example.event.ThemeChangedEvent
import tech.harmonysoft.oss.leonardo.example.scroll.ScrollManager
import tech.harmonysoft.oss.leonardo.example.settings.SettingsManager
import tech.harmonysoft.oss.leonardo.model.config.LeonardoConfigFactory
import tech.harmonysoft.oss.leonardo.model.config.axis.impl.TimeValueRepresentationStrategy
import tech.harmonysoft.oss.leonardo.model.runtime.impl.ChartModelImpl
import tech.harmonysoft.oss.leonardo.model.util.LeonardoUtil
import tech.harmonysoft.oss.leonardo.view.navigator.NavigatorChartView
import tech.harmonysoft.oss.leonardo.view.chart.ChartView
import tech.harmonysoft.oss.leonardo.view.navigator.ScrollListener
import javax.inject.Inject

class StaticChartFragment : Fragment() {

    @Inject lateinit var eventBus: EventBus
    @Inject lateinit var settingsManager: SettingsManager
    @Inject lateinit var scrollManager: ScrollManager

    private val chartViews = mutableListOf<Pair<ChartView, NavigatorChartView>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LeonardoApplication.graph.inject(this)
        eventBus.register(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_static_chart, container).apply {
            initUi(findViewById(R.id.charts))
        }
    }

    private fun initUi(holder: ViewGroup) {
        val charts = JsonDataSourceParser().parse(resources.openRawResource(R.raw.input))
        charts.forEach { chartData ->
            val row = layoutInflater.inflate(R.layout.layout_chart_row, holder, false)

            val chart = row.findViewById<ChartView>(R.id.row_chart)
            val navigator = row.findViewById<NavigatorChartView>(R.id.row_navigator)
            chartViews += chart to navigator

            navigator.scrollListener = object : ScrollListener {
                override fun onStarted() {
                    scrollManager.scrollOwner = navigator
                }

                override fun onStopped() {
                    scrollManager.scrollOwner = null
                }
            }

            navigator.apply(LeonardoUtil.asNavigatorShowCase(chart))
            applyUiSettings(chart, navigator, settingsManager.chartStyle, holder.context)

            val model = ChartModelImpl()
            chartData.dataSources.forEach {
                model.addDataSource(it)
            }
            chart.apply(model)
            navigator.apply(model)
            model.setActiveRange(chartData.xRange, navigator.dataAnchor)

            row.findViewById<TextView>(R.id.row_title).text = chartData.name

            holder.addView(row)
        }
    }

    private fun applyUiSettings(chart: ChartView, navigator: NavigatorChartView, style: Int, context: Context) {
        val chartConfig = LeonardoConfigFactory.newChartConfigBuilder()
            .withStyle(style)
            .withContext(context)
            .withXAxisConfigBuilder(LeonardoConfigFactory.newAxisConfigBuilder()
                                        .withStyle(style)
                                        .withLabelTextStrategy(TimeValueRepresentationStrategy.INSTANCE))
            .build()

        chart.apply(chartConfig)

        val navigatorConfig = LeonardoConfigFactory.newNavigatorConfigBuilder()
            .withStyle(style)
            .withContext(context)
            .build()
        navigator.apply(navigatorConfig, chartConfig)
    }

    @Subscribe
    fun onThemeChanged(event: ThemeChangedEvent) {
        val style = settingsManager.chartStyle
        for ((chart, navigator) in chartViews) {
            applyUiSettings(chart, navigator, style, context!!)
        }
    }
}