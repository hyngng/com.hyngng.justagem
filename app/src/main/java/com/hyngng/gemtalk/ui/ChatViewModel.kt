package com.hyngng.gemtalk.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.genai.common.FeatureStatus
import com.hyngng.gemtalk.ai.DownloadEvent
import com.hyngng.gemtalk.ai.GemmaEngine
import com.hyngng.gemtalk.ai.GenerationUpdate
import com.hyngng.gemtalk.ai.ModelOption
import com.hyngng.gemtalk.ai.DEFAULT_SYSTEM_INSTRUCTIONS
import com.hyngng.gemtalk.ai.tools.ToolConfirmation
import com.hyngng.gemtalk.ai.tools.buildLocalTools
import com.hyngng.gemtalk.audio.SpeechEvent
import com.hyngng.gemtalk.audio.SpeechInput
import com.hyngng.gemtalk.audio.SpeechOutput
import com.hyngng.gemtalk.audio.AppFeedbackCue
import com.hyngng.gemtalk.audio.AppFeedbackEvent
import com.hyngng.gemtalk.data.ChatMessage
import com.hyngng.gemtalk.data.ChatRole
import com.hyngng.gemtalk.data.ChatSession
import com.hyngng.gemtalk.data.SessionStore
import com.hyngng.gemtalk.data.titleFrom
import com.hyngng.gemtalk.media.PhotoSemanticIndexManager
import com.hyngng.gemtalk.ui.i18n.tr
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

enum class ModelPhase { CHECKING, DOWNLOADABLE, DOWNLOADING, READY, UNAVAILABLE, ERROR }

data class ModelUi(
    val phase: ModelPhase = ModelPhase.CHECKING,
    val label: String = GemmaEngine.LABEL_E4B,
    val baseModel: String? = null,
    val status: Int? = null,
    val detail: String? = null,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val fallback: Boolean = false,
    val error: String? = null,
    val options: List<ModelOption> = emptyList(),
    val supportsTools: Boolean = false,
    val pendingSelection: Boolean = false,
)

enum class VoicePhase { IDLE, LISTENING, THINKING, SPEAKING }

data class VoiceUi(
    val active: Boolean = false,
    val phase: VoicePhase = VoicePhase.IDLE,
    val transcript: String = "",
    val caption: String = "",
    val captionProgress: Float = 0f,
    val speechCharacterIndex: Int = 0,
    val speechLevel: Float = 0f,
    val inputLevel: Float = 0f,
    val hint: String = "",
)

data class Notice(val id: Long, val text: String)

data class PendingToolConfirmation(
    val title: String,
    val detail: String,
)

data class ChatUiState(
    val sessions: List<ChatSession> = emptyList(),
    val activeId: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val attachmentUris: List<String> = emptyList(),
    val editingMessageId: String? = null,
    val isGenerating: Boolean = false,
    val streamingMessageId: String? = null,
    val toolCallPreparing: Boolean = false,
    val toolCallName: String? = null,
    val model: ModelUi = ModelUi(),
    val voice: VoiceUi = VoiceUi(),
    val drawerOpen: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val soundsEnabled: Boolean = true,
    // User-controlled warm up; off trades first-turn latency for battery.
    val warmupEnabled: Boolean = true,
    val prePrompt: String = DEFAULT_SYSTEM_INSTRUCTIONS,
    val thinkingEnabled: Boolean = false,
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val maxOutputTokens: Int = 900,
    val notice: Notice? = null,
    val toolConfirmation: PendingToolConfirmation? = null,
)

/**
 * Single owner of conversation state.
 *
 * Persistence points are deliberately limited to user messages and turn ends;
 * streaming tokens only touch Compose state. The ML Kit Prompt API is stateless,
 * so a restored session can be continued by replaying its transcript, even after
 * the process was killed.
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val store = SessionStore(application)
    private val engine = GemmaEngine(application)
    private val speechIn = SpeechInput(application)
    private val speechOut = SpeechOutput(application)
    val photoIndexManager = PhotoSemanticIndexManager(application)

    var uiState by mutableStateOf(ChatUiState())
        private set

    private var probeJob: Job? = null
    private var downloadJob: Job? = null
    // Pre-loads the accelerator once after the model turns READY (probe or
    // download), or on the first composer touch, so the first real turn
    // skips cold start.
    private var warmupJob: Job? = null
    private var generationJob: Job? = null
    private val persistLock = Any()
    private var persistFailureNoticeShown = false
    private var pendingPersist: SessionStore.PersistedState? = null
    private var persistJob: Job? = null
    private var titleJob: Job? = null
    private var titleJobSessionId: String? = null
    private var listenJob: Job? = null
    private var inputPulseJob: Job? = null
    private var inputPulseVersion = 0L
    private var speakJob: Job? = null
    private var speechQueueJob: Job? = null
    private var speechChannel: Channel<String>? = null
    private var autoListenJob: Job? = null
    private var persistedStateLoaded = false
    private var toolConfirmation: CompletableDeferred<Boolean>? = null
    // Model chosen during streaming; applied when the current turn ends.
    private var pendingModel: ModelOption? = null
    private val noticeIds = AtomicLong(0L)
    private val _feedbackEvents = MutableSharedFlow<AppFeedbackEvent>(extraBufferCapacity = 8)
    val feedbackEvents = _feedbackEvents.asSharedFlow()

    init {
        // Remove legacy feature stores after their code paths have been removed.
        getApplication<Application>().filesDir.listFiles { file ->
            file.name == "gemtalk_memories.json" || file.name.startsWith("gemtalk_memories.json.") ||
                file.name == "gemtalk_memories.json.tmp" || file.name == "gemtalk_documents.json" ||
                file.name == "gemtalk_documents.json.tmp"
        }?.forEach { runCatching { it.delete() } }
        probeModel()
        viewModelScope.launch(Dispatchers.IO) {
            val persisted = store.load()
            val sessions = persisted.sessions.filter { it.messages.isNotEmpty() }
            withContext(kotlinx.coroutines.Dispatchers.Main.immediate) {
                if (!persistedStateLoaded) {
                    val defaults = ChatUiState()
                    val current = uiState
                    uiState = uiState.copy(
                        sessions = if (current.sessions.isEmpty()) sessions else current.sessions,
                        activeId = current.activeId,
                        messages = current.messages,
                        hapticsEnabled = if (current.hapticsEnabled == defaults.hapticsEnabled) persisted.hapticsEnabled else current.hapticsEnabled,
                        soundsEnabled = if (current.soundsEnabled == defaults.soundsEnabled) persisted.soundsEnabled else current.soundsEnabled,
                        warmupEnabled = if (current.warmupEnabled == defaults.warmupEnabled) persisted.warmupEnabled else current.warmupEnabled,
                        prePrompt = if (current.prePrompt == defaults.prePrompt) persisted.prePrompt else current.prePrompt,
                        thinkingEnabled = if (current.thinkingEnabled == defaults.thinkingEnabled) persisted.thinkingEnabled else current.thinkingEnabled,
                        temperature = if (current.temperature == defaults.temperature) persisted.temperature else current.temperature,
                        topK = if (current.topK == defaults.topK) persisted.topK else current.topK,
                        maxOutputTokens = if (current.maxOutputTokens == defaults.maxOutputTokens) persisted.maxOutputTokens else current.maxOutputTokens,
                    )
                    persistedStateLoaded = true
                    // A persisted "off" wins over any warmup already kicked
                    // off before the stored state was readable.
                    if (!uiState.warmupEnabled) cancelModelWarmup()
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            photoIndexManager.loadIndex()
        }
    }

    // ---------------------------------------------------------------- model

    fun probeModel() {
        probeJob?.cancel()
        uiState = uiState.copy(
            model = uiState.model.copy(phase = ModelPhase.CHECKING, error = null),
        )
        probeJob = viewModelScope.launch {
            val probe = engine.probe()
            val phase = when (probe.status) {
                FeatureStatus.AVAILABLE -> ModelPhase.READY
                FeatureStatus.DOWNLOADABLE, FeatureStatus.DOWNLOADING -> ModelPhase.DOWNLOADABLE
                else -> if (probe.error != null) ModelPhase.ERROR else ModelPhase.UNAVAILABLE
            }
            uiState = uiState.copy(
                model = ModelUi(
                    phase = phase,
                    label = probe.label,
                    baseModel = probe.baseModel,
                    status = probe.status,
                    detail = probe.detail ?: probe.error,
                    fallback = probe.fallback,
                    error = if (phase == ModelPhase.ERROR) probe.error else null,
                    options = probe.options,
                    supportsTools = probe.supportsTools,
                ),
            )
            if (phase == ModelPhase.READY) warmupModel()
        }
    }

    /**
     * Quiet background warmup: raises the accelerator so the first user turn
     * avoids the 1.5–3s cold start. Guarded strategies per the warm-up plan:
     * not while generating (real turns own AICore), canceled before send.
     */
    fun warmupModel() {
        if (!uiState.warmupEnabled) return
        if (uiState.model.phase != ModelPhase.READY) return
        if (uiState.isGenerating) return
        warmupJob?.cancel()
        warmupJob = viewModelScope.launch(Dispatchers.IO) {
            engine.warmup()
        }
    }

    private fun cancelModelWarmup() {
        warmupJob?.cancel()
        warmupJob = null
    }

    fun downloadModel() {
        if (uiState.model.phase == ModelPhase.DOWNLOADING) return
        downloadJob?.cancel()
        uiState = uiState.copy(
            model = uiState.model.copy(
                phase = ModelPhase.DOWNLOADING,
                downloadedBytes = 0,
                totalBytes = 0,
                error = null,
            ),
        )
        downloadJob = viewModelScope.launch {
            engine.download().collect { event ->
                when (event) {
                    is DownloadEvent.Started -> uiState = uiState.copy(
                        model = uiState.model.copy(
                            phase = ModelPhase.DOWNLOADING,
                            totalBytes = event.totalBytes,
                        ),
                    )

                    is DownloadEvent.Progress -> uiState = uiState.copy(
                        model = uiState.model.copy(
                            phase = ModelPhase.DOWNLOADING,
                            downloadedBytes = event.downloadedBytes,
                            totalBytes = event.totalBytes,
                        ),
                    )

                    is DownloadEvent.Completed -> {
                        uiState = uiState.copy(
                            model = uiState.model.copy(
                                phase = ModelPhase.READY,
                                baseModel = event.baseModel ?: uiState.model.baseModel,
                                error = null,
                            ),
                        )
                        notice(
                            tr(
                                "${uiState.model.label} 준비가 끝났어요.",
                                "${uiState.model.label} is ready.",
                            ),
                        )
                        warmupModel()
                    }

                    is DownloadEvent.Failed -> uiState = uiState.copy(
                        model = uiState.model.copy(
                            phase = ModelPhase.ERROR,
                            error = event.message,
                        ),
                    )
                }
            }
        }
    }

    fun selectModel(option: ModelOption) {
        titleJob?.cancel()
        titleJob = null
        if (uiState.isGenerating) {
            // Switching the engine closes the active client, which would abort
            // the response that is still streaming. Keep the choice visible now
            // and swap the engine once the current turn ends.
            pendingModel = option
        } else {
            pendingModel = null
            cancelModelWarmup()
            engine.selectModel(option)
        }
        val phase = when (option.status) {
            FeatureStatus.AVAILABLE -> ModelPhase.READY
            FeatureStatus.DOWNLOADABLE -> ModelPhase.DOWNLOADABLE
            FeatureStatus.DOWNLOADING -> ModelPhase.DOWNLOADING
            else -> if (option.error != null) ModelPhase.ERROR else ModelPhase.UNAVAILABLE
        }
        uiState = uiState.copy(
            model = uiState.model.copy(
                phase = phase,
                label = option.label,
                baseModel = option.baseModel,
                status = option.status,
                fallback = option.preference == com.google.mlkit.genai.prompt.ModelPreference.FAST,
                detail = option.error,
                error = if (phase == ModelPhase.ERROR) option.error else null,
                supportsTools = option.supportsTools,
                pendingSelection = uiState.isGenerating,
            ),
        )
    }

    fun setThinkingEnabled(enabled: Boolean) {
        // The active GenerateContentRequest captured its own setting at start.
        // Keep this preference mutable during streaming for the next request.
        uiState = uiState.copy(thinkingEnabled = enabled)
        persist()
    }

    fun toggleThinking() = setThinkingEnabled(!uiState.thinkingEnabled)

    fun updatePrePrompt(value: String) {
        uiState = uiState.copy(prePrompt = value.filter { it == '\n' || it == '\t' || !it.isISOControl() }.take(8_000))
    }

    fun resetGenerationSettings() {
        uiState = uiState.copy(
            temperature = 0.7f,
            topK = 40,
            maxOutputTokens = 900,
            prePrompt = DEFAULT_SYSTEM_INSTRUCTIONS,
            thinkingEnabled = false,
            hapticsEnabled = true,
            soundsEnabled = true,
            warmupEnabled = true,
        )
        persist()
    }

    fun updateGenerationSettings(temperature: Float, topK: Int, maxOutputTokens: Int) {
        uiState = uiState.copy(
            temperature = temperature.coerceIn(0f, 1f),
            topK = topK.coerceIn(1, 128),
            maxOutputTokens = maxOutputTokens.coerceIn(128, 4096),
        )
    }

    fun saveGenerationSettings() = persist()

    fun updateFeedbackSettings(hapticsEnabled: Boolean, soundsEnabled: Boolean) {
        uiState = uiState.copy(
            hapticsEnabled = hapticsEnabled,
            soundsEnabled = soundsEnabled,
        )
    }

    fun setWarmupEnabled(enabled: Boolean) {
        uiState = uiState.copy(warmupEnabled = enabled)
        if (enabled && !uiState.isGenerating) {
            warmupModel()
        } else if (!enabled) {
            cancelModelWarmup()
        }
        persist()
    }

    // ------------------------------------------------------------- sessions

    fun openDrawer() {
        uiState = uiState.copy(drawerOpen = true)
    }

    fun closeDrawer() {
        uiState = uiState.copy(drawerOpen = false)
    }

    fun newSession() {
        stopVoice(closeOverlay = true)
        stopGeneration()
        val kept = uiState.sessions.filter { it.messages.isNotEmpty() }
        uiState = uiState.copy(
            sessions = kept,
            activeId = "",
            messages = emptyList(),
            draft = "",
            attachmentUris = emptyList(),
            editingMessageId = null,
            isGenerating = false,
            streamingMessageId = null,
            drawerOpen = false,
        )
        persist()
    }

    fun selectSession(id: String) {
        if (id == uiState.activeId) {
            closeDrawer()
            return
        }
        if (uiState.sessions.none { it.id == id }) return
        stopVoice(closeOverlay = true)
        stopGeneration()
        uiState = uiState.copy(
            activeId = id,
            messages = uiState.sessions.firstOrNull { it.id == id }?.messages.orEmpty(),
            draft = "",
            attachmentUris = emptyList(),
            editingMessageId = null,
            drawerOpen = false,
            isGenerating = false,
            streamingMessageId = null,
        )
        persist()
    }

    fun renameSession(id: String, title: String) {
        val normalizedTitle = title.trim()
        if (normalizedTitle.isEmpty() || uiState.sessions.none { it.id == id }) return
        if (titleJobSessionId == id) {
            titleJob?.cancel()
            titleJob = null
            titleJobSessionId = null
        }
        applySessions(
            uiState.sessions.map { session ->
                if (session.id == id) {
                    session.copy(title = normalizedTitle, updatedAt = System.currentTimeMillis())
                } else session
            },
        )
        persist()
    }

    fun deleteSession(id: String) {
        val deletingActive = id == uiState.activeId
        if (deletingActive) {
            stopVoice(closeOverlay = true)
            stopGeneration()
        }
        val remaining = uiState.sessions.filterNot { it.id == id }
        val nextActiveId = if (deletingActive) {
            remaining.maxByOrNull { it.updatedAt }?.id.orEmpty()
        } else {
            uiState.activeId
        }
        val nextActiveSession = remaining.firstOrNull { it.id == nextActiveId }
        uiState = uiState.copy(
            sessions = remaining.sortedByDescending { it.updatedAt },
            activeId = nextActiveSession?.id.orEmpty(),
            messages = nextActiveSession?.messages.orEmpty(),
            editingMessageId = if (deletingActive) null else uiState.editingMessageId,
        )
        persist()
    }

    // -------------------------------------------------------------- sending

    fun updateDraft(text: String) {
        if (uiState.draft.isEmpty() && text.isNotBlank()) {
            // First composer interaction: a natural moment to pre-load the
            // accelerator when nothing else has done it yet.
            warmupModel()
        }
        uiState = uiState.copy(draft = text)
    }

    fun addAttachments(uris: List<String>) {
        val all = (uiState.attachmentUris + uris).distinct()
        uiState = uiState.copy(attachmentUris = all)
    }

    fun removeAttachment(uri: String) {
        uiState = uiState.copy(attachmentUris = uiState.attachmentUris.filterNot { it == uri })
    }

    fun startEditingMessage(message: ChatMessage) {
        if (uiState.isGenerating) {
            stopGeneration()
        }
        val currentMessages = uiState.messages
        val targetIndex = currentMessages.indexOfFirst { it.id == message.id }
        if (targetIndex != -1) {
            uiState = uiState.copy(
                draft = message.text,
                attachmentUris = message.attachmentUris,
                editingMessageId = message.id,
            )
        } else {
            uiState = uiState.copy(
                draft = message.text,
                attachmentUris = message.attachmentUris,
                editingMessageId = message.id,
            )
        }
    }

    fun cancelEditingMessage() {
        uiState = uiState.copy(
            draft = "",
            attachmentUris = emptyList(),
            editingMessageId = null,
        )
    }

    fun send(text: String = uiState.draft) {
        val content = text.trim()
        if (content.isEmpty() && uiState.attachmentUris.isEmpty()) return
        if (uiState.isGenerating) {
            notice(tr("응답을 기다리는 중이에요.", "A response is still in progress."))
            return
        }
        when (uiState.model.phase) {
            ModelPhase.READY -> Unit
            ModelPhase.DOWNLOADABLE -> {
                notice(tr("먼저 Gemma 4 모델을 준비해 주세요.", "Set up the Gemma 4 model first."))
                return
            }

            ModelPhase.DOWNLOADING -> {
                notice(tr("모델을 내려받는 중이에요.", "The model is downloading."))
                return
            }

            ModelPhase.CHECKING -> {
                notice(tr("모델 상태를 확인하는 중이에요.", "Checking the model status."))
                return
            }

            ModelPhase.UNAVAILABLE, ModelPhase.ERROR -> {
                notice(tr("이 기기에서는 온디바이스 모델을 사용할 수 없어요.", "The on-device model is unavailable on this device."))
                return
            }
        }
        val editingId = uiState.editingMessageId
        // The real turn owns AICore from here on — drop any pending warmup so
        // it cannot compete for quota or leave a BUSY window behind.
        cancelModelWarmup()
        if (editingId != null) {
            editAndSend(editingId, content)
            return
        }
        appendUser(content, uiState.attachmentUris)
        titleJob?.cancel()
        titleJob = null
        emitFeedback(AppFeedbackCue.MESSAGE_SENT)
        uiState = uiState.copy(
            draft = "",
            attachmentUris = emptyList(),
            isGenerating = true,
            streamingMessageId = null,
            toolCallPreparing = false,
            toolCallName = null,
        )
        generate(uiState.messages, fromVoice = false)
    }

    private fun editAndSend(targetMessageId: String, newContent: String) {
        val currentMessages = uiState.messages
        val targetIndex = currentMessages.indexOfFirst { it.id == targetMessageId }
        if (targetIndex == -1) {
            uiState = uiState.copy(editingMessageId = null)
            appendUser(newContent, uiState.attachmentUris)
            titleJob?.cancel()
            titleJob = null
            emitFeedback(AppFeedbackCue.MESSAGE_SENT)
            uiState = uiState.copy(
                draft = "",
                attachmentUris = emptyList(),
                isGenerating = true,
                streamingMessageId = null,
                toolCallPreparing = false,
                toolCallName = null,
            )
            generate(uiState.messages, fromVoice = false)
            return
        }

        val now = System.currentTimeMillis()
        val targetMessage = currentMessages[targetIndex]
        val updatedUserMessage = targetMessage.copy(
            text = newContent,
            createdAt = now,
            attachmentUris = uiState.attachmentUris,
        )
        // targetIndex 이후의 대화(기존 어시스턴트 답변 및 후속 질의응답)를 잘라내어 새로운 브랜치로 대체
        val truncatedMessages = currentMessages.take(targetIndex) + updatedUserMessage

        val currentSessionId = uiState.activeId
        val sessions = uiState.sessions.map { session ->
            if (session.id == currentSessionId) {
                session.copy(
                    title = if (targetIndex == 0) titleFrom(newContent) else session.title,
                    messages = truncatedMessages,
                    updatedAt = now,
                )
            } else {
                session
            }
        }
        applySessions(sessions)
        persist()

        titleJob?.cancel()
        titleJob = null
        emitFeedback(AppFeedbackCue.MESSAGE_SENT)
        uiState = uiState.copy(
            draft = "",
            attachmentUris = emptyList(),
            editingMessageId = null,
            isGenerating = true,
            streamingMessageId = null,
            toolCallPreparing = false,
            toolCallName = null,
        )
        generate(truncatedMessages, fromVoice = false)
    }

    fun sendSuggestion(text: String) {
        updateDraft(text)
        send(text)
    }

    fun stopGeneration() {
        if (!uiState.isGenerating) return
        generationJob?.cancel()
        generationJob = null
        engine.abort()
        uiState.streamingMessageId?.let { id ->
            val current = uiState.messages.lastOrNull { it.id == id }
            if (current != null && current.text.isBlank()) {
                updateAssistant(id, tr("생성을 중지했어요.", "Generation stopped."))
            }
        }
        uiState = uiState.copy(
            isGenerating = false,
            streamingMessageId = null,
            toolCallPreparing = false,
            toolCallName = null,
        )
        pendingModel?.let {
            pendingModel = null
            engine.selectModel(it)
        }
        persist()
    }

    private fun appendUser(text: String, attachmentUris: List<String> = emptyList()) {
        val now = System.currentTimeMillis()
        val message = ChatMessage(
            role = ChatRole.USER,
            text = text,
            createdAt = now,
            attachmentUris = attachmentUris,
        )

        // 세션이 아직 없는 대기 상태(activeId.isEmpty())인 경우 첫 메시지 전송 시 실제 세션을 생성한다.
        val currentSessionId = uiState.activeId.ifBlank {
            val newSession = ChatSession(
                title = titleFrom(text),
                createdAt = now,
                updatedAt = now,
            )
            uiState = uiState.copy(
                sessions = listOf(newSession) + uiState.sessions,
                activeId = newSession.id,
            )
            newSession.id
        }

        val sessions = uiState.sessions.map { session ->
            if (session.id == currentSessionId) {
                session.copy(
                    title = if (session.title.isBlank()) titleFrom(text) else session.title,
                    messages = session.messages + message,
                    updatedAt = now,
                )
            } else {
                session
            }
        }
        applySessions(sessions)
        persist()
    }

    private fun generate(history: List<ChatMessage>, fromVoice: Boolean) {
        generationJob?.cancel()
        val targetSessionId = uiState.activeId
        val firstUserMessage = history.singleOrNull { it.role == ChatRole.USER }?.text
        val sessionSnapshot = uiState.sessions
        val currentIds = history.map { it.id }.toSet()
        val previousPhotoUris = sessionSnapshot
            .firstOrNull { it.id == targetSessionId }
            ?.messages
            ?.lastOrNull { it.role == ChatRole.ASSISTANT && it.photoSearchContext.isNotBlank() }
            ?.photoUris
            .orEmpty()
        val tools = buildLocalTools(
            sessions = sessionSnapshot,
            currentMessageIds = currentIds,
            photoIndexManager = photoIndexManager,
            previousPhotoUris = previousPhotoUris,
        )
        generationJob = viewModelScope.launch {
            var assistantId = UUID.randomUUID().toString()
            var builder = StringBuilder()
            val turnSpeechChannel = if (fromVoice) Channel<String>(Channel.UNLIMITED) else null
            speechChannel = turnSpeechChannel
            speechQueueJob = turnSpeechChannel?.let { channel ->
                launch { speakReplyChunks(channel) }
            }
            var speechBuffer = ""
            // Voice turns take the lower-latency path without changing the
            // user's saved preference for text conversations.
            val thinkingEnabled = uiState.thinkingEnabled && !fromVoice
            var turnThinkingAvailable = thinkingEnabled
            var thinkingStartedCuePlayed = false
            var thinkingFinishedCuePlayed = false
            val prePrompt = uiState.prePrompt
            val currentTurnPhotoUris = mutableListOf<String>()
            val currentTurnPhotoSearchContext = mutableListOf<String>()
            appendAssistantShell(assistantId, thinkingEnabled)
            uiState = uiState.copy(
                streamingMessageId = assistantId,
            )
            try {
                engine.streamReply(
                    history = history,
                    conversationId = uiState.activeId,
                    enableThinking = thinkingEnabled,
                    temperature = uiState.temperature,
                    topK = uiState.topK,
                    maxOutputTokens = uiState.maxOutputTokens,
                    prePrompt = prePrompt,
                    tools = tools,
                    confirmTool = ::awaitToolConfirmation,
                ).collect { update ->
                    when (update) {
                        is GenerationUpdate.Thought -> {
                            updateAssistantThinking(assistantId, observed = true)
                            appendAssistantThought(assistantId, update.value)
                        }
                        GenerationUpdate.ThinkingUnavailable -> {
                            turnThinkingAvailable = false
                            updateAssistantThinking(
                                assistantId,
                                observed = false,
                                unavailable = true,
                            )
                        }
                        GenerationUpdate.ThinkingStarted -> {
                            thinkingStartedCuePlayed = true
                            emitFeedback(
                                AppFeedbackCue.THINKING_STARTED,
                                allowSound = !fromVoice,
                            )
                        }
                        GenerationUpdate.AnswerStarted -> {
                            updateAssistantThinking(assistantId, finished = true)
                            if (thinkingStartedCuePlayed && !thinkingFinishedCuePlayed) {
                                thinkingFinishedCuePlayed = true
                                emitFeedback(AppFeedbackCue.THINKING_FINISHED, allowSound = !fromVoice)
                            }
                        }
                        GenerationUpdate.ToolBatchStarted -> {
                            val currentMessage = uiState.messages.firstOrNull { it.id == assistantId }
                            val hasVisibleSegment = builder.isNotBlank() ||
                                currentMessage?.thoughtText?.isNotBlank() == true
                            if (hasVisibleSegment) {
                                if (turnThinkingAvailable && currentMessage?.thinkingFinished != true) {
                                    updateAssistantThinking(assistantId, finished = true)
                                }
                                assistantId = UUID.randomUUID().toString()
                                builder = StringBuilder()
                                appendAssistantShell(assistantId, turnThinkingAvailable)
                                uiState = uiState.copy(streamingMessageId = assistantId)
                            }
                        }
                        is GenerationUpdate.ToolCallPreparing -> {
                            uiState = uiState.copy(
                                toolCallPreparing = true,
                                toolCallName = update.name ?: uiState.toolCallName,
                            )
                        }
                        GenerationUpdate.ToolCallPreparationEnded -> {
                            uiState = uiState.copy(toolCallPreparing = false, toolCallName = null)
                        }
                        is GenerationUpdate.ToolStarted -> {
                            uiState = uiState.copy(toolCallPreparing = false, toolCallName = null)
                            appendToolCall(assistantId, update.id, update.name, update.arguments)
                        }
                        is GenerationUpdate.ToolFinished -> {
                            updateToolCall(update.id, update.summary)
                            if (update.photoUris.isNotEmpty()) {
                                currentTurnPhotoUris.addAll(update.photoUris)
                            }
                            if (update.photoSearchContext.isNotBlank()) {
                                currentTurnPhotoSearchContext += update.photoSearchContext
                            }
                        }
                        is GenerationUpdate.OutputLimitAdjusted -> notice(
                            tr(
                                "대화 길이에 맞춰 최대 출력을 ${update.effective} 토큰으로 낮췄어요. (설정 ${update.requested})",
                                "Reduced the max output to ${update.effective} tokens to fit this conversation. (setting ${update.requested})",
                            ),
                        )
                        is GenerationUpdate.Text -> {
                            builder.append(update.value)
                            updateAssistant(assistantId, builder.toString())
                            if (fromVoice && turnSpeechChannel != null) {
                                uiState = uiState.copy(
                                    voice = uiState.voice.copy(caption = builder.toString()),
                                )
                                speechBuffer += update.value
                                var boundary = findSpeechBoundary(speechBuffer)
                                while (boundary != -1) {
                                    turnSpeechChannel.send(speechBuffer.take(boundary))
                                    speechBuffer = speechBuffer.drop(boundary).trimStart()
                                    boundary = findSpeechBoundary(speechBuffer)
                                }
                            }
                        }
                    }
                }

                if (turnSpeechChannel != null) {
                    if (speechBuffer.isNotBlank()) turnSpeechChannel.send(speechBuffer)
                    turnSpeechChannel.close()
                }
                if (builder.isBlank()) {
                    removeAssistant(assistantId)
                    if (!fromVoice) {
                        appendAssistantError(tr("응답이 비어 있어요. 다시 한 번 말씀해 주세요.", "The response was empty. Please try again."))
                    }
                    finishGeneration()
                    persist()
                    if (fromVoice && uiState.voice.active) {
                        uiState = uiState.copy(
                            voice = uiState.voice.copy(
                                phase = VoicePhase.IDLE,
                                hint = tr("응답이 비어 있어요. 다시 말씀해 주세요.", "The response was empty. Please try again."),
                            ),
                        )
                    }
                    return@launch
                }

                updateAssistant(
                    assistantId,
                    builder.toString(),
                    (currentTurnPhotoUris + extractContentUris(builder.toString())).distinct(),
                    currentTurnPhotoSearchContext.distinct().joinToString("\n"),
                )

                uiState = uiState.copy(streamingMessageId = null)
                emitFeedback(AppFeedbackCue.ANSWER_COMPLETED, allowSound = !fromVoice)
                finishGeneration()
                persist()
                if (firstUserMessage != null) {
                    requestSessionTitle(targetSessionId, firstUserMessage, builder.toString())
                }
                if (fromVoice && uiState.voice.active) {
                    speechQueueJob?.join()
                }
            } catch (c: CancellationException) {
                turnSpeechChannel?.close()
                finishGeneration()
                persist()
                if (fromVoice && uiState.voice.active) {
                    uiState = uiState.copy(voice = uiState.voice.copy(phase = VoicePhase.IDLE))
                }
                throw c
            } catch (t: Throwable) {
                turnSpeechChannel?.close()
                uiState = uiState.copy(streamingMessageId = null)
                removeAssistant(assistantId)
                if (!fromVoice) {
                    appendAssistantError(
                        tr(
                            "응답을 만들지 못했어요.\n${GemmaEngine.describeError(t)}",
                            "Could not build a response.\n${GemmaEngine.describeError(t)}",
                        ),
                    )
                }
                finishGeneration()
                persist()
                if (fromVoice && uiState.voice.active) {
                    uiState = uiState.copy(
                        voice = uiState.voice.copy(
                            phase = VoicePhase.IDLE,
                            hint = tr("응답을 만들지 못했어요. 다시 시도해 주세요.", "Could not build a response. Please try again."),
                        ),
                    )
                }
            }
            if (speechChannel === turnSpeechChannel) speechChannel = null
            if (speechQueueJob?.isCompleted == true) speechQueueJob = null
        }
    }

    private fun requestSessionTitle(sessionId: String, userMessage: String, assistantMessage: String) {
        titleJob?.cancel()
        titleJobSessionId = sessionId
        titleJob = viewModelScope.launch {
            val generatedTitle = runCatching {
                engine.generateSessionTitle(userMessage, assistantMessage)
            }.getOrNull()?.takeIf(String::isNotBlank) ?: return@launch
            val target = uiState.sessions.firstOrNull { it.id == sessionId } ?: return@launch
            if (target.userMessageCount != 1) return@launch
            applySessions(uiState.sessions.map { session ->
                if (session.id == sessionId) {
                    session.copy(title = generatedTitle, updatedAt = System.currentTimeMillis())
                } else session
            })
            persist()
            titleJob = null
            titleJobSessionId = null
        }
    }

    private fun appendAssistantShell(id: String, thinkingEnabled: Boolean) {
        val message = ChatMessage(
            id = id,
            role = ChatRole.ASSISTANT,
            text = "",
            thinkingEnabled = thinkingEnabled,
        )
        val now = System.currentTimeMillis()
        val sessions = uiState.sessions.map { session ->
            if (session.id == uiState.activeId) {
                session.copy(messages = session.messages + message, updatedAt = now)
            } else {
                session
            }
        }
        applySessions(sessions)
    }

    private fun appendToolCall(assistantId: String, id: String, name: String, arguments: String) {
        val message = ChatMessage(
            id = id,
            role = ChatRole.TOOL,
            text = tr(
                "도구 호출 · $name\n$arguments\n실행 중",
                "Tool call · $name\n$arguments\nRunning",
            ),
        )
        val now = System.currentTimeMillis()
        applySessions(uiState.sessions.map { session ->
            if (session.id == uiState.activeId) {
                val answerIndex = session.messages.indexOfFirst { it.id == assistantId }
                    .takeIf { it >= 0 } ?: session.messages.size
                session.copy(
                    messages = session.messages.toMutableList().apply { add(answerIndex, message) },
                    updatedAt = now,
                )
            } else session
        })
    }

    private fun updateToolCall(id: String, summary: String) {
        applySessions(uiState.sessions.map { session ->
            if (session.id == uiState.activeId) {
                session.copy(messages = session.messages.map { message ->
                    if (message.id == id && message.role == ChatRole.TOOL) {
                        val headerAndArguments = message.text.substringBeforeLast("\n")
                        message.copy(
                            text = "$headerAndArguments\n" + tr(
                                "완료 · ${summary.ifBlank { "결과 없음" }}",
                                "Done · ${summary.ifBlank { "No result" }}",
                            ),
                        )
                    } else message
                }, updatedAt = System.currentTimeMillis())
            } else session
        })
        persist()
    }

    private fun updateAssistantThinking(
        id: String,
        observed: Boolean = false,
        unavailable: Boolean = false,
        finished: Boolean = false,
    ) {
        val sessions = uiState.sessions.map { session ->
            if (session.id == uiState.activeId) {
                session.copy(messages = session.messages.map { message ->
                    if (message.id == id) message.copy(
                        thinkingObserved = observed,
                        thinkingUnavailable = message.thinkingUnavailable || unavailable,
                        thinkingFinished = message.thinkingFinished || finished,
                    ) else message
                })
            } else session
        }
        applySessions(sessions)
    }

    private fun appendAssistantThought(id: String, value: String) {
        val sessions = uiState.sessions.map { session ->
            if (session.id == uiState.activeId) {
                session.copy(messages = session.messages.map { message ->
                    if (message.id == id) message.copy(thoughtText = message.thoughtText + value) else message
                })
            } else session
        }
        applySessions(sessions)
    }

    private fun removeAssistant(id: String) {
        val sessions = uiState.sessions.map { session ->
            if (session.id == uiState.activeId) {
                session.copy(messages = session.messages.filterNot { it.id == id })
            } else session
        }
        applySessions(sessions)
    }

    private fun updateAssistant(
        id: String,
        text: String,
        photoUris: List<String> = emptyList(),
        photoSearchContext: String = "",
    ) {
        val sessions = uiState.sessions.map { session ->
            if (session.id == uiState.activeId) {
                session.copy(
                    messages = session.messages.map { message ->
                        if (message.id == id) {
                            val combinedUris = if (photoUris.isNotEmpty()) photoUris else message.photoUris
                            message.copy(
                                text = text,
                                photoUris = combinedUris,
                                photoSearchContext = photoSearchContext.ifBlank { message.photoSearchContext },
                            )
                        } else {
                            message
                        }
                    },
                )
            } else {
                session
            }
        }
        applySessions(sessions)
    }

    private fun appendAssistantError(text: String) {
        val message = ChatMessage(role = ChatRole.ASSISTANT, text = text, isError = true)
        val sessions = uiState.sessions.map { session ->
            if (session.id == uiState.activeId) {
                session.copy(messages = session.messages + message, updatedAt = System.currentTimeMillis())
            } else {
                session
            }
        }
        applySessions(sessions)
    }

    private fun finishGeneration() {
        // Apply a model the user picked while this turn was streaming. The
        // engine switch closes the old client, so it must wait until the turn
        // has really ended.
        pendingModel?.let {
            pendingModel = null
            engine.selectModel(it)
        }
        uiState = uiState.copy(
            isGenerating = false,
            streamingMessageId = null,
            toolCallPreparing = false,
            toolCallName = null,
            model = uiState.model.copy(pendingSelection = false),
        )
    }

    private fun applySessions(sessions: List<ChatSession>) {
        val sorted = sessions.sortedByDescending { it.updatedAt }
        val active = if (uiState.activeId.isBlank()) {
            null
        } else {
            sorted.firstOrNull { it.id == uiState.activeId }
        }
        uiState = uiState.copy(
            sessions = sorted,
            activeId = active?.id.orEmpty(),
            messages = active?.messages.orEmpty(),
        )
    }

    private suspend fun awaitToolConfirmation(request: ToolConfirmation): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        withContext(Dispatchers.Main.immediate) {
            toolConfirmation = deferred
            uiState = uiState.copy(
                toolConfirmation = PendingToolConfirmation(request.title, request.detail),
            )
        }
        return try {
            deferred.await()
        } finally {
            withContext(Dispatchers.Main.immediate) {
                if (toolConfirmation === deferred) toolConfirmation = null
                uiState = uiState.copy(toolConfirmation = null)
            }
        }
    }

    fun resolveToolConfirmation(approved: Boolean) {
        toolConfirmation?.complete(approved)
        toolConfirmation = null
        uiState = uiState.copy(toolConfirmation = null)
    }

    // ---------------------------------------------------------------- voice

    fun enterVoiceMode() {
        when (uiState.model.phase) {
            ModelPhase.READY -> Unit
            ModelPhase.DOWNLOADABLE, ModelPhase.DOWNLOADING -> {
                notice(tr("먼저 Gemma 4 모델을 준비해 주세요.", "Set up the Gemma 4 model first."))
                return
            }

            ModelPhase.CHECKING -> {
                notice(tr("모델 상태를 확인하는 중이에요.", "Checking the model status."))
                return
            }

            ModelPhase.UNAVAILABLE, ModelPhase.ERROR -> {
                notice(tr("이 기기에서는 온디바이스 모델을 사용할 수 없어요.", "The on-device model is unavailable on this device."))
                return
            }
        }
        if (uiState.isGenerating) stopGeneration()
        // Do not grab the microphone before the user asks: the overlay opens
        // in a clear "tap to talk" state, so a quick tap can never cut a
        // listening session short by accident.
        uiState = uiState.copy(
            voice = VoiceUi(active = true, phase = VoicePhase.IDLE),
        )
    }

    fun exitVoiceMode() {
        stopVoice(closeOverlay = true)
    }

    fun onVoicePrimaryAction() {
        when (uiState.voice.phase) {
            VoicePhase.IDLE -> {
                startListening()
            }
            VoicePhase.LISTENING -> {
                speechIn.stop()
                // Some recognizers never answer after stopListening(); don't
                // leave the orb stuck in LISTENING.
                autoListenJob?.cancel()
                autoListenJob = viewModelScope.launch {
                    delay(2_000)
                    if (uiState.voice.active && uiState.voice.phase == VoicePhase.LISTENING) {
                        listenJob?.cancel()
                        listenJob = null
                        speechIn.stop()
                        uiState = uiState.copy(
                            voice = uiState.voice.copy(
                                phase = VoicePhase.IDLE,
                                hint = tr("듣지 못했어요. 다시 말씀해 주세요.", "I didn't catch that. Please say it again."),
                            ),
                        )
                    }
                }
            }

            VoicePhase.THINKING -> {
                stopGeneration()
                startListening()
            }

            VoicePhase.SPEAKING -> {
                speechQueueJob?.cancel()
                speechQueueJob = null
                speechChannel?.cancel()
                speechChannel = null
                speechOut.stop()
                scheduleListening(200)
            }
        }
    }

    private fun startListening() {
        if (!uiState.voice.active) return
        if (uiState.isGenerating) return
        autoListenJob?.cancel()
        listenJob?.cancel()
        clearVoiceInputLevel()
        speechOut.stop()
        uiState = uiState.copy(
            voice = uiState.voice.copy(
                phase = VoicePhase.LISTENING,
                transcript = "",
                caption = "",
                hint = "",
            ),
        )
        listenJob = viewModelScope.launch {
            var terminal = false
            var hasInputLevel = false
            try {
                speechIn.listen(Locale.KOREAN).collect { event ->
                    if (!uiState.voice.active || uiState.voice.phase != VoicePhase.LISTENING) {
                        return@collect
                    }
                    when (event) {
                        is SpeechEvent.InputStarted -> {
                            if (!hasInputLevel) pulseVoiceInput(0.48f)
                        }

                        is SpeechEvent.InputLevel -> {
                            hasInputLevel = true
                            setVoiceInputLevel(event.level)
                        }

                        is SpeechEvent.Partial -> {
                            if (event.text != uiState.voice.transcript) {
                                uiState = uiState.copy(
                                    voice = uiState.voice.copy(transcript = event.text),
                                )
                                if (!hasInputLevel) pulseVoiceInput(0.55f)
                            }
                        }

                        is SpeechEvent.Final -> {
                            terminal = true
                            val text = event.text.trim()
                            if (text.isBlank()) {
                                uiState = uiState.copy(
                                    voice = uiState.voice.copy(
                                        phase = VoicePhase.IDLE,
                                        transcript = "",
                                        caption = "",
                                        inputLevel = 0f,
                                        hint = tr("듣지 못했어요. 다시 말씀해 주세요.", "I didn't catch that. Please say it again."),
                                    ),
                                )
                            } else {
                                uiState = uiState.copy(
                                    voice = uiState.voice.copy(
                                        phase = VoicePhase.THINKING,
                                        transcript = text,
                                        caption = text,
                                        inputLevel = 0f,
                                        hint = "",
                                    ),
                                )
                                appendUser(text)
                                emitFeedback(AppFeedbackCue.MESSAGE_SENT, allowSound = false)
                                uiState = uiState.copy(
                                    isGenerating = true,
                                    streamingMessageId = null,
                                )
                                generate(uiState.messages, fromVoice = true)
                            }
                        }

                        is SpeechEvent.Failed -> {
                            terminal = true
                            uiState = uiState.copy(
                                voice = uiState.voice.copy(
                                    phase = VoicePhase.IDLE,
                                    inputLevel = 0f,
                                    hint = event.message,
                                ),
                            )
                        }
                    }
                }
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                terminal = true
                if (uiState.voice.active) {
                    uiState = uiState.copy(
                        voice = uiState.voice.copy(
                            phase = VoicePhase.IDLE,
                            inputLevel = 0f,
                            hint = tr("음성 인식을 사용할 수 없어요.", "Speech recognition is unavailable."),
                        ),
                    )
                }
            } finally {
                // The flow ended without a final/error event (for example the
                // engine was torn down). Never leave the UI claiming to listen.
                if (!terminal && uiState.voice.active &&
                    uiState.voice.phase == VoicePhase.LISTENING
                ) {
                    uiState = uiState.copy(
                        voice = uiState.voice.copy(
                            phase = VoicePhase.IDLE,
                            inputLevel = 0f,
                            hint = tr("다시 탭하여 말씀해 주세요.", "Tap again to speak."),
                        ),
                    )
                }
            }
        }
    }

    private fun pulseVoiceInput(level: Float) {
        val version = ++inputPulseVersion
        inputPulseJob?.cancel()
        uiState = uiState.copy(voice = uiState.voice.copy(inputLevel = level))
        inputPulseJob = viewModelScope.launch {
            delay(180)
            if (version == inputPulseVersion && uiState.voice.phase == VoicePhase.LISTENING) {
                uiState = uiState.copy(voice = uiState.voice.copy(inputLevel = 0f))
            }
        }
    }

    private fun setVoiceInputLevel(level: Float) {
        inputPulseVersion++
        inputPulseJob?.cancel()
        inputPulseJob = null
        uiState = uiState.copy(voice = uiState.voice.copy(inputLevel = level.coerceIn(0f, 1f)))
    }

    private fun clearVoiceInputLevel() {
        inputPulseVersion++
        inputPulseJob?.cancel()
        inputPulseJob = null
        if (uiState.voice.inputLevel != 0f) {
            uiState = uiState.copy(voice = uiState.voice.copy(inputLevel = 0f))
        }
    }

    private suspend fun speakReplyChunks(chunks: ReceiveChannel<String>) {
        var spokenCharacters = 0
        for (chunk in chunks) {
            if (chunk.isBlank() || !uiState.voice.active) continue
            if (uiState.voice.phase != VoicePhase.SPEAKING) {
                uiState = uiState.copy(
                    voice = uiState.voice.copy(
                        phase = VoicePhase.SPEAKING,
                        transcript = "",
                        caption = uiState.voice.caption.ifBlank { chunk },
                        captionProgress = 0f,
                        speechCharacterIndex = 0,
                        speechLevel = 0f,
                        hint = "",
                    ),
                )
            }
            val chunkStart = spokenCharacters
            speechOut.speak(
                text = chunk,
                onProgress = { progress ->
                    if (uiState.voice.active && uiState.voice.phase == VoicePhase.SPEAKING) {
                        val total = uiState.voice.caption.length.coerceAtLeast(1)
                        val absolute = (chunkStart + progress * chunk.length) / total
                        uiState = uiState.copy(
                            voice = uiState.voice.copy(
                                captionProgress = absolute.coerceIn(0f, 1f),
                                speechCharacterIndex = (chunkStart + progress * chunk.length).toInt(),
                            ),
                        )
                    }
                },
                onAmplitude = { level ->
                    if (uiState.voice.active && uiState.voice.phase == VoicePhase.SPEAKING) {
                        uiState = uiState.copy(
                            voice = uiState.voice.copy(speechLevel = level.coerceIn(0f, 1f)),
                        )
                    }
                },
            )
            spokenCharacters += chunk.length
        }
        if (uiState.voice.active && uiState.voice.phase == VoicePhase.SPEAKING) {
            uiState = uiState.copy(
                voice = uiState.voice.copy(
                    captionProgress = 1f,
                    speechCharacterIndex = uiState.voice.caption.length,
                    speechLevel = 0f,
                ),
            )
            scheduleListening()
        }
    }

    private fun findSpeechBoundary(text: String): Int {
        val index = text.indexOfFirst { it in ".!?。！？\n" }
        return if (index >= 0) index + 1 else -1
    }

    private fun scheduleListening(delayMs: Long = 520) {
        autoListenJob?.cancel()
        autoListenJob = viewModelScope.launch {
            delay(delayMs)
            if (uiState.voice.active && !uiState.isGenerating) startListening()
        }
    }

    private fun stopVoice(closeOverlay: Boolean) {
        autoListenJob?.cancel()
        autoListenJob = null
        listenJob?.cancel()
        listenJob = null
        speakJob?.cancel()
        speakJob = null
        speechQueueJob?.cancel()
        speechQueueJob = null
        speechChannel?.cancel()
        speechChannel = null
        speechIn.stop()
        speechOut.stop()
        uiState = uiState.copy(
            voice = uiState.voice.copy(active = !closeOverlay),
        )
        if (closeOverlay) {
            uiState = uiState.copy(voice = VoiceUi())
        }
    }

    // ---------------------------------------------------------------- misc

    fun onAppBackgrounded() {
        if (uiState.voice.active) {
            stopVoice(closeOverlay = true)
        }
        if (uiState.isGenerating) {
            stopGeneration()
        }
    }

    fun clearNotice(id: Long) {
        if (uiState.notice?.id == id) {
            uiState = uiState.copy(notice = null)
        }
    }

    fun showNotice(text: String) = notice(text)

    fun startPhotoIndexing() {
        viewModelScope.launch {
            photoIndexManager.startIndexing {
                notice(tr("사진 색인이 완료되었습니다.", "Photo indexing is complete."))
            }
        }
    }

    fun refreshPhotoCount() {
        viewModelScope.launch(Dispatchers.IO) {
            photoIndexManager.refreshDevicePhotoCount()
        }
    }

    fun stopPhotoIndexing() {
        photoIndexManager.stopIndexing()
    }

    fun clearPhotoIndex() {
        viewModelScope.launch {
            val cleared = photoIndexManager.clearIndex()
            notice(
                if (cleared) tr("사진 색인이 초기화되었습니다.", "Photo index cleared.")
                else tr("색인을 완전히 초기화하지 못했습니다.", "Could not fully clear the index."),
            )
        }
    }

    private fun notice(text: String) {
        uiState = uiState.copy(notice = Notice(noticeIds.incrementAndGet(), text))
    }

    private fun emitFeedback(cue: AppFeedbackCue, allowSound: Boolean = true) {
        _feedbackEvents.tryEmit(AppFeedbackEvent(cue = cue, allowSound = allowSound))
    }

    private fun persist() {
        if (!persistedStateLoaded) return
        val snapshot = SessionStore.PersistedState(
            sessions = uiState.sessions,
            activeId = uiState.activeId,
            hapticsEnabled = uiState.hapticsEnabled,
            soundsEnabled = uiState.soundsEnabled,
            warmupEnabled = uiState.warmupEnabled,
            prePrompt = uiState.prePrompt,
            thinkingEnabled = uiState.thinkingEnabled,
            temperature = uiState.temperature,
            topK = uiState.topK,
            maxOutputTokens = uiState.maxOutputTokens,
        )
        synchronized(persistLock) {
            pendingPersist = snapshot
            if (persistJob != null) return
            persistJob = viewModelScope.launch(Dispatchers.IO) {
                while (true) {
                    val next = synchronized(persistLock) {
                        val value = pendingPersist
                        pendingPersist = null
                        if (value == null) {
                            persistJob = null
                        }
                        value
                    } ?: return@launch
                    val saved = store.save(next)
                    val notifyFailure = synchronized(persistLock) {
                        if (saved) {
                            persistFailureNoticeShown = false
                            false
                        } else if (persistFailureNoticeShown) {
                            false
                        } else {
                            persistFailureNoticeShown = true
                            true
                        }
                    }
                    if (notifyFailure) {
                        withContext(Dispatchers.Main.immediate) {
                            notice(tr("대화를 저장하지 못했어요. 저장 공간을 확인해 주세요.", "Could not save the conversation. Check available storage."))
                        }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        val snapshot = synchronized(persistLock) {
            pendingPersist ?: SessionStore.PersistedState(
                sessions = uiState.sessions,
                activeId = uiState.activeId,
                hapticsEnabled = uiState.hapticsEnabled,
                soundsEnabled = uiState.soundsEnabled,
                prePrompt = uiState.prePrompt,
                thinkingEnabled = uiState.thinkingEnabled,
                temperature = uiState.temperature,
                topK = uiState.topK,
                maxOutputTokens = uiState.maxOutputTokens,
            )
        }
        store.save(snapshot)
        generationJob?.cancel()
        photoIndexManager.close()
        engine.close()
        speechIn.close()
        speechOut.close()
    }
}

private val CONTENT_URI_REGEX = Regex("""content://media/external/images/media/\d+""")

private fun extractContentUris(text: String): List<String> =
    CONTENT_URI_REGEX.findAll(text).map { it.value }.distinct().toList()
