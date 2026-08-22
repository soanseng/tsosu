package app.tsosu.domain.recurrence

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

data class QuickAddGrammarResult(
    /** Title with every recognized token stripped and whitespace collapsed. */
    val title: String,
    /** `@project` token, if any (last one wins). */
    val projectName: String?,
    /** `due:<phrase>` token, if any. */
    val dueDate: LocalDate?,
)

/**
 * Quick-add token grammar on top of the existing extractors
 * (priority `p1..p4`, recurrence phrases):
 *
 * - `@Project`  — files the task into a project (matched by name on add)
 * - `due:today` / `due:tomorrow` / `due:next week` / `due:8/31` /
 *   `due:2026-08-31` / `due:8月31` — sets the first due date
 *
 * ZH aliases: 今天 / 明天 / 後天(后天) / 下週(下周).
 * `#tag` tokens are deliberately left in the title: they are searchable
 * and full label plumbing is a separate feature.
 */
object QuickAddGrammar {

    private val PROJECT = Regex("""(?<![a-zA-Z0-9_])@([\p{L}0-9_-]+)""")
    private val DUE = Regex("""(?<![a-zA-Z0-9_])due:\s*([^\s#@]+)""")
    private val DUE_ZH = Regex("""(?<![a-zA-Z0-9_])(?:due|到期)[:：]\s*([^\s#@]+)""")

    fun extract(
        fullTitle: String,
        today: LocalDate,
        parseFlexibleDate: (String) -> LocalDate? = { null },
    ): QuickAddGrammarResult {
        val dueMatch = (DUE_ZH.find(fullTitle) ?: DUE.find(fullTitle))
        val dueDate = dueMatch?.groupValues?.get(1)?.let { phrase ->
            parseRelativeDate(phrase, today) ?: parseFlexibleDate(phrase)
        }
        val withoutDue = dueMatch?.let { fullTitle.replaceRange(it.range, " ") } ?: fullTitle

        val projectMatch = PROJECT.findAll(withoutDue).lastOrNull()
        val projectName = projectMatch?.groupValues?.get(1)

        val cleaned = withoutDue
            .let { if (projectMatch != null) PROJECT.replace(it, " ") else it }
            .replace(Regex("\\s+"), " ")
            .trim()

        return QuickAddGrammarResult(
            title = cleaned,
            projectName = projectName,
            dueDate = dueDate,
        )
    }

    private fun parseRelativeDate(phrase: String, today: LocalDate): LocalDate? = when (phrase.lowercase()) {
        "today" -> today
        "tomorrow" -> today.plus(1, DateTimeUnit.DAY)
        "next week", "nextweek" -> today.plus(7, DateTimeUnit.DAY)
        "今天" -> today
        "明天" -> today.plus(1, DateTimeUnit.DAY)
        "後天", "后天" -> today.plus(2, DateTimeUnit.DAY)
        "下週", "下周", "下星期" -> today.plus(7, DateTimeUnit.DAY)
        else -> null
    }
}
