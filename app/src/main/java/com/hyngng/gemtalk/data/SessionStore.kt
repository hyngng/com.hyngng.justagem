package com.hyngng.gemtalk.data

import android.content.Context
import com.hyngng.gemtalk.ai.CURRENT_PROMPT_SETTINGS_VERSION
import com.hyngng.gemtalk.ai.DEFAULT_SYSTEM_INSTRUCTIONS
import com.hyngng.gemtalk.ai.migratePrePrompt
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * JSON file store for conversation transcripts.
 *
 * Deliberately not Room: the whole state is a few kilobytes, the write points
 * are user-visible events (send / turn end / session switch), and a plain file
 * is easy to inspect when a device misbehaves. Writes are atomic (tmp + rename)
 * so a process death mid-save cannot truncate every conversation.
 */
class SessionStore(context: Context) {

    private val file = File(context.applicationContext.filesDir, FILE_NAME)
    private val lock = Any()

    fun load(): PersistedState = synchronized(lock) {
        if (!file.exists()) return@synchronized PersistedState()
        runCatching {
            val root = JSONObject(file.readText(Charsets.UTF_8))
            val sessions = mutableListOf<ChatSession>()
            val array = root.optJSONArray(KEY_SESSIONS) ?: JSONArray()
            for (i in 0 until array.length()) {
                val obj = runCatching { array.getJSONObject(i) }.getOrNull() ?: continue
                val messages = mutableListOf<ChatMessage>()
                val messageArray = obj.optJSONArray(KEY_MESSAGES) ?: JSONArray()
                for (j in 0 until messageArray.length()) {
                    // One malformed transcript entry must not make the whole
                    // session appear empty during restore.
                    val m = runCatching { messageArray.getJSONObject(j) }.getOrNull() ?: continue
                    val text = m.optString("text")
                    val thoughtText = m.optString("thoughtText", "")
                    val attachmentUris = m.optJSONArray("attachmentUris")?.let { items ->
                        buildList { for (k in 0 until items.length()) items.optString(k).takeIf(String::isNotBlank)?.let(::add) }
                    }.orEmpty()
                    val photoUrisArray = m.optJSONArray("photoUris")
                    val photoUris = mutableListOf<String>()
                    if (photoUrisArray != null) {
                        for (k in 0 until photoUrisArray.length()) {
                            val uri = photoUrisArray.optString(k)
                            if (uri.isNotBlank()) photoUris.add(uri)
                        }
                    }
                    if (text.isBlank() && thoughtText.isBlank() && photoUris.isEmpty() && attachmentUris.isEmpty()) continue
                    messages += ChatMessage(
                        id = m.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                        role = runCatching {
                            ChatRole.valueOf(m.optString("role", ChatRole.USER.name))
                        }.getOrDefault(ChatRole.USER),
                        text = text,
                        createdAt = m.optLong("createdAt", System.currentTimeMillis()),
                        isError = m.optBoolean("isError", false),
                        thinkingEnabled = m.optBoolean("thinkingEnabled", false),
                        thinkingObserved = m.optBoolean("thinkingObserved", false),
                        thinkingFinished = m.optBoolean("thinkingFinished", false),
                        thinkingUnavailable = m.optBoolean("thinkingUnavailable", false),
                        thoughtText = thoughtText,
                        attachmentUris = attachmentUris,
                        photoUris = photoUris,
                        photoSearchContext = m.optString("photoSearchContext", ""),
                    )
                }
                runCatching {
                    ChatSession(
                        id = obj.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                        title = obj.optString("title"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                        messages = messages,
                    )
                }.getOrNull()?.let(sessions::add)
            }
            val savedVersion = root.optInt(KEY_VERSION, 0)
            val state = PersistedState(
                sessions = sessions.sortedByDescending { it.updatedAt },
                activeId = root.optString(KEY_ACTIVE).ifBlank { null },
                hapticsEnabled = root.optBoolean(KEY_HAPTICS, true),
                soundsEnabled = root.optBoolean(KEY_SOUNDS, true),
                warmupEnabled = root.optBoolean(KEY_WARMUP_ENABLED, true),
                prePrompt = migratePrePrompt(root.optString(KEY_PRE_PROMPT, ""), savedVersion),
                thinkingEnabled = root.optBoolean(KEY_THINKING, false),
                temperature = root.optDouble(KEY_TEMPERATURE, 0.7).toFloat().coerceIn(0f, 1f),
                topK = root.optInt(KEY_TOP_K, 40).coerceIn(1, 128),
                maxOutputTokens = root.optInt(KEY_MAX_OUTPUT_TOKENS, 900).coerceIn(128, 4096),
            )
            if (savedVersion < CURRENT_PROMPT_SETTINGS_VERSION) save(state)
            state
        }.onFailure {
            runCatching { file.renameTo(File(file.parentFile, "$FILE_NAME.corrupt")) }
        }.getOrDefault(PersistedState())
    }

    fun save(state: PersistedState): Boolean = synchronized(lock) {
        runCatching {
            val root = JSONObject().apply {
                put(KEY_VERSION, CURRENT_PROMPT_SETTINGS_VERSION)
                put(KEY_ACTIVE, state.activeId ?: JSONObject.NULL)
                put(KEY_HAPTICS, state.hapticsEnabled)
                put(KEY_SOUNDS, state.soundsEnabled)
                put(KEY_WARMUP_ENABLED, state.warmupEnabled)
                put(KEY_PRE_PROMPT, state.prePrompt)
                put(KEY_THINKING, state.thinkingEnabled)
                put(KEY_TEMPERATURE, state.temperature.toDouble())
                put(KEY_TOP_K, state.topK)
                put(KEY_MAX_OUTPUT_TOKENS, state.maxOutputTokens)
                put(KEY_SESSIONS, JSONArray().apply {
                    state.sessions.forEach { session ->
                        put(JSONObject().apply {
                            put("id", session.id)
                            put("title", session.title)
                            put("createdAt", session.createdAt)
                            put("updatedAt", session.updatedAt)
                            put(KEY_MESSAGES, JSONArray().apply {
                                session.messages.forEach { message ->
                                    put(JSONObject().apply {
                                        put("id", message.id)
                                        put("role", message.role.name)
                                        put("text", message.text)
                                        put("createdAt", message.createdAt)
                                        put("isError", message.isError)
                                        put("thinkingEnabled", message.thinkingEnabled)
                                        put("thinkingObserved", message.thinkingObserved)
                                        put("thinkingFinished", message.thinkingFinished)
                                        put("thinkingUnavailable", message.thinkingUnavailable)
                                        put("thoughtText", message.thoughtText)
                                        if (message.attachmentUris.isNotEmpty()) {
                                            put("attachmentUris", JSONArray().apply { message.attachmentUris.forEach { put(it) } })
                                        }
                                        if (message.photoUris.isNotEmpty()) {
                                            put("photoUris", JSONArray().apply {
                                                message.photoUris.forEach { put(it) }
                                            })
                                        }
                                        if (message.photoSearchContext.isNotBlank()) {
                                            put("photoSearchContext", message.photoSearchContext)
                                        }
                                    })
                                }
                            })
                        })
                    }
                })
            }
            val tmp = File(file.parentFile, "$FILE_NAME.tmp")
            tmp.writeText(root.toString(), Charsets.UTF_8)
            if (!tmp.renameTo(file)) {
                tmp.delete()
                error("Could not atomically replace $FILE_NAME")
            }
            true
        }.getOrDefault(false)
    }

    data class PersistedState(
        val sessions: List<ChatSession> = emptyList(),
        val activeId: String? = null,
        val hapticsEnabled: Boolean = true,
        val soundsEnabled: Boolean = true,
        val warmupEnabled: Boolean = true,
        val prePrompt: String = DEFAULT_SYSTEM_INSTRUCTIONS,
        val thinkingEnabled: Boolean = false,
        val temperature: Float = 0.7f,
        val topK: Int = 40,
        val maxOutputTokens: Int = 900,
    )

    private companion object {
        const val FILE_NAME = "gemtalk_sessions.json"
        const val KEY_VERSION = "version"
        const val KEY_SESSIONS = "sessions"
        const val KEY_MESSAGES = "messages"
        const val KEY_ACTIVE = "activeId"
        const val KEY_HAPTICS = "hapticsEnabled"
        const val KEY_SOUNDS = "soundsEnabled"
        const val KEY_WARMUP_ENABLED = "warmupEnabled"
        const val KEY_PRE_PROMPT = "prePrompt"
        const val KEY_THINKING = "thinkingEnabled"
        const val KEY_TEMPERATURE = "temperature"
        const val KEY_TOP_K = "topK"
        const val KEY_MAX_OUTPUT_TOKENS = "maxOutputTokens"
    }
}
