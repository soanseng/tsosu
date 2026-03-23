package app.tsosu.data.markdown.di

import android.content.Context
import app.tsosu.data.local.dao.HabitDao
import app.tsosu.data.local.dao.ProjectDao
import app.tsosu.data.local.dao.TaskDao
import app.tsosu.data.markdown.MarkdownFileAccess
import app.tsosu.data.markdown.MarkdownHabitParser
import app.tsosu.data.markdown.MarkdownHabitSerializer
import app.tsosu.data.markdown.MarkdownPreferences
import app.tsosu.data.markdown.MarkdownSyncManager
import app.tsosu.data.markdown.MarkdownSyncRepository
import app.tsosu.data.markdown.MarkdownTaskParser
import app.tsosu.data.markdown.MarkdownTaskSerializer
import app.tsosu.data.markdown.NoOpImportRepository
import app.tsosu.data.markdown.SafMarkdownFileAccess
import app.tsosu.data.markdown.dailynote.DailyNoteWriter
import app.tsosu.data.markdown.habitnote.HabitNoteParser
import app.tsosu.data.markdown.habitnote.HabitNoteSerializer
import app.tsosu.data.markdown.index.HabitIndexGenerator
import app.tsosu.data.markdown.index.TaskIndexGenerator
import app.tsosu.data.markdown.tasknote.TaskNoteParser
import app.tsosu.data.markdown.tasknote.TaskNoteSerializer
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
object MarkdownModule {

    @Provides
    @Singleton
    fun provideMarkdownPreferences(@ApplicationContext context: Context): MarkdownPreferences =
        MarkdownPreferences(context)

    @Provides
    @Singleton
    fun provideMarkdownFileAccess(
        @ApplicationContext context: Context,
        preferences: MarkdownPreferences,
    ): MarkdownFileAccess = SafMarkdownFileAccess(context) { preferences.getFolderUri() }

    @Provides
    @Singleton
    fun provideMarkdownSyncManager(
        fileAccess: MarkdownFileAccess,
    ): MarkdownSyncManager = MarkdownSyncManager(
        fileAccess = fileAccess,
        taskSerializer = MarkdownTaskSerializer(),
        taskParser = MarkdownTaskParser(),
        habitSerializer = MarkdownHabitSerializer(),
        habitParser = MarkdownHabitParser(),
        taskNoteSerializer = TaskNoteSerializer(),
        taskNoteParser = TaskNoteParser(),
        habitNoteSerializer = HabitNoteSerializer(),
        habitNoteParser = HabitNoteParser(),
        dailyNoteWriter = DailyNoteWriter(),
        taskIndexGenerator = TaskIndexGenerator(),
        habitIndexGenerator = HabitIndexGenerator(),
    )

    @Provides
    @Singleton
    fun provideSyncRepository(
        preferences: MarkdownPreferences,
        syncManager: MarkdownSyncManager,
        taskDao: TaskDao,
        habitDao: HabitDao,
        projectDao: ProjectDao,
    ): SyncRepository = MarkdownSyncRepository(
        preferences = preferences,
        syncManager = syncManager,
        taskDao = taskDao,
        habitDao = habitDao,
        projectDao = projectDao,
    )

    @Provides
    @Singleton
    fun provideImportRepository(): ImportRepository = NoOpImportRepository()
}
