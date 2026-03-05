package app.tsosu.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
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
import app.tsosu.data.local.entity.SyncQueueEntity
import app.tsosu.data.local.entity.TaskEntity

@Database(
    entities = [
        TaskEntity::class,
        HabitEntity::class,
        HabitCompletionEntity::class,
        RoutineEntity::class,
        DailyFocusEntity::class,
        ProjectEntity::class,
        LabelEntity::class,
        SyncQueueEntity::class,
    ],
    version = 1,
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
