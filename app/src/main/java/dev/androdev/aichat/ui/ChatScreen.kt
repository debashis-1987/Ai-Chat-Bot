package dev.androdev.aichat.ui

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.androdev.aichat.ChatUiState
import dev.androdev.aichat.R
import dev.androdev.aichat.data.Message
import dev.androdev.aichat.data.Role
import dev.androdev.aichat.ui.components.AssistantTurn
import dev.androdev.aichat.ui.components.ChatHeader
import dev.androdev.aichat.ui.components.Composer
import dev.androdev.aichat.ui.components.EmptyChatState
import dev.androdev.aichat.ui.components.UserTurn
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onNewChat: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDrawer: () -> Unit,
    onShare: () -> Unit,
    onRegenerate: () -> Unit,
    onRetry: () -> Unit,
    onEditMessage: (String, String) -> Unit,
    onUndoDelete: () -> Unit,
    onErrorShown: () -> Unit,
    onNoticeShown: () -> Unit,
) {
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    var editingMessage by remember { mutableStateOf<Message?>(null) }

    LaunchedEffect(state.error) {
        val error = state.error ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = error,
            actionLabel = "Try again",
            withDismissAction = true,
            duration = SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed) onRetry()
        onErrorShown()
    }

    LaunchedEffect(state.notice) {
        val notice = state.notice ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = notice,
            actionLabel = if (state.canUndoDelete) "Undo" else null,
            duration = if (state.canUndoDelete) SnackbarDuration.Long else SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) onUndoDelete()
        onNoticeShown()
    }

    val isAtBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible >= info.totalItemsCount - 2
        }
    }
    val lastContentLength = state.messages.lastOrNull()?.content?.length ?: 0
    LaunchedEffect(state.messages.size, lastContentLength) {
        if (state.messages.isNotEmpty() && isAtBottom) {
            listState.scrollToItem(state.messages.lastIndex, scrollOffset = 100_000)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ChatHeader(
                title = state.conversationTitle,
                subtitle = when {
                    !state.settings.isReady -> "Provider not configured"
                    state.isStreaming -> "${state.settings.model} · Responding"
                    else -> state.settings.model.ifBlank { "No model selected" }
                },
                streaming = state.isStreaming,
                canShare = state.canShare,
                onOpenDrawer = onOpenDrawer,
                onShare = onShare,
                onNewChat = onNewChat,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (state.messages.isEmpty()) {
                    EmptyChatState(
                        configured = state.settings.isReady,
                        onOpenSettings = onOpenSettings,
                        onSuggestion = onInputChange,
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        items(state.messages, key = { it.id }) { message ->
                            val isLast = message.id == state.messages.last().id
                            Box(modifier = Modifier.animateItem()) {
                                if (message.role == Role.USER) {
                                    UserTurn(
                                        message = message,
                                        onEdit = { editingMessage = message },
                                    )
                                } else {
                                    AssistantTurn(
                                        message = message,
                                        streaming = state.isStreaming && isLast,
                                        showActions = isLast && state.canRegenerate,
                                        onCopy = {
                                            clipboard?.setPrimaryClip(
                                                ClipData.newPlainText("Message", message.content),
                                            )
                                        },
                                        onRegenerate = onRegenerate,
                                    )
                                }
                            }
                        }
                    }
                }

                ScrollToBottomButton(
                    visible = state.messages.isNotEmpty() && !isAtBottom,
                    onClick = {
                        scope.launch {
                            listState.animateScrollToItem(state.messages.lastIndex, scrollOffset = 100_000)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 8.dp),
                )
            }

            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 10.dp),
            ) {
                if (!state.settings.isReady) {
                    SetupHint(onOpenSettings = onOpenSettings)
                }
                Composer(
                    value = state.input,
                    isStreaming = state.isStreaming,
                    canSend = state.settings.isReady,
                    onValueChange = onInputChange,
                    onSend = onSend,
                    onStop = onStop,
                )
            }
        }
    }

    editingMessage?.let { message ->
        EditMessageDialog(
            message = message,
            onDismiss = { editingMessage = null },
            onSave = { updated ->
                onEditMessage(message.id, updated)
                editingMessage = null
            },
        )
    }
}

@Composable
private fun ScrollToBottomButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(140)) + scaleIn(initialScale = 0.9f, animationSpec = tween(140)),
        exit = fadeOut(tween(110)) + scaleOut(targetScale = 0.9f, animationSpec = tween(110)),
    ) {
        FilledIconButton(
            onClick = onClick,
            shape = androidx.compose.foundation.shape.CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_down),
                contentDescription = "Scroll to latest message",
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun SetupHint(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Add a provider to start chatting.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onOpenSettings) {
            Text("Set up", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun EditMessageDialog(
    message: Message,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by rememberSaveable(message.id) { mutableStateOf(message.content) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text("Edit message", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    minLines = 3,
                    maxLines = 8,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Sending replaces this message and regenerates the reply.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) {
                Text("Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
