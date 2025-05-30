package com.example.aplikasicapstonelaskarai

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.aplikasicapstonelaskarai.databinding.FragmentAudioBinding

class AudioFragment : Fragment() {
    private var _binding: FragmentAudioBinding? = null
    private val binding get() = _binding!!
    private lateinit var ttsManager: TtsManager
    private var isPlaying = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAudioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ambil skrip langsung dari arguments
        val script = arguments?.getString("script") ?: ""

        // Inisialisasi TtsManager
        ttsManager = TtsManager(requireContext()) { success, errorMessage ->
            activity?.runOnUiThread {
                if (success) {
                    binding.textViewStatus.text = "Memutar terapi..."
                    if (script.isNotEmpty()) {
                        ttsManager.speak(script)
                        isPlaying = true
                    }
                } else {
                    binding.textViewStatus.text = errorMessage ?: "Gagal memutar audio"
                }
            }
        }

        // Setup UI
        binding.textviewscript.text = if (script.isNotEmpty()) script else "Tidak ada skrip tersedia"

        binding.buttonPlayPause.setOnClickListener {
            if (isPlaying) {
                ttsManager.stop()
                binding.buttonPlayPause.text = "Putar"
                isPlaying = false
            } else {
                if (script.isNotEmpty()) {
                    ttsManager.speak(script)
                    binding.buttonPlayPause.text = "Jeda"
                    isPlaying = true
                } else {
                    binding.textViewStatus.text = "Tidak ada skrip untuk diputar"
                }
            }
        }

        binding.buttonStop.setOnClickListener {
            ttsManager.stop()
            binding.buttonPlayPause.text = "Putar"
            isPlaying = false
        }

        binding.buttonFeedback.setOnClickListener {
            ttsManager.stop()
            findNavController().navigate(R.id.action_audioFragment_to_feedbackActivity)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ttsManager.shutdown()
        _binding = null
    }
}