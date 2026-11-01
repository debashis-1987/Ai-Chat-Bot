package dev.androdev.aichat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dev.androdev.aichat.ui.ChatApp
import dev.androdev.aichat.ui.theme.AiChatTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels { ChatViewModel.factory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AiChatTheme {
                ChatApp(viewModel)
            }
        }
    }
}
