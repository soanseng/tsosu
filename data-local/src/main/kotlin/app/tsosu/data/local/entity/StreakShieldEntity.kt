package app.tsosu.data.local.entity

import androidx.room.Entity


/** A streak gap day that was bridged by consuming a streak freeze. */
@Entity(tableName = "streak_shields", primaryKeys = ["habitId", "date"])
data class StreakShieldEntity(
    val habitId: String,
    val date: Long,
)
