package app.tsosu.domain.repository

import kotlinx.coroutines.flow.Flow

data class SyncResult(val exported: Int, val imported: Int)

enum class SyncState { IDLE, SYNCING, ERROR }

interface SyncRepository {
    fun syncState(): Flow<SyncState>
    fun isConfigured(): Flow<Boolean>
    suspend fun sync(): Result<SyncResult>
    suspend fun disconnect()
}
