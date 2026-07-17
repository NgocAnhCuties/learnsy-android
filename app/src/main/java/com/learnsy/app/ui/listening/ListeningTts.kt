package com.learnsy.app.ui.listening

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

/**
 * ── ListeningTts ──
 * Thay toàn bộ hệ thống Edge TTS (/api/tts) + speechSynthesis fallback
 * trong listening-practice.jsx bằng android.speech.tts.TextToSpeech.
 */
class ListeningTts(context: Context) {

    private var tts: TextToSpeech? = null
    private var ready = false
    private var onStart: (() -> Unit)? = null
    private var onEnd: (() -> Unit)? = null
    private var onError: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                tts?.language = Locale.US
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        onStart?.invoke()
                    }
                    override fun onDone(utteranceId: String?) {
                        onEnd?.invoke()
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        onError?.invoke()
                    }
                })
            }
        }
    }

    fun setListeners(onStart: () -> Unit, onEnd: () -> Unit, onError: () -> Unit) {
        this.onStart = onStart
        this.onEnd = onEnd
        this.onError = onError
    }

    fun speak(raw: String, rate: Float = 1.0f) {
        if (!ready || raw.isBlank()) {
            onError?.invoke()
            return
        }
        val plain = stripHtml(raw)
            .replace(Regex("_{3,}|▁{3,}"), " blank ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (plain.isEmpty()) return

        tts?.setSpeechRate(rate)
        tts?.speak(plain, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    fun pause() {
        tts?.stop()
        onEnd?.invoke()
    }

    fun stop() {
        tts?.stop()
    }

    fun isSpeaking(): Boolean = tts?.isSpeaking == true

    fun setRate(rate: Float) {
        tts?.setSpeechRate(rate)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
