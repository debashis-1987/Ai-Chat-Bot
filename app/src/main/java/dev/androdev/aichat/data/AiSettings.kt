package dev.androdev.aichat.data

enum class ThinkingLevel(val label: String, val wireValue: String) {
    LOW("Low", "low"),
    MEDIUM("Medium", "medium"),
    HIGH("High", "high"),
}

data class AiSettings(
    val baseUrl: String = "https://api.openai.com/v1",
    val apiKey: String = "",
    val model: String = "gpt-4o-mini",
    val systemPrompt: String = "You are a helpful, concise assistant.",
    val maxContextTokens: Int = 12000,
    val thinkingEnabled: Boolean = false,
    val thinkingLevel: ThinkingLevel = ThinkingLevel.MEDIUM,
) {
    val isReady: Boolean
        get() = apiKey.isNotBlank() && baseUrl.isNotBlank() && model.isNotBlank()
}
