package com.hyngng.gemtalk.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyngng.gemtalk.ui.components.LocalInteractionHaptics
import com.hyngng.gemtalk.ui.components.GemTalkDesign
import com.hyngng.gemtalk.ui.components.LanguageDropdownRow
import com.hyngng.gemtalk.ui.i18n.LocalStrings
import com.hyngng.gemtalk.ui.theme.Hairline
import com.hyngng.gemtalk.ui.theme.Ink
import com.hyngng.gemtalk.ui.theme.Jade
import com.hyngng.gemtalk.ui.theme.Mist
import com.hyngng.gemtalk.ui.theme.MistFaint
import com.hyngng.gemtalk.ui.theme.MistMuted
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * App settings as a standalone page instead of a modal dialog.
 * Follows the system theme background to read as its own screen.
 */
@Composable
fun AppSettingsScreen(
    visible: Boolean,
    hapticsEnabled: Boolean,
    soundsEnabled: Boolean,
    warmupEnabled: Boolean,
    languageTag: String,
    onHapticsChange: (Boolean) -> Unit,
    onSoundsChange: (Boolean) -> Unit,
    onWarmupChange: (Boolean) -> Unit,
    onLanguageTagChange: (String?) -> Unit,
    onBack: () -> Unit,
    onEnterAnimationComplete: () -> Unit,
    onExitAnimationComplete: () -> Unit,
) {
    val interactionHaptics = LocalInteractionHaptics.current
    val s = LocalStrings.current

    val slide = remember { Animatable(0.55f) }
    val backgroundCover = remember { Animatable(1f) }
    val enterMillis = GemTalkDesign.settingsPageDimMillis
    val exitMillis = GemTalkDesign.settingsExitPageMillis
    // The page is pre-mounted while the root curtain is still darkening, with
    // visible=false. That first LaunchedEffect run must not fire the exit
    // callback (it would unmount the page mid-curtain); only actual
    // visible=true → false transitions animate.
    var hasRunFirstPass by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (!hasRunFirstPass) {
            hasRunFirstPass = true
            if (!visible) return@LaunchedEffect
        }
        // Exits are not a temporal reversal of the entry curve — the page
        // leaves and its background cover darkens with a fast attack (most
        // of the darkening right as the clock starts, settle at the end),
        // and the whole exit runs at half the entry clock. The entry cover
        // reveal stays linear.
        coroutineScope {
            val clock = if (visible) enterMillis else exitMillis
            launch { slide.animateTo(if (visible) 1f else 0.55f, tween(clock, easing = GemTalkDesign.arrivalEase)) }
            launch {
                backgroundCover.animateTo(
                    if (visible) 0f else 1f,
                    tween(clock, easing = if (visible) LinearEasing else GemTalkDesign.arrivalEase),
                )
            }
        }
        if (visible) onEnterAnimationComplete() else onExitAnimationComplete()
    }

    BackHandler(enabled = visible) { onBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Start with nearly half the page already on screen so entry never begins offscreen.
            .graphicsLayer { translationX = (1f - slide.value) * size.width }
            .background(Ink),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // Header: back affordance like any pushed page
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {
                    interactionHaptics?.perform(HapticFeedbackType.TextHandleMove)
                    onBack()
                }) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = s.t("뒤로", "Back"),
                        tint = Mist,
                    )
                }
                Text(
                    text = s.t("설정", "Settings"),
                    style = MaterialTheme.typography.titleLarge,
                    color = Mist,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = GemTalkDesign.dialogHorizontalPadding,
                        vertical = GemTalkDesign.sectionSpacing,
                    ),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                // Language sits first — a device-wide preference, not a chat-detail toggle.
                LanguageDropdownRow(
                    languageTag = languageTag,
                    onLanguageTagChange = onLanguageTagChange,
                )

                Spacer(Modifier.height(12.dp))

                DirectSwitchRow(
                    title = s.t("모델 답변 시 햅틱", "Haptics during model responses"),
                    description = s.t(
                        "답변 텍스트가 새로 도착할 때마다 짧은 진동이 울립니다.",
                        "A short vibration marks each new response text chunk.",
                    ),
                    checked = hapticsEnabled,
                    onCheckedChange = onHapticsChange,
                )

                DirectSwitchRow(
                    title = s.t("모델 답변 시 효과음 재생", "Play sound effects during model responses"),
                    description = s.t(
                        "답변 텍스트가 새로 도착할 때마다 효과음을 재생합니다.",
                        "Plays a sound as each new response text chunk arrives.",
                    ),
                    checked = soundsEnabled,
                    onCheckedChange = onSoundsChange,
                )

                DirectSwitchRow(
                    title = s.t("모델 미리 예열 (빠른 첫 응답)", "Pre-warm model (faster first reply)"),
                    description = s.t(
                        "앱 실행 시 모델을 가속기에 미리 적재하여 첫 질문의 답변 시작을 빠르게 합니다. 배터리를 소량 더 소모할 수 있습니다.",
                        "Loads the model onto the accelerator in advance so the first reply starts sooner. May use slightly more battery.",
                    ),
                    checked = warmupEnabled,
                    onCheckedChange = onWarmupChange,
                )

                // Discreet on-device privacy statement
                Spacer(Modifier.height(24.dp))
                Text(
                    text = s.t(
                        "모든 벡터 추출 및 사진 분석은 기기 내부에서만 안전하게 실행되며, 외부 서버로 전송되지 않습니다. 온디바이스 모델에서 가장 매력적인 점이죠.",
                        "All vector extraction and photo analysis run on this device and are never sent to a server. That’s one of the best things about using an on-device model.",
                    ),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                    color = MistFaint,
                )
            }
        }
        if (backgroundCover.value > 0.001f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = backgroundCover.value))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            )
        }
    }
}

@Composable
private fun DirectSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
) {
    val interactionHaptics = LocalInteractionHaptics.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Mist,
            )
            description?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                    color = MistFaint,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = {
                interactionHaptics?.perform(HapticFeedbackType.TextHandleMove)
                onCheckedChange(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Ink,
                checkedTrackColor = Jade,
                checkedBorderColor = Jade,
                uncheckedThumbColor = MistMuted,
                uncheckedTrackColor = Ink.copy(alpha = 0f),
                uncheckedBorderColor = Hairline,
            ),
        )
    }
}
