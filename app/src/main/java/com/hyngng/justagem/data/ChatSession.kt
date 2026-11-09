package com.hyngng.justagem.data

import com.hyngng.justagem.ui.i18n.tr
import java.util.UUID

enum class ChatRole { USER, ASSISTANT, TOOL }

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    val thinkingEnabled: Boolean = false,
    val thinkingObserved: Boolean = false,
    val thinkingFinished: Boolean = false,
    val thinkingUnavailable: Boolean = false,
    val thoughtText: String = "",
    val attachmentUris: List<String> = emptyList(),
    val photoUris: List<String> = emptyList(),
    val photoSearchContext: String = "",
)

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val messages: List<ChatMessage> = emptyList(),
) {
    val displayTitle: String
        get() = title.ifBlank { tr("새 대화", "New chat") }

    val preview: String
        get() = messages
            .lastOrNull { it.role != ChatRole.TOOL && it.text.isNotBlank() && !it.isError }
            ?.text
            ?.replace('\n', ' ')
            ?.trim()
            ?.take(64)
            .orEmpty()

    val userMessageCount: Int
        get() = messages.count { it.role == ChatRole.USER }
}

fun titleFrom(text: String): String {
    val line = text.lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotEmpty() }
        .orEmpty()
    if (line.isEmpty()) return ""
    return if (line.length <= 34) line else line.take(33).trimEnd() + "…"
}
