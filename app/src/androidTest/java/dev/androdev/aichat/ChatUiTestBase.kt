package dev.androdev.aichat

import android.content.Context
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.input.ImeAction
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import dev.androdev.aichat.data.AiSettings
import dev.androdev.aichat.data.ChatDatabase
import dev.androdev.aichat.data.ChatService
import dev.androdev.aichat.data.ServiceLocator
import dev.androdev.aichat.data.SettingsStore
import org.junit.Before
import org.junit.Rule

abstract class ChatUiTestBase {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    protected val context: Context = ApplicationProvider.getApplicationContext()

    protected open fun createService(): ChatService = FakeChatService()

    @Before
    fun setUpState() {
        ChatDatabase.resetForTests()
        ServiceLocator.chatService = createService()
        context.deleteDatabase("chat.db")
        listOf("ai_settings", "ai_secrets", "ai_drafts").forEach { name ->
            context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
        }
        SettingsStore(context).save(
            AiSettings(
                baseUrl = "http://fake.local/v1",
                apiKey = "test-key",
                model = "fake-model",
                systemPrompt = "Test prompt",
                maxContextTokens = 4000,
            ),
        )
    }

    protected fun launchChat(): ActivityScenario<MainActivity> =
        ActivityScenario.launch(MainActivity::class.java)

    protected fun sendMessage(text: String) {
        composeRule
            .onDisplayed(hasSetTextAction() and hasImeAction(ImeAction.Send))
            .performTextInput(text)
        waitForText(text)
        waitForEnabled("Send message")
        clickContentDescription("Send message")
    }

    protected fun typeDraft(text: String) {
        composeRule
            .onDisplayed(hasSetTextAction() and hasImeAction(ImeAction.Send))
            .performTextInput(text)
        waitForText(text)
    }

    protected fun replaceText(current: String, replacement: String) {
        composeRule
            .onDisplayed(hasSetTextAction() and hasText(current))
            .performTextReplacement(replacement)
    }

    protected fun clickText(text: String) {
        waitForStable(hasText(text))
        composeRule.onDisplayed(hasText(text)).performClick()
    }

    protected fun clickContentDescription(description: String) {
        waitForStable(hasContentDescription(description))
        composeRule.onDisplayed(hasContentDescription(description)).performClick()
    }

    protected fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
            composeRule
                .onAllNodes(hasText(text))
                .fetchSemanticsNodes()
                .any { it.isOnScreen() }
        }
    }

    protected fun waitForTextContaining(text: String) {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
            composeRule
                .onAllNodes(hasText(text, substring = true))
                .fetchSemanticsNodes()
                .any { it.isOnScreen() }
        }
    }

    protected fun clickTextContaining(text: String) {
        waitForStable(hasText(text, substring = true))
        composeRule.onDisplayed(hasText(text, substring = true)).performClick()
    }

    protected fun waitForNoText(text: String) {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
            composeRule
                .onAllNodes(hasText(text))
                .fetchSemanticsNodes()
                .none { it.isOnScreen() }
        }
    }

    protected fun waitForEnabled(description: String) {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
            composeRule
                .onAllNodes(hasContentDescription(description))
                .fetchSemanticsNodes()
                .any { node ->
                    node.isOnScreen() &&
                        !node.config.contains(SemanticsProperties.Disabled)
                }
        }
    }

    private fun waitForStable(matcher: SemanticsMatcher) {
        var previous: Rect? = null
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
            val current = composeRule
                .onAllNodes(matcher)
                .fetchSemanticsNodes()
                .firstOrNull { it.isOnScreen() }
                ?.boundsInRoot
            val stable = current != null && current == previous
            previous = current
            stable
        }
    }

    private fun ComposeTestRule.onDisplayed(matcher: SemanticsMatcher): SemanticsNodeInteraction {
        val nodes = onAllNodes(matcher).fetchSemanticsNodes()
        val index = nodes.indexOfFirst { it.isOnScreen() }
        check(index >= 0) {
            "No on-screen node matched $matcher. Found ${nodes.size} node(s) at " +
                nodes.map { it.boundsInRoot }
        }
        return onAllNodes(matcher)[index]
    }

    private val screenBounds: Rect by lazy {
        val metrics = context.resources.displayMetrics
        Rect(0f, 0f, metrics.widthPixels.toFloat(), metrics.heightPixels.toFloat())
    }

    private fun SemanticsNode.isOnScreen(): Boolean {
        return boundsInRoot.width > 0f &&
            boundsInRoot.height > 0f &&
            boundsInRoot.left >= screenBounds.left &&
            boundsInRoot.right <= screenBounds.right &&
            boundsInRoot.top >= screenBounds.top &&
            boundsInRoot.bottom <= screenBounds.bottom
    }

    private companion object {
        const val TIMEOUT_MS = 60_000L
    }
}
