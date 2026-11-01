package dev.androdev.aichat

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ConversationActionsTest : ChatUiTestBase() {

    @Test
    fun renamesConversationAndSurvivesRelaunch() {
        var scenario = launchChat()
        try {
            waitForText("What are you working on?")
            sendMessage("Hello there")
            waitForText("Hello from the fake service.")

            clickContentDescription("Open conversations")
            waitForText("Recent")

            clickContentDescription("Chat options")
            waitForText("Rename")
            clickText("Rename")

            waitForText("Rename chat")
            replaceText("Hello there", "Project notes")
            clickText("Save")
            waitForText("Project notes")
        } finally {
            scenario.close()
        }

        scenario = launchChat()
        try {
            waitForText("Project notes")
        } finally {
            scenario.close()
        }
    }

    @Test
    fun pinsAndUnpinsConversation() {
        val scenario = launchChat()
        try {
            waitForText("What are you working on?")
            sendMessage("Hello there")
            waitForText("Hello from the fake service.")

            clickContentDescription("Open conversations")
            waitForText("Recent")

            clickContentDescription("Chat options")
            waitForText("Pin to top")
            clickText("Pin to top")
            waitForText("Pinned")

            clickContentDescription("Chat options")
            waitForText("Unpin from top")
            clickText("Unpin from top")
            waitForNoText("Pinned")

            clickContentDescription("Chat options")
            waitForText("Pin to top")
        } finally {
            scenario.close()
        }
    }

    @Test
    fun draftsAreScopedToEachConversation() {
        val scenario = launchChat()
        try {
            waitForText("What are you working on?")
            sendMessage("Hello there")
            waitForText("Hello from the fake service.")

            typeDraft("Draft for first")

            clickContentDescription("Open conversations")
            waitForText("Recent")
            clickText("New chat")
            waitForNoText("Draft for first")

            typeDraft("Draft for new chat")

            clickContentDescription("Open conversations")
            waitForText("Hello there")
            clickText("Hello there")
            waitForText("Draft for first")
            waitForNoText("Draft for new chat")

            clickContentDescription("Open conversations")
            clickText("New chat")
            waitForText("Draft for new chat")
        } finally {
            scenario.close()
        }
    }
}
