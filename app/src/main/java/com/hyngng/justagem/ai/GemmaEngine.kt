package com.hyngng.justagem.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.FunctionArgValue
import com.google.mlkit.genai.common.FunctionCall
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.common.StreamingCallback
import com.google.mlkit.genai.prompt.Content
import com.google.mlkit.genai.prompt.FunctionCallPart
import com.google.mlkit.genai.prompt.FunctionResponsePart
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.Feature
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.ModelPreference
import com.google.mlkit.genai.prompt.ModelReleaseStage
import com.google.mlkit.genai.prompt.SystemInstruction
import com.google.mlkit.genai.prompt.generationConfig
import com.google.mlkit.genai.prompt.modelConfig
import com.hyngng.justagem.ai.tools.LocalTool
import com.hyngng.justagem.ai.tools.ToolArgs
import com.hyngng.justagem.ai.tools.ToolConfirmation
import com.hyngng.justagem.ai.tools.ToolOutcome
import com.hyngng.justagem.data.ChatMessage
import com.hyngng.justagem.data.ChatRole
import com.hyngng.justagem.ui.i18n.tr
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** AICore model configuration the app is currently pointed at. */
data class ModelProbe(
    val preference: Int?,
    val label: String,
    val status: Int?,
    val baseModel: String?,
    val fallback: Boolean = false,
    val error: String? = null,
    val detail: String? = null,
    val options: List<ModelOption> = emptyList(),
    val supportsTools: Boolean = false,
)

data class ModelOption(
    val key: String,
    val label: String,
    val releaseStage: Int,
    val preference: Int,
    val status: Int?,
    val baseModel: String?,
    val error: String? = null,
    val supportsTools: Boolean = false,
)

sealed interface DownloadEvent {
    data class Started(val totalBytes: Long) : DownloadEvent
    data class Progress(val downloadedBytes: Long, val totalBytes: Long) : DownloadEvent
    data class Completed(val baseModel: String?) : DownloadEvent
    data class Failed(val message: String) : DownloadEvent
}

sealed interface GenerationUpdate {
    data class Thought(val value: String) : GenerationUpdate
    data object ThinkingUnavailable : GenerationUpdate
    data object ThinkingStarted : GenerationUpdate
    data object AnswerStarted : GenerationUpdate
    data object ToolBatchStarted : GenerationUpdate
    data class ToolCallPreparing(val name: String? = null) : GenerationUpdate
    data object ToolCallPreparationEnded : GenerationUpdate
    data class ToolStarted(val id: String, val name: String, val arguments: String) : GenerationUpdate
    data class ToolFinished(
        val id: String,
        val summary: String,
        val photoUris: List<String> = emptyList(),
        val photoSearchContext: String = "",
    ) : GenerationUpdate
    data class OutputLimitAdjusted(val requested: Int, val effective: Int) : GenerationUpdate
    data class Text(val value: String) : GenerationUpdate
}

/** Holds everything after a possible tool-call opener until the API returns structured calls. */
private class ToolProtocolStreamGate(toolNames: List<String>) {
    private val opener = Regex(
        "(?i)<(?:ctrl|ctr)\\d+>|<start_function_call>|<function_call>|<\\|tool_call\\|>",
    )
    private val openerPrefixes = listOf(
        "<ctrl", "<ctr", "<start_function_call>", "<function_call>", "<|tool_call|>",
    )
    private var pending = ""
    private val withheld = StringBuilder()
    private var protocolStarted = false
    private var protocolStartPending = false
    private var reportedToolName: String? = null
    private val toolNamePatterns = toolNames.distinct().mapNotNull { name ->
        name.takeIf(String::isNotBlank)?.let {
            it to Regex("(?<![A-Za-z0-9_])${Regex.escape(it)}(?![A-Za-z0-9_])")
        }
    }

    fun consumeProtocolStart(): Boolean {
        val started = protocolStartPending
        protocolStartPending = false
        return started
    }

    fun isProtocolStarted(): Boolean = protocolStarted

    /** Expose only a registered tool's name; the rest of the payload stays withheld. */
    fun consumeToolNameUpdate(): String? {
        if (!protocolStarted) return null
        val payload = withheld.toString()
        val detected = toolNamePatterns.mapNotNull { (name, pattern) ->
            pattern.find(payload)?.let { it.range.first to name }
        }.minByOrNull { it.first }?.second ?: return null
        if (detected == reportedToolName) return null
        reportedToolName = detected
        return detected
    }

    fun reportToolName(name: String): String? {
        val registeredName = toolNamePatterns.firstOrNull { it.first == name }?.first ?: return null
        if (registeredName == reportedToolName) return null
        reportedToolName = registeredName
        return registeredName
    }

    fun append(chunk: String): String {
        if (protocolStarted) {
            withheld.append(chunk)
            return ""
        }

        val combined = pending + chunk
        pending = ""
        val match = opener.find(combined)
        if (match != null) {
            protocolStarted = true
            protocolStartPending = true
            withheld.append(combined.substring(match.range.first))
            return combined.substring(0, match.range.first)
        }

        val possibleStart = combined.lastIndexOf('<')
        val suffix = if (possibleStart >= 0) combined.substring(possibleStart) else ""
        val safeEnd = if (suffix.isPossibleToolOpenerPrefix()) {
            pending = suffix
            possibleStart
        } else {
            combined.length
        }
        return combined.substring(0, safeEnd)
    }

    /** Restore buffered content only when the completed pass was not a structured tool call. */
    fun finish(hasStructuredCalls: Boolean): String {
        if (hasStructuredCalls) {
            pending = ""
            withheld.clear()
            protocolStarted = false
            protocolStartPending = false
            reportedToolName = null
            return ""
        }
        val result = buildString {
            append(withheld)
            append(pending)
        }
        withheld.clear()
        pending = ""
        protocolStarted = false
        protocolStartPending = false
        reportedToolName = null
        return result
    }

    private fun String.isPossibleToolOpenerPrefix(): Boolean {
        if (isEmpty()) return false
        if (openerPrefixes.any { it.startsWith(this, ignoreCase = true) }) return true
        return listOf("<ctrl", "<ctr").any { prefix ->
            startsWith(prefix, ignoreCase = true) && drop(prefix.length).all(Char::isDigit)
        }
    }
}

/**
 * Thin wrapper around the ML Kit GenAI Prompt API.
 *
 * Model choice: AICore exposes Gemma 4 E4B as PREVIEW + FULL. If E4B is not
 * available at all, we transparently fall back to E2B (PREVIEW + FAST) so the
 * app remains usable on devices where only the small preview model is present.
 * The chosen model is reflected in the UI and in [ModelProbe.baseModel].
 */
class GemmaEngine(context: Context) {

    private val appContext = context.applicationContext
    private val lock = Any()
    private val requestMutex = Mutex()

    private var client: GenerativeModel? = null
    private var activeKey: String? = null
    private var selectedReleaseStage: Int = ModelReleaseStage.PREVIEW
    private var selectedPreference: Int = ModelPreference.FULL

    suspend fun probe(): ModelProbe = withContext(Dispatchers.IO) {
        val options = listOf(
            ModelReleaseStage.STABLE to ModelPreference.FULL,
            ModelReleaseStage.STABLE to ModelPreference.FAST,
            ModelReleaseStage.PREVIEW to ModelPreference.FULL,
            ModelReleaseStage.PREVIEW to ModelPreference.FAST,
        ).map { (stage, preference) ->
            val result = check(stage, preference)
            ModelOption(
                key = modelKey(stage, preference),
                label = modelLabel(stage, preference),
                releaseStage = stage,
                preference = preference,
                status = result.status,
                baseModel = result.baseModel,
                error = result.error,
                supportsTools = result.supportsTools,
            )
        }
        val chosen = options.firstOrNull { it.key == modelKey(selectedReleaseStage, selectedPreference) }
            ?.takeIf { it.status != FeatureStatus.UNAVAILABLE && it.status != null }
            ?: options.firstOrNull { it.status == FeatureStatus.AVAILABLE }
            ?: options.firstOrNull { it.status == FeatureStatus.DOWNLOADABLE || it.status == FeatureStatus.DOWNLOADING }
        if (chosen != null) {
            selectedReleaseStage = chosen.releaseStage
            selectedPreference = chosen.preference
        }
        ModelProbe(
            preference = chosen?.preference,
            label = chosen?.label ?: LABEL_E4B,
            status = chosen?.status ?: options.firstNotNullOfOrNull { it.status },
            baseModel = chosen?.baseModel,
            fallback = chosen?.preference == ModelPreference.FAST,
            error = chosen?.error ?: options.firstNotNullOfOrNull { it.error },
            detail = if (chosen == null) options.joinToString(" · ") { "${it.label}: ${statusLabel(it.status)}" } else null,
            options = options.filter { it.status != FeatureStatus.UNAVAILABLE && it.status != null },
            supportsTools = chosen?.supportsTools == true,
        )
    }

    fun selectModel(option: ModelOption) = synchronized(lock) {
        if (selectedReleaseStage != option.releaseStage || selectedPreference != option.preference) {
            closeClientLocked()
        }
        selectedReleaseStage = option.releaseStage
        selectedPreference = option.preference
    }

    /** Downloads whichever preview model [probe] selected. */
    fun download(): Flow<DownloadEvent> = flow {
        val preference = selectedPreference
        val model = synchronized(lock) {
            closeClientLocked()
            Generation.getClient(configFor(selectedReleaseStage, preference)).also {
                client = it
                activeKey = modelKey(selectedReleaseStage, preference)
            }
        }

        var total = 0L
        var failed = false
        try {
            model.download().collect { status ->
                when (status) {
                    is DownloadStatus.DownloadStarted -> {
                        total = status.bytesToDownload
                        emit(DownloadEvent.Started(total))
                    }

                    is DownloadStatus.DownloadProgress -> emit(
                        DownloadEvent.Progress(status.totalBytesDownloaded, total),
                    )

                    is DownloadStatus.DownloadCompleted -> Unit

                    is DownloadStatus.DownloadFailed -> {
                        failed = true
                        emit(DownloadEvent.Failed(describeError(status.e)))
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failed = true
            emit(DownloadEvent.Failed(describeError(e)))
        }
        if (failed) return@flow

        val status = try {
            model.checkStatus()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        if (status != FeatureStatus.AVAILABLE) {
            emit(DownloadEvent.Failed(tr("모델이 아직 준비되지 않았어요 (status=${status ?: "?"})", "The model is not ready yet (status=${status ?: "?"})")))
            return@flow
        }
        val base = try {
            model.getBaseModelName()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        emit(DownloadEvent.Completed(base))
    }.flowOn(Dispatchers.IO)

    /**
     * Streams the next assistant turn.
     *
     * [conversationId] is reused as the ML Kit request's session id so AICore
     * can keep implicit caches warm for this conversation. Correctness does not
     * depend on it: the transcript is sent in full on every request.
     */
    fun streamReply(
        history: List<ChatMessage>,
        conversationId: String,
        enableThinking: Boolean,
        temperature: Float,
        topK: Int,
        maxOutputTokens: Int,
        prePrompt: String,
        tools: List<LocalTool> = emptyList(),
        confirmTool: suspend (ToolConfirmation) -> Boolean = { false },
    ): Flow<GenerationUpdate> = channelFlow {
        val trimmed = PromptHistory.trim(history)
        check(trimmed.isNotEmpty()) { tr("보낼 메시지가 없습니다.", "There is no message to send.") }
        val imageUrisInContext = trimmed.flatMap { it.attachmentUris }.distinct().takeLast(4).toMutableSet()
        val requestBitmaps = mutableListOf<Bitmap>()
        channel.invokeOnClose {
            requestBitmaps.forEach { bitmap -> if (!bitmap.isRecycled) bitmap.recycle() }
        }
        val contents = trimmed.map { message ->
            val contentBuilder = Content.Builder()
                .setRole(
                    if (message.role == ChatRole.USER) Content.Role.USER else Content.Role.MODEL,
                )
                .text(message.text)
            message.attachmentUris.filter { imageUrisInContext.remove(it) }.forEach { uriString ->
                val bitmap = withContext(Dispatchers.IO) {
                    loadAndScaleBitmap(Uri.parse(uriString))
                }
                if (bitmap != null) {
                    requestBitmaps += bitmap
                    contentBuilder.image(bitmap)
                }
            }
            contentBuilder.build()
        }.toMutableList()
        val model = ensureClient()
        val features = runCatching { model.getAvailableFeatures() }.getOrDefault(emptyList())
        val thinkingAvailable = enableThinking && Feature.THINKING_MODE in features
        val toolSupport = tools.isNotEmpty() && Feature.FUNCTION_CALLING in features
        if (enableThinking && !thinkingAvailable) send(GenerationUpdate.ThinkingUnavailable)
        if (thinkingAvailable) send(GenerationUpdate.ThinkingStarted)

        val systemPrompt = prePrompt.trim()
        val tokenLimit = runCatching { model.getTokenLimit() }.getOrDefault(4_096)
        var outputLimitNotified = false
        val attemptedToolCalls = mutableSetOf<String>()
        val toolMemos = mutableMapOf<String, ToolMemo>()
        var searchHistoryInContext = false
        var estimatedChars = trimmed.sumOf { it.text.length } + systemPrompt.length
        val searchImageUris = linkedSetOf<String>()

        while (true) {
            var answerStarted = false
            if (estimatedChars > tokenLimit * CHAR_PER_TOKEN * 0.40 && toolMemos.size > 3) {
                contents.foldOldToolResults(toolMemos)
                estimatedChars = contents.estimateChars() + systemPrompt.length
            }

            val useTools = toolSupport
            val requestBuilder = GenerateContentRequest.Builder(contents)
                .apply {
                    if (systemPrompt.isNotBlank()) {
                        systemInstruction = SystemInstruction(systemPrompt)
                    }
                    this.temperature = temperature
                    this.topK = topK
                    this.maxOutputTokens = maxOutputTokens
                    this.sessionId = conversationId
                    this.enableThinking = thinkingAvailable
                    if (useTools) setTools(tools.map { it.declaration })
                }
            val availableOutputTokens = runCatching {
                model.getTokenLimit() - model.countTokens(requestBuilder.build()).totalTokens
            }.getOrNull()?.coerceAtLeast(1)
            val effectiveMaxOutputTokens = availableOutputTokens?.let { minOf(maxOutputTokens, it) } ?: maxOutputTokens
            if (effectiveMaxOutputTokens < maxOutputTokens && !outputLimitNotified) {
                outputLimitNotified = true
                send(GenerationUpdate.OutputLimitAdjusted(maxOutputTokens, effectiveMaxOutputTokens))
            }
            requestBuilder.maxOutputTokens = effectiveMaxOutputTokens
            val request = requestBuilder.build()

            val passCalls = linkedMapOf<String, FunctionCall>()
            val toolProtocolGate = ToolProtocolStreamGate(
                tools.map { it.declaration.name },
            )
            fun emitAssistantText(text: String) {
                if (text.isEmpty()) return
                if (thinkingAvailable && !answerStarted) {
                    answerStarted = true
                    trySend(GenerationUpdate.AnswerStarted)
                }
                trySend(GenerationUpdate.Text(text))
            }
            requestMutex.withLock {
                val response = model.generateContent(request, object : StreamingCallback {
                        override fun onNewText(additionalText: String) {
                            if (additionalText.isEmpty()) return
                            emitAssistantText(toolProtocolGate.append(additionalText))
                            if (toolProtocolGate.consumeProtocolStart()) {
                                trySend(GenerationUpdate.ToolCallPreparing())
                            }
                            toolProtocolGate.consumeToolNameUpdate()?.let { name ->
                                trySend(GenerationUpdate.ToolCallPreparing(name))
                            }
                        }

                        override fun onNewThought(additionalThought: String) {
                            if (thinkingAvailable && additionalThought.isNotEmpty()) {
                                trySend(GenerationUpdate.Thought(additionalThought))
                            }
                        }

                        override fun onNewFunctionCall(functionCall: FunctionCall) {
                            val name = toolProtocolGate.reportToolName(functionCall.name)
                            trySend(GenerationUpdate.ToolCallPreparing(name))
                            passCalls[functionCall.key()] = functionCall
                        }
                })
                response.candidates.firstOrNull()?.functionCalls?.forEach { call ->
                    passCalls[call.key()] = call
                }
            }

            // Hold the complete protocol section, including streamed arguments, until structured calls arrive.
            val wasPreparingToolCall = toolProtocolGate.isProtocolStarted()
            emitAssistantText(toolProtocolGate.finish(hasStructuredCalls = passCalls.isNotEmpty()))

            if (passCalls.isEmpty()) {
                if (wasPreparingToolCall) trySend(GenerationUpdate.ToolCallPreparationEnded)
                break
            }

            send(GenerationUpdate.ToolBatchStarted)
            contents += Content.Builder()
                .setRole(Content.Role.MODEL)
                .apply { passCalls.values.forEach { addPart(FunctionCallPart(it)) } }
                .build()

            val responseParts = mutableListOf<FunctionResponsePart>()
            var searchHistoryReturnedResult = false
            for (call in passCalls.values) {
                val callName = call.name.orEmpty().ifBlank { "unknown" }
                val responseId = call.id?.takeIf(String::isNotBlank)
                val eventId = responseId ?: UUID.randomUUID().toString()
                val args = call.arguments.mapValues { (_, value) -> value.toAny() }
                val argsSummary = summarizeArguments(callName, args)
                val toolRecordId = "tool-$eventId"
                send(GenerationUpdate.ToolStarted(toolRecordId, callName, argsSummary))
                val outcome = try {
                    executeTool(
                        call = call,
                        args = args,
                        tools = tools,
                        attemptedToolCalls = attemptedToolCalls,
                        searchHistoryInContext = searchHistoryInContext,
                        confirmTool = confirmTool,
                    )
                } catch (c: CancellationException) {
                    send(GenerationUpdate.ToolFinished(toolRecordId, tr("중단됨", "Stopped")))
                    throw c
                }
                if (callName == "search_conversations" && SEARCH_RESULT_COUNT_REGEX.containsMatchIn(outcome.result)) {
                    searchHistoryReturnedResult = true
                }
                if (callName == "search_images") {
                    outcome.photoUris.forEach { photoUri ->
                        if (searchImageUris.size >= MAX_PHOTO_SEARCH_RESULTS || photoUri in searchImageUris) return@forEach
                        searchImageUris += photoUri
                    }
                }
                val wrapped = wrapToolResult(outcome.result)
                responseParts += FunctionResponsePart(
                    responseId,
                    callName,
                    mapOf("result" to wrapped),
                )
                send(
                    GenerationUpdate.ToolFinished(
                        id = toolRecordId,
                        summary = outcome.result.lineSequence().firstOrNull().orEmpty().take(100),
                        photoUris = outcome.photoUris,
                        photoSearchContext = if (callName == "search_images" && outcome.photoUris.isNotEmpty()) {
                            buildString {
                                appendLine("Search query: ${args["query"]?.toString().orEmpty()}")
                                append(outcome.result)
                            }
                        } else {
                            ""
                        },
                    ),
                )
                toolMemos[eventId] = ToolMemo(
                    id = eventId,
                    name = callName,
                    argsSummary = argsSummary,
                    resultCount = outcome.result.lineSequence().count().coerceAtLeast(1),
                )
                estimatedChars += wrapped.length
            }
            if (searchImageUris.isNotEmpty()) {
                synthesizePhotoSearchAnswer(
                    model = model,
                    history = trimmed,
                    photoUris = searchImageUris.toList(),
                    systemPrompt = systemPrompt,
                    temperature = temperature,
                    topK = topK,
                    maxOutputTokens = maxOutputTokens,
                    thinkingAvailable = thinkingAvailable,
                    sendUpdate = { update -> trySend(update) },
                )
                break
            }

            contents += Content.Builder()
                .setRole(Content.Role.USER)
                .apply { responseParts.forEach { addPart(it) } }
                .build()
            searchHistoryInContext = searchHistoryInContext || searchHistoryReturnedResult
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun synthesizePhotoSearchAnswer(
        model: GenerativeModel,
        history: List<ChatMessage>,
        photoUris: List<String>,
        systemPrompt: String,
        temperature: Float,
        topK: Int,
        maxOutputTokens: Int,
        thinkingAvailable: Boolean,
        sendUpdate: (GenerationUpdate) -> Unit,
    ) {
        val latestUserIndex = history.indexOfLast { it.role == ChatRole.USER }
        if (latestUserIndex < 0) return
        val latestUserMessage = history[latestUserIndex]
        val analyses = mutableListOf<String>()
        val failures = mutableListOf<String>()

        photoUris.forEachIndexed { index, uriString ->
            val photoNumber = index + 1
            val bitmap = loadAndScaleBitmap(Uri.parse(uriString))
            if (bitmap == null) {
                failures += "Photo $photoNumber: the image could not be loaded."
                return@forEachIndexed
            }

            try {
                val photoPrompt = buildString {
                    appendLine("The user asked: ${latestUserMessage.text}")
                    appendLine("You retrieved photo $photoNumber of ${photoUris.size} from the device's indexed photo library using search_images. It is a tool result, not an image uploaded by the user.")
                    append("Analyze only this photo. Give concise, factual visual observations that directly help answer the user's request. Do not claim details that are not visible.")
                }
                val imageContent = Content.Builder()
                    .setRole(Content.Role.USER)
                    .text(photoPrompt)
                    .image(bitmap)
                    .build()
                val requestBuilder = GenerateContentRequest.Builder(listOf(imageContent)).apply {
                    if (systemPrompt.isNotBlank()) systemInstruction = SystemInstruction(systemPrompt)
                    this.temperature = temperature
                    this.topK = topK
                    this.maxOutputTokens = maxOutputTokens
                    this.enableThinking = thinkingAvailable
                }
                val availableOutputTokens = runCatching {
                    model.getTokenLimit() - model.countTokens(requestBuilder.build()).totalTokens
                }.getOrNull()?.coerceAtLeast(1)
                requestBuilder.maxOutputTokens = availableOutputTokens?.let { minOf(maxOutputTokens, it) } ?: maxOutputTokens
                val analysis = StringBuilder()
                requestMutex.withLock {
                    model.generateContent(requestBuilder.build(), object : StreamingCallback {
                        override fun onNewText(additionalText: String) {
                            if (additionalText.isNotEmpty()) analysis.append(additionalText)
                        }

                        override fun onNewThought(additionalThought: String) {
                            if (thinkingAvailable && additionalThought.isNotEmpty()) {
                                sendUpdate(GenerationUpdate.Thought(additionalThought))
                            }
                        }
                    })
                }
                if (analysis.isNotBlank()) {
                    analyses += "$photoNumber. ${analysis.toString().trim()}"
                } else {
                    failures += "Photo $photoNumber: the model returned no visual analysis."
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                failures += "Photo $photoNumber: ${describeError(error)}"
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }

        if (analyses.isEmpty()) {
            if (thinkingAvailable) sendUpdate(GenerationUpdate.AnswerStarted)
            sendUpdate(
                GenerationUpdate.Text(
                    tr(
                        "사진 ${photoUris.size}장을 찾았지만 시각 분석을 완료하지 못했어요.\n\n${failures.joinToString("\n")}",
                        "I found ${photoUris.size} photos, but couldn't analyze their contents.\n\n${failures.joinToString("\n")}",
                    ),
                ),
            )
            return
        }

        val synthesisPrompt = buildString {
            appendLine("The user asked: ${latestUserMessage.text}")
            appendLine("You searched the device's indexed photo library using search_images. These are your retrieved tool results, not user-uploaded images.")
            appendLine("The following visual observations were produced by analyzing each retrieved photo separately, one image per request:")
            analyses.forEach(::appendLine)
            if (failures.isNotEmpty()) {
                appendLine("Some retrieved photos could not be analyzed. Do not infer their contents; mention that the analysis was incomplete.")
                failures.forEach(::appendLine)
            }
            append("Answer the user's original request using only these observations. Be concise and do not invent visual details.")
        }
        val textContent = Content.Builder()
            .setRole(Content.Role.USER)
            .text(synthesisPrompt)
            .build()
        val requestBuilder = GenerateContentRequest.Builder(listOf(textContent)).apply {
            if (systemPrompt.isNotBlank()) systemInstruction = SystemInstruction(systemPrompt)
            this.temperature = temperature
            this.topK = topK
            this.maxOutputTokens = maxOutputTokens
            this.enableThinking = thinkingAvailable
        }
        val availableOutputTokens = runCatching {
            model.getTokenLimit() - model.countTokens(requestBuilder.build()).totalTokens
        }.getOrNull()?.coerceAtLeast(1)
        requestBuilder.maxOutputTokens = availableOutputTokens?.let { minOf(maxOutputTokens, it) } ?: maxOutputTokens
        var answerStarted = false
        try {
            requestMutex.withLock {
                model.generateContent(requestBuilder.build(), object : StreamingCallback {
                    override fun onNewText(additionalText: String) {
                        if (additionalText.isEmpty()) return
                        if (thinkingAvailable && !answerStarted) {
                            answerStarted = true
                            sendUpdate(GenerationUpdate.AnswerStarted)
                        }
                        sendUpdate(GenerationUpdate.Text(additionalText))
                    }

                    override fun onNewThought(additionalThought: String) {
                        if (thinkingAvailable && additionalThought.isNotEmpty()) {
                            sendUpdate(GenerationUpdate.Thought(additionalThought))
                        }
                    }
                })
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (thinkingAvailable && !answerStarted) sendUpdate(GenerationUpdate.AnswerStarted)
            sendUpdate(
                GenerationUpdate.Text(
                    buildString {
                        appendLine()
                        appendLine(
                            tr(
                                "전체 답변을 마무리하지 못해 사진별 분석 결과를 표시합니다.",
                                "I couldn't finish the combined answer, so here are the per-photo analyses.",
                            ),
                        )
                        analyses.forEach { appendLine(it) }
                        failures.forEach { appendLine(it) }
                        append(describeError(error))
                    },
                ),
            )
        }
    }

    private suspend fun executeTool(
        call: FunctionCall,
        args: Map<String, Any?>,
        tools: List<LocalTool>,
        attemptedToolCalls: MutableSet<String>,
        searchHistoryInContext: Boolean,
        confirmTool: suspend (ToolConfirmation) -> Boolean,
    ): ToolOutcome {
        val toolName = call.name.orEmpty()
        val tool = tools.firstOrNull { it.declaration.name == toolName }
        if (tool == null) {
            val available = tools.joinToString(", ") { it.declaration.name }
            return ToolOutcome(tr("없는 도구 '$toolName'. 사용 가능: $available", "Unknown tool '$toolName'. Available: $available"))
        }
        val key = toolName + "|" + args.entries.sortedBy { it.key }.joinToString("|") { "${it.key}=${it.value}" }
        if (!attemptedToolCalls.add(key)) {
            return ToolOutcome(tr("이 도구와 인자 조합은 이미 한 번 시도해서 재시도하지 않았습니다.", "This tool and argument combination was already tried once, so it was not retried."))
        }
        val webUrl = args["url"] as? String
        val requiresLongQueryConfirmation = toolName == "web_fetch" &&
            searchHistoryInContext && webUrl?.let(::hasLongQueryString) == true
        if (tool.requiresConfirmation || requiresLongQueryConfirmation) {
            val confirmation = if (requiresLongQueryConfirmation) {
                ToolConfirmation(tr("웹 페이지를 가져올까요?", "Fetch this web page?"), webUrl.orEmpty())
            } else {
                tool.confirmation?.invoke(ToolArgs(args))
                    ?: ToolConfirmation(tr("도구를 실행할까요?", "Run this tool?"), toolName)
            }
            val approved = confirmTool(confirmation)
            if (!approved) return ToolOutcome(tr("사용자가 실행을 취소했습니다.", "The user cancelled the run."))
        }
        return try {
            val timeoutMillis = tool.timeoutMillis
            if (timeoutMillis == null) {
                tool.execute(ToolArgs(args))
            } else {
                withTimeout(timeoutMillis) {
                    tool.execute(ToolArgs(args))
                }
            }
        } catch (t: TimeoutCancellationException) {
            val timeoutSeconds = tool.timeoutMillis?.div(1_000)
            ToolOutcome(
                if (timeoutSeconds != null) {
                    tr("시간 초과(${timeoutSeconds}초)", "Timed out (${timeoutSeconds}s)")
                } else {
                    tr("도구 실행 시간 초과", "Tool execution timed out")
                },
            )
        } catch (c: CancellationException) {
            throw c
        } catch (t: Exception) {
            ToolOutcome("error: ${t.message ?: t.javaClass.simpleName}")
        }
    }

    private fun wrapToolResult(result: String): String {
        val safe = result.replace("</tool_result>", "&lt;/tool_result&gt;")
        val wrapperLength = "<tool_result>\n".length + "\n</tool_result>".length
        val reserve = wrapperLength
        val body = if (safe.length + reserve <= MAX_TOOL_RESULT_CHARS) {
            safe
        } else {
            var keep = safe.length
            var marker = ""
            // The omission marker changes length as `keep` changes; settle in a
            // couple of passes so the wrapped result never exceeds the budget.
            while (true) {
                marker = tr("…(이후 ${safe.length - keep}자 생략, 전체 ${safe.length}자)", "…(truncated; showing first $keep of ${safe.length} characters)")
                val next = (MAX_TOOL_RESULT_CHARS - reserve - marker.length).coerceAtLeast(0)
                if (next >= keep) break
                keep = next
            }
            safe.take(keep) + marker
        }
        return "<tool_result>\n$body\n</tool_result>"
    }

    private fun FunctionCall.key(): String = id.orEmpty().ifBlank {
        name.orEmpty() + "|" + arguments.entries.sortedBy { it.key }.joinToString("|") { "${it.key}=${it.value}" }
    }

    private fun FunctionArgValue.toAny(): Any? = when (this) {
        is FunctionArgValue.StringValue -> value
        is FunctionArgValue.IntValue -> value
        is FunctionArgValue.LongValue -> value
        is FunctionArgValue.DoubleValue -> value
        is FunctionArgValue.BoolValue -> value
        is FunctionArgValue.ListValue -> values.map { it.toAny() }
        is FunctionArgValue.MapValue -> map.mapValues { (_, value) -> value.toAny() }
    }

    private fun summarizeArguments(name: String, args: Map<String, Any?>): String {
        if (name == "web_fetch") return args["url"]?.toString().orEmpty()
        val text = args.entries.sortedBy { it.key }
            .joinToString(", ") { (key, value) -> "$key=${value.toString().take(60)}" }
        return if (text.isBlank()) name else "$name($text)"
    }

    private fun hasLongQueryString(url: String): Boolean =
        runCatching { java.net.URI(url).rawQuery?.length ?: 0 }.getOrDefault(0) > 100

    private fun List<Content>.estimateChars(): Int = sumOf { content ->
        content.parts.sumOf { part ->
            when (part) {
                is com.google.mlkit.genai.prompt.TextPart -> part.textString.length
                is FunctionResponsePart -> part.response.values.sumOf { it.toString().length + 20 }
                else -> 120
            }
        }
    }

    private fun MutableList<Content>.foldOldToolResults(memos: MutableMap<String, ToolMemo>) {
        val keep = memos.values.sortedBy { it.id }.takeLast(3).map { it.id }.toSet()
        val folded = mutableSetOf<String>()
        for (i in indices) {
            val content = this[i]
            if (content.parts.none { it is FunctionResponsePart }) continue
            val rebuilt = Content.Builder().setRole(content.role)
            for (part in content.parts) {
                if (part is FunctionResponsePart) {
                    val partId = part.id.orEmpty()
                    if (partId !in keep && memos.containsKey(partId)) {
                        val memo = memos.getValue(partId)
                        folded += partId
                        rebuilt.addPart(
                            FunctionResponsePart(
                                partId,
                                part.name.orEmpty(),
                                mapOf(
                                    "result" to tr(
                                        "[접힘: ${memo.name} \"${memo.argsSummary.take(80)}\" → ${memo.resultCount}건. 같은 인자로 다시 호출하면 복원]",
                                        "[folded: ${memo.name} \"${memo.argsSummary.take(80)}\" → ${memo.resultCount} rows. Call again with the same arguments to restore]",
                                    ),
                                ),
                            ),
                        )
                        continue
                    }
                }
                rebuilt.addPart(part)
            }
            this[i] = rebuilt.build()
        }
        folded.forEach { memos.remove(it) }
    }

    private data class ToolMemo(
        val id: String,
        val name: String,
        val argsSummary: String,
        val resultCount: Int,
    )

    /** Create a short session label after its first successful text answer. */
    suspend fun generateSessionTitle(userMessage: String, assistantMessage: String): String? =
        requestMutex.withLock {
            withContext(Dispatchers.IO) {
                val model = ensureClient()
                val prompt = buildString {
                    appendLine("다음 대화 내용을 대표하는 짧은 제목 하나를 사용자의 언어로 작성하세요.")
                    appendLine("규칙: 한 줄, 제목만 출력하고 따옴표나 설명은 쓰지 마세요.")
                    appendLine()
                    appendLine("사용자: ${userMessage.take(500)}")
                    append("답변: ${assistantMessage.take(700)}")
                }
                val content = Content.Builder()
                    .setRole(Content.Role.USER)
                    .text(prompt)
                    .build()
                val request = GenerateContentRequest.Builder(listOf(content)).apply {
                    temperature = 0.2f
                    topK = 16
                    maxOutputTokens = 32
                }.build()
                model.generateContent(request).candidates.firstOrNull()?.text
                    ?.lineSequence()
                    ?.firstOrNull()
                    ?.trim()
                    ?.trim('"', '\'', '「', '」', '“', '”', '*', '#', ' ')
                    ?.take(34)
                    ?.takeIf(String::isNotBlank)
            }
        }

    /** Drops the native client so an in-flight generation is actually stopped. */
    fun abort() = synchronized(lock) { closeClientLocked() }

    fun close() = synchronized(lock) { closeClientLocked() }

    /**
     * Quietly raises the accelerator and its buffers so the first user turn
     * skips the slow cold start. Runs only when nothing is generating —
     * [ChatViewModel.warmupModel] guards that and cancels this before a real
     * turn, so warmup and generateContent never contend for AICore.
     */
    suspend fun warmup() {
        try {
            val model = ensureClient()
            if (model.checkStatus() == FeatureStatus.AVAILABLE) {
                model.warmup()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Low-memory devices or a busy AICore can reject warmup; the first
            // real turn then simply runs its normal cold start.
        }
    }

    private suspend fun ensureClient(): GenerativeModel {
        client?.let { return it }
        val stage = selectedReleaseStage
        val preference = selectedPreference
        return Generation.getClient(configFor(stage, preference)).also {
            synchronized(lock) {
                client = it
                activeKey = modelKey(stage, preference)
            }
        }
    }

    private fun closeClientLocked() {
        runCatching { client?.close() }
        client = null
        activeKey = null
    }

    private fun configFor(stage: Int, preference: Int) = generationConfig {
        modelConfig = modelConfig {
            releaseStage = stage
            this.preference = preference
        }
    }

    private fun statusLabel(status: Int?): String = when (status) {
        FeatureStatus.AVAILABLE -> tr("사용 가능", "Available")
        FeatureStatus.DOWNLOADABLE -> tr("내려받기 필요", "Download required")
        FeatureStatus.DOWNLOADING -> tr("내려받는 중", "Downloading")
        FeatureStatus.UNAVAILABLE -> tr("사용 불가", "Unavailable")
        null -> tr("응답 없음", "No response")
        else -> tr("알 수 없음", "Unknown")
    }

    private data class CheckResult(
        val status: Int?,
        val baseModel: String?,
        val error: String?,
        val supportsTools: Boolean = false,
    )

    private suspend fun check(stage: Int, preference: Int): CheckResult {
        var model: GenerativeModel? = null
        return try {
            model = Generation.getClient(configFor(stage, preference))
            val status = model.checkStatus()
            val base = if (status == FeatureStatus.AVAILABLE) {
                runCatching { model.getBaseModelName() }.getOrNull()
            } else {
                null
            }
            val supportsTools = status == FeatureStatus.AVAILABLE &&
                runCatching { Feature.FUNCTION_CALLING in model.getAvailableFeatures() }.getOrDefault(false)
            CheckResult(status, base, null, supportsTools)
        } catch (c: CancellationException) {
            throw c
        } catch (t: Exception) {
            CheckResult(null, null, describeError(t))
        } finally {
            runCatching { model?.close() }
        }
    }

    private fun loadAndScaleBitmap(uri: Uri, maxDimension: Int = 768): Bitmap? {
        return runCatching {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            appContext.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            if (options.outWidth <= 0 || options.outHeight <= 0) return null

            var sample = 1
            while (options.outWidth / (sample * 2) >= maxDimension && options.outHeight / (sample * 2) >= maxDimension) {
                sample *= 2
            }
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = appContext.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            } ?: return null

            val width = decoded.width
            val height = decoded.height
            val maxEdge = maxOf(width, height)
            if (maxEdge <= maxDimension) return decoded

            val ratio = maxDimension.toFloat() / maxEdge
            val scaledW = (width * ratio).toInt().coerceAtLeast(1)
            val scaledH = (height * ratio).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(decoded, scaledW, scaledH, true)
            if (scaled != decoded) {
                decoded.recycle()
            }
            scaled
        }.getOrNull()
    }

    companion object {
        const val LABEL_E4B = "Gemma 4 · E4B"
        const val LABEL_E2B = "Gemma 4 · E2B"
        private const val MAX_TOOL_RESULT_CHARS = 2_000
        private const val MAX_PHOTO_SEARCH_RESULTS = 3
        private const val CHAR_PER_TOKEN = 2.5
        private val SEARCH_RESULT_COUNT_REGEX = Regex("^\\s*[1-9]\\d*\\s*(건|results?)")
        fun modelKey(stage: Int, preference: Int) = "$stage:$preference"
        fun modelLabel(stage: Int, preference: Int): String = buildString {
            append(if (stage == ModelReleaseStage.PREVIEW) "Preview" else "Stable")
            append(" · ")
            append(if (preference == ModelPreference.FULL) "Full" else "Fast")
        }

        fun describeError(t: Throwable): String = buildString {
            append(t.javaClass.simpleName)
            append(": ")
            append(t.message ?: tr("(메시지 없음)", "(no message)"))
            if (t is GenAiException) {
                append("\n  errorCode=")
                append(t.errorCode)
                append(" (")
                append(errorCodeName(t.errorCode))
                append(")")
                append(", aiCoreErrorCode=")
                append(t.aiCoreErrorCode)
                append(", aiCoreErrorType=")
                append(t.aiCoreErrorType)
            }
            var cause = t.cause
            var depth = 0
            while (cause != null && depth < 2) {
                append("\n  caused by ")
                append(cause.javaClass.simpleName)
                append(": ")
                append(cause.message)
                cause = cause.cause
                depth++
            }
        }

        private fun errorCodeName(code: Int): String = when (code) {
            GenAiException.ErrorCode.UNKNOWN -> "UNKNOWN"
            GenAiException.ErrorCode.REQUEST_PROCESSING_ERROR -> "REQUEST_PROCESSING_ERROR"
            GenAiException.ErrorCode.CANCELLED -> "CANCELLED"
            GenAiException.ErrorCode.NOT_AVAILABLE -> "NOT_AVAILABLE"
            GenAiException.ErrorCode.BUSY -> "BUSY"
            GenAiException.ErrorCode.RESPONSE_PROCESSING_ERROR -> "RESPONSE_PROCESSING_ERROR"
            GenAiException.ErrorCode.REQUEST_TOO_LARGE -> "REQUEST_TOO_LARGE"
            GenAiException.ErrorCode.REQUEST_TOO_SMALL -> "REQUEST_TOO_SMALL"
            GenAiException.ErrorCode.RESPONSE_GENERATION_ERROR -> "RESPONSE_GENERATION_ERROR"
            GenAiException.ErrorCode.NOT_SUPPORTED -> "NOT_SUPPORTED"
            GenAiException.ErrorCode.PER_APP_BATTERY_USE_QUOTA_EXCEEDED ->
                "PER_APP_BATTERY_USE_QUOTA_EXCEEDED"
            GenAiException.ErrorCode.BACKGROUND_USE_BLOCKED -> "BACKGROUND_USE_BLOCKED"
            GenAiException.ErrorCode.NOT_ENOUGH_DISK_SPACE -> "NOT_ENOUGH_DISK_SPACE"
            GenAiException.ErrorCode.NEEDS_SYSTEM_UPDATE -> "NEEDS_SYSTEM_UPDATE"
            GenAiException.ErrorCode.AICORE_INCOMPATIBLE -> "AICORE_INCOMPATIBLE"
            GenAiException.ErrorCode.MAX_TURNS_EXCEEDED -> "MAX_TURNS_EXCEEDED"
            else -> "OTHER($code)"
        }
    }
}
