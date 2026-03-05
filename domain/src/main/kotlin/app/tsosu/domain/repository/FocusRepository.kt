package app.tsosu.domain.repository

import app.tsosu.domain.model.DailyFocus
import app.tsosu.domain.model.WeeklyReview
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface FocusRepository {
    fun getDailyFocus(date: LocalDate): Flow<DailyFocus?>
    suspend fun setDailyFocus(date: LocalDate, taskIds: List<String>): Result<DailyFocus>
    fun getWeeklyReview(weekStart: LocalDate): Flow<WeeklyReview>
}
