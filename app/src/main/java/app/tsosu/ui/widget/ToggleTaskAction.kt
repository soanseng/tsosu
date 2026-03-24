package app.tsosu.ui.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback

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
        val dao = WidgetEntryPoint.get(context).taskDao()
        val now = System.currentTimeMillis()
        val task = dao.getByIdSync(taskId) ?: return
        val newStatus = if (task.status == DONE_ORDINAL) TODO_ORDINAL else DONE_ORDINAL
        val completedDate = if (newStatus == DONE_ORDINAL) now else null
        dao.setStatus(taskId, newStatus, completedDate, null, now)
        FocusWidget().update(context, glanceId)
    }
}

private const val TODO_ORDINAL = 0
private const val DONE_ORDINAL = 4
