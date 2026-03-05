package app.tsosu.domain.usecase

import app.tsosu.domain.model.WeeklyReview
import app.tsosu.domain.repository.FocusRepository
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GetWeeklyReviewUseCaseTest {
    private val focusRepository = mockk<FocusRepository>()
    private val useCase = GetWeeklyReviewUseCase(focusRepository)

    @Test
    fun `returns weekly review from repository`() = runTest {
        val weekStart = LocalDate(2026, 3, 2)
        val review = WeeklyReview(
            weekStart = weekStart,
            tasksCompleted = 12,
            habitsCompletedTotal = 25,
            focusDaysCompleted = 5,
            totalEstimatedMinutes = 360,
            topProject = "Work",
            longestHabitStreak = null,
        )
        every { focusRepository.getWeeklyReview(weekStart) } returns flowOf(review)

        useCase(weekStart).test {
            val result = awaitItem()
            assertEquals(12, result.tasksCompleted)
            assertEquals(25, result.habitsCompletedTotal)
            assertEquals(5, result.focusDaysCompleted)
            awaitComplete()
        }
    }
}
