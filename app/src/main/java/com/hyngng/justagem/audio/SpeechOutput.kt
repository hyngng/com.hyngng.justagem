package com.hyngng.justagem.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.google.mlkit.genai.common.FeatureStatus
import com.hyngng.justagem.ui.i18n.tr
import com.google.mlkit.genai.texttospeech.SpeechSynthesis
import com.google.mlkit.genai.texttospeech.SpeechSynthesizer
import com.google.mlkit.genai.texttospeech.SpeechSynthesizerRequest
import com.google.mlkit.genai.texttospeech.SpeechSynthesizerResponse
import com.google.mlkit.genai.texttospeech.VoiceInfo
import com.google.mlkit.genai.texttospeech.speechSynthesizerOptions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.math.sqrt

/**
 * Speech output with the same graceful chain as [SpeechInput]:
 *
 * 1. ML Kit GenAI Text to Speech when a Korean voice is already downloaded.
 *    (The generated voices are only offered on Pixel 8+/Galaxy S25+ class
 *    devices; we never block a reply to download one.)
 * 2. Platform TextToSpeech, which exists on almost every device.
 */
class SpeechOutput(context: Context) : Closeable {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var mlClient: SpeechSynthesizer? = null
    private var mlVoice: VoiceInfo? = null
    private var mlChecked = false

    private var platform: TextToSpeech? = null
    private var platformReady: CompletableDeferred<Boolean>? = null

    /** Speaks [text]; returns true when playback completed. */
    suspend fun speak(
        text: String,
        locale: Locale = Locale.getDefault(),
        onProgress: (Float) -> Unit = {},
        onAmplitude: (Float) -> Unit = {},
    ): Boolean {
        val clean = cleanForSpeech(text)
        if (clean.isBlank()) return false

        if (ensureMlKit(locale)) {
            val ok = try {
                speakMlKit(clean, onProgress, onAmplitude)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                false
            }
            if (ok) return true
        }
        return try {
            speakPlatform(clean, locale, onProgress, onAmplitude)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
    }

    fun stop() {
        scope.launch {
            runCatching { mlClient?.stop() }
            runCatching { platform?.stop() }
        }
    }

    override fun close() {
        stop()
        runCatching { mlClient?.close() }
        mlClient = null
        runCatching { platform?.shutdown() }
        platform = null
        scope.cancel()
    }

    private var mlCurrentLocale: Locale? = null

    private suspend fun ensureMlKit(locale: Locale): Boolean {
        // 앱 표시 언어가 바뀌었을 때 이전 언어의 합성 엔진을 해제하고 새 로케일로 재초기화한다.
        if (mlCurrentLocale != locale) {
            mlChecked = false
            runCatching { mlClient?.close() }
            mlClient = null
            mlVoice = null
            mlCurrentLocale = locale
        }
        if (mlChecked) return mlClient != null && mlVoice != null
        mlChecked = true
        var client: SpeechSynthesizer? = null
        return try {
            client = SpeechSynthesis.getClient(
                speechSynthesizerOptions(locale) {
                    setVoiceFallbackEnabled(true)
                    setLocale(locale)
                },
            )
            val voices = client.getSupportedVoices(locale)
            val preferred = if (locale.language == "ko") {
                voices.firstOrNull { it.name == "star08" }
                    ?: voices.firstOrNull { it.name == "star02" }
                    ?: voices.firstOrNull()
            } else {
                voices.firstOrNull()
            }
            if (preferred == null) {
                client.close()
                return false
            }
            if (client.checkStatus(preferred) == FeatureStatus.AVAILABLE) {
                mlClient = client
                mlVoice = preferred
                true
            } else {
                client.close()
                false
            }
        } catch (cancelled: CancellationException) {
            mlChecked = false
            runCatching { client?.close() }
            throw cancelled
        } catch (_: Exception) {
            runCatching { client?.close() }
            false
        }
    }

    private suspend fun speakMlKit(
        text: String,
        onProgress: (Float) -> Unit,
        onAmplitude: (Float) -> Unit,
    ): Boolean {
        val client = mlClient ?: return false
        val voice = mlVoice ?: return false
        return withTimeoutOrNull(90_000) {
            val request = SpeechSynthesizerRequest(
                SpeechSynthesizerRequest.Input.Text(text, voice),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
                SpeechSynthesizerRequest.QueueMode.QUEUE_FLUSH,
            )
            var completed = false
            var failed = false
            coroutineScope {
                var progressJob: kotlinx.coroutines.Job? = null
                client.synthesize(request).collect { response ->
                    when (response) {
                        is SpeechSynthesizerResponse.StatusUpdate -> when (response.status) {
                            SpeechSynthesizerResponse.Status.START -> {
                                progressJob?.cancel()
                                progressJob = launchEstimatedProgress(text, onProgress)
                                reportOnMain { onAmplitude(0.32f) }
                            }

                            SpeechSynthesizerResponse.Status.DONE,
                            SpeechSynthesizerResponse.Status.STOP,
                            -> completed = true
                        }

                        is SpeechSynthesizerResponse.Error -> failed = true
                        // ML Kit exposes bytes without a public PCM format or playback-frame clock.
                        is SpeechSynthesizerResponse.AudioAvailable -> Unit
                    }
                }
                progressJob?.cancel()
            }
            if (completed && !failed) onProgress(1f)
            onAmplitude(0f)
            completed && !failed
        } ?: false
    }

    private suspend fun speakPlatform(
        text: String,
        locale: Locale,
        onProgress: (Float) -> Unit,
        onAmplitude: (Float) -> Unit,
    ): Boolean {
        val tts = platformTts(locale) ?: return false
        val done = CompletableDeferred<Boolean>()
        val currentUtteranceId = "gemtalk-${System.nanoTime()}"
        var progressJob: kotlinx.coroutines.Job? = null
        val audioLock = Any()
        var pcmEncoding = 0
        var pcmChannels = 1
        var pcmSampleRate = 0
        var nextFrame = 0L
        val audioEnvelope = mutableListOf<AudioEnvelope>()
        var playbackLevelJob: kotlinx.coroutines.Job? = null
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                if (utteranceId != currentUtteranceId) return
                progressJob?.cancel()
                progressJob = scope.launchEstimatedProgress(text, onProgress)
                playbackLevelJob?.cancel()
                playbackLevelJob = scope.launchPlaybackLevel(
                    startFrame = 0L,
                    sampleRate = synchronized(audioLock) { pcmSampleRate },
                    audioLock = audioLock,
                    audioEnvelope = audioEnvelope,
                    onAmplitude = onAmplitude,
                )
            }

            override fun onBeginSynthesis(
                utteranceId: String?,
                sampleRateInHz: Int,
                audioFormat: Int,
                channelCount: Int,
            ) {
                if (utteranceId != currentUtteranceId) return
                // The audio callback contains synthesized PCM. Keep its RMS
                // by frame so onRangeStart can select the level being played.
                synchronized(audioLock) {
                    pcmEncoding = audioFormat
                    pcmChannels = channelCount.coerceAtLeast(1)
                    pcmSampleRate = sampleRateInHz
                    nextFrame = 0L
                    audioEnvelope.clear()
                }
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                if (utteranceId == currentUtteranceId) {
                    progressJob?.cancel()
                    playbackLevelJob?.cancel()
                    playbackLevelJob = scope.launchPlaybackLevel(
                        startFrame = frame.toLong(),
                        sampleRate = synchronized(audioLock) { pcmSampleRate },
                        audioLock = audioLock,
                        audioEnvelope = audioEnvelope,
                        onAmplitude = onAmplitude,
                    )
                    reportOnMain {
                        onProgress(end.coerceIn(0, text.length).toFloat() / text.length.coerceAtLeast(1))
                    }
                }
            }

            override fun onAudioAvailable(utteranceId: String?, audio: ByteArray) {
                if (utteranceId != currentUtteranceId) return
                synchronized(audioLock) {
                    val startFrame = nextFrame
                    val frames = audio.size / bytesPerSample(pcmEncoding).coerceAtLeast(1) /
                        pcmChannels
                    val envelopes = decodeAudioEnvelopeBins(
                        audio = audio,
                        encoding = pcmEncoding,
                        channelCount = pcmChannels,
                        sampleRate = pcmSampleRate,
                        startFrame = startFrame,
                    )
                    nextFrame += frames
                    audioEnvelope += envelopes
                }
            }

            override fun onDone(utteranceId: String?) {
                if (utteranceId != currentUtteranceId) return
                progressJob?.cancel()
                playbackLevelJob?.cancel()
                reportOnMain {
                    onProgress(1f)
                    onAmplitude(0f)
                }
                done.complete(true)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                if (utteranceId != currentUtteranceId) return
                progressJob?.cancel()
                playbackLevelJob?.cancel()
                reportOnMain { onAmplitude(0f) }
                done.complete(false)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                if (utteranceId != currentUtteranceId) return
                progressJob?.cancel()
                playbackLevelJob?.cancel()
                reportOnMain { onAmplitude(0f) }
                done.complete(false)
            }
        })
        val result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, currentUtteranceId)
        if (result != TextToSpeech.SUCCESS) return false
        val completed = withTimeoutOrNull(90_000) { done.await() } ?: false
        progressJob?.cancel()
        playbackLevelJob?.cancel()
        return completed
    }

    private fun CoroutineScope.launchEstimatedProgress(
        text: String,
        onProgress: (Float) -> Unit,
    ) = launch {
        val estimatedDurationMs = estimateSpeechDurationMs(text)
        val startedAt = android.os.SystemClock.elapsedRealtime()
        while (true) {
            delay(40)
            val elapsed = android.os.SystemClock.elapsedRealtime() - startedAt
            val progress = (elapsed.toFloat() / estimatedDurationMs).coerceIn(0f, 0.97f)
            withContext(Dispatchers.Main.immediate) {
                onProgress(progress)
            }
        }
    }

    private fun CoroutineScope.launchPlaybackLevel(
        startFrame: Long,
        sampleRate: Int,
        audioLock: Any,
        audioEnvelope: MutableList<AudioEnvelope>,
        onAmplitude: (Float) -> Unit,
    ) = launch {
        if (sampleRate <= 0) return@launch
        val anchorTime = android.os.SystemClock.elapsedRealtimeNanos()
        while (isActive) {
            val elapsedFrames = ((android.os.SystemClock.elapsedRealtimeNanos() - anchorTime) *
                sampleRate / 1_000_000_000L)
            val frame = startFrame + elapsedFrames
            val level = synchronized(audioLock) {
                audioEnvelope.firstOrNull { frame in it.startFrame until it.endFrame }?.level ?: 0f
            }
            withContext(Dispatchers.Main.immediate) { onAmplitude(level) }
            delay(16)
        }
    }

    private fun reportOnMain(block: () -> Unit) {
        scope.launch { withContext(Dispatchers.Main.immediate) { block() } }
    }

    private fun estimateSpeechDurationMs(text: String): Long {
        val spokenCharacters = text.count { !it.isWhitespace() }.coerceAtLeast(1)
        val pauseCount = text.count { it in ",.!?;:。！？、" }
        return (spokenCharacters / 7.0 * 1_000 + pauseCount * 120)
            .toLong()
            .coerceAtLeast(900)
    }

    private var platformLocale: Locale? = null

    private suspend fun platformTts(locale: Locale): TextToSpeech? {
        val existing = platform
        val ready = platformReady
        if (existing != null && ready != null) {
            val isReady = ready.await()
            if (isReady) {
                if (platformLocale != locale) {
                    platformLocale = locale
                    withContext(Dispatchers.Main) {
                        existing.setLanguage(locale)
                    }
                }
                return existing
            }
            return null
        }

        val deferred = CompletableDeferred<Boolean>()
        platformReady = deferred
        platformLocale = locale
        var engine: TextToSpeech? = null
        withContext(Dispatchers.Main) {
            engine = TextToSpeech(appContext) { status ->
                val tts = engine
                if (status == TextToSpeech.SUCCESS && tts != null) {
                    val language = tts.setLanguage(locale)
                    deferred.complete(
                        language != TextToSpeech.LANG_MISSING_DATA &&
                            language != TextToSpeech.LANG_NOT_SUPPORTED,
                    )
                } else {
                    deferred.complete(false)
                }
            }
            platform = engine
        }
        return if (withTimeoutOrNull(7_000) { deferred.await() } == true) engine else null
    }

    private fun cleanForSpeech(text: String): String {
        val withoutCode = text
            .replace(
                Regex("```[\\s\\S]*?```"),
                tr(" 코드 예시는 화면에서 확인해 주세요. ", " See the code sample on screen. "),
            )
            .replace(Regex("`([^`]*)`"), "$1")
            .replace(Regex("https?://\\S+"), tr(" 링크 ", " link "))
        val plain = buildString {
            withoutCode.forEach { ch ->
                val code = ch.code
                val emoji = code in 0x1F000..0x1FAFF || code in 0x2600..0x27BF
                if (!emoji) append(ch)
            }
        }
        val cleaned = plain
            .replace(Regex("[#*_>~|]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        return if (cleaned.length <= 1_200) cleaned else cleaned.take(1_200).trimEnd() + "."
    }
}

private data class AudioEnvelope(
    val startFrame: Long,
    val endFrame: Long,
    val level: Float,
)

private fun bytesPerSample(encoding: Int): Int = when (encoding) {
    AudioFormat.ENCODING_PCM_8BIT -> 1
    AudioFormat.ENCODING_PCM_16BIT -> 2
    AudioFormat.ENCODING_PCM_FLOAT -> 4
    else -> 0
}

private fun decodeAudioEnvelopeBins(
    audio: ByteArray,
    encoding: Int,
    channelCount: Int,
    sampleRate: Int,
    startFrame: Long,
): List<AudioEnvelope> {
    val bytesPerSample = bytesPerSample(encoding)
    val sampleCount = audio.size / bytesPerSample.coerceAtLeast(1)
    if (bytesPerSample == 0 || sampleCount == 0 || channelCount <= 0 || sampleRate <= 0) return emptyList()

    val buffer = ByteBuffer.wrap(audio).order(ByteOrder.LITTLE_ENDIAN)
    val totalFrames = sampleCount / channelCount
    val framesPerBin = (sampleRate / 50).coerceAtLeast(1)
    val result = ArrayList<AudioEnvelope>((totalFrames + framesPerBin - 1) / framesPerBin)
    var frameOffset = 0
    while (frameOffset < totalFrames) {
        val endFrameOffset = (frameOffset + framesPerBin).coerceAtMost(totalFrames)
        var energy = 0.0
        var values = 0
        for (frame in frameOffset until endFrameOffset) {
            for (channel in 0 until channelCount) {
                val sampleIndex = frame * channelCount + channel
                val sample = when (encoding) {
                    AudioFormat.ENCODING_PCM_8BIT ->
                        ((audio[sampleIndex].toInt() and 0xff) - 128) / 128f
                    AudioFormat.ENCODING_PCM_16BIT -> buffer.getShort(sampleIndex * 2) / 32768f
                    AudioFormat.ENCODING_PCM_FLOAT -> buffer.getFloat(sampleIndex * 4).coerceIn(-1f, 1f)
                    else -> return emptyList()
                }
                energy += sample * sample
                values++
            }
        }
        val rms = if (values == 0) 0f else sqrt(energy / values).toFloat()
        result += AudioEnvelope(
            startFrame = startFrame + frameOffset,
            endFrame = startFrame + endFrameOffset,
            level = ((rms - 0.012f) / 0.20f).coerceIn(0f, 1f),
        )
        frameOffset = endFrameOffset
    }
    return result
}
