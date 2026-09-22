package app.tsosu.ui.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TaskTextTest {

    @Test
    fun `excerpt takes the first real line and drops markdown noise`() {
        val description = "\n# 背景\n\n- [文獻](https://example.com/paper) 的重點\n第二行不該出現"

        assertEquals("文獻 的重點", descriptionExcerpt(description))
    }

    @Test
    fun `excerpt is empty for a blank description and capped when long`() {
        assertEquals("", descriptionExcerpt("   \n\n  "))

        val long = "字".repeat(300)
        val excerpt = descriptionExcerpt(long, maxChars = 10)

        assertEquals("字".repeat(10) + "…", excerpt)
    }

    @Test
    fun `links keep markdown labels and find bare urls once`() {
        val description = """
            See [the paper](https://example.com/paper) and https://example.com/notes.
            Duplicate: https://example.com/paper
        """.trimIndent()

        assertEquals(
            listOf(
                TaskLink("the paper", "https://example.com/paper"),
                TaskLink("https://example.com/notes", "https://example.com/notes"),
            ),
            extractLinks(description),
        )
    }

    @Test
    fun `links trim sentence punctuation and ignore non-urls`() {
        val description = "Read https://example.com/a, then (https://example.com/b). mail me"

        assertEquals(
            listOf(
                TaskLink("https://example.com/a", "https://example.com/a"),
                TaskLink("https://example.com/b", "https://example.com/b"),
            ),
            extractLinks(description),
        )
    }

    @Test
    fun `links keep the order they appear in`() {
        val description = "start https://example.com/1 then [two](https://example.com/2) last"

        assertEquals(
            listOf(
                TaskLink("https://example.com/1", "https://example.com/1"),
                TaskLink("two", "https://example.com/2"),
            ),
            extractLinks(description),
        )
    }

    @Test
    fun `no links for plain text`() {
        assertEquals(emptyList<TaskLink>(), extractLinks("just notes, no links"))
    }
}
