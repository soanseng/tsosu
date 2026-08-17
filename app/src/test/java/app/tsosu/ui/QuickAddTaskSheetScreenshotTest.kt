package app.tsosu.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tsosu.ui.screens.quickadd.QuickAddTaskSheet
import app.tsosu.ui.theme.TsosuTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QuickAddTaskSheetScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent() {
        composeRule.setContent {
            TsosuTheme(darkTheme = false) {
                QuickAddTaskSheet(
                    onDismiss = {},
                    onAdd = { _, _, _, _, _, _, _ -> },
                )
            }
        }
    }

    @Test
    @Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel6)
    fun customRecurrenceField_renders() {
        setContent()
        // Only the title field initially
        composeRule.onAllNodes(hasSetTextAction()).assertCountEquals(1)
        composeRule.onNodeWithText("Custom").performClick()
        // Custom recurrence input appears
        composeRule.onAllNodes(hasSetTextAction()).assertCountEquals(2)
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(sdk = [35], qualifiers = "w411dp-h500dp-420dpi")
    fun sheetContent_scrollsToSaveButton() {
        setContent()
        // Reveal the recurrence section first — it starts below the fold on a short screen
        repeat(2) { composeRule.onRoot().performTouchInput { swipeUp() } }
        composeRule.onNodeWithText("Custom").performClick()
        composeRule.onAllNodes(hasSetTextAction()).assertCountEquals(2)
        // On a short screen the save button starts below the fold
        composeRule.onNodeWithText("Add Task").assertIsNotDisplayed()
        // The sheet body must be scrollable down to it
        repeat(3) { composeRule.onRoot().performTouchInput { swipeUp() } }
        composeRule.onNodeWithText("Add Task").assertIsDisplayed()
    }
}
