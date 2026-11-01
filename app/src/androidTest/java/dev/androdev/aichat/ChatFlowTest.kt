package dev.androdev.aichat

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ChatFlowTest : ChatUiTestBase() {

    @Test
    fun sendsMessageChangesModelDeletesAndUndoes() {
        val scenario = launchChat()
        try {
            waitForText("What are you working on?")

            sendMessage("Hello there")
            waitForText("Hello from the fake service.")

            clickContentDescription("Open conversations")
            waitForText("Recent")

            clickContentDescription("Chat options")
            waitForText("Delete chat")
            clickText("Delete chat")

            waitForText("Chat deleted")
            clickText("Undo")

            clickContentDescription("Open conversations")
            waitForText("Hello there")

            clickText("Settings")
            waitForText("Save changes")

            clickText("Load models")
            waitForText("Choose (2)")
            clickText("Choose (2)")
            waitForText("fake-alt")
            clickText("fake-alt")

            clickText("Save changes")
            waitForText("Message the assistant")
            waitForText("fake-alt")
        } finally {
            scenario.close()
        }
    }
}
