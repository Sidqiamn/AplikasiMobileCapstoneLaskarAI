package com.example.aplikasicapstonelaskarai

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class AudioActivity : AppCompatActivity() {
    private lateinit var ttsManager: TtsManager
    private var script: String? = null
    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio)

        // Ambil skrip dari intent
        script = intent.getStringExtra("SCRIPT")

        // Inisialisasi TtsManager
        ttsManager = TtsManager(this) { success, errorMessage ->
            runOnUiThread {
                val textViewStatus = findViewById<TextView>(R.id.textViewStatus)
                if (success) {
                    textViewStatus.text = "Memutar terapi..."
                    if (script != null) {
                        ttsManager.speak(script!!)
                        isPlaying = true
                    }
                } else {
                    textViewStatus.text = errorMessage ?: "Gagal memutar audio"
                }
            }
        }

        // Setup UI
        val textViewScript = findViewById<TextView>(R.id.textviewscript)
        val buttonPlayPause = findViewById<MaterialButton>(R.id.buttonPlayPause)
        val buttonStop = findViewById<MaterialButton>(R.id.buttonStop)
        val buttonFeedback = findViewById<MaterialButton>(R.id.buttonFeedback)

        textViewScript.text = script ?: "Tidak ada skrip tersedia"

        buttonPlayPause.setOnClickListener {
            if (isPlaying) {
                ttsManager.stop()
                buttonPlayPause.text = "Putar"
                isPlaying = false
            } else {
                if (script != null) {
                    ttsManager.speak(script!!)
                    buttonPlayPause.text = "Jeda"
                    isPlaying = true
                }
            }
        }

        buttonStop.setOnClickListener {
            ttsManager.stop()
            buttonPlayPause.text = "Putar"
            isPlaying = false
        }

        buttonFeedback.setOnClickListener {
            ttsManager.stop()
            val intent = Intent(this, FeedbackActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
    }
}