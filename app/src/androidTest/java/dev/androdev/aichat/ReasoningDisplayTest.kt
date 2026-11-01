package dev.androdev.aichat

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import dev.androdev.aichat.data.ChatService
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ReasoningDisplayTest : ChatUiTestBase() {

    private val fake = FakeChatService(reasoning = "Let me think about this.")

    override fun createService(): ChatService = fake

    @Test
    fun showsReasoningCollapsedAndKeepsItAcrossChats() {
        val scenario = launchChat()
        try {
            waitForText("What are you working on?")
            sendMessage("Hello there")
            waitForText("Hello from the fake service.")
            waitForTextContaining("Thought")

            waitForNoText("Let me think about this.")
            clickTextContaining("Thought")
            waitForText("Let me think about this.")

            clickContentDescription("Open conversations")
            waitForText("Recent")
            clickText("New chat")
            waitForNoText("Let me think about this.")

            clickContentDescription("Open conversations")
            waitForText("Hello there")
            clickText("Hello there")
            waitForText("Hello from the fake service.")
            waitForTextContaining("Thought")

            clickTextContaining("Thought")
            waitForText("Let me think about this.")
        } finally {
            scenario.close()
        }
    }
}
