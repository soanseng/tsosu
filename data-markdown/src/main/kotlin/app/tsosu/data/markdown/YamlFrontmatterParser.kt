package app.tsosu.data.markdown

data class ParsedDocument(
    val frontmatter: Map<String, String>,
    val body: String,
)

class YamlFrontmatterParser {

    fun parse(content: String): ParsedDocument {
        val lines = content.lines()
        if (lines.isEmpty() || lines[0].trim() != "---") {
            return ParsedDocument(emptyMap(), content)
        }

        val closingIdx = lines.drop(1).indexOfFirst { it.trim() == "---" }
        if (closingIdx == -1) {
            return ParsedDocument(emptyMap(), content)
        }

        val yamlLines = lines.subList(1, closingIdx + 1)
        val body = lines.drop(closingIdx + 2).joinToString("\n")
        val frontmatter = mutableMapOf<String, String>()

        for (line in yamlLines) {
            val colonIdx = line.indexOf(':')
            if (colonIdx > 0) {
                val key = line.substring(0, colonIdx).trim()
                val rawValue = line.substring(colonIdx + 1).trim()
                val value = rawValue.removeSurrounding("\"")
                frontmatter[key] = value
            }
        }

        return ParsedDocument(frontmatter, body)
    }

    fun serialize(frontmatter: Map<String, String>, body: String): String = buildString {
        appendLine("---")
        for ((key, value) in frontmatter) {
            if (value.startsWith("[") || value.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) || value == "true" || value == "false") {
                appendLine("$key: $value")
            } else if (value.contains(":") || value.contains(" ")) {
                appendLine("$key: \"$value\"")
            } else {
                appendLine("$key: $value")
            }
        }
        appendLine("---")
        appendLine()
        append(body)
    }
}
