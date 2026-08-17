package app.tsosu.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tsosu.domain.model.Task
import app.tsosu.ui.components.ProgressCard
import app.tsosu.ui.components.TaskListItem
import app.tsosu.ui.theme.TsosuTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.datetime.LocalDateTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel6)
class ComponentsScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun progressCardLight() {
        composeRule.setContent {
            TsosuTheme(darkTheme = false) {
                ProgressCard(
                    completedCount = 3,
                    totalCount = 8,
                    totalMinutes = 120,
                    streakDays = 12,
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun progressCardDark() {
        composeRule.setContent {
            TsosuTheme(darkTheme = true) {
                ProgressCard(
                    completedCount = 5,
                    totalCount = 5,
                    totalMinutes = 210,
                    streakDays = 30,
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun taskListItem() {
        val task = Task(
            title = "整理簽證文件",
            description = "護照影本 + 財力證明",
            estimatedMinutes = 40,
            dueDate = LocalDateTime(2026, 8, 18, 9, 30),
        )
        composeRule.setContent {
            TsosuTheme(darkTheme = false) {
                TaskListItem(
                    task = task,
                    onToggleDone = {},
                    onClick = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
