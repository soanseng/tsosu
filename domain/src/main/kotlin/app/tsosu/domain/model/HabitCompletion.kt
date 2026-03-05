package app.tsosu.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

data class HabitCompletion(
    val habitId: String,
    val date: LocalDate,
    val completedAt: Instant,
)
