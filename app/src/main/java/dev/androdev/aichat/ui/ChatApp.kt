package dev.androdev.aichat.ui

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.androdev.aichat.ChatViewModel
import dev.androdev.aichat.data.Message
import dev.androdev.aichat.data.Role
import kotlinx.coroutines.launch

@Composable
fun ChatApp(viewModel: ChatViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    BackHandler(enabled = showSettings) {
        showSettings = false
    }
    BackHandler(enabled = !showSettings && drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    AnimatedContent(
        targetState = showSettings,
        transitionSpec = {
            (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 12 })
                .togetherWith(fadeOut(tween(140)) + slideOutHorizontally(tween(160)) { -it / 12 })
        },
        label = "screens",
    ) { settingsVisible ->
        if (settingsVisible) {
            SettingsScreen(
                settings = state.settings,
                models = state.models,
                isLoadingModels = state.isLoadingModels,
                modelsError = state.modelsError,
                onLoadModels = viewModel::loadModels,
                onSave = { settings ->
                    viewModel.saveSettings(settings)
                    showSettings = false
                },
                onBack = { showSettings = false },
            )
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ConversationDrawer(
                        conversations = state.conversations,
                        currentId = state.conversationId,
                        onSelect = { conversationId ->
                            viewModel.selectConversation(conversationId)
                            scope.launch { drawerState.close() }
                        },
                        onRename = viewModel::renameConversation,
                        onTogglePin = viewModel::togglePin,
                        onDelete = { conversationId ->
                            viewModel.deleteConversation(conversationId)
                            scope.launch { drawerState.close() }
                        },
                        onNewChat = {
                            viewModel.newChat()
                            scope.launch { drawerState.close() }
                        },
                        onOpenSettings = {
                            showSettings = true
                            scope.launch { drawerState.close() }
                        },
                    )
                },
            ) {
                ChatScreen(
                    state = state,
                    onInputChange = viewModel::onInputChange,
                    onSend = viewModel::send,
                    onStop = viewModel::stop,
                    onNewChat = viewModel::newChat,
                    onOpenSettings = { showSettings = true },
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onShare = {
                        shareChat(context, state.conversationTitle, state.messages)
                    },
                    onRegenerate = viewModel::regenerate,
                    onRetry = viewModel::retry,
                    onEditMessage = viewModel::editMessage,
                    onUndoDelete = viewModel::undoDelete,
                    onErrorShown = viewModel::dismissError,
                    onNoticeShown = viewModel::dismissNotice,
                )
            }
        }
    }
}

private fun shareChat(context: Context, title: String, messages: List<Message>) {
    val text = buildString {
        append("# ").append(title).append("\n\n")
        messages.forEach { message ->
            append(if (message.role == Role.USER) "**You:** " else "**AI:** ")
            append(message.content)
            append("\n\n")
        }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share chat"))
}
