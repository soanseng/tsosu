package app.tsosu.domain.model

data class HabitStreakInfo(
    val habitId: String,
    val habitTitle: String,
    val completedLast7Days: Int,
    val completedLast30Days: Int,
    val currentConsecutiveDays: Int,
    val completionRate: Float,
)
