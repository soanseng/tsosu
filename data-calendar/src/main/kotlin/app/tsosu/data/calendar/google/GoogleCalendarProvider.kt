package app.tsosu.data.calendar.google

import app.tsosu.domain.model.Task
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import java.util.TimeZone

class GoogleCalendarProvider {

    fun buildEvent(task: Task): Event {
        val dueDate = task.dueDate
            ?: throw IllegalArgumentException("Task must have a due date")

        val durationMinutes = task.estimatedMinutes ?: 60
        val startMillis = toEpochMillis(dueDate)
        val endMillis = startMillis + durationMinutes * 60 * 1000L

        val timeZone = TimeZone.getDefault().id

        return Event().apply {
            id = "tsosu-${task.id}"
            summary = task.title
            description = task.description.ifEmpty { null }
            start = EventDateTime()
                .setDateTime(DateTime(startMillis))
                .setTimeZone(timeZone)
            end = EventDateTime()
                .setDateTime(DateTime(endMillis))
                .setTimeZone(timeZone)
        }
    }

    private fun toEpochMillis(dt: kotlinx.datetime.LocalDateTime): Long {
        val javaLdt = java.time.LocalDateTime.of(
            dt.year, dt.monthNumber, dt.dayOfMonth,
            dt.hour, dt.minute, dt.second,
        )
        return javaLdt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
