package tech.harmonysoft.oss.leonardo.example.view.dynamic

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import tech.harmonysoft.oss.leonardo.example.R
import tech.harmonysoft.oss.leonardo.example.data.ModelHolder
import tech.harmonysoft.oss.leonardo.example.data.input.infinite.InfiniteChartDataLoader
import tech.harmonysoft.oss.leonardo.example.event.LeonardoEvents
import tech.harmonysoft.oss.leonardo.example.event.LeonardoKey
import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.config.LeonardoConfigFactory
import tech.harmonysoft.oss.leonardo.model.data.ChartDataSource
import tech.harmonysoft.oss.leonardo.model.runtime.ChartModel
import tech.harmonysoft.oss.leonardo.model.runtime.impl.ChartModelImpl
import tech.harmonysoft.oss.leonardo.model.util.LeonardoUtil
import tech.harmonysoft.oss.leonardo.view.NavigatorChartView
import tech.harmonysoft.oss.leonardo.view.chart.ChartView

class DynamicChartFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_infinite_chart, container).apply {
            val chart = findViewById<ChartView>(R.id.infinite_chart)
            val navigator = findViewById<NavigatorChartView>(R.id.dynamic_chart_navigator)

            initUi(chart, navigator, R.style.Charts_Light)

            val model = createModel()
            ModelHolder.model = model
            chart.apply(model)
            navigator.apply(model)
            model.setActiveRange(Range(-100L, 100L), navigator)

            LocalBroadcastManager.getInstance(context).registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent) {
                    initUi(chart, navigator, intent.getIntExtra(LeonardoKey.CHART_STYLE, R.style.Charts_Light))
                }
            }, IntentFilter(LeonardoEvents.THEME_CHANGED))
        }
    }


    private fun initUi(chart: ChartView, navigator: NavigatorChartView, style: Int) {
        val chartConfig = LeonardoConfigFactory.newChartConfigBuilder()
            .withStyle(style)
            .withContext(context!!.applicationContext)
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
}