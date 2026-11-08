package com.hyngng.gemtalk.audio

enum class AppFeedbackCue {
    MESSAGE_SENT,
    THINKING_STARTED,
    THINKING_FINISHED,
    ANSWER_LEAD_IN,
    ANSWER_COMPLETED,
    TEXT_CHARACTER,
    THOUGHT_CHARACTER,
}

data class AppFeedbackEvent(
    val cue: AppFeedbackCue,
    val allowSound: Boolean = true,
)
