package com.hyngng.justagem.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.google.mlkit.genai.common.FeatureStatus
import com.hyngng.justagem.ai.ModelOption
import com.hyngng.justagem.ui.ModelPhase
import com.hyngng.justagem.ui.ModelUi
import com.hyngng.justagem.ui.i18n.LocalStrings
import com.hyngng.justagem.ui.theme.GraphiteRaised
import com.hyngng.justagem.ui.theme.GraphiteSoft
import com.hyngng.justagem.ui.theme.Hairline
import com.hyngng.justagem.ui.theme.Jade
import com.hyngng.justagem.ui.theme.Mist
import com.hyngng.justagem.ui.theme.MistFaint
import com.hyngng.justagem.ui.theme.MistMuted
import kotlinx.coroutines.delay

/**
 * Model switcher anchored in the chat top bar. The menu is a separate popup
 * window, so the enter animation is driven by a MutableTransitionState that
 * starts from `currentState = false` on the popup's first frame.
 */
@Composable
fun ModelSelector(
    model: ModelUi,
    enabled: Boolean,
    supportsTools: Boolean,
    thinkingEnabled: Boolean,
    onThinkingChange: (Boolean) -> Unit,
    onSelect: (ModelOption) -> Unit,
    modifier: Modifier = Modifier,
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
            delay(JustagemDesign.motionStandard.toLong())
            menuPresent = false
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                anchorWidth = with(density) { coordinates.size.width.toDp() }
                anchorHeight = with(density) { coordinates.size.height.toDp() }
            },
    ) {
        ModelSelectionRow(
            model = model,
            expanded = expanded,
            enabled = enabled,
            supportsTools = supportsTools,
            onClick = {
                interactionHaptics?.perform(HapticFeedbackType.TextHandleMove)
                expanded = !expanded
            },
        )
        if (menuPresent) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, with(density) { anchorHeight.roundToPx() + 6.dp.roundToPx() }),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visibleState = menuTransition,
                    enter = fadeIn(tween(150)) +
                        scaleIn(
                            initialScale = 0.94f,
                            transformOrigin = TransformOrigin(0.5f, 0f),
                            animationSpec = tween(
                                durationMillis = JustagemDesign.motionStandard,
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
                            if (model.options.isEmpty()) {
                                ModelMenuRow(
                                    onClick = { expanded = false },
                                    enabled = false,
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                        ShimmerBox(Modifier.fillMaxWidth(0.72f).height(11.dp))
                                        ShimmerBox(Modifier.fillMaxWidth(0.48f).height(9.dp))
                                    }
                                }
                            }
                            model.options.forEachIndexed { index, option ->
                                val selected = option.label == model.label
                                val status = when (option.status) {
                                    FeatureStatus.AVAILABLE -> s.t("기기에 준비됨", "Ready on device")
                                    FeatureStatus.DOWNLOADING -> s.t("내려받는 중", "Downloading")
                                    FeatureStatus.DOWNLOADABLE -> s.t("선택 후 다운로드 필요", "Needs download")
                                    else -> s.t("상태 확인 필요", "Status unknown")
                                }
                                ModelMenuRow(
                                    onClick = {
                                        interactionHaptics?.perform(HapticFeedbackType.TextHandleMove)
                                        onSelect(option)
                                        expanded = false
                                    },
                                    enabled = enabled,
                                    selected = selected,
                                    trailingContent = if (selected) {
                                        {
                                            Icon(
                                                Icons.Rounded.Check,
                                                contentDescription = s.t("현재 선택", "Currently selected"),
                                                tint = Jade,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            option.label,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                            ),
                                            color = if (selected) Jade else Mist,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            option.baseModel ?: status,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selected) Jade.copy(alpha = 0.72f) else MistFaint,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                            if (model.options.isNotEmpty()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                                    color = Hairline.copy(alpha = 0.42f),
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    s.t("사고 모드", "Thinking mode"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Mist,
                                    modifier = Modifier.weight(1f),
                                )
                                Switch(
                                    checked = thinkingEnabled,
                                    onCheckedChange = {
                                        interactionHaptics?.perform(HapticFeedbackType.TextHandleMove)
                                        onThinkingChange(it)
                                    },
                                    colors = SwitchDefaults.colors(
                                        uncheckedTrackColor = GraphiteRaised.copy(alpha = 0f),
                                    ),
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
private fun ModelSelectionRow(
    model: ModelUi,
    expanded: Boolean,
    enabled: Boolean,
    supportsTools: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val s = LocalStrings.current
    val pressed by interactionSource.collectIsPressedAsState()
    val background by animateColorAsState(
        targetValue = if (pressed) GraphiteSoft.copy(alpha = 0.62f) else Color.Transparent,
        animationSpec = tween(JustagemDesign.motionQuick),
        label = "model selection pressed background",
    )
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(JustagemDesign.motionStandard, easing = FastOutSlowInEasing),
        label = "model chevron rotation",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .background(background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).height(36.dp), contentAlignment = Alignment.CenterStart) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .alpha(if (model.phase == ModelPhase.CHECKING) 0f else 1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    model.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = Mist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        if (model.phase != ModelPhase.CHECKING) {
                            append(model.baseModel ?: when (model.phase) {
                                ModelPhase.CHECKING -> ""
                                ModelPhase.DOWNLOADABLE -> s.t("다운로드 필요", "Download needed")
                                ModelPhase.DOWNLOADING -> s.t("내려받는 중", "Downloading")
                                ModelPhase.READY -> s.t("기기에 준비됨", "Ready on device")
                                ModelPhase.UNAVAILABLE -> s.t("사용할 수 없음", "Unavailable")
                                ModelPhase.ERROR -> s.t("상태 확인 필요", "Status unknown")
                            })
                        }
                        if (model.phase == ModelPhase.READY) {
                            append(
                                if (supportsTools) {
                                    s.t(" · 도구 지원", " · tools supported")
                                } else {
                                    s.t(" · 도구 미지원", " · tools unavailable")
                                },
                            )
                        }
                        if (model.pendingSelection) {
                            append(s.t(" · 다음 응답부터 적용", " · applies to the next response"))
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MistFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = model.phase == ModelPhase.CHECKING,
                enter = fadeIn(tween(140)),
                exit = fadeOut(tween(220)),
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShimmerBox(Modifier.fillMaxWidth(0.72f).height(11.dp))
                    ShimmerBox(Modifier.fillMaxWidth(0.48f).height(9.dp))
                }
            }
        }
        Icon(
            Icons.Rounded.ExpandMore,
            contentDescription = if (expanded) s.t("모델 목록 닫기", "Close model list") else s.t("모델 목록 열기", "Open model list"),
            tint = if (expanded) Jade else MistMuted,
            modifier = Modifier
                .padding(start = 4.dp)
                .rotate(chevronRotation)
                .size(20.dp),
        )
    }
}

@Composable
private fun ModelMenuRow(
    onClick: () -> Unit,
    enabled: Boolean,
    selected: Boolean = false,
    trailingContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val background by animateColorAsState(
        targetValue = when {
            selected -> Jade.copy(alpha = 0.11f)
            pressed -> GraphiteSoft.copy(alpha = 0.55f)
            else -> Color.Transparent
        },
        animationSpec = tween(JustagemDesign.motionQuick),
        label = "model menu row background",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .background(background)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            content()
        }
        trailingContent?.invoke()
    }
}
