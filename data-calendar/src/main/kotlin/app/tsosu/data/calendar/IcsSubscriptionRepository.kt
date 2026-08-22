package app.tsosu.data.calendar

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import okhttp3.OkHttpClient
import okhttp3.Request

private val Context.icsDataStore by preferencesDataStore(name = "ics_subscriptions")

/**
 * Read-only ICS subscriptions: user pastes calendar feed URLs, the events
 * overlay the in-app calendar. Fetch failures skip the URL silently — a
 * broken feed must never take the calendar down.
 */
@Singleton
class IcsSubscriptionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val client = OkHttpClient()

    private companion object {
        val URLS = stringSetPreferencesKey("urls")
    }

    val urls: Flow<Set<String>> = context.icsDataStore.data.map { it[URLS] ?: emptySet() }

    suspend fun addUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isNotEmpty()) {
            context.icsDataStore.edit { it[URLS] = (it[URLS] ?: emptySet()) + trimmed }
        }
    }

    suspend fun removeUrl(url: String) {
        context.icsDataStore.edit { it[URLS] = (it[URLS] ?: emptySet()) - url }
    }

    /** Fetches every subscription and returns events overlapping the window. */
    suspend fun fetchEvents(windowStart: LocalDate, windowEnd: LocalDate): List<IcsEvent> =
        withContext(Dispatchers.IO) {
            urls.first().flatMap { url ->
                runCatching {
                    val response = client.newCall(Request.Builder().url(url).build()).execute()
                    response.use { resp ->
                        if (!resp.isSuccessful) return@runCatching emptyList()
                        resp.body?.string()?.let { IcsParser.parse(it, windowStart, windowEnd) }
                            ?: emptyList()
                    }
                }.getOrDefault(emptyList())
            }
        }
}
