package dev.androdev.aichat

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import dev.androdev.aichat.data.ChatService
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ChatErrorRetryTest : ChatUiTestBase() {

    private val fake = FakeChatService()

    override fun createService(): ChatService = fake

    @Test
    fun failedReplyOffersRetry() {
        fake.failNextReplyWith = "Couldn't reach the provider."
        val scenario = launchChat()
        try {
            waitForText("What are you working on?")
            sendMessage("Hello there")

            waitForText("Couldn't reach the provider.")
            clickText("Try again")

            waitForText("Hello from the fake service.")
        } finally {
            scenario.close()
        }
    }
}
