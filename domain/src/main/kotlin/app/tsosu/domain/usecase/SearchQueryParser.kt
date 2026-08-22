package app.tsosu.domain.usecase

import app.tsosu.domain.model.TaskStatus

data class SearchQuery(
    /** Free-text terms ANDed against title + description (case-insensitive). */
    val textTerms: List<String>,
    val status: TaskStatus?,
    /** due:<=7d — due within N days from now. */
    val dueWithinDays: Int?,
    /** due:>=3d — due in N days or later. */
    val dueInDaysOrLater: Int?,
    /** due:today */
    val dueToday: Boolean,
    /** due:overdue — due in the past and not done. */
    val overdue: Boolean,
    /** #tag tokens (matched against the title until labels are persisted). */
    val tags: List<String>,
)

/**
 * Operator syntax for task search:
 * `status:todo|doing|hold|planned|someday|done|cancelled`,
 * `due:<=7d`, `due:>=3d`, `due:today`, `due:overdue`, `#tag`.
 * Everything else is a free-text term.
 */
object SearchQueryParser {

    private val STATUS = Regex("""(?i)\bstatus:(todo|doing|hold|planned|someday|done|cancelled)\b""")
    private val DUE_WITHIN = Regex("""(?i)\bdue:<=\s*(\d+)d\b""")
    private val DUE_LATER = Regex("""(?i)\bdue:>=\s*(\d+)d\b""")
    private val DUE_TODAY = Regex("""(?i)\bdue:today\b""")
    private val DUE_OVERDUE = Regex("""(?i)\bdue:overdue\b""")
    private val TAG = Regex("""(?<![a-zA-Z0-9_])#([\p{L}0-9_-]+)""")

    private val STATUS_MAP = mapOf(
        "todo" to TaskStatus.TODO,
        "doing" to TaskStatus.IN_PROGRESS,
        "hold" to TaskStatus.ON_HOLD,
        "planned" to TaskStatus.PLANNED,
        "someday" to TaskStatus.PLANNED,
        "done" to TaskStatus.DONE,
        "cancelled" to TaskStatus.CANCELLED,
    )

    fun parse(raw: String): SearchQuery {
        var rest = raw

        val status = STATUS.find(rest)?.groupValues?.get(1)?.lowercase()
            ?.let { STATUS_MAP[it] }
        rest = STATUS.replace(rest, " ")

        val dueWithin = DUE_WITHIN.find(rest)?.groupValues?.get(1)?.toIntOrNull()
        rest = DUE_WITHIN.replace(rest, " ")

        val dueLater = DUE_LATER.find(rest)?.groupValues?.get(1)?.toIntOrNull()
        rest = DUE_LATER.replace(rest, " ")

        val dueToday = DUE_TODAY.containsMatchIn(rest)
        rest = DUE_TODAY.replace(rest, " ")

        val overdue = DUE_OVERDUE.containsMatchIn(rest)
        rest = DUE_OVERDUE.replace(rest, " ")

        val tags = TAG.findAll(rest).map { it.groupValues[1] }.distinct().toList()
        rest = TAG.replace(rest, " ")

        val textTerms = rest.split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        return SearchQuery(
            textTerms = textTerms,
            status = status,
            dueWithinDays = dueWithin,
            dueInDaysOrLater = dueLater,
            dueToday = dueToday,
            overdue = overdue,
            tags = tags,
        )
    }
}
