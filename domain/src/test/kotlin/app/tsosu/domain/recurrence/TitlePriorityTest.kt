package app.tsosu.domain.recurrence

import app.tsosu.domain.model.Priority
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TitlePriorityTest {

    @Test
    fun `p1 maps to urgent and is stripped`() {
        val result = TitlePriority.extract("Submit report p1")
        assertEquals("Submit report", result.title)
        assertEquals(Priority.URGENT, result.priority)
    }

    @Test
    fun `uppercase P3 works`() {
        val result = TitlePriority.extract("buy milk P3")
        assertEquals("buy milk", result.title)
        assertEquals(Priority.MEDIUM, result.priority)
    }

    @Test
    fun `last token wins when several`() {
        val result = TitlePriority.extract("draft p4 then review p2")
        assertEquals("draft then review", result.title)
        assertEquals(Priority.HIGH, result.priority)
    }

    @Test
    fun `word-adjacent pN does not match`() {
        val result = TitlePriority.extract("check ap1 pressure")
        assertEquals("check ap1 pressure", result.title)
        assertEquals(null, result.priority)
    }

    @Test
    fun `p5 does not match`() {
        val result = TitlePriority.extract("tune p5 knob")
        assertEquals("tune p5 knob", result.title)
        assertEquals(null, result.priority)
    }

    @Test
    fun `no token leaves title untouched`() {
        val result = TitlePriority.extract("plain task")
        assertEquals("plain task", result.title)
        assertEquals(null, result.priority)
    }
}
