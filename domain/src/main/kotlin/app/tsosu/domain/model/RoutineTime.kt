package app.tsosu.domain.model

enum class RoutineTime(val emoji: String) {
    MORNING("\uD83C\uDF05"),
    AFTERNOON("☀\uFE0F"),
    EVENING("\uD83C\uDF19");

    companion object {
        fun fromOrdinal(ordinal: Int): RoutineTime =
            entries.getOrElse(ordinal) { MORNING }
    }
}
