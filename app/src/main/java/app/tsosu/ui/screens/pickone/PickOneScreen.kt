package app.tsosu.ui.screens.pickone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tsosu.domain.model.EnergyLevel

@Composable
fun PickOneScreen(viewModel: PickOneViewModel = hiltViewModel()) {
    val pickedTask by viewModel.pickedTask.collectAsStateWithLifecycle()
    val selectedEnergy by viewModel.selectedEnergy.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Pick One",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "How's your energy right now?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EnergyLevel.entries.forEach { level ->
                FilterChip(
                    selected = selectedEnergy == level,
                    onClick = { viewModel.selectEnergy(level) },
                    label = { Text("${level.emoji} ${level.name.lowercase()}") },
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        pickedTask?.let { task ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    task.estimatedMinutes?.let { min ->
                        Text(
                            text = "$min min",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(onClick = { viewModel.pickAnother() }) {
                Text("Pick another")
            }
        } ?: run {
            Text(
                text = "No tasks at this energy level. Try another!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
