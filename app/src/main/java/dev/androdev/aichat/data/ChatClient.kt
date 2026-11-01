package dev.androdev.aichat.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class ChatException(message: String) : Exception(message)

internal fun parseStreamDelta(data: String): StreamDelta {
    val choices = JSONObject(data).optJSONArray("choices") ?: return StreamDelta()
    val delta = choices.optJSONObject(0)?.optJSONObject("delta") ?: return StreamDelta()
    val content = if (delta.isNull("content")) "" else delta.optString("content")
    val reasoningContent = if (delta.isNull("reasoning_content")) "" else delta.optString("reasoning_content")
    val reasoning = if (delta.isNull("reasoning")) "" else delta.optString("reasoning")
    return StreamDelta(content = content, reasoning = reasoningContent.ifEmpty { reasoning })
}

internal fun buildRequestBody(settings: AiSettings, history: List<Message>): JSONObject {
    val messages = JSONArray()
    if (settings.systemPrompt.isNotBlank()) {
        messages.put(JSONObject().put("role", "system").put("content", settings.systemPrompt))
    }
    history.forEach { message ->
        messages.put(JSONObject().put("role", message.role.apiValue).put("content", message.content))
    }
    return JSONObject()
        .put("model", settings.model)
        .put("stream", true)
        .put("messages", messages)
        .apply {
            if (settings.thinkingEnabled) {
                put("reasoning_effort", settings.thinkingLevel.wireValue)
            }
        }
}

class ChatClient : ChatService {

    override suspend fun streamReply(
        settings: AiSettings,
        history: List<Message>,
        onDelta: (StreamDelta) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val endpoint = settings.baseUrl.trim().trimEnd('/') + "/chat/completions"
        val body = buildRequestBody(settings, history)

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 120_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "text/event-stream")
            if (settings.apiKey.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer ${settings.apiKey}")
            }
        }
        currentCoroutineContext()[Job]?.invokeOnCompletion { connection.disconnect() }

        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val status = connection.responseCode
            if (status !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw ChatException(errorMessage(status, errorBody))
            }

            connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val line = reader.readLine() ?: break
                    if (!line.startsWith("data:")) continue
                    val data = line.removePrefix("data:").trim()
                    if (data == "[DONE]") break
                    val delta = parseStreamDelta(data)
                    if (delta.content.isNotEmpty() || delta.reasoning.isNotEmpty()) onDelta(delta)
                }
            }
        } catch (e: IOException) {
            currentCoroutineContext().ensureActive()
            throw ChatException(ioMessage(e))
        } finally {
            connection.disconnect()
        }
    }

    override suspend fun fetchModels(settings: AiSettings): List<String> = withContext(Dispatchers.IO) {
        val endpoint = settings.baseUrl.trim().trimEnd('/') + "/models"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
            if (settings.apiKey.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer ${settings.apiKey}")
            }
        }

        try {
            val status = connection.responseCode
            val body = if (status in 200..299) {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            }
            if (status !in 200..299) {
                throw ChatException(errorMessage(status, body))
            }
            val data = JSONObject(body).optJSONArray("data") ?: return@withContext emptyList()
            (0 until data.length())
                .mapNotNull { index -> data.optJSONObject(index)?.optString("id")?.takeIf { it.isNotBlank() } }
                .sorted()
        } catch (e: IOException) {
            throw ChatException(ioMessage(e))
        } finally {
            connection.disconnect()
        }
    }

    private fun ioMessage(e: IOException): String = when (e) {
        is java.net.SocketTimeoutException -> "The provider took too long to respond. Try again."
        is java.net.UnknownHostException -> "That provider address could not be found. Check the base URL."
        is java.net.ConnectException -> "Could not reach the provider. Check your connection and base URL."
        else -> "The connection dropped before the reply finished. Try again."
    }

    private fun errorMessage(status: Int, body: String): String {
        val detail = runCatching {
            JSONObject(body).optJSONObject("error")?.optString("message").orEmpty()
        }.getOrDefault("")
        val hint = when (status) {
            401, 403 -> "Authentication failed. Check your API key."
            404 -> "Endpoint not found. Check the base URL and model name."
            429 -> "Rate limited or out of quota. Try again shortly."
            in 500..599 -> "The AI provider had a server error. Try again."
            else -> "Request failed."
        }
        return "HTTP $status: ${detail.ifBlank { hint }}"
    }
}
