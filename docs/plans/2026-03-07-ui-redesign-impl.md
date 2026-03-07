# UI Redesign Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Full Material 3 UI redesign with warm orange theme, dopamine feedback (konfetti, haptics, animated cards), consolidated 3-tab navigation, and overview dashboard.

**Architecture:** Layered approach — theme first, then shared components, then screen-by-screen updates. No domain/data changes needed; all changes are in `app/` module. New dependency: konfetti-compose for celebration animations.

**Tech Stack:** Jetpack Compose + Material 3, Hilt DI, konfetti-compose, DataStore for theme preferences

---

## Batch 1: Foundation (Theme + Dependencies)

### Task 1: Add konfetti-compose dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

**Step 1: Add version and library entry to version catalog**

In `gradle/libs.versions.toml`, add after line 22 (`ical4j = "4.0.7"`):

```toml
konfetti = "2.0.4"
```

In the `[libraries]` section, add after `glance-material3` (line 66):

```toml
konfetti-compose = { group = "nl.dionsegijn", name = "konfetti-compose", version.ref = "konfetti" }
```

**Step 2: Add dependency to app module**

In `app/build.gradle.kts`, add after line 111 (`implementation(libs.kotlinx.coroutines.android)`):

```kotlin
implementation(libs.konfetti.compose)
```

**Step 3: Verify build compiles**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "chore: add konfetti-compose dependency for celebration animations"
```

---

### Task 2: Create TsosuTheme with warm orange brand color

**Files:**
- Create: `app/src/main/java/app/tsosu/ui/theme/TsosuTheme.kt`

**Step 1: Create the theme composable**

```kotlin
package app.tsosu.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val BrandOrange = Color(0xFFFF7043)
private val BrandOrangeDark = Color(0xFFFFAB91)

private val LightColorScheme = lightColorScheme(
    primary = BrandOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBD0),
    onPrimaryContainer = Color(0xFF3B0900),
    secondary = Color(0xFF77574D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBD0),
    onSecondaryContainer = Color(0xFF2C160E),
    tertiary = Color(0xFF6C5D2F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF6E1A7),
    onTertiaryContainer = Color(0xFF231B00),
    error = Color(0xFFBA1A1A),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF201A18),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF201A18),
    surfaceVariant = Color(0xFFF5DED6),
    onSurfaceVariant = Color(0xFF53433E),
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandOrangeDark,
    onPrimary = Color(0xFF5F1600),
    primaryContainer = Color(0xFF862200),
    onPrimaryContainer = Color(0xFFFFDBD0),
    secondary = Color(0xFFE7BDB1),
    onSecondary = Color(0xFF442A21),
    secondaryContainer = Color(0xFF5D4037),
    onSecondaryContainer = Color(0xFFFFDBD0),
    tertiary = Color(0xFFD9C58D),
    onTertiary = Color(0xFF3B2F05),
    tertiaryContainer = Color(0xFF534519),
    onTertiaryContainer = Color(0xFFF6E1A7),
    error = Color(0xFFFFB4AB),
    background = Color(0xFF201A18),
    onBackground = Color(0xFFEDE0DC),
    surface = Color(0xFF201A18),
    onSurface = Color(0xFFEDE0DC),
    surfaceVariant = Color(0xFF53433E),
    onSurfaceVariant = Color(0xFFD8C2BA),
)

@Composable
fun TsosuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
```

**Step 2: Verify build compiles**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/app/tsosu/ui/theme/TsosuTheme.kt
git commit -m "feat(theme): create TsosuTheme with warm orange brand palette"
```

---

### Task 3: Wire TsosuTheme into MainActivity

**Files:**
- Modify: `app/src/main/java/app/tsosu/MainActivity.kt` (line 44)

**Step 1: Replace bare MaterialTheme with TsosuTheme**

Change line 44 from:
```kotlin
MaterialTheme {
```
to:
```kotlin
TsosuTheme {
```

Add import at top:
```kotlin
import app.tsosu.ui.theme.TsosuTheme
```

**Step 2: Verify build compiles**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/app/tsosu/MainActivity.kt
git commit -m "feat(theme): wire TsosuTheme into MainActivity"
```

---

### Task 4: Add theme preferences to DataStore

**Files:**
- Create: `app/src/main/java/app/tsosu/ui/theme/ThemePreferences.kt`

**Step 1: Create ThemePreferences class**

```kotlin
package app.tsosu.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

enum class DarkModeOption { SYSTEM, LIGHT, DARK }

class ThemePreferences(private val context: Context) {

    private companion object {
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DARK_MODE = intPreferencesKey("dark_mode")
    }

    val dynamicColor: Flow<Boolean> = context.themeDataStore.data
        .map { it[DYNAMIC_COLOR] ?: false }

    val darkMode: Flow<DarkModeOption> = context.themeDataStore.data
        .map { prefs ->
            DarkModeOption.entries.getOrElse(prefs[DARK_MODE] ?: 0) { DarkModeOption.SYSTEM }
        }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.themeDataStore.edit { it[DYNAMIC_COLOR] = enabled }
    }

    suspend fun setDarkMode(option: DarkModeOption) {
        context.themeDataStore.edit { it[DARK_MODE] = option.ordinal }
    }
}
```

**Step 2: Provide via Hilt**

Create `app/src/main/java/app/tsosu/di/ThemeModule.kt`:

```kotlin
package app.tsosu.di

import android.content.Context
import app.tsosu.ui.theme.ThemePreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ThemeModule {

    @Provides
    @Singleton
    fun provideThemePreferences(@ApplicationContext context: Context): ThemePreferences {
        return ThemePreferences(context)
    }
}
```

**Step 3: Update MainActivity to read theme prefs**

In `MainActivity.kt`, update the `setContent` block to read theme preferences:

```kotlin
import androidx.compose.runtime.collectAsState
import app.tsosu.ui.theme.DarkModeOption
import app.tsosu.ui.theme.ThemePreferences
import javax.inject.Inject

// Inside class, add:
@Inject lateinit var themePreferences: ThemePreferences

// In setContent, before TsosuTheme:
val dynamicColor by themePreferences.dynamicColor.collectAsState(initial = false)
val darkModeOption by themePreferences.darkMode.collectAsState(initial = DarkModeOption.SYSTEM)
val darkTheme = when (darkModeOption) {
    DarkModeOption.SYSTEM -> isSystemInDarkTheme()
    DarkModeOption.LIGHT -> false
    DarkModeOption.DARK -> true
}

TsosuTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
```

Add import: `import androidx.compose.foundation.isSystemInDarkTheme`

**Step 4: Verify build compiles**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add app/src/main/java/app/tsosu/ui/theme/ThemePreferences.kt \
       app/src/main/java/app/tsosu/di/ThemeModule.kt \
       app/src/main/java/app/tsosu/MainActivity.kt
git commit -m "feat(theme): add DataStore theme preferences with dynamic color and dark mode"
```

---

## Batch 2: Shared Components

### Task 5: Create haptic feedback utility

**Files:**
- Create: `app/src/main/java/app/tsosu/ui/util/HapticUtils.kt`

**Step 1: Create haptic utility**

```kotlin
package app.tsosu.ui.util

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView

@Composable
fun rememberHaptic(): HapticHelper {
    val view = LocalView.current
    return HapticHelper(view)
}

class HapticHelper(private val view: View) {
    fun confirm() = view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    fun reject() = view.performHapticFeedback(HapticFeedbackConstants.REJECT)
    fun tick() = view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    fun longPress() = view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    fun gestureStart() = view.performHapticFeedback(HapticFeedbackConstants.GESTURE_START)
}
```

**Step 2: Verify build**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/app/tsosu/ui/util/HapticUtils.kt
git commit -m "feat(ui): add haptic feedback utility helper"
```

---

### Task 6: Create KonfettiOverlay component

**Files:**
- Create: `app/src/main/java/app/tsosu/ui/components/KonfettiOverlay.kt`

**Step 1: Create konfetti composable**

```kotlin
package app.tsosu.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

@Composable
fun KonfettiOverlay(showState: MutableState<Boolean>) {
    if (!showState.value) return

    val primary = MaterialTheme.colorScheme.primary.toArgb()
    val colors = listOf(
        primary,
        Color(0xFFFFA726).toArgb(),
        Color(0xFFFFD54F).toArgb(),
        Color(0xFFFF8A65).toArgb(),
        Color(0xFF81C784).toArgb(),
        Color(0xFF4FC3F7).toArgb(),
        Color(0xFFBA68C8).toArgb(),
        Color(0xFFFF7043).toArgb(),
        Color(0xFFE0E0E0).toArgb(),
    )

    val parties = listOf(
        Party(
            emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(100),
            colors = colors,
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            position = Position.Relative(0.5, 1.0),
            spread = 360,
        ),
        Party(
            emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(100),
            colors = colors,
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            position = Position.Relative(0.0, 1.0),
            angle = 45,
            spread = 90,
        ),
        Party(
            emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(100),
            colors = colors,
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            position = Position.Relative(1.0, 1.0),
            angle = 135,
            spread = 90,
        ),
    )

    KonfettiView(
        parties = parties,
        updateListener = { state ->
            if (state.activeSystems == 0) {
                showState.value = false
            }
        },
    )
}
```

**Step 2: Verify build**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/app/tsosu/ui/components/KonfettiOverlay.kt
git commit -m "feat(ui): add KonfettiOverlay celebration animation component"
```

---

### Task 7: Redesign TaskListItem with animated card and haptics

**Files:**
- Modify: `app/src/main/java/app/tsosu/ui/components/TaskListItem.kt`

**Step 1: Rewrite TaskListItem**

Replace entire file content with:

```kotlin
package app.tsosu.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Task
import app.tsosu.ui.util.rememberHaptic
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun TaskListItem(
    task: Task,
    onToggleDone: (String) -> Unit,
    onClick: (Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = rememberHaptic()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) 8.dp else 16.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "corner",
    )

    val containerColor by animateColorAsState(
        targetValue = when {
            task.done -> MaterialTheme.colorScheme.secondaryContainer
            task.priority != Priority.NONE -> Color(task.priority.color).copy(alpha = 0.08f)
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        label = "cardColor",
    )

    Card(
        onClick = {
            haptic.tick()
            onClick(task)
        },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = task.done,
                onCheckedChange = {
                    if (!task.done) haptic.confirm() else haptic.tick()
                    onToggleDone(task.id)
                },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                ),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val details = buildDetailString(task)
                if (details.isNotEmpty()) {
                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOverdue(task)) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = task.energyLevel.emoji,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

private fun buildDetailString(task: Task): String {
    val parts = mutableListOf<String>()
    task.dueDate?.let { due ->
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val today = now.date
        val tomorrow = kotlinx.datetime.DateTimeUnit.DAY.let { unit ->
            kotlinx.datetime.Instant.fromEpochMilliseconds(
                Clock.System.now().toEpochMilliseconds() + 86_400_000
            ).toLocalDateTime(TimeZone.currentSystemDefault()).date
        }
        parts += when (due.date) {
            today -> "Today"
            tomorrow -> "Tomorrow"
            else -> "${due.monthNumber}/${due.dayOfMonth}"
        }
    } ?: run {
        parts += "No date"
    }
    task.estimatedMinutes?.let { min ->
        parts += "${min}m"
    }
    return parts.joinToString(" \u00B7 ")
}

private fun isOverdue(task: Task): Boolean {
    val due = task.dueDate ?: return false
    if (task.done) return false
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return due < now
}
```

**Step 2: Verify build**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/app/tsosu/ui/components/TaskListItem.kt
git commit -m "feat(ui): redesign TaskListItem with animated card, haptics, and relative dates"
```

---

### Task 8: Create ProgressCard component

**Files:**
- Create: `app/src/main/java/app/tsosu/ui/components/ProgressCard.kt`

**Step 1: Create ProgressCard**

```kotlin
package app.tsosu.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProgressCard(
    completedCount: Int,
    totalCount: Int,
    totalMinutes: Int,
    streakDays: Int,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "progress",
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(56.dp),
                strokeWidth = 6.dp,
                trackColor = MaterialTheme.colorScheme.primaryContainer,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.width(20.dp))

            Column {
                Text(
                    text = "$completedCount / $totalCount tasks",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(4.dp))
                Row {
                    if (totalMinutes > 0) {
                        Text(
                            text = "${totalMinutes}m est.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    if (streakDays > 0) {
                        Text(
                            text = "$streakDays day streak",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
```

**Step 2: Verify build and commit**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :app:assembleDebug`

```bash
git add app/src/main/java/app/tsosu/ui/components/ProgressCard.kt
git commit -m "feat(ui): add ProgressCard component with animated circular progress"
```

---

## Batch 3: Navigation Restructure

### Task 9: Update Screen sealed class and BottomNavBar to 3 tabs

**Files:**
- Modify: `app/src/main/java/app/tsosu/navigation/Screen.kt` (line 24)
- Modify: `app/src/main/java/app/tsosu/navigation/TsosuNavHost.kt` (line 27 — remove Inbox composable from NavHost)

**Step 1: Update bottomNavItems**

In `Screen.kt`, change line 24:
```kotlin
val bottomNavItems = listOf(Focus, Habits, Upcoming)
```

**Step 2: Remove Inbox route from NavHost**

In `TsosuNavHost.kt`, remove line 27:
```kotlin
composable(Screen.Inbox.route) { InboxScreen(onTaskClick = onTaskClick) }
```

Also remove the import for InboxScreen.

**Step 3: Verify build**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add app/src/main/java/app/tsosu/navigation/Screen.kt \
       app/src/main/java/app/tsosu/navigation/TsosuNavHost.kt
git commit -m "feat(nav): consolidate to 3-tab navigation (Focus, Habits, Upcoming)"
```

---

### Task 10: Add FAB long-press for Pick One sheet

**Files:**
- Modify: `app/src/main/java/app/tsosu/MainActivity.kt`
- Create: `app/src/main/java/app/tsosu/ui/screens/pickone/PickOneSheet.kt`

**Step 1: Create PickOneSheet**

```kotlin
package app.tsosu.ui.screens.pickone

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.ui.util.rememberHaptic

@Composable
fun PickOneSheet(
    onDismiss: () -> Unit,
    viewModel: PickOneViewModel = hiltViewModel(),
) {
    val pickedTask by viewModel.pickedTask.collectAsStateWithLifecycle()
    val selectedEnergy by viewModel.selectedEnergy.collectAsStateWithLifecycle()
    val haptic = rememberHaptic()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Pick One",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "How's your energy right now?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EnergyLevel.entries.forEach { level ->
                FilterChip(
                    selected = selectedEnergy == level,
                    onClick = {
                        haptic.tick()
                        viewModel.selectEnergy(level)
                    },
                    label = { Text("${level.emoji} ${level.name.lowercase()}") },
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        pickedTask?.let { task ->
            AnimatedVisibility(
                visible = true,
                enter = scaleIn(initialScale = 0.8f) + fadeIn(),
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        task.estimatedMinutes?.let { min ->
                            Text(
                                text = "$min min",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = {
                    haptic.tick()
                    viewModel.pickAnother()
                }) {
                    Text("Pick another")
                }
                Button(onClick = {
                    haptic.confirm()
                    onDismiss()
                }) {
                    Text("Start this one")
                }
            }
        } ?: run {
            Text(
                text = "No tasks at this energy level. Try another!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}
```

**Step 2: Update MainActivity FAB with long-press and add PickOneSheet**

In `MainActivity.kt`, add state and replace FAB:

Add state variable (after `editingTaskId`):
```kotlin
var showPickOne by remember { mutableStateOf(false) }
```

Replace the `FloatingActionButton` with:
```kotlin
FloatingActionButton(
    onClick = { showAddTask = true },
    modifier = Modifier.pointerInput(Unit) {
        detectTapGestures(
            onLongPress = { showPickOne = true },
            onTap = { showAddTask = true },
        )
    },
) {
    Icon(Icons.Default.Add, contentDescription = "Add Task")
}
```

Wait — `FloatingActionButton` onClick conflicts with `pointerInput`. Instead, use a simpler approach: wrap in `combinedClickable`:

Actually, the simplest approach: use two separate interactions. Replace FAB block with:

```kotlin
floatingActionButton = {
    FloatingActionButton(
        onClick = { showAddTask = true },
        modifier = Modifier.combinedClickable(
            onClick = { showAddTask = true },
            onLongClick = {
                showPickOne = true
            },
        ),
    ) {
        Icon(Icons.Default.Add, contentDescription = "Add Task")
    }
},
```

Add import: `import androidx.compose.foundation.combinedClickable`
Add import: `import androidx.compose.foundation.ExperimentalFoundationApi`

Add the PickOneSheet modal after the TaskDetailSheet block:

```kotlin
if (showPickOne) {
    ModalBottomSheet(
        onDismissRequest = { showPickOne = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        PickOneSheet(onDismiss = { showPickOne = false })
    }
}
```

Add import: `import app.tsosu.ui.screens.pickone.PickOneSheet`

**Step 3: Remove PickOne from NavHost**

In `TsosuNavHost.kt`, remove the line:
```kotlin
composable(Screen.PickOne.route) { PickOneScreen() }
```

Also remove the PickOneScreen import.

**Step 4: Verify build**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add app/src/main/java/app/tsosu/ui/screens/pickone/PickOneSheet.kt \
       app/src/main/java/app/tsosu/MainActivity.kt \
       app/src/main/java/app/tsosu/navigation/TsosuNavHost.kt
git commit -m "feat(nav): move Pick One to FAB long-press bottom sheet"
```

---

## Batch 4: Screen Redesigns

### Task 11: Redesign FocusScreen with Overview Dashboard

**Files:**
- Modify: `app/src/main/java/app/tsosu/ui/screens/focus/FocusViewModel.kt`
- Modify: `app/src/main/java/app/tsosu/ui/screens/focus/FocusScreen.kt`

**Step 1: Expand FocusUiState to include inbox tasks and completion stats**

In `FocusViewModel.kt`, update the data class and ViewModel:

```kotlin
package app.tsosu.ui.screens.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.domain.model.Task
import app.tsosu.domain.repository.TaskRepository
import app.tsosu.domain.usecase.GetTodayOverviewUseCase
import app.tsosu.domain.usecase.ToggleTaskDoneUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FocusUiState(
    val focusTasks: List<Task> = emptyList(),
    val otherTasks: List<Task> = emptyList(),
    val inboxTasks: List<Task> = emptyList(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val totalEstimatedMinutes: Int = 0,
)

@HiltViewModel
class FocusViewModel @Inject constructor(
    getTodayOverview: GetTodayOverviewUseCase,
    taskRepository: TaskRepository,
    private val toggleTaskDone: ToggleTaskDoneUseCase,
) : ViewModel() {

    val uiState: StateFlow<FocusUiState> = combine(
        getTodayOverview(),
        taskRepository.getInboxTasks(),
    ) { overview, inbox ->
        FocusUiState(
            focusTasks = overview.tasks.filter { it.isFocus && !it.done },
            otherTasks = overview.tasks.filter { !it.isFocus && !it.done },
            inboxTasks = inbox.filter { !it.done },
            completedCount = overview.tasks.count { it.done },
            totalCount = overview.tasks.size,
            totalEstimatedMinutes = overview.totalEstimatedMinutes,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FocusUiState())

    fun onToggleDone(taskId: String) {
        viewModelScope.launch {
            toggleTaskDone(taskId)
        }
    }
}
```

**Step 2: Rewrite FocusScreen with overview dashboard**

```kotlin
package app.tsosu.ui.screens.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tsosu.ui.components.KonfettiOverlay
import app.tsosu.ui.components.ProgressCard
import app.tsosu.ui.components.TaskListItem

@Composable
fun FocusScreen(
    viewModel: FocusViewModel = hiltViewModel(),
    onTaskClick: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val showKonfetti = remember { mutableStateOf(false) }

    KonfettiOverlay(showKonfetti)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            ProgressCard(
                completedCount = state.completedCount,
                totalCount = state.totalCount,
                totalMinutes = state.totalEstimatedMinutes,
                streakDays = 0, // TODO: implement streak tracking
            )
        }

        if (state.focusTasks.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Today's Focus",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            items(state.focusTasks, key = { it.id }) { task ->
                TaskListItem(
                    task = task,
                    onToggleDone = { id ->
                        viewModel.onToggleDone(id)
                        showKonfetti.value = true
                    },
                    onClick = { onTaskClick(it.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        if (state.otherTasks.isNotEmpty() || state.inboxTasks.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Other tasks",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(state.otherTasks + state.inboxTasks, key = { it.id }) { task ->
                TaskListItem(
                    task = task,
                    onToggleDone = { id ->
                        viewModel.onToggleDone(id)
                        showKonfetti.value = true
                    },
                    onClick = { onTaskClick(it.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        if (state.focusTasks.isEmpty() && state.otherTasks.isEmpty() && state.inboxTasks.isEmpty()) {
            item {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Nothing today. You earned a break!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}
```

**Step 3: Verify build**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add app/src/main/java/app/tsosu/ui/screens/focus/FocusViewModel.kt \
       app/src/main/java/app/tsosu/ui/screens/focus/FocusScreen.kt
git commit -m "feat(focus): redesign with overview dashboard, konfetti, and inbox merge"
```

---

### Task 12: Polish HabitsScreen with animated cards and progress ring

**Files:**
- Modify: `app/src/main/java/app/tsosu/ui/screens/habits/HabitsScreen.kt`

**Step 1: Update HabitsScreen**

Add haptics to habit toggles, replace `HabitRow` Card with animated card style, add progress ring header with konfetti. Full rewrite of file (147 lines → ~160 lines). Key changes:

- Add `rememberHaptic()` and `KonfettiOverlay`
- Replace `Card` in HabitRow with animated `containerColor` using `animateColorAsState`
- Add `ProgressCard` at top showing habits completion
- Trigger konfetti when toggling a habit that completes a routine group
- Add `animateItem()` to list items

**Step 2: Verify build and commit**

```bash
git add app/src/main/java/app/tsosu/ui/screens/habits/HabitsScreen.kt
git commit -m "feat(habits): add animated cards, haptics, progress ring, and konfetti"
```

---

### Task 13: Polish UpcomingScreen with date group headers

**Files:**
- Modify: `app/src/main/java/app/tsosu/ui/screens/upcoming/UpcomingScreen.kt`
- Modify: `app/src/main/java/app/tsosu/ui/screens/upcoming/UpcomingViewModel.kt`

**Step 1: Update UpcomingViewModel to group by date**

Add a data class for grouped tasks and group logic:

```kotlin
data class UpcomingUiState(
    val groups: List<DateGroup> = emptyList(),
)

data class DateGroup(
    val label: String,
    val tasks: List<Task>,
)
```

Group tasks into "Today", "Tomorrow", "This Week", "Later" based on `dueDate`.

**Step 2: Update UpcomingScreen with sticky headers**

Use `stickyHeader` items in LazyColumn for each date group. Add konfetti on task completion. Add `animateItem()`.

**Step 3: Verify build and commit**

```bash
git add app/src/main/java/app/tsosu/ui/screens/upcoming/UpcomingViewModel.kt \
       app/src/main/java/app/tsosu/ui/screens/upcoming/UpcomingScreen.kt
git commit -m "feat(upcoming): group tasks by date with sticky headers and konfetti"
```

---

### Task 14: Update QuickAddTaskSheet with quick-date buttons and haptics

**Files:**
- Modify: `app/src/main/java/app/tsosu/ui/screens/quickadd/QuickAddTaskSheet.kt`

**Step 1: Add quick-date row and haptics**

Before the calendar picker button, add a row of quick-date chips:
- "Today", "Tomorrow", "Next Week"
- Each sets `dueDate` to the appropriate `LocalDateTime`
- Add `rememberHaptic()` and call `haptic.tick()` on each chip selection
- Add `haptic.gestureStart()` at sheet composition

**Step 2: Verify build and commit**

```bash
git add app/src/main/java/app/tsosu/ui/screens/quickadd/QuickAddTaskSheet.kt
git commit -m "feat(quickadd): add quick-date buttons and haptic feedback"
```

---

### Task 15: Update TaskDetailSheet with haptics

**Files:**
- Modify: `app/src/main/java/app/tsosu/ui/screens/taskdetail/TaskDetailSheet.kt`

**Step 1: Add haptics**

- `rememberHaptic()` at top
- `haptic.tick()` on priority, energy, time chip selections
- `haptic.confirm()` on save
- `haptic.reject()` on delete
- Add confirmation dialog before delete (AlertDialog with "Delete this task?" + confirm/cancel)

**Step 2: Verify build and commit**

```bash
git add app/src/main/java/app/tsosu/ui/screens/taskdetail/TaskDetailSheet.kt
git commit -m "feat(taskdetail): add haptic feedback and delete confirmation dialog"
```

---

## Batch 5: Settings + Theme Toggle

### Task 16: Add theme settings to SettingsScreen

**Files:**
- Modify: `app/src/main/java/app/tsosu/ui/screens/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/app/tsosu/ui/screens/settings/SettingsViewModel.kt`

**Step 1: Add theme fields to SettingsViewModel**

Inject `ThemePreferences`, expose `dynamicColor` and `darkMode` as StateFlows, add setter methods.

**Step 2: Add appearance section to SettingsScreen**

At top of settings (before Vikunja section), add:
- "Appearance" section title
- Switch for "Dynamic Colors" (with note "Adapts to your wallpaper")
- Segmented buttons or dropdown for Dark Mode (System / Light / Dark)

**Step 3: Verify build and commit**

```bash
git add app/src/main/java/app/tsosu/ui/screens/settings/SettingsScreen.kt \
       app/src/main/java/app/tsosu/ui/screens/settings/SettingsViewModel.kt
git commit -m "feat(settings): add dynamic colors toggle and dark mode picker"
```

---

## Batch 6: Navigation Transitions

### Task 17: Add screen transitions to TsosuNavHost

**Files:**
- Modify: `app/src/main/java/app/tsosu/navigation/TsosuNavHost.kt`

**Step 1: Add transition specs**

Use `composable()` with `enterTransition` and `exitTransition` parameters:

- Tab screens (Focus, Habits, Upcoming): `fadeIn + scaleIn(0.92f)` / `fadeOut + scaleOut(0.92f)`
- Settings: `slideInHorizontally(initialOffsetX = { it })` / `slideOutHorizontally(targetOffsetX = { it })`
- Other screens: default fade

**Step 2: Verify build and commit**

```bash
git add app/src/main/java/app/tsosu/navigation/TsosuNavHost.kt
git commit -m "feat(nav): add fadeScale tab transitions and slideX settings transition"
```

---

## Batch 7: Wire Konfetti into MainActivity overlay

### Task 18: Add global KonfettiOverlay to MainActivity

**Files:**
- Modify: `app/src/main/java/app/tsosu/MainActivity.kt`

**Step 1: Add a global konfetti state**

The FocusScreen/HabitsScreen already have local konfetti. For the global case, add a `Box` wrapping the Scaffold with a `KonfettiOverlay` on top (using `Box` with `matchParentSize`). This ensures konfetti renders above all content including bottom sheets.

Actually — the per-screen approach is cleaner. Skip this task; each screen handles its own konfetti.

---

## Batch 8: Final Build Verification

### Task 19: Full build and smoke test

**Step 1: Run full build**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 2: Run domain tests**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :domain:test`
Expected: All tests pass (FocusViewModel now takes extra parameter but is in app module, not domain)

**Step 3: Commit final state if any fixups needed**

---

## Summary

| Batch | Tasks | Focus |
|-------|-------|-------|
| 1: Foundation | 1-4 | Dependency, TsosuTheme, DataStore prefs |
| 2: Components | 5-8 | Haptics, Konfetti, TaskListItem, ProgressCard |
| 3: Navigation | 9-10 | 3 tabs, FAB long-press Pick One |
| 4: Screens | 11-15 | Focus, Habits, Upcoming, QuickAdd, TaskDetail |
| 5: Settings | 16 | Theme toggle in Settings |
| 6: Transitions | 17 | Screen transition animations |
| 7: Verification | 18-19 | Build + test |

Total: ~17 implementation tasks, each 2-10 minutes.
