package app.tsosu.data.local

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Pure planner for the epoch-millis -> epoch-days date migration
 * (MIGRATION_10_11). Extracted so the conversion + collision handling is
 * unit-testable without a database.
 */
internal object EpochDateMigrationPlanner {

    data class MigrationRow(val rowId: Long, val habitId: String, val dateMillis: Long)

    /** Winner rows get `UPDATE ... SET date = epochDays`; loser rows get deleted. */
    data class Plan(
        val updates: List<Pair<Long, Long>>,
        val deletes: List<Long>,
    )

    /**
     * Converts each row's local-midnight millis to epoch days in [tz], and
     * collapses collisions (same habit + day, caused by the very timezone bug
     * being fixed) keeping the lowest rowid.
     */
    fun plan(rows: List<MigrationRow>, tz: TimeZone): Plan {
        data class Key(val habitId: String, val epochDays: Long)

        val byKey = rows
            .map { row ->
                val days = Instant.fromEpochMilliseconds(row.dateMillis)
                    .toLocalDateTime(tz).date.toEpochDays().toLong()
                Key(row.habitId, days) to row.rowId
            }
            .groupBy({ it.first }, { it.second })

        val updates = mutableListOf<Pair<Long, Long>>()
        val deletes = mutableListOf<Long>()
        for ((key, rowIds) in byKey) {
            val winner = rowIds.min()
            updates.add(winner to key.epochDays)
            deletes.addAll(rowIds.filter { it != winner })
        }
        return Plan(updates, deletes)
    }
}
