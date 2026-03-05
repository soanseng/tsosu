# Phase 2: Vikunja Sync + CalDAV + Nudge Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Connect Tsosu to Vikunja REST API for two-way task/habit sync, add Fastmail CalDAV calendar sync, gentle notifications, stale cleanup, and Todoist import.

**Architecture:** New `data-vikunja/` module (Retrofit + Kotlin Serialization) for API access and sync engine. New `data-calendar/` module (ical4j + OkHttp) for CalDAV. Both implement existing domain repository interfaces (SyncRepository, CalendarRepository, ImportRepository). Sync is local-first: changes queue in SyncQueueEntity, push on connectivity. Server-wins conflict resolution preserving local-only fields (isFocus, calendarEventId, HabitCompletion).

**Tech Stack:** Retrofit 2.11+, OkHttp 4.12+, Kotlin Serialization, ical4j 4.0+, WorkManager (periodic sync), DataStore (credentials), MockWebServer (tests)

**Fastmail CalDAV:** `https://caldav.fastmail.com/dav/calendars/user/soanseng@anatomind.com/268d22a4-6fa0-4019-ba4c-a517ac503b2b` — requires app-specific password, SSL required.

**Critical API behaviors (from Phase 0):**
- POST /api/v1/tasks/{id} zeros fields not included — MUST send ALL fields
- Repeating tasks: same ID reused, `done` resets to false, `due_date` advances
- Label `hex_color` stored WITHOUT `#` prefix
- Labels are read-only on task GET; use PUT /api/v1/tasks/{task}/labels to attach

---

## Batch 0: Module Scaffolding + Dependencies (3 tasks)

### Task 1: Add networking dependencies to version catalog

**Files:**
- Modify: `gradle/libs.versions.toml`

**Steps:**

1. Add versions and libraries to `gradle/libs.versions.toml`:

```toml
# Under [versions], add:
retrofit = "2.11.0"
okhttp = "4.12.0"
okhttp-mockwebserver = "4.12.0"
datastore = "1.1.3"
work = "2.10.0"
ical4j = "4.0.7"

# Under [libraries], add:
# Networking
retrofit-core = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-kotlinx-serialization = { group = "com.squareup.retrofit2", name = "converter-kotlinx-serialization", version.ref = "retrofit" }
okhttp-core = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
okhttp-mockwebserver = { group = "com.squareup.okhttp3", name = "mockwebserver", version.ref = "okhttp-mockwebserver" }

# DataStore
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

# WorkManager
work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "work" }
work-testing = { group = "androidx.work", name = "work-testing", version.ref = "work" }

# CalDAV
ical4j = { group = "org.mnode.ical4j", name = "ical4j", version.ref = "ical4j" }

# Under [plugins], add:
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

2. Verify: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew --refresh-dependencies help`

### Task 2: Create data-vikunja module

**Files:**
- Create: `data-vikunja/build.gradle.kts`
- Create: `data-vikunja/src/main/AndroidManifest.xml`
- Modify: `settings.gradle.kts` (add `:data-vikunja`)

**Steps:**

1. Add `include(":data-vikunja")` to `settings.gradle.kts`

2. Create `data-vikunja/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "app.tsosu.data.vikunja"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data-local"))

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.datastore.preferences)
    implementation(libs.work.runtime.ktx)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)

    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(kotlin("test"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

3. Create minimal `data-vikunja/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest />
```

4. Verify build: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-vikunja:assemble`

### Task 3: Create data-calendar module

**Files:**
- Create: `data-calendar/build.gradle.kts`
- Create: `data-calendar/src/main/AndroidManifest.xml`
- Modify: `settings.gradle.kts` (add `:data-calendar`)

**Steps:**

1. Add `include(":data-calendar")` to `settings.gradle.kts`

2. Create `data-calendar/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "app.tsosu.data.calendar"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":domain"))

    implementation(libs.ical4j)
    implementation(libs.okhttp.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.datastore.preferences)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(kotlin("test"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

3. Create minimal `data-calendar/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest />
```

4. Update `app/build.gradle.kts` to depend on new modules:

```kotlin
// Add under existing implementation lines:
implementation(project(":data-vikunja"))
implementation(project(":data-calendar"))
```

5. Verify full build: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug`

---

## Batch 1: Vikunja API Layer — DTOs + Retrofit (3 tasks)

### Task 4: Vikunja DTOs (Kotlin Serialization)

**Files:**
- Create: `data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/dto/VikunjaTaskDto.kt`
- Create: `data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/dto/VikunjaProjectDto.kt`
- Create: `data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/dto/VikunjaLabelDto.kt`
- Create: `data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/dto/VikunjaLoginDto.kt`

**Steps:**

1. Create `VikunjaTaskDto.kt`:

```kotlin
package app.tsosu.data.vikunja.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VikunjaTaskDto(
    val id: Long = 0,
    val title: String = "",
    val description: String = "",
    val done: Boolean = false,
    @SerialName("done_at") val doneAt: String? = null,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    val priority: Long = 0,
    @SerialName("project_id") val projectId: Long = 0,
    val position: Double = 0.0,
    @SerialName("repeat_after") val repeatAfter: Long = 0,
    @SerialName("repeat_mode") val repeatMode: Int = 0,
    @SerialName("hex_color") val hexColor: String = "",
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    val labels: List<VikunjaLabelDto> = emptyList(),
    @SerialName("percent_done") val percentDone: Double = 0.0,
    val created: String? = null,
    val updated: String? = null,
)
```

2. Create `VikunjaProjectDto.kt`:

```kotlin
package app.tsosu.data.vikunja.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VikunjaProjectDto(
    val id: Long = 0,
    val title: String = "",
    val description: String = "",
    @SerialName("hex_color") val hexColor: String = "",
    @SerialName("parent_project_id") val parentProjectId: Long = 0,
    val position: Double = 0.0,
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    @SerialName("is_archived") val isArchived: Boolean = false,
)
```

3. Create `VikunjaLabelDto.kt`:

```kotlin
package app.tsosu.data.vikunja.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VikunjaLabelDto(
    val id: Long = 0,
    val title: String = "",
    @SerialName("hex_color") val hexColor: String = "",
    val description: String = "",
)

@Serializable
data class VikunjaLabelTaskDto(
    @SerialName("label_id") val labelId: Long,
)
```

4. Create `VikunjaLoginDto.kt`:

```kotlin
package app.tsosu.data.vikunja.dto

import kotlinx.serialization.Serializable

@Serializable
data class VikunjaLoginRequest(
    val username: String,
    val password: String,
    val long_token: Boolean = true,
)

@Serializable
data class VikunjaLoginResponse(
    val token: String,
)

@Serializable
data class VikunjaInfoResponse(
    val version: String,
    val frontend_url: String = "",
)
```

5. Verify: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-vikunja:compileDebugKotlin`

### Task 5: Retrofit API Interface

**Files:**
- Create: `data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/api/VikunjaApi.kt`

**Steps:**

1. Create `VikunjaApi.kt`:

```kotlin
package app.tsosu.data.vikunja.api

import app.tsosu.data.vikunja.dto.*
import retrofit2.http.*

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

    @GET("projects/{id}/views/{view}/tasks")
    suspend fun getProjectTasks(
        @Path("id") projectId: Long,
        @Path("view") viewId: Long,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50,
        @Query("expand") expand: String = "labels",
    ): List<VikunjaTaskDto>
}
```

2. Add missing DTO for bulk labels:

```kotlin
// Append to VikunjaLabelDto.kt
@Serializable
data class VikunjaBulkLabelsDto(
    val labels: List<VikunjaLabelDto>,
)
```

3. Verify: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-vikunja:compileDebugKotlin`

### Task 6: Retrofit provider + Auth interceptor

**Files:**
- Create: `data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/api/AuthInterceptor.kt`
- Create: `data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/api/VikunjaApiProvider.kt`

**Steps:**

1. Create `AuthInterceptor.kt`:

```kotlin
package app.tsosu.data.vikunja.api

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenProvider: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = tokenProvider()
        return if (token != null) {
            val authenticatedRequest = request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            chain.proceed(authenticatedRequest)
        } else {
            chain.proceed(request)
        }
    }
}
```

2. Create `VikunjaApiProvider.kt`:

```kotlin
package app.tsosu.data.vikunja.api

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object VikunjaApiProvider {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    fun create(baseUrl: String, tokenProvider: () -> String?): VikunjaApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenProvider))
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl(baseUrl.trimEnd('/') + "/api/v1/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(VikunjaApi::class.java)
    }
}
```

3. Verify: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-vikunja:compileDebugKotlin`

---

## Batch 2: Vikunja Task Mapper — TDD (3 tasks)

### Task 7: VikunjaTaskMapper tests

**Files:**
- Create: `data-vikunja/src/test/kotlin/app/tsosu/data/vikunja/mapper/VikunjaTaskMapperTest.kt`

**Steps:**

1. Create test file:

```kotlin
package app.tsosu.data.vikunja.mapper

import app.tsosu.data.vikunja.dto.VikunjaLabelDto
import app.tsosu.data.vikunja.dto.VikunjaTaskDto
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Priority
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VikunjaTaskMapperTest {

    private val mapper = VikunjaTaskMapper()

    // --- Description metadata encoding ---

    @Test
    fun `appendEstimate adds metadata comment to description`() {
        val result = mapper.appendEstimate("Buy groceries", 30)
        assertEquals("Buy groceries\n<!-- tsosu:{\"est\":30} -->", result)
    }

    @Test
    fun `appendEstimate with null minutes returns clean description`() {
        val result = mapper.appendEstimate("Buy groceries", null)
        assertEquals("Buy groceries", result)
    }

    @Test
    fun `appendEstimate replaces existing metadata`() {
        val desc = "Buy groceries\n<!-- tsosu:{\"est\":15} -->"
        val result = mapper.appendEstimate(desc, 30)
        assertEquals("Buy groceries\n<!-- tsosu:{\"est\":30} -->", result)
    }

    @Test
    fun `extractEstimate parses minutes from metadata`() {
        val desc = "Buy groceries\n<!-- tsosu:{\"est\":30} -->"
        assertEquals(30, mapper.extractEstimate(desc))
    }

    @Test
    fun `extractEstimate returns null when no metadata`() {
        assertNull(mapper.extractEstimate("Buy groceries"))
    }

    @Test
    fun `stripMetadata removes tsosu comment`() {
        val desc = "Buy groceries\n<!-- tsosu:{\"est\":30} -->"
        assertEquals("Buy groceries", mapper.stripMetadata(desc))
    }

    // --- Energy label mapping ---

    @Test
    fun `extractEnergyFromLabels finds high energy label`() {
        val labels = listOf(
            VikunjaLabelDto(id = 1, title = "urgent"),
            VikunjaLabelDto(id = 2, title = "high"),
        )
        assertEquals(EnergyLevel.HIGH, mapper.extractEnergyFromLabels(labels))
    }

    @Test
    fun `extractEnergyFromLabels finds medium energy label`() {
        val labels = listOf(VikunjaLabelDto(id = 3, title = "medium"))
        assertEquals(EnergyLevel.MEDIUM, mapper.extractEnergyFromLabels(labels))
    }

    @Test
    fun `extractEnergyFromLabels finds low energy label`() {
        val labels = listOf(VikunjaLabelDto(id = 4, title = "low"))
        assertEquals(EnergyLevel.LOW, mapper.extractEnergyFromLabels(labels))
    }

    @Test
    fun `extractEnergyFromLabels returns null when no energy label`() {
        val labels = listOf(VikunjaLabelDto(id = 1, title = "urgent"))
        assertNull(mapper.extractEnergyFromLabels(labels))
    }

    // --- Domain to DTO ---

    @Test
    fun `domainToDto maps all task fields correctly`() {
        val dto = mapper.domainToDto(
            title = "Test task",
            description = "Some notes",
            done = false,
            dueDate = "2026-03-10T09:00:00+08:00",
            priority = 3,
            projectId = 5L,
            position = 1.5,
            estimatedMinutes = 30,
            repeatAfterSeconds = null,
            hexColor = "",
        )
        assertEquals("Test task", dto.title)
        assertEquals("Some notes\n<!-- tsosu:{\"est\":30} -->", dto.description)
        assertEquals(3L, dto.priority)
        assertEquals(5L, dto.projectId)
        assertEquals(1.5, dto.position)
    }

    // --- DTO to domain fields ---

    @Test
    fun `dtoToDomain extracts estimatedMinutes from description`() {
        val dto = VikunjaTaskDto(
            id = 1,
            title = "Test",
            description = "Notes\n<!-- tsosu:{\"est\":45} -->",
            priority = 2,
        )
        val fields = mapper.dtoToDomainFields(dto)
        assertEquals(45, fields.estimatedMinutes)
        assertEquals("Notes", fields.cleanDescription)
        assertEquals(Priority.MEDIUM, fields.priority)
    }

    @Test
    fun `dtoToDomain preserves null estimate when no metadata`() {
        val dto = VikunjaTaskDto(id = 1, title = "Test", description = "Plain notes")
        val fields = mapper.dtoToDomainFields(dto)
        assertNull(fields.estimatedMinutes)
        assertEquals("Plain notes", fields.cleanDescription)
    }

    @Test
    fun `non-energy labels are preserved in extraction`() {
        val labels = listOf(
            VikunjaLabelDto(id = 1, title = "work"),
            VikunjaLabelDto(id = 2, title = "high"),
        )
        val nonEnergy = mapper.getNonEnergyLabels(labels)
        assertEquals(1, nonEnergy.size)
        assertEquals("work", nonEnergy[0].title)
    }
}
```

2. Run tests — they should fail (class not found):

```bash
ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-vikunja:test
```

### Task 8: VikunjaTaskMapper implementation

**Files:**
- Create: `data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/mapper/VikunjaTaskMapper.kt`

**Steps:**

1. Create `VikunjaTaskMapper.kt`:

```kotlin
package app.tsosu.data.vikunja.mapper

import app.tsosu.data.vikunja.dto.VikunjaLabelDto
import app.tsosu.data.vikunja.dto.VikunjaTaskDto
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Priority

class VikunjaTaskMapper {

    private val metadataRegex = Regex("\\s*<!-- tsosu:\\{.*?} -->\\s*")

    // Energy label titles (match EnergyLevel.labelTitle but without emoji prefix for search)
    private val energyLabelTitles = EnergyLevel.entries.associateBy { it.labelTitle }

    fun appendEstimate(description: String, minutes: Int?): String {
        val cleaned = description.replace(metadataRegex, "").trimEnd()
        return if (minutes != null) {
            "$cleaned\n<!-- tsosu:{\"est\":$minutes} -->"
        } else {
            cleaned
        }
    }

    fun extractEstimate(description: String): Int? {
        return Regex("<!-- tsosu:\\{\"est\":(\\d+)} -->")
            .find(description)
            ?.groupValues?.get(1)?.toIntOrNull()
    }

    fun stripMetadata(description: String): String {
        return description.replace(metadataRegex, "").trimEnd()
    }

    fun extractEnergyFromLabels(labels: List<VikunjaLabelDto>): EnergyLevel? {
        return labels.firstNotNullOfOrNull { label ->
            energyLabelTitles[label.title]
        }
    }

    fun getNonEnergyLabels(labels: List<VikunjaLabelDto>): List<VikunjaLabelDto> {
        return labels.filter { label -> label.title !in energyLabelTitles }
    }

    fun domainToDto(
        title: String,
        description: String,
        done: Boolean,
        dueDate: String?,
        priority: Int,
        projectId: Long,
        position: Double,
        estimatedMinutes: Int?,
        repeatAfterSeconds: Long?,
        hexColor: String,
    ): VikunjaTaskDto {
        return VikunjaTaskDto(
            title = title,
            description = appendEstimate(description, estimatedMinutes),
            done = done,
            dueDate = dueDate,
            priority = priority.toLong(),
            projectId = projectId,
            position = position,
            repeatAfter = repeatAfterSeconds ?: 0,
            hexColor = hexColor,
        )
    }

    data class DomainFields(
        val cleanDescription: String,
        val estimatedMinutes: Int?,
        val energyLevel: EnergyLevel?,
        val priority: Priority,
    )

    fun dtoToDomainFields(dto: VikunjaTaskDto): DomainFields {
        return DomainFields(
            cleanDescription = stripMetadata(dto.description),
            estimatedMinutes = extractEstimate(dto.description),
            energyLevel = extractEnergyFromLabels(dto.labels),
            priority = Priority.fromValue(dto.priority.toInt()),
        )
    }
}
```

2. Run tests: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-vikunja:test`
   Expected: ALL PASS

### Task 9: VikunjaHabitMapper + VikunjaRoutineMapper (TDD)

**Files:**
- Create: `data-vikunja/src/test/kotlin/app/tsosu/data/vikunja/mapper/VikunjaHabitMapperTest.kt`
- Create: `data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/mapper/VikunjaHabitMapper.kt`
- Create: `data-vikunja/src/test/kotlin/app/tsosu/data/vikunja/mapper/VikunjaRoutineMapperTest.kt`
- Create: `data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/mapper/VikunjaRoutineMapper.kt`

**Steps:**

1. Create `VikunjaHabitMapperTest.kt`:

```kotlin
package app.tsosu.data.vikunja.mapper

import app.tsosu.data.vikunja.dto.VikunjaTaskDto
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.HabitFrequency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VikunjaHabitMapperTest {

    private val mapper = VikunjaHabitMapper()

    @Test
    fun `habitToTaskDto sets repeatAfter to 86400 for daily`() {
        val dto = mapper.habitToTaskDto(
            title = "Meditate",
            tinyVersion = "Sit, take 3 breaths",
            frequency = HabitFrequency.DAILY,
            routineProjectId = 10L,
            position = 1.0,
            hexColor = "4CAF50",
        )
        assertEquals(86400L, dto.repeatAfter)
        assertEquals("Meditate", dto.title)
        assertEquals(10L, dto.projectId)
        assertTrue(dto.description.contains("Tsosu Habit"))
        assertTrue(dto.description.contains("Sit, take 3 breaths"))
    }

    @Test
    fun `habitToTaskDto without tinyVersion still has marker`() {
        val dto = mapper.habitToTaskDto(
            title = "Exercise",
            tinyVersion = null,
            frequency = HabitFrequency.DAILY,
            routineProjectId = 10L,
            position = 0.0,
            hexColor = "4CAF50",
        )
        assertTrue(dto.description.contains("-- Tsosu Habit"))
        assertFalse(dto.description.contains("Tiny version"))
    }

    @Test
    fun `extractTinyVersion parses from description`() {
        val desc = "Tiny version: Sit, take 3 breaths\n\n-- Tsosu Habit"
        assertEquals("Sit, take 3 breaths", mapper.extractTinyVersion(desc))
    }

    @Test
    fun `extractTinyVersion returns null when absent`() {
        assertEquals(null, mapper.extractTinyVersion("-- Tsosu Habit"))
    }

    @Test
    fun `isHabitTask identifies habit by all three criteria`() {
        val dto = VikunjaTaskDto(
            id = 1,
            repeatAfter = 86400,
            projectId = 10,
            description = "Some text\n\n-- Tsosu Habit",
        )
        assertTrue(mapper.isHabitTask(dto, setOf(10L)))
    }

    @Test
    fun `isHabitTask rejects task without marker`() {
        val dto = VikunjaTaskDto(
            id = 1,
            repeatAfter = 86400,
            projectId = 10,
            description = "Just a repeating task",
        )
        assertFalse(mapper.isHabitTask(dto, setOf(10L)))
    }

    @Test
    fun `isHabitTask rejects task not in routine project`() {
        val dto = VikunjaTaskDto(
            id = 1,
            repeatAfter = 86400,
            projectId = 99,
            description = "-- Tsosu Habit",
        )
        assertFalse(mapper.isHabitTask(dto, setOf(10L)))
    }
}
```

2. Create `VikunjaHabitMapper.kt`:

```kotlin
package app.tsosu.data.vikunja.mapper

import app.tsosu.data.vikunja.dto.VikunjaTaskDto
import app.tsosu.domain.model.HabitFrequency

class VikunjaHabitMapper {

    companion object {
        const val HABIT_MARKER = "-- Tsosu Habit"
    }

    fun habitToTaskDto(
        title: String,
        tinyVersion: String?,
        frequency: HabitFrequency,
        routineProjectId: Long,
        position: Double,
        hexColor: String,
    ): VikunjaTaskDto {
        return VikunjaTaskDto(
            title = title,
            description = buildHabitDescription(tinyVersion),
            repeatAfter = frequency.repeatAfterSeconds,
            repeatMode = 0,
            projectId = routineProjectId,
            position = position,
            hexColor = hexColor,
        )
    }

    private fun buildHabitDescription(tinyVersion: String?): String {
        return buildString {
            if (tinyVersion != null) append("Tiny version: $tinyVersion\n\n")
            append(HABIT_MARKER)
        }
    }

    fun extractTinyVersion(description: String?): String? {
        if (description == null) return null
        val match = Regex("Tiny version: (.+)").find(description)
        return match?.groupValues?.get(1)?.trim()
    }

    fun isHabitTask(dto: VikunjaTaskDto, routineProjectIds: Set<Long>): Boolean {
        return dto.repeatAfter > 0
            && dto.projectId in routineProjectIds
            && dto.description.contains(HABIT_MARKER)
    }
}
```

3. Create `VikunjaRoutineMapperTest.kt`:

```kotlin
package app.tsosu.data.vikunja.mapper

import app.tsosu.data.vikunja.dto.VikunjaProjectDto
import app.tsosu.domain.model.RoutineTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VikunjaRoutineMapperTest {

    private val mapper = VikunjaRoutineMapper()

    @Test
    fun `routineToProjectDto includes metadata marker`() {
        val dto = mapper.routineToProjectDto(
            title = "Morning Routine",
            timeOfDay = RoutineTime.MORNING,
        )
        assertTrue(dto.description.contains("<!-- tsosu-routine:MORNING -->"))
        assertEquals("Morning Routine", dto.title)
    }

    @Test
    fun `extractRoutineTime parses MORNING from description`() {
        val desc = "<!-- tsosu-routine:MORNING -->"
        assertEquals(RoutineTime.MORNING, mapper.extractRoutineTime(desc))
    }

    @Test
    fun `extractRoutineTime parses EVENING from description`() {
        val desc = "Some notes\n<!-- tsosu-routine:EVENING -->"
        assertEquals(RoutineTime.EVENING, mapper.extractRoutineTime(desc))
    }

    @Test
    fun `extractRoutineTime returns null for non-routine project`() {
        assertNull(mapper.extractRoutineTime("Just a regular project"))
    }

    @Test
    fun `isRoutineProject detects routine marker`() {
        val dto = VikunjaProjectDto(description = "<!-- tsosu-routine:AFTERNOON -->")
        assertTrue(mapper.isRoutineProject(dto))
    }

    @Test
    fun `isRoutineProject rejects normal project`() {
        val dto = VikunjaProjectDto(description = "Regular project")
        assertFalse(mapper.isRoutineProject(dto))
    }
}
```

4. Create `VikunjaRoutineMapper.kt`:

```kotlin
package app.tsosu.data.vikunja.mapper

import app.tsosu.data.vikunja.dto.VikunjaProjectDto
import app.tsosu.domain.model.RoutineTime

class VikunjaRoutineMapper {

    private val routineRegex = Regex("<!-- tsosu-routine:(\\w+) -->")

    fun routineToProjectDto(
        title: String,
        timeOfDay: RoutineTime,
    ): VikunjaProjectDto {
        return VikunjaProjectDto(
            title = title,
            description = "<!-- tsosu-routine:${timeOfDay.name} -->",
        )
    }

    fun extractRoutineTime(description: String?): RoutineTime? {
        if (description == null) return null
        val match = routineRegex.find(description) ?: return null
        return try {
            RoutineTime.valueOf(match.groupValues[1])
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    fun isRoutineProject(dto: VikunjaProjectDto): Boolean {
        return dto.description.contains("<!-- tsosu-routine:")
    }
}
```

5. Run all tests: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-vikunja:test`
   Expected: ALL PASS

---

## Batch 3: Credential Storage + Energy Label Init + SyncManager (3 tasks)

### Task 10: Credential storage with DataStore

**Files:**
- Create: `data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/auth/VikunjaCredentialStore.kt`

**Steps:**

1. Create `VikunjaCredentialStore.kt`:

```kotlin
package app.tsosu.data.vikunja.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.vikunjaDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "vikunja_credentials"
)

class VikunjaCredentialStore(private val context: Context) {

    companion object {
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_TOKEN = stringPreferencesKey("token")
        private val KEY_USERNAME = stringPreferencesKey("username")
    }

    val serverUrl: Flow<String?> = context.vikunjaDataStore.data.map { it[KEY_SERVER_URL] }
    val token: Flow<String?> = context.vikunjaDataStore.data.map { it[KEY_TOKEN] }

    fun isConfigured(): Flow<Boolean> = context.vikunjaDataStore.data.map {
        it[KEY_SERVER_URL] != null && it[KEY_TOKEN] != null
    }

    suspend fun save(serverUrl: String, token: String, username: String) {
        context.vikunjaDataStore.edit { prefs ->
            prefs[KEY_SERVER_URL] = serverUrl
            prefs[KEY_TOKEN] = token
            prefs[KEY_USERNAME] = username
        }
    }

    suspend fun getToken(): String? {
        var result: String? = null
        context.vikunjaDataStore.data.collect { result = it[KEY_TOKEN]; return@collect }
        return result
    }

    suspend fun clear() {
        context.vikunjaDataStore.edit { it.clear() }
    }
}
```

2. Verify: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-vikunja:compileDebugKotlin`

### Task 11: Energy label initializer

**Files:**
- Create: `data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/sync/EnergyLabelManager.kt`
- Create: `data-vikunja/src/test/kotlin/app/tsosu/data/vikunja/sync/EnergyLabelManagerTest.kt`

**Steps:**

1. Create `EnergyLabelManagerTest.kt`:

```kotlin
package app.tsosu.data.vikunja.sync

import app.tsosu.data.vikunja.api.VikunjaApi
import app.tsosu.data.vikunja.dto.VikunjaLabelDto
import app.tsosu.domain.model.EnergyLevel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class EnergyLabelManagerTest {

    private val api = mockk<VikunjaApi>()
    private val manager = EnergyLabelManager(api)

    @Test
    fun `ensureLabelsExist creates missing labels`() = runTest {
        coEvery { api.getLabels(any(), any()) } returns emptyList()
        coEvery { api.createLabel(any()) } answers {
            val input = firstArg<VikunjaLabelDto>()
            input.copy(id = 100)
        }

        val ids = manager.ensureLabelsExist()

        assertEquals(3, ids.size)
        coVerify(exactly = 3) { api.createLabel(any()) }
    }

    @Test
    fun `ensureLabelsExist reuses existing labels`() = runTest {
        val existing = EnergyLevel.entries.mapIndexed { i, level ->
            VikunjaLabelDto(id = (i + 1).toLong(), title = level.labelTitle, hexColor = "")
        }
        coEvery { api.getLabels(any(), any()) } returns existing

        val ids = manager.ensureLabelsExist()

        assertEquals(3, ids.size)
        coVerify(exactly = 0) { api.createLabel(any()) }
    }
}
```

2. Create `EnergyLabelManager.kt`:

```kotlin
package app.tsosu.data.vikunja.sync

import app.tsosu.data.vikunja.api.VikunjaApi
import app.tsosu.data.vikunja.dto.VikunjaLabelDto
import app.tsosu.domain.model.EnergyLevel

class EnergyLabelManager(private val api: VikunjaApi) {

    // Maps EnergyLevel to its Vikunja label ID (populated after ensureLabelsExist)
    private val labelIds = mutableMapOf<EnergyLevel, Long>()

    fun getLabelId(level: EnergyLevel): Long? = labelIds[level]

    suspend fun ensureLabelsExist(): Map<EnergyLevel, Long> {
        val existingLabels = api.getLabels(page = 1, perPage = 200)

        for (level in EnergyLevel.entries) {
            val existing = existingLabels.firstOrNull { it.title == level.labelTitle }
            if (existing != null) {
                labelIds[level] = existing.id
            } else {
                val colorHex = when (level) {
                    EnergyLevel.HIGH -> "4CAF50"
                    EnergyLevel.MEDIUM -> "FFC107"
                    EnergyLevel.LOW -> "90A4AE"
                }
                val created = api.createLabel(
                    VikunjaLabelDto(
                        title = level.labelTitle,
                        hexColor = colorHex,
                    )
                )
                labelIds[level] = created.id
            }
        }

        return labelIds.toMap()
    }
}
```

3. Run tests: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-vikunja:test`

### Task 12: SyncManager — core sync orchestrator (TDD)

**Files:**
- Create: `data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/sync/SyncManager.kt`
- Create: `data-vikunja/src/test/kotlin/app/tsosu/data/vikunja/sync/SyncManagerTest.kt`

**Steps:**

1. Create `SyncManagerTest.kt`:

```kotlin
package app.tsosu.data.vikunja.sync

import app.tsosu.data.local.dao.LabelDao
import app.tsosu.data.local.dao.ProjectDao
import app.tsosu.data.local.dao.TaskDao
import app.tsosu.data.vikunja.api.VikunjaApi
import app.tsosu.data.vikunja.dto.VikunjaLabelDto
import app.tsosu.data.vikunja.dto.VikunjaProjectDto
import app.tsosu.data.vikunja.dto.VikunjaTaskDto
import app.tsosu.data.vikunja.mapper.VikunjaTaskMapper
import app.tsosu.domain.model.EnergyLevel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncManagerTest {

    private val api = mockk<VikunjaApi>(relaxed = true)
    private val taskDao = mockk<TaskDao>(relaxed = true)
    private val projectDao = mockk<ProjectDao>(relaxed = true)
    private val labelDao = mockk<LabelDao>(relaxed = true)
    private val energyLabelManager = mockk<EnergyLabelManager>(relaxed = true)
    private val taskMapper = VikunjaTaskMapper()

    private val syncManager = SyncManager(
        api = api,
        taskDao = taskDao,
        projectDao = projectDao,
        labelDao = labelDao,
        energyLabelManager = energyLabelManager,
        taskMapper = taskMapper,
    )

    @Test
    fun `pullTasks fetches all tasks and upserts to local DB`() = runTest {
        val remoteTasks = listOf(
            VikunjaTaskDto(id = 1, title = "Task 1"),
            VikunjaTaskDto(id = 2, title = "Task 2"),
        )
        coEvery { api.getAllTasks(page = 1) } returns remoteTasks
        coEvery { api.getAllTasks(page = 2) } returns emptyList()
        coEvery { taskDao.getByServerId(any()) } returns null

        val result = syncManager.pullTasks()

        assertEquals(2, result)
        coVerify(exactly = 2) { taskDao.upsert(any()) }
    }

    @Test
    fun `pullTasks preserves local isFocus when updating existing task`() = runTest {
        val remoteTask = VikunjaTaskDto(
            id = 1, title = "Updated Task",
            description = "<!-- tsosu:{\"est\":30} -->",
        )
        coEvery { api.getAllTasks(page = 1) } returns listOf(remoteTask)
        coEvery { api.getAllTasks(page = 2) } returns emptyList()

        val existingLocal = mockk<app.tsosu.data.local.entity.TaskEntity>(relaxed = true)
        coEvery { existingLocal.id } returns "local-uuid-1"
        coEvery { existingLocal.isFocus } returns true
        coEvery { existingLocal.calendarEventId } returns "cal-123"
        coEvery { taskDao.getByServerId(1) } returns existingLocal

        syncManager.pullTasks()

        coVerify {
            taskDao.upsert(match {
                it.isFocus && it.calendarEventId == "cal-123"
            })
        }
    }

    @Test
    fun `pullProjects fetches and stores projects`() = runTest {
        val remoteProjects = listOf(
            VikunjaProjectDto(id = 1, title = "Work"),
            VikunjaProjectDto(id = 2, title = "Morning", description = "<!-- tsosu-routine:MORNING -->"),
        )
        coEvery { api.getProjects(page = 1) } returns remoteProjects
        coEvery { api.getProjects(page = 2) } returns emptyList()
        coEvery { projectDao.getByServerId(any()) } returns null

        val result = syncManager.pullProjects()

        assertEquals(2, result)
    }
}
```

2. Create `SyncManager.kt`:

```kotlin
package app.tsosu.data.vikunja.sync

import app.tsosu.data.local.dao.LabelDao
import app.tsosu.data.local.dao.ProjectDao
import app.tsosu.data.local.dao.TaskDao
import app.tsosu.data.local.entity.LabelEntity
import app.tsosu.data.local.entity.ProjectEntity
import app.tsosu.data.local.entity.TaskEntity
import app.tsosu.data.vikunja.api.VikunjaApi
import app.tsosu.data.vikunja.dto.VikunjaTaskDto
import app.tsosu.data.vikunja.mapper.VikunjaRoutineMapper
import app.tsosu.data.vikunja.mapper.VikunjaTaskMapper
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Priority
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class SyncManager(
    private val api: VikunjaApi,
    private val taskDao: TaskDao,
    private val projectDao: ProjectDao,
    private val labelDao: LabelDao,
    private val energyLabelManager: EnergyLabelManager,
    private val taskMapper: VikunjaTaskMapper,
) {
    private val routineMapper = VikunjaRoutineMapper()

    suspend fun pullTasks(): Int {
        var page = 1
        var totalPulled = 0

        while (true) {
            val tasks = api.getAllTasks(page = page)
            if (tasks.isEmpty()) break

            for (dto in tasks) {
                upsertTaskFromDto(dto)
                totalPulled++
            }
            page++
        }

        return totalPulled
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun upsertTaskFromDto(dto: VikunjaTaskDto) {
        val existing = taskDao.getByServerId(dto.id)
        val fields = taskMapper.dtoToDomainFields(dto)
        val now = Clock.System.now().toEpochMilliseconds()

        val entity = TaskEntity(
            id = existing?.id ?: Uuid.random().toString(),
            serverId = dto.id,
            title = dto.title,
            description = fields.cleanDescription,
            done = dto.done,
            dueDate = null, // TODO: parse ISO date
            priority = fields.priority.value,
            projectId = existing?.projectId, // keep local project mapping
            position = dto.position,
            repeatAfterSeconds = if (dto.repeatAfter > 0) dto.repeatAfter else null,
            calendarEventId = existing?.calendarEventId, // preserve local-only
            estimatedMinutes = fields.estimatedMinutes,
            energyLevel = (fields.energyLevel ?: EnergyLevel.MEDIUM).ordinal,
            isFocus = existing?.isFocus ?: false, // preserve local-only
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            syncStatus = 0,
        )

        taskDao.upsert(entity)
    }

    suspend fun pullProjects(): Int {
        var page = 1
        var totalPulled = 0

        while (true) {
            val projects = api.getProjects(page = page)
            if (projects.isEmpty()) break

            for (dto in projects) {
                upsertProjectFromDto(dto)
                totalPulled++
            }
            page++
        }

        return totalPulled
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun upsertProjectFromDto(dto: app.tsosu.data.vikunja.dto.VikunjaProjectDto) {
        val existing = projectDao.getByServerId(dto.id)
        val isRoutine = routineMapper.isRoutineProject(dto)

        val entity = ProjectEntity(
            id = existing?.id ?: Uuid.random().toString(),
            serverId = dto.id,
            title = dto.title,
            color = if (dto.hexColor.isNotEmpty()) "#${dto.hexColor}" else "#808080",
            parentProjectId = null,
            position = dto.position,
            isFavorite = dto.isFavorite,
            isRoutine = isRoutine,
        )

        projectDao.upsert(entity)
    }

    suspend fun pullLabels() {
        val labels = api.getLabels(page = 1, perPage = 200)
        for (dto in labels) {
            val existing = labelDao.getByServerId(dto.id)
            val entity = LabelEntity(
                id = existing?.id ?: @OptIn(ExperimentalUuidApi::class) Uuid.random().toString(),
                serverId = dto.id,
                title = dto.title,
                color = if (dto.hexColor.isNotEmpty()) "#${dto.hexColor}" else "#4287f5",
            )
            labelDao.upsert(entity)
        }
    }

    suspend fun pushTask(entity: TaskEntity) {
        val projectServerId = entity.projectId?.let { projectDao.getById(it)?.serverId } ?: 1L
        val energyLevel = EnergyLevel.fromOrdinal(entity.energyLevel)

        val dto = taskMapper.domainToDto(
            title = entity.title,
            description = entity.description,
            done = entity.done,
            dueDate = null, // TODO: format ISO date from entity.dueDate
            priority = entity.priority,
            projectId = projectServerId,
            position = entity.position,
            estimatedMinutes = entity.estimatedMinutes,
            repeatAfterSeconds = entity.repeatAfterSeconds,
            hexColor = "",
        )

        if (entity.serverId != null) {
            // Update existing
            api.updateTask(entity.serverId, dto)
            // Update energy label
            val labelId = energyLabelManager.getLabelId(energyLevel)
            if (labelId != null) {
                api.attachLabel(entity.serverId, app.tsosu.data.vikunja.dto.VikunjaLabelTaskDto(labelId))
            }
        } else {
            // Create new
            val created = api.createTask(projectServerId, dto)
            taskDao.updateServerId(entity.id, created.id)
            // Attach energy label
            val labelId = energyLabelManager.getLabelId(energyLevel)
            if (labelId != null) {
                api.attachLabel(created.id, app.tsosu.data.vikunja.dto.VikunjaLabelTaskDto(labelId))
            }
        }
    }
}
```

3. Need to add `upsert` and `getByServerId` methods to existing DAOs. Add to `TaskDao.kt`:

```kotlin
// Add these methods:
@Upsert
suspend fun upsert(task: TaskEntity)

@Query("SELECT * FROM tasks WHERE serverId = :serverId LIMIT 1")
suspend fun getByServerId(serverId: Long): TaskEntity?

@Query("UPDATE tasks SET serverId = :serverId WHERE id = :id")
suspend fun updateServerId(id: String, serverId: Long)
```

Add to `ProjectDao.kt`:

```kotlin
@Upsert
suspend fun upsert(project: ProjectEntity)

@Query("SELECT * FROM projects WHERE serverId = :serverId LIMIT 1")
suspend fun getByServerId(serverId: Long): ProjectEntity?

@Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
suspend fun getById(id: String): ProjectEntity?
```

Add to `LabelDao.kt`:

```kotlin
@Upsert
suspend fun upsert(label: LabelEntity)

@Query("SELECT * FROM labels WHERE serverId = :serverId LIMIT 1")
suspend fun getByServerId(serverId: Long): LabelEntity?
```

4. Run tests: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-vikunja:test`

---

## Batch 4: SyncRepository Implementation + Hilt DI (3 tasks)

### Task 13: SyncRepositoryImpl

**Files:**
- Create: `data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/repository/SyncRepositoryImpl.kt`

**Steps:**

1. Create `SyncRepositoryImpl.kt`:

```kotlin
package app.tsosu.data.vikunja.repository

import app.tsosu.data.vikunja.api.VikunjaApi
import app.tsosu.data.vikunja.api.VikunjaApiProvider
import app.tsosu.data.vikunja.auth.VikunjaCredentialStore
import app.tsosu.data.vikunja.dto.VikunjaLoginRequest
import app.tsosu.data.vikunja.sync.EnergyLabelManager
import app.tsosu.data.vikunja.sync.SyncManager
import app.tsosu.domain.repository.ServerInfo
import app.tsosu.domain.repository.SyncRepository
import app.tsosu.domain.repository.SyncResult
import app.tsosu.domain.repository.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class SyncRepositoryImpl(
    private val credentialStore: VikunjaCredentialStore,
    private val syncManagerFactory: (VikunjaApi) -> SyncManager,
    private val energyLabelManagerFactory: (VikunjaApi) -> EnergyLabelManager,
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

    suspend fun login(url: String, username: String, password: String): Result<ServerInfo> {
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
        val api = currentApi ?: return Result.failure(IllegalStateException("Not configured"))

        _syncState.value = SyncState.SYNCING
        return try {
            // Initialize energy labels if needed
            val energyManager = energyLabelManagerFactory(api)
            energyManager.ensureLabelsExist()

            val syncManager = syncManagerFactory(api)

            // Pull first (server wins)
            val pulledProjects = syncManager.pullProjects()
            syncManager.pullLabels()
            val pulledTasks = syncManager.pullTasks()

            // TODO: Push local changes from sync queue

            _syncState.value = SyncState.IDLE
            Result.success(SyncResult(pushed = 0, pulled = pulledTasks + pulledProjects, conflicts = 0))
        } catch (e: Exception) {
            _syncState.value = SyncState.ERROR
            Result.failure(e)
        }
    }

    override fun isRemoteConfigured(): Flow<Boolean> = credentialStore.isConfigured()
}
```

2. Verify: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-vikunja:compileDebugKotlin`

### Task 14: Vikunja Hilt DI module

**Files:**
- Create: `data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/di/VikunjaModule.kt`
- Modify: `app/src/main/java/app/tsosu/di/RepositoryModule.kt` (bind SyncRepository)

**Steps:**

1. Create `VikunjaModule.kt`:

```kotlin
package app.tsosu.data.vikunja.di

import android.content.Context
import app.tsosu.data.local.dao.LabelDao
import app.tsosu.data.local.dao.ProjectDao
import app.tsosu.data.local.dao.TaskDao
import app.tsosu.data.vikunja.api.VikunjaApi
import app.tsosu.data.vikunja.auth.VikunjaCredentialStore
import app.tsosu.data.vikunja.mapper.VikunjaTaskMapper
import app.tsosu.data.vikunja.repository.SyncRepositoryImpl
import app.tsosu.data.vikunja.sync.EnergyLabelManager
import app.tsosu.data.vikunja.sync.SyncManager
import app.tsosu.domain.repository.SyncRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VikunjaModule {

    @Provides
    @Singleton
    fun provideCredentialStore(@ApplicationContext context: Context): VikunjaCredentialStore {
        return VikunjaCredentialStore(context)
    }

    @Provides
    @Singleton
    fun provideTaskMapper(): VikunjaTaskMapper = VikunjaTaskMapper()

    @Provides
    @Singleton
    fun provideSyncRepository(
        credentialStore: VikunjaCredentialStore,
        taskDao: TaskDao,
        projectDao: ProjectDao,
        labelDao: LabelDao,
        taskMapper: VikunjaTaskMapper,
    ): SyncRepository {
        return SyncRepositoryImpl(
            credentialStore = credentialStore,
            syncManagerFactory = { api ->
                SyncManager(api, taskDao, projectDao, labelDao, EnergyLabelManager(api), taskMapper)
            },
            energyLabelManagerFactory = { api -> EnergyLabelManager(api) },
        )
    }
}
```

2. Update `RepositoryModule.kt` — remove the old SyncRepository stub binding if it exists, since VikunjaModule now provides it directly. If there's no existing binding, no change needed.

3. Verify: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug`

### Task 15: Settings screen — Vikunja server configuration UI

**Files:**
- Create: `app/src/main/java/app/tsosu/ui/screens/settings/SettingsViewModel.kt`
- Create: `app/src/main/java/app/tsosu/ui/screens/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/app/tsosu/navigation/Screen.kt` (add Settings)
- Modify: `app/src/main/java/app/tsosu/navigation/TsosuNavHost.kt` (add route)

**Steps:**

1. Create `SettingsViewModel.kt`:

```kotlin
package app.tsosu.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.data.vikunja.repository.SyncRepositoryImpl
import app.tsosu.domain.repository.SyncRepository
import app.tsosu.domain.repository.SyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val isConnected: Boolean = false,
    val syncState: SyncState = SyncState.IDLE,
    val message: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            syncRepository.isRemoteConfigured().collect { configured ->
                _uiState.value = _uiState.value.copy(isConnected = configured)
            }
        }
        viewModelScope.launch {
            syncRepository.syncState().collect { state ->
                _uiState.value = _uiState.value.copy(syncState = state)
            }
        }
    }

    fun updateServerUrl(url: String) {
        _uiState.value = _uiState.value.copy(serverUrl = url)
    }

    fun updateUsername(username: String) {
        _uiState.value = _uiState.value.copy(username = username)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun connect() {
        viewModelScope.launch {
            val state = _uiState.value
            val repo = syncRepository as? SyncRepositoryImpl ?: return@launch
            val result = repo.login(state.serverUrl, state.username, state.password)
            result.fold(
                onSuccess = { info ->
                    _uiState.value = _uiState.value.copy(
                        isConnected = true,
                        message = "Connected to ${info.version}",
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        message = "Error: ${e.message}",
                    )
                },
            )
        }
    }

    fun sync() {
        viewModelScope.launch {
            val result = syncRepository.sync()
            result.fold(
                onSuccess = { r ->
                    _uiState.value = _uiState.value.copy(
                        message = "Synced: ${r.pulled} pulled, ${r.pushed} pushed",
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        message = "Sync error: ${e.message}",
                    )
                },
            )
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            syncRepository.disconnect()
            _uiState.value = SettingsUiState()
        }
    }
}
```

2. Create `SettingsScreen.kt`:

```kotlin
package app.tsosu.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tsosu.domain.repository.SyncState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        Text("Vikunja Server", style = MaterialTheme.typography.titleMedium)

        if (state.isConnected) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Connected", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.sync() },
                            enabled = state.syncState != SyncState.SYNCING,
                        ) {
                            Text(if (state.syncState == SyncState.SYNCING) "Syncing..." else "Sync Now")
                        }
                        OutlinedButton(onClick = { viewModel.disconnect() }) {
                            Text("Disconnect")
                        }
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = state.serverUrl,
                onValueChange = { viewModel.updateServerUrl(it) },
                label = { Text("Server URL") },
                placeholder = { Text("http://localhost:3456") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.username,
                onValueChange = { viewModel.updateUsername(it) },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.password,
                onValueChange = { viewModel.updatePassword(it) },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
            Button(
                onClick = { viewModel.connect() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Connect")
            }
        }

        state.message?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
```

3. Add Settings to `Screen.kt` and navigation. Add gear icon to top bar.

4. Verify: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug`

---

## Batch 5: CalDAV Calendar Sync (3 tasks)

### Task 16: CalDAV credential storage

**Files:**
- Create: `data-calendar/src/main/kotlin/app/tsosu/data/calendar/CalDavCredentialStore.kt`

**Steps:**

1. Create `CalDavCredentialStore.kt`:

```kotlin
package app.tsosu.data.calendar

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.caldavDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "caldav_credentials"
)

class CalDavCredentialStore(private val context: Context) {

    companion object {
        private val KEY_SERVER_URL = stringPreferencesKey("caldav_server_url")
        private val KEY_CALENDAR_URL = stringPreferencesKey("caldav_calendar_url")
        private val KEY_EMAIL = stringPreferencesKey("caldav_email")
        private val KEY_PASSWORD = stringPreferencesKey("caldav_password")
    }

    val calendarUrl: Flow<String?> = context.caldavDataStore.data.map { it[KEY_CALENDAR_URL] }

    fun isConfigured(): Flow<Boolean> = context.caldavDataStore.data.map {
        it[KEY_CALENDAR_URL] != null && it[KEY_EMAIL] != null && it[KEY_PASSWORD] != null
    }

    suspend fun save(serverUrl: String, calendarUrl: String, email: String, password: String) {
        context.caldavDataStore.edit { prefs ->
            prefs[KEY_SERVER_URL] = serverUrl
            prefs[KEY_CALENDAR_URL] = calendarUrl
            prefs[KEY_EMAIL] = email
            prefs[KEY_PASSWORD] = password
        }
    }

    suspend fun getCredentials(): CalDavCredentials? {
        var result: CalDavCredentials? = null
        context.caldavDataStore.data.collect { prefs ->
            val url = prefs[KEY_CALENDAR_URL]
            val email = prefs[KEY_EMAIL]
            val password = prefs[KEY_PASSWORD]
            result = if (url != null && email != null && password != null) {
                CalDavCredentials(url, email, password)
            } else null
            return@collect
        }
        return result
    }

    suspend fun clear() {
        context.caldavDataStore.edit { it.clear() }
    }
}

data class CalDavCredentials(
    val calendarUrl: String,
    val email: String,
    val password: String,
)
```

2. Verify: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-calendar:compileDebugKotlin`

### Task 17: CalDAV VEVENT builder (TDD)

**Files:**
- Create: `data-calendar/src/test/kotlin/app/tsosu/data/calendar/VEventBuilderTest.kt`
- Create: `data-calendar/src/main/kotlin/app/tsosu/data/calendar/VEventBuilder.kt`

**Steps:**

1. Create `VEventBuilderTest.kt`:

```kotlin
package app.tsosu.data.calendar

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class VEventBuilderTest {

    private val builder = VEventBuilder()

    @Test
    fun `builds VEVENT with title and due date`() {
        val ical = builder.buildVEvent(
            uid = "task-uuid-1",
            title = "Buy groceries",
            description = "Get milk and eggs",
            dueDate = "2026-03-10T09:00:00",
            estimatedMinutes = null,
        )
        assertTrue(ical.contains("BEGIN:VCALENDAR"))
        assertTrue(ical.contains("BEGIN:VEVENT"))
        assertTrue(ical.contains("SUMMARY:Buy groceries"))
        assertTrue(ical.contains("UID:task-uuid-1"))
        assertTrue(ical.contains("DTSTART"))
        assertTrue(ical.contains("END:VEVENT"))
        assertTrue(ical.contains("END:VCALENDAR"))
    }

    @Test
    fun `uses estimatedMinutes for DTEND duration`() {
        val ical = builder.buildVEvent(
            uid = "task-uuid-2",
            title = "Meeting",
            description = "",
            dueDate = "2026-03-10T14:00:00",
            estimatedMinutes = 30,
        )
        assertTrue(ical.contains("DURATION:PT30M"))
    }

    @Test
    fun `defaults to 1 hour when no estimate`() {
        val ical = builder.buildVEvent(
            uid = "task-uuid-3",
            title = "Task",
            description = "",
            dueDate = "2026-03-10T10:00:00",
            estimatedMinutes = null,
        )
        assertTrue(ical.contains("DURATION:PT60M"))
    }

    @Test
    fun `escapes special characters in title`() {
        val ical = builder.buildVEvent(
            uid = "task-uuid-4",
            title = "Meeting, with; semicolons",
            description = "",
            dueDate = "2026-03-10T10:00:00",
            estimatedMinutes = null,
        )
        assertTrue(ical.contains("SUMMARY:Meeting\\, with\\; semicolons"))
    }
}
```

2. Create `VEventBuilder.kt`:

```kotlin
package app.tsosu.data.calendar

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class VEventBuilder {

    private val icalDateFormat = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")

    fun buildVEvent(
        uid: String,
        title: String,
        description: String,
        dueDate: String,
        estimatedMinutes: Int?,
    ): String {
        val dt = LocalDateTime.parse(dueDate)
        val dtStart = dt.format(icalDateFormat)
        val duration = estimatedMinutes ?: 60

        return buildString {
            appendLine("BEGIN:VCALENDAR")
            appendLine("VERSION:2.0")
            appendLine("PRODID:-//Tsosu//NONSGML v1//EN")
            appendLine("BEGIN:VEVENT")
            appendLine("UID:$uid")
            appendLine("DTSTART:$dtStart")
            appendLine("DURATION:PT${duration}M")
            appendLine("SUMMARY:${escapeIcal(title)}")
            if (description.isNotEmpty()) {
                appendLine("DESCRIPTION:${escapeIcal(description)}")
            }
            appendLine("END:VEVENT")
            appendLine("END:VCALENDAR")
        }
    }

    private fun escapeIcal(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace(",", "\\,")
            .replace(";", "\\;")
            .replace("\n", "\\n")
    }
}
```

3. Run tests: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-calendar:test`

### Task 18: CalendarRepositoryImpl — PUT VEVENT to Fastmail

**Files:**
- Create: `data-calendar/src/main/kotlin/app/tsosu/data/calendar/CalendarRepositoryImpl.kt`
- Create: `data-calendar/src/main/kotlin/app/tsosu/data/calendar/di/CalendarModule.kt`

**Steps:**

1. Create `CalendarRepositoryImpl.kt`:

```kotlin
package app.tsosu.data.calendar

import app.tsosu.domain.model.Task
import app.tsosu.domain.repository.CalendarInfo
import app.tsosu.domain.repository.CalendarRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class CalendarRepositoryImpl(
    private val credentialStore: CalDavCredentialStore,
    private val vEventBuilder: VEventBuilder = VEventBuilder(),
) : CalendarRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun isConfigured(): Flow<Boolean> = credentialStore.isConfigured()

    override suspend fun configureCaldav(
        serverUrl: String,
        email: String,
        password: String,
    ): Result<Unit> {
        return try {
            // Test connection with PROPFIND
            val request = Request.Builder()
                .url(serverUrl)
                .method("PROPFIND", "".toRequestBody("application/xml".toMediaType()))
                .header("Authorization", Credentials.basic(email, password))
                .header("Depth", "0")
                .build()

            val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
            if (response.isSuccessful || response.code == 207) {
                credentialStore.save(
                    serverUrl = "https://caldav.fastmail.com",
                    calendarUrl = serverUrl,
                    email = email,
                    password = password,
                )
                Result.success(Unit)
            } else {
                Result.failure(Exception("CalDAV error: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun configureGoogle(oauthToken: String): Result<Unit> {
        // Deferred to Phase 3
        return Result.failure(UnsupportedOperationException("Google Calendar not yet implemented"))
    }

    override suspend fun disconnect() {
        credentialStore.clear()
    }

    override suspend fun listCalendars(): Result<List<CalendarInfo>> {
        // For now, return the single configured calendar
        return Result.success(listOf(CalendarInfo("default", "Tsosu Calendar")))
    }

    override suspend fun setDefaultCalendar(calendarId: String) {
        // Single calendar for now
    }

    override suspend fun syncTaskToCalendar(task: Task): Result<String> {
        val creds = credentialStore.getCredentials()
            ?: return Result.failure(IllegalStateException("CalDAV not configured"))

        val dueDate = task.dueDate ?: return Result.failure(IllegalArgumentException("Task has no due date"))

        val uid = "tsosu-${task.id}"
        val ical = vEventBuilder.buildVEvent(
            uid = uid,
            title = task.title,
            description = task.description,
            dueDate = dueDate.toString(),
            estimatedMinutes = task.estimatedMinutes,
        )

        return putEvent(creds, uid, ical)
    }

    override suspend fun updateCalendarEvent(task: Task): Result<Unit> {
        val eventId = task.calendarEventId ?: return Result.failure(IllegalArgumentException("No calendar event ID"))
        syncTaskToCalendar(task)
        return Result.success(Unit)
    }

    override suspend fun removeCalendarEvent(eventId: String): Result<Unit> {
        val creds = credentialStore.getCredentials()
            ?: return Result.failure(IllegalStateException("CalDAV not configured"))

        return try {
            val url = "${creds.calendarUrl.trimEnd('/')}/$eventId.ics"
            val request = Request.Builder()
                .url(url)
                .delete()
                .header("Authorization", Credentials.basic(creds.email, creds.password))
                .build()

            withContext(Dispatchers.IO) { client.newCall(request).execute() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun putEvent(creds: CalDavCredentials, uid: String, ical: String): Result<String> {
        return try {
            val url = "${creds.calendarUrl.trimEnd('/')}/$uid.ics"
            val body = ical.toRequestBody("text/calendar; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .put(body)
                .header("Authorization", Credentials.basic(creds.email, creds.password))
                .build()

            val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
            if (response.isSuccessful || response.code == 201 || response.code == 204) {
                Result.success(uid)
            } else {
                Result.failure(Exception("CalDAV PUT failed: ${response.code} ${response.body?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

2. Create `CalendarModule.kt`:

```kotlin
package app.tsosu.data.calendar.di

import android.content.Context
import app.tsosu.data.calendar.CalDavCredentialStore
import app.tsosu.data.calendar.CalendarRepositoryImpl
import app.tsosu.domain.repository.CalendarRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CalendarModule {

    @Provides
    @Singleton
    fun provideCalDavCredentialStore(@ApplicationContext context: Context): CalDavCredentialStore {
        return CalDavCredentialStore(context)
    }

    @Provides
    @Singleton
    fun provideCalendarRepository(credentialStore: CalDavCredentialStore): CalendarRepository {
        return CalendarRepositoryImpl(credentialStore)
    }
}
```

3. Verify: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug`

---

## Batch 6: Gentle Nudge + Stale Cleanup + Weekly Review UI (3 tasks)

### Task 19: Gentle Nudge notification manager

**Files:**
- Create: `app/src/main/java/app/tsosu/notification/GentleNudgeManager.kt`
- Modify: `app/src/main/AndroidManifest.xml` (notification permission + WorkManager init)

**Steps:**

1. Create `GentleNudgeManager.kt`:

```kotlin
package app.tsosu.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.tsosu.R

class GentleNudgeManager(private val context: Context) {

    companion object {
        const val CHANNEL_MORNING = "tsosu_morning"
        const val CHANNEL_FOCUS = "tsosu_focus"
        const val NOTIFICATION_MORNING = 1001
        const val NOTIFICATION_FOCUS_COMPLETE = 1002
    }

    fun createChannels() {
        val morningChannel = NotificationChannel(
            CHANNEL_MORNING,
            context.getString(R.string.notif_channel_morning),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        val focusChannel = NotificationChannel(
            CHANNEL_FOCUS,
            context.getString(R.string.notif_channel_focus),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(morningChannel)
        manager.createNotificationChannel(focusChannel)
    }

    fun showMorningRoutine() {
        if (!hasPermission()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_MORNING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentText(context.getString(R.string.notif_morning_routine))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_MORNING, notification)
    }

    fun showFocusComplete() {
        if (!hasPermission()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_FOCUS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentText(context.getString(R.string.notif_focus_complete))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_FOCUS_COMPLETE, notification)
    }

    private fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }
}
```

2. Add notification channel strings to `values/strings.xml`:

```xml
<!-- Notification Channels -->
<string name="notif_channel_morning">Morning Routine</string>
<string name="notif_channel_focus">Focus Updates</string>
```

And `values-zh-rTW/strings.xml`:

```xml
<string name="notif_channel_morning">晨間習慣</string>
<string name="notif_channel_focus">專注更新</string>
```

3. Add notification permission to `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

4. Verify: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug`

### Task 20: Stale task cleanup use case integration

**Files:**
- Modify: `app/src/main/java/app/tsosu/ui/screens/settings/SettingsScreen.kt` (add cleanup button)
- Modify: `app/src/main/java/app/tsosu/ui/screens/settings/SettingsViewModel.kt` (add cleanup action)

**Steps:**

1. Add to `SettingsViewModel.kt`:

```kotlin
// Add to SettingsUiState:
val staleTasks: Int = 0

// Add function:
fun cleanupStaleTasks() {
    viewModelScope.launch {
        getStaleTaskIdsUseCase().collect { staleIds ->
            if (staleIds.isNotEmpty()) {
                taskRepository.archiveTasks(staleIds)
                _uiState.value = _uiState.value.copy(
                    message = "Archived ${staleIds.size} stale tasks",
                    staleTasks = 0,
                )
            }
        }
    }
}
```

2. Add a "Clean up stale tasks" section to SettingsScreen with a button and count.

3. Verify: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug`

### Task 21: Weekly Review screen

**Files:**
- Create: `app/src/main/java/app/tsosu/ui/screens/weeklyreview/WeeklyReviewViewModel.kt`
- Create: `app/src/main/java/app/tsosu/ui/screens/weeklyreview/WeeklyReviewScreen.kt`

**Steps:**

1. Create `WeeklyReviewViewModel.kt`:

```kotlin
package app.tsosu.ui.screens.weeklyreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.domain.model.WeeklyReview
import app.tsosu.domain.usecase.GetWeeklyReviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class WeeklyReviewViewModel @Inject constructor(
    getWeeklyReviewUseCase: GetWeeklyReviewUseCase,
) : ViewModel() {

    val review: StateFlow<WeeklyReview?> = getWeeklyReviewUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
```

2. Create `WeeklyReviewScreen.kt`:

```kotlin
package app.tsosu.ui.screens.weeklyreview

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun WeeklyReviewScreen(viewModel: WeeklyReviewViewModel = hiltViewModel()) {
    val review by viewModel.review.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Weekly Review", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        review?.let { r ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatRow("Tasks completed", "${r.tasksCompleted}")
                    StatRow("Habits completed", "${r.habitsCompletedTotal}")
                    StatRow("Focus days", "${r.focusDaysCompleted}")
                    StatRow("Time invested", "${r.totalEstimatedMinutes} min")
                    r.topProject?.let { StatRow("Top project", it) }
                    r.longestHabitStreak?.let {
                        StatRow("Best streak", "${it.habitTitle}: ${it.currentConsecutiveDays} days")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Great work this week! Every step counts.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        } ?: Text("No data yet for this week.")
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
```

3. Add route to navigation.

4. Verify: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug`

---

## Batch 7: Todoist Import + UI Polish + i18n (3 tasks)

### Task 22: Todoist CSV import

**Files:**
- Create: `data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/importer/TodoistImporter.kt`
- Create: `data-vikunja/src/test/kotlin/app/tsosu/data/vikunja/importer/TodoistImporterTest.kt`

**Steps:**

1. Create `TodoistImporterTest.kt`:

```kotlin
package app.tsosu.data.vikunja.importer

import app.tsosu.domain.repository.ImportFormat
import kotlin.test.Test
import kotlin.test.assertEquals

class TodoistImporterTest {

    private val importer = TodoistImporter()

    @Test
    fun `parses CSV with basic task fields`() {
        val csv = """
            TYPE,CONTENT,DESCRIPTION,PRIORITY,INDENT,AUTHOR,RESPONSIBLE,DATE,DATE_LANG,TIMEZONE
            task,Buy milk,,4,1,,,2026-03-10,,
            task,Write report,Draft for Monday,2,1,,,2026-03-12,,
        """.trimIndent()

        val result = importer.parse(csv.toByteArray(), ImportFormat.TODOIST_CSV)
        assertEquals(2, result.tasks.size)
        assertEquals("Buy milk", result.tasks[0].title)
        assertEquals("Write report", result.tasks[1].title)
        assertEquals("Draft for Monday", result.tasks[1].description)
    }

    @Test
    fun `maps Todoist priority to Tsosu priority`() {
        val csv = """
            TYPE,CONTENT,DESCRIPTION,PRIORITY,INDENT,AUTHOR,RESPONSIBLE,DATE,DATE_LANG,TIMEZONE
            task,Urgent task,,1,1,,,,,
            task,Normal task,,4,1,,,,,
        """.trimIndent()

        val result = importer.parse(csv.toByteArray(), ImportFormat.TODOIST_CSV)
        // Todoist priority 1 = highest (p1), maps to Tsosu URGENT (4)
        assertEquals(4, result.tasks[0].priority)
        // Todoist priority 4 = lowest (p4), maps to Tsosu NONE (0)
        assertEquals(0, result.tasks[1].priority)
    }
}
```

2. Create `TodoistImporter.kt`:

```kotlin
package app.tsosu.data.vikunja.importer

import app.tsosu.domain.repository.ImportFormat

data class ImportedTask(
    val title: String,
    val description: String = "",
    val priority: Int = 0,
    val dueDate: String? = null,
    val projectName: String? = null,
)

data class ParseResult(
    val tasks: List<ImportedTask>,
    val projectNames: Set<String> = emptySet(),
)

class TodoistImporter {

    fun parse(data: ByteArray, format: ImportFormat): ParseResult {
        return when (format) {
            ImportFormat.TODOIST_CSV -> parseCsv(data.decodeToString())
            ImportFormat.TODOIST_JSON -> ParseResult(emptyList()) // TODO
        }
    }

    private fun parseCsv(csv: String): ParseResult {
        val lines = csv.lines().filter { it.isNotBlank() }
        if (lines.size < 2) return ParseResult(emptyList())

        val headers = lines[0].split(",").map { it.trim() }
        val typeIdx = headers.indexOf("TYPE")
        val contentIdx = headers.indexOf("CONTENT")
        val descIdx = headers.indexOf("DESCRIPTION")
        val priorityIdx = headers.indexOf("PRIORITY")
        val dateIdx = headers.indexOf("DATE")

        val tasks = mutableListOf<ImportedTask>()

        for (line in lines.drop(1)) {
            val cols = parseCsvLine(line)
            if (cols.getOrNull(typeIdx) != "task") continue

            tasks.add(
                ImportedTask(
                    title = cols.getOrElse(contentIdx) { "" },
                    description = cols.getOrElse(descIdx) { "" },
                    priority = mapTodoistPriority(cols.getOrNull(priorityIdx)?.toIntOrNull() ?: 4),
                    dueDate = cols.getOrNull(dateIdx)?.takeIf { it.isNotBlank() },
                )
            )
        }

        return ParseResult(tasks)
    }

    private fun parseCsvLine(line: String): List<String> {
        // Simple CSV parser (no quoted fields with commas for now)
        return line.split(",").map { it.trim() }
    }

    // Todoist: 1=highest (p1), 4=lowest (p4)
    // Tsosu: 0=NONE, 1=LOW, 2=MEDIUM, 3=HIGH, 4=URGENT
    private fun mapTodoistPriority(todoistPriority: Int): Int {
        return when (todoistPriority) {
            1 -> 4 // p1 -> URGENT
            2 -> 3 // p2 -> HIGH
            3 -> 2 // p3 -> MEDIUM
            else -> 0 // p4 -> NONE
        }
    }
}
```

3. Run tests: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-vikunja:test`

### Task 23: Sync status indicators + new i18n strings

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rTW/strings.xml`

**Steps:**

1. Add Phase 2 strings to English:

```xml
<!-- Settings -->
<string name="settings_title">Settings</string>
<string name="settings_vikunja_server">Vikunja Server</string>
<string name="settings_connected">Connected</string>
<string name="settings_sync_now">Sync Now</string>
<string name="settings_syncing">Syncing...</string>
<string name="settings_disconnect">Disconnect</string>
<string name="settings_server_url">Server URL</string>
<string name="settings_username">Username</string>
<string name="settings_password">Password</string>
<string name="settings_connect">Connect</string>
<string name="settings_caldav">Calendar (CalDAV)</string>
<string name="settings_caldav_email">Email</string>
<string name="settings_caldav_password">App Password</string>
<string name="settings_caldav_connect">Connect Calendar</string>
<string name="settings_cleanup">Stale Task Cleanup</string>
<string name="settings_cleanup_desc">Archive tasks untouched for 14+ days</string>
<string name="settings_cleanup_btn">Clean Up</string>
<string name="settings_cleanup_result">Archived %d stale tasks</string>

<!-- Weekly Review -->
<string name="review_title">Weekly Review</string>
<string name="review_tasks_completed">Tasks completed</string>
<string name="review_habits_completed">Habits completed</string>
<string name="review_focus_days">Focus days</string>
<string name="review_time_invested">Time invested</string>
<string name="review_top_project">Top project</string>
<string name="review_best_streak">Best streak</string>
<string name="review_celebration">Great work this week! Every step counts.</string>
<string name="review_no_data">No data yet for this week.</string>

<!-- Sync -->
<string name="sync_pulled">Synced: %1$d pulled, %2$d pushed</string>
<string name="sync_error">Sync error: %s</string>

<!-- Notification Channels -->
<string name="notif_channel_morning">Morning Routine</string>
<string name="notif_channel_focus">Focus Updates</string>
```

2. Add Phase 2 strings to Traditional Chinese:

```xml
<!-- Settings -->
<string name="settings_title">設定</string>
<string name="settings_vikunja_server">Vikunja 伺服器</string>
<string name="settings_connected">已連線</string>
<string name="settings_sync_now">立即同步</string>
<string name="settings_syncing">同步中...</string>
<string name="settings_disconnect">中斷連線</string>
<string name="settings_server_url">伺服器網址</string>
<string name="settings_username">使用者名稱</string>
<string name="settings_password">密碼</string>
<string name="settings_connect">連線</string>
<string name="settings_caldav">行事曆 (CalDAV)</string>
<string name="settings_caldav_email">電子郵件</string>
<string name="settings_caldav_password">應用程式密碼</string>
<string name="settings_caldav_connect">連接行事曆</string>
<string name="settings_cleanup">過期任務清理</string>
<string name="settings_cleanup_desc">封存超過 14 天未動的任務</string>
<string name="settings_cleanup_btn">清理</string>
<string name="settings_cleanup_result">已封存 %d 個過期任務</string>

<!-- Weekly Review -->
<string name="review_title">每週回顧</string>
<string name="review_tasks_completed">完成任務</string>
<string name="review_habits_completed">完成習慣</string>
<string name="review_focus_days">專注天數</string>
<string name="review_time_invested">投入時間</string>
<string name="review_top_project">最多專案</string>
<string name="review_best_streak">最佳連續</string>
<string name="review_celebration">這週做得很好！每一步都算數。</string>
<string name="review_no_data">這週還沒有資料。</string>

<!-- Sync -->
<string name="sync_pulled">同步完成：拉取 %1$d，推送 %2$d</string>
<string name="sync_error">同步錯誤：%s</string>

<!-- Notification Channels -->
<string name="notif_channel_morning">晨間習慣</string>
<string name="notif_channel_focus">專注更新</string>
```

3. Verify: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug`

### Task 24: Final build verification + domain tests

**Steps:**

1. Run all domain tests: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :domain:test`
2. Run data-vikunja tests: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-vikunja:test`
3. Run data-calendar tests: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-calendar:test`
4. Run full build: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug`
5. Verify all pass, fix any issues.

---

## Summary

| Batch | Tasks | Description |
|-------|-------|-------------|
| 0 | 1-3 | Module scaffolding + dependencies |
| 1 | 4-6 | Vikunja DTOs + Retrofit API + Auth |
| 2 | 7-9 | Task/Habit/Routine mappers (TDD) |
| 3 | 10-12 | Credentials + Energy labels + SyncManager |
| 4 | 13-15 | SyncRepository + DI + Settings UI |
| 5 | 16-18 | CalDAV calendar sync |
| 6 | 19-21 | Nudge + Stale cleanup + Weekly Review |
| 7 | 22-24 | Todoist import + i18n + final verification |

**Total: 24 tasks across 8 batches**
