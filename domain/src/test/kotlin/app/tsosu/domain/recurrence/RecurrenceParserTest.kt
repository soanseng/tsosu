package app.tsosu.domain.recurrence

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RecurrenceParserTest {

    private val parser = RecurrenceParser()

    // ── English patterns ──

    @Test
    fun `every day`() {
        val result = parser.parse("every day")
        assertSuccess("RRULE:FREQ=DAILY", result)
    }

    @Test
    fun `every weekday`() {
        val result = parser.parse("every weekday")
        assertSuccess("RRULE:FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", result)
    }

    @Test
    fun `every Monday`() {
        val result = parser.parse("every Monday")
        assertSuccess("RRULE:FREQ=WEEKLY;BYDAY=MO", result)
    }

    @Test
    fun `every monday lowercase`() {
        val result = parser.parse("every monday")
        assertSuccess("RRULE:FREQ=WEEKLY;BYDAY=MO", result)
    }

    @Test
    fun `every Mon Wed Fri`() {
        val result = parser.parse("every Mon, Wed, Fri")
        assertSuccess("RRULE:FREQ=WEEKLY;BYDAY=MO,WE,FR", result)
    }

    @Test
    fun `every Tuesday and Thursday`() {
        val result = parser.parse("every Tuesday and Thursday")
        assertSuccess("RRULE:FREQ=WEEKLY;BYDAY=TU,TH", result)
    }

    @Test
    fun `every 2 weeks`() {
        val result = parser.parse("every 2 weeks")
        assertSuccess("RRULE:FREQ=WEEKLY;INTERVAL=2", result)
    }

    @Test
    fun `every 3 days`() {
        val result = parser.parse("every 3 days")
        assertSuccess("RRULE:FREQ=DAILY;INTERVAL=3", result)
    }

    @Test
    fun `every week`() {
        val result = parser.parse("every week")
        assertSuccess("RRULE:FREQ=WEEKLY", result)
    }

    @Test
    fun `every month`() {
        val result = parser.parse("every month")
        assertSuccess("RRULE:FREQ=MONTHLY", result)
    }

    @Test
    fun `every 2 months`() {
        val result = parser.parse("every 2 months")
        assertSuccess("RRULE:FREQ=MONTHLY;INTERVAL=2", result)
    }

    @Test
    fun `every year`() {
        val result = parser.parse("every year")
        assertSuccess("RRULE:FREQ=YEARLY", result)
    }

    @Test
    fun `every month on the 15th`() {
        val result = parser.parse("every month on the 15th")
        assertSuccess("RRULE:FREQ=MONTHLY;BYMONTHDAY=15", result)
    }

    @Test
    fun `every month on the 1st`() {
        val result = parser.parse("every month on the 1st")
        assertSuccess("RRULE:FREQ=MONTHLY;BYMONTHDAY=1", result)
    }

    @Test
    fun `every month on the 22nd`() {
        val result = parser.parse("every month on the 22nd")
        assertSuccess("RRULE:FREQ=MONTHLY;BYMONTHDAY=22", result)
    }

    @Test
    fun `every month on the 3rd`() {
        val result = parser.parse("every month on the 3rd")
        assertSuccess("RRULE:FREQ=MONTHLY;BYMONTHDAY=3", result)
    }

    // ── Chinese patterns ──

    @Test
    fun `每天`() {
        val result = parser.parse("每天")
        assertSuccess("RRULE:FREQ=DAILY", result)
    }

    @Test
    fun `每週一`() {
        val result = parser.parse("每週一")
        assertSuccess("RRULE:FREQ=WEEKLY;BYDAY=MO", result)
    }

    @Test
    fun `每周一 simplified`() {
        val result = parser.parse("每周一")
        assertSuccess("RRULE:FREQ=WEEKLY;BYDAY=MO", result)
    }

    @Test
    fun `每週一三五`() {
        val result = parser.parse("每週一三五")
        assertSuccess("RRULE:FREQ=WEEKLY;BYDAY=MO,WE,FR", result)
    }

    @Test
    fun `每週二四`() {
        val result = parser.parse("每週二四")
        assertSuccess("RRULE:FREQ=WEEKLY;BYDAY=TU,TH", result)
    }

    @Test
    fun `每週日`() {
        val result = parser.parse("每週日")
        assertSuccess("RRULE:FREQ=WEEKLY;BYDAY=SU", result)
    }

    @Test
    fun `每兩週`() {
        val result = parser.parse("每兩週")
        assertSuccess("RRULE:FREQ=WEEKLY;INTERVAL=2", result)
    }

    @Test
    fun `每三天`() {
        val result = parser.parse("每三天")
        assertSuccess("RRULE:FREQ=DAILY;INTERVAL=3", result)
    }

    @Test
    fun `每週`() {
        val result = parser.parse("每週")
        assertSuccess("RRULE:FREQ=WEEKLY", result)
    }

    @Test
    fun `每月`() {
        val result = parser.parse("每月")
        assertSuccess("RRULE:FREQ=MONTHLY", result)
    }

    @Test
    fun `每年`() {
        val result = parser.parse("每年")
        assertSuccess("RRULE:FREQ=YEARLY", result)
    }

    @Test
    fun `每月15號`() {
        val result = parser.parse("每月15號")
        assertSuccess("RRULE:FREQ=MONTHLY;BYMONTHDAY=15", result)
    }

    @Test
    fun `每月1號`() {
        val result = parser.parse("每月1號")
        assertSuccess("RRULE:FREQ=MONTHLY;BYMONTHDAY=1", result)
    }

    @Test
    fun `每個工作日`() {
        val result = parser.parse("每個工作日")
        assertSuccess("RRULE:FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", result)
    }

    // ── Edge cases ──

    @Test
    fun `case insensitive English`() {
        val result = parser.parse("Every Day")
        assertSuccess("RRULE:FREQ=DAILY", result)
    }

    @Test
    fun `extra whitespace`() {
        val result = parser.parse("  every   day  ")
        assertSuccess("RRULE:FREQ=DAILY", result)
    }

    @Test
    fun `unrecognized pattern returns Unrecognized`() {
        val result = parser.parse("every other Tuesday")
        assertTrue(result is RecurrenceResult.Unrecognized)
        assertEquals("every other Tuesday", (result as RecurrenceResult.Unrecognized).original)
    }

    @Test
    fun `empty string returns Unrecognized`() {
        val result = parser.parse("")
        assertTrue(result is RecurrenceResult.Unrecognized)
    }

    @Test
    fun `random text returns Unrecognized`() {
        val result = parser.parse("buy groceries")
        assertTrue(result is RecurrenceResult.Unrecognized)
    }

    @Test
    fun `whitespace only returns Unrecognized`() {
        val result = parser.parse("   ")
        assertTrue(result is RecurrenceResult.Unrecognized)
    }

    @Test
    fun `invalid English day name returns Unrecognized`() {
        val result = parser.parse("every Someday")
        assertTrue(result is RecurrenceResult.Unrecognized)
    }

    @Test
    fun `每日 alternative Chinese daily`() {
        val result = parser.parse("每日")
        assertSuccess("RRULE:FREQ=DAILY", result)
    }

    @Test
    fun `每个工作日 simplified Chinese`() {
        val result = parser.parse("每个工作日")
        assertSuccess("RRULE:FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", result)
    }

    @Test
    fun `每月15号 simplified Chinese`() {
        val result = parser.parse("每月15号")
        assertSuccess("RRULE:FREQ=MONTHLY;BYMONTHDAY=15", result)
    }

    @Test
    fun `every month on the 0th is Unrecognized`() {
        val result = parser.parse("every month on the 0th")
        assertTrue(result is RecurrenceResult.Unrecognized)
    }

    @Test
    fun `every month on the 32nd is Unrecognized`() {
        val result = parser.parse("every month on the 32nd")
        assertTrue(result is RecurrenceResult.Unrecognized)
    }

    @Test
    fun `每月0號 is Unrecognized`() {
        val result = parser.parse("每月0號")
        assertTrue(result is RecurrenceResult.Unrecognized)
    }

    @Test
    fun `multi-char Chinese numeral returns Unrecognized`() {
        // 十一 is two chars, not in ZH_NUM_MAP, and not a digit string
        val result = parser.parse("每十一天")
        assertTrue(result is RecurrenceResult.Unrecognized)
    }

    // ── extractFromTitle ──

    @Test
    fun `extractFromTitle detects trailing English recurrence`() {
        val result = parser.extractFromTitle("買菜 every Monday")
        assertEquals("買菜", result.title)
        assertEquals("RRULE:FREQ=WEEKLY;BYDAY=MO", result.rrule)
    }

    @Test
    fun `extractFromTitle detects trailing Chinese recurrence`() {
        val result = parser.extractFromTitle("買菜 每週一")
        assertEquals("買菜", result.title)
        assertEquals("RRULE:FREQ=WEEKLY;BYDAY=MO", result.rrule)
    }

    @Test
    fun `extractFromTitle no recurrence returns null rrule`() {
        val result = parser.extractFromTitle("買菜")
        assertEquals("買菜", result.title)
        assertEquals(null, result.rrule)
    }

    @Test
    fun `extractFromTitle only recurrence returns empty title`() {
        val result = parser.extractFromTitle("every day")
        assertEquals("", result.title)
        assertEquals("RRULE:FREQ=DAILY", result.rrule)
    }

    @Test
    fun `extractFromTitle with Chinese recurrence at end`() {
        val result = parser.extractFromTitle("運動 每天")
        assertEquals("運動", result.title)
        assertEquals("RRULE:FREQ=DAILY", result.rrule)
    }

    @Test
    fun `extractFromTitle bare Chinese recurrence`() {
        val result = parser.extractFromTitle("每天")
        assertEquals("", result.title)
        assertEquals("RRULE:FREQ=DAILY", result.rrule)
    }

    @Test
    fun `extractFromTitle unrecognized English suffix preserves full title`() {
        val result = parser.extractFromTitle("Send report every other Tuesday")
        assertEquals("Send report every other Tuesday", result.title)
        assertEquals(null, result.rrule)
    }

    @Test
    fun `extractFromTitle unrecognized Chinese suffix preserves full title`() {
        val result = parser.extractFromTitle("做事 每星期")
        assertEquals("做事 每星期", result.title)
        assertEquals(null, result.rrule)
    }

    // ── toDisplayLabel ──

    @Test
    fun `toDisplayLabel for daily`() {
        assertEquals("Every day", RecurrenceParser.toDisplayLabel("RRULE:FREQ=DAILY"))
    }

    @Test
    fun `toDisplayLabel for weekly Monday`() {
        assertEquals("Every Mon", RecurrenceParser.toDisplayLabel("RRULE:FREQ=WEEKLY;BYDAY=MO"))
    }

    @Test
    fun `toDisplayLabel for weekly multiple days`() {
        assertEquals("Every Mon, Wed, Fri", RecurrenceParser.toDisplayLabel("RRULE:FREQ=WEEKLY;BYDAY=MO,WE,FR"))
    }

    @Test
    fun `toDisplayLabel for weekly`() {
        assertEquals("Every week", RecurrenceParser.toDisplayLabel("RRULE:FREQ=WEEKLY"))
    }

    @Test
    fun `toDisplayLabel for biweekly`() {
        assertEquals("Every 2 weeks", RecurrenceParser.toDisplayLabel("RRULE:FREQ=WEEKLY;INTERVAL=2"))
    }

    @Test
    fun `toDisplayLabel for monthly`() {
        assertEquals("Every month", RecurrenceParser.toDisplayLabel("RRULE:FREQ=MONTHLY"))
    }

    @Test
    fun `toDisplayLabel for monthly on day`() {
        assertEquals("Every month on day 15", RecurrenceParser.toDisplayLabel("RRULE:FREQ=MONTHLY;BYMONTHDAY=15"))
    }

    @Test
    fun `toDisplayLabel for yearly`() {
        assertEquals("Every year", RecurrenceParser.toDisplayLabel("RRULE:FREQ=YEARLY"))
    }

    @Test
    fun `toDisplayLabel for weekdays`() {
        assertEquals("Every weekday", RecurrenceParser.toDisplayLabel("RRULE:FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR"))
    }

    @Test
    fun `toDisplayLabel unknown returns raw`() {
        assertEquals("RRULE:FREQ=SECONDLY", RecurrenceParser.toDisplayLabel("RRULE:FREQ=SECONDLY"))
    }

    @Test
    fun `toDisplayLabel with INTERVAL=1 omits interval`() {
        assertEquals("Every day", RecurrenceParser.toDisplayLabel("RRULE:FREQ=DAILY;INTERVAL=1"))
    }

    @Test
    fun `toDisplayLabel with interval and byday shows interval only`() {
        // When both INTERVAL and BYDAY are present, interval takes precedence
        assertEquals("Every 2 weeks", RecurrenceParser.toDisplayLabel("RRULE:FREQ=WEEKLY;INTERVAL=2;BYDAY=MO"))
    }

    @Test
    fun `toDisplayLabel with malformed segment ignores it`() {
        assertEquals("Every day", RecurrenceParser.toDisplayLabel("RRULE:FREQ=DAILY;BROKEN"))
    }

    @Test
    fun `toDisplayLabel without RRULE prefix still works`() {
        assertEquals("Every day", RecurrenceParser.toDisplayLabel("FREQ=DAILY"))
    }

    // ── Todoist-style shorthand ──

    @Test
    fun `ev day shorthand`() {
        val result = parser.parse("ev day")
        assertSuccess("RRULE:FREQ=DAILY", result)
    }

    @Test
    fun `ev mon shorthand`() {
        val result = parser.parse("ev mon")
        assertSuccess("RRULE:FREQ=WEEKLY;BYDAY=MO", result)
    }

    @Test
    fun `ev bang shorthand`() {
        val result = parser.parse("ev! daily")
        assertSuccess("RRULE:FREQ=DAILY", result)
    }

    @Test
    fun `every other day`() {
        val result = parser.parse("every other day")
        assertSuccess("RRULE:FREQ=DAILY;INTERVAL=2", result)
    }

    @Test
    fun `every other week`() {
        val result = parser.parse("every other week")
        assertSuccess("RRULE:FREQ=WEEKLY;INTERVAL=2", result)
    }

    @Test
    fun `extract ev shorthand from title`() {
        val result = parser.extractFromTitle("Water plants ev mon")
        assertEquals("Water plants", result.title)
        assertEquals("RRULE:FREQ=WEEKLY;BYDAY=MO", result.rrule)
    }

    @Test
    fun `title containing ev-adjacent word does not false match`() {
        // "level 3 checks" contains " ev " — candidate "level…" wait, we match LAST occurrence:
        // last " ev " here is inside "level 3 checks"? No — regex requires space before ev.
        // "level" has no space-ev boundary, so no match expected.
        val result = parser.extractFromTitle("Check level 3 gauge")
        assertEquals("Check level 3 gauge", result.title)
        assertEquals(null, result.rrule)
    }

    @Test
    fun `time of day keyword sets daily and reminder preset`() {
        val extraction = parser.extractFromTitle("Water plants every morning")
        assertEquals("Water plants", extraction.title)
        assertEquals("RRULE:FREQ=DAILY", extraction.rrule)
        assertEquals(kotlinx.datetime.LocalTime(8, 0), extraction.suggestedReminder)
    }

    @Test
    fun `time of day keyword evening with until`() {
        val year = java.time.Year.now().value
        val extraction = parser.extractFromTitle("Stretch every evening until 9/15")
        assertEquals("Stretch", extraction.title)
        assertEquals("RRULE:FREQ=DAILY;UNTIL=${year}0915T235959Z", extraction.rrule)
        assertEquals(kotlinx.datetime.LocalTime(18, 0), extraction.suggestedReminder)
    }

    @Test
    fun `chinese time of day keyword`() {
        val extraction = parser.extractFromTitle("澆花 每天早上")
        assertEquals("澆花", extraction.title)
        assertEquals("RRULE:FREQ=DAILY", extraction.rrule)
        assertEquals(kotlinx.datetime.LocalTime(8, 0), extraction.suggestedReminder)
    }

    @Test
    fun `until date becomes UNTIL in rrule`() {
        val year = java.time.Year.now().value
        assertSuccess("RRULE:FREQ=DAILY;UNTIL=${year}0831T235959Z", parser.parse("every day until 8/31"))
    }

    @Test
    fun `until with iso date`() {
        assertSuccess("RRULE:FREQ=WEEKLY;BYDAY=MO;UNTIL=20270831T235959Z", parser.parse("every mon until 2027-08-31"))
    }

    @Test
    fun `starting date surfaced separately`() {
        val year = java.time.Year.now().value
        val extraction = parser.extractFromTitle("Meditate every day starting 8/20 until 9/30")
        assertEquals("Meditate", extraction.title)
        assertEquals("RRULE:FREQ=DAILY;UNTIL=${year}0930T235959Z", extraction.rrule)
        assertEquals(kotlinx.datetime.LocalDate(year, 8, 20), extraction.startDate)
    }

    @Test
    fun `modifiers work with chinese`() {
        val year = java.time.Year.now().value
        assertSuccess("RRULE:FREQ=DAILY;UNTIL=${year}0831T235959Z", parser.parse("每天 到 8月31"))
    }

    @Test
    fun `no modifiers unchanged behavior`() {
        assertSuccess("RRULE:FREQ=DAILY", parser.parse("every day"))
    }

    @Test
    fun `display label appends until`() {
        assertEquals("Every day until Aug 31", RecurrenceParser.toDisplayLabel("RRULE:FREQ=DAILY;UNTIL=20260831T235959Z"))
    }

    private fun assertSuccess(expectedRrule: String, result: RecurrenceResult) {
        assertTrue(result is RecurrenceResult.Success, "Expected Success but got $result")
        assertEquals(expectedRrule, (result as RecurrenceResult.Success).rrule)
    }
}
