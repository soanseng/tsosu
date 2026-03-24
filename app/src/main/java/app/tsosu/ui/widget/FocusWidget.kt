package app.tsosu.ui.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.coroutines.flow.first

class FocusWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = WidgetEntryPoint.get(context).taskDao()
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis
        val endOfDay = startOfDay + 86_400_000
        val tasks = dao.getFocusTasks(startOfDay, endOfDay).first().take(MAX_TASKS)

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier.fillMaxSize().padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = "Focus",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = GlanceTheme.colors.onSurface,
                        ),
                    )
                    Spacer(GlanceModifier.size(8.dp))
                    if (tasks.isEmpty()) {
                        Text(
                            text = "No focus tasks",
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = GlanceTheme.colors.secondary,
                            ),
                        )
                    } else {
                        tasks.forEach { task ->
                            val isDone = task.status == DONE_ORDINAL
                            CheckBox(
                                checked = isDone,
                                onCheckedChange = actionRunCallback<ToggleTaskAction>(
                                    actionParametersOf(
                                        ToggleTaskAction.TaskIdKey to task.id,
                                    ),
                                ),
                                text = task.title,
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    color = GlanceTheme.colors.onSurface,
                                ),
                            )
                            Spacer(GlanceModifier.size(4.dp))
                        }
                    }
                }
            }
        }
    }
}

private const val DONE_ORDINAL = 4
private const val MAX_TASKS = 3
