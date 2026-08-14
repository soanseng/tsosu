package app.tsosu.data.markdown

import app.tsosu.data.local.dao.HabitDao
import app.tsosu.data.local.dao.ProjectDao
import app.tsosu.data.local.dao.TaskDao
import app.tsosu.data.local.entity.TaskEntity
import app.tsosu.domain.model.Task
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MarkdownSyncRepositoryTest {

    private val fixed = Instant.parse("2026-03-20T10:00:00Z")

    private fun task(id: String, title: String) = Task(
        id = id,
        title = title,
        createdAt = fixed,
        updatedAt = fixed,
    )

    private fun entity(id: String, title: String) = TaskEntity(
        id = id,
        title = title,
        createdAt = fixed.toEpochMilliseconds(),
        updatedAt = fixed.toEpochMilliseconds(),
    )

    private fun repo(
        preferences: MarkdownPreferences,
        syncManager: MarkdownSyncManager,
        taskDao: TaskDao,
        habitDao: HabitDao,
        projectDao: ProjectDao,
    ) = MarkdownSyncRepository(preferences, syncManager, taskDao, habitDao, projectDao)

    private class Mocks(
        val preferences: MarkdownPreferences,
        val syncManager: MarkdownSyncManager,
        val taskDao: TaskDao,
        val habitDao: HabitDao,
        val projectDao: ProjectDao,
    )

    private fun baseMocks(
        imported: Task,
        roomBefore: Task,
        roomAfter: Task,
        lastHashes: Map<String, String>,
    ): Mocks {
        val preferences = mockk<MarkdownPreferences>(relaxed = true)
        coEvery { preferences.getTaskHashes() } returns lastHashes
        val syncManager = mockk<MarkdownSyncManager>(relaxed = true)
        coEvery { syncManager.importTasks() } returns ParsedTasks(listOf(imported), emptyMap())
        coEvery { syncManager.importHabits() } returns ParsedHabits(emptyList(), emptyList())
        val taskDao = mockk<TaskDao>(relaxed = true)
        coEvery { taskDao.getAllTasks() } returnsMany listOf(
            flowOf(listOf(entity(roomBefore.id, roomBefore.title))),
            flowOf(listOf(entity(roomAfter.id, roomAfter.title))),
        )
        val habitDao = mockk<HabitDao>(relaxed = true)
        coEvery { habitDao.getActiveHabits() } returns flowOf(emptyList())
        val projectDao = mockk<ProjectDao>(relaxed = true)
        coEvery { projectDao.getAll() } returns flowOf(emptyList())
        return Mocks(preferences, syncManager, taskDao, habitDao, projectDao)
    }

    @Test
    fun `sync flags conflict and exports with conflictIds when both sides changed`() = runTest {
        val original = task("t1", "Original")
        val external = task("t1", "External edit")
        val app = task("t1", "App edit")
        val lastHash = ConflictDetector().serializer.formatTask(original)

        val mocks = baseMocks(
            imported = external,
            roomBefore = app,
            roomAfter = external,
            lastHashes = mapOf("t1" to lastHash),
        )

        val result = repo(
            mocks.preferences, mocks.syncManager, mocks.taskDao, mocks.habitDao, mocks.projectDao,
        ).sync()

        assertTrue(result.isSuccess)
        coVerify {
            mocks.syncManager.exportTasks(any(), any(), match { it == setOf("t1") })
        }
        coVerify { mocks.preferences.setTaskHashes(match { it.containsKey("t1") }) }
    }

    @Test
    fun `sync exports without conflictIds when only vault changed`() = runTest {
        val app = task("t1", "App version") // unchanged since last export
        val external = task("t1", "External edit")
        val lastHash = ConflictDetector().serializer.formatTask(app)

        val mocks = baseMocks(
            imported = external,
            roomBefore = app,
            roomAfter = external,
            lastHashes = mapOf("t1" to lastHash),
        )

        val result = repo(
            mocks.preferences, mocks.syncManager, mocks.taskDao, mocks.habitDao, mocks.projectDao,
        ).sync()

        assertTrue(result.isSuccess)
        coVerify {
            mocks.syncManager.exportTasks(any(), any(), match { it.isEmpty() })
        }
    }

    @Test
    fun `sync exports without conflictIds when only app changed`() = runTest {
        val original = task("t1", "Original")
        val app = task("t1", "App edit")
        val lastHash = ConflictDetector().serializer.formatTask(original)

        val mocks = baseMocks(
            imported = original,
            roomBefore = app,
            roomAfter = original,
            lastHashes = mapOf("t1" to lastHash),
        )

        val result = repo(
            mocks.preferences, mocks.syncManager, mocks.taskDao, mocks.habitDao, mocks.projectDao,
        ).sync()

        assertTrue(result.isSuccess)
        coVerify {
            mocks.syncManager.exportTasks(any(), any(), match { it.isEmpty() })
        }
    }
}
