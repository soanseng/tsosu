package app.tsosu.domain.model

enum class HabitFrequency(val repeatAfterSeconds: Long) {
    DAILY(86400),
    WEEKDAYS(86400),
    CUSTOM(86400);

    companion object {
        fun fromOrdinal(ordinal: Int): HabitFrequency =
            entries.getOrElse(ordinal) { DAILY }
    }
}
