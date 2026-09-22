package app.tsosu.data.local

import android.content.Context
import androidx.room.Room
import app.tsosu.data.local.entity.TaskEntity
import app.tsosu.data.local.repository.GamificationRepositoryImpl
import app.tsosu.data.local.repository.TaskRepositoryImpl
import app.tsosu.domain.repository.GamificationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Energy is awarded per completed day: re-completing a recurring task that is
 * already done today (Inbox, Today, or a second tap anywhere) must not mint a
 * second +2. Also covers the fresh-database case where no gamification row
 * exists yet — the award has to create it instead of silently no-op'ing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TaskCompletionEnergyTest {

    private lateinit var db: TsosuDatabase
    private lateinit var tasks: TaskRepositoryImpl
    private lateinit var gamification: GamificationRepositoryImpl

    @Before
    fun setUp() {
        val context = org.robolectric.RuntimeEnvironment.getApplication() as Context
        db = Room.inMemoryDatabaseBuilder(context, TsosuDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        gamification = GamificationRepositoryImpl(db.gamificationDao(), db.streakShieldDao())
        tasks = TaskRepositoryImpl(db.taskDao(), onTaskChanged = null, gamification = gamification)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `completing the same recurring day twice awards energy once`() = runBlocking {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val midnight = today.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        db.taskDao().insert(
            TaskEntity(
                id = "habit",
                title = "Read",
                dueDate = midnight,
                recurrenceRule = "RRULE:FREQ=DAILY",
                createdAt = midnight,
                updatedAt = midnight,
            ),
        )

        tasks.toggleDone("habit").getOrThrow()
        val afterFirst = db.gamificationDao().getEnergy()

        tasks.toggleDone("habit").getOrThrow()
        val afterSecond = db.gamificationDao().getEnergy()

        assertEquals(TaskRepositoryImpl.ENERGY_PER_TASK, afterFirst)
        assertEquals(TaskRepositoryImpl.ENERGY_PER_TASK, afterSecond)

        val completions = db.taskDao().getByIdSync("habit")?.completionsCsv
        assertEquals(listOf(today.toString()), completions?.split(","))
    }

    @Test
    fun `a non-recurring task awards energy on completion`() = runBlocking {
        val now = Clock.System.now().toEpochMilliseconds()
        db.taskDao().insert(
            TaskEntity(id = "one-off", title = "File taxes", createdAt = now, updatedAt = now),
        )

        tasks.toggleDone("one-off").getOrThrow()

        assertEquals(TaskRepositoryImpl.ENERGY_PER_TASK, db.gamificationDao().getEnergy())
    }

    @Test
    fun `bridging the same gap twice spends one freeze`() = runBlocking {
        gamification.awardEnergy(GamificationRepository.FREEZE_COST)
        assertEquals(true, gamification.buyFreeze())
        val gap = LocalDate.fromEpochDays(20_000)

        assertEquals(true, gamification.shieldGap("habit", gap.toEpochDays().toLong()))
        assertEquals(true, gamification.shieldGap("habit", gap.toEpochDays().toLong()))

        assertEquals(0, gamification.freezes().first())
        assertEquals(1, gamification.shieldedDates("habit").first().size)
    }
}
