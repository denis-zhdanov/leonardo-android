package tech.harmonysoft.oss.leonardo.example.view.dynamic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import kotlinx.android.synthetic.main.fragment_infinite_chart.*
import tech.harmonysoft.oss.leonardo.example.LeonardoApplication
import tech.harmonysoft.oss.leonardo.example.R
import tech.harmonysoft.oss.leonardo.example.data.ModelHolder
import tech.harmonysoft.oss.leonardo.example.data.input.infinite.InfiniteChartDataLoader
import tech.harmonysoft.oss.leonardo.example.event.ThemeChangedEvent
import tech.harmonysoft.oss.leonardo.example.scroll.ScrollManager
import tech.harmonysoft.oss.leonardo.example.settings.SettingsManager
import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.config.LeonardoConfigFactory
import tech.harmonysoft.oss.leonardo.model.data.ChartDataSource
import tech.harmonysoft.oss.leonardo.model.runtime.ChartModel
import tech.harmonysoft.oss.leonardo.model.runtime.impl.ChartModelImpl
import tech.harmonysoft.oss.leonardo.model.util.LeonardoUtil
import tech.harmonysoft.oss.leonardo.view.navigator.NavigatorChartView
import tech.harmonysoft.oss.leonardo.view.chart.ChartView
import tech.harmonysoft.oss.leonardo.view.navigator.ScrollListener
import javax.inject.Inject

class DynamicChartFragment : Fragment() {

    @Inject lateinit var eventBus: EventBus
    @Inject lateinit var settingsManager: SettingsManager
    @Inject lateinit var scrollManager: ScrollManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LeonardoApplication.graph.inject(this)
        eventBus.register(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_infinite_chart, container).apply {
            val chart = findViewById<ChartView>(R.id.infinite_chart)
            val navigator = findViewById<NavigatorChartView>(R.id.dynamic_chart_navigator)

            navigator.scrollListener = object : ScrollListener {
                override fun onStarted() {
                    scrollManager.scrollOwner = navigator
                }

                override fun onStopped() {
                    scrollManager.scrollOwner = null
                }
            }

            initUi(chart, navigator, settingsManager.chartStyle)

            val model = createModel()
            ModelHolder.model = model
            chart.apply(model)
            navigator.apply(model)
            model.setActiveRange(Range(-100L, 100L), navigator)
        }
    }


    private fun initUi(chart: ChartView, navigator: NavigatorChartView, style: Int) {
        val chartConfig = LeonardoConfigFactory.newChartConfigBuilder()
            .withStyle(style)
            .withContext(context!!)
            .withXAxisConfigBuilder(LeonardoConfigFactory.newAxisConfigBuilder().withStyle(style))
            .build()

        chart.apply(chartConfig)

        val navigatorConfig = LeonardoConfigFactory.newNavigatorConfigBuilder()
            .withStyle(style)
            .withContext(context!!.applicationContext)
            .build()
        navigator.apply(navigatorConfig, chartConfig)
        navigator.apply(LeonardoUtil.asNavigatorShowCase(chart))
    }

    private fun createModel(): ChartModel {
        val model = ChartModelImpl(3)
        model.addDataSource(ChartDataSource("first",
                                            ResourcesCompat.getColor(resources, R.color.plot1, null),
                                            InfiniteChartDataLoader()))
        model.addDataSource(ChartDataSource("second",
                                            ResourcesCompat.getColor(resources, R.color.plot2, null),
                                            InfiniteChartDataLoader()))
//        model.addDataSource(ChartDataSource("third",
//                                            ResourcesCompat.getColor(resources, R.color.plot3, null),
//                                            InfiniteChartDataLoader()))
//        model.addDataSource(ChartDataSource("forth",
//                                            ResourcesCompat.getColor(resources, R.color.plot4, null),
//                                            InfiniteChartDataLoader()))
        return model
    }

    @Subscribe
    fun onThemeChanged(event: ThemeChangedEvent) {
        val style = settingsManager.chartStyle
        initUi(infinite_chart, dynamic_chart_navigator, style)
    }
}