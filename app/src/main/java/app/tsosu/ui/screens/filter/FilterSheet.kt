package app.tsosu.ui.screens.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tsosu.R
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.FilterSpec
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.SortField
import app.tsosu.domain.model.SortSpec
import app.tsosu.domain.model.TaskStatus
import app.tsosu.ui.util.rememberHaptic

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    currentFilter: FilterSpec,
    currentSort: SortSpec,
    onApply: (FilterSpec, SortSpec) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val haptic = rememberHaptic()

    var selectedStatuses by remember {
        mutableStateOf(currentFilter.statuses ?: emptySet())
    }
    var selectedMinPriority by remember {
        mutableStateOf(currentFilter.minPriority)
    }
    var selectedEnergyLevels by remember {
        mutableStateOf(currentFilter.energyLevels ?: emptySet())
    }
    var titleSearch by remember {
        mutableStateOf(currentFilter.titleContains ?: "")
    }
    var sortField by remember {
        mutableStateOf(currentSort.field)
    }
    var sortAscending by remember {
        mutableStateOf(currentSort.ascending)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.filter_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        // -- Title search --
        OutlinedTextField(
            value = titleSearch,
            onValueChange = { titleSearch = it },
            label = { Text(stringResource(R.string.filter_search_title)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        // -- Status filter --
        Text(stringResource(R.string.filter_status), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TaskStatus.entries.forEach { status ->
                FilterChip(
                    selected = status in selectedStatuses,
                    onClick = {
                        haptic.tick()
                        selectedStatuses = if (status in selectedStatuses) {
                            selectedStatuses - status
                        } else {
                            selectedStatuses + status
                        }
                    },
                    label = {
                        Text(
                            status.name.lowercase().replace('_', ' ')
                                .replaceFirstChar { it.uppercase() },
                        )
                    },
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        // -- Priority filter (minimum priority) --
        Text(stringResource(R.string.filter_min_priority), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Priority.entries.forEach { p ->
                FilterChip(
                    selected = selectedMinPriority == p,
                    onClick = {
                        haptic.tick()
                        selectedMinPriority = if (selectedMinPriority == p) null else p
                    },
                    label = {
                        Text(
                            text = p.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = if (selectedMinPriority == p) Color(p.color) else Color.Unspecified,
                        )
                    },
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        // -- Energy filter --
        Text(stringResource(R.string.filter_energy_level), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EnergyLevel.entries.forEach { level ->
                FilterChip(
                    selected = level in selectedEnergyLevels,
                    onClick = {
                        haptic.tick()
                        selectedEnergyLevels = if (level in selectedEnergyLevels) {
                            selectedEnergyLevels - level
                        } else {
                            selectedEnergyLevels + level
                        }
                    },
                    label = { Text("${level.emoji} ${level.name.lowercase()}") },
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        // -- Sort --
        Text(stringResource(R.string.filter_sort_by), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SortField.entries.forEach { field ->
                FilterChip(
                    selected = sortField == field,
                    onClick = {
                        haptic.tick()
                        if (sortField == field) {
                            sortAscending = !sortAscending
                        } else {
                            sortField = field
                            sortAscending = true
                        }
                    },
                    label = {
                        val arrow = if (sortField == field) {
                            if (sortAscending) " \u2191" else " \u2193"
                        } else {
                            ""
                        }
                        Text(
                            field.name.lowercase().replace('_', ' ')
                                .replaceFirstChar { it.uppercase() } + arrow,
                        )
                    },
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // -- Action buttons --
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = {
                    haptic.tick()
                    onClear()
                    onDismiss()
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.filter_clear))
            }
            Button(
                onClick = {
                    haptic.confirm()
                    val filter = FilterSpec(
                        statuses = selectedStatuses.takeIf { it.isNotEmpty() },
                        minPriority = selectedMinPriority,
                        energyLevels = selectedEnergyLevels.takeIf { it.isNotEmpty() },
                        titleContains = titleSearch.takeIf { it.isNotBlank() },
                    )
                    val sort = SortSpec(field = sortField, ascending = sortAscending)
                    onApply(filter, sort)
                    onDismiss()
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.filter_apply))
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
