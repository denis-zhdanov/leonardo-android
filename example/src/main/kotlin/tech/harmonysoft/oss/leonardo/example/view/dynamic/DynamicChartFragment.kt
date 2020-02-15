package tech.harmonysoft.oss.leonardo.example.view.dynamic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import tech.harmonysoft.oss.leonardo.example.LeonardoApplication
import tech.harmonysoft.oss.leonardo.example.R
import tech.harmonysoft.oss.leonardo.example.data.input.infinite.InfiniteChartDataLoader
import tech.harmonysoft.oss.leonardo.example.data.input.predefined.ChartData
import tech.harmonysoft.oss.leonardo.example.view.ChartInitializer
import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.config.axis.impl.DefaultValueRepresentationStrategy
import tech.harmonysoft.oss.leonardo.model.data.ChartDataSource
import javax.inject.Inject

class DynamicChartFragment : Fragment() {

    @Inject lateinit var chartInitializer: ChartInitializer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LeonardoApplication.graph.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_chart, container).apply {
            val chartData = ChartData(
                    resources.getString(R.string.dynamic_chart_title),
                    Range(-100L, 100L),
                    createDataSources(),
                    DefaultValueRepresentationStrategy.INSTANCE
            )
            chartInitializer.prepareChart(chartData, findViewById(R.id.charts), context, layoutInflater)
        }
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
}