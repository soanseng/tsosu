package app.tsosu.data.calendar.di

import android.content.Context
import app.tsosu.data.calendar.CalDavCredentialStore
import app.tsosu.data.calendar.CalendarRepositoryImpl
import app.tsosu.data.calendar.google.GoogleCredentialStore
import app.tsosu.data.calendar.IcsExporterImpl
import app.tsosu.domain.repository.CalendarRepository
import app.tsosu.domain.repository.IcsExporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CalendarModule {

    @Provides
    @Singleton
    fun provideCalDavCredentialStore(@ApplicationContext context: Context): CalDavCredentialStore {
        return CalDavCredentialStore(context)
    }

    @Provides
    @Singleton
    fun provideGoogleCredentialStore(@ApplicationContext context: Context): GoogleCredentialStore {
        return GoogleCredentialStore(context)
    }

    @Provides
    @Singleton
    fun provideIcsExporter(): IcsExporter {
        return IcsExporterImpl()
    }

    @Provides
    @Singleton
    fun provideCalendarRepository(
        caldavCredentialStore: CalDavCredentialStore,
        googleCredentialStore: GoogleCredentialStore,
    ): CalendarRepository {
        return CalendarRepositoryImpl(caldavCredentialStore, googleCredentialStore)
    }
}
