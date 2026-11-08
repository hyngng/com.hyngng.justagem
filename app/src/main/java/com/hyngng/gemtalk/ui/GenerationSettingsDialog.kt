package com.hyngng.gemtalk.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hyngng.gemtalk.ui.components.AlphaMaskEdge
import com.hyngng.gemtalk.ui.components.SoftAlphaMaskCurve
import com.hyngng.gemtalk.ui.components.verticalAlphaMask
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.hyngng.gemtalk.media.PhotoSemanticIndexManager
import com.hyngng.gemtalk.ui.components.LocalInteractionHaptics
import com.hyngng.gemtalk.ui.components.GemConfirmDialog
import com.hyngng.gemtalk.ui.components.GemDialogActionText
import com.hyngng.gemtalk.ui.components.GemTalkDesign
import com.hyngng.gemtalk.ui.components.GemTalkMotion
import com.hyngng.gemtalk.ui.components.ThinkingDots
import com.hyngng.gemtalk.ui.i18n.LocalStrings
import com.hyngng.gemtalk.ui.theme.GraphiteRaised
import com.hyngng.gemtalk.ui.theme.GraphiteSoft
import com.hyngng.gemtalk.ui.theme.Hairline
import com.hyngng.gemtalk.ui.theme.Ice
import com.hyngng.gemtalk.ui.theme.Ink
import com.hyngng.gemtalk.ui.theme.Jade
import com.hyngng.gemtalk.ui.theme.Mist
import com.hyngng.gemtalk.ui.theme.MistFaint
import com.hyngng.gemtalk.ui.theme.MistMuted
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun GenerationSettingsDialog(
    temperature: Float,
    topK: Int,
    maxOutputTokens: Int,
    prePrompt: String,
    photoIndexManager: PhotoSemanticIndexManager,
    onStartIndexing: () -> Unit,
    onStopIndexing: () -> Unit,
    onTemperatureChange: (Float) -> Unit,
    onTopKChange: (Int) -> Unit,
    onMaxOutputTokensChange: (Int) -> Unit,
    onPrePromptChange: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val s = LocalStrings.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = GemTalkDesign.dialogHorizontalPadding),
        ) {
            val availableHeight = maxHeight
            val interactionHaptics = LocalInteractionHaptics.current

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = availableHeight * 0.88f),
                shape = RoundedCornerShape(26.dp),
                color = GraphiteRaised,
                shadowElevation = 12.dp,
            ) {
                Column(Modifier.fillMaxWidth()) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = GemTalkDesign.dialogHorizontalPadding,
                                vertical = GemTalkDesign.dialogHeaderTopPadding,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = s.t("모델 설정", "Model settings"),
                            style = MaterialTheme.typography.titleLarge,
                            color = Mist,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = {
                            interactionHaptics?.perform(HapticFeedbackType.TextHandleMove)
                            onDismiss()
                        }) {
                            Icon(Icons.Rounded.Close, contentDescription = s.t("닫기", "Close"), tint = MistMuted)
                        }
                    }

                    // Scrollable content: Gemma generation parameters plus the
                    // EmbeddingGemma index section.
                    val settingsScrollState = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                            .verticalAlphaMask(
                                top = if (settingsScrollState.canScrollBackward) {
                                    AlphaMaskEdge(extent = 24.dp, curve = SoftAlphaMaskCurve)
                                } else {
                                    null
                                },
                                bottom = if (settingsScrollState.canScrollForward) {
                                    AlphaMaskEdge(extent = 24.dp, curve = SoftAlphaMaskCurve)
                                } else {
                                    null
                                },
                            ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(settingsScrollState)
                                .padding(
                                    horizontal = GemTalkDesign.dialogHorizontalPadding,
                                    vertical = GemTalkDesign.sectionSpacing,
                                ),
                            verticalArrangement = Arrangement.spacedBy(GemTalkDesign.sectionSpacing),
                        ) {
                            // Section header matches the EmbeddingGemma one so both
                            // groups read as peers.
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Gemma",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Jade,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            SettingSlider(
                                title = s.t("Temperature (0.00-1.00)", "Temperature (0.00-1.00)"),
                                value = temperature,
                                display = "%.4f".format(temperature),
                                range = 0f..1f,
                                valueStep = 0.0001f,
                                onValueChange = onTemperatureChange,
                            )
                            SettingSlider(
                                title = s.t("Top K (1-128)", "Top K (1-128)"),
                                value = topK.toFloat(),
                                display = topK.toString(),
                                range = 1f..128f,
                                valueStep = 1f,
                            ) { onTopKChange(it.roundToInt().coerceIn(1, 128)) }
                            SettingSlider(
                                title = s.t("최대 출력 토큰 (128-4096)", "Max output tokens (128-4096)"),
                                value = maxOutputTokens.toFloat(),
                                display = maxOutputTokens.toString(),
                                range = 128f..4096f,
                                valueStep = 1f,
                            ) {
                                onMaxOutputTokensChange(it.roundToInt().coerceIn(128, 4096))
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(GemTalkDesign.compactSpacing)) {
                                Text(s.t("시스템 지시 및 사전 프롬프트", "System instructions and pre-prompt"), style = MaterialTheme.typography.bodyMedium, color = Mist)
                                OutlinedTextField(
                                    value = prePrompt,
                                    onValueChange = onPrePromptChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3,
                                    maxLines = 6,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = settingsFieldColors(),
                                )
                            }

                            // ==========================================
                            // Section 2: EmbeddingGemma (사진 의미 검색 색인)
                            // ==========================================
                            Spacer(Modifier.height(GemTalkDesign.sectionSpacing))
                            EmbeddingGemmaSection(
                                photoIndexManager = photoIndexManager,
                                onStartIndexing = onStartIndexing,
                                onStopIndexing = onStopIndexing,
                            )

                            Spacer(Modifier.height(4.dp))
                        }
                    }

                    // Footer Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            // Footer keeps the equal bottom/right inset rule
                            // shared with GemDialogActionRow (14dp here).
                            .padding(start = 14.dp, top = 8.dp, end = 14.dp, bottom = 14.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GemDialogActionText(
                            label = s.t("기본값", "Defaults"),
                            color = MistMuted,
                            onClick = onReset,
                        )
                        GemDialogActionText(
                            label = s.t("저장", "Save"),
                            color = Jade,
                            onClick = onDismiss,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmbeddingGemmaSection(
    photoIndexManager: PhotoSemanticIndexManager,
    onStartIndexing: () -> Unit,
    onStopIndexing: () -> Unit,
) {
    val context = LocalContext.current
    val interactionHaptics = LocalInteractionHaptics.current
    val s = LocalStrings.current
    val indexState by photoIndexManager.indexingState.collectAsState()

    val permissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        onStartIndexing()
    }

    fun startIndexingWithNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onStartIndexing()
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result.values.any { it }
        if (granted) {
            interactionHaptics?.perform(HapticFeedbackType.Confirm)
            startIndexingWithNotificationPermission()
        }
    }

    fun checkPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    var confirmStart by remember { mutableStateOf(false) }

    // Make the section self-sufficient: if the app-level load has not finished
    // (or never started), opening settings kicks it off. loadIndex() is
    // idempotent and mutex-guarded.
    LaunchedEffect(photoIndexManager) {
        photoIndexManager.loadIndex()
    }

    fun beginIndexing() {
        if (checkPermissionGranted()) {
            startIndexingWithNotificationPermission()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    val totalCount = indexState.totalDeviceCount.coerceAtLeast(indexState.indexedCount)
    val progress by animateFloatAsState(
        targetValue = indexState.currentProgress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 320),
        label = "photo index progress",
    )
    // While the saved index loads: a subtle blue-tinted gray (secondary
    // container) — slightly brighter than the modal background. Once loading
    // resolves it animates to Jade alongside the real progress value.
    val barColor by animateColorAsState(
        targetValue = if (indexState.isLoading) MaterialTheme.colorScheme.secondaryContainer else Jade,
        animationSpec = tween(durationMillis = 320),
        label = "photo index bar color",
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(GemTalkDesign.sectionSpacing),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "EmbeddingGemma",
                style = MaterialTheme.typography.titleSmall,
                color = Jade,
                modifier = Modifier.weight(1f),
            )
        }

        val isComplete = !indexState.isIndexing && !indexState.isLoading &&
            !indexState.modelUnavailable && indexState.retryableFailureCount == 0 &&
            indexState.currentProgress >= 1f
        val busy = indexState.isStopping || indexState.isLoading || isComplete
        val targetContainerColor = if (busy) {
            Jade.copy(alpha = 0.22f)
        } else {
            Ice
        }
        val targetContentColor = if (busy) Jade else Ink
        val animatedContainerColor by animateColorAsState(
            targetValue = targetContainerColor,
            animationSpec = tween(durationMillis = GemTalkMotion.feedbackColorMillis),
            label = "photo indexing button container color",
        )
        val animatedContentColor by animateColorAsState(
            targetValue = targetContentColor,
            animationSpec = tween(durationMillis = GemTalkMotion.feedbackColorMillis),
            label = "photo indexing button content color",
        )

        // Gemma 섹션 헤더와 하위 컨트롤 간의 상단 기준 정렬 및 간격(sectionSpacing, 16dp)과 일치시키기 위해,
        // 버튼 높이로 인해 좌측 타이틀 텍스트가 아래로 처지지 않도록 verticalAlignment를 Alignment.Top으로 둔다.
        // IntrinsicSize.Min makes the button stretch to exactly the height of the
        // label+progress bundle on the left; its 96dp width mirrors the numeric
        // field slot of the sliders above, so the bar tracks align.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(GemTalkDesign.compactSpacing),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (indexState.isLoading) {
                            s.t("사진 색인을 불러오는 중", "Loading photo index")
                        } else {
                            s.t("색인된 사진", "Indexed photos")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Mist,
                    )
                    Spacer(Modifier.weight(1f))
                    if (indexState.isLoading) {
                        // The thinking indicator reuse: drop the override so it
                        // renders in the same Jade the stream preview uses.
                        ThinkingDots()
                    } else {
                        Text(
                            text = "${"%,d".format(indexState.indexedCount)}/${"%,d".format(totalCount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MistMuted,
                        )
                    }
                }

                LinearProgressIndicator(
                    // While loading, the bar is gray (a neutral placeholder); once
                    // loading resolves the fill color animates to Jade and the
                    // value tweens into the real index progress.
                    progress = { if (indexState.isLoading) 1f else progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = barColor,
                    trackColor = GraphiteSoft,
                )
            }

            Spacer(Modifier.width(10.dp))

            Button(
                onClick = {
                    interactionHaptics?.perform(HapticFeedbackType.TextHandleMove)
                    if (indexState.isIndexing) {
                        onStopIndexing()
                    } else {
                        confirmStart = true
                    }
                },
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = animatedContainerColor,
                    contentColor = animatedContentColor,
                    disabledContainerColor = animatedContainerColor,
                    disabledContentColor = animatedContentColor,
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                // Icon+text act as one logical unit, centered in the fixed slot.
                modifier = Modifier
                    .width(96.dp)
                    .fillMaxHeight(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                Icon(
                    when {
                        indexState.isIndexing -> Icons.Rounded.Stop
                        isComplete -> Icons.Rounded.Check
                        else -> Icons.Rounded.PlayArrow
                    },
                    contentDescription = null,
                    tint = animatedContentColor,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    when {
                        indexState.isStopping -> s.t("중지 중", "Stopping")
                        indexState.isLoading -> s.t("확인 중", "Checking")
                        indexState.isIndexing -> s.t("색인 중지", "Stop")
                        isComplete -> s.t("색인 완료", "Complete")
                        else -> s.t("색인하기", "Index")
                    },
                    color = animatedContentColor,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
                }
            }
        }
    }

    if (confirmStart) {
        val analysisText = buildString {
            append(
                s.t(
                    "사진 한 장마다 온디바이스 모델이 벡터를 계산하므로 수 분 이상 걸릴 수 있고 배터리를 사용합니다. ",
                    "An on-device model computes a vector for each photo, so it can take several minutes and use battery. ",
                ),
            )
            append(
                s.t(
                    "중간에 중지해도 색인된 사진은 유지되어 다음에 이어서 진행됩니다.",
                    "Stopping partway keeps the photos already indexed and resumes from there next time.",
                ),
            )
        }
        GemConfirmDialog(
            title = s.t("사진 색인을 시작할까요?", "Start indexing photos?"),
            body = analysisText,
            bodyStyle = MaterialTheme.typography.bodySmall,
            confirmLabel = s.t("시작", "Start"),
            confirmColor = Jade,
            onConfirm = {
                confirmStart = false
                interactionHaptics?.perform(HapticFeedbackType.Confirm)
                beginIndexing()
            },
            onDismissRequest = { confirmStart = false },
            dismissLabel = s.t("취소", "Cancel"),
        )
    }
}

@Composable
private fun settingsFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Mist, unfocusedTextColor = MistMuted,
    focusedBorderColor = Jade, unfocusedBorderColor = Hairline,
    focusedLabelColor = Jade, unfocusedLabelColor = MistFaint,
)

@Composable
private fun SettingSlider(
    title: String,
    value: Float,
    display: String,
    range: ClosedFloatingPointRange<Float>,
    valueStep: Float,
    onValueChange: (Float) -> Unit,
) {
    val interactionHaptics = LocalInteractionHaptics.current
    val focusManager = LocalFocusManager.current
    val s = LocalStrings.current
    val decimal = valueStep < 1f
    val normalizedValue = remember(value, valueStep) {
        (value / valueStep).roundToInt() * valueStep
    }
    var lastFeedbackValue by remember(normalizedValue) { mutableStateOf(normalizedValue) }
    var sliderValue by remember(value) { mutableStateOf(value) }
    var input by remember(value) { mutableStateOf(display) }
    var editing by remember { mutableStateOf(false) }

    fun canonical(f: Float): String =
        if (decimal) "%.4f".format(f) else f.roundToInt().toString()

    fun updateValue(newVal: Float) {
        val stepped = (newVal / valueStep).roundToInt() * valueStep
        val coerced = stepped.coerceIn(range.start, range.endInclusive)
        sliderValue = coerced
        onValueChange(coerced)
    }

    fun commit() {
        val parsed = input.trim().replace(',', '.').toFloatOrNull()
        if (parsed == null) {
            input = canonical(sliderValue)
        } else {
            val stepped = (parsed / valueStep).roundToInt() * valueStep
            val coerced = stepped.coerceIn(range.start, range.endInclusive)
            input = canonical(coerced)
            sliderValue = coerced
            onValueChange(coerced)
        }
        editing = false
        focusManager.clearFocus()
    }

    Column {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = Mist)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Slider(
                value = sliderValue.coerceIn(range),
                onValueChange = { nextValue ->
                    editing = false
                    val nextFeedbackValue = (nextValue / valueStep).roundToInt() * valueStep
                    if (nextFeedbackValue != lastFeedbackValue) {
                        lastFeedbackValue = nextFeedbackValue
                        interactionHaptics?.perform(HapticFeedbackType.TextHandleMove)
                    }
                    val coerced = nextFeedbackValue.coerceIn(range)
                    sliderValue = coerced
                    input = canonical(coerced)
                    updateValue(coerced)
                },
                valueRange = range,
                steps = 0,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            OutlinedTextField(
                value = input,
                onValueChange = {
                    editing = true
                    input = it
                    it.trim().replace(',', '.').toFloatOrNull()?.let { parsed ->
                        val stepped = (parsed / valueStep).roundToInt() * valueStep
                        val coerced = stepped.coerceIn(range.start, range.endInclusive)
                        sliderValue = coerced
                        onValueChange(coerced)
                    }
                },
                modifier = Modifier.width(96.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.labelLarge.copy(color = Mist),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { commit() }),
                colors = settingsFieldColors(),
                shape = RoundedCornerShape(8.dp),
                isError = editing && input.trim().toFloatOrNull() == null,
            )
        }
        if (editing && input.trim().toFloatOrNull() == null) {
            Text(
                text = s.t(
                    "0-${range.endInclusive.roundToInt()} 사이의 ${if (decimal) "소수" else "정수"}를 입력하세요",
                    "Enter a ${if (decimal) "decimal" else "whole number"} between 0 and ${range.endInclusive.roundToInt()}",
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
