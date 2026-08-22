package app.tsosu.data.local

import app.tsosu.data.local.EpochDateMigrationPlanner.MigrationRow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

class EpochDateMigrationPlannerTest {

    // A timezone without DST keeps conversions exact.
    private val tz = TimeZone.of("Asia/Taipei")

    private fun midnightMillis(date: LocalDate): Long =
        date.atStartOfDayIn(tz).toEpochMilliseconds()

    @Test
    fun `converts local-midnight millis to epoch days`() {
        val d = LocalDate(2026, 8, 21)
        val plan = EpochDateMigrationPlanner.plan(
            listOf(MigrationRow(rowId = 1, habitId = "h1", dateMillis = midnightMillis(d))),
            tz,
        )
        assertEquals(listOf(1L to d.toEpochDays().toLong()), plan.updates)
        assertTrue(plan.deletes.isEmpty())
    }

    @Test
    fun `collisions on the same habit and day keep the lowest rowid`() {
        // Two millis on the same calendar day (e.g. double-writes from
        // different write paths — the exact bug the migration fixes).
        val day = LocalDate(2026, 8, 21)
        val midnight = midnightMillis(day)
        val threeAm = midnight + 3 * 3_600_000L

        val plan = EpochDateMigrationPlanner.plan(
            listOf(
                MigrationRow(rowId = 7, habitId = "h1", dateMillis = midnight),
                MigrationRow(rowId = 3, habitId = "h1", dateMillis = threeAm),
            ),
            tz,
        )

        assertEquals(listOf(3L to day.toEpochDays().toLong()), plan.updates, "lowest rowid wins")
        assertEquals(listOf(7L), plan.deletes)
    }

    @Test
    fun `same day for different habits does not collide`() {
        val day = LocalDate(2026, 8, 21)
        val millis = midnightMillis(day)
        val plan = EpochDateMigrationPlanner.plan(
            listOf(
                MigrationRow(rowId = 1, habitId = "h1", dateMillis = millis),
                MigrationRow(rowId = 2, habitId = "h2", dateMillis = millis),
            ),
            tz,
        )
        assertEquals(2, plan.updates.size)
        assertTrue(plan.deletes.isEmpty())
    }

    @Test
    fun `empty input yields empty plan`() {
        val plan = EpochDateMigrationPlanner.plan(emptyList(), tz)
        assertTrue(plan.updates.isEmpty())
        assertTrue(plan.deletes.isEmpty())
    }
}
