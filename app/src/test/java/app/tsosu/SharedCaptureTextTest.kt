package app.tsosu

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SharedCaptureTextTest {

    @Test
    fun `ACTION_SEND with plain text extracts trimmed content`() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "  Buy oat milk  ")
        }
        assertEquals("Buy oat milk", SharedCaptureText.fromIntent(intent))
    }

    @Test
    fun `ACTION_SEND with non-text mime is ignored`() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_TEXT, "not text")
        }
        assertNull(SharedCaptureText.fromIntent(intent))
    }

    @Test
    fun `ACTION_PROCESS_TEXT extracts selection`() {
        val intent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_PROCESS_TEXT, "Read this paper")
        }
        assertEquals("Read this paper", SharedCaptureText.fromIntent(intent))
    }

    @Test
    fun `blank or missing extras yield null`() {
        assertNull(SharedCaptureText.fromIntent(Intent(Intent.ACTION_SEND).apply { type = "text/plain" }))
        assertNull(SharedCaptureText.fromIntent(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "   ")
        }))
        assertNull(SharedCaptureText.fromIntent(null))
        assertNull(SharedCaptureText.fromIntent(Intent(Intent.ACTION_VIEW)))
    }
}
