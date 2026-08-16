package app.tsosu.di

import android.content.Context
import androidx.room.Room
import app.tsosu.data.local.MIGRATION_1_2
import app.tsosu.data.local.MIGRATION_2_3
import app.tsosu.data.local.MIGRATION_3_4
import app.tsosu.data.local.MIGRATION_4_5
import app.tsosu.data.local.TsosuDatabase
import app.tsosu.data.local.dao.FocusDao
import app.tsosu.data.local.dao.HabitDao
import app.tsosu.data.local.dao.LabelDao
import app.tsosu.data.local.dao.ProjectDao
import app.tsosu.data.local.dao.RoutineDao
import app.tsosu.data.local.dao.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TsosuDatabase =
        Room.databaseBuilder(context, TsosuDatabase::class.java, "tsosu.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()

    @Provides fun provideTaskDao(db: TsosuDatabase): TaskDao = db.taskDao()
    @Provides fun provideHabitDao(db: TsosuDatabase): HabitDao = db.habitDao()
    @Provides fun provideRoutineDao(db: TsosuDatabase): RoutineDao = db.routineDao()
    @Provides fun provideFocusDao(db: TsosuDatabase): FocusDao = db.focusDao()
    @Provides fun provideProjectDao(db: TsosuDatabase): ProjectDao = db.projectDao()
    @Provides fun provideLabelDao(db: TsosuDatabase): LabelDao = db.labelDao()
}
