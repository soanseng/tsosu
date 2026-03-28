package app.tsosu.data.markdown

import app.tsosu.domain.repository.ImportFormat
import app.tsosu.domain.repository.ImportRepository
import app.tsosu.domain.repository.ImportResult
import app.tsosu.domain.repository.ImportTarget

class NoOpImportRepository : ImportRepository {
    override suspend fun importFromTodoist(
        data: ByteArray,
        format: ImportFormat,
        target: ImportTarget,
    ): Result<ImportResult> =
        Result.success(ImportResult(tasksImported = 0, projectsImported = 0, labelsImported = 0))
}
