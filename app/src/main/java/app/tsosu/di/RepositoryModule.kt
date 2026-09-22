package app.tsosu.di

import app.tsosu.VaultChangeWatcher
import app.tsosu.data.local.dao.GamificationDao
import app.tsosu.data.local.dao.StreakShieldDao
import app.tsosu.data.local.dao.ProjectDao
import app.tsosu.data.local.dao.TaskDao
import app.tsosu.data.local.repository.GamificationRepositoryImpl
import app.tsosu.data.local.repository.ProjectRepositoryImpl
import app.tsosu.data.local.repository.TaskRepositoryImpl
import app.tsosu.domain.repository.GamificationRepository
import app.tsosu.domain.repository.ProjectRepository
import app.tsosu.domain.repository.TaskRepository
import app.tsosu.domain.repository.SyncRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideTaskRepository(
        taskDao: TaskDao,
        watcher: VaultChangeWatcher,
        gamification: GamificationRepository,
        syncRepository: SyncRepository,
    ): TaskRepository =
        TaskRepositoryImpl(
            taskDao,
            onTaskChanged = { taskId, operation, _ ->
                if (operation == "DELETE") syncRepository.removeTaskNote(taskId)
                watcher.pushSoon()
            },
            gamification = gamification,
        )

    @Provides
    @Singleton
    fun provideGamificationRepository(
        gamificationDao: GamificationDao,
        streakShieldDao: StreakShieldDao,
    ): GamificationRepository =
        GamificationRepositoryImpl(gamificationDao, streakShieldDao)

    @Provides
    @Singleton
    fun provideProjectRepository(projectDao: ProjectDao): ProjectRepository =
        ProjectRepositoryImpl(projectDao)
}
