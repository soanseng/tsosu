package app.tsosu.ui.util

/** A link found in a task description: what to show, and where it goes. */
data class TaskLink(val label: String, val url: String)

private val MARKDOWN_LINK = Regex("""\[([^\]\n]*)\]\((<?[^)\s>]+>?)\)""")
private val BARE_URL = Regex("""https?://[^\s<>()\[\]]+""")
private val HEADING = Regex("""^#{1,6}\s+\S.*$""")
private val LEADING_MARKERS = Regex("""^\s*(#{1,6}\s+|[-*+>]\s+|\d+[.)]\s+)""")
private val TRAILING_PUNCTUATION = charArrayOf('.', ',', ';', ':', '!', '?', ')', ']', '"', '\'')

/**
 * One-line preview of a description for the task row: first meaningful line
 * (headings are skipped while anything else exists), markdown markers
 * dropped, links shown as their label, collapsed and capped. Empty when
 * there is nothing worth showing.
 */
fun descriptionExcerpt(text: String, maxChars: Int = 140): String {
    val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
    val line = lines.firstOrNull { !HEADING.matches(it) } ?: lines.firstOrNull() ?: return ""
    val flattened = MARKDOWN_LINK.replace(line) { it.groupValues[1].ifBlank { it.groupValues[2] } }
    val cleaned = LEADING_MARKERS.replace(flattened, "")
        .replace(Regex("""[*_`~]"""), "")
        .replace(Regex("""\s+"""), " ")
        .trim()
    if (cleaned.length <= maxChars) return cleaned
    return cleaned.take(maxChars).trimEnd() + "…"
}

/**
 * Every http(s) link in a description, in the order they appear,
 * de-duplicated by url. Markdown links keep their label; bare urls use the
 * url as the label.
 */
fun extractLinks(text: String): List<TaskLink> {
    if (text.isBlank()) return emptyList()
    val markdown = MARKDOWN_LINK.findAll(text).toList()
    val hits = mutableListOf<Triple<Int, String, String>>()
    markdown.forEach { match ->
        hits += Triple(match.range.first, match.groupValues[1], match.groupValues[2])
    }
    BARE_URL.findAll(text).forEach { match ->
        // A url inside [label](url) is already captured with its label.
        if (markdown.any { match.range.first in it.range }) return@forEach
        hits += Triple(match.range.first, "", match.value)
    }

    val seen = mutableSetOf<String>()
    return hits.sortedBy { it.first }.mapNotNull { (_, label, rawUrl) ->
        val url = rawUrl.trim().removeSurrounding("<", ">").trimEnd(*TRAILING_PUNCTUATION)
        if (url.isEmpty() || !seen.add(url)) return@mapNotNull null
        TaskLink(label.trim().ifBlank { url }, url)
    }
}
