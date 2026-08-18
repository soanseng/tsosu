package app.tsosu.data.calendar

import app.tsosu.data.calendar.google.GoogleCalendarProvider
import app.tsosu.data.calendar.google.GoogleCredentialStore
import app.tsosu.data.calendar.google.GoogleCalendarCredentials
import app.tsosu.domain.model.Task
import app.tsosu.domain.repository.CalendarInfo
import app.tsosu.domain.repository.CalendarProvider
import app.tsosu.domain.repository.CalendarRepository
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.Calendar
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class CalendarRepositoryImpl(
    private val caldavCredentialStore: CalDavCredentialStore,
    private val googleCredentialStore: GoogleCredentialStore,
    private val webDavCredentialStore: WebDavCredentialStore,
    private val vEventBuilder: VEventBuilder = VEventBuilder(),
    private val googleProvider: GoogleCalendarProvider = GoogleCalendarProvider(),
) : CalendarRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun isConfigured(): Flow<Boolean> = combine(
        caldavCredentialStore.isConfigured(),
        googleCredentialStore.isConfigured(),
        webDavCredentialStore.isConfigured(),
    ) { caldav, google, webdav -> caldav || google || webdav }

    override fun activeProvider(): Flow<CalendarProvider> = combine(
        caldavCredentialStore.isConfigured(),
        googleCredentialStore.isConfigured(),
        webDavCredentialStore.isConfigured(),
    ) { caldav, google, webdav ->
        when {
            google -> CalendarProvider.GOOGLE
            caldav -> CalendarProvider.CALDAV
            webdav -> CalendarProvider.WEBDAV
            else -> CalendarProvider.NONE
        }
    }

    override suspend fun configureCaldav(
        serverUrl: String,
        email: String,
        password: String,
    ): Result<Unit> {
        return try {
            val request = Request.Builder()
                .url(serverUrl)
                .method("PROPFIND", "".toRequestBody("application/xml".toMediaType()))
                .header("Authorization", Credentials.basic(email, password))
                .header("Depth", "0")
                .build()

            val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
            response.use {
                if (it.isSuccessful || it.code == 207) {
                    // Disconnect the others when switching
                    googleCredentialStore.clear()
                    webDavCredentialStore.clear()
                    caldavCredentialStore.save(
                        calendarUrl = serverUrl,
                        email = email,
                        password = password,
                    )
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("CalDAV error: ${it.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun configureWebdav(
        baseUrl: String,
        username: String,
        password: String,
    ): Result<Unit> {
        return try {
            val request = Request.Builder()
                .url(baseUrl)
                .method("PROPFIND", "".toRequestBody("application/xml".toMediaType()))
                .header("Authorization", Credentials.basic(username, password))
                .header("Depth", "0")
                .build()

            val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
            response.use {
                if (it.isSuccessful || it.code == 207) {
                    googleCredentialStore.clear()
                    caldavCredentialStore.clear()
                    webDavCredentialStore.save(baseUrl, username, password)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("WebDAV error: ${it.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun configureGoogle(
        accessToken: String,
        refreshToken: String?,
        email: String,
    ): Result<Unit> {
        return try {
            // Verify token works by listing calendars
            val calendarService = buildGoogleCalendarService(accessToken)
            withContext(Dispatchers.IO) {
                calendarService.calendarList().list().setMaxResults(1).execute()
            }
            // Disconnect the others when switching
            caldavCredentialStore.clear()
            webDavCredentialStore.clear()
            googleCredentialStore.save(
                accessToken = accessToken,
                refreshToken = refreshToken,
                accountEmail = email,
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun disconnect() {
        caldavCredentialStore.clear()
        googleCredentialStore.clear()
        webDavCredentialStore.clear()
    }

    override suspend fun listCalendars(): Result<List<CalendarInfo>> {
        val googleCreds = googleCredentialStore.getCredentials()
        if (googleCreds != null) {
            return try {
                val service = buildGoogleCalendarService(googleCreds.accessToken)
                val list = withContext(Dispatchers.IO) {
                    service.calendarList().list().execute()
                }
                Result.success(
                    list.items.map { CalendarInfo(it.id, it.summary ?: it.id) }
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        return Result.success(listOf(CalendarInfo("default", "Tsosu Calendar")))
    }

    override suspend fun setDefaultCalendar(calendarId: String) {
        val googleCreds = googleCredentialStore.getCredentials()
        if (googleCreds != null) {
            googleCredentialStore.save(
                accessToken = googleCreds.accessToken,
                refreshToken = googleCreds.refreshToken,
                accountEmail = googleCreds.accountEmail,
                calendarId = calendarId,
            )
        }
    }

    override suspend fun syncTaskToCalendar(task: Task): Result<String> {
        val googleCreds = googleCredentialStore.getCredentials()
        if (googleCreds != null) {
            return syncToGoogle(task, googleCreds)
        }

        val caldavCreds = caldavCredentialStore.getCredentials()
        val webdavCreds = webDavCredentialStore.getCredentials()
        if (caldavCreds == null && webdavCreds == null) {
            return Result.failure(IllegalStateException("No calendar configured"))
        }

        val dueDate = task.dueDate
            ?: return Result.failure(IllegalArgumentException("Task has no due date"))

        val uid = "tsosu-${task.id}"
        val ical = vEventBuilder.buildVEvent(
            uid = uid,
            title = task.title,
            description = task.description,
            dueDate = dueDate.toString(),
            estimatedMinutes = task.estimatedMinutes,
        )

        return when {
            caldavCreds != null -> putIcs(
                caldavCreds.calendarUrl, caldavCreds.email, caldavCreds.password, uid, ical,
            )
            else -> putIcs(
                webdavCreds!!.baseUrl, webdavCreds.username, webdavCreds.password, uid, ical,
            )
        }
    }

    override suspend fun updateCalendarEvent(task: Task): Result<Unit> {
        syncTaskToCalendar(task)
        return Result.success(Unit)
    }

    override suspend fun removeCalendarEvent(eventId: String): Result<Unit> {
        val googleCreds = googleCredentialStore.getCredentials()
        if (googleCreds != null) {
            return try {
                val service = buildGoogleCalendarService(googleCreds.accessToken)
                withContext(Dispatchers.IO) {
                    service.events().delete(googleCreds.calendarId, eventId).execute()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        val caldavCreds = caldavCredentialStore.getCredentials()
        val webdavCreds = webDavCredentialStore.getCredentials()
        if (caldavCreds == null && webdavCreds == null) {
            return Result.failure(IllegalStateException("No calendar configured"))
        }

        return when {
            caldavCreds != null -> deleteIcs(
                caldavCreds.calendarUrl, caldavCreds.email, caldavCreds.password, eventId,
            )
            else -> deleteIcs(
                webdavCreds!!.baseUrl, webdavCreds.username, webdavCreds.password, eventId,
            )
        }
    }

    private suspend fun syncToGoogle(
        task: Task,
        creds: GoogleCalendarCredentials,
    ): Result<String> {
        return try {
            val event = googleProvider.buildEvent(task)
            val service = buildGoogleCalendarService(creds.accessToken)
            val eventId = event.id

            withContext(Dispatchers.IO) {
                try {
                    service.events().get(creds.calendarId, eventId).execute()
                    service.events().update(creds.calendarId, eventId, event).execute()
                } catch (_: com.google.api.client.googleapis.json.GoogleJsonResponseException) {
                    service.events().insert(creds.calendarId, event).execute()
                }
            }
            Result.success(eventId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildGoogleCalendarService(accessToken: String): Calendar {
        val credentials = GoogleCredentials.create(AccessToken(accessToken, null))
        val transport = GoogleNetHttpTransport.newTrustedTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()
        return Calendar.Builder(transport, jsonFactory, HttpCredentialsAdapter(credentials))
            .setApplicationName("Tsosu")
            .build()
    }

    /** PUTs `<base>/<uid>.ics` — works for both CalDAV servers and plain WebDAV. */
    private suspend fun putIcs(
        baseUrl: String,
        username: String,
        password: String,
        uid: String,
        ical: String,
    ): Result<String> {
        return try {
            val url = "${baseUrl.trimEnd('/')}/$uid.ics"
            val body = ical.toRequestBody("text/calendar; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .put(body)
                .header("Authorization", Credentials.basic(username, password))
                .build()

            val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
            response.use {
                if (it.isSuccessful || it.code == 201 || it.code == 204) {
                    Result.success(uid)
                } else {
                    Result.failure(Exception("WebDAV/CalDAV PUT failed: ${it.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** DELETEs `<base>/<eventId>.ics`. */
    private suspend fun deleteIcs(
        baseUrl: String,
        username: String,
        password: String,
        eventId: String,
    ): Result<Unit> {
        return try {
            val url = "${baseUrl.trimEnd('/')}/$eventId.ics"
            val request = Request.Builder()
                .url(url)
                .delete()
                .header("Authorization", Credentials.basic(username, password))
                .build()

            withContext(Dispatchers.IO) { client.newCall(request).execute() }.use {}
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
