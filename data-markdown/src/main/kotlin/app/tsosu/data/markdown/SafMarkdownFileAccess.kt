package app.tsosu.data.markdown

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

class SafMarkdownFileAccess(
    private val context: Context,
    private val folderUriProvider: suspend () -> Uri?,
) : MarkdownFileAccess {

    override suspend fun readTasksFile(): String? = readFile(TASKS_FILENAME)
    override suspend fun writeTasksFile(content: String) = writeFile(TASKS_FILENAME, content)
    override suspend fun readHabitsFile(): String? = readFile(HABITS_FILENAME)
    override suspend fun writeHabitsFile(content: String) = writeFile(HABITS_FILENAME, content)

    private suspend fun readFile(filename: String): String? {
        val folderUri = folderUriProvider() ?: return null
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return null
        val file = folder.findFile(filename) ?: return null
        return context.contentResolver.openInputStream(file.uri)?.use { stream ->
            stream.bufferedReader().readText()
        }
    }

    private suspend fun writeFile(filename: String, content: String) {
        val folderUri = folderUriProvider() ?: return
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return
        val file = folder.findFile(filename)
            ?: folder.createFile("text/markdown", filename)
            ?: return
        context.contentResolver.openOutputStream(file.uri, "wt")?.use { stream ->
            stream.bufferedWriter().use { it.write(content) }
        }
    }

    companion object {
        const val TASKS_FILENAME = "tasks.md"
        const val HABITS_FILENAME = "habits.md"
    }
}
