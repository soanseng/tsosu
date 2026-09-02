package app.tsosu.di

import app.tsosu.domain.repository.IcsExporter
import app.tsosu.domain.repository.CalendarRepository
import app.tsosu.domain.repository.TaskRepository
import app.tsosu.domain.usecase.TaskCalendarCoordinator
import app.tsosu.domain.usecase.CreateTaskUseCase
import app.tsosu.domain.usecase.DeleteTaskUseCase
import app.tsosu.domain.usecase.ExportIcsUseCase
import app.tsosu.domain.usecase.GetStaleTaskIdsUseCase
import app.tsosu.domain.usecase.SetTaskStatusUseCase
import app.tsosu.domain.usecase.ToggleTaskDoneUseCase
import app.tsosu.domain.usecase.UpdateTaskUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides fun provideCreateTask(repo: TaskRepository, calendar: CalendarRepository) =
        CreateTaskUseCase(repo, TaskCalendarCoordinator(repo, calendar))
    @Provides fun provideToggleTaskDone(repo: TaskRepository, calendar: CalendarRepository) =
        ToggleTaskDoneUseCase(repo, TaskCalendarCoordinator(repo, calendar))
    @Provides fun provideSetTaskStatus(repo: TaskRepository, calendar: CalendarRepository) =
        SetTaskStatusUseCase(repo, TaskCalendarCoordinator(repo, calendar))
    @Provides fun provideGetStaleTaskIds(repo: TaskRepository) = GetStaleTaskIdsUseCase(repo)
    @Provides fun provideExportIcs(repo: TaskRepository, icsExporter: IcsExporter) = ExportIcsUseCase(repo, icsExporter)
    @Provides fun provideUpdateTask(repo: TaskRepository, calendar: CalendarRepository) =
        UpdateTaskUseCase(repo, TaskCalendarCoordinator(repo, calendar))
    @Provides fun provideDeleteTask(repo: TaskRepository, calendar: CalendarRepository) =
        DeleteTaskUseCase(repo, TaskCalendarCoordinator(repo, calendar))
}
