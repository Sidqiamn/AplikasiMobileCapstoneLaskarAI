package com.example.aplikasicapstonelaskarai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aplikasicapstonelaskarai.databinding.FragmentMainBinding
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var classifier: EmotionClassifier
    private lateinit var scriptManager: ActScriptManager
    private lateinit var ttsManager: TtsManager
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    private var currentScript: String? = null
    private var initialEmotion: String? = null // Menyimpan emosi pertama
    private var isFirstMessage: Boolean = true // Melacak apakah ini pesan pertama
    private val TAG = "MainFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi EmotionClassifier
        try {
            classifier = EmotionClassifier(requireContext())
            Log.d(TAG, "EmotionClassifier initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize EmotionClassifier: ${e.message}")
            binding.textViewError.text = "Error: Gagal menginisialisasi model"
            return
        }

        // Inisialisasi ActScriptManager
        scriptManager = ActScriptManager(requireContext())

        // Inisialisasi TtsManager
        ttsManager = TtsManager(requireContext()) { success, errorMessage ->
            activity?.runOnUiThread {
                if (success) {
                    binding.textViewError.text = errorMessage ?: ""
                    if (currentScript != null) {
                        binding.buttonPlayScript.isEnabled = true
                        binding.buttonPlayScript.setBackgroundColor(android.graphics.Color.parseColor("#34C759"))
                    }
                } else {
                    binding.textViewError.text = errorMessage ?: "Error: Gagal menginisialisasi Text-to-Speech"
                    binding.buttonPlayScript.isEnabled = false
                    binding.buttonPlayScript.setBackgroundColor(android.graphics.Color.parseColor("#B0BEC5"))
                    showInstallTtsButton()
                }
            }
        }

        // Inisialisasi RecyclerView untuk chat
        chatAdapter = ChatAdapter(messages)
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.chatRecyclerView.adapter = chatAdapter

        // Tampilkan pertanyaan template
        addMessage("Ceritakan perasaanmu saat ini!", false)

        // Inisialisasi UI
        binding.buttonPlayScript.isEnabled = false

        // Tombol kirim pesan
        binding.sendButton.setOnClickListener {
            val userMessage = binding.inputMessage.text.toString().trim()
            if (userMessage.isNotEmpty()) {
                addMessage(userMessage, true)
                binding.inputMessage.text.clear()
                processUserMessage(userMessage)
            } else {
                binding.textViewError.text = "Masukkan teks terlebih dahulu!"
            }
        }

        // Tombol putar skrip
        binding.buttonPlayScript.setOnClickListener {
            if (currentScript != null && currentScript!!.isNotEmpty() && currentScript != "Maaf, skrip tidak tersedia.") {
                val action = MainFragmentDirections.actionMainFragmentToAudioFragment(script = currentScript!!)
                findNavController().navigate(action)
                binding.textViewError.text = "Memutar audio terapi..."
            } else {
                binding.textViewError.text = "Tidak ada audio untuk diputar"
            }
        }
    }

    private fun showInstallTtsButton() {
        val buttonInstallTts = Button(requireContext()).apply {
            text = "Install TTS"
            setOnClickListener {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.tts")))
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            }
        }
        binding.root.addView(buttonInstallTts)
        // Atur posisi tombol jika diperlukan
    }

    private fun addMessage(message: String, isUser: Boolean) {
        messages.add(ChatMessage(message, isUser))
        chatAdapter.notifyDataSetChanged()
        binding.chatRecyclerView.scrollToPosition(messages.size - 1)
    }

    private fun processUserMessage(inputText: String) {
        try {
            // Prediksi emosi hanya pada pesan pertama
            if (isFirstMessage) {
                val emotion = classifier.predict(inputText)
                initialEmotion = emotion // Simpan emosi pertama
                Log.d(TAG, "Initial emotion predicted: $initialEmotion")

                // Dapatkan skrip berdasarkan emosi (hanya sekali)
                currentScript = scriptManager.getRandomScript(emotion)
                Log.d(TAG, "Current script: $currentScript")

                if (currentScript != null && currentScript!!.isNotEmpty() && currentScript != "Maaf, skrip tidak tersedia untuk $emotion.") {
                    binding.buttonPlayScript.isEnabled = true
                    binding.buttonPlayScript.setBackgroundColor(android.graphics.Color.parseColor("#34C759"))
                    binding.textViewError.text = "Audio terapi siap diputar"
                    Log.d(TAG, "ButtonPlayScript enabled: true")
                } else {
                    binding.buttonPlayScript.isEnabled = false
                    binding.buttonPlayScript.setBackgroundColor(android.graphics.Color.parseColor("#B0BEC5"))
                    binding.textViewError.text = "Audio terapi tidak tersedia"
                    Log.d(TAG, "ButtonPlayScript enabled: false")
                }

                isFirstMessage = false // Tandai bahwa pesan pertama sudah diproses
            }

            // Panggil API Gemini untuk respons, gunakan emosi yang sudah disimpan
            if (initialEmotion != null) {
                modelCall(inputText, initialEmotion!!)
            } else {
                // Jika emosi belum diprediksi (kasus error), gunakan emosi default atau tangani error
                modelCall(inputText, "neutral")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Prediction failed: ${e.message}")
            addMessage("Gagal memproses pesan: ${e.message}", false)
            binding.buttonPlayScript.isEnabled = false
            binding.buttonPlayScript.setBackgroundColor(android.graphics.Color.parseColor("#B0BEC5"))
            binding.textViewError.text = "Error: ${e.message}"
            Log.d(TAG, "ButtonPlayScript enabled: false (error case)")
        }
    }

    private fun modelCall(prompt: String, emotion: String) {
        binding.loadingProgressBar.visibility = View.VISIBLE

        val generativeModel = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = "AIzaSyDnMHce4Y1me6zjYpTys3HVnNz2hqCx6J8" // Ganti dengan kunci API Anda
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = generativeModel.generateContent(
                    "Bayangkan kamu adalah seorang teman yang peduli dan empatik. Berdasarkan emosi '$emotion' yang dirasakan pengguna, berikan satu respons yang sangat mendukung, relevan dengan konteks, dan penuh empati untuk: '$prompt'. Jangan memberikan opsi atau daftar respons, tetapi langsung berikan satu respons terbaik yang alami dan terasa seperti percakapan dengan teman dekat. Jika konteks tidak jelas, buat asumsi yang masuk akal berdasarkan emosi dan input pengguna, lalu tawarkan bantuan spesifik."
                )

                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    addMessage(response.text.toString(), false)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    addMessage("Error: ${e.message}", false)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        classifier.close()
        ttsManager.shutdown()
        _binding = null
    }
}