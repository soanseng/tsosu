package app.tsosu.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tsosu.domain.model.Project
import app.tsosu.domain.model.Task
import app.tsosu.ui.screens.categories.CategoriesContent
import app.tsosu.ui.screens.categories.CategoriesUiState
import app.tsosu.ui.screens.categories.CategoryGroup
import app.tsosu.ui.theme.TsosuTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The Categories view renders each group with its own header and its tasks —
 * including the catch-all bucket, which must not lose tasks whose project
 * no longer exists.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CategoriesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel6)
    fun groupsRenderHeadersWithTheirTasks() {
        val state = CategoriesUiState(
            groups = listOf(
                CategoryGroup(
                    project = Project(id = "p1", title = "論文相關"),
                    tasks = listOf(Task(title = "整理文獻回顧第二章")),
                ),
                CategoryGroup(
                    project = null,
                    tasks = listOf(Task(title = "繳電話費")),
                ),
            ),
            taskCount = 2,
        )
        composeRule.setContent {
            TsosuTheme(darkTheme = false) {
                CategoriesContent(state = state, onToggleDone = {}, onTaskClick = {})
            }
        }

        composeRule.onNodeWithText("📁 論文相關").assertIsDisplayed()
        composeRule.onNodeWithText("整理文獻回顧第二章").assertIsDisplayed()
        composeRule.onNodeWithText("📁 Uncategorized").assertIsDisplayed()
        composeRule.onNodeWithText("繳電話費").assertIsDisplayed()
    }
}
