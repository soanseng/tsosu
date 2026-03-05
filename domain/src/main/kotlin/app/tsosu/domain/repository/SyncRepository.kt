package app.tsosu.domain.repository

import kotlinx.coroutines.flow.Flow

data class ServerInfo(val url: String, val version: String)

data class SyncResult(val pushed: Int, val pulled: Int, val conflicts: Int)

enum class SyncState { IDLE, SYNCING, ERROR }

interface SyncRepository {
    fun syncState(): Flow<SyncState>
    suspend fun configureServer(url: String, token: String): Result<ServerInfo>
    suspend fun disconnect()
    suspend fun sync(): Result<SyncResult>
    fun isRemoteConfigured(): Flow<Boolean>
}
