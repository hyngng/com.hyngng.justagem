package com.hyngng.justagem.audio

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.common.audio.AudioSource
import com.google.mlkit.genai.speechrecognition.SpeechRecognition
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerResponse
import com.google.mlkit.genai.speechrecognition.speechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.speechRecognizerRequest
import com.hyngng.justagem.ui.i18n.tr
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.util.Locale

sealed interface SpeechEvent {
    data class Partial(val text: String) : SpeechEvent
    data class Final(val text: String) : SpeechEvent
    data class Failed(val message: String) : SpeechEvent
    data class InputLevel(val level: Float) : SpeechEvent
    data object InputStarted : SpeechEvent
}

/**
 * Microphone input with a graceful chain:
 *
 * 1. ML Kit GenAI Speech Recognition (Basic mode) when its model is available.
 * 2. Platform SpeechRecognizer otherwise (the ML Kit Basic mode wraps the same
 *    on-device recognizer, but availability differs by device).
 *
 * Advanced mode is intentionally not requested: it is limited to Pixel 10/11
 * and a handful of 2026 flagships, so Basic keeps the app portable.
 *
 * Threading matters here: the platform SpeechRecognizer **must** be created and
 * driven from the main thread. The flow itself is collected on IO, so every
 * platform call hops to Main explicitly.
 */
class SpeechInput(context: Context) : Closeable {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var mlClient: com.google.mlkit.genai.speechrecognition.SpeechRecognizer? = null

    @Volatile
    private var mlLoopActive = false

    @Volatile
    private var platform: SpeechRecognizer? = null

    fun listen(locale: Locale = Locale.getDefault()): Flow<SpeechEvent> = flow {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ensureMlKit(locale)) {
                try {
                    emitAll(mlKitFlow())
                    return@flow
                } catch (c: CancellationException) {
                    throw c
                } catch (_: Throwable) {
                    // Fall through to the platform recognizer. This is a
                    // last-resort repair path; the UI only shows a failure if
                    // both paths fail.
                }
            }
        }
        emitAll(platformFlow(locale))
    }.flowOn(Dispatchers.IO)

    /** Asks the active recognizer to finish; a final result still arrives. */
    fun stop() {
        val engine = platform
        if (engine != null) {
            scope.launch { runCatching { engine.stopListening() } }
        }
        if (mlLoopActive) {
            val client = mlClient
            if (client != null) {
                scope.launch { runCatching { client.stopRecognition() } }
            }
        }
    }

    override fun close() {
        val engine = platform
        platform = null
        val client = mlClient
        mlClient = null
        // onCleared() calls this from the main thread; the launch also covers
        // any caller that happens to be on a background thread.
        scope.launch {
            if (engine != null) {
                runCatching { engine.stopListening() }
                runCatching { engine.destroy() }
            }
            if (client != null) {
                runCatching { client.stopRecognition() }
                runCatching { client.close() }
            }
        }
    }

    private var mlLocale: Locale? = null

    private suspend fun ensureMlKit(locale: Locale): Boolean {
        if (Build.VERSION.SDK_INT < 31) return false
        // 언어 설정이 바뀌었을 때 기존 언어 클라이언트를 해제하고 새 로케일로 재바인딩한다.
        if (mlClient != null && mlLocale != locale) {
            runCatching { mlClient?.close() }
            mlClient = null
        }
        mlLocale = locale
        val client = mlClient ?: runCatching {
            SpeechRecognition.getClient(
                speechRecognizerOptions {
                    this.locale = locale
                    preferredMode = SpeechRecognizerOptions.Mode.MODE_BASIC
                },
            ).also { mlClient = it }
        }.getOrNull() ?: return false
        // Re-check on every session: the model may have arrived after the
        // first attempt, and checkStatus() is cheap compared to recognition.
        return runCatching { client.checkStatus() == FeatureStatus.AVAILABLE }.getOrDefault(false)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun mlKitFlow(): Flow<SpeechEvent> = flow {
        val client = mlClient ?: error("Speech recognizer is not ready")
        val audio = AudioSource.fromMic()
        mlLoopActive = true
        try {
            client.startRecognition(speechRecognizerRequest { audioSource = audio })
                .transformWhile { response ->
                    when (response) {
                        is SpeechRecognizerResponse.PartialTextResponse -> {
                            if (response.text.isNotBlank()) emit(SpeechEvent.Partial(response.text))
                            true
                        }

                        is SpeechRecognizerResponse.FinalTextResponse -> {
                            emit(SpeechEvent.Final(response.text))
                            false
                        }

                        is SpeechRecognizerResponse.ErrorResponse -> {
                            emit(SpeechEvent.Failed(describe(response.e)))
                            false
                        }

                        is SpeechRecognizerResponse.CompletedResponse -> false
                    }
                }
                .collect { emit(it) }
        } finally {
            mlLoopActive = false
            withContext(NonCancellable) {
                runCatching { audio.close() }
                runCatching { client.stopRecognition() }
            }
        }
    }

    private fun platformFlow(locale: Locale): Flow<SpeechEvent> = callbackFlow {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            trySend(SpeechEvent.Failed(tr("이 기기에는 음성 인식 서비스를 사용할 수 없어요.", "Speech recognition is unavailable on this device.")))
            close()
            return@callbackFlow
        }

        var finished = false
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() {
                trySend(SpeechEvent.InputStarted)
            }
            override fun onRmsChanged(rmsdB: Float) {
                if (rmsdB.isFinite()) trySend(SpeechEvent.InputLevel(normalizeRms(rmsdB)))
            }
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit

            override fun onPartialResults(partialResults: Bundle?) {
                if (finished) return
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (text.isNotBlank()) trySend(SpeechEvent.Partial(text))
            }

            override fun onResults(results: Bundle?) {
                if (finished) return
                finished = true
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                    .trim()
                if (text.isBlank()) {
                    trySend(SpeechEvent.Failed(tr("듣지 못했어요. 다시 말씀해 주세요.", "I didn't catch that. Please try again.")))
                } else {
                    trySend(SpeechEvent.Final(text))
                }
                close()
            }

            override fun onError(error: Int) {
                if (finished) return
                finished = true
                trySend(SpeechEvent.Failed(errorText(error)))
                close()
            }
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1100L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 700L)
        }

        // SpeechRecognizer enforces main-thread construction and calls.
        val engine: SpeechRecognizer? = withContext(Dispatchers.Main) {
            val created = try {
                SpeechRecognizer.createSpeechRecognizer(appContext)
            } catch (_: Throwable) {
                null
            }
            if (created == null) {
                trySend(SpeechEvent.Failed(tr("음성 인식 엔진을 준비하지 못했어요.", "Could not prepare speech recognition.")))
                close()
                return@withContext null
            }
            platform = created
            created.setRecognitionListener(listener)
            try {
                created.startListening(intent)
                created
            } catch (_: Throwable) {
                if (platform === created) platform = null
                runCatching { created.destroy() }
                trySend(SpeechEvent.Failed(tr("음성 인식을 시작하지 못했어요.", "Could not start speech recognition.")))
                close()
                null
            }
        }
        if (engine == null) return@callbackFlow

        awaitClose {
            if (platform === engine) platform = null
            scope.launch {
                runCatching { engine.stopListening() }
                runCatching { engine.destroy() }
            }
        }
    }

    private fun errorText(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH -> tr("듣지 못했어요. 다시 말씀해 주세요.", "I didn't catch that. Please try again.")
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> tr("말소리가 들리지 않았어요.", "No speech was heard.")
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> tr("마이크 권한이 필요해요.", "Microphone permission is required.")
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            tr("음성 인식 연결을 확인해 주세요.", "Check the speech recognition connection.")
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> tr("음성 인식이 사용 중이에요. 잠시 후 다시 시도해 주세요.", "Speech recognition is busy. Please try again soon.")
        else -> tr("음성 인식을 사용할 수 없어요. (code $error)", "Speech recognition is unavailable. (code $error)")
    }

    private fun normalizeRms(rmsdB: Float): Float {
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        return if (normalized < 0.12f) 0f else ((normalized - 0.12f) / 0.88f).coerceIn(0f, 1f)
    }

    private fun describe(t: GenAiException): String = buildString {
        append(t.message ?: tr("음성 인식 오류", "Speech recognition error"))
        append(" (errorCode=")
        append(t.errorCode)
        append(")")
    }
}
