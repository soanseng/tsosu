package app.tsosu.data.markdown

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AtomicFileWriterTest {

    private class FakeFile(
        private val map: MutableMap<String, FakeFile>,
        var name: String,
    ) : WritableFile {
        var content: String? = null
        var deleted = false
        var renameFails = false
        var writeFails = false

        override fun writeText(content: String): Boolean {
            if (writeFails) return false
            this.content = content
            return true
        }

        override fun delete(): Boolean {
            map.remove(name)
            deleted = true
            return true
        }

        override fun renameTo(name: String): Boolean {
            if (renameFails) return false
            map.remove(this.name)
            this.name = name
            map[name] = this
            return true
        }
    }

    private class FakeFolder : WritableFolder {
        val files = mutableMapOf<String, FakeFile>()

        override fun findFile(name: String): WritableFile? = files[name]

        override fun createFile(name: String): WritableFile? =
            FakeFile(files, name).also { files[name] = it }
    }

    private fun folderOf(vararg entries: Pair<String, String>): FakeFolder =
        FakeFolder().apply {
            for ((name, content) in entries) {
                files[name] = FakeFile(files, name).apply { this.content = content }
            }
        }

    @Test
    fun `happy path writes temp then renames over deleted target`() {
        val folder = folderOf("tasks.md" to "OLD")

        AtomicFileWriter(folder).write("tasks.md", "NEW")

        assertEquals("NEW", folder.files["tasks.md"]!!.content)
        assertNull(folder.files["tasks.md.tmp"], "temp should be gone (renamed)")
    }

    @Test
    fun `rename refusal falls back to direct write and cleans temp`() {
        val folder = folderOf("tasks.md" to "OLD")
        // Stale temp file whose rename always fails (provider quirk).
        val staleTemp = FakeFile(folder.files, "tasks.md.tmp").apply { renameFails = true }
        folder.files["tasks.md.tmp"] = staleTemp

        AtomicFileWriter(folder).write("tasks.md", "NEW")

        assertEquals("NEW", folder.files["tasks.md"]!!.content)
        assertTrue(staleTemp.deleted, "temp should be cleaned up")
    }

    @Test
    fun `temp write failure deletes temp and leaves target untouched`() {
        val folder = folderOf("tasks.md" to "OLD")
        val staleTemp = FakeFile(folder.files, "tasks.md.tmp").apply { writeFails = true }
        folder.files["tasks.md.tmp"] = staleTemp

        AtomicFileWriter(folder).write("tasks.md", "NEW")

        // Temp write failed: bailing out means the target must stay exactly
        // as it was and the temp must be removed.
        assertEquals("OLD", folder.files["tasks.md"]!!.content, "target must be untouched")
        assertTrue(staleTemp.deleted, "failed temp should be removed")
    }

    @Test
    fun `first write with no existing target creates via rename`() {
        val folder = FakeFolder()

        AtomicFileWriter(folder).write("habits.md", "FIRST")

        assertEquals("FIRST", folder.files["habits.md"]!!.content)
        assertNull(folder.files["habits.md.tmp"])
    }
}
