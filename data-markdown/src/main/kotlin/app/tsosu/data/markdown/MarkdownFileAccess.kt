package app.tsosu.data.markdown

interface MarkdownFileAccess {
    suspend fun readTasksFile(): String?
    suspend fun writeTasksFile(content: String)
    suspend fun readHabitsFile(): String?
    suspend fun writeHabitsFile(content: String)
}
