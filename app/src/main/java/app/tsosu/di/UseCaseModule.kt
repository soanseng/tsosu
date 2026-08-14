package app.tsosu.di

import app.tsosu.domain.repository.FocusRepository
import app.tsosu.domain.repository.HabitRepository
import app.tsosu.domain.repository.IcsExporter
import app.tsosu.domain.repository.RoutineRepository
import app.tsosu.domain.repository.TaskRepository
import app.tsosu.domain.usecase.CompleteHabitUseCase
import app.tsosu.domain.usecase.CreateHabitUseCase
import app.tsosu.domain.usecase.ConvertTaskToHabitUseCase
import app.tsosu.domain.usecase.CreateTaskUseCase
import app.tsosu.domain.usecase.DeleteTaskUseCase
import app.tsosu.domain.usecase.ExportIcsUseCase
import app.tsosu.domain.usecase.GetRoutineUseCase
import app.tsosu.domain.usecase.GetStaleTaskIdsUseCase
import app.tsosu.domain.usecase.GetTodayHabitsUseCase
import app.tsosu.domain.usecase.GetTodayOverviewUseCase
import app.tsosu.domain.usecase.GetWeeklyReviewUseCase
import app.tsosu.domain.usecase.PickOneTaskUseCase
import app.tsosu.domain.usecase.SetDailyFocusUseCase
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

    @Provides fun provideCreateTask(repo: TaskRepository) = CreateTaskUseCase(repo)
    @Provides fun provideToggleTaskDone(repo: TaskRepository) = ToggleTaskDoneUseCase(repo)
    @Provides fun provideSetTaskStatus(repo: TaskRepository) = SetTaskStatusUseCase(repo)
    @Provides fun provideGetTodayOverview(repo: TaskRepository) = GetTodayOverviewUseCase(repo)
    @Provides fun providePickOneTask(repo: TaskRepository) = PickOneTaskUseCase(repo)
    @Provides fun provideSetDailyFocus(focus: FocusRepository, task: TaskRepository) = SetDailyFocusUseCase(focus, task)
    @Provides fun provideGetStaleTaskIds(repo: TaskRepository) = GetStaleTaskIdsUseCase(repo)
    @Provides fun provideGetWeeklyReview(repo: FocusRepository) = GetWeeklyReviewUseCase(repo)
    @Provides fun provideCreateHabit(repo: HabitRepository) = CreateHabitUseCase(repo)
    @Provides fun provideCompleteHabit(repo: HabitRepository) = CompleteHabitUseCase(repo)
    @Provides fun provideGetTodayHabits(repo: HabitRepository) = GetTodayHabitsUseCase(repo)
    @Provides fun provideGetRoutine(repo: RoutineRepository) = GetRoutineUseCase(repo)
    @Provides fun provideUpdateTask(repo: TaskRepository) = UpdateTaskUseCase(repo)
    @Provides fun provideDeleteTask(repo: TaskRepository) = DeleteTaskUseCase(repo)
    @Provides fun provideConvertTaskToHabit(task: TaskRepository, habit: HabitRepository) = ConvertTaskToHabitUseCase(task, habit)
    @Provides fun provideExportIcs(repo: TaskRepository, icsExporter: IcsExporter) = ExportIcsUseCase(repo, icsExporter)
}
