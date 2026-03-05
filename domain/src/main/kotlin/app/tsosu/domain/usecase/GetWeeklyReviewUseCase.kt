package app.tsosu.domain.usecase

import app.tsosu.domain.model.WeeklyReview
import app.tsosu.domain.repository.FocusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class GetWeeklyReviewUseCase(
    private val focusRepository: FocusRepository,
) {
    operator fun invoke(weekStart: LocalDate): Flow<WeeklyReview> {
        return focusRepository.getWeeklyReview(weekStart)
    }
}
