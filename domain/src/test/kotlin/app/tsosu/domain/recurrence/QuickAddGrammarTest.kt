package app.tsosu.domain.recurrence

import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QuickAddGrammarTest {

    private val today = LocalDate(2026, 8, 21)

    private fun extract(text: String) = QuickAddGrammar.extract(text, today)

    @Test
    fun `plain title passes through untouched`() {
        val r = extract("Buy oat milk #errands")
        assertEquals("Buy oat milk #errands", r.title)
        assertNull(r.projectName)
        assertNull(r.dueDate)
    }

    @Test
    fun `project token is stripped and captured`() {
        val r = extract("Pay rent @Home")
        assertEquals("Pay rent", r.title)
        assertEquals("Home", r.projectName)
    }

    @Test
    fun `project token supports CJK names and last wins`() {
        val r = extract("繳房租 @家裡 @財務")
        assertEquals("繳房租", r.title)
        assertEquals("財務", r.projectName)
    }

    @Test
    fun `due today and tomorrow in english`() {
        assertEquals(today, extract("Call mom due:today").dueDate)
        assertEquals(LocalDate(2026, 8, 22), extract("Call mom due:tomorrow").dueDate)
        assertEquals("Call mom", extract("Call mom due:tomorrow").title)
    }

    @Test
    fun `due relative in chinese`() {
        assertEquals(today, extract("回媽媽訊息 due:今天").dueDate)
        assertEquals(LocalDate(2026, 8, 23), extract("回媽媽訊息 due:後天").dueDate)
        assertEquals(LocalDate(2026, 8, 28), extract("週會 due:下週").dueDate)
    }

    @Test
    fun `due flexible date delegates to the date parser`() {
        val r = QuickAddGrammar.extract("Submit form due:8/31", today, parseFlexibleDate = {
            if (it == "8/31") LocalDate(2026, 8, 31) else null
        })
        assertEquals(LocalDate(2026, 8, 31), r.dueDate)
        assertEquals("Submit form", r.title)
    }

    @Test
    fun `combined project and due tokens`() {
        val r = extract("Submit tax form @Taxes due:tomorrow p1 every month")
        assertEquals("Submit tax form p1 every month", r.title)
        assertEquals("Taxes", r.projectName)
        assertEquals(LocalDate(2026, 8, 22), r.dueDate)
    }

    @Test
    fun `email addresses are not project tokens`() {
        val r = extract("Email bob@example.com now")
        assertEquals("Email bob@example.com now", r.title)
        assertNull(r.projectName)
    }
}
