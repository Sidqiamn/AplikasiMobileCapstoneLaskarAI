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
import com.example.aplikasicapstonelaskarai.databinding.FragmentMainBinding

class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var classifier: EmotionClassifier
    private lateinit var scriptManager: ActScriptManager
    private lateinit var ttsManager: TtsManager
    private var currentScript: String? = null
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

        // Inisialisasi UI
        binding.buttonPlayScript.isEnabled = false
        binding.buttonInstallTts.isEnabled = false
        binding.buttonInstallTts.visibility = Button.GONE

        // Inisialisasi TtsManager dengan listener
        ttsManager = TtsManager(requireContext()) { success, errorMessage ->
            activity?.runOnUiThread {
                if (success) {
                    binding.textViewError.text = errorMessage ?: ""
                    binding.buttonInstallTts.visibility = Button.GONE
                    if (currentScript != null) {
                        binding.buttonPlayScript.isEnabled = true
                    }
                } else {
                    binding.textViewError.text = errorMessage ?: "Error: Gagal menginisialisasi Text-to-Speech"
                    binding.buttonPlayScript.isEnabled = false
                    binding.buttonInstallTts.isEnabled = true
                    binding.buttonInstallTts.visibility = Button.VISIBLE
                }
            }
        }

        binding.buttonPredict.setOnClickListener {
            val inputText = binding.editTextInput.text.toString().trim()
            if (inputText.isNotEmpty()) {
                try {
                    val emotion = classifier.predict(inputText)
                    binding.textViewResult.text = "Emosi: $emotion"

                    currentScript = scriptManager.getRandomScript(emotion)
                    if (currentScript!!.isNotEmpty() && currentScript != "Maaf, skrip tidak tersedia untuk $emotion.") {
                        binding.buttonPlayScript.isEnabled = true
                        binding.textViewError.text = "Skrip terapi siap diputar"
                    } else {
                        binding.buttonPlayScript.isEnabled = false
                        binding.textViewError.text = "Skrip terapi tidak tersedia"
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Prediction failed: ${e.message}")
                    binding.textViewResult.text = "Gagal memprediksi emosi"
                    binding.buttonPlayScript.isEnabled = false
                    currentScript = null
                    binding.textViewError.text = "Error: ${e.message}"
                }
            } else {
                binding.textViewResult.text = "Masukkan teks terlebih dahulu!"
                binding.buttonPlayScript.isEnabled = false
                currentScript = null
                binding.textViewError.text = ""
            }
        }

        binding.buttonPlayScript.setOnClickListener {
            if (currentScript != null && currentScript!!.isNotEmpty() && currentScript != "Maaf, skrip tidak tersedia untuk $currentScript.") {
                val action = MainFragmentDirections.actionMainFragmentToAudioFragment(script = currentScript!!)
                findNavController().navigate(action)
                binding.textViewError.text = "Memutar skrip terapi..."
            } else {
                binding.textViewError.text = "Tidak ada skrip untuk diputar"
            }
        }

        binding.buttonInstallTts.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.tts")))
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
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