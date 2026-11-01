package dev.androdev.aichat

import dev.androdev.aichat.data.AiSettings
import dev.androdev.aichat.data.ChatException
import dev.androdev.aichat.data.ChatService
import dev.androdev.aichat.data.Message
import dev.androdev.aichat.data.StreamDelta
import kotlinx.coroutines.delay

class FakeChatService(
    private val reply: String = "Hello from the fake service.",
    private val reasoning: String = "",
    private val models: List<String> = listOf("fake-model", "fake-alt"),
) : ChatService {

    var failNextReplyWith: String? = null
    var lastSettings: AiSettings? = null

    override suspend fun streamReply(
        settings: AiSettings,
        history: List<Message>,
        onDelta: (StreamDelta) -> Unit,
    ) {
        lastSettings = settings
        failNextReplyWith?.let { message ->
            failNextReplyWith = null
            throw ChatException(message)
        }
        if (reasoning.isNotEmpty()) {
            val first = reasoning.take(reasoning.length / 2)
            onDelta(StreamDelta(reasoning = first))
            delay(30)
            onDelta(StreamDelta(reasoning = reasoning.drop(first.length)))
        }
        val first = reply.take(reply.length / 2)
        onDelta(StreamDelta(content = first))
        delay(30)
        onDelta(StreamDelta(content = reply.drop(first.length)))
    }

    override suspend fun fetchModels(settings: AiSettings): List<String> = models
}
