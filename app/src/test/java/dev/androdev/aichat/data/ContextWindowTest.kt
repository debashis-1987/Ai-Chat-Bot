package dev.androdev.aichat.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ContextWindowTest {

    private fun message(role: Role, content: String, position: Long) =
        Message(id = "m$position", role = role, content = content, position = position)

    @Test
    fun keepsEverythingWhenBudgetIsUnlimited() {
        val messages = listOf(
            message(Role.USER, "a".repeat(100), 1),
            message(Role.ASSISTANT, "b".repeat(100), 2),
        )

        assertEquals(messages, ContextWindow.fit(messages, maxTokens = 0, systemPrompt = ""))
    }

    @Test
    fun dropsOldestMessagesWhenOverBudget() {
        val first = message(Role.USER, "a".repeat(40), 1)
        val second = message(Role.ASSISTANT, "b".repeat(40), 2)
        val last = message(Role.USER, "abcd", 3)

        val result = ContextWindow.fit(listOf(first, second, last), maxTokens = 15, systemPrompt = "")

        assertEquals(listOf(second, last), result)
    }

    @Test
    fun alwaysKeepsTheNewestMessage() {
        val huge = message(Role.USER, "x".repeat(400), 1)

        assertEquals(listOf(huge), ContextWindow.fit(listOf(huge), maxTokens = 10, systemPrompt = ""))
    }

    @Test
    fun systemPromptConsumesPartOfTheBudget() {
        val system = "s".repeat(40)
        val older = message(Role.USER, "a".repeat(40), 1)
        val newest = message(Role.ASSISTANT, "abcd", 2)

        val result = ContextWindow.fit(listOf(older, newest), maxTokens = 20, systemPrompt = system)

        assertEquals(listOf(newest), result)
    }

    @Test
    fun estimatesRoughlyFourCharactersPerToken() {
        assertEquals(0, ContextWindow.estimateTokens(""))
        assertEquals(2, ContextWindow.estimateTokens("abcd"))
        assertEquals(101, ContextWindow.estimateTokens("x".repeat(400)))
    }
}
