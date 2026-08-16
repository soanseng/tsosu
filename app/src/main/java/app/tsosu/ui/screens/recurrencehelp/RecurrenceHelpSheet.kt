package app.tsosu.ui.screens.recurrencehelp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tsosu.R

/**
 * In-app reference for natural-language recurrence keywords. Shown from a
 * ModalBottomSheet; everything read-only text so it stays localizable.
 */
@Composable
fun RecurrenceHelpSheet() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            stringResource(R.string.recurrence_help_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.recurrence_help_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        HelpSection(R.string.recurrence_help_basic_title, R.string.recurrence_help_basic_body)
        HelpSection(R.string.recurrence_help_days_title, R.string.recurrence_help_days_body)
        HelpSection(R.string.recurrence_help_other_title, R.string.recurrence_help_other_body)
        HelpSection(R.string.recurrence_help_bang_title, R.string.recurrence_help_bang_body)
        HelpSection(R.string.recurrence_help_short_title, R.string.recurrence_help_short_body)
        HelpSection(R.string.recurrence_help_obsidian_title, R.string.recurrence_help_obsidian_body)

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun HelpSection(titleRes: Int, bodyRes: Int) {
    Text(
        stringResource(titleRes),
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        stringResource(bodyRes),
        style = MaterialTheme.typography.bodySmall,
    )
    Spacer(Modifier.height(12.dp))
}
