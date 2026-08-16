package app.tsosu.domain.repository

import kotlinx.coroutines.flow.Flow

data class SyncResult(val exported: Int, val imported: Int)

enum class SyncState { IDLE, SYNCING, ERROR }

interface SyncRepository {
    fun syncState(): Flow<SyncState>
    fun isConfigured(): Flow<Boolean>

    /** Import the markdown vault into the local cache; external edits win. */
    suspend fun pull(): Result<Unit>

    /** Export the local cache back to the markdown vault. */
    suspend fun push(): Result<Unit>

    /** Pull then push — full round-trip (setup, manual sync, import). */
    suspend fun sync(): Result<SyncResult>
    suspend fun disconnect()
}
