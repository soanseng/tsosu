package app.tsosu.data.markdown

import app.tsosu.domain.model.Task

/**
 * Detects tasks that were modified on BOTH sides since the last sync:
 * the vault version differs from the last exported state AND the app (Room)
 * version differs from it too. External edits still win, but the task is
 * flagged so the exported index can carry a `<!-- conflict -->` marker
 * instead of silently dropping one side.
 *
 * Canonical comparison uses the index-line serialization (title, status,
 * due/reminder/recurrence/priority/energy/estimate/created). Description-only
 * changes to noted tasks are not conflict-detected — documented limitation.
 */
class ConflictDetector(
    val serializer: MarkdownTaskSerializer = MarkdownTaskSerializer(),
) {

    fun detect(
        importedTasks: List<Task>,
        appTasks: List<Task>,
        lastExportedHashes: Map<String, String>,
    ): Set<String> {
        val importedById = importedTasks.associateBy { it.id }
        val appById = appTasks.associateBy { it.id }

        return lastExportedHashes.keys.filter { id ->
            val lastHash = lastExportedHashes[id] ?: return@filter false
            val externalChanged = importedById[id]
                ?.let { serializer.formatTask(it) != lastHash }
                ?: false
            val appChanged = appById[id]
                ?.let { serializer.formatTask(it) != lastHash }
                ?: false
            externalChanged && appChanged
        }.toSet()
    }
}
