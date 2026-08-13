package com.myhomechores.app.features.activities

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

interface WordSpeaker : AutoCloseable {
    fun speak(word: String)
    override fun close()
}

class AndroidWordSpeaker(context: Context) : WordSpeaker {
    private var ready = false
    private var textToSpeech: TextToSpeech? = null

    init {
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            val engine = textToSpeech ?: return@TextToSpeech
            if (status == TextToSpeech.SUCCESS) {
                ready = engine.setLanguage(Locale.US) >= TextToSpeech.LANG_AVAILABLE
                engine.setSpeechRate(0.82f)
            }
        }
    }

    override fun speak(word: String) {
        if (ready) {
            textToSpeech?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "myway-$word")
        }
    }

    override fun close() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }
}
