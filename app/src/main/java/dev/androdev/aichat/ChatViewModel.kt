package dev.androdev.aichat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.androdev.aichat.data.AiSettings
import dev.androdev.aichat.data.ChatDatabase
import dev.androdev.aichat.data.ChatRepository
import dev.androdev.aichat.data.ChatService
import dev.androdev.aichat.data.ConversationEntity
import dev.androdev.aichat.data.ContextWindow
import dev.androdev.aichat.data.DraftStore
import dev.androdev.aichat.data.Message
import dev.androdev.aichat.data.Role
import dev.androdev.aichat.data.ServiceLocator
import dev.androdev.aichat.data.SettingsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class ChatUiState(
    val conversations: List<ConversationEntity> = emptyList(),
    val conversationId: String? = null,
    val messages: List<Message> = emptyList(),
    val input: String = "",
    val isStreaming: Boolean = false,
    val error: String? = null,
    val settings: AiSettings = AiSettings(),
    val models: List<String> = emptyList(),
    val isLoadingModels: Boolean = false,
    val modelsError: String? = null,
    val notice: String? = null,
    val canUndoDelete: Boolean = false,
) {
    val conversationTitle: String
        get() = conversations.firstOrNull { it.id == conversationId }?.title ?: "New chat"

    val canRegenerate: Boolean
        get() = !isStreaming && conversationId != null && messages.lastOrNull()?.role == Role.ASSISTANT

    val canShare: Boolean
        get() = messages.isNotEmpty()
}

class ChatViewModel(
    private val settingsStore: SettingsStore,
    private val repository: ChatRepository,
    private val client: ChatService,
    private val draftStore: DraftStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState(settings = settingsStore.load()))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamingJob: Job? = null
    private var isSending = false
    private var initialConversationLoaded = false
    private var deletedConversation: Pair<ConversationEntity, List<Message>>? = null

    init {
        viewModelScope.launch {
            repository.conversations().collect { conversations ->
                _uiState.update { it.copy(conversations = conversations) }
                if (!initialConversationLoaded) {
                    initialConversationLoaded = true
                    conversations.firstOrNull()?.let { openConversation(it.id) }
                }
            }
        }
    }

    fun onInputChange(value: String) {
        _uiState.update { it.copy(input = value, error = null) }
        draftStore.save(currentDraftKey(), value)
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun saveSettings(settings: AiSettings) {
        val normalized = settings.copy(
            baseUrl = settings.baseUrl.trim().trimEnd('/'),
            apiKey = settings.apiKey.trim(),
            model = settings.model.trim(),
            maxContextTokens = settings.maxContextTokens.coerceAtLeast(0),
        )
        settingsStore.save(normalized)
        _uiState.update { it.copy(settings = normalized, error = null) }
    }

    fun loadModels(baseUrl: String, apiKey: String) {
        if (baseUrl.isBlank()) {
            _uiState.update { it.copy(modelsError = "Enter a base URL first.") }
            return
        }
        if (_uiState.value.isLoadingModels) return

        _uiState.update { it.copy(isLoadingModels = true, modelsError = null) }
        viewModelScope.launch {
            try {
                val models = client.fetchModels(AiSettings(baseUrl = baseUrl, apiKey = apiKey))
                _uiState.update {
                    it.copy(
                        models = models,
                        isLoadingModels = false,
                        modelsError = if (models.isEmpty()) "The provider returned no models." else null,
                    )
                }
            } catch (e: CancellationException) {
                _uiState.update { it.copy(isLoadingModels = false) }
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingModels = false,
                        models = emptyList(),
                        modelsError = e.message ?: "Could not load models.",
                    )
                }
            }
        }
    }

    fun selectConversation(conversationId: String) {
        if (_uiState.value.conversationId == conversationId) return
        streamingJob?.cancel()
        streamingJob = null
        viewModelScope.launch { openConversation(conversationId) }
    }

    private suspend fun openConversation(conversationId: String) {
        val messages = withContext(Dispatchers.IO) { repository.messages(conversationId) }
        _uiState.update {
            it.copy(
                conversationId = conversationId,
                messages = messages,
                input = draftStore.load(conversationId),
                isStreaming = false,
                error = null,
            )
        }
    }

    fun newChat() {
        streamingJob?.cancel()
        streamingJob = null
        _uiState.update {
            it.copy(
                conversationId = null,
                messages = emptyList(),
                input = draftStore.load(DraftStore.NEW_CHAT_KEY),
                isStreaming = false,
                error = null,
            )
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            streamingJob?.cancelAndJoin()
            streamingJob = null
            val snapshot = withContext(Dispatchers.IO) {
                val conversation = repository.conversation(conversationId) ?: return@withContext null
                val messages = repository.messages(conversationId)
                repository.deleteConversation(conversationId)
                conversation to messages
            }
            deletedConversation = snapshot
            draftStore.clear(conversationId)
            if (_uiState.value.conversationId == conversationId) {
                _uiState.update {
                    it.copy(conversationId = null, messages = emptyList(), input = "", isStreaming = false, error = null)
                }
            }
            _uiState.update { it.copy(notice = "Chat deleted", canUndoDelete = snapshot != null) }
        }
    }

    fun undoDelete() {
        val snapshot = deletedConversation ?: return
        deletedConversation = null
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.restoreConversation(snapshot.first, snapshot.second) }
            _uiState.update { it.copy(notice = null, canUndoDelete = false) }
        }
    }

    fun dismissNotice() {
        deletedConversation = null
        _uiState.update { it.copy(notice = null, canUndoDelete = false) }
    }

    fun retry() {
        val state = _uiState.value
        if (state.isStreaming || isSending) return
        val last = state.messages.lastOrNull() ?: return
        when (last.role) {
            Role.ASSISTANT -> regenerate()
            Role.USER -> {
                val conversationId = state.conversationId ?: return
                isSending = true
                viewModelScope.launch {
                    try {
                        startStreaming(conversationId)
                    } finally {
                        isSending = false
                    }
                }
            }
        }
    }

    fun renameConversation(conversationId: String, title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.renameConversation(conversationId, trimmed) }
        }
    }

    fun togglePin(conversationId: String) {
        val conversation = _uiState.value.conversations.firstOrNull { it.id == conversationId } ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.setPinned(conversationId, !conversation.pinned) }
        }
    }

    fun send() {
        val state = _uiState.value
        val text = state.input.trim()
        if (text.isEmpty() || state.isStreaming || isSending) return
        if (!state.settings.isReady) {
            _uiState.update { it.copy(error = "Add your API key in settings first.") }
            return
        }

        val userMessage = Message(
            role = Role.USER,
            content = text,
            position = nextPosition(state.messages),
        )
        draftStore.clear(DraftStore.NEW_CHAT_KEY)
        state.conversationId?.let { draftStore.clear(it) }
        _uiState.update { it.copy(input = "", messages = it.messages + userMessage, error = null) }

        isSending = true
        viewModelScope.launch {
            try {
                val conversationId = ensureConversation(userMessage.content)
                withContext(Dispatchers.IO) { repository.saveMessage(conversationId, userMessage) }
                startStreaming(conversationId)
            } finally {
                isSending = false
            }
        }
    }

    fun regenerate() {
        val state = _uiState.value
        if (state.isStreaming || isSending) return
        val conversationId = state.conversationId ?: return
        val last = state.messages.lastOrNull() ?: return
        if (last.role != Role.ASSISTANT) return

        val history = state.messages.dropLast(1)
        if (history.lastOrNull()?.role != Role.USER) return

        _uiState.update { it.copy(messages = history, error = null) }
        isSending = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { repository.deleteMessage(last.id) }
                startStreaming(conversationId)
            } finally {
                isSending = false
            }
        }
    }

    fun editMessage(messageId: String, newContent: String) {
        val state = _uiState.value
        val text = newContent.trim()
        if (text.isEmpty() || state.isStreaming || isSending) return
        val conversationId = state.conversationId ?: return
        val target = state.messages.firstOrNull { it.id == messageId } ?: return
        if (target.role != Role.USER) return

        val index = state.messages.indexOf(target)
        if (index == -1) return
        val edited = target.copy(content = text)
        val kept = state.messages.subList(0, index)

        _uiState.update { it.copy(messages = kept + edited, error = null) }
        isSending = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.deleteMessagesFrom(conversationId, target.position)
                    repository.saveMessage(conversationId, edited)
                    if (index == 0) repository.renameConversation(conversationId, titleFrom(text))
                }
                startStreaming(conversationId)
            } finally {
                isSending = false
            }
        }
    }

    fun stop() {
        streamingJob?.cancel()
        streamingJob = null
    }

    private fun currentDraftKey(): String =
        _uiState.value.conversationId ?: DraftStore.NEW_CHAT_KEY

    private suspend fun ensureConversation(titleSeed: String): String {
        val existing = _uiState.value.conversationId
        if (existing != null) return existing
        val conversationId = UUID.randomUUID().toString()
        withContext(Dispatchers.IO) {
            repository.createConversation(conversationId, titleFrom(titleSeed))
        }
        _uiState.update { it.copy(conversationId = conversationId) }
        return conversationId
    }

    private fun titleFrom(text: String): String =
        text.lineSequence().first().trim().take(48).ifBlank { "New chat" }

    private fun nextPosition(messages: List<Message>): Long =
        (messages.maxOfOrNull { it.position } ?: 0L) + 1L

    private fun startStreaming(conversationId: String) {
        val state = _uiState.value
        val history = state.messages
        if (history.isEmpty()) return

        val reply = Message(
            role = Role.ASSISTANT,
            content = "",
            position = nextPosition(history),
        )
        _uiState.update { it.copy(messages = history + reply, isStreaming = true, error = null) }

        val settings = state.settings
        streamingJob = viewModelScope.launch {
            var reasoningStartedAt: Long? = null
            var reasoningMs = 0L
            try {
                val context = ContextWindow.fit(history, settings.maxContextTokens, settings.systemPrompt)
                client.streamReply(settings, context) { delta ->
                    val now = System.currentTimeMillis()
                    if (delta.reasoning.isNotEmpty() && reasoningStartedAt == null) {
                        reasoningStartedAt = now
                    }
                    if (delta.content.isNotEmpty() && reasoningMs == 0L) {
                        reasoningStartedAt?.let { reasoningMs = now - it }
                    }
                    _uiState.update { current ->
                        current.copy(
                            messages = current.messages.map { message ->
                                if (message.id == reply.id) {
                                    message.copy(
                                        content = message.content + delta.content,
                                        reasoning = message.reasoning + delta.reasoning,
                                        reasoningMs = reasoningMs,
                                    )
                                } else {
                                    message
                                }
                            },
                        )
                    }
                }
                val elapsed = if (reasoningMs == 0L) {
                    reasoningStartedAt?.let { System.currentTimeMillis() - it } ?: 0L
                } else {
                    reasoningMs
                }
                _uiState.update { current ->
                    current.copy(
                        isStreaming = false,
                        messages = current.messages.map { message ->
                            if (message.id == reply.id) message.copy(reasoningMs = elapsed) else message
                        },
                    )
                }
                val finished = _uiState.value.messages.firstOrNull { it.id == reply.id }
                if (finished != null && finished.content.isNotBlank()) {
                    withContext(Dispatchers.IO) { repository.saveMessage(conversationId, finished) }
                }
            } catch (e: CancellationException) {
                _uiState.update { it.copy(isStreaming = false) }
                savePartial(conversationId, reply.id)
                throw e
            } catch (e: Exception) {
                _uiState.update { current ->
                    current.copy(
                        isStreaming = false,
                        error = e.message ?: "Something went wrong.",
                        messages = current.messages.filterNot { it.id == reply.id && it.content.isBlank() },
                    )
                }
                savePartial(conversationId, reply.id)
            }
        }
    }

    private suspend fun savePartial(conversationId: String, messageId: String) {
        val partial = _uiState.value.messages.firstOrNull { it.id == messageId } ?: return
        if (partial.content.isBlank()) return
        withContext(NonCancellable + Dispatchers.IO) {
            runCatching { repository.saveMessage(conversationId, partial) }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return viewModelFactory {
                initializer {
                    ChatViewModel(
                        settingsStore = SettingsStore(appContext),
                        repository = ChatRepository(ChatDatabase.get(appContext).chatDao()),
                        client = ServiceLocator.chatService,
                        draftStore = DraftStore(appContext),
                    )
                }
            }
        }
    }
}
