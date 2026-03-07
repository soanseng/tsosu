package app.tsosu.data.vikunja.repository

import app.tsosu.data.vikunja.api.VikunjaApi
import app.tsosu.data.vikunja.api.VikunjaApiProvider
import app.tsosu.data.vikunja.auth.VikunjaCredentialStore
import app.tsosu.data.vikunja.dto.VikunjaLoginRequest
import app.tsosu.data.vikunja.sync.EnergyLabelManager
import app.tsosu.data.vikunja.sync.SyncDispatcher
import app.tsosu.data.vikunja.sync.SyncManager
import app.tsosu.domain.repository.ServerInfo
import app.tsosu.domain.repository.SyncRepository
import app.tsosu.domain.repository.SyncResult
import app.tsosu.domain.repository.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class SyncRepositoryImpl(
    private val credentialStore: VikunjaCredentialStore,
    private val syncManagerFactory: (VikunjaApi) -> SyncManager,
    private val energyLabelManagerFactory: (VikunjaApi) -> EnergyLabelManager,
    private val syncDispatcher: SyncDispatcher,
) : SyncRepository {

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    private var currentApi: VikunjaApi? = null

    override fun syncState(): Flow<SyncState> = _syncState

    override suspend fun configureServer(url: String, token: String): Result<ServerInfo> {
        return try {
            val api = VikunjaApiProvider.create(url) { token }
            val info = api.getInfo()
            credentialStore.save(url, token, "")
            currentApi = api
            Result.success(ServerInfo(url, info.version))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun login(url: String, username: String, password: String): Result<ServerInfo> {
        return try {
            val tempApi = VikunjaApiProvider.create(url) { null }
            val loginResponse = tempApi.login(VikunjaLoginRequest(username, password))
            val api = VikunjaApiProvider.create(url) { loginResponse.token }
            val info = api.getInfo()
            credentialStore.save(url, loginResponse.token, username)
            currentApi = api
            Result.success(ServerInfo(url, info.version))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun disconnect() {
        credentialStore.clear()
        currentApi = null
        _syncState.value = SyncState.IDLE
    }

    override suspend fun sync(): Result<SyncResult> {
        val api = getOrRestoreApi()
            ?: return Result.failure(IllegalStateException("Not configured"))

        _syncState.value = SyncState.SYNCING
        return try {
            // Push local changes first
            syncDispatcher.drainQueue()

            val energyManager = energyLabelManagerFactory(api)
            energyManager.ensureLabelsExist()

            val syncManager = syncManagerFactory(api)
            val pulledProjects = syncManager.pullProjects()
            syncManager.pullLabels()
            val pulledTasks = syncManager.pullTasks()

            _syncState.value = SyncState.IDLE
            Result.success(SyncResult(pushed = 0, pulled = pulledTasks + pulledProjects, conflicts = 0))
        } catch (e: Exception) {
            _syncState.value = SyncState.ERROR
            Result.failure(e)
        }
    }

    override fun isRemoteConfigured(): Flow<Boolean> = credentialStore.isConfigured()

    private suspend fun getOrRestoreApi(): VikunjaApi? {
        currentApi?.let { return it }
        val url = credentialStore.getServerUrl() ?: return null
        val token = credentialStore.getToken() ?: return null
        val api = VikunjaApiProvider.create(url) { token }
        currentApi = api
        return api
    }
}
