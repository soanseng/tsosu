package app.tsosu.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.tsosu.R
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Priority

/** Localized energy label with emoji prefix, e.g. "🫫 低". */
@Composable
fun EnergyLevel.localizedLabel(): String = "$emoji " + stringResource(
    when (this) {
        EnergyLevel.LOW -> R.string.energy_low
        EnergyLevel.MEDIUM -> R.string.energy_medium
        EnergyLevel.HIGH -> R.string.energy_high
    },
)

/** Localized priority name, e.g. "緊急". */
@Composable
fun Priority.localizedName(): String = stringResource(
    when (this) {
        Priority.NONE -> R.string.priority_none
        Priority.LOW -> R.string.priority_low
        Priority.MEDIUM -> R.string.priority_medium
        Priority.HIGH -> R.string.priority_high
        Priority.URGENT -> R.string.priority_urgent
    },
)
