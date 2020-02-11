package tech.harmonysoft.oss.leonardo.model.config

import tech.harmonysoft.oss.leonardo.model.config.axis.AxisConfigBuilder
import tech.harmonysoft.oss.leonardo.model.config.axis.impl.AxisConfigBuilderImpl
import tech.harmonysoft.oss.leonardo.model.config.chart.ChartConfigBuilder
import tech.harmonysoft.oss.leonardo.model.config.chart.impl.ChartConfigBuilderImpl

object LeonardoConfigFactory {

    fun newAxisConfigBuilder(): AxisConfigBuilder {
        return AxisConfigBuilderImpl()
    }

    fun newChartConfigBuilder(): ChartConfigBuilder {
        return ChartConfigBuilderImpl()
    }
}