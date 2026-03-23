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
        dao.setStatus(taskId, DONE_ORDINAL, now, now)
        FocusWidget().update(context, glanceId)
    }
}

private const val DONE_ORDINAL = 4
