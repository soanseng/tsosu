package app.tsosu.ui.screens.filter

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.FilterSpec
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.SortField
import app.tsosu.domain.model.SortSpec
import app.tsosu.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

private val Context.savedViewDataStore by preferencesDataStore(name = "saved_views")

data class SavedView(
    val name: String,
    val filter: FilterSpec,
    val sort: SortSpec,
)

/**
 * Named filter/sort presets persisted as compact strings in DataStore.
 * Format (| separated, empty segment = null):
 * name|s=1,2|p>=2|e=0,1|pr=id1,id2|from=2026-08-01|to=2026-08-31|q=text|sort=PRIORITY,asc
 */
class SavedViewPreferences(private val context: Context) {

    private val key = stringSetPreferencesKey("views")

    val views: Flow<List<SavedView>> = context.savedViewDataStore.data.map { prefs ->
        (prefs[key] ?: emptySet()).mapNotNull(::decode).sortedBy { it.name }
    }

    suspend fun save(name: String, filter: FilterSpec, sort: SortSpec) {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty() && !trimmed.contains('|') && !trimmed.contains('\n')) {
            "Invalid view name"
        }
        context.savedViewDataStore.edit { prefs ->
            val existing = (prefs[key] ?: emptySet()).filterNot { decode(it)?.name == trimmed }.toSet()
            prefs[key] = existing + encode(trimmed, filter, sort)
        }
    }

    suspend fun delete(name: String) {
        context.savedViewDataStore.edit { prefs ->
            prefs[key] = (prefs[key] ?: emptySet()).filterNot { decode(it)?.name == name }.toSet()
        }
    }

    internal fun encode(name: String, filter: FilterSpec, sort: SortSpec): String {
        val parts = mutableListOf(name)
        filter.statuses?.let { parts += "s=" + it.joinToString(",") { s -> s.ordinal.toString() } }
        filter.minPriority?.let { parts += "p>=${it.value}" }
        filter.energyLevels?.let { parts += "e=" + it.joinToString(",") { e -> e.ordinal.toString() } }
        filter.projectIds?.let { parts += "pr=" + it.joinToString(",") }
        filter.dueDateFrom?.let { parts += "from=$it" }
        filter.dueDateTo?.let { parts += "to=$it" }
        filter.titleContains?.let { parts += "q=" + it.replace("|", " ").replace("\n", " ") }
        parts += "sort=${sort.field.name},${if (sort.ascending) "asc" else "desc"}"
        return parts.joinToString("|")
    }

    internal fun decode(line: String): SavedView? {
        val parts = line.split('|')
        val name = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return null
        var statuses: Set<TaskStatus>? = null
        var minPriority: Priority? = null
        var energies: Set<EnergyLevel>? = null
        var projects: Set<String>? = null
        var from: LocalDate? = null
        var to: LocalDate? = null
        var query: String? = null
        var sort = SortSpec()

        for (part in parts.drop(1)) {
            val value = part.substringAfter('=', "")
            when {
                part.startsWith("s=") -> statuses = value.split(',')
                    .mapNotNull { TaskStatus.entries.getOrNull(it.toIntOrNull() ?: -1) }.toSet().takeIf { it.isNotEmpty() }
                part.startsWith("p>=") -> minPriority = Priority.fromValue(value.toIntOrNull() ?: 0)
                part.startsWith("e=") -> energies = value.split(',')
                    .mapNotNull { EnergyLevel.entries.getOrNull(it.toIntOrNull() ?: -1) }.toSet().takeIf { it.isNotEmpty() }
                part.startsWith("pr=") -> projects = value.split(',').filter { it.isNotBlank() }.toSet().takeIf { it.isNotEmpty() }
                part.startsWith("from=") -> from = runCatching { LocalDate.parse(value) }.getOrNull()
                part.startsWith("to=") -> to = runCatching { LocalDate.parse(value) }.getOrNull()
                part.startsWith("q=") -> query = value.takeIf { it.isNotBlank() }
                part.startsWith("sort=") -> {
                    val seg = value.split(',')
                    val field = seg.getOrNull(0)?.let { f -> SortField.entries.firstOrNull { it.name == f } }
                    if (field != null) sort = SortSpec(field = field, ascending = seg.getOrNull(1) != "desc")
                }
            }
        }
        return SavedView(name, FilterSpec(statuses, minPriority, energies, projects, from, to, query, hasDescription = null), sort)
    }
}
