package com.hyngng.gemtalk.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val shimmerColor = MaterialTheme.colorScheme.onSurface
    val transition = rememberInfiniteTransition(label = "model loading shimmer")
    val phase by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer phase",
    )
    Canvas(modifier) {
        val corner = CornerRadius(size.height / 2f)
        drawRoundRect(
            color = shimmerColor.copy(alpha = 0.075f),
            cornerRadius = corner,
        )
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    shimmerColor.copy(alpha = 0f),
                    shimmerColor.copy(alpha = 0.16f),
                    shimmerColor.copy(alpha = 0f),
                ),
                start = Offset(size.width * phase, 0f),
                end = Offset(size.width * (phase + 0.55f), size.height),
            ),
            cornerRadius = corner,
        )
    }
}
