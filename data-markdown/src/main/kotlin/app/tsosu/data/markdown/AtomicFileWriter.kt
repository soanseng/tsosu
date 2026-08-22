package app.tsosu.data.markdown

/**
 * Testable seam over a folder of writable files (SAF DocumentFile in
 * production, an in-memory fake in tests). Implementations must make
 * `writeText` replace the file's whole content.
 */
interface WritableFolder {
    fun findFile(name: String): WritableFile?
    fun createFile(name: String): WritableFile?
}

interface WritableFile {
    /** Replaces the whole content; false when the stream could not be opened. */
    fun writeText(content: String): Boolean
    fun delete(): Boolean
    fun renameTo(name: String): Boolean
}

/**
 * Crash-safe write: the full content lands in a temp sibling first, then the
 * target is replaced by rename. A process death mid-write can only orphan
 * `<filename>.tmp` (overwritten next sync), never truncate the real note.
 * If the provider refuses the rename, falls back to a direct write so the
 * data still lands.
 */
class AtomicFileWriter(private val folder: WritableFolder) {

    fun write(filename: String, content: String) {
        val tempName = "$filename.tmp"
        val temp = folder.findFile(tempName)
            ?: folder.createFile(tempName)
            ?: return

        if (!temp.writeText(content)) {
            temp.delete()
            return
        }

        folder.findFile(filename)?.delete()
        if (temp.renameTo(filename)) return

        // Rename refused (provider quirk): recreate the target (the old
        // handle is dead after delete) and write through directly, then
        // clean up the temp file.
        val direct = folder.createFile(filename)
        direct?.writeText(content)
        temp.delete()
    }
}
