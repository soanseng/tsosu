package app.tsosu.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.tsosu.data.local.dao.TaskDao
import app.tsosu.data.local.dao.FocusDao
import app.tsosu.data.local.dao.GamificationDao
import app.tsosu.data.local.dao.HabitDao
import app.tsosu.data.local.dao.LabelDao
import app.tsosu.data.local.dao.ProjectDao
import app.tsosu.data.local.dao.RoutineDao
import app.tsosu.data.local.dao.StreakShieldDao
import app.tsosu.data.local.entity.DailyFocusEntity
import app.tsosu.data.local.entity.GamificationEntity
import app.tsosu.data.local.entity.HabitCompletionEntity
import app.tsosu.data.local.entity.HabitEntity
import app.tsosu.data.local.entity.LabelEntity
import app.tsosu.data.local.entity.ProjectEntity
import app.tsosu.data.local.entity.RoutineEntity
import app.tsosu.data.local.entity.StreakShieldEntity
import app.tsosu.data.local.entity.TaskEntity
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Collapse historical duplicate completions (same habit + day),
        // then enforce one row per (habitId, date) forever.
        db.execSQL(
            """DELETE FROM habit_completions WHERE id NOT IN (
               SELECT MIN(id) FROM habit_completions GROUP BY habitId, date)""",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_habit_completions_habitId_date " +
                "ON habit_completions(habitId, date)",
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `gamification` (" +
                "`id` INTEGER NOT NULL, `energy` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL("INSERT OR IGNORE INTO gamification (id, energy) VALUES (1, 0)")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE gamification ADD COLUMN freezes INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `streak_shields` (" +
                "`habitId` TEXT NOT NULL, `date` INTEGER NOT NULL, PRIMARY KEY(`habitId`, `date`))",
        )
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE habits ADD COLUMN projectId TEXT")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE habits ADD COLUMN weekdays TEXT")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN tinyVersion TEXT")
        db.execSQL("ALTER TABLE tasks ADD COLUMN routineTime INTEGER")
        db.execSQL("ALTER TABLE tasks ADD COLUMN completionsCsv TEXT")
    }
}

/**
 * `habit_completions.date` and `streak_shields.date` historically stored
 * local-midnight epoch millis, which drift across timezone changes (a
 * completion recorded in Taipei reads as the previous day in San Francisco).
 * Convert both to timezone-independent epoch days, interpreting each row in
 * the current timezone — the same conversion the read path has always
 * applied, so displayed dates are preserved.
 *
 * Rows that collide after conversion (double-writes caused by the very bug
 * being fixed) are collapsed to the lowest rowid per (habitId, epochDays).
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val tz = TimeZone.currentSystemDefault()
        for (table in listOf("habit_completions", "streak_shields")) {
            // rowid -> converted epoch days
            val converted = mutableListOf<Pair<Long, Long>>()
            db.query("SELECT rowid, habitId, date FROM $table").use { cursor ->
                while (cursor.moveToNext()) {
                    val days = Instant.fromEpochMilliseconds(cursor.getLong(2))
                        .toLocalDateTime(tz).date.toEpochDays().toLong()
                    converted.add(cursor.getLong(0) to days)
                }
            }
            // Collapse collisions (same habit + day) keeping the lowest rowid.
            val habitIdByRow = db.query("SELECT rowid, habitId FROM $table").use { cursor ->
                buildMap {
                    while (cursor.moveToNext()) {
                        put(cursor.getLong(0), cursor.getString(1))
                    }
                }
            }
            val winners = converted
                .groupBy { habitIdByRow[it.first] to it.second }
                .mapValues { (_, rows) -> rows.minBy { (rowId, _) -> rowId }.first }
                .values.toSet()
            val losers = converted.map { it.first }.filter { it !in winners }

            db.beginTransaction()
            try {
                val delete = db.compileStatement("DELETE FROM $table WHERE rowid = ?")
                for (rowId in losers) {
                    delete.bindLong(1, rowId)
                    delete.executeUpdateDelete()
                }
                val update = db.compileStatement("UPDATE $table SET date = ? WHERE rowid = ?")
                for ((rowId, days) in converted) {
                    if (rowId !in winners) continue
                    update.bindLong(1, days)
                    update.bindLong(2, rowId)
                    update.executeUpdateDelete()
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }
}

@Database(entities = [TaskEntity::class, HabitEntity::class, HabitCompletionEntity::class, RoutineEntity::class, DailyFocusEntity::class, ProjectEntity::class, GamificationEntity::class, StreakShieldEntity::class, LabelEntity::class], version = 11)
abstract class TsosuDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun routineDao(): RoutineDao
    abstract fun focusDao(): FocusDao
    abstract fun projectDao(): ProjectDao
    abstract fun gamificationDao(): GamificationDao
    abstract fun streakShieldDao(): StreakShieldDao
    abstract fun labelDao(): LabelDao
}
