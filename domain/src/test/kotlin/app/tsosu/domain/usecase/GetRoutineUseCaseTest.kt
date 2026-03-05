package app.tsosu.domain.usecase

import app.tsosu.domain.model.Routine
import app.tsosu.domain.model.RoutineTime
import app.tsosu.domain.repository.RoutineRepository
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GetRoutineUseCaseTest {
    private val routineRepository = mockk<RoutineRepository>()
    private val useCase = GetRoutineUseCase(routineRepository)

    @Test
    fun `returns routine with habits`() = runTest {
        val routine = Routine(id = "r1", title = "Morning", timeOfDay = RoutineTime.MORNING)
        every { routineRepository.getRoutine("r1") } returns flowOf(routine)

        useCase("r1").test {
            val result = awaitItem()
            assertNotNull(result)
            assertEquals("Morning", result.title)
            assertEquals(RoutineTime.MORNING, result.timeOfDay)
            awaitComplete()
        }
    }

    @Test
    fun `returns null for unknown routine`() = runTest {
        every { routineRepository.getRoutine("unknown") } returns flowOf(null)

        useCase("unknown").test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }
}
