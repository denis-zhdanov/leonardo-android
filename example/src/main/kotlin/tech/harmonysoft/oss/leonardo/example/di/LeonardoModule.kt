package tech.harmonysoft.oss.leonardo.example.di

import android.content.Context
import com.google.common.eventbus.EventBus
import dagger.Module
import dagger.Provides
import tech.harmonysoft.oss.leonardo.example.settings.SettingsManager
import javax.inject.Singleton

@Module
class LeonardoModule(private val context: Context) {

    @Provides
    @Singleton
    fun provideContext() : Context {
        return context
    }

    @Provides
    @Singleton
    fun provideEventBus() : EventBus {
        return EventBus()
    }

    @Provides
    @Singleton
    fun provideSettingsManager(context: Context): SettingsManager {
        return SettingsManager(context)
    }
}