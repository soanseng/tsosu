package app.tsosu.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tsosu.ui.screens.inbox.StaleCleanupCard
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
class StaleCleanupCardScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel6)
    fun staleCleanupCard_renders() {
        composeRule.setContent {
            TsosuTheme(darkTheme = false) {
                StaleCleanupCard(count = 5, onCleanUp = {}, onLater = {})
            }
        }
        composeRule.onNodeWithText("Clean up").assertExists()
        composeRule.onRoot().captureRoboImage()
    }
}
