package app.tsosu.data.local

import android.content.Context
import androidx.room.Room
import app.tsosu.data.local.entity.GamificationEntity
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
            gamificationDao = db.gamificationDao(),
            streakShieldDao = db.streakShieldDao(),
            projectDao = db.projectDao(),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `export then restore round-trips tasks and gamification`() = runBlocking {
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
        db.gamificationDao().ensureRow()
        db.gamificationDao().awardEnergy(7)

        val json = repo.exportJson()

        // Wipe everything, then restore from the JSON.
        db.taskDao().clearAll()
        db.gamificationDao().clearAll()
        assertTrue(db.taskDao().getAllTasks().first().isEmpty())

        repo.restore(repo.decode(json))

        val restoredTasks = db.taskDao().getByIdSync("t1")!!
        assertEquals("Water plants", restoredTasks.title)
        assertEquals("RRULE:FREQ=DAILY", restoredTasks.recurrenceRule)
        assertEquals(7, db.gamificationDao().getEnergy())
    }

    @Test
    fun `restoring a backup without gamification leaves a usable row`() = runBlocking {
        db.gamificationDao().awardEnergy(5)
        val backupWithoutGamification = repo.decode(repo.exportJson())
            .copy(gamification = null)

        repo.restore(backupWithoutGamification)

        // Not null: the row exists so the next award lands and the UI can read 0.
        assertEquals(0, db.gamificationDao().getEnergy())
        db.gamificationDao().awardEnergy(2)
        assertEquals(2, db.gamificationDao().getEnergy())
    }

}
