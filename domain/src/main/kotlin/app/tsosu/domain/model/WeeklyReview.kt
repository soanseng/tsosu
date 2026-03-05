package app.tsosu.domain.model

import kotlinx.datetime.LocalDate

data class WeeklyReview(
    val weekStart: LocalDate,
    val tasksCompleted: Int,
    val habitsCompletedTotal: Int,
    val focusDaysCompleted: Int,
    val totalEstimatedMinutes: Int,
    val topProject: String?,
    val longestHabitStreak: HabitStreakInfo?,
)
