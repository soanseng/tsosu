package app.tsosu.data.markdown

import app.tsosu.data.local.dao.ProjectDao
import app.tsosu.data.local.dao.TaskDao
import app.tsosu.data.local.entity.ProjectEntity
import app.tsosu.data.local.mapper.toEntity
import app.tsosu.domain.model.Task
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * The vault carries categories as section names ("## 論文") or note
 * frontmatter ("project:") — the pull must resolve them back to project ids,
 * or every sync wipes the category off inline tasks.
 */
class MarkdownSyncRepositoryPullTest {

    private val preferences = mockk<MarkdownPreferences>(relaxed = true)
    private val syncManager = mockk<MarkdownSyncManager>()
    private val taskDao = mockk<TaskDao>(relaxed = true)
    private val projectDao = mockk<ProjectDao>(relaxed = true)
    private val repository = MarkdownSyncRepository(preferences, syncManager, taskDao, projectDao)

    private fun paper(id: String = "p1") = ProjectEntity(id = id, title = "論文")

    @Test
    fun `a vault section files the task under that project, creating it if needed`() = runTest {
        val task = Task(id = "t1", title = "Read")
        coEvery { syncManager.importTasks() } returns ParsedTasks(
            tasks = listOf(task),
            projectSections = mapOf("t1" to "論文"),
        )
        every { taskDao.getAllTasks() } returns MutableStateFlow(emptyList())
        every { projectDao.getAll() } returns MutableStateFlow(emptyList())
        val createdProject = slot<ProjectEntity>()
        coEvery { projectDao.insert(capture(createdProject)) } returns Unit

        repository.pull().getOrThrow()

        assertEquals("論文", createdProject.captured.title)
        coVerify {
            taskDao.upsert(withArg { entity ->
                assertEquals(createdProject.captured.id, entity.projectId)
            })
        }
    }

    @Test
    fun `an existing project is reused and a task with no section is uncategorized`() = runTest {
        val filed = Task(id = "t1", title = "Read")
        val loose = Task(id = "t2", title = "Note")
        coEvery { syncManager.importTasks() } returns ParsedTasks(
            tasks = listOf(filed, loose),
            projectSections = mapOf("t1" to "論文"),
        )
        every { taskDao.getAllTasks() } returns MutableStateFlow(emptyList())
        every { projectDao.getAll() } returns MutableStateFlow(listOf(paper()))

        repository.pull().getOrThrow()

        coVerify(exactly = 0) { projectDao.insert(any()) }
        coVerify {
            taskDao.upsert(withArg { assertEquals("p1", it.projectId) })   // t1
        }
        coVerify {
            taskDao.upsert(withArg { assertNull(it.projectId) })           // t2
        }
    }
}
