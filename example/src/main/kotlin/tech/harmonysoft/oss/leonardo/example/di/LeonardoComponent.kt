package tech.harmonysoft.oss.leonardo.example.di

import dagger.Component
import tech.harmonysoft.oss.leonardo.example.scroll.MyScrollView
import tech.harmonysoft.oss.leonardo.example.view.ExampleActivity
import tech.harmonysoft.oss.leonardo.example.view.dynamic.DynamicChartFragment
import tech.harmonysoft.oss.leonardo.example.view.predefined.StaticChartFragment
import javax.inject.Singleton

@Singleton
@Component(modules = [LeonardoModule::class])
interface LeonardoComponent {

    fun inject(activity: ExampleActivity)

    fun inject(fragment: StaticChartFragment)

    fun inject(fragment: DynamicChartFragment)

    fun inject(view: MyScrollView)
}