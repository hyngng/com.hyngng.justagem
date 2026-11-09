package com.hyngng.justagem.ai

import com.hyngng.justagem.data.ChatMessage
import com.hyngng.justagem.data.ChatRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptHistoryTest {

    private var stamp = 0L

    private fun user(text: String) =
        ChatMessage(role = ChatRole.USER, text = text, createdAt = stamp++)

    private fun assistant(text: String) =
        ChatMessage(role = ChatRole.ASSISTANT, text = text, createdAt = stamp++)

    @Test
    fun `drops blank and error messages`() {
        val history = listOf(
            user("안녕"),
            assistant("  "),
            assistant("실패").copy(isError = true),
            assistant("반가워요"),
        )

        val trimmed = PromptHistory.trim(history)

        assertEquals(listOf("안녕", "반가워요"), trimmed.map { it.text })
    }

    @Test
    fun `never starts with an assistant message`() {
        val history = listOf(
            assistant("먼저 말해버림"),
            user("질문"),
            assistant("답변"),
        )

        val trimmed = PromptHistory.trim(history)

        assertEquals(ChatRole.USER, trimmed.first().role)
        assertEquals(listOf("질문", "답변"), trimmed.map { it.text })
    }

    @Test
    fun `keeps only the newest messages up to the message cap`() {
        val history = (1..20).flatMap { listOf(user("u$it"), assistant("a$it")) }

        val trimmed = PromptHistory.trim(history, maxMessages = 6)

        assertEquals(6, trimmed.size)
        assertEquals("u18", trimmed.first().text)
        assertEquals("a20", trimmed.last().text)
        assertEquals(ChatRole.USER, trimmed.first().role)
    }

    @Test
    fun `drops whole oldest turns until the character budget fits`() {
        val history = (1..5).flatMap { listOf(user("u".repeat(100)), assistant("a".repeat(100))) }

        val trimmed = PromptHistory.trim(history, maxChars = 450)

        assertTrue(trimmed.sumOf { it.text.length } <= 450)
        assertEquals(ChatRole.USER, trimmed.first().role)
        assertEquals(ChatRole.ASSISTANT, trimmed.last().role)
    }

    @Test
    fun `truncates a single oversized message keeping the tail`() {
        val long = "start-" + "x".repeat(900) + "-end"
        val trimmed = PromptHistory.trim(listOf(user(long)), maxChars = 120)

        assertEquals(1, trimmed.size)
        assertEquals(120, trimmed[0].text.length)
        assertTrue(trimmed[0].text.endsWith("-end"))
    }

    @Test
    fun `leaves a short conversation untouched`() {
        val history = listOf(user("오늘 날씨 어때?"), assistant("맑아요."))

        assertEquals(history, PromptHistory.trim(history))
    }

    @Test
    fun `shrinks the older message when a two message turn is oversized`() {
        val trimmed = PromptHistory.trim(
            listOf(user("u".repeat(300)), user("v".repeat(80))),
            maxChars = 120,
        )

        assertEquals(2, trimmed.size)
        assertTrue(trimmed.sumOf { it.text.length } <= 120)
        assertEquals("v".repeat(80), trimmed.last().text)
        assertEquals(40, trimmed.first().text.length)
    }
}
