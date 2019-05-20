package tech.harmonysoft.oss.leonardo.example.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import tech.harmonysoft.oss.leonardo.example.R
import tech.harmonysoft.oss.leonardo.example.data.input.predefined.ChartData
import tech.harmonysoft.oss.leonardo.example.event.ThemeChangedEvent
import tech.harmonysoft.oss.leonardo.example.scroll.ScrollManager
import tech.harmonysoft.oss.leonardo.example.settings.ChartSettings
import tech.harmonysoft.oss.leonardo.example.settings.SettingsManager
import tech.harmonysoft.oss.leonardo.model.config.LeonardoConfigFactory
import tech.harmonysoft.oss.leonardo.model.data.ChartDataSource
import tech.harmonysoft.oss.leonardo.model.runtime.ChartModel
import tech.harmonysoft.oss.leonardo.model.runtime.ChartModelListenerAdapter
import tech.harmonysoft.oss.leonardo.model.runtime.impl.ChartModelImpl
import tech.harmonysoft.oss.leonardo.model.text.ValueRepresentationStrategy
import tech.harmonysoft.oss.leonardo.model.util.LeonardoUtil
import tech.harmonysoft.oss.leonardo.view.chart.ChartView
import tech.harmonysoft.oss.leonardo.view.navigator.NavigatorChartView
import tech.harmonysoft.oss.leonardo.view.navigator.ScrollListener
import java.util.concurrent.Executors

class ChartInitializer(
    private val scrollManager: ScrollManager,
    private val settingsManager: SettingsManager,
    eventBus: EventBus
) {

    private val chartViews = mutableListOf<Triple<ChartView, NavigatorChartView, ValueRepresentationStrategy>>()

    private lateinit var context: Context

    init {
        eventBus.register(this)
    }

    fun prepareChart(chartData: ChartData, holder: ViewGroup, context: Context, layoutInflater: LayoutInflater) {
        this.context = context
        val row = layoutInflater.inflate(R.layout.layout_chart_row, holder, false)

        val progress = row.findViewById<View>(R.id.row_loading_progress)
        val chart = row.findViewById<ChartView>(R.id.row_chart)
        val navigator = row.findViewById<NavigatorChartView>(R.id.row_navigator)
        chartViews += Triple(chart, navigator, chartData.xLabelStrategy)

        navigator.scrollListener = object : ScrollListener {
            override fun onStarted() {
                scrollManager.scrollOwner = navigator
            }

            override fun onStopped() {
                scrollManager.scrollOwner = null
            }
        }

        navigator.apply(LeonardoUtil.asNavigatorShowCase(chart))
        applyUiSettings(chart, navigator, settingsManager.chartStyle, chartData.xLabelStrategy, holder.context)

        val model = ChartModelImpl(workersPool = THREAD_POOL)
        chartData.dataSources.forEach {
            model.addDataSource(it)
        }
        chart.apply(model)
        navigator.apply(model)
        model.setActiveRange(chartData.xRange, navigator.dataAnchor)

        setUpProgressUi(model, progress)

        addSelectors(row.findViewById(R.id.row_settings), model)
        setupPersistentSettings(settingsManager.getChartSettings(chartData.name), model, chart, navigator)

        row.findViewById<TextView>(R.id.row_title).text = chartData.name

        holder.addView(row)
    }

    private fun setUpProgressUi(model: ChartModel, progress: View) {
        model.addListener(object : ChartModelListenerAdapter() {

            override fun onRangeChanged(anchor: Any) {
                progress.postDelayed({
                                for (source in model.registeredDataSources) {
                                    if (model.isLoadingInProgress(source)) {
                                        progress.visibility = View.VISIBLE
                                        break
                                    }
                                }
                            }, 500)
            }

            override fun onPointsLoadingIterationEnd(dataSource: ChartDataSource) {
                for (source in model.registeredDataSources) {
                    if (model.isLoadingInProgress(source)) {
                        return
                    }
                }
                progress.visibility = View.GONE
            }
        })
    }

    private fun addSelectors(contentHolder: ViewGroup, model: ChartModel) {
        contentHolder.removeAllViews()
        model.registeredDataSources.sortedBy { it.legend }.forEach { ds ->
            val view = CheckBoxView(contentHolder.context).apply {
                color = ds.color
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                                         ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            view.color = ds.color
            view.callback = { enabled ->
                if (enabled) {
                    model.enableDataSource(ds)
                } else {
                    model.disableDataSource(ds)
                }
            }
            model.addListener(object : ChartModelListenerAdapter() {
                override fun onDataSourceEnabled(dataSource: ChartDataSource) {
                    if (dataSource == ds) {
                        view.checked = true
                    }
                }

                override fun onDataSourceDisabled(dataSource: ChartDataSource) {
                    if (dataSource == ds) {
                        view.checked = false
                    }
                }
            })
            contentHolder.addView(createFiller(contentHolder.context))
            contentHolder.addView(view)
        }
        contentHolder.addView(createFiller(contentHolder.context))
    }

    private fun createFiller(context: Context): View {
        return Space(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        }
    }

    private fun applyUiSettings(chart: ChartView,
                                navigator: NavigatorChartView,
                                style: Int,
                                xLabelStrategy: ValueRepresentationStrategy,
                                context: Context) {
        val chartConfig = LeonardoConfigFactory.newChartConfigBuilder()
            .withStyle(style)
            .withContext(context)
            .withXAxisConfigBuilder(LeonardoConfigFactory.newAxisConfigBuilder()
                                        .withStyle(style)
                                        .withContext(context)
                                        .withLabelTextStrategy(xLabelStrategy))
            .withNoChartsDrawableId(R.drawable.no_charts)
            .build()

        chart.apply(chartConfig)

        val navigatorConfig = LeonardoConfigFactory.newNavigatorConfigBuilder()
            .withStyle(style)
            .withContext(context)
            .build()
        navigator.apply(navigatorConfig, chartConfig)
    }

    private fun setupPersistentSettings(settings: ChartSettings,
                                        model: ChartModel,
                                        chart: ChartView,
                                        navigator: NavigatorChartView) {
        for (dataSource in model.registeredDataSources) {
            if (settings.isDataSourceEnabled(dataSource)) {
                model.enableDataSource(dataSource)
            } else {
                model.disableDataSource(dataSource)
            }
        }

        settings.getRange("navigator")?.let { model.setActiveRange(it, navigator.dataAnchor) }
        settings.getRange("chart")?.let { model.setActiveRange(it, chart.dataAnchor) }

        model.addListener(object : ChartModelListenerAdapter() {
            override fun onRangeChanged(anchor: Any) {
                when (anchor) {
                    chart.dataAnchor -> settings.storeRange("chart", model.getActiveRange(anchor))
                    navigator.dataAnchor -> settings.storeRange("navigator", model.getActiveRange(anchor))
                }
            }

            override fun onDataSourceEnabled(dataSource: ChartDataSource) {
                settings.enableDataSource(dataSource)
            }

            override fun onDataSourceDisabled(dataSource: ChartDataSource) {
                settings.disableDataSource(dataSource)
            }
        })
    }


    @Subscribe
    fun onThemeChanged(event: ThemeChangedEvent) {
        for ((chart, navigator, xLabelStrategy) in chartViews) {
            applyUiSettings(chart, navigator, event.currentStyle, xLabelStrategy, context)
        }
    }

    companion object {
        private val THREAD_POOL = Executors.newFixedThreadPool(2)
    }
}