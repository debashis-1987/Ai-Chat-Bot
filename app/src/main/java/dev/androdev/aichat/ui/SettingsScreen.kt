package dev.androdev.aichat.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.androdev.aichat.R
import dev.androdev.aichat.data.AiSettings
import dev.androdev.aichat.data.ThinkingLevel

private data class ProviderPreset(val label: String, val baseUrl: String)

private val providerPresets = listOf(
    ProviderPreset("OpenAI", "https://api.openai.com/v1"),
    ProviderPreset("OpenRouter", "https://openrouter.ai/api/v1"),
    ProviderPreset("Groq", "https://api.groq.com/openai/v1"),
    ProviderPreset("Together", "https://api.together.xyz/v1"),
    ProviderPreset("Ollama", "http://10.0.2.2:11434/v1"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AiSettings,
    models: List<String>,
    isLoadingModels: Boolean,
    modelsError: String?,
    onLoadModels: (String, String) -> Unit,
    onSave: (AiSettings) -> Unit,
    onBack: () -> Unit,
) {
    var baseUrl by rememberSaveable { mutableStateOf(settings.baseUrl) }
    var apiKey by rememberSaveable { mutableStateOf(settings.apiKey) }
    var model by rememberSaveable { mutableStateOf(settings.model) }
    var systemPrompt by rememberSaveable { mutableStateOf(settings.systemPrompt) }
    var apiKeyVisible by rememberSaveable { mutableStateOf(false) }
    var maxContextTokens by rememberSaveable { mutableStateOf(settings.maxContextTokens.toString()) }
    var thinkingEnabled by rememberSaveable { mutableStateOf(settings.thinkingEnabled) }
    var thinkingLevel by rememberSaveable { mutableStateOf(settings.thinkingLevel) }
    var modelsMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SettingsHeader(onBack = onBack) },
        bottomBar = {
            SaveBar(
                enabled = baseUrl.isNotBlank() && model.isNotBlank(),
                onSave = {
                    onSave(
                        AiSettings(
                            baseUrl = baseUrl,
                            apiKey = apiKey,
                            model = model,
                            systemPrompt = systemPrompt,
                            maxContextTokens = maxContextTokens.toIntOrNull() ?: settings.maxContextTokens,
                            thinkingEnabled = thinkingEnabled,
                            thinkingLevel = thinkingLevel,
                        ),
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsSection(title = "Provider") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    providerPresets.forEach { preset ->
                        FilterChip(
                            selected = baseUrl.trim() == preset.baseUrl,
                            onClick = { baseUrl = preset.baseUrl },
                            label = { Text(preset.label, style = MaterialTheme.typography.labelMedium) },
                            shape = RoundedCornerShape(10.dp),
                        )
                    }
                }
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("API base URL") },
                    supportingText = {
                        Text("OpenAI-compatible endpoint. For Ollama on this computer use http://10.0.2.2:11434/v1")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API key") },
                    supportingText = { Text("Encrypted with the Android Keystore on this device.") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (apiKeyVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        TextButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                            Text(if (apiKeyVisible) "Hide" else "Show")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SectionDivider()

            SettingsSection(title = "Model") {
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model") },
                    supportingText = {
                        Text(
                            text = modelsError ?: "For example gpt-4o-mini, gpt-4.1-mini or llama3.2.",
                            color = if (modelsError != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { onLoadModels(baseUrl, apiKey) },
                        enabled = !isLoadingModels,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        if (isLoadingModels) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Load models")
                        }
                    }
                    if (models.isNotEmpty()) {
                        Box {
                            OutlinedButton(
                                onClick = { modelsMenuOpen = true },
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("Choose (${models.size})")
                            }
                            DropdownMenu(
                                expanded = modelsMenuOpen,
                                onDismissRequest = { modelsMenuOpen = false },
                            ) {
                                models.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text(item, style = MaterialTheme.typography.bodyMedium) },
                                        onClick = {
                                            model = item
                                            modelsMenuOpen = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            SectionDivider()

            SettingsSection(title = "Thinking") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = thinkingEnabled,
                            role = Role.Switch,
                            onValueChange = { thinkingEnabled = it },
                        )
                        .semantics(mergeDescendants = true) { },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Thinking mode",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Sends reasoning_effort with each request. Use a model that supports reasoning.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(checked = thinkingEnabled, onCheckedChange = null)
                }
                if (thinkingEnabled) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThinkingLevel.entries.forEach { level ->
                            FilterChip(
                                selected = thinkingLevel == level,
                                onClick = { thinkingLevel = level },
                                label = { Text(level.label, style = MaterialTheme.typography.labelMedium) },
                                shape = RoundedCornerShape(10.dp),
                            )
                        }
                    }
                }
            }

            SectionDivider()

            SettingsSection(title = "Conversation") {
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text("System prompt") },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = maxContextTokens,
                    onValueChange = { input -> maxContextTokens = input.filter { it.isDigit() } },
                    label = { Text("Context budget") },
                    suffix = { Text("tokens") },
                    supportingText = {
                        Text("Older messages are dropped from the request once a chat exceeds this. Set 0 to always send the full history.")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Text(
                text = "Requests go directly to your provider. Your API key is encrypted with the Android Keystore and never leaves this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(14.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        modifier = Modifier.padding(horizontal = 20.dp),
    )
}

@Composable
private fun SaveBar(
    enabled: Boolean,
    onSave: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Button(
                    onClick = onSave,
                    enabled = enabled,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                ) {
                    Text("Save changes", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
