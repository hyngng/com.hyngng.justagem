package com.hyngng.gemtalk.ui

import android.Manifest
import android.net.Uri
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Size
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hyngng.gemtalk.data.ChatMessage
import com.hyngng.gemtalk.data.ChatRole
import com.hyngng.gemtalk.data.ChatSession
import com.hyngng.gemtalk.audio.AppFeedbackCue
import com.hyngng.gemtalk.audio.FeedbackSoundPlayer
import com.hyngng.gemtalk.ui.components.GemConfirmDialog
import com.hyngng.gemtalk.ui.components.GemOrb
import com.hyngng.gemtalk.ui.components.GemTalkDesign
import com.hyngng.gemtalk.ui.components.GemTalkMotion
import com.hyngng.gemtalk.ui.components.GemTalkIconControl
import com.hyngng.gemtalk.ui.components.InteractionHaptics
import com.hyngng.gemtalk.ui.components.LocalInteractionHaptics
import com.hyngng.gemtalk.ui.components.IconControlTone
import com.hyngng.gemtalk.ui.components.MessageBubble
import com.hyngng.gemtalk.ui.AppSettingsScreen
import com.hyngng.gemtalk.ui.components.ModelStatusCard
import com.hyngng.gemtalk.ui.components.ShimmerBox
import com.hyngng.gemtalk.ui.components.ModelSelector
import com.hyngng.gemtalk.ui.components.OrbState
import com.hyngng.gemtalk.ui.components.ThinkingDots
import com.hyngng.gemtalk.ui.components.AlphaMaskEdge
import com.hyngng.gemtalk.ui.components.ChatBottomAlphaMaskCurve
import com.hyngng.gemtalk.ui.components.SoftAlphaMaskCurve
import com.hyngng.gemtalk.ui.components.verticalAlphaMask
import com.hyngng.gemtalk.ui.i18n.AppLanguage
import com.hyngng.gemtalk.ui.i18n.LocalStrings
import com.hyngng.gemtalk.ui.i18n.Strings
import com.hyngng.gemtalk.ui.theme.Graphite
import com.hyngng.gemtalk.ui.theme.GraphiteRaised
import com.hyngng.gemtalk.ui.theme.GraphiteSoft
import com.hyngng.gemtalk.ui.theme.Hairline
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import com.hyngng.gemtalk.ui.theme.Ice
import com.hyngng.gemtalk.ui.theme.Ink
import com.hyngng.gemtalk.ui.theme.Jade
import com.hyngng.gemtalk.ui.theme.Mist
import com.hyngng.gemtalk.ui.theme.MistFaint
import com.hyngng.gemtalk.ui.theme.MistMuted
import java.util.Locale
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@Composable
fun GemTalkApp(viewModel: ChatViewModel = viewModel()) {
    val state = viewModel.uiState
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val hapticFeedback = LocalHapticFeedback.current
    val interactionHaptics = remember(hapticFeedback) { InteractionHaptics(hapticFeedback) }
    val feedbackSoundPlayer = remember(context) { FeedbackSoundPlayer(context) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
    ) { uris ->
        val accepted = uris.mapNotNull { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            uri.toString()
        }
        viewModel.addAttachments(accepted)
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { saved ->
        val uri = pendingCameraUri
        if (saved && uri != null) viewModel.addAttachments(listOf(uri.toString()))
        else uri?.let { runCatching { context.contentResolver.delete(it, null, null) } }
        pendingCameraUri = null
    }
    var settingsOpen by remember { mutableStateOf(false) }
    var appSettingsOpen by remember { mutableStateOf(false) }
    var settingsMounted by remember { mutableStateOf(false) }
    var settingsTransitioning by remember { mutableStateOf(false) }
    val settingsScope = rememberCoroutineScope()
    val settingsDim = remember { Animatable(0f) }
    // The drawer dims to a full background cover half as fast on entry as on
    // exit; the covered dwell is what made opening settings feel sluggish.
    val settingsEntryDimMillis = GemTalkDesign.settingsEntryDimMillis
    val settingsExitDimMillis = GemTalkDesign.settingsExitDimMillis
    val settingsDimAlpha = GemTalkDesign.settingsPageDimAlpha

    val openAppSettings = {
        if (!settingsTransitioning && !settingsMounted) {
            settingsTransitioning = true
            settingsScope.launch {
                // Sequence per the transition design: darken the drawer to a
                // full background cover, then let the settings page (which
                // enters as full background color and brightens) reveal.
                // The page is mounted while the dim is still running, but
                // appSettingsOpen stays false — its cover overlay keeps it
                // visually identical to the background, and its enter
                // animations are driven by `visible`, so nothing moves yet.
                // When the curtain reaches full alpha the pre-composed page
                // starts its slide/cover fade on the next frame: the
                // fully-covered state exists only for a frame, not for a
                // whole mount+compose round-trip.
                settingsMounted = true
                settingsDim.snapTo(0f)
                settingsDim.animateTo(settingsDimAlpha, tween(settingsEntryDimMillis, easing = GemTalkDesign.arrivalEase))
                appSettingsOpen = true
            }
        }
    }
    val closeAppSettings = {
        if (!settingsTransitioning && appSettingsOpen) {
            settingsTransitioning = true
            appSettingsOpen = false
        }
    }

    val finishAppSettingsExit: () -> Unit = {
        settingsScope.launch {
            settingsMounted = false
            settingsDim.animateTo(0f, tween(settingsExitDimMillis, easing = GemTalkDesign.arrivalEase))
            settingsTransitioning = false
        }
    }

    DisposableEffect(feedbackSoundPlayer) {
        onDispose { feedbackSoundPlayer.close() }
    }

    LaunchedEffect(viewModel) {
        viewModel.feedbackEvents.collect { event ->
            if (event.cue == AppFeedbackCue.MESSAGE_SENT || viewModel.uiState.hapticsEnabled) {
                val haptic = when (event.cue) {
                    AppFeedbackCue.ANSWER_COMPLETED -> HapticFeedbackType.Confirm
                    AppFeedbackCue.MESSAGE_SENT,
                    AppFeedbackCue.THINKING_STARTED,
                    AppFeedbackCue.THINKING_FINISHED -> HapticFeedbackType.TextHandleMove
                    AppFeedbackCue.TEXT_CHARACTER,
                    AppFeedbackCue.THOUGHT_CHARACTER,
                    AppFeedbackCue.ANSWER_LEAD_IN -> HapticFeedbackType.KeyboardTap
                }
                interactionHaptics.perform(haptic)
            }
            if (event.allowSound && viewModel.uiState.soundsEnabled) {
                feedbackSoundPlayer.play(event.cue)
            }
        }
    }

    LaunchedEffect(viewModel, hapticFeedback, feedbackSoundPlayer) {
        var activeMessageId: String? = null
        var previousText = ""
        var previousThought = ""
        var answerLeadInPlayed = false
        snapshotFlow {
            val current = viewModel.uiState
            val message = current.messages.firstOrNull { it.id == current.streamingMessageId }
            // Voice mode reserves audio playback for TTS.
            Triple(
                current.streamingMessageId,
                message?.text.orEmpty(),
                Triple(
                    message?.thoughtText.orEmpty(),
                    current.hapticsEnabled,
                    current.soundsEnabled && !current.voice.active,
                ),
            )
        }.collect { (messageId, text, soundState) ->
            val (thoughtText, hapticsEnabled, soundsEnabled) = soundState
            if (messageId == null) {
                answerLeadInPlayed = false
            }
            if (messageId != activeMessageId) {
                activeMessageId = messageId
                previousText = ""
                previousThought = ""
            }
            if (messageId != null && text.startsWith(previousText)) {
                val appendedText = text.substring(previousText.length)
                val appendedCharacters = appendedText.codePointCount(0, appendedText.length)
                if (hapticsEnabled) {
                    repeat(appendedCharacters) {
                        interactionHaptics.perform(HapticFeedbackType.KeyboardTap, minimumIntervalMs = 0)
                    }
                }
                if (soundsEnabled) {
                    if (!answerLeadInPlayed) {
                        answerLeadInPlayed = true
                        feedbackSoundPlayer.play(AppFeedbackCue.ANSWER_LEAD_IN)
                        repeat((appendedCharacters - 1).coerceAtLeast(0)) {
                            feedbackSoundPlayer.play(AppFeedbackCue.TEXT_CHARACTER)
                        }
                    } else {
                        repeat(appendedCharacters) {
                            feedbackSoundPlayer.play(AppFeedbackCue.TEXT_CHARACTER)
                        }
                    }
                }
            }
            if (messageId != null && soundsEnabled && thoughtText.startsWith(previousThought)) {
                val appendedThought = thoughtText.substring(previousThought.length)
                val appendedCharacters = appendedThought.codePointCount(0, appendedThought.length)
                repeat(appendedCharacters) {
                    feedbackSoundPlayer.play(AppFeedbackCue.THOUGHT_CHARACTER)
                }
            }
            previousText = text
            previousThought = thoughtText
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.enterVoiceMode()
        } else {
            viewModel.showNotice(
                com.hyngng.gemtalk.ui.i18n.tr(
                    "음성 대화에는 마이크 권한이 필요해요.",
                    "Voice chat needs microphone permission.",
                ),
            )
        }
    }

    val openVoiceMode = {
        if (state.model.phase != ModelPhase.READY) {
            // Let the ViewModel explain why (checking/downloading/unavailable)
            // before asking for a microphone permission we cannot use yet.
            viewModel.enterVoiceMode()
        } else {
            // Drop the text field focus first: otherwise the keyboard stays
            // behind the voice overlay (and shrinks it).
            focusManager.clearFocus(force = true)
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                viewModel.enterVoiceMode()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val openDrawer = {
        // The drawer is a full-screen surface; a lingering IME would sit on top
        // of it and make the back gesture ambiguous.
        focusManager.clearFocus(force = true)
        viewModel.openDrawer()
    }

    LaunchedEffect(state.notice?.id) {
        val notice = state.notice ?: return@LaunchedEffect
        Toast.makeText(context, notice.text, Toast.LENGTH_SHORT).show()
        viewModel.clearNotice(notice.id)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.onAppBackgrounded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    BackHandler(enabled = state.voice.active) { viewModel.exitVoiceMode() }
    BackHandler(enabled = !state.voice.active && state.drawerOpen) {
        // If an IME is somehow still up, consume back for the keyboard first;
        // the next back closes the drawer. Normally openDrawer() already hid it.
        if (imeVisible) {
            focusManager.clearFocus(force = true)
        } else {
            viewModel.closeDrawer()
        }
    }
    BackHandler(enabled = !state.voice.active && !state.drawerOpen && state.editingMessageId != null) {
        focusManager.clearFocus(force = true)
        viewModel.cancelEditingMessage()
    }

    CompositionLocalProvider(
        LocalInteractionHaptics provides interactionHaptics,
        LocalStrings provides Strings(AppLanguage.device()),
        // Cursor and drag handles share Jade: one accent family for input
        // caret and touch affordance, per the final tone decision.
        LocalTextSelectionColors provides TextSelectionColors(
            handleColor = Jade,
            backgroundColor = Jade.copy(alpha = 0.25f),
        ),
    ) {
    val s = LocalStrings.current
    Box(Modifier.fillMaxSize()) {
        AmbientBackground()

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 840.dp)
                .fillMaxWidth()
                .statusBarsPadding()
                .align(Alignment.TopCenter),
        ) {
            ChatTopBar(
                model = state.model,
                modelEnabled = true,
                supportsTools = state.model.supportsTools,
                thinkingEnabled = state.thinkingEnabled,
                onThinkingChange = viewModel::setThinkingEnabled,
                onSelectModel = viewModel::selectModel,
                onMenu = openDrawer,
                onNewSession = viewModel::newSession,
                onSettings = { settingsOpen = true },
            )

            val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
            val composerBottomPadding = max(navBottom, imeBottom) + 12.dp
            // Keep the viewport clear of the bottom fade and composer dock.
            val listBottomPadding = composerBottomPadding + 156.dp

            var composerDockHeightPx by remember { mutableIntStateOf(0) }
            val density = LocalDensity.current
            // 텍스트 입력창 높이가 측정되기 전(첫 프레임) 안전한 초기 추정값
            val fallbackDockHeightPx = with(density) { (composerBottomPadding + 76.dp).toPx() }
            val effectiveDockHeightPx = if (composerDockHeightPx > 0) composerDockHeightPx.toFloat() else fallbackDockHeightPx

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                // The message viewport fades text out at the top using an alpha mask (DstIn)
                // so no hardcoded color is painted: text simply fades away, exposing
                // the exact underlying AmbientBackground identical to the transparent ChatTopBar.
                val messageListState = remember(state.activeId) { LazyListState() }
                // Gentle content transition when switching sessions, so a new or
                // selected session settles in instead of popping in abruptly.
                val sessionEnter = remember { Animatable(1f) }
                var lastAnimatedSessionId by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(state.activeId) {
                    if (lastAnimatedSessionId != null && lastAnimatedSessionId != state.activeId) {
                        sessionEnter.snapTo(0f)
                        sessionEnter.animateTo(
                            1f,
                            animationSpec = tween(220, easing = FastOutSlowInEasing),
                        )
                    }
                    lastAnimatedSessionId = state.activeId
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                            alpha = sessionEnter.value
                            translationY = (1f - sessionEnter.value) * 10.dp.toPx()
                        }
                        .verticalAlphaMask(
                            top = if (state.messages.isNotEmpty() && messageListState.canScrollBackward) {
                                AlphaMaskEdge(extent = 24.dp, curve = SoftAlphaMaskCurve)
                            } else {
                                null
                            },
                            // 화면 최하단이 아닌 텍스트 입력창(Composer) 상단 경계를 향해 1.0 -> 0.75(75% 불투명도)로 완만하게 페이드아웃한다.
                            bottom = if (state.messages.isNotEmpty()) {
                                AlphaMaskEdge(
                                    extent = 36.dp,
                                    insetPx = effectiveDockHeightPx,
                                    curve = ChatBottomAlphaMaskCurve,
                                )
                            } else {
                                null
                            },
                        ),
                ) {
                    if (state.messages.isEmpty()) {
                        EmptyState(
                            model = state.model,
                            isGenerating = state.isGenerating,
                            onSuggestion = viewModel::sendSuggestion,
                            onDownload = viewModel::downloadModel,
                            onRetry = viewModel::probeModel,
                            bottomPadding = listBottomPadding,
                        )
                    } else {
                        key(state.activeId) {
                            MessageList(
                                messages = state.messages,
                                streamingMessageId = state.streamingMessageId,
                                editingMessageId = state.editingMessageId,
                                toolCallPreparing = state.toolCallPreparing,
                                toolCallName = state.toolCallName,
                                onEditMessage = viewModel::startEditingMessage,
                                bottomPadding = listBottomPadding,
                                listState = messageListState,
                            )
                        }
                    }
                }

                // DstIn 알파 마스크(VerticalAlphaMask)가 메시지 뷰포트 하단을 75% 불투명도로 직접 제어하므로,
                // 도크에 별도 배경색을 칠하면 도크 내부 메시지가 이중 감쇠되어
                // 마스크 타겟 불투명도(75%)와의 경계에서 시각적 단차가 발생한다.
                // 따라서 도크의 중복 배경 없이 마스크의 75% 불투명도가 입력창 영역까지 매끄럽게 이어지도록 한다.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { composerDockHeightPx = it.height },
                ) {
                    if (state.messages.isNotEmpty() &&
                        state.model.phase != ModelPhase.READY &&
                        state.model.phase != ModelPhase.CHECKING
                    ) {
                        CompactModelBanner(
                            model = state.model,
                            onDownload = viewModel::downloadModel,
                            onRetry = viewModel::probeModel,
                        )
                    }

                    Composer(
                        draft = state.draft,
                        isGenerating = state.isGenerating,
                        attachmentUris = state.attachmentUris,
                        onRemoveAttachment = viewModel::removeAttachment,
                        isEditing = state.editingMessageId != null,
                        onDraftChange = viewModel::updateDraft,
                        onSend = { viewModel.send() },
                        onStop = viewModel::stopGeneration,
                        onVoice = openVoiceMode,
                        onOpenCamera = {
                            createCameraUri(context).also {
                                pendingCameraUri = it
                                cameraLauncher.launch(it)
                            }
                        },
                        onOpenGallery = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                    )
                }
            }
        }

        SessionDrawer(
            visible = state.drawerOpen,
            sessions = state.sessions,
            activeId = state.activeId,
            onClose = viewModel::closeDrawer,
            onSelect = viewModel::selectSession,
            onDelete = { viewModel.deleteSession(it.id) },
            onRename = viewModel::renameSession,
            onOpenSettings = openAppSettings,
        )

        VoiceOverlay(
            voice = state.voice,
            thinkingPreferenceEnabled = state.thinkingEnabled,
            onClose = viewModel::exitVoiceMode,
            onPrimary = viewModel::onVoicePrimaryAction,
        )

        if (settingsOpen) {
            GenerationSettingsDialog(
                temperature = state.temperature,
                topK = state.topK,
                maxOutputTokens = state.maxOutputTokens,
                prePrompt = state.prePrompt,
                photoIndexManager = viewModel.photoIndexManager,
                onStartIndexing = viewModel::startPhotoIndexing,
                onStopIndexing = viewModel::stopPhotoIndexing,
                onTemperatureChange = { viewModel.updateGenerationSettings(it, state.topK, state.maxOutputTokens) },
                onTopKChange = { viewModel.updateGenerationSettings(state.temperature, it, state.maxOutputTokens) },
                onMaxOutputTokensChange = { viewModel.updateGenerationSettings(state.temperature, state.topK, it) },
                onPrePromptChange = viewModel::updatePrePrompt,
                onReset = viewModel::resetGenerationSettings,
                onDismiss = {
                    viewModel.saveGenerationSettings()
                    settingsOpen = false
                },
            )
        }

        if (settingsDim.value > 0.001f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = settingsDim.value))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            )
        }

        if (settingsMounted) {
            AppSettingsScreen(
                visible = appSettingsOpen,
                hapticsEnabled = state.hapticsEnabled,
                soundsEnabled = state.soundsEnabled,
                warmupEnabled = state.warmupEnabled,
                onHapticsChange = { viewModel.updateFeedbackSettings(it, state.soundsEnabled) },
                onSoundsChange = { viewModel.updateFeedbackSettings(state.hapticsEnabled, it) },
                onWarmupChange = { viewModel.setWarmupEnabled(it) },
                onBack = closeAppSettings,
                onEnterAnimationComplete = { settingsTransitioning = false },
                onExitAnimationComplete = finishAppSettingsExit,
            )
        }
    }

    state.toolConfirmation?.let { pending ->
        GemConfirmDialog(
            title = pending.title,
            body = pending.detail,
            confirmLabel = s.t("허용", "Allow"),
            confirmColor = Jade,
            onConfirm = {
                interactionHaptics.perform(HapticFeedbackType.TextHandleMove)
                viewModel.resolveToolConfirmation(true)
            },
            onDismissRequest = {
                interactionHaptics.perform(HapticFeedbackType.TextHandleMove)
                viewModel.resolveToolConfirmation(false)
            },
            dismissLabel = s.t("취소", "Cancel"),
        )
    }

    }
}
@Composable
private fun AmbientBackground() {
    Box(Modifier.fillMaxSize().background(Ink))
}

@Composable
private fun ChatTopBar(
    model: ModelUi,
    modelEnabled: Boolean,
    supportsTools: Boolean,
    thinkingEnabled: Boolean,
    onThinkingChange: (Boolean) -> Unit,
    onSelectModel: (com.hyngng.gemtalk.ai.ModelOption) -> Unit,
    onMenu: () -> Unit,
    onNewSession: () -> Unit,
    onSettings: () -> Unit,
) {
    val s = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GemTalkIconControl(onClick = onMenu, tone = IconControlTone.Quiet) {
            Icon(
                Icons.Rounded.Menu,
                contentDescription = s.t("대화 목록", "Conversations"),
                modifier = Modifier.size(GemTalkDesign.iconSize),
            )
        }
        Spacer(Modifier.width(8.dp))
        // The selector is capped so the bar keeps breathing room for the action
        // icons on narrow phones; the popup mirrors this exact width.
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            ModelSelector(
                model = model,
                enabled = modelEnabled,
                supportsTools = supportsTools,
                thinkingEnabled = thinkingEnabled,
                onThinkingChange = onThinkingChange,
                onSelect = onSelectModel,
                modifier = Modifier
                    .widthIn(max = 160.dp)
                    .fillMaxWidth(),
            )
        }
        GemTalkIconControl(onClick = onSettings, tone = IconControlTone.Quiet) {
            Icon(Icons.Rounded.Tune, contentDescription = s.t("모델 설정", "Model settings"), modifier = Modifier.size(GemTalkDesign.iconSize))
        }
        GemTalkIconControl(onClick = onNewSession, tone = IconControlTone.Quiet) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = s.t("새 대화", "New chat"),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun EmptyState(
    model: ModelUi,
    isGenerating: Boolean,
    onSuggestion: (String) -> Unit,
    onDownload: () -> Unit,
    onRetry: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp = 22.dp,
) {
    val s = LocalStrings.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = bottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        item {
            GemOrb(
                state = if (isGenerating) OrbState.THINKING else OrbState.IDLE,
                modifier = Modifier.size(156.dp),
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
        item {
            Text(
                text = s.t("무엇이든 말해보세요", "Ask me anything"),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 33.sp,
                    lineHeight = 42.sp,
                    letterSpacing = (-0.9).sp,
                ),
                color = Mist,
                textAlign = TextAlign.Center,
            )
        }
        item { Spacer(Modifier.height(30.dp)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionChip(s.t("오늘의 상식", "Fun fact")) {
                    onSuggestion(
                        s.t(
                            "흥미롭고 유용한 과학 또는 일상 상식 하나 알려줘.",
                            "Tell me one interesting and useful science or everyday fact.",
                        ),
                    )
                }
                SuggestionChip(s.t("여행 계획", "Trip plan")) {
                    onSuggestion(s.t("주말 여행 계획을 세워줘.", "Plan a weekend trip for me."))
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionChip(s.t("저녁 메뉴", "Dinner idea")) {
                    onSuggestion(
                        s.t(
                            "간단하고 맛있게 만들어 먹기 좋은 저녁 메뉴 추천해줘.",
                            "Suggest an easy and delicious dinner I can make.",
                        ),
                    )
                }
                SuggestionChip(s.t("아이디어", "Idea")) {
                    onSuggestion(
                        s.t(
                            "오늘 해볼 만한 재미있는 아이디어를 하나 제안해줘.",
                            "Suggest one fun idea I could try today.",
                        ),
                    )
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
        item {
            when (model.phase) {
                ModelPhase.READY, ModelPhase.CHECKING -> {
                    Box(
                        Modifier
                            .widthIn(max = 360.dp)
                            .fillMaxWidth()
                            .height(18.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        ReadyLine(
                            model = model,
                            modifier = Modifier.alpha(if (model.phase == ModelPhase.READY) 1f else 0f),
                        )
                        AnimatedVisibility(
                            visible = model.phase == ModelPhase.CHECKING,
                            enter = fadeIn(tween(140)),
                            exit = fadeOut(tween(220)),
                        ) {
                            ShimmerBox(Modifier.width(176.dp).height(12.dp))
                        }
                    }
                }

                else -> ModelStatusCard(
                    model = model,
                    onDownload = onDownload,
                    onRetry = onRetry,
                    modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth(),
                )
            }
        }
    }
}
@Composable
private fun ReadyLine(model: ModelUi, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(Jade),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = if (model.fallback) {
                s.t("경량 모델로 준비됨", "Ready with a lightweight model")
            } else {
                s.t("E4B 준비됨", "E4B ready")
            },
            style = MaterialTheme.typography.labelSmall,
            color = MistFaint,
        )
        model.baseModel?.let {
            Text(
                text = "  ·  $it",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = MistFaint.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SuggestionChip(text: String, onClick: () -> Unit) {
    val interactionHaptics = LocalInteractionHaptics.current
    Surface(
        onClick = {
            interactionHaptics?.perform(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = Modifier.height(42.dp),
        shape = RoundedCornerShape(13.dp),
        color = GraphiteRaised.copy(alpha = 0.72f),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MistMuted,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    streamingMessageId: String?,
    editingMessageId: String?,
    toolCallPreparing: Boolean,
    toolCallName: String?,
    onEditMessage: (ChatMessage) -> Unit = {},
    bottomPadding: androidx.compose.ui.unit.Dp = 20.dp,
    listState: LazyListState = rememberLazyListState(),
) {
    val s = LocalStrings.current
    val conversationItems = remember(messages) {
        val pendingToolRecords = mutableListOf<ChatMessage>()
        val recordsByAssistant = mutableMapOf<String, List<ChatMessage>>()
        val visibleMessages = mutableListOf<ChatMessage>()
        var previousAssistantId: String? = null

        fun attachPendingRecords(assistantId: String?) {
            if (pendingToolRecords.isEmpty()) return
            if (assistantId == null) {
                visibleMessages += pendingToolRecords
            } else {
                recordsByAssistant[assistantId] =
                    recordsByAssistant[assistantId].orEmpty() + pendingToolRecords
            }
            pendingToolRecords.clear()
        }

        messages.forEach { message ->
            if (message.role == ChatRole.TOOL) {
                pendingToolRecords += message
            } else {
                if (pendingToolRecords.isNotEmpty()) {
                    val ownerId = if (message.role == ChatRole.ASSISTANT) message.id else previousAssistantId
                    attachPendingRecords(ownerId)
                }
                visibleMessages += message
                previousAssistantId = if (message.role == ChatRole.ASSISTANT) message.id else null
            }
        }
        attachPendingRecords(previousAssistantId)
        visibleMessages.map { message ->
            message to recordsByAssistant[message.id].orEmpty()
        }
    }
    // 사용자가 과거 메시지를 읽기 위해 위로 스크롤한 상태인지 감지
    var userScrolledUp by remember { mutableStateOf(false) }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            userScrolledUp = listState.canScrollForward
        }
    }

    val lastItem = conversationItems.lastOrNull()?.first
    val lastMessageId = lastItem?.id

    // 새 턴/메시지 발화 시 과거 탐색 모드 해제 및 바닥으로 이동
    LaunchedEffect(lastMessageId) {
        userScrolledUp = false
        val targetIndex = conversationItems.size + (if (toolCallPreparing) 1 else 0)
        listState.scrollToItem(targetIndex.coerceAtLeast(0))
    }

    // 스트리밍 답변 또는 Thinking 진행 중: 사용자가 위로 스크롤하지 않은 상태라면 바닥 앵커 유지
    val isStreaming = streamingMessageId != null
    val lastContentLength = (lastItem?.text?.length ?: 0) + (lastItem?.thoughtText?.length ?: 0)
    LaunchedEffect(lastContentLength, toolCallPreparing) {
        if (isStreaming && !userScrolledUp && !listState.isScrollInProgress) {
            val targetIndex = conversationItems.size + (if (toolCallPreparing) 1 else 0)
            listState.scrollToItem(targetIndex.coerceAtLeast(0))
        }
    }

    val editingIndex = conversationItems.indexOfFirst { it.first.id == editingMessageId }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        reverseLayout = false,
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 24.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(
            items = conversationItems,
            key = { _, (message, _) -> message.id },
            contentType = { _, (message, _) -> message.role },
        ) { messageIndex, (message, toolRecords) ->
            val targetAlpha by animateFloatAsState(
                targetValue = if (editingIndex >= 0 && messageIndex > editingIndex) 0.28f else 1f,
                animationSpec = tween(GemTalkDesign.motionStandard),
                label = "edited branch alpha",
            )
            MessageBubble(
                message = message,
                isStreaming = message.id == streamingMessageId,
                toolRecords = toolRecords,
                onEditMessage = onEditMessage,
                modifier = Modifier.alpha(targetAlpha),
            )
        }
        if (toolCallPreparing) {
            item(key = "tool-call-preparing", contentType = "activity") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ThinkingDots(modifier = Modifier.width(22.dp), color = MistMuted)
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = if (toolCallName.isNullOrBlank()) {
                            s.t("도구 사용 중", "Using a tool")
                        } else {
                            s.t("$toolCallName 사용 중", "Using $toolCallName")
                        },
                        style = MaterialTheme.typography.labelMedium.copy(lineHeight = 18.sp),
                        color = MistMuted,
                    )
                }
            }
        }
        item(key = "bottom-anchor") {
            Spacer(Modifier.height(1.dp))
        }
    }
}

@Composable
private fun CompactModelBanner(
    model: ModelUi,
    onDownload: () -> Unit,
    onRetry: () -> Unit,
) {
    val interactionHaptics = LocalInteractionHaptics.current
    val s = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(GraphiteRaised.copy(alpha = 0.9f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (model.phase == ModelPhase.DOWNLOADABLE) {
                Icons.AutoMirrored.Rounded.VolumeUp
            } else {
                Icons.Rounded.ErrorOutline
            },
            contentDescription = null,
            tint = when (model.phase) {
                ModelPhase.DOWNLOADABLE -> MaterialTheme.colorScheme.tertiary
                ModelPhase.DOWNLOADING -> Ice
                else -> MaterialTheme.colorScheme.error
            },
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = when (model.phase) {
                ModelPhase.DOWNLOADABLE -> s.t("모델 준비가 필요해요", "Model needs setup")
                ModelPhase.DOWNLOADING -> s.t("모델을 내려받는 중이에요", "Downloading model")
                ModelPhase.CHECKING -> ""
                ModelPhase.UNAVAILABLE, ModelPhase.ERROR -> s.t("모델을 사용할 수 없어요", "Model unavailable")
                ModelPhase.READY -> ""
            },
            style = MaterialTheme.typography.labelSmall,
            color = MistMuted,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = {
                interactionHaptics?.perform(HapticFeedbackType.TextHandleMove)
                if (model.phase == ModelPhase.DOWNLOADABLE) onDownload() else onRetry()
            },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                text = if (model.phase == ModelPhase.DOWNLOADABLE) s.t("내려받기", "Download") else s.t("다시 확인", "Retry"),
                style = MaterialTheme.typography.labelSmall,
                color = Jade,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun Composer(
    draft: String,
    isGenerating: Boolean,
    attachmentUris: List<String>,
    onRemoveAttachment: (String) -> Unit,
    isEditing: Boolean = false,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onVoice: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenGallery: () -> Unit,
) {
    val s = LocalStrings.current
    var attachmentSheetOpen by remember { mutableStateOf(false) }
    val interactionHaptics = LocalInteractionHaptics.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val sheetScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val imeVisible = imeInsets.getBottom(density) > 0
    val navBottom = androidx.compose.foundation.layout.WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
    val imeBottom = androidx.compose.foundation.layout.WindowInsets.ime
        .asPaddingValues()
        .calculateBottomPadding()
    // Match the composer card's 12dp side inset (matching model response width) so it has equal breathing room above the IME/nav bar.
    val bottomPadding = max(navBottom, imeBottom) + 12.dp

    val focusRequester = remember { FocusRequester() }
    // TextFieldValue keeps the caret under program control: entering edit
    // mode places the cursor past the last character and raises the keyboard
    // immediately, so the field is ready to type the moment it appears.
    var fieldValue by remember { mutableStateOf(TextFieldValue("")) }
    LaunchedEffect(isEditing) {
        if (isEditing) {
            fieldValue = TextFieldValue(draft, TextRange(draft.length))
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    // External draft writes (send clearing the field, canceling an edit)
    // re-sync here; typing flows through onValueChange and stays a no-op.
    LaunchedEffect(draft) {
        if (fieldValue.text != draft) {
            fieldValue = TextFieldValue(draft, TextRange(draft.length))
        }
    }

    // The field grows naturally and action targets remain at least 48dp.
    // Aligning its text at the top keeps the left and top inset visually even.
    val inputStyle = MaterialTheme.typography.bodyLarge.copy(color = Mist)

    val animatedBorderColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isEditing) Jade.copy(alpha = 0.65f) else Hairline,
        animationSpec = tween(durationMillis = 200),
        label = "composer border color",
    )
    val openAttachmentSheet: () -> Unit = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        if (imeVisible) {
            sheetScope.launch {
                snapshotFlow { imeInsets.getBottom(density) }.first { it == 0 }
                delay(80)
                attachmentSheetOpen = true
            }
        } else {
            attachmentSheetOpen = true
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = bottomPadding, top = 0.dp),
        shape = RoundedCornerShape(28.dp),
        color = GraphiteRaised,
        border = androidx.compose.foundation.BorderStroke(1.dp, animatedBorderColor),
        shadowElevation = 0.dp,
    ) {
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)) {
            if (attachmentUris.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(attachmentUris, key = { it }) { uri ->
                        ComposerImageAttachment(uri, onRemove = { onRemoveAttachment(uri) })
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            BasicTextField(
                value = fieldValue,
                onValueChange = { value ->
                    fieldValue = value
                    onDraftChange(value.text)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                textStyle = inputStyle,
                maxLines = 5,
                // Cursor (blink line) in Jade; the drag handle below follows
                // the app-wide Ice→Mist TextSelectionColors override.
                cursorBrush = SolidColor(Jade),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp)
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (draft.isEmpty()) {
                            Text(
                                if (isEditing) s.t("수정할 내용 입력", "Edit your message") else s.t("메시지 보내기", "Send a message"),
                                style = inputStyle.copy(color = MistFaint),
                            )
                        }
                        inner()
                    }
                },
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GemTalkIconControl(
                    onClick = openAttachmentSheet,
                    tone = IconControlTone.Neutral,
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = s.t("이미지 첨부", "Attach image"),
                        modifier = Modifier.size(GemTalkDesign.compactIconSize),
                    )
                }
                Spacer(Modifier.weight(1f))
                GemTalkIconControl(
                    onClick = onVoice,
                    tone = IconControlTone.Neutral,
                    enabled = !isGenerating,
                ) {
                    Icon(
                        Icons.Rounded.Mic,
                        contentDescription = s.t("음성 대화", "Voice chat"),
                        modifier = Modifier.size(GemTalkDesign.compactIconSize),
                    )
                }
                Spacer(Modifier.width(8.dp))
                GemTalkIconControl(
                    onClick = { if (isGenerating) onStop() else onSend() },
                    tone = when {
                        isGenerating -> IconControlTone.Destructive
                        draft.isNotBlank() || attachmentUris.isNotEmpty() -> IconControlTone.Accent
                        else -> IconControlTone.Neutral
                    },
                ) {
                    Icon(
                        imageVector = if (isGenerating) Icons.Rounded.Stop else Icons.Rounded.ArrowUpward,
                        contentDescription = if (isGenerating) {
                            s.t("응답 중지", "Stop response")
                        } else if (isEditing) {
                            s.t("수정 완료", "Save edit")
                        } else {
                            s.t("보내기", "Send")
                        },
                        modifier = Modifier.size(GemTalkDesign.iconSize),
                    )
                }
            }
        }
    }

    if (attachmentSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { attachmentSheetOpen = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = GraphiteRaised,
            contentColor = Mist,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Jade) },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
            ) {
                Text(
                    text = s.t("이미지 첨부", "Attach an image"),
                    style = MaterialTheme.typography.titleMedium,
                    color = Mist,
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AttachmentChoiceBlock(
                        modifier = Modifier.weight(1f),
                        label = s.t("카메라", "Camera"),
                        icon = Icons.Rounded.CameraAlt,
                        onClick = {
                            interactionHaptics?.perform(HapticFeedbackType.TextHandleMove)
                            attachmentSheetOpen = false
                            onOpenCamera()
                        },
                    )
                    AttachmentChoiceBlock(
                        modifier = Modifier.weight(1f),
                        label = s.t("사진", "Photos"),
                        icon = Icons.Rounded.PhotoLibrary,
                        onClick = {
                            interactionHaptics?.perform(HapticFeedbackType.TextHandleMove)
                            attachmentSheetOpen = false
                            onOpenGallery()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachmentChoiceBlock(
    modifier: Modifier = Modifier,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = GraphiteSoft,
        border = androidx.compose.foundation.BorderStroke(1.dp, Hairline.copy(alpha = 0.35f)),
        contentColor = Mist,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 116.dp)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(icon, contentDescription = null, tint = Jade, modifier = Modifier.size(26.dp))
                Text(label, style = MaterialTheme.typography.titleSmall, color = Mist)
            }
        }
    }
}

@Composable
private fun ComposerImageAttachment(uriString: String, onRemove: () -> Unit) {
    val context = LocalContext.current
    val uri = remember(uriString) { Uri.parse(uriString) }
    var bitmap by remember(uriString) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uriString) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(uri, Size(180, 180), null)
                } else {
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                }?.asImageBitmap()
            }.getOrNull()
        }
    }
    Box(Modifier.size(72.dp)) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = LocalStrings.current.t("첨부한 사진", "Attached photo"),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)),
            )
        } else {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).background(GraphiteSoft))
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(26.dp)
                .clickable(
                    role = Role.Button,
                    onClickLabel = LocalStrings.current.t("사진 제거", "Remove photo"),
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onRemove,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier.size(18.dp).background(Graphite.copy(alpha = 0.9f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = null,
                    modifier = Modifier.size(11.dp),
                    tint = Mist,
                )
            }
        }
    }
}

private fun createCameraUri(context: android.content.Context): Uri {
    val directory = File(context.filesDir, "attachments").apply { mkdirs() }
    val imageFile = File(directory, "camera-${UUID.randomUUID()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.files", imageFile)
}
