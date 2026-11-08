package com.hyngng.gemtalk.ai

import com.hyngng.gemtalk.data.ChatMessage
import com.hyngng.gemtalk.data.ChatRole

/**
 * Keeps the replayed transcript inside AICore's context window.
 *
 * The ML Kit Prompt API is stateless: every request carries the conversation.
 * That is what makes session restore possible after a process death, but it also
 * means we have to trim. We drop whole oldest turns (user + assistant) and never
 * split a message, so the model still sees coherent dialogue.
 */
object PromptHistory {

    const val MAX_CHARS = 5_200
    const val MAX_MESSAGES = 24

    fun trim(
        messages: List<ChatMessage>,
        maxChars: Int = MAX_CHARS,
        maxMessages: Int = MAX_MESSAGES,
    ): List<ChatMessage> {
        val usable = messages
            .filter { it.role != ChatRole.TOOL && it.text.isNotBlank() && !it.isError }
            .takeLast(maxMessages)
            .map(::includePhotoSearchContext)
            .toMutableList()

        while (usable.firstOrNull()?.role == ChatRole.ASSISTANT) {
            usable.removeAt(0)
        }

        var total = usable.sumOf { it.text.length }
        while (total > maxChars && usable.size > 2) {
            val removed = usable.removeAt(0)
            total -= removed.text.length
            if (usable.firstOrNull()?.role == ChatRole.ASSISTANT) {
                total -= usable.removeAt(0).text.length
            }
        }

        while (usable.firstOrNull()?.role == ChatRole.ASSISTANT) {
            usable.removeAt(0)
        }

        // A single oversized turn still has to fit. Keep its tail: the newest
        // instruction matters more than the opening sentence.
        if (usable.size == 1 && usable[0].text.length > maxChars) {
            val only = usable[0]
            usable[0] = only.copy(text = only.text.takeLast(maxChars))
            return usable
        }

        // Two messages can still be over budget (huge previous answer + a new
        // question). Shrink the older half rather than dropping the turn.
        if (total > maxChars && usable.size >= 2) {
            val last = usable.last()
            val budget = maxChars - last.text.length
            if (budget <= 0) {
                usable.clear()
                usable += last.copy(text = last.text.takeLast(maxChars))
            } else {
                val first = usable.first()
                if (first.text.length > budget) {
                    usable[0] = first.copy(text = first.text.takeLast(budget))
                }
            }
        }
        return usable
    }

    private fun includePhotoSearchContext(message: ChatMessage): ChatMessage {
        if (message.role != ChatRole.ASSISTANT || message.photoSearchContext.isBlank()) return message
        return message.copy(
            text = buildString {
                append(message.text)
                appendLine()
                appendLine()
                appendLine("[Photo search record: I searched the device's indexed photo library with search_images. These photos were retrieved by me as tool results, not uploaded by the user. The search query and numbered result details follow. If the user asks for further visual analysis of one of these exact photos, retrieve it again with search_images using its one-based previous_result_index.]")
                append(message.photoSearchContext)
            },
        )
    }
}
