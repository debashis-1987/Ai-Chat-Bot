package dev.androdev.aichat.data

import java.util.UUID

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val role: Role,
    val content: String,
    val position: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val reasoning: String = "",
    val reasoningMs: Long = 0L,
)

enum class Role(val apiValue: String) {
    USER("user"),
    ASSISTANT("assistant"),
}
