package dev.androdev.aichat.data

import kotlinx.coroutines.flow.Flow

class ChatRepository(private val dao: ChatDao) {

    fun conversations(): Flow<List<ConversationEntity>> = dao.observeConversations()

    suspend fun messages(conversationId: String): List<Message> =
        dao.messages(conversationId).mapNotNull { entity ->
            val role = Role.entries.firstOrNull { it.apiValue == entity.role } ?: return@mapNotNull null
            Message(
                id = entity.id,
                role = role,
                content = entity.content,
                position = entity.position,
                timestamp = entity.timestamp,
                reasoning = entity.reasoning,
                reasoningMs = entity.reasoningMs,
            )
        }

    suspend fun conversation(conversationId: String): ConversationEntity? =
        dao.conversation(conversationId)

    suspend fun restoreConversation(conversation: ConversationEntity, messages: List<Message>) {
        dao.upsertConversation(conversation)
        messages.forEach { dao.upsertMessage(it.toEntity(conversation.id)) }
    }

    suspend fun createConversation(id: String, title: String) {
        val now = System.currentTimeMillis()
        dao.upsertConversation(
            ConversationEntity(id = id, title = title, pinned = false, createdAt = now, updatedAt = now),
        )
    }

    suspend fun saveMessage(conversationId: String, message: Message) {
        dao.upsertMessage(message.toEntity(conversationId))
        dao.touchConversation(conversationId, System.currentTimeMillis())
    }

    suspend fun deleteMessage(messageId: String) {
        dao.deleteMessage(messageId)
    }

    suspend fun deleteMessagesFrom(conversationId: String, position: Long) {
        dao.deleteMessagesFrom(conversationId, position)
    }

    suspend fun deleteConversation(conversationId: String) {
        dao.deleteConversation(conversationId)
    }

    suspend fun renameConversation(conversationId: String, title: String) {
        dao.renameConversation(conversationId, title)
    }

    suspend fun setPinned(conversationId: String, pinned: Boolean) {
        dao.setPinned(conversationId, pinned)
    }

    private fun Message.toEntity(conversationId: String) = MessageEntity(
        id = id,
        conversationId = conversationId,
        role = role.apiValue,
        content = content,
        position = position,
        timestamp = timestamp,
        reasoning = reasoning,
        reasoningMs = reasoningMs,
    )
}
