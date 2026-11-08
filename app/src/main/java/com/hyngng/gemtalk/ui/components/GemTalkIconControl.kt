package com.hyngng.gemtalk.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import kotlin.math.pow

/** Shared shape, hit target, and tones for all compact icon actions in GemTalk. */
enum class IconControlTone { Quiet, Neutral, Accent, Success, Destructive }

object GemTalkDesign {
    val iconControlSize = 48.dp
    val iconSize = 20.dp
    val compactIconSize = 18.dp
    val standardCornerRadius = 16.dp
    val contentHorizontalPadding = 16.dp
    val drawerHorizontalPadding = 32.dp
    val dialogHorizontalPadding = 20.dp
    val dialogHeaderTopPadding = 16.dp
    val dialogHeaderBottomPadding = 8.dp
    val compactSpacing = 8.dp
    val sectionSpacing = 16.dp
    val controlSpacing = 24.dp
    val drawerHeaderToActionSpacing = 16.dp
    val drawerLabelToListSpacing = 12.dp
    val drawerListToSettingsSpacing = 24.dp
    val drawerSettingsBottomSpacing = 48.dp
    val drawerSettingsEndInset = 16.dp
    val motionQuick = 120
    val motionStandard = 220

    // A solid background-color curtain hides the drawer and chat while the settings page enters.
    const val settingsPageDimAlpha = 1f
    const val settingsPageDimMillis = 220
    // Entry dim only covers the drawer, so it finishes on clockQuick to keep
    // the fully-covered dwell short; the exit curtain keeps the full clock.
    const val settingsEntryDimMillis = 120
    // Page slide-out clock: half of the entry page clock — exits run 2x fast.
    const val settingsExitPageMillis = 110
    // Root curtain lift on exit is deliberately slow relative to the page
    // exit: page leaves 2x faster than the drawer reappears (2:1).
    const val settingsExitDimMillis = 220

    /** Shared page-push easing: strong ease-out — fast entry, long smooth deceleration. */
    val slideEase: Easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

    /** Fast-arrival easing: ~2/3 of the change lands in the first fifth of the clock, then it settles gently (0 → 0.7 → 0.9 → 1). */
    val arrivalEase: Easing = ArrivalEasing(power = 5f)
}

/**
 * Fast-arrival profile: 1 − (1−fraction)^power. With power = 5 the value at
 * 20% of the clock is already ~67%, ~90% at mid-clock, exactly 1 at the end.
 */
class ArrivalEasing(private val power: Float) : Easing {
    override fun transform(fraction: Float): Float = 1f - (1f - fraction).pow(power)
}

/** Shared motion rules: state colors ease through, while repeated indicators pulse in sequence. */
object GemTalkMotion {
    const val feedbackColorMillis = 220
    const val indicatorCycleMillis = 1_050
    const val indicatorCount = 3
}

@Composable
fun GemTalkIconControl(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: IconControlTone = IconControlTone.Quiet,
    enabled: Boolean = true,
    hapticOnClick: Boolean = true,
    size: androidx.compose.ui.unit.Dp = GemTalkDesign.iconControlSize,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val interactionHaptics = LocalInteractionHaptics.current
    val successContainer = colors.primary.copy(alpha = 0.22f)
    val container = when (tone) {
        IconControlTone.Quiet -> Color.Transparent
        IconControlTone.Neutral -> colors.surfaceVariant
        IconControlTone.Accent -> colors.primary
        IconControlTone.Success -> successContainer
        IconControlTone.Destructive -> colors.primary.copy(alpha = 0.22f)
    }
    val animatedContainer by animateColorAsState(
        targetValue = container,
        animationSpec = tween(durationMillis = GemTalkMotion.feedbackColorMillis),
        label = "icon control container color",
    )
    val contentColor = when (tone) {
        IconControlTone.Quiet, IconControlTone.Neutral -> colors.onSurfaceVariant
        IconControlTone.Accent -> colors.onPrimary
        IconControlTone.Success -> colors.primary
        IconControlTone.Destructive -> colors.primary
    }
    val animatedContentColor by animateColorAsState(
        targetValue = contentColor,
        animationSpec = tween(durationMillis = GemTalkMotion.feedbackColorMillis),
        label = "icon control content color",
    )
    val outline = when (tone) {
        IconControlTone.Quiet -> null
        IconControlTone.Neutral -> BorderStroke(1.dp, colors.outlineVariant)
        IconControlTone.Accent -> null
        IconControlTone.Success -> null
        IconControlTone.Destructive -> BorderStroke(1.dp, colors.primary.copy(alpha = 0.35f))
    }
    Surface(
        onClick = {
            if (hapticOnClick) interactionHaptics?.perform(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        enabled = enabled,
        modifier = modifier.size(size),
        shape = CircleShape,
        color = animatedContainer,
        contentColor = animatedContentColor,
        border = outline,
    ) {
        Box(contentAlignment = Alignment.Center, content = content)
    }
}

@Composable
fun successCheckTint(): Color = MaterialTheme.colorScheme.primary
