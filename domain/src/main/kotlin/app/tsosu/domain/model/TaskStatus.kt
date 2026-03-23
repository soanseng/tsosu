package app.tsosu.domain.model

enum class TaskStatus(val checkboxMarker: Char) {
    TODO(' '),
    IN_PROGRESS('/'),
    ON_HOLD('!'),
    PLANNED('>'),
    DONE('x'),
    CANCELLED('-');

    val isDone: Boolean get() = this == DONE
    val isTerminal: Boolean get() = this == DONE || this == CANCELLED

    companion object {
        fun fromCheckboxChar(char: Char): TaskStatus = when (char) {
            ' ' -> TODO
            '/' -> IN_PROGRESS
            '!' -> ON_HOLD
            '>' -> PLANNED
            'x', 'X' -> DONE
            '-' -> CANCELLED
            else -> TODO
        }

        fun fromOrdinal(ordinal: Int): TaskStatus =
            entries.getOrElse(ordinal) { TODO }
    }
}
