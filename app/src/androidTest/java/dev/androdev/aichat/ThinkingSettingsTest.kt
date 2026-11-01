package dev.androdev.aichat

import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import dev.androdev.aichat.data.ChatService
import dev.androdev.aichat.data.SettingsStore
import dev.androdev.aichat.data.ThinkingLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ThinkingSettingsTest : ChatUiTestBase() {

    private val fake = FakeChatService()

    override fun createService(): ChatService = fake

    @Test
    fun enablesThinkingAndSendsSelectedLevel() {
        val scenario = launchChat()
        try {
            waitForText("What are you working on?")
            clickContentDescription("Open conversations")
            waitForText("No chats yet. Start one above.")
            clickText("Settings")
            waitForText("Save changes")

            composeRule.onNode(hasText("Thinking mode")).performScrollTo()
            waitForText("Thinking mode")
            composeRule.onNode(hasText("Thinking mode") and isToggleable()).performClick()
            composeRule.onNode(hasText("High")).performScrollTo().performClick()

            clickText("Save changes")
            waitForText("Message the assistant")

            sendMessage("Hello there")
            waitForText("Hello from the fake service.")

            val used = fake.lastSettings
            assertEquals(true, used?.thinkingEnabled)
            assertEquals(ThinkingLevel.HIGH, used?.thinkingLevel)

            clickContentDescription("Open conversations")
            waitForText("Recent")
            clickText("Settings")
            waitForText("Save changes")
            composeRule.onNode(hasText("Thinking mode") and isToggleable()).assertIsOn()
            composeRule.onNode(hasText("High") and isSelected()).assertIsSelected()

            val saved = SettingsStore(context).load()
            assertTrue(saved.thinkingEnabled)
            assertEquals(ThinkingLevel.HIGH, saved.thinkingLevel)
        } finally {
            scenario.close()
        }
    }
}
