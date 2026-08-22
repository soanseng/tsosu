package app.tsosu.domain.usecase

/**
 * Pure pomodoro state machine. Presets: 15/3, 25/5, 50/10
 * (work/break minutes). The UI ticks it once per second.
 */
object PomodoroEngine {

    data class Preset(val workMinutes: Int, val breakMinutes: Int) {
        val label: String get() = "${workMinutes}/${breakMinutes}"
    }

    enum class Phase { IDLE, WORK, BREAK, FINISHED_WORK, FINISHED_BREAK }

    data class State(
        val phase: Phase = Phase.IDLE,
        val secondsLeft: Int = 0,
        val completedWorkSessions: Int = 0,
        val preset: Preset = DEFAULT_PRESET,
    ) {
        val isRunning: Boolean get() = phase == Phase.WORK || phase == Phase.BREAK
    }

    val DEFAULT_PRESET = Preset(25, 5)
    val PRESETS = listOf(Preset(15, 3), DEFAULT_PRESET, Preset(50, 10))

    /** Fresh start from idle: resets the session count. */
    fun start(state: State, preset: Preset = state.preset): State =
        State(phase = Phase.WORK, secondsLeft = preset.workMinutes * 60, preset = preset)

    /** One-second tick; completes the phase when the countdown reaches zero. */
    fun tick(state: State): State = when (state.phase) {
        Phase.WORK -> {
            val left = state.secondsLeft - 1
            if (left <= 0) {
                state.copy(
                    phase = Phase.FINISHED_WORK,
                    secondsLeft = 0,
                    completedWorkSessions = state.completedWorkSessions + 1,
                )
            } else {
                state.copy(secondsLeft = left)
            }
        }
        Phase.BREAK -> {
            val left = state.secondsLeft - 1
            if (left <= 0) {
                state.copy(phase = Phase.FINISHED_BREAK, secondsLeft = 0)
            } else {
                state.copy(secondsLeft = left)
            }
        }
        else -> state
    }

    /** After a finished work phase: start the break. */
    fun startBreak(state: State): State =
        if (state.phase == Phase.FINISHED_WORK) {
            state.copy(
                phase = Phase.BREAK,
                secondsLeft = state.preset.breakMinutes * 60,
            )
        } else {
            state
        }

    /** After a finished break: back to work, keeping the session count. */
    fun startWork(state: State): State =
        if (state.phase == Phase.FINISHED_BREAK) {
            state.copy(phase = Phase.WORK, secondsLeft = state.preset.workMinutes * 60)
        } else {
            state
        }

    fun reset(): State = State()
}
