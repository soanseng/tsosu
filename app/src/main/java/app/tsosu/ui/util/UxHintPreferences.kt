package app.tsosu.ui.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.uxHintDataStore by preferencesDataStore(name = "ux_hints")

class UxHintPreferences(private val context: Context) {

    private companion object {
        val STATUS_LONG_PRESS_HINT_SHOWN = booleanPreferencesKey("status_long_press_hint_shown")
    }

    val statusLongPressHintShown: Flow<Boolean> = context.uxHintDataStore.data
        .map { it[STATUS_LONG_PRESS_HINT_SHOWN] ?: false }

    suspend fun markStatusLongPressHintShown() {
        context.uxHintDataStore.edit { it[STATUS_LONG_PRESS_HINT_SHOWN] = true }
    }
}
