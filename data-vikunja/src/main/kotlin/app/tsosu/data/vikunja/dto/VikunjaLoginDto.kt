package app.tsosu.data.vikunja.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VikunjaLoginRequest(
    val username: String,
    val password: String,
    @SerialName("long_token") val longToken: Boolean = true,
)

@Serializable
data class VikunjaLoginResponse(
    val token: String,
)

@Serializable
data class VikunjaInfoResponse(
    val version: String,
    @SerialName("frontend_url") val frontendUrl: String = "",
)
