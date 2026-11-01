package dev.androdev.aichat.data

import android.content.Context
import androidx.core.content.edit

class DraftStore(context: Context) {
    private val prefs = context.getSharedPreferences("ai_drafts", Context.MODE_PRIVATE)

    fun load(key: String): String = prefs.getString(key, "").orEmpty()

    fun save(key: String, text: String) {
        if (text.isEmpty()) {
            clear(key)
        } else {
            prefs.edit { putString(key, text) }
        }
    }

    fun clear(key: String) {
        prefs.edit { remove(key) }
    }

    companion object {
        const val NEW_CHAT_KEY = "__new_chat__"
    }
}
