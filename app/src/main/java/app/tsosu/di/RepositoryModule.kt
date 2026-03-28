package app.tsosu.di

import app.tsosu.data.local.dao.FocusDao
import app.tsosu.data.local.dao.HabitDao
import app.tsosu.data.local.dao.ProjectDao
import app.tsosu.data.local.dao.RoutineDao
import app.tsosu.data.local.dao.TaskDao
import app.tsosu.data.local.repository.FocusRepositoryImpl
import app.tsosu.data.local.repository.HabitRepositoryImpl
import app.tsosu.data.local.repository.ProjectRepositoryImpl
import app.tsosu.data.local.repository.RoutineRepositoryImpl
import app.tsosu.data.local.repository.TaskRepositoryImpl
import app.tsosu.domain.repository.FocusRepository
import app.tsosu.domain.repository.HabitRepository
import app.tsosu.domain.repository.ProjectRepository
import app.tsosu.domain.repository.RoutineRepository
import app.tsosu.domain.repository.TaskRepository
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
    fun provideTaskRepository(taskDao: TaskDao): TaskRepository =
        TaskRepositoryImpl(taskDao)

    @Provides
    @Singleton
    fun provideHabitRepository(habitDao: HabitDao): HabitRepository =
        HabitRepositoryImpl(habitDao)

    @Provides
    @Singleton
    fun provideRoutineRepository(routineDao: RoutineDao, habitDao: HabitDao): RoutineRepository =
        RoutineRepositoryImpl(routineDao, habitDao)

    @Provides
    @Singleton
    fun provideFocusRepository(
        focusDao: FocusDao,
        taskDao: TaskDao,
        habitDao: HabitDao,
    ): FocusRepository = FocusRepositoryImpl(focusDao, taskDao, habitDao)

    @Provides
    @Singleton
    fun provideProjectRepository(projectDao: ProjectDao): ProjectRepository =
        ProjectRepositoryImpl(projectDao)
}
