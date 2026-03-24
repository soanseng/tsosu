package app.tsosu.ui.screens.quickadd

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tsosu.R

@Composable
fun QuickAddHabitSheet(
    onDismiss: () -> Unit,
    onAdd: (title: String, tinyVersion: String?) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var tinyVersion by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.quick_add_habit_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(stringResource(R.string.quick_add_habit_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = tinyVersion,
            onValueChange = { tinyVersion = it },
            label = { Text(stringResource(R.string.quick_add_tiny_version)) },
            supportingText = { Text(stringResource(R.string.quick_add_tiny_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                if (title.isNotBlank()) {
                    onAdd(title, tinyVersion.takeIf { it.isNotBlank() })
                    onDismiss()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.quick_add_submit_habit))
        }
    }
}
