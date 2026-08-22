package app.tsosu.domain.usecase

import app.tsosu.domain.usecase.PomodoroEngine.Phase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PomodoroEngineTest {

    @Test
    fun `start begins work phase with preset minutes`() {
        val s = PomodoroEngine.start(PomodoroEngine.State(), PomodoroEngine.Preset(15, 3))
        assertEquals(Phase.WORK, s.phase)
        assertEquals(15 * 60, s.secondsLeft)
    }

    @Test
    fun `ticks count down without changing phase`() {
        var s = PomodoroEngine.start(PomodoroEngine.State(), PomodoroEngine.Preset(25, 5))
        repeat(59) { s = PomodoroEngine.tick(s) }
        assertEquals(Phase.WORK, s.phase)
        assertEquals(25 * 60 - 59, s.secondsLeft)
    }

    @Test
    fun `work phase completion increments sessions and finishes`() {
        var s = PomodoroEngine.start(PomodoroEngine.State(), PomodoroEngine.Preset(15, 3))
        repeat(15 * 60) { s = PomodoroEngine.tick(s) }
        assertEquals(Phase.FINISHED_WORK, s.phase)
        assertEquals(0, s.secondsLeft)
        assertEquals(1, s.completedWorkSessions)
    }

    @Test
    fun `break alternates back to work`() {
        var s = PomodoroEngine.start(PomodoroEngine.State(), PomodoroEngine.Preset(15, 3))
        repeat(15 * 60) { s = PomodoroEngine.tick(s) }
        s = PomodoroEngine.startBreak(s)
        assertEquals(Phase.BREAK, s.phase)
        assertEquals(3 * 60, s.secondsLeft)
        repeat(3 * 60) { s = PomodoroEngine.tick(s) }
        assertEquals(Phase.FINISHED_BREAK, s.phase)
        s = PomodoroEngine.startWork(s)
        assertEquals(Phase.WORK, s.phase)
        assertEquals(15 * 60, s.secondsLeft)
        assertEquals(1, s.completedWorkSessions)
    }

    @Test
    fun `idle state ignores ticks`() {
        val s = PomodoroEngine.tick(PomodoroEngine.State())
        assertEquals(Phase.IDLE, s.phase)
    }

    @Test
    fun `reset returns to idle`() {
        val s = PomodoroEngine.reset()
        assertEquals(Phase.IDLE, s.phase)
        assertEquals(0, s.completedWorkSessions)
    }
}
