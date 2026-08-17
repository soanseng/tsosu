package app.tsosu.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tsosu.domain.model.Task
import app.tsosu.ui.screens.habits.RecurringTaskRow
import app.tsosu.ui.theme.TsosuTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HabitsRecurringScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel6)
    fun recurringTaskRow_rendersWithStreakAndRule() {
        val task = Task(
            title = "每天走路 10 分鐘",
            recurrenceRule = "RRULE:FREQ=DAILY",
            dueDate = LocalDateTime(2026, 8, 17, 0, 0),
            completions = listOf(
                LocalDate(2026, 8, 15),
                LocalDate(2026, 8, 16),
                LocalDate(2026, 8, 17),
            ),
            tinyVersion = "站起來走 1 分鐘",
        )
        composeRule.setContent {
            TsosuTheme(darkTheme = false) {
                RecurringTaskRow(task = task, streak = 3, onToggle = {}, onOpen = {})
            }
        }
        composeRule.onNodeWithText("每天走路 10 分鐘").assertIsDisplayed()
        composeRule.onRoot().captureRoboImage()
    }
}
