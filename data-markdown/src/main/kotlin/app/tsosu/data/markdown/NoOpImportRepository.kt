package app.tsosu.data.markdown

import app.tsosu.domain.repository.ImportFormat
import app.tsosu.domain.repository.ImportRepository
import app.tsosu.domain.repository.ImportResult

class NoOpImportRepository : ImportRepository {
    override suspend fun importFromTodoist(data: ByteArray, format: ImportFormat): Result<ImportResult> =
        Result.success(ImportResult(tasksImported = 0, projectsImported = 0, labelsImported = 0))
}
