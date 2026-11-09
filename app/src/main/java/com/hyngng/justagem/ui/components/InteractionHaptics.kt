package com.hyngng.justagem.ui.components

import android.os.SystemClock
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

class InteractionHaptics(private val platform: HapticFeedback) {
    private var lastFeedbackAt = 0L

    fun perform(type: HapticFeedbackType, minimumIntervalMs: Long = 45) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastFeedbackAt < minimumIntervalMs) return
        lastFeedbackAt = now
        platform.performHapticFeedback(type)
    }
}

val LocalInteractionHaptics = staticCompositionLocalOf<InteractionHaptics?> { null }
