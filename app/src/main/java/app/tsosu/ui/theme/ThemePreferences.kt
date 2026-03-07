package app.tsosu.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

enum class DarkModeOption { SYSTEM, LIGHT, DARK }

class ThemePreferences(private val context: Context) {

    private companion object {
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DARK_MODE = intPreferencesKey("dark_mode")
    }

    val dynamicColor: Flow<Boolean> = context.themeDataStore.data
        .map { it[DYNAMIC_COLOR] ?: false }

    val darkMode: Flow<DarkModeOption> = context.themeDataStore.data
        .map { prefs ->
            DarkModeOption.entries.getOrElse(prefs[DARK_MODE] ?: 0) { DarkModeOption.SYSTEM }
        }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.themeDataStore.edit { it[DYNAMIC_COLOR] = enabled }
    }

    suspend fun setDarkMode(option: DarkModeOption) {
        context.themeDataStore.edit { it[DARK_MODE] = option.ordinal }
    }
}
