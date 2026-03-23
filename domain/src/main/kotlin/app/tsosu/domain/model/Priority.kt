package app.tsosu.domain.model

enum class Priority(val value: Int, val color: Long, val emoji: String) {
    NONE(0, 0xFF808080, ""),
    LOW(1, 0xFF4A90D9, "\uD83D\uDD3D"),
    MEDIUM(2, 0xFFF5A623, "\uD83D\uDD3C"),
    HIGH(3, 0xFFEB8909, "\uD83D\uDD3A"),
    URGENT(4, 0xFFD1453B, "\u23EB");

    companion object {
        fun fromValue(value: Int): Priority =
            entries.firstOrNull { it.value == value } ?: NONE
    }
}
