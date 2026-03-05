package app.tsosu.domain.repository

import app.tsosu.domain.model.Task
import kotlinx.coroutines.flow.Flow

data class CalendarInfo(
    val id: String,
    val name: String,
)

enum class CalendarProvider { CALDAV, GOOGLE, NONE }

interface CalendarRepository {
    fun isConfigured(): Flow<Boolean>
    fun activeProvider(): Flow<CalendarProvider>
    suspend fun configureCaldav(serverUrl: String, email: String, password: String): Result<Unit>
    suspend fun configureGoogle(accessToken: String, refreshToken: String?, email: String): Result<Unit>
    suspend fun disconnect()
    suspend fun listCalendars(): Result<List<CalendarInfo>>
    suspend fun setDefaultCalendar(calendarId: String)
    suspend fun syncTaskToCalendar(task: Task): Result<String>
    suspend fun updateCalendarEvent(task: Task): Result<Unit>
    suspend fun removeCalendarEvent(eventId: String): Result<Unit>
}
