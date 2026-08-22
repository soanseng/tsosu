package app.tsosu

import android.content.Intent

/**
 * Extracts shared text from ACTION_SEND / ACTION_PROCESS_TEXT intents so any
 * app can send content straight into quick-add.
 */
object SharedCaptureText {

    fun fromIntent(intent: Intent?): String? {
        if (intent == null) return null
        val text = when (intent.action) {
            Intent.ACTION_SEND ->
                if (intent.type == "text/plain" || intent.type == null) {
                    @Suppress("DEPRECATION")
                    intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
                } else {
                    null
                }
            Intent.ACTION_PROCESS_TEXT ->
                if (intent.type == "text/plain" || intent.type == null) {
                    intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
                } else {
                    null
                }
            else -> null
        }
        return text?.trim()?.takeIf { it.isNotEmpty() }
    }
}
