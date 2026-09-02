package app.tsosu.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tsosu.R

/**
 * Explains the gamification loop (⚡ earn → ❄ buy → auto-bridge → 🔥 keep).
 * Shared by the Habits ⓘ, the top-bar ⚡ and the Settings entry so the rules
 * read identically everywhere. Numbers must match
 * [app.tsosu.domain.repository.GamificationRepository.FREEZE_COST]/MAX_FREEZES,
 * TaskRepositoryImpl.ENERGY_PER_TASK and HabitStreakCalculator's 3-day bridge
 * window — update the strings when those change.
 */
@Composable
fun GamificationHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.task_detail_ok))
            }
        },
        title = { Text(stringResource(R.string.gamification_help_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.gamification_help_energy),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(R.string.gamification_help_shield_buy),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(R.string.gamification_help_shield_auto),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(R.string.gamification_help_streak),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
    )
}
