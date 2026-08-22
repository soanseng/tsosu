package app.tsosu.di

import android.content.Context
import androidx.room.Room
import app.tsosu.data.local.MIGRATION_1_2
import app.tsosu.data.local.MIGRATION_2_3
import app.tsosu.data.local.MIGRATION_3_4
import app.tsosu.data.local.MIGRATION_4_5
import app.tsosu.data.local.MIGRATION_5_6
import app.tsosu.data.local.MIGRATION_6_7
import app.tsosu.data.local.MIGRATION_7_8
import app.tsosu.data.local.MIGRATION_8_9
import app.tsosu.data.local.MIGRATION_9_10
import app.tsosu.data.local.MIGRATION_10_11
import app.tsosu.data.local.BackupRepository
import app.tsosu.data.local.TsosuDatabase
import app.tsosu.data.local.dao.GamificationDao
import app.tsosu.data.local.dao.FocusDao
import app.tsosu.data.local.dao.StreakShieldDao
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
            .build()
    @Provides
    @Singleton
    fun provideBackupRepository(db: TsosuDatabase): BackupRepository = BackupRepository(
        db = db,
        taskDao = db.taskDao(),
        habitDao = db.habitDao(),
        focusDao = db.focusDao(),
        gamificationDao = db.gamificationDao(),
        streakShieldDao = db.streakShieldDao(),
        projectDao = db.projectDao(),
        routineDao = db.routineDao(),
    )

    @Provides fun provideStreakShieldDao(db: TsosuDatabase): StreakShieldDao = db.streakShieldDao()
    @Provides fun provideGamificationDao(db: TsosuDatabase): GamificationDao = db.gamificationDao()

    @Provides fun provideTaskDao(db: TsosuDatabase): TaskDao = db.taskDao()
    @Provides fun provideHabitDao(db: TsosuDatabase): HabitDao = db.habitDao()
    @Provides fun provideRoutineDao(db: TsosuDatabase): RoutineDao = db.routineDao()
    @Provides fun provideFocusDao(db: TsosuDatabase): FocusDao = db.focusDao()
    @Provides fun provideProjectDao(db: TsosuDatabase): ProjectDao = db.projectDao()
    @Provides fun provideLabelDao(db: TsosuDatabase): LabelDao = db.labelDao()
}
