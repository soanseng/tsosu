package app.tsosu.data.vikunja.di

import android.content.Context
import app.tsosu.data.local.dao.LabelDao
import app.tsosu.data.local.dao.ProjectDao
import app.tsosu.data.local.dao.TaskDao
import app.tsosu.data.vikunja.auth.VikunjaCredentialStore
import app.tsosu.data.vikunja.importer.TodoistImporter
import app.tsosu.data.vikunja.mapper.VikunjaTaskMapper
import app.tsosu.data.vikunja.repository.ImportRepositoryImpl
import app.tsosu.data.vikunja.repository.SyncRepositoryImpl
import app.tsosu.data.local.dao.SyncQueueDao
import app.tsosu.data.vikunja.api.VikunjaApiProvider
import app.tsosu.data.vikunja.sync.EnergyLabelManager
import app.tsosu.data.vikunja.sync.SyncDispatcher
import app.tsosu.data.vikunja.sync.SyncManager
import app.tsosu.domain.repository.ImportRepository
import app.tsosu.domain.repository.SyncRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VikunjaModule {

    @Provides
    @Singleton
    fun provideCredentialStore(@ApplicationContext context: Context): VikunjaCredentialStore {
        return VikunjaCredentialStore(context)
    }

    @Provides
    @Singleton
    fun provideTaskMapper(): VikunjaTaskMapper = VikunjaTaskMapper()

    @Provides
    @Singleton
    fun provideSyncDispatcher(
        credentialStore: VikunjaCredentialStore,
        syncQueueDao: SyncQueueDao,
        taskDao: TaskDao,
        projectDao: ProjectDao,
        labelDao: LabelDao,
        taskMapper: VikunjaTaskMapper,
    ): SyncDispatcher {
        return SyncDispatcher(
            syncManagerProvider = {
                val url = kotlinx.coroutines.runBlocking { credentialStore.getServerUrl() } ?: return@SyncDispatcher null
                val token = kotlinx.coroutines.runBlocking { credentialStore.getToken() } ?: return@SyncDispatcher null
                val api = VikunjaApiProvider.create(url) { token }
                SyncManager(api, taskDao, projectDao, labelDao, EnergyLabelManager(api), taskMapper)
            },
            syncQueueDao = syncQueueDao,
            taskDao = taskDao,
        )
    }

    @Provides
    @Singleton
    fun provideSyncRepository(
        credentialStore: VikunjaCredentialStore,
        taskDao: TaskDao,
        projectDao: ProjectDao,
        labelDao: LabelDao,
        taskMapper: VikunjaTaskMapper,
        syncDispatcher: SyncDispatcher,
    ): SyncRepository {
        return SyncRepositoryImpl(
            credentialStore = credentialStore,
            syncManagerFactory = { api ->
                SyncManager(api, taskDao, projectDao, labelDao, EnergyLabelManager(api), taskMapper)
            },
            energyLabelManagerFactory = { api -> EnergyLabelManager(api) },
            syncDispatcher = syncDispatcher,
        )
    }

    @Provides
    @Singleton
    fun provideImportRepository(
        taskDao: TaskDao,
    ): ImportRepository {
        return ImportRepositoryImpl(
            taskDao = taskDao,
            importer = TodoistImporter(),
        )
    }
}
