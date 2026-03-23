package app.tsosu.data.markdown

interface MarkdownFileAccess {
    suspend fun readTasksFile(): String?
    suspend fun writeTasksFile(content: String)
    suspend fun readHabitsFile(): String?
    suspend fun writeHabitsFile(content: String)

    // Folder operations for multi-file sync
    suspend fun listFolder(folderName: String): List<String>
    suspend fun readFileInFolder(folderName: String, filename: String): String?
    suspend fun writeFileInFolder(folderName: String, filename: String, content: String)
    suspend fun ensureFolder(folderName: String)
}
