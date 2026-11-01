package dev.androdev.aichat.data

import android.content.Context
import androidx.core.content.edit

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("ai_settings", Context.MODE_PRIVATE)
    private val secrets = SecretStore(context)

    fun load(): AiSettings {
        val defaults = AiSettings()
        return AiSettings(
            baseUrl = prefs.getString(KEY_BASE_URL, defaults.baseUrl) ?: defaults.baseUrl,
            apiKey = loadApiKey(),
            model = prefs.getString(KEY_MODEL, defaults.model) ?: defaults.model,
            systemPrompt = prefs.getString(KEY_SYSTEM_PROMPT, defaults.systemPrompt) ?: defaults.systemPrompt,
            maxContextTokens = prefs.getInt(KEY_MAX_CONTEXT_TOKENS, defaults.maxContextTokens),
            thinkingEnabled = prefs.getBoolean(KEY_THINKING_ENABLED, defaults.thinkingEnabled),
            thinkingLevel = prefs.getString(KEY_THINKING_LEVEL, null)
                ?.let { name -> ThinkingLevel.entries.firstOrNull { it.name == name } }
                ?: defaults.thinkingLevel,
        )
    }

    fun save(settings: AiSettings) {
        secrets.saveApiKey(settings.apiKey)
        prefs.edit {
            putString(KEY_BASE_URL, settings.baseUrl)
            putString(KEY_MODEL, settings.model)
            putString(KEY_SYSTEM_PROMPT, settings.systemPrompt)
            putInt(KEY_MAX_CONTEXT_TOKENS, settings.maxContextTokens)
            putBoolean(KEY_THINKING_ENABLED, settings.thinkingEnabled)
            putString(KEY_THINKING_LEVEL, settings.thinkingLevel.name)
        }
    }

    private fun loadApiKey(): String {
        val encrypted = secrets.loadApiKey()
        if (encrypted.isNotEmpty()) return encrypted

        val legacy = prefs.getString(KEY_LEGACY_API_KEY, null)
        if (legacy.isNullOrEmpty()) return ""

        secrets.saveApiKey(legacy)
        prefs.edit { remove(KEY_LEGACY_API_KEY) }
        return legacy
    }

    private companion object {
        const val KEY_BASE_URL = "base_url"
        const val KEY_LEGACY_API_KEY = "api_key"
        const val KEY_MODEL = "model"
        const val KEY_SYSTEM_PROMPT = "system_prompt"
        const val KEY_MAX_CONTEXT_TOKENS = "max_context_tokens"
        const val KEY_THINKING_ENABLED = "thinking_enabled"
        const val KEY_THINKING_LEVEL = "thinking_level"
    }
}
