package app.tsosu.domain.repository

enum class ImportFormat { TODOIST_CSV, TODOIST_JSON }

data class ImportResult(val tasksImported: Int, val projectsImported: Int, val labelsImported: Int)

interface ImportRepository {
    suspend fun importFromTodoist(data: ByteArray, format: ImportFormat): Result<ImportResult>
}
