package dev.androdev.aichat.data

data class StreamDelta(
    val content: String = "",
    val reasoning: String = "",
)

interface ChatService {
    suspend fun streamReply(
        settings: AiSettings,
        history: List<Message>,
        onDelta: (StreamDelta) -> Unit,
    )

    suspend fun fetchModels(settings: AiSettings): List<String>
}
