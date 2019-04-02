package tech.harmonysoft.oss.leonardo.model.config

import tech.harmonysoft.oss.leonardo.model.config.axis.AxisConfigBuilder
import tech.harmonysoft.oss.leonardo.model.config.axis.impl.AxisConfigBuilderImpl
import tech.harmonysoft.oss.leonardo.model.config.chart.ChartConfigBuilder
import tech.harmonysoft.oss.leonardo.model.config.chart.impl.ChartConfigBuilderImpl
import tech.harmonysoft.oss.leonardo.model.config.navigator.NavigatorConfigBuilder
import tech.harmonysoft.oss.leonardo.model.config.navigator.impl.NavigatorConfigBuilderImpl

object LeonardoConfigFactory {

    fun newAxisConfigBuilder(): AxisConfigBuilder {
        return AxisConfigBuilderImpl()
    }

    fun newChartConfigBuilder(): ChartConfigBuilder {
        return ChartConfigBuilderImpl()
    }

    fun newNavigatorConfigBuilder(): NavigatorConfigBuilder {
        return NavigatorConfigBuilderImpl()
    }
}