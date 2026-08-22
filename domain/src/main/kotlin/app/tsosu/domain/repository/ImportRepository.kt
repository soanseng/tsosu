package app.tsosu.domain.repository

enum class ImportFormat { TODOIST_CSV, TODOIST_JSON }

sealed class ImportTarget {
    data object Inbox : ImportTarget()
    data class ExistingProject(val projectId: String) : ImportTarget()
    data class NewProject(val name: String) : ImportTarget()
}

data class ImportResult(
    val tasksImported: Int,
    val projectsImported: Int,
    val labelsImported: Int,
    val warnings: List<String> = emptyList(),
)

interface ImportRepository {
    suspend fun importFromTodoist(
        data: ByteArray,
        format: ImportFormat,
        target: ImportTarget = ImportTarget.Inbox,
    ): Result<ImportResult>

    /** Imports a TickTick CSV export straight into the inbox. */
    suspend fun importFromTickTick(data: ByteArray): Result<ImportResult>
}
