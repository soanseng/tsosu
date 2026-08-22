package app.tsosu.data.local

import android.content.Context
import androidx.room.Room
import app.tsosu.data.local.entity.GamificationEntity
import app.tsosu.data.local.entity.HabitCompletionEntity
import app.tsosu.data.local.entity.HabitEntity
import app.tsosu.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Real in-memory Room round-trip: seed -> export JSON -> wipe -> restore ->
 * verify equality. Guards the whole backup path, not just serialization.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupRepositoryTest {

    private lateinit var db: TsosuDatabase
    private lateinit var repo: BackupRepository

    @Before
    fun setUp() {
        val context = org.robolectric.RuntimeEnvironment.getApplication() as Context
        db = Room.inMemoryDatabaseBuilder(context, TsosuDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = BackupRepository(
            db = db,
            taskDao = db.taskDao(),
            habitDao = db.habitDao(),
            focusDao = db.focusDao(),
            gamificationDao = db.gamificationDao(),
            streakShieldDao = db.streakShieldDao(),
            projectDao = db.projectDao(),
            routineDao = db.routineDao(),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `export then restore round-trips tasks and completions`() = runBlocking {
        val task = TaskEntity(
            id = "t1",
            title = "Water plants",
            status = 0,
            createdAt = 1L,
            updatedAt = 1L,
            dueDate = 1_800_000_000_000,
            recurrenceRule = "RRULE:FREQ=DAILY",
            completionsCsv = "2026-08-21",
        )
        db.taskDao().insert(task)
        db.habitDao().insert(HabitEntity(id = "h1", title = "Read", position = 0.0, createdAt = 1L))
        db.habitDao().insertCompletionOnce("h1", date = 20_686, completedAt = 123L)
        db.gamificationDao().ensureRow()
        db.gamificationDao().awardEnergy(7)

        val json = repo.exportJson()

        // Wipe everything, then restore from the JSON.
        db.taskDao().clearAll()
        db.habitDao().clearCompletions()
        db.habitDao().clearAll()
        db.gamificationDao().clearAll()
        assertTrue(db.taskDao().getAllTasks().first().isEmpty())

        repo.restore(repo.decode(json))

        val restoredTasks = db.taskDao().getByIdSync("t1")!!
        assertEquals("Water plants", restoredTasks.title)
        assertEquals("RRULE:FREQ=DAILY", restoredTasks.recurrenceRule)
        assertEquals(20_686L, db.habitDao().getCompletionDatesSync("h1").single())
        assertEquals(7, db.gamificationDao().getEnergy())
    }

}
