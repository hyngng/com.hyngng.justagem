package com.hyngng.gemtalk.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyngng.gemtalk.ui.components.GemOrb
import com.hyngng.gemtalk.ui.components.GemTalkDesign
import com.hyngng.gemtalk.ui.components.GemTalkIconControl
import com.hyngng.gemtalk.ui.components.IconControlTone
import com.hyngng.gemtalk.ui.components.LocalInteractionHaptics
import com.hyngng.gemtalk.ui.components.OrbState
import com.hyngng.gemtalk.ui.i18n.LocalStrings
import com.hyngng.gemtalk.ui.i18n.tr
import com.hyngng.gemtalk.ui.theme.Graphite
import com.hyngng.gemtalk.ui.theme.Hairline
import com.hyngng.gemtalk.ui.theme.Ice
import com.hyngng.gemtalk.ui.theme.Ink
import com.hyngng.gemtalk.ui.theme.Jade
import com.hyngng.gemtalk.ui.theme.Mist
import com.hyngng.gemtalk.ui.theme.MistFaint
import com.hyngng.gemtalk.ui.theme.MistMuted
import kotlin.math.roundToInt

@Composable
fun VoiceOverlay(
    voice: VoiceUi,
    thinkingPreferenceEnabled: Boolean,
    onClose: () -> Unit,
    onPrimary: () -> Unit,
) {
    val interactionHaptics = LocalInteractionHaptics.current
    AnimatedVisibility(
        visible = voice.active,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(170)),
    ) {
        val orbState = when (voice.phase) {
            VoicePhase.LISTENING -> OrbState.LISTENING
            VoicePhase.THINKING -> OrbState.THINKING
            VoicePhase.SPEAKING -> OrbState.SPEAKING
            VoicePhase.IDLE -> OrbState.IDLE
        }
        val s = LocalStrings.current
        val listeningPlaceholder = s.t("편하게 말씀해 주세요", "Speak whenever you're ready")
        val idlePlaceholder = s.t("마이크를 탭해 시작하세요", "Tap the microphone to start")
        val title = when (voice.phase) {
            VoicePhase.LISTENING -> s.t("듣고 있어요", "Listening")
            VoicePhase.THINKING -> s.t("답변을 만들고 있어요", "Writing a reply")
            VoicePhase.SPEAKING -> s.t("말하고 있어요", "Speaking")
            VoicePhase.IDLE -> if (voice.hint.isNotBlank()) s.t("잠시 멈췄어요", "Paused") else s.t("준비됐어요", "Ready")
        }
        val mainText = when (voice.phase) {
            VoicePhase.LISTENING -> voice.transcript.ifBlank { listeningPlaceholder }
            VoicePhase.THINKING -> voice.caption.ifBlank { voice.transcript }
            VoicePhase.SPEAKING -> voice.caption
            VoicePhase.IDLE -> if (voice.hint.isNotBlank()) voice.hint else idlePlaceholder
        }
        val isPlaceholder = mainText.isBlank() ||
            mainText == listeningPlaceholder ||
            mainText == idlePlaceholder
        val caption = if (mainText.isNotBlank()) mainText else s.t("잠시만 기다려 주세요", "One moment")
        val captionScroll = rememberScrollState()
        var lineEnds by remember { mutableStateOf(emptyList<Int>()) }
        // A new answer restarts the caption, so only then jump back to the top.
        // The old effect ran on every chunk, yanking the text back to the first
        // line mid-animation and making the scroll look like stray taps.
        LaunchedEffect(voice.phase, caption.isNotBlank()) {
            if (voice.phase == VoicePhase.SPEAKING && caption.isNotBlank()) {
                captionScroll.scrollTo(0)
            }
        }
        // Step once per spoken line instead of once per character: the caption
        // advances continuously while TTS speaks, and animating on every
        // character kept restarting the tween before it could settle.
        val spokenLine = if (lineEnds.isEmpty()) {
            0
        } else {
            lineEnds.indexOfLast { it <= voice.speechCharacterIndex } + 1
        }
        LaunchedEffect(voice.phase, spokenLine, lineEnds.size) {
            if (voice.phase != VoicePhase.SPEAKING) return@LaunchedEffect
            if (lineEnds.isEmpty() || captionScroll.maxValue <= 0) return@LaunchedEffect
            val lineProgress = (spokenLine.toFloat() / lineEnds.size).coerceIn(0f, 1f)
            val target = (captionScroll.maxValue * lineProgress).roundToInt()
            if (target <= captionScroll.value) return@LaunchedEffect
            captionScroll.animateScrollTo(
                target,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
            )
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val orbSize = minOf(252.dp, maxHeight * 0.36f).coerceAtLeast(148.dp)
            val captionHeight = minOf(146.dp, maxHeight * 0.22f).coerceAtLeast(86.dp)
            val voiceControlLift = minOf(56.dp, maxHeight * 0.07f)
            val backdrop = MaterialTheme.colorScheme.surfaceVariant
            val background = Ink
            val jadeGlow = Jade
            val iceGlow = Ice
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(backdrop, background, background),
                    ),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(jadeGlow.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 0.40f),
                        radius = size.width * 0.95f,
                    ),
                    radius = size.width * 0.95f,
                    center = Offset(size.width / 2f, size.height * 0.40f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(iceGlow.copy(alpha = 0.05f), Color.Transparent),
                        center = Offset(size.width * 0.82f, size.height * 0.15f),
                        radius = size.width * 0.55f,
                    ),
                    radius = size.width * 0.55f,
                    center = Offset(size.width * 0.82f, size.height * 0.15f),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.size(GemTalkDesign.iconControlSize))
                    Spacer(Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (voice.phase == VoicePhase.IDLE) MistFaint else Jade),
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = s.t("음성 대화", "Voice chat"),
                            style = MaterialTheme.typography.labelLarge,
                            color = Mist.copy(alpha = 0.82f),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    GemTalkIconControl(
                        onClick = onClose,
                        tone = IconControlTone.Quiet,
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = s.t("음성 대화 닫기", "Close voice chat"),
                            modifier = Modifier.size(21.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.weight(0.35f))

                    GemOrb(
                        state = orbState,
                        modifier = Modifier.size(orbSize),
                        detail = true,
                        inputLevel = voice.inputLevel,
                        speechLevel = voice.speechLevel,
                    )

                    Spacer(Modifier.height(30.dp))

                    AnimatedContent(
                        targetState = title,
                        transitionSpec = {
                            fadeIn(tween(220)) togetherWith fadeOut(tween(160))
                        },
                        label = "voiceTitle",
                    ) { text ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 24.sp,
                                letterSpacing = (-0.4).sp,
                            ),
                            color = Mist,
                            textAlign = TextAlign.Center,
                        )
                    }

                    if (voice.phase == VoicePhase.THINKING && thinkingPreferenceEnabled) {
                        Text(
                            text = s.t(
                                "음성 응답에서는 Thinking을 잠시 생략해요 · 설정은 유지됩니다",
                                "Thinking is skipped for voice replies · setting is kept",
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MistFaint,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 5.dp),
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(captionHeight),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Text(
                            text = caption,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 17.sp,
                                lineHeight = 27.sp,
                                textAlign = TextAlign.Center,
                            ),
                            color = if (isPlaceholder) MistFaint else Mist.copy(alpha = 0.94f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(captionScroll, enabled = false)
                                .padding(horizontal = 8.dp),
                            onTextLayout = { result ->
                                lineEnds = (0 until result.lineCount).map(result::getLineEnd)
                            },
                        )
                    }

                    if (voice.hint.isNotBlank() && voice.hint != mainText) {
                        Text(
                            text = voice.hint,
                            style = MaterialTheme.typography.labelSmall,
                            color = MistFaint,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }

                    Spacer(Modifier.weight(1.45f))

                    Column(
                        modifier = Modifier.offset(y = -voiceControlLift),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        VoicePrimaryButton(
                            phase = voice.phase,
                            onClick = onPrimary,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = when (voice.phase) {
                                VoicePhase.LISTENING -> s.t("탭하여 마치기", "Tap to finish")
                                VoicePhase.THINKING, VoicePhase.SPEAKING -> s.t("탭하여 멈추기", "Tap to stop")
                                VoicePhase.IDLE -> s.t("탭하여 말하기", "Tap to speak")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MistFaint,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun VoicePrimaryButton(
    phase: VoicePhase,
    onClick: () -> Unit,
) {
    val interactionHaptics = LocalInteractionHaptics.current
    val listening = phase == VoicePhase.LISTENING
    val active = phase != VoicePhase.IDLE
    Box(
        modifier = Modifier
            .size(78.dp)
            .clip(CircleShape)
            .background(if (active) Jade.copy(alpha = 0.13f) else Graphite.copy(alpha = 0.75f))
            .border(
                width = 1.dp,
                color = if (active) Jade.copy(alpha = 0.55f) else Hairline,
                shape = CircleShape,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    interactionHaptics?.perform(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = when {
                listening || active -> Icons.Rounded.Stop
                else -> Icons.Rounded.Mic
            },
            contentDescription = if (active) tr("멈추기", "Stop") else tr("말하기", "Speak"),
            tint = if (active) Jade else Mist,
            modifier = Modifier.size(if (active) 26.dp else 30.dp),
        )
    }
}
