package app.tsosu.data.vikunja.importer

import app.tsosu.domain.repository.ImportFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TodoistImporterTest {

    private val importer = TodoistImporter()

    @Test
    fun `parses CSV with basic task fields`() {
        val csv = """
            TYPE,CONTENT,DESCRIPTION,PRIORITY,INDENT,AUTHOR,RESPONSIBLE,DATE,DATE_LANG,TIMEZONE
            task,Buy milk,,4,1,,,2026-03-10,,
            task,Write report,Draft for Monday,2,1,,,2026-03-12,,
        """.trimIndent()

        val result = importer.parse(csv.toByteArray(), ImportFormat.TODOIST_CSV)
        assertEquals(2, result.tasks.size)
        assertEquals("Buy milk", result.tasks[0].title)
        assertEquals("Write report", result.tasks[1].title)
        assertEquals("Draft for Monday", result.tasks[1].description)
    }

    @Test
    fun `maps Todoist priority to Tsosu priority`() {
        val csv = """
            TYPE,CONTENT,DESCRIPTION,PRIORITY,INDENT,AUTHOR,RESPONSIBLE,DATE,DATE_LANG,TIMEZONE
            task,Urgent task,,1,1,,,,,
            task,Normal task,,4,1,,,,,
        """.trimIndent()

        val result = importer.parse(csv.toByteArray(), ImportFormat.TODOIST_CSV)
        assertEquals(4, result.tasks[0].priority) // p1 -> URGENT
        assertEquals(0, result.tasks[1].priority) // p4 -> NONE
    }

    @Test
    fun `parses due dates`() {
        val csv = """
            TYPE,CONTENT,DESCRIPTION,PRIORITY,INDENT,AUTHOR,RESPONSIBLE,DATE,DATE_LANG,TIMEZONE
            task,Task with date,,4,1,,,2026-03-15,,
            task,Task without date,,4,1,,,,,
        """.trimIndent()

        val result = importer.parse(csv.toByteArray(), ImportFormat.TODOIST_CSV)
        assertEquals("2026-03-15", result.tasks[0].dueDate)
        assertNull(result.tasks[1].dueDate)
    }

    @Test
    fun `skips non-task rows`() {
        val csv = """
            TYPE,CONTENT,DESCRIPTION,PRIORITY,INDENT,AUTHOR,RESPONSIBLE,DATE,DATE_LANG,TIMEZONE
            project,My Project,,4,1,,,,,
            task,Real task,,4,1,,,,,
        """.trimIndent()

        val result = importer.parse(csv.toByteArray(), ImportFormat.TODOIST_CSV)
        assertEquals(1, result.tasks.size)
        assertEquals("Real task", result.tasks[0].title)
    }

    @Test
    fun `handles empty CSV`() {
        val result = importer.parse("".toByteArray(), ImportFormat.TODOIST_CSV)
        assertEquals(0, result.tasks.size)
    }

    @Test
    fun `returns empty for JSON format (not yet supported)`() {
        val result = importer.parse("{}".toByteArray(), ImportFormat.TODOIST_JSON)
        assertEquals(0, result.tasks.size)
    }
}
