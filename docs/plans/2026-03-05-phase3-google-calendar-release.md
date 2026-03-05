# Phase 3: Google Calendar + Play Store Release

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add Google Calendar as alternative calendar provider and prepare app for Google Play Store release.

**Architecture:** Extend data-calendar module with GoogleCalendarProvider alongside existing CalDAV. Add Google Sign-In for OAuth2. Prepare release build config, ProGuard rules, and Play Store metadata.

**Tech Stack:** Google API Client for Java, Google Sign-In (Credential Manager), Google Calendar API v3, R8/ProGuard

---

### Task 1: Add Google Calendar Dependencies

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `data-calendar/build.gradle.kts`

**Step 1: Add versions and libraries to catalog**

Add to `gradle/libs.versions.toml`:
- `google-api-client = "2.7.1"` (Google API Client for Android)
- `google-calendar-api = "v3-rev20241101-2.0.0"` (Calendar API)
- `google-auth = "1.30.1"` (Google Auth Library)
- `credential = "1.5.0-alpha05"` (Credential Manager)
- `googleid = "1.1.1"` (Google ID)
- Libraries: google-api-client-android, google-api-services-calendar, google-auth-oauth2, credentials, credentials-play-services-auth, googleid

**Step 2: Update data-calendar/build.gradle.kts**

Add new dependencies for Google Calendar API.

**Step 3: Verify build**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-calendar:compileDebugKotlin`

---

### Task 2: Google Calendar Credential Store

**Files:**
- Create: `data-calendar/src/main/kotlin/app/tsosu/data/calendar/google/GoogleCredentialStore.kt`

**Step 1: Implement credential store**

DataStore-based storage for Google OAuth2 tokens (access_token, refresh_token, account_email). Provides `isConfigured(): Flow<Boolean>`, `save()`, `getCredentials()`, `clear()`.

**Step 2: Verify build**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-calendar:compileDebugKotlin`

---

### Task 3: Write GoogleCalendarProvider Tests

**Files:**
- Create: `data-calendar/src/test/kotlin/app/tsosu/data/calendar/google/GoogleCalendarProviderTest.kt`

**Step 1: Write tests**

Tests for:
- `creates event from task with due date and duration`
- `updates existing event`
- `deletes event by ID`
- `lists user calendars`
- `returns failure when not configured`

**Step 2: Run tests to verify they fail**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-calendar:test`
Expected: FAIL (classes not implemented)

---

### Task 4: Implement GoogleCalendarProvider

**Files:**
- Create: `data-calendar/src/main/kotlin/app/tsosu/data/calendar/google/GoogleCalendarProvider.kt`

**Step 1: Implement provider**

Uses Google Calendar API v3 (com.google.api.services.calendar). Methods:
- `createEvent(task)` - Creates Event with summary, description, start/end (from dueDate + estimatedMinutes), eventId="tsosu-{task.id}"
- `updateEvent(task)` - Updates by eventId
- `deleteEvent(eventId)` - Deletes by eventId
- `listCalendars()` - Returns user's calendar list
- Uses GoogleCredentialStore for auth

**Step 2: Run tests**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-calendar:test`
Expected: PASS

---

### Task 5: Refactor CalendarRepository for Multi-Provider

**Files:**
- Modify: `data-calendar/src/main/kotlin/app/tsosu/data/calendar/CalendarRepositoryImpl.kt`
- Modify: `data-calendar/src/main/kotlin/app/tsosu/data/calendar/di/CalendarModule.kt`
- Modify: `domain/src/main/kotlin/app/tsosu/domain/repository/CalendarRepository.kt`

**Step 1: Add CalendarProvider enum to domain**

```kotlin
enum class CalendarProvider { CALDAV, GOOGLE, NONE }
```

Add `fun activeProvider(): Flow<CalendarProvider>` to CalendarRepository.

**Step 2: Refactor CalendarRepositoryImpl**

- Accept both CalDavCredentialStore and GoogleCredentialStore
- Route operations based on which provider is configured
- `configureGoogle()` now delegates to GoogleCalendarProvider
- `activeProvider()` checks which store is configured

**Step 3: Update CalendarModule DI**

Provide GoogleCredentialStore and inject both into CalendarRepositoryImpl.

**Step 4: Verify build**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug`

---

### Task 6: Google Sign-In UI in Settings

**Files:**
- Modify: `app/src/main/java/app/tsosu/ui/screens/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/app/tsosu/ui/screens/settings/SettingsViewModel.kt`

**Step 1: Add calendar section to Settings**

Add "Calendar" section with:
- Show active provider (CalDAV / Google / None)
- "Connect Google Calendar" button (launches Credential Manager flow)
- "Connect CalDAV" expandable (existing fields)
- "Disconnect Calendar" when connected

**Step 2: Implement Google Sign-In flow in ViewModel**

Use Credential Manager API with Google ID token + Calendar scope.
Save tokens to GoogleCredentialStore on success.

**Step 3: Verify build**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug`

---

### Task 7: Strings for Calendar Settings (en + zh-TW)

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rTW/strings.xml`

**Step 1: Add new strings**

English:
- `settings_calendar` = "Calendar"
- `settings_calendar_provider` = "Provider: %s"
- `settings_google_connect` = "Connect Google Calendar"
- `settings_google_connected` = "Google Calendar connected"
- `settings_caldav_title` = "CalDAV (Fastmail, etc.)"
- `settings_calendar_disconnect` = "Disconnect Calendar"
- `settings_calendar_none` = "No calendar connected"

zh-TW translations.

---

### Task 8: ProGuard/R8 Rules

**Files:**
- Modify: `app/proguard-rules.pro`

**Step 1: Add rules for all libraries**

Rules for:
- Retrofit (keep annotations, interfaces)
- OkHttp (platform classes)
- Kotlin Serialization (@Serializable classes)
- Google API Client (reflection-based)
- Google Calendar API models
- Hilt (generated code)
- kotlinx-datetime
- Room entities

**Step 2: Test release build**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleRelease`

---

### Task 9: Release Build Configuration

**Files:**
- Modify: `app/build.gradle.kts`

**Step 1: Version bump**

Change versionCode to 1, versionName to "1.0.0".

**Step 2: Add signing config placeholder**

Add `signingConfigs` block reading from `keystore.properties` (gitignored).
Configure release buildType to use signingConfig.
Add `isShrinkResources = true` to release.

**Step 3: Create keystore.properties.example**

Template file showing required properties (storeFile, storePassword, keyAlias, keyPassword).

**Step 4: Verify release build compiles**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleRelease`

---

### Task 10: Play Store Metadata

**Files:**
- Create: `fastlane/metadata/android/en-US/full_description.txt`
- Create: `fastlane/metadata/android/en-US/short_description.txt`
- Create: `fastlane/metadata/android/en-US/title.txt`
- Create: `fastlane/metadata/android/zh-TW/full_description.txt`
- Create: `fastlane/metadata/android/zh-TW/short_description.txt`
- Create: `fastlane/metadata/android/zh-TW/title.txt`
- Create: `PRIVACY_POLICY.md`

**Step 1: Create store listing text**

Title: "Tsosu - ADHD Task Manager"
Short: "Designed by a psychiatrist with ADHD. Built for minds that work differently."
Full: Feature list highlighting ADHD-friendly design, Focus 3, habits, energy levels, Pick One, Vikunja sync, calendar sync.

**Step 2: Create privacy policy**

Standard privacy policy covering:
- Data collected (tasks, habits stored locally)
- Optional sync (Vikunja self-hosted, CalDAV, Google Calendar)
- No analytics, no ads
- Data deletion instructions

---

### Task 11: Final Build Verification

**Step 1: Run all tests**

```bash
ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :domain:test :data-calendar:test :data-vikunja:test
```

**Step 2: Full debug build**

```bash
ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug
```

**Step 3: Release build (unsigned)**

```bash
ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleRelease
```
