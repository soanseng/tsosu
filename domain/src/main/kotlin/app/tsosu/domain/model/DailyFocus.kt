package app.tsosu.domain.model

import kotlinx.datetime.LocalDate

data class DailyFocus(
    val date: LocalDate,
    val taskIds: List<String>,
    val completedCount: Int = 0,
)
