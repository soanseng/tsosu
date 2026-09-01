package app.tsosu.ui.screens.pickone

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tsosu.domain.model.EnergyLevel
import androidx.compose.ui.res.stringResource
import app.tsosu.R
import app.tsosu.ui.util.localizedLabel
import app.tsosu.ui.util.rememberHaptic

@Composable
fun PickOneSheet(
    onDismiss: () -> Unit,
    viewModel: PickOneViewModel = hiltViewModel(),
) {
    val pickedTask by viewModel.pickedTask.collectAsStateWithLifecycle()
    val selectedEnergy by viewModel.selectedEnergy.collectAsStateWithLifecycle()
    val haptic = rememberHaptic()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.pick_one_title), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.pick_one_energy_prompt),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EnergyLevel.entries.forEach { level ->
                FilterChip(
                    selected = selectedEnergy == level,
                    onClick = {
                        haptic.tick()
                        viewModel.selectEnergy(level)
                    },
                    label = { Text(level.localizedLabel()) },
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        pickedTask?.let { task ->
            AnimatedVisibility(
                visible = true,
                enter = scaleIn(initialScale = 0.8f) + fadeIn(),
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(task.title, style = MaterialTheme.typography.headlineSmall)
                        task.estimatedMinutes?.let { min ->
                            Text(
                                stringResource(R.string.pick_one_minutes, min),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = {
                    haptic.tick()
                    viewModel.pickAnother()
                }) { Text(stringResource(R.string.pick_one_another)) }
                Button(onClick = {
                    haptic.confirm()
                    onDismiss()
                }) { Text(stringResource(R.string.pick_one_start)) }
            }
        } ?: run {
            Text(
                stringResource(R.string.pick_one_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}
