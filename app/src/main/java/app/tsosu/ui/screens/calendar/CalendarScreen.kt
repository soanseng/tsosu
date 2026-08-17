package app.tsosu.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tsosu.R
import app.tsosu.ui.components.KonfettiOverlay
import app.tsosu.ui.components.TaskListItem
import kotlinx.datetime.LocalDate
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel(),
    onTaskClick: (String) -> Unit = {},
    onQuickAddDate: (java.time.LocalDate) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val konfettiTrigger = remember { mutableIntStateOf(0) }

    KonfettiOverlay(konfettiTrigger)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        MonthHeader(
            yearMonth = state.yearMonth,
            onPrevious = viewModel::previousMonth,
            onNext = viewModel::nextMonth,
        )

        DayOfWeekHeaders()

        MonthGrid(
            yearMonth = state.yearMonth,
            selectedDate = state.selectedDate,
            tasksByDate = state.tasksByDate.mapKeys {
                java.time.LocalDate.of(it.key.year, it.key.monthNumber, it.key.dayOfMonth)
            },
            onDateSelected = { javaDate ->
                viewModel.selectDate(
                    LocalDate(javaDate.year, javaDate.monthValue, javaDate.dayOfMonth)
                )
            },
            onDateLongPress = onQuickAddDate,
        )

        if (state.selectedDate != null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatSelectedDateHeader(state.selectedDate!!),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                IconButton(onClick = { onQuickAddDate(toJavaDate(state.selectedDate!!)) }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.calendar_add_task))
                }
            }

            if (state.selectedDayTasks.isEmpty()) {
                Text(
                    text = "No tasks on this day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Long-press any day to quickly add a task.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(state.selectedDayTasks, key = { it.id }) { task ->
                        TaskListItem(
                            task = task,
                            onToggleDone = { id ->
                                viewModel.toggleDone(id)
                                konfettiTrigger.intValue++
                            },
                            onStatusChange = { id, status ->
                                viewModel.setStatus(id, status)
                            },
                            onClick = { onTaskClick(it.id) },
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    yearMonth: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.calendar_previous_month),
            )
        }

        Text(
            text = "${yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${yearMonth.year}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
        )

        IconButton(onClick = onNext) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.calendar_next_month),
            )
        }
    }
}

@Composable
private fun DayOfWeekHeaders() {
    val daysOfWeek = listOf(
        DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
    )

    Row(modifier = Modifier.fillMaxWidth()) {
        daysOfWeek.forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Spacer(Modifier.height(4.dp))
}

@Composable
private fun MonthGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate?,
    tasksByDate: Map<java.time.LocalDate, List<app.tsosu.domain.model.Task>>,
    onDateSelected: (java.time.LocalDate) -> Unit,
    onDateLongPress: (java.time.LocalDate) -> Unit,
) {
    val firstOfMonth = yearMonth.atDay(1)
    // Sunday = 0 offset for US-style calendar
    val startOffset = firstOfMonth.dayOfWeek.value % 7
    val daysInMonth = yearMonth.lengthOfMonth()
    val totalCells = startOffset + daysInMonth

    val selectedJavaDate = selectedDate?.let {
        java.time.LocalDate.of(it.year, it.monthNumber, it.dayOfMonth)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxWidth(),
        userScrollEnabled = false,
    ) {
        // Empty cells before the first day
        items(startOffset) {
            Box(modifier = Modifier.aspectRatio(1f))
        }

        // Day cells
        items(daysInMonth) { index ->
            val day = index + 1
            val date = yearMonth.atDay(day)
            val isSelected = date == selectedJavaDate
            val hasTasks = tasksByDate.containsKey(date)

            DayCell(
                day = day,
                isSelected = isSelected,
                hasTasks = hasTasks,
                onClick = { onDateSelected(date) },
                onLongClick = { onDateLongPress(date) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayCell(
    day: Int,
    isSelected: Boolean,
    hasTasks: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .then(
                if (isSelected) {
                    Modifier.background(
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape,
                    )
                } else {
                    Modifier
                }
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )

            if (hasTasks) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            CircleShape,
                        ),
                )
            }
        }
    }
}

private fun toJavaDate(date: LocalDate): java.time.LocalDate =
    java.time.LocalDate.of(date.year, date.monthNumber, date.dayOfMonth)

private fun formatSelectedDateHeader(date: LocalDate): String {
    val javaDate = java.time.LocalDate.of(date.year, date.monthNumber, date.dayOfMonth)
    val month = javaDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
    return "Tasks for $month ${date.dayOfMonth}"
}
