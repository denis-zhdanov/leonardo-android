package tech.harmonysoft.oss.leonardo.example

import androidx.multidex.MultiDexApplication
import tech.harmonysoft.oss.leonardo.example.di.DaggerLeonardoComponent
import tech.harmonysoft.oss.leonardo.example.di.LeonardoComponent
import tech.harmonysoft.oss.leonardo.example.di.LeonardoModule

class LeonardoApplication : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        graph = DaggerLeonardoComponent.builder().leonardoModule(LeonardoModule(this)).build()
    }

    companion object {
        lateinit var graph: LeonardoComponent
    }
}