package app.tsosu.data.local.repository

import app.tsosu.data.local.dao.ProjectDao
import app.tsosu.data.local.mapper.toDomain
import app.tsosu.data.local.mapper.toEntity
import app.tsosu.domain.model.Project
import app.tsosu.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProjectRepositoryImpl(
    private val projectDao: ProjectDao,
) : ProjectRepository {

    override fun getAllProjects(): Flow<List<Project>> =
        projectDao.getAll().map { entities -> entities.map { it.toDomain() } }

    override fun getProject(projectId: String): Flow<Project?> =
        projectDao.getById(projectId).map { it?.toDomain() }

    override suspend fun createProject(project: Project): Result<Project> = runCatching {
        projectDao.insert(project.toEntity())
        project
    }
}
