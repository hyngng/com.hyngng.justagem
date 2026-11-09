package com.hyngng.justagem.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.hyngng.justagem.R

/** Plays short, locally bundled interaction chimes at a deliberately low level. */
class FeedbackSoundPlayer(context: Context) : AutoCloseable {

    private val lock = Any()
    private val loaded = mutableSetOf<Int>()
    private val samples = mutableMapOf<AppFeedbackCue, Int>()
    private val pending = mutableListOf<AppFeedbackCue>()
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                synchronized(lock) {
                    loaded += sampleId
                    val cue = samples.entries.firstOrNull { it.value == sampleId }?.key
                    if (cue != null && pending.remove(cue)) playLoaded(cue, sampleId)
                }
            }
        }
        val appContext = context.applicationContext
        samples[AppFeedbackCue.MESSAGE_SENT] = soundPool.load(appContext, R.raw.cue_message_sent, 1)
        // Thinking start/end cues still drive haptics but intentionally have no sound.
        samples[AppFeedbackCue.ANSWER_LEAD_IN] = soundPool.load(appContext, R.raw.cue_answer_lead_in, 1)
        samples[AppFeedbackCue.ANSWER_COMPLETED] = soundPool.load(appContext, R.raw.cue_answer_completed, 1)
        samples[AppFeedbackCue.TEXT_CHARACTER] = soundPool.load(appContext, R.raw.cue_text_character, 1)
        samples[AppFeedbackCue.THOUGHT_CHARACTER] = soundPool.load(appContext, R.raw.cue_thought_character, 1)
    }

    fun play(cue: AppFeedbackCue) {
        synchronized(lock) {
            val sampleId = samples[cue] ?: return
            if (sampleId !in loaded) {
                if (cue !in pending) pending += cue
                return
            }
            playLoaded(cue, sampleId)
        }
    }

    private fun playLoaded(cue: AppFeedbackCue, sampleId: Int) {
        val volume = when (cue) {
            AppFeedbackCue.ANSWER_LEAD_IN -> 0.15f
            AppFeedbackCue.TEXT_CHARACTER -> 0.13f
            AppFeedbackCue.THOUGHT_CHARACTER -> 0.10f
            else -> 0.34f
        }
        soundPool.play(sampleId, volume, volume, 1, 0, 1f)
    }

    override fun close() {
        soundPool.release()
    }
}
