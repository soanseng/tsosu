package app.tsosu.data.markdown

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class YamlFrontmatterParserTest {

    private val parser = YamlFrontmatterParser()

    @Test
    fun `parse simple key-value pairs`() {
        val yaml = """
            ---
            id: abc-123
            status: todo
            priority: high
            due: 2026-03-25
            ---

            # Title
            Body content here.
        """.trimIndent()

        val result = parser.parse(yaml)
        assertEquals("abc-123", result.frontmatter["id"])
        assertEquals("todo", result.frontmatter["status"])
        assertEquals("high", result.frontmatter["priority"])
        assertEquals("2026-03-25", result.frontmatter["due"])
        assertEquals("# Title\nBody content here.", result.body.trim())
    }

    @Test
    fun `parse list values`() {
        val yaml = """
            ---
            tags: [errands, shopping]
            ---
        """.trimIndent()

        val result = parser.parse(yaml)
        assertEquals("[errands, shopping]", result.frontmatter["tags"])
    }

    @Test
    fun `no frontmatter returns empty map and full body`() {
        val content = "# Just a title\nSome body."
        val result = parser.parse(content)
        assertEquals(emptyMap<String, String>(), result.frontmatter)
        assertEquals(content, result.body.trim())
    }

    @Test
    fun `empty frontmatter`() {
        val content = "---\n---\nBody"
        val result = parser.parse(content)
        assertEquals(emptyMap<String, String>(), result.frontmatter)
        assertEquals("Body", result.body.trim())
    }

    @Test
    fun `quoted string values preserve quotes`() {
        val yaml = """
            ---
            reminder: "14:30"
            tiny: "Take 3 deep breaths"
            ---
        """.trimIndent()

        val result = parser.parse(yaml)
        assertEquals("14:30", result.frontmatter["reminder"])
        assertEquals("Take 3 deep breaths", result.frontmatter["tiny"])
    }
}
