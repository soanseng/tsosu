package app.tsosu.domain.repository

import app.tsosu.domain.model.Project
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun getAllProjects(): Flow<List<Project>>
    fun getProject(projectId: String): Flow<Project?>
    suspend fun createProject(project: Project): Result<Project>
}
