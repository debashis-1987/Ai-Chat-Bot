package dev.androdev.aichat.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatRequestBodyTest {

    private val settings = AiSettings(
        baseUrl = "http://test.local/v1",
        apiKey = "key",
        model = "gpt-5",
        systemPrompt = "Be brief",
    )

    private val history = listOf(
        Message(id = "m1", role = Role.USER, content = "hi", position = 1),
    )

    @Test
    fun omitsReasoningEffortWhenThinkingIsOff() {
        val body = buildRequestBody(settings, history)

        assertFalse(body.has("reasoning_effort"))
    }

    @Test
    fun sendsSelectedReasoningEffortWhenThinkingIsOn() {
        ThinkingLevel.entries.forEach { level ->
            val body = buildRequestBody(
                settings.copy(thinkingEnabled = true, thinkingLevel = level),
                history,
            )

            assertEquals(level.wireValue, body.getString("reasoning_effort"))
        }
    }

    @Test
    fun keepsSystemPromptModelAndMessages() {
        val body = buildRequestBody(settings, history)

        assertEquals("gpt-5", body.getString("model"))
        assertTrue(body.getBoolean("stream"))
        val messages = body.getJSONArray("messages")
        assertEquals(2, messages.length())
        assertEquals("system", messages.getJSONObject(0).getString("role"))
        assertEquals("Be brief", messages.getJSONObject(0).getString("content"))
        assertEquals("user", messages.getJSONObject(1).getString("role"))
        assertEquals("hi", messages.getJSONObject(1).getString("content"))
    }
}
