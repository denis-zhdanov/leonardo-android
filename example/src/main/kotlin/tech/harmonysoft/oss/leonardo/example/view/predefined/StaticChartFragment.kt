package tech.harmonysoft.oss.leonardo.example.view.predefined

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import tech.harmonysoft.oss.leonardo.example.LeonardoApplication
import tech.harmonysoft.oss.leonardo.example.R
import tech.harmonysoft.oss.leonardo.example.data.input.predefined.JsonDataSourceParser
import tech.harmonysoft.oss.leonardo.example.view.ChartInitializer
import tech.harmonysoft.oss.leonardo.example.view.SeparatorViewFactory
import javax.inject.Inject

class StaticChartFragment : Fragment() {

    @Inject lateinit var chartInitializer: ChartInitializer
    @Inject lateinit var separatorFactory: SeparatorViewFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LeonardoApplication.graph.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_chart, container).apply {
            val charts = JsonDataSourceParser().parse(resources.openRawResource(R.raw.input))
            val holder = findViewById<ViewGroup>(R.id.charts)
            charts.forEachIndexed { i, chartData ->
                if (i > 0) {
                    holder.addView(separatorFactory.createSeparator(context))
                }
                chartInitializer.prepareChart(chartData, holder, context, layoutInflater)
            }
        }
    }
}