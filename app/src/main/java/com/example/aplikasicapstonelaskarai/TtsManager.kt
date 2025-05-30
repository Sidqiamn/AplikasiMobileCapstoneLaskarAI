package com.example.aplikasicapstonelaskarai

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.LinkedList
import java.util.Locale

class TtsManager(context: Context, private val onInitializationListener: (Boolean, String?) -> Unit) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val speechQueue = LinkedList<String>()
    private val TAG = "TtsManager"

    init {
        tts = TextToSpeech(context, this, "com.google.android.tts") // Paksa gunakan Google TTS
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Coba set bahasa Indonesia
            var result = tts?.setLanguage(Locale("id", "ID")) ?: TextToSpeech.LANG_NOT_SUPPORTED
            var errorMessage: String? = null
            var success = true

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Bahasa Indonesia tidak didukung, mencoba fallback ke bahasa Inggris")
                // Fallback ke bahasa Inggris
                result = tts?.setLanguage(Locale.US) ?: TextToSpeech.LANG_NOT_SUPPORTED
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    success = false
                    errorMessage = "Bahasa Indonesia tidak didukung. Silakan instal Google Text-to-Speech dari Play Store atau periksa pengaturan TTS."
                } else {
                    errorMessage = "Bahasa Indonesia tidak tersedia, menggunakan bahasa Inggris sebagai gantinya."
                }
            }

            if (success && errorMessage == null) {
                Log.d(TAG, "TextToSpeech initialized successfully for Indonesian")
            }

            isInitialized = success
            onInitializationListener(success, errorMessage)
            if (isInitialized) {
                // Proses antrean setelah inisialisasi
                while (speechQueue.isNotEmpty()) {
                    speak(speechQueue.removeFirst())
                }
            }
        } else {
            Log.e(TAG, "TextToSpeech initialization failed with status: $status")
            onInitializationListener(false, "Gagal menginisialisasi Text-to-Speech. Pastikan Google Text-to-Speech terinstal.")
        }
    }

    fun speak(text: String) {
        if (isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            Log.d(TAG, "Playing speech: $text")
        } else {
            speechQueue.add(text)
            Log.d(TAG, "TextToSpeech belum diinisialisasi, menambahkan teks ke antrean: $text")
        }
    }

    fun stop() {
        if (isInitialized) {
            tts?.stop()
            Log.d(TAG, "Stopping speech")
        } else {
            Log.w(TAG, "TextToSpeech belum diinisialisasi, tidak bisa menghentikan speech")
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        isInitialized = false
        speechQueue.clear()
        Log.d(TAG, "TextToSpeech shutdown")
    }
}