package com.hyngng.gemtalk.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import com.hyngng.gemtalk.ui.theme.MistMuted

/**
 * Shared action-cluster grammar for app dialogs (GemConfirmDialog /
 * GemTextInputDialog). The two look-invariant rules:
 *
 * 1. The visible gap from the action cluster to the dialog's right edge and
 *    to its bottom edge MUST be equal. The visible gap per axis is
 *    [GemDialogActionInset] + [GemDialogActionTouchPadding] (the first outside
 *    the row, the second inside each action), so when touching either value
 *    up, change both axes together.
 * 2. Individual actions are plain labelLarge texts; the action's own touch
 *    padding is square (equal horizontal/vertical) so it can never skew the
 *    bottom/right ratio, unlike M3 TextButton whose enforced 48dp touch
 *    target silently adds ~16dp of invisible height per button.
 */

/** Outside-the-row inset applied to both the right and bottom edge. */
val GemDialogActionInset = 16.dp

/** Square per-action touch padding (kept equal on both axes, see top doc). */
val GemDialogActionTouchPadding = 8.dp

/**
 * Bottom-right action row with the equal bottom/right inset baked in.
 * Always right-align the cluster through this component, never local padding.
 */
@Composable
fun GemDialogActionRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.padding(
            end = GemDialogActionInset,
            bottom = GemDialogActionInset,
        ),
        horizontalArrangement = Arrangement.spacedBy(4.dp, alignment = Alignment.End),
        content = content,
    )
}

/** One dialog action label: labelLarge text with a square pill touch area. */
@Composable
fun GemDialogActionText(
    label: String,
    color: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val interactionHaptics = LocalInteractionHaptics.current
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (enabled) color else color.copy(alpha = 0.38f),
        modifier = Modifier
            .padding(
                horizontal = GemDialogActionTouchPadding,
                vertical = GemDialogActionTouchPadding,
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) {
                interactionHaptics?.perform(HapticFeedbackType.TextHandleMove)
                onClick()
            },
    )
}
