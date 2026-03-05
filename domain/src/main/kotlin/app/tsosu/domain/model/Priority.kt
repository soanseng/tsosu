package app.tsosu.domain.model

enum class Priority(val value: Int, val color: Long) {
    NONE(0, 0xFF808080),
    LOW(1, 0xFF4A90D9),
    MEDIUM(2, 0xFFF5A623),
    HIGH(3, 0xFFEB8909),
    URGENT(4, 0xFFD1453B);

    companion object {
        fun fromValue(value: Int): Priority =
            entries.firstOrNull { it.value == value } ?: NONE
    }
}
