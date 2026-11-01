package dev.androdev.aichat.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatStreamDeltaTest {

    private fun delta(json: String): StreamDelta = parseStreamDelta("""{"choices":[{"delta":$json}]}""")

    @Test
    fun readsContentChunk() {
        assertEquals(StreamDelta(content = "Hi"), delta("""{"content":"Hi"}"""))
    }

    @Test
    fun readsReasoningContentChunk() {
        assertEquals(StreamDelta(reasoning = "Thinking"), delta("""{"reasoning_content":"Thinking"}"""))
    }

    @Test
    fun readsOpenRouterReasoningChunk() {
        assertEquals(StreamDelta(reasoning = "Thinking"), delta("""{"reasoning":"Thinking"}"""))
    }

    @Test
    fun readsContentAndReasoningTogether() {
        assertEquals(
            StreamDelta(content = "Answer", reasoning = "Plan"),
            delta("""{"content":"Answer","reasoning_content":"Plan"}"""),
        )
    }

    @Test
    fun toleratesNullsAndMissingChoices() {
        assertEquals(StreamDelta(), delta("""{"content":null,"reasoning_content":null}"""))
        assertEquals(StreamDelta(), parseStreamDelta("""{"choices":[]}"""))
        assertEquals(StreamDelta(), parseStreamDelta("""{"id":"x"}"""))
    }
}
