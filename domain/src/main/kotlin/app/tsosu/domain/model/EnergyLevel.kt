package app.tsosu.domain.model

enum class EnergyLevel(val emoji: String, val labelTitle: String) {
    LOW("\uD83E\uDEAB", "\uD83E\uDEABlow"),
    MEDIUM("\uD83D\uDE10", "\uD83D\uDE10medium"),
    HIGH("\uD83D\uDD0B", "⚡high");

    companion object {
        fun fromOrdinal(ordinal: Int): EnergyLevel =
            entries.getOrElse(ordinal) { MEDIUM }
    }
}
