package app.tsosu.di

import android.content.Context
import app.tsosu.ui.theme.ThemePreferences
import app.tsosu.ui.util.UxHintPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ThemeModule {

    @Provides
    @Singleton
    fun provideThemePreferences(@ApplicationContext context: Context): ThemePreferences {
        return ThemePreferences(context)
    }

    @Provides
    @Singleton
    fun provideSavedViewPreferences(@ApplicationContext context: Context): app.tsosu.ui.screens.filter.SavedViewPreferences {
        return app.tsosu.ui.screens.filter.SavedViewPreferences(context)
    }

    @Provides
    @Singleton
    fun provideUxHintPreferences(@ApplicationContext context: Context): UxHintPreferences {
        return UxHintPreferences(context)
    }
}
