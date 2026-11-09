package com.hyngng.justagem.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hyngng.justagem.ui.theme.Jade

/**
 * Three-dot "thinking" indicator used above the composer and in the hero.
 *
 * A single shared phase drives every dot with a fixed offset, so the dots can
 * never drift into different cycle lengths. Other waiting states (photo index
 * loading) reuse this instead of rolling their own per-dot timers.
 */
@Composable
fun ThinkingDots(modifier: Modifier = Modifier, color: Color? = null) {
    val dotColor = color ?: Jade
    val transition = rememberInfiniteTransition(label = "dots")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(JustagemMotion.indicatorCycleMillis, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "dotPulsePhase",
    )
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(JustagemMotion.indicatorCount) { index ->
            val shiftedPhase = (phase - index.toFloat() / JustagemMotion.indicatorCount + 1f) % 1f
            val triangle = 1f - kotlin.math.abs(2f * shiftedPhase - 1f)
            val glow = triangle * triangle * (3f - 2f * triangle)
            Canvas(Modifier.size(5.dp)) {
                val diameter = 4.dp.toPx() * (0.78f + 0.34f * glow)
                drawCircle(
                    color = dotColor.copy(alpha = 0.28f + 0.67f * glow),
                    radius = diameter / 2f,
                )
            }
        }
    }
}
