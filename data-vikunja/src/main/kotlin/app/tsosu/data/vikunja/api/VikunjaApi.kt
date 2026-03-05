package app.tsosu.data.vikunja.api

import app.tsosu.data.vikunja.dto.VikunjaBulkLabelsDto
import app.tsosu.data.vikunja.dto.VikunjaInfoResponse
import app.tsosu.data.vikunja.dto.VikunjaLabelDto
import app.tsosu.data.vikunja.dto.VikunjaLabelTaskDto
import app.tsosu.data.vikunja.dto.VikunjaLoginRequest
import app.tsosu.data.vikunja.dto.VikunjaLoginResponse
import app.tsosu.data.vikunja.dto.VikunjaProjectDto
import app.tsosu.data.vikunja.dto.VikunjaTaskDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface VikunjaApi {

    // Auth
    @POST("login")
    suspend fun login(@Body request: VikunjaLoginRequest): VikunjaLoginResponse

    @GET("info")
    suspend fun getInfo(): VikunjaInfoResponse

    // Tasks
    @GET("tasks")
    suspend fun getAllTasks(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50,
        @Query("expand") expand: String = "labels",
    ): List<VikunjaTaskDto>

    @GET("tasks/{id}")
    suspend fun getTask(
        @Path("id") id: Long,
        @Query("expand") expand: String = "labels",
    ): VikunjaTaskDto

    @PUT("projects/{projectId}/tasks")
    suspend fun createTask(
        @Path("projectId") projectId: Long,
        @Body task: VikunjaTaskDto,
    ): VikunjaTaskDto

    @POST("tasks/{id}")
    suspend fun updateTask(
        @Path("id") id: Long,
        @Body task: VikunjaTaskDto,
    ): VikunjaTaskDto

    @DELETE("tasks/{id}")
    suspend fun deleteTask(@Path("id") id: Long)

    // Labels
    @GET("labels")
    suspend fun getLabels(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50,
    ): List<VikunjaLabelDto>

    @PUT("labels")
    suspend fun createLabel(@Body label: VikunjaLabelDto): VikunjaLabelDto

    @PUT("tasks/{taskId}/labels")
    suspend fun attachLabel(
        @Path("taskId") taskId: Long,
        @Body label: VikunjaLabelTaskDto,
    ): VikunjaLabelDto

    @DELETE("tasks/{taskId}/labels/{labelId}")
    suspend fun detachLabel(
        @Path("taskId") taskId: Long,
        @Path("labelId") labelId: Long,
    )

    @PUT("tasks/{taskId}/labels/bulk")
    suspend fun bulkUpdateLabels(
        @Path("taskId") taskId: Long,
        @Body body: VikunjaBulkLabelsDto,
    )

    // Projects
    @GET("projects")
    suspend fun getProjects(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50,
    ): List<VikunjaProjectDto>

    @GET("projects/{id}")
    suspend fun getProject(@Path("id") id: Long): VikunjaProjectDto

    @PUT("projects")
    suspend fun createProject(@Body project: VikunjaProjectDto): VikunjaProjectDto

    @POST("projects/{id}")
    suspend fun updateProject(
        @Path("id") id: Long,
        @Body project: VikunjaProjectDto,
    ): VikunjaProjectDto
}
