package tech.harmonysoft.oss.leonardo.example.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import tech.harmonysoft.oss.leonardo.example.R
import tech.harmonysoft.oss.leonardo.example.data.InfiniteChartDataLoader
import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.config.LeonardoConfigFactory
import tech.harmonysoft.oss.leonardo.model.data.ChartDataSource
import tech.harmonysoft.oss.leonardo.model.runtime.ChartModel
import tech.harmonysoft.oss.leonardo.model.runtime.impl.ChartDataAutoLoader
import tech.harmonysoft.oss.leonardo.model.runtime.impl.ChartModelImpl
import tech.harmonysoft.oss.leonardo.model.util.LeonardoUtil
import tech.harmonysoft.oss.leonardo.view.NavigatorChartView
import tech.harmonysoft.oss.leonardo.view.chart.ChartView

/**
 * @author Denis Zhdanov
 * @since 29/3/19
 */
class ChartFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_chart, container).apply {
            val chart = findViewById<ChartView>(R.id.chart)
            val navigator = findViewById<NavigatorChartView>(R.id.navigator)

            initUi(chart, navigator)

            val model = createModel()
            chart.apply(model)
            navigator.apply(model)
            model.setActiveRange(Range(-100L, 100L), navigator)
        }
    }


    private fun initUi(chart: ChartView, navigator: NavigatorChartView) {
        val chartConfig = LeonardoConfigFactory.newChartConfigBuilder()
            .withContext(context!!)
            .withXAxisConfigBuilder(LeonardoConfigFactory.newAxisConfigBuilder())
//                                        .withLabelTextStrategy(TimeValueRepresentationStrategy.INSTANCE))
            .build()

        chart.apply(chartConfig)

        val navigatorConfig = LeonardoConfigFactory.newNavigatorConfigBuilder()
            .withContext(context!!)
            .build()
        navigator.apply(navigatorConfig, chartConfig)
        navigator.apply(LeonardoUtil.asNavigatorShowCase(chart))
    }

    private fun createModel(): ChartModel {
        val model = ChartModelImpl(3)
        ChartDataAutoLoader(model)
        model.addDataSource(ChartDataSource("first",
                                            ResourcesCompat.getColor(resources, R.color.plot1, null),
                                            InfiniteChartDataLoader()))
        model.addDataSource(ChartDataSource("second",
                                            ResourcesCompat.getColor(resources, R.color.plot2, null),
                                            InfiniteChartDataLoader()))
        model.addDataSource(ChartDataSource("third",
                                            ResourcesCompat.getColor(resources, R.color.plot3, null),
                                            InfiniteChartDataLoader()))
        model.addDataSource(ChartDataSource("forth",
                                            ResourcesCompat.getColor(resources, R.color.plot4, null),
                                            InfiniteChartDataLoader()))
        return model
    }
}