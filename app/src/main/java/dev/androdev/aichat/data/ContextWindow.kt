package dev.androdev.aichat.data

object ContextWindow {

    fun estimateTokens(text: String): Int = if (text.isEmpty()) 0 else text.length / 4 + 1

    fun fit(messages: List<Message>, maxTokens: Int, systemPrompt: String): List<Message> {
        if (messages.isEmpty() || maxTokens <= 0) return messages

        val budget = (maxTokens - estimateTokens(systemPrompt)).coerceAtLeast(1)
        var used = 0
        val kept = ArrayDeque<Message>()
        for (index in messages.indices.reversed()) {
            val cost = estimateTokens(messages[index].content)
            if (kept.isNotEmpty() && used + cost > budget) break
            used += cost
            kept.addFirst(messages[index])
        }
        return kept.toList()
    }
}
