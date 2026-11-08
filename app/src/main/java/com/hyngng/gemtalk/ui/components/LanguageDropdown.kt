package com.hyngng.gemtalk.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hyngng.gemtalk.ui.i18n.AppLanguage
import com.hyngng.gemtalk.ui.i18n.LocalStrings
import com.hyngng.gemtalk.ui.theme.GraphiteRaised
import com.hyngng.gemtalk.ui.theme.GraphiteSoft
import com.hyngng.gemtalk.ui.theme.Hairline
import com.hyngng.gemtalk.ui.theme.Jade
import com.hyngng.gemtalk.ui.theme.Mist
import com.hyngng.gemtalk.ui.theme.MistFaint
import kotlinx.coroutines.delay

/**
 * Settings-page dropdown row following the ModelSelector popup grammar:
 * measured anchor width, MutableTransitionState fade+scale popup over a
 * GraphiteRaised surface, and single-line rows with a Jade check for the
 * active choice. A blank tag means "follow the system language".
 */
@Composable
internal fun LanguageDropdownRow(
    languageTag: String,
    onLanguageTagChange: (String?) -> Unit,
) {
    val interactionHaptics = LocalInteractionHaptics.current
    val s = LocalStrings.current
    val density = LocalDensity.current
    var expanded by remember { mutableStateOf(false) }
    var anchorWidth by remember { mutableStateOf(0.dp) }
    var anchorHeight by remember { mutableStateOf(0.dp) }
    val menuTransition = remember { MutableTransitionState(false) }
    var menuPresent by remember { mutableStateOf(false) }
    LaunchedEffect(expanded) {
        if (expanded) {
            menuTransition.targetState = true
            menuPresent = true
        } else {
            menuTransition.targetState = false
            delay(GemTalkDesign.motionStandard.toLong())
            menuPresent = false
        }
    }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(GemTalkDesign.motionStandard, easing = FastOutSlowInEasing),
        label = "language chevron rotation",
    )
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val background by animateColorAsState(
        targetValue = if (pressed || expanded) GraphiteSoft.copy(alpha = 0.62f) else Color.Transparent,
        animationSpec = tween(GemTalkDesign.motionQuick),
        label = "language row pressed background",
    )

    val selectedLanguage = AppLanguage.resolve(languageTag)
    val currentValue = if (languageTag.isBlank()) {
        s.t("시스템 언어 따름", "Follow system language")
    } else {
        selectedLanguage.label
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                anchorWidth = with(density) { coordinates.size.width.toDp() }
                anchorHeight = with(density) { coordinates.size.height.toDp() }
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                ) {
                    interactionHaptics?.perform(HapticFeedbackType.TextHandleMove)
                    expanded = !expanded
                }
                .background(background)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = s.t("언어", "Language"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Mist,
                )
                Text(
                    text = currentValue,
                    style = MaterialTheme.typography.labelSmall,
                    color = MistFaint,
                )
            }
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) s.t("언어 목록 닫기", "Close language list") else s.t("언어 목록 열기", "Open language list"),
                tint = if (expanded) Jade else MistFaint,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .rotate(chevronRotation)
                    .size(20.dp),
            )
        }
        if (menuPresent) {
            androidx.compose.ui.window.Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, with(density) { anchorHeight.roundToPx() + 6.dp.roundToPx() }),
                onDismissRequest = { expanded = false },
                properties = androidx.compose.ui.window.PopupProperties(focusable = true),
            ) {
                AnimatedVisibility(
                    visibleState = menuTransition,
                    enter = fadeIn(tween(150)) +
                        scaleIn(
                            initialScale = 0.94f,
                            transformOrigin = TransformOrigin(0.5f, 0f),
                            animationSpec = tween(
                                durationMillis = GemTalkDesign.motionStandard,
                                easing = FastOutSlowInEasing,
                            ),
                        ),
                    exit = fadeOut(tween(120)) +
                        scaleOut(
                            targetScale = 0.97f,
                            transformOrigin = TransformOrigin(0.5f, 0f),
                            animationSpec = tween(140),
                        ),
                ) {
                    Surface(
                        modifier = Modifier.width(anchorWidth),
                        shape = RoundedCornerShape(18.dp),
                        color = GraphiteRaised,
                        tonalElevation = 10.dp,
                        shadowElevation = 14.dp,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp),
                        ) {
                            LanguageMenuRow(
                                label = s.t("시스템 언어 따름", "Follow system language"),
                                selected = languageTag.isBlank(),
                                onClick = {
                                    interactionHaptics?.perform(HapticFeedbackType.TextHandleMove)
                                    onLanguageTagChange(null)
                                    expanded = false
                                },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                color = Hairline.copy(alpha = 0.42f),
                            )
                            AppLanguage.entries.forEach { language ->
                                LanguageMenuRow(
                                    label = language.label,
                                    selected = languageTag == language.tag,
                                    onClick = {
                                        interactionHaptics?.perform(HapticFeedbackType.TextHandleMove)
                                        onLanguageTagChange(language.tag)
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageMenuRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val background by animateColorAsState(
        targetValue = if (pressed) GraphiteSoft.copy(alpha = 0.62f) else Color.Transparent,
        animationSpec = tween(GemTalkDesign.motionQuick),
        label = "language menu row pressed background",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .background(background)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal,
            ),
            color = if (selected) Jade else Mist,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = Jade,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
