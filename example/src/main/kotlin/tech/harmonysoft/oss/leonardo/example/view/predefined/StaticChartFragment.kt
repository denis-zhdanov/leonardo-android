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
import javax.inject.Inject

class StaticChartFragment : Fragment() {

    @Inject lateinit var chartInitializer: ChartInitializer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LeonardoApplication.graph.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_chart, container).apply {
            val charts = JsonDataSourceParser().parse(resources.openRawResource(R.raw.input))
            charts.forEach {
                chartInitializer.prepareChart(it, findViewById(R.id.charts), context, layoutInflater)
            }
        }
    }
}