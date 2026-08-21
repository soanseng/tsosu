package app.tsosu.ui.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback

/**
 * Widget checkbox toggle. Runs through [ToggleTaskDoneUseCase] so recurring
 * tasks expand to their next occurrence, completions are recorded, energy is
 * awarded, and the calendar event is kept in sync — identical semantics to the
 * in-app toggle.
 */
class ToggleTaskAction : ActionCallback {
    companion object {
        val TaskIdKey = ActionParameters.Key<String>("taskId")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val taskId = parameters[TaskIdKey] ?: return
        val entry = WidgetEntryPoint.get(context)
        entry.toggleTaskDone()(taskId).getOrNull()?.let { task ->
            entry.reminderScheduler().schedule(task)
        }
        FocusWidget().update(context, glanceId)
    }
}
