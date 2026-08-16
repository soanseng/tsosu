package app.tsosu.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.tsosu.data.local.dao.FocusDao
import app.tsosu.data.local.dao.HabitDao
import app.tsosu.data.local.dao.LabelDao
import app.tsosu.data.local.dao.ProjectDao
import app.tsosu.data.local.dao.RoutineDao
import app.tsosu.data.local.dao.TaskDao
import app.tsosu.data.local.entity.DailyFocusEntity
import app.tsosu.data.local.entity.HabitCompletionEntity
import app.tsosu.data.local.entity.HabitEntity
import app.tsosu.data.local.entity.LabelEntity
import app.tsosu.data.local.entity.ProjectEntity
import app.tsosu.data.local.entity.RoutineEntity
import app.tsosu.data.local.entity.TaskEntity

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS sync_queue")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN status INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE tasks SET status = 4 WHERE done = 1")
        db.execSQL("ALTER TABLE tasks ADD COLUMN scheduledDate INTEGER")
        db.execSQL("ALTER TABLE tasks ADD COLUMN startDate INTEGER")
        db.execSQL("ALTER TABLE tasks ADD COLUMN reminderTimeMinutes INTEGER")
        db.execSQL("ALTER TABLE tasks ADD COLUMN completedDate INTEGER")
        db.execSQL("ALTER TABLE tasks ADD COLUMN cancelledDate INTEGER")
        db.execSQL("ALTER TABLE tasks ADD COLUMN recurrenceRule TEXT")
        db.execSQL("UPDATE tasks SET completedDate = doneAt WHERE done = 1")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE habits ADD COLUMN reminderMinutes INTEGER")
    }
}

@Database(
    entities = [
        TaskEntity::class,
        HabitEntity::class,
        HabitCompletionEntity::class,
        RoutineEntity::class,
        DailyFocusEntity::class,
        ProjectEntity::class,
        LabelEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class TsosuDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun routineDao(): RoutineDao
    abstract fun focusDao(): FocusDao
    abstract fun projectDao(): ProjectDao
    abstract fun labelDao(): LabelDao
}
