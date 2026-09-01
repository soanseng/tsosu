package app.tsosu.util

import android.net.Uri
import android.provider.DocumentsContract

/**
 * Initial location for SAF tree pickers. Without it the picker opens on
 * "Recent" / the storage root. Landing in Documents puts the user in a
 * grantable, browsable location (the storage root itself and Download are
 * not grantable since Android 11). If the location can't be resolved the
 * picker silently falls back to its default view.
 *
 * Note: if the picker shows "Can't use this folder" everywhere with empty
 * listings, the device's com.android.externalstorage provider has a stale
 * mount namespace (NoSuchFileException: /storage/emulated in logcat).
 * Restarting that provider (`am force-stop com.android.externalstorage`)
 * or rebooting fixes it — not an app bug.
 */
object StorageUris {
    private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"

    fun browseStartUri(): Uri = DocumentsContract.buildDocumentUri(
        EXTERNAL_STORAGE_AUTHORITY,
        "primary:Documents",
    )
}
