package tech.harmonysoft.oss.leonardo.example.di

import android.content.Context
import com.google.common.eventbus.EventBus
import dagger.Module
import dagger.Provides
import tech.harmonysoft.oss.leonardo.example.scroll.ScrollManager
import tech.harmonysoft.oss.leonardo.example.settings.SettingsManager
import tech.harmonysoft.oss.leonardo.example.view.ChartInitializer
import tech.harmonysoft.oss.leonardo.example.view.SeparatorViewFactory
import javax.inject.Singleton

@Module
class LeonardoModule(private val context: Context) {

    @Provides
    @Singleton
    fun provideContext(): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideEventBus(): EventBus {
        return EventBus()
    }

    @Provides
    @Singleton
    fun provideSettingsManager(context: Context): SettingsManager {
        return SettingsManager(context)
    }

    @Provides
    @Singleton
    fun providesScrollManager(): ScrollManager {
        return ScrollManager()
    }

    @Provides
    @Singleton
    fun providesChartInitializer(scrollManager: ScrollManager,
                                 settingsManager: SettingsManager,
                                 eventBus: EventBus): ChartInitializer {
        return ChartInitializer(scrollManager, settingsManager, eventBus)
    }

    @Provides
    @Singleton
    fun providesSeparatorFactory(settingsManager: SettingsManager, eventBus: EventBus): SeparatorViewFactory {
        return SeparatorViewFactory(settingsManager, eventBus)
    }
}