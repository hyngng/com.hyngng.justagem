package com.hyngng.justagem.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.hyngng.justagem.ui.theme.Jade
import kotlin.math.min

enum class OrbState { IDLE, LISTENING, THINKING, SPEAKING }

/**
 * The app's one memorable object: a flat jade disc.
 *
 * The disc itself is deliberately a single solid colour — no glass gradients,
 * rim light or specular highlights. State is communicated around it instead:
 *
 * IDLE       slow breathing
 * LISTENING  a quiet breath + live input-level ring
 * THINKING   a calm breath
 * SPEAKING   the disc alone scales with TTS output level
 */
@Composable
fun GemOrb(
    state: OrbState,
    modifier: Modifier = Modifier,
    detail: Boolean = true,
    inputLevel: Float = 0f,
    speechLevel: Float = 0f,
) {
    val orbColor = Jade
    val transition = rememberInfiniteTransition(label = "orb")
    val isSpeaking = state == OrbState.SPEAKING

    // ── Breathing (IDLE / LISTENING / THINKING) ───────────────────────────
    val breathPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 4_600,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathPhase",
    )
    val breathAmplitude by animateFloatAsState(
        targetValue = when (state) {
            OrbState.LISTENING -> 0.010f
            OrbState.SPEAKING -> 0f          // Speaking cadence takes over
            OrbState.THINKING -> 0.020f
            OrbState.IDLE -> 0.014f
        },
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "breathAmplitude",
    )

    val listeningLevel by animateFloatAsState(
        targetValue = if (state == OrbState.LISTENING) inputLevel.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(
            durationMillis = if (state == OrbState.LISTENING) 110 else 280,
            easing = FastOutSlowInEasing,
        ),
        label = "listeningLevel",
    )

    val voiceEnergy by animateFloatAsState(
        targetValue = if (isSpeaking) speechLevel.coerceIn(0f, 1f) else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "voiceEnergy",
    )
    val meterOpacity by animateFloatAsState(
        targetValue = if (state == OrbState.LISTENING) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "meterOpacity",
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val unit = min(size.width, size.height)

        // Base breathing motion when not speaking
        val breath = 1f + (breathPhase * 2f - 1f) * breathAmplitude

        val activeScale = when (state) {
            OrbState.LISTENING -> breath * (1f + listeningLevel * 0.045f)
            OrbState.SPEAKING -> 1f + voiceEnergy * 0.065f
            else -> breath
        }
        val radius = unit * 0.335f * activeScale

        if (detail && meterOpacity > 0.001f) {
            val meterRadius = radius * 1.14f
            val stroke = 3.dp.toPx()
            drawCircle(
                color = orbColor.copy(alpha = 0.22f * meterOpacity),
                radius = meterRadius,
                center = center,
                style = Stroke(width = stroke),
            )
            if (listeningLevel > 0.01f) {
                drawArc(
                    color = orbColor.copy(alpha = 0.95f * meterOpacity),
                    startAngle = -90f,
                    sweepAngle = 360f * listeningLevel,
                    useCenter = false,
                    topLeft = Offset(center.x - meterRadius, center.y - meterRadius),
                    size = Size(meterRadius * 2f, meterRadius * 2f),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }

        // ── Core Disc ───────────────────────────────────────────────
        drawCircle(
            color = orbColor,
            radius = radius,
            center = center,
        )

    }
}
