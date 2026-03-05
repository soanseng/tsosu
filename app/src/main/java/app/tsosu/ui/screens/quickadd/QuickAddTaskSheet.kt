package app.tsosu.ui.screens.quickadd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Priority

@Composable
fun QuickAddTaskSheet(
    onDismiss: () -> Unit,
    onAdd: (title: String, priority: Priority, energy: EnergyLevel, estimatedMinutes: Int?) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(Priority.NONE) }
    var selectedEnergy by remember { mutableStateOf(EnergyLevel.MEDIUM) }
    var estimatedMinutes by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text("New Task", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("What needs doing?") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(Modifier.height(12.dp))

        Text("Energy", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EnergyLevel.entries.forEach { level ->
                FilterChip(
                    selected = selectedEnergy == level,
                    onClick = { selectedEnergy = level },
                    label = { Text("${level.emoji} ${level.name.lowercase()}") },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text("Time estimate", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 15, 30, 60).forEach { minutes ->
                FilterChip(
                    selected = estimatedMinutes == minutes,
                    onClick = { estimatedMinutes = minutes },
                    label = { Text("${minutes}m") },
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                if (title.isNotBlank()) {
                    onAdd(title, selectedPriority, selectedEnergy, estimatedMinutes.takeIf { it > 0 })
                    onDismiss()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Add Task")
        }
    }
}
