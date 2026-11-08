package com.hyngng.gemtalk.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatSessionTest {

    @Test
    fun `title uses the first non-blank line`() {
        assertEquals("주말 여행 계획을 세워줘", titleFrom("\n\n  주말 여행 계획을 세워줘\n둘째 줄"))
    }

    @Test
    fun `blank input yields no title`() {
        assertEquals("", titleFrom("   \n\t\n"))
    }

    @Test
    fun `long titles are shortened with an ellipsis`() {
        val title = titleFrom("가".repeat(60))

        assertEquals(34, title.length)
        assertEquals("가".repeat(33) + "…", title)
    }

    @Test
    fun `max length titles are kept as is`() {
        val line = "나".repeat(34)

        assertEquals(line, titleFrom(line))
    }

    @Test
    fun `display title falls back and preview uses the last real message`() {
        val session = ChatSession(
            messages = listOf(
                ChatMessage(role = ChatRole.USER, text = "첫 질문"),
                ChatMessage(role = ChatRole.ASSISTANT, text = "답변\n두 줄"),
                ChatMessage(role = ChatRole.ASSISTANT, text = "오류", isError = true),
            ),
        )

        assertEquals("새 대화", session.displayTitle)
        assertEquals("답변 두 줄", session.preview)
        assertEquals(1, session.userMessageCount)
    }
}
