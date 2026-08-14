package app.tsosu.data.markdown

import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ConflictDetectorTest {

    private val detector = ConflictDetector()

    private fun task(id: String, title: String = "Task $id", due: String? = null) = Task(
        id = id,
        title = title,
        status = TaskStatus.TODO,
        dueDate = due?.let { kotlinx.datetime.LocalDateTime.parse(it) },
        priority = Priority.NONE,
        energyLevel = EnergyLevel.MEDIUM,
        createdAt = Instant.parse("2026-03-20T10:00:00Z"),
        updatedAt = Instant.parse("2026-03-20T10:00:00Z"),
    )

    private fun lastHash(task: Task): String = detector.serializer.formatTask(task)

    @Test
    fun `flags task changed on both sides`() {
        val original = task("t1", title = "Original")
        val external = task("t1", title = "External edit")
        val app = task("t1", title = "App edit")

        val conflicts = detector.detect(
            importedTasks = listOf(external),
            appTasks = listOf(app),
            lastExportedHashes = mapOf("t1" to lastHash(original)),
        )

        assertEquals(setOf("t1"), conflicts)
    }

    @Test
    fun `does not flag when only vault changed`() {
        val original = task("t1", title = "Original")
        val external = task("t1", title = "External edit")

        val conflicts = detector.detect(
            importedTasks = listOf(external),
            appTasks = listOf(original),
            lastExportedHashes = mapOf("t1" to lastHash(original)),
        )

        assertEquals(emptySet<String>(), conflicts)
    }

    @Test
    fun `does not flag when only app changed`() {
        val original = task("t1", title = "Original")
        val app = task("t1", title = "App edit")

        val conflicts = detector.detect(
            importedTasks = listOf(original),
            appTasks = listOf(app),
            lastExportedHashes = mapOf("t1" to lastHash(original)),
        )

        assertEquals(emptySet<String>(), conflicts)
    }

    @Test
    fun `does not flag tasks with no prior export state`() {
        val external = task("t1", title = "External edit")
        val app = task("t1", title = "App edit")

        val conflicts = detector.detect(
            importedTasks = listOf(external),
            appTasks = listOf(app),
            lastExportedHashes = emptyMap(),
        )

        assertEquals(emptySet<String>(), conflicts)
    }

    @Test
    fun `does not flag when either side deleted the task`() {
        val original = task("t1", title = "Original")
        val app = task("t1", title = "App edit")

        // deleted externally
        assertEquals(
            emptySet<String>(),
            detector.detect(emptyList(), listOf(app), mapOf("t1" to lastHash(original))),
        )
        // deleted in app
        assertEquals(
            emptySet<String>(),
            detector.detect(listOf(original), emptyList(), mapOf("t1" to lastHash(original))),
        )
    }

    @Test
    fun `flags only the conflicting subset`() {
        val t1 = task("t1", title = "Original 1")
        val t2 = task("t2", title = "Original 2")
        val t3 = task("t3", title = "Original 3")

        val conflicts = detector.detect(
            importedTasks = listOf(task("t1", title = "Ext 1"), task("t2", title = "Ext 2")),
            appTasks = listOf(task("t1", title = "App 1"), task("t3", title = "App 3")),
            lastExportedHashes = mapOf(
                "t1" to lastHash(t1),
                "t2" to lastHash(t2),
                "t3" to lastHash(t3),
            ),
        )

        assertEquals(setOf("t1"), conflicts)
    }
}
