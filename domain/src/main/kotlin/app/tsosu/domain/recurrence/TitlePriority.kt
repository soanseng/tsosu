package app.tsosu.domain.recurrence

import app.tsosu.domain.model.Priority

data class TitlePriorityResult(
    val title: String,
    val priority: Priority?,
)

/**
 * Todoist-style priority shorthand in a task title: "p1".."p4".
 * p1 = URGENT (highest) … p4 = LOW, matching Todoist semantics.
 * Matched case-sensitively as a standalone word so "ap1" or "p5" never hit;
 * the last match wins when several appear.
 */
object TitlePriority {
    private val P_TOKEN = Regex("""(?<![a-zA-Z0-9])[pP]([1-4])(?![a-zA-Z0-9])""")

    fun extract(fullTitle: String): TitlePriorityResult {
        val matches = P_TOKEN.findAll(fullTitle).toList()
        if (matches.isEmpty()) return TitlePriorityResult(fullTitle, null)

        val last = matches.last()
        val priority = when (last.groupValues[1]) {
            "1" -> Priority.URGENT
            "2" -> Priority.HIGH
            "3" -> Priority.MEDIUM
            else -> Priority.LOW
        }
        // Remove every pN token, collapse leftover spaces.
        val cleaned = P_TOKEN.replace(fullTitle, " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return TitlePriorityResult(cleaned, priority)
    }
}
