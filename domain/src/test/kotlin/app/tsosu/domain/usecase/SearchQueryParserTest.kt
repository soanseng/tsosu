package app.tsosu.domain.usecase

import app.tsosu.domain.model.TaskStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SearchQueryParserTest {

    @Test
    fun `plain terms become and-ed text terms`() {
        val q = SearchQueryParser.parse("buy milk")
        assertEquals(listOf("buy", "milk"), q.textTerms)
        assertNull(q.status)
        assertFalse(q.dueToday)
    }

    @Test
    fun `status operator maps synonyms`() {
        assertEquals(TaskStatus.DONE, SearchQueryParser.parse("status:done").status)
        assertEquals(TaskStatus.IN_PROGRESS, SearchQueryParser.parse("status:doing report").status)
        assertEquals(TaskStatus.PLANNED, SearchQueryParser.parse("status:someday").status)
        assertEquals(listOf("report"), SearchQueryParser.parse("status:doing report").textTerms)
    }

    @Test
    fun `due window operators parse and are stripped`() {
        val q = SearchQueryParser.parse("rent due:<=7d")
        assertEquals(7, q.dueWithinDays)
        assertNull(q.dueInDaysOrLater)
        assertEquals(listOf("rent"), q.textTerms)

        val later = SearchQueryParser.parse("due:>=3d visa")
        assertEquals(3, later.dueInDaysOrLater)
    }

    @Test
    fun `due today and overdue flags`() {
        assertTrue(SearchQueryParser.parse("due:today").dueToday)
        assertTrue(SearchQueryParser.parse("due:overdue").overdue)
        assertFalse(SearchQueryParser.parse("due:overdue").dueToday)
    }

    @Test
    fun `tag tokens collect distinctly and stay out of text terms`() {
        val q = SearchQueryParser.parse("#home chores #home #errands")
        assertEquals(listOf("home", "errands"), q.tags)
        assertEquals(listOf("chores"), q.textTerms)
    }

    @Test
    fun `combined query keeps every operator`() {
        val q = SearchQueryParser.parse("status:todo #tax due:<=14d accountant")
        assertEquals(TaskStatus.TODO, q.status)
        assertEquals(listOf("tax"), q.tags)
        assertEquals(14, q.dueWithinDays)
        assertEquals(listOf("accountant"), q.textTerms)
    }
}
