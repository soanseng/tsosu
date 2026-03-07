package app.tsosu.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProgressCard(
    completedCount: Int,
    totalCount: Int,
    totalMinutes: Int,
    streakDays: Int,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "progress",
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(56.dp),
                strokeWidth = 6.dp,
                trackColor = MaterialTheme.colorScheme.primaryContainer,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.width(20.dp))

            Column {
                Text(
                    text = "$completedCount / $totalCount tasks",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(4.dp))
                Row {
                    if (totalMinutes > 0) {
                        Text(
                            text = "${totalMinutes}m est.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    if (streakDays > 0) {
                        Text(
                            text = "$streakDays day streak",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
