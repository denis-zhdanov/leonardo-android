package tech.harmonysoft.oss.leonardo.example.di

import dagger.Component
import tech.harmonysoft.oss.leonardo.example.view.ExampleActivity
import javax.inject.Singleton

@Singleton
@Component(modules = [LeonardoModule::class])
interface LeonardoComponent {

    fun inject(activity: ExampleActivity)
}